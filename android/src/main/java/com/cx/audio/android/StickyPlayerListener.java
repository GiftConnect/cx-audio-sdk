package com.cx.audio.android;

public interface StickyPlayerListener {
    void onStickyPlayerLoaded(StickyPlayerHandle handle);

    void onStickyPlayerClicked(String clickThroughUrl);

    void onStickyPlayerDismissed();

    void onStickyPlayerFailed(Throwable error);
}
