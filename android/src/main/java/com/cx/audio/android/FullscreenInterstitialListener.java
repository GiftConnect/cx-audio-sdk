package com.cx.audio.android;

public interface FullscreenInterstitialListener {
    void onFullscreenInterstitialLoaded(FullscreenInterstitialHandle handle);

    void onFullscreenInterstitialClicked(String clickThroughUrl);

    void onFullscreenInterstitialDismissed();

    void onFullscreenInterstitialFailed(Throwable error);
}
