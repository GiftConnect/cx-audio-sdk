package com.cx.audio.android;

public interface FullscreenInterstitialListener {
    void onFullscreenInterstitialLoaded(FullscreenInterstitialHandle handle);

    default void onFullscreenInterstitialAudioStarted(FullscreenInterstitialHandle handle) {
    }

    default void onFullscreenInterstitialAudioCompleted() {
    }

    default void onFullscreenInterstitialCompleted() {
    }

    void onFullscreenInterstitialClicked(String clickThroughUrl);

    void onFullscreenInterstitialDismissed();

    void onFullscreenInterstitialFailed(Throwable error);
}
