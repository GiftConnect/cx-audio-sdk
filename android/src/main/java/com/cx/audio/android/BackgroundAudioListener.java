package com.cx.audio.android;

public interface BackgroundAudioListener {
    void onBackgroundAudioStarted(BackgroundAudioHandle handle);

    void onBackgroundAudioCompleted();

    void onBackgroundAudioStopped();

    void onBackgroundAudioFailed(Throwable error);
}
