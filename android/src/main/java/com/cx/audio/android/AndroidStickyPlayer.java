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
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.cx.audio.android.internal.StickyPlayerOverlayView;
import com.cx.audio.android.internal.PerAdTrackingState;
import com.cx.audio.android.internal.VastAdLoader;
import com.cx.audio.android.internal.PrivacyEnforcer;
import com.cx.audio.android.internal.PrivacyRequestPurpose;
import com.cx.audio.android.internal.VastCompanion;
import com.cx.audio.android.internal.VastMediaFile;
import com.cx.audio.android.internal.VastStickyPlayerAd;
import com.cx.audio.android.internal.VastTracking;

import java.util.Collections;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AndroidStickyPlayer {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private AndroidStickyPlayer() {
    }

    public static StickyPlayerHandle show(
            Activity activity,
            StickyPlayerRequest request,
            StickyPlayerOptions options,
            StickyPlayerListener listener) {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(listener, "listener");

        StickyPlayerOptions resolvedOptions =
                options == null ? StickyPlayerOptions.builder().build() : options;
        Context appContext = activity.getApplicationContext() == null
                ? activity
                : activity.getApplicationContext();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        PendingStickyPlayerHandle handle = new PendingStickyPlayerHandle(
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
                VastStickyPlayerAd stickyAd = VastAdLoader.loadStickyPlayerAd(request);
                VastCompanion companion = stickyAd.getCompanion();
                Bitmap bitmap = VastAdLoader.loadBitmap(companion.getStaticResourceUrl(), request);
                File localMedia = VastAdLoader.downloadMedia(
                        appContext, stickyAd.getMediaFile().getUrl(), request);
                mainHandler.post(() -> attachOverlay(
                        activity,
                        appContext,
                        request,
                        stickyAd.getMediaFile(),
                        localMedia,
                        companion,
                        stickyAd.getTracking(),
                        bitmap,
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

    public static StickyPlayerHandle show(
            Activity activity,
            StickyPlayerRequest request,
            StickyPlayerListener listener) {
        return show(activity, request, StickyPlayerOptions.builder().build(), listener);
    }

    private static void attachOverlay(
            Activity activity,
            Context appContext,
            StickyPlayerRequest request,
            VastMediaFile mediaFile,
            File localMedia,
            VastCompanion companion,
            VastTracking tracking,
            Bitmap bitmap,
            StickyPlayerOptions options,
            StickyPlayerListener listener,
            PendingStickyPlayerHandle handle) {
        if (handle.isDismissed() || activity.isFinishing() || activity.isDestroyed()) {
            localMedia.delete();
            return;
        }

        FrameLayout decorView = activity.findViewById(android.R.id.content);
        if (decorView == null) {
            localMedia.delete();
            listener.onStickyPlayerFailed(new IllegalStateException("Activity content view is unavailable"));
            return;
        }

        StickyPlayerOverlayView overlayView = new StickyPlayerOverlayView(activity);
        overlayView.bind(companion, bitmap, options);
        overlayView.setOnClickListener(view -> {
            handle.fireClickTracking();
            listener.onStickyPlayerClicked(companion.getClickThroughUrl());
            try {
                VastAdLoader.openClickThrough(activity, companion.getClickThroughUrl());
            } catch (Throwable error) {
                listener.onStickyPlayerFailed(error);
            }
        });
        overlayView.setOnDismissListener(() -> {
            handle.markDismissed();
            listener.onStickyPlayerDismissed();
        });

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 72));
        layoutParams.gravity = options.getPosition() == StickyPlayerPosition.TOP
                ? Gravity.TOP
                : Gravity.BOTTOM;

        decorView.addView(overlayView, layoutParams);
        handle.setTrackingState(new PerAdTrackingState(tracking));
        handle.setTemporaryMediaFile(localMedia);
        handle.setOverlayView(overlayView);
        listener.onStickyPlayerLoaded(handle);
        preparePlayer(appContext, request, mediaFile, localMedia, handle);
    }

    private static void preparePlayer(
            Context context,
            StickyPlayerRequest request,
            VastMediaFile mediaFile,
            File localMedia,
            PendingStickyPlayerHandle handle) {
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
                    Uri.fromFile(localMedia),
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

    private static int dp(Activity activity, int value) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private static final class PendingStickyPlayerHandle implements StickyPlayerHandle {
        private final StickyPlayerListener listener;
        private final Handler mainHandler;
        private StickyPlayerOverlayView overlayView;
        private MediaPlayer mediaPlayer;
        private PerAdTrackingState trackingState;
        private File temporaryMediaFile;
        private boolean dismissed;
        private boolean completed;
        private boolean failed;
        private boolean audioStarted;
        private float volume;
        private final Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                trackProgress();
            }
        };

        private PendingStickyPlayerHandle(
                StickyPlayerListener listener,
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
            if (overlayView != null) {
                ViewParent parent = overlayView.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(overlayView);
                }
            }
            listener.onStickyPlayerDismissed();
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

        private void setOverlayView(StickyPlayerOverlayView overlayView) {
            this.overlayView = overlayView;
        }

        private void setMediaPlayer(MediaPlayer mediaPlayer) {
            if (isInactive()) {
                mediaPlayer.release();
                return;
            }
            this.mediaPlayer = mediaPlayer;
        }

        private void setTrackingState(PerAdTrackingState trackingState) {
            this.trackingState = trackingState;
        }

        private void setTemporaryMediaFile(File file) {
            if (isInactive()) { file.delete(); return; }
            temporaryMediaFile = file;
        }

        private float getVolume() {
            return volume;
        }

        private void notifyAudioStarted() {
            if (isInactive() || audioStarted) {
                return;
            }
            audioStarted = true;
            if (trackingState != null) {
                trackingState.fireImpression();
                trackingState.fireStart();
            }
            scheduleProgressTracking();
            listener.onStickyPlayerAudioStarted(this);
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
            cleanTemporaryMedia();
            listener.onStickyPlayerAudioCompleted();
            removeOverlay();
            listener.onStickyPlayerDismissed();
        }

        private void fail(Throwable error) {
            if (isInactive()) {
                return;
            }
            failed = true;
            mainHandler.removeCallbacks(progressRunnable);
            releasePlayer();
            removeOverlay();
            listener.onStickyPlayerFailed(error);
        }

        private void fireClickTracking() {
            if (trackingState != null) {
                trackingState.fireClick();
            }
        }

        private void scheduleProgressTracking() {
            mainHandler.removeCallbacks(progressRunnable);
            mainHandler.post(progressRunnable);
        }

        private void trackProgress() {
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

        private void releasePlayer() {
            mainHandler.removeCallbacks(progressRunnable);
            MediaPlayer player = mediaPlayer;
            mediaPlayer = null;
            if (player != null) {
                player.release();
            }
            cleanTemporaryMedia();
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

        private void cleanTemporaryMedia() {
            File file = temporaryMediaFile;
            temporaryMediaFile = null;
            if (file != null) file.delete();
        }
    }
}
