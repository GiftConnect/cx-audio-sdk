package com.cx.audio.android;

public interface BackgroundAudioHandle {
    boolean isPlaying();

    void stop();

    void setVolume(float volume);
}
