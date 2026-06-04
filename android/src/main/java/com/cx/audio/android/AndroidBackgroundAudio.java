package com.cx.audio.android;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.cx.audio.android.internal.VastAdLoader;
import com.cx.audio.android.internal.VastMediaFile;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AndroidBackgroundAudio {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private AndroidBackgroundAudio() {
    }

    public static BackgroundAudioHandle play(
            Context context,
            BackgroundAudioRequest request,
            BackgroundAudioOptions options,
            BackgroundAudioListener listener) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(listener, "listener");

        BackgroundAudioOptions resolvedOptions =
                options == null ? BackgroundAudioOptions.builder().build() : options;
        Context appContext = context.getApplicationContext() == null
                ? context
                : context.getApplicationContext();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        PendingBackgroundAudioHandle handle = new PendingBackgroundAudioHandle(
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
                VastMediaFile mediaFile = VastAdLoader.loadAudioMediaFile(request);
                mainHandler.post(() -> preparePlayer(appContext, request, mediaFile, handle));
            } catch (Throwable error) {
                mainHandler.post(() -> handle.fail(error));
            }
        });

        return handle;
    }

    public static BackgroundAudioHandle play(
            Context context,
            BackgroundAudioRequest request,
            BackgroundAudioListener listener) {
        return play(context, request, BackgroundAudioOptions.builder().build(), listener);
    }

    private static void preparePlayer(
            Context context,
            BackgroundAudioRequest request,
            VastMediaFile mediaFile,
            PendingBackgroundAudioHandle handle) {
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
                    handle.notifyStarted();
                } catch (Throwable error) {
                    handle.fail(error);
                }
            });
            mediaPlayer.setOnCompletionListener(handle::complete);
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

    private static void validateVolume(float volume) {
        if (Float.isNaN(volume) || volume < 0.0f || volume > 1.0f) {
            throw new IllegalArgumentException("volume must be between 0.0 and 1.0");
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

    private static final class PendingBackgroundAudioHandle implements BackgroundAudioHandle {
        private final BackgroundAudioListener listener;
        private final Handler mainHandler;
        private MediaPlayer mediaPlayer;
        private boolean stopped;
        private boolean completed;
        private boolean failed;
        private boolean started;
        private float volume;

        private PendingBackgroundAudioHandle(
                BackgroundAudioListener listener,
                float volume,
                Handler mainHandler) {
            this.listener = listener;
            this.volume = volume;
            this.mainHandler = mainHandler;
        }

        @Override
        public boolean isPlaying() {
            if (mediaPlayer == null || stopped || completed || failed) {
                return false;
            }
            try {
                return mediaPlayer.isPlaying();
            } catch (IllegalStateException ignored) {
                return false;
            }
        }

        @Override
        public void stop() {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                stopOnMainThread();
            } else {
                mainHandler.post(this::stopOnMainThread);
            }
        }

        @Override
        public void setVolume(float volume) {
            validateVolume(volume);
            if (Looper.myLooper() == Looper.getMainLooper()) {
                setVolumeOnMainThread(volume);
            } else {
                mainHandler.post(() -> setVolumeOnMainThread(volume));
            }
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

        private boolean isInactive() {
            return stopped || completed || failed;
        }

        private void notifyStarted() {
            if (isInactive() || started) {
                return;
            }
            started = true;
            listener.onBackgroundAudioStarted(this);
        }

        private void complete(MediaPlayer player) {
            if (isInactive()) {
                return;
            }
            completed = true;
            if (mediaPlayer == player) {
                mediaPlayer = null;
            }
            player.release();
            listener.onBackgroundAudioCompleted();
        }

        private void fail(Throwable error) {
            if (isInactive()) {
                return;
            }
            failed = true;
            MediaPlayer player = mediaPlayer;
            mediaPlayer = null;
            if (player != null) {
                player.release();
            }
            listener.onBackgroundAudioFailed(error);
        }

        private void stopOnMainThread() {
            if (stopped || completed) {
                return;
            }
            stopped = true;
            MediaPlayer player = mediaPlayer;
            mediaPlayer = null;
            if (player != null) {
                player.release();
            }
            if (!failed) {
                listener.onBackgroundAudioStopped();
            }
        }

        private void setVolumeOnMainThread(float volume) {
            this.volume = volume;
            if (mediaPlayer == null || isInactive()) {
                return;
            }
            try {
                mediaPlayer.setVolume(volume, volume);
            } catch (IllegalStateException ignored) {
                // The player may have completed or failed while this update was queued.
            }
        }
    }
}
