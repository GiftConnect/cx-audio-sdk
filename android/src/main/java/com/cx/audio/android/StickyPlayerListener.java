package com.cx.audio.android;

public interface StickyPlayerListener {
    void onStickyPlayerLoaded(StickyPlayerHandle handle);

    default void onStickyPlayerAudioStarted(StickyPlayerHandle handle) {
    }

    default void onStickyPlayerAudioCompleted() {
    }

    void onStickyPlayerClicked(String clickThroughUrl);

    void onStickyPlayerDismissed();

    void onStickyPlayerFailed(Throwable error);
}
