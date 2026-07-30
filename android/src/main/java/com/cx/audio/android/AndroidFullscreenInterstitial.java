package com.cx.audio.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.cx.audio.android.internal.FullscreenInterstitialOverlayView;
import com.cx.audio.android.internal.PerAdTrackingState;
import com.cx.audio.android.internal.VastAdLoader;
import com.cx.audio.android.internal.VastCompanion;
import com.cx.audio.android.internal.VastInterstitialAd;
import com.cx.audio.android.internal.VastMediaFile;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AndroidFullscreenInterstitial {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private AndroidFullscreenInterstitial() {
    }

    public static FullscreenInterstitialHandle show(
            Activity activity,
            FullscreenInterstitialRequest request,
            FullscreenInterstitialOptions options,
            FullscreenInterstitialListener listener) {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(listener, "listener");

        FullscreenInterstitialOptions resolvedOptions =
                options == null ? FullscreenInterstitialOptions.builder().build() : options;
        Context appContext = activity.getApplicationContext() == null
                ? activity
                : activity.getApplicationContext();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        PendingFullscreenInterstitialHandle handle =
                new PendingFullscreenInterstitialHandle(
                        listener,
                        resolvedOptions.getVolume(),
                        mainHandler);

        if (!isDeviceVolumeAllowed(appContext, resolvedOptions.getMinDeviceVolume())) {
            mainHandler.post(() -> handle.fail(new IllegalStateException(
                    "VOLUME_TOO_LOW: device media volume is below "
                            + resolvedOptions.getMinDeviceVolume())));
            return handle;
        }

        EXECUTOR.execute(() -> {
            try {
                VastInterstitialAd interstitialAd = VastAdLoader.loadInterstitial(request);
                Bitmap mediaBitmap = loadMediaBitmapIfNeeded(interstitialAd, request);
                Bitmap companionBitmap = loadCompanionBitmapIfNeeded(interstitialAd, request);
                mainHandler.post(() -> attachOverlay(
                        activity,
                        appContext,
                        request,
                        interstitialAd,
                        mediaBitmap,
                        companionBitmap,
                        resolvedOptions,
                        listener,
                        handle));
            } catch (Throwable error) {
                mainHandler.post(() -> {
                    if (!handle.isDismissed()) {
                        handle.fail(error);
                    }
                });
            }
        });

        return handle;
    }

    public static FullscreenInterstitialHandle show(
            Activity activity,
            FullscreenInterstitialRequest request,
            FullscreenInterstitialListener listener) {
        return show(activity, request, FullscreenInterstitialOptions.builder().build(), listener);
    }

    private static Bitmap loadMediaBitmapIfNeeded(
            VastInterstitialAd interstitialAd,
            FullscreenInterstitialRequest request) throws Exception {
        VastMediaFile mediaFile = interstitialAd.getMediaFile();
        if (mediaFile == null || !mediaFile.isImage()) {
            return null;
        }
        return VastAdLoader.loadBitmap(mediaFile.getUrl(), request);
    }

    private static Bitmap loadCompanionBitmapIfNeeded(
            VastInterstitialAd interstitialAd,
            FullscreenInterstitialRequest request) throws Exception {
        VastCompanion companion = interstitialAd.getCompanion();
        if (companion == null) {
            return null;
        }
        return VastAdLoader.loadBitmap(companion.getStaticResourceUrl(), request);
    }

    private static void attachOverlay(
            Activity activity,
            Context appContext,
            FullscreenInterstitialRequest request,
            VastInterstitialAd interstitialAd,
            Bitmap mediaBitmap,
            Bitmap companionBitmap,
            FullscreenInterstitialOptions options,
            FullscreenInterstitialListener listener,
            PendingFullscreenInterstitialHandle handle) {
        if (handle.isDismissed() || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        FrameLayout decorView = activity.findViewById(android.R.id.content);
        if (decorView == null) {
            listener.onFullscreenInterstitialFailed(
                    new IllegalStateException("Activity content view is unavailable"));
            return;
        }

        FullscreenInterstitialOverlayView overlayView =
                new FullscreenInterstitialOverlayView(activity);
        overlayView.bind(
                interstitialAd,
                mediaBitmap,
                companionBitmap,
                options,
                request.getUserAgent(),
                view -> handleCompanionClick(activity, interstitialAd, listener, handle),
                new FullscreenInterstitialOverlayView.VideoPlaybackListener() {
                    @Override
                    public void onVideoStarted() {
                        handle.notifyVideoStarted();
                    }

                    @Override
                    public void onVideoProgress(int currentPositionMs, int durationMs) {
                        handle.trackProgress(currentPositionMs, durationMs);
                    }

                    @Override
                    public void onVideoCompleted() {
                        handle.completeVideo();
                    }
                });
        overlayView.setOnDismissListener(() -> {
            handle.markDismissed();
            listener.onFullscreenInterstitialDismissed();
        });

        decorView.addView(overlayView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        handle.setTrackingState(new PerAdTrackingState(interstitialAd.getTracking()));
        handle.setOverlayView(overlayView);
        listener.onFullscreenInterstitialLoaded(handle);
        handle.notifyAdRendered();
        VastMediaFile audioMediaFile = interstitialAd.getAudioMediaFile();
        if (audioMediaFile != null
                && (interstitialAd.getMediaFile() == null || !interstitialAd.getMediaFile().isVideo())) {
            prepareAudioPlayer(appContext, request, audioMediaFile, handle);
        }
    }

    private static void prepareAudioPlayer(
            Context context,
            FullscreenInterstitialRequest request,
            VastMediaFile mediaFile,
            PendingFullscreenInterstitialHandle handle) {
        if (handle.isInactive()) {
            return;
        }

        MediaPlayer mediaPlayer = new MediaPlayer();
        handle.setMediaPlayer(mediaPlayer);
        try {
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setVolume(handle.getVolume(), handle.getVolume());
            mediaPlayer.setOnPreparedListener(player -> {
                if (handle.isInactive()) {
                    return;
                }
                try {
                    player.setVolume(handle.getVolume(), handle.getVolume());
                    player.start();
                    handle.notifyAudioStarted();
                } catch (Throwable error) {
                    handle.fail(error);
                }
            });
            mediaPlayer.setOnCompletionListener(handle::completeAudio);
            mediaPlayer.setOnErrorListener((player, what, extra) -> {
                handle.fail(new IllegalStateException(
                        "MediaPlayer failed with what=" + what + ", extra=" + extra));
                return true;
            });
            mediaPlayer.setDataSource(
                    context,
                    Uri.parse(mediaFile.getUrl()),
                    Collections.singletonMap("User-Agent", request.getUserAgent()));
            mediaPlayer.prepareAsync();
        } catch (Throwable error) {
            handle.fail(error);
        }
    }

    private static boolean isDeviceVolumeAllowed(Context context, float minDeviceVolume) {
        validateVolume(minDeviceVolume);
        if (minDeviceVolume <= 0.0f) {
            return true;
        }

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return true;
        }

        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (maxVolume <= 0) {
            return true;
        }

        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        return currentVolume / (float) maxVolume >= minDeviceVolume;
    }

    private static void validateVolume(float volume) {
        if (Float.isNaN(volume) || volume < 0.0f || volume > 1.0f) {
            throw new IllegalArgumentException("volume must be between 0.0 and 1.0");
        }
    }

    private static void handleCompanionClick(
            Activity activity,
            VastInterstitialAd interstitialAd,
            FullscreenInterstitialListener listener,
            PendingFullscreenInterstitialHandle handle) {
        VastCompanion companion = interstitialAd.getCompanion();
        if (companion == null) {
            return;
        }

        handle.fireClickTracking();
        listener.onFullscreenInterstitialClicked(companion.getClickThroughUrl());
        try {
            VastAdLoader.openClickThrough(activity, companion.getClickThroughUrl());
        } catch (Throwable error) {
            listener.onFullscreenInterstitialFailed(error);
        }
    }

    private static final class PendingFullscreenInterstitialHandle
            implements FullscreenInterstitialHandle {
        private final FullscreenInterstitialListener listener;
        private final Handler mainHandler;
        private FullscreenInterstitialOverlayView overlayView;
        private MediaPlayer mediaPlayer;
        private PerAdTrackingState trackingState;
        private boolean dismissed;
        private boolean completed;
        private boolean failed;
        private boolean rendered;
        private boolean audioStarted;
        private final float volume;
        private final Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                trackAudioProgress();
            }
        };

        private PendingFullscreenInterstitialHandle(
                FullscreenInterstitialListener listener,
                float volume,
                Handler mainHandler) {
            this.listener = listener;
            this.volume = volume;
            this.mainHandler = mainHandler;
        }

        @Override
        public boolean isShowing() {
            return overlayView != null && overlayView.getParent() != null && !dismissed;
        }

        @Override
        public void dismiss() {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                dismissOnMainThread();
            } else {
                new Handler(Looper.getMainLooper()).post(this::dismissOnMainThread);
            }
        }

        private void dismissOnMainThread() {
            if (isInactive()) {
                return;
            }
            dismissed = true;
            releasePlayer();
            removeOverlay();
            listener.onFullscreenInterstitialDismissed();
        }

        private boolean isDismissed() {
            return dismissed;
        }

        private boolean isInactive() {
            return dismissed || completed || failed;
        }

        private void markDismissed() {
            dismissed = true;
            releasePlayer();
        }

        private void setOverlayView(FullscreenInterstitialOverlayView overlayView) {
            this.overlayView = overlayView;
        }

        private void setTrackingState(PerAdTrackingState trackingState) {
            this.trackingState = trackingState;
        }

        private void setMediaPlayer(MediaPlayer mediaPlayer) {
            if (isInactive()) {
                mediaPlayer.release();
                return;
            }
            this.mediaPlayer = mediaPlayer;
        }

        private float getVolume() {
            return volume;
        }

        private void notifyAudioStarted() {
            if (isInactive() || audioStarted) {
                return;
            }
            audioStarted = true;
            scheduleAudioProgressTracking();
            listener.onFullscreenInterstitialAudioStarted(this);
        }

        private void notifyAdRendered() {
            if (isInactive() || rendered) {
                return;
            }
            rendered = true;
            if (trackingState != null) {
                trackingState.fireImpression();
                trackingState.fireStart();
            }
        }

        private void notifyVideoStarted() {
            notifyAdRendered();
        }

        private void completeAudio(MediaPlayer player) {
            if (isInactive()) {
                return;
            }
            completed = true;
            mainHandler.removeCallbacks(progressRunnable);
            if (trackingState != null) {
                trackingState.fireComplete();
            }
            if (mediaPlayer == player) {
                mediaPlayer = null;
            }
            player.release();
            listener.onFullscreenInterstitialAudioCompleted();
            listener.onFullscreenInterstitialCompleted();
            removeOverlay();
            listener.onFullscreenInterstitialDismissed();
        }

        private void completeVideo() {
            if (isInactive()) {
                return;
            }
            completed = true;
            if (trackingState != null) {
                trackingState.fireComplete();
            }
            listener.onFullscreenInterstitialCompleted();
            removeOverlay();
            listener.onFullscreenInterstitialDismissed();
        }

        private void fireClickTracking() {
            if (trackingState != null) {
                trackingState.fireClick();
            }
        }

        private void trackProgress(long currentPositionMs, long durationMs) {
            if (trackingState != null) {
                trackingState.fireProgress(currentPositionMs, durationMs);
            }
        }

        private void scheduleAudioProgressTracking() {
            mainHandler.removeCallbacks(progressRunnable);
            mainHandler.post(progressRunnable);
        }

        private void trackAudioProgress() {
            if (isInactive() || mediaPlayer == null || trackingState == null) {
                return;
            }

            try {
                trackingState.fireProgress(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
            } catch (IllegalStateException ignored) {
                return;
            }

            if (!isInactive()) {
                mainHandler.postDelayed(progressRunnable, 500);
            }
        }

        private void fail(Throwable error) {
            if (isInactive()) {
                return;
            }
            failed = true;
            mainHandler.removeCallbacks(progressRunnable);
            releasePlayer();
            removeOverlay();
            listener.onFullscreenInterstitialFailed(error);
        }

        private void releasePlayer() {
            mainHandler.removeCallbacks(progressRunnable);
            MediaPlayer player = mediaPlayer;
            mediaPlayer = null;
            if (player != null) {
                player.release();
            }
        }

        private void removeOverlay() {
            if (overlayView == null) {
                return;
            }
            ViewParent parent = overlayView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(overlayView);
            }
        }
    }
}
