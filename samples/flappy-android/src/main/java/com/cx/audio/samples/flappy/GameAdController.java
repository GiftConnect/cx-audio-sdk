package com.cx.audio.samples.flappy;

import android.app.Activity;

import com.cx.audio.android.AndroidFullscreenInterstitial;
import com.cx.audio.android.AndroidStickyPlayer;
import com.cx.audio.android.ConsumableAdRequest;
import com.cx.audio.android.CxAudio;
import com.cx.audio.android.FullscreenInterstitialHandle;
import com.cx.audio.android.FullscreenInterstitialListener;
import com.cx.audio.android.FullscreenInterstitialOptions;
import com.cx.audio.android.FullscreenInterstitialRequest;
import com.cx.audio.android.StickyPlayerHandle;
import com.cx.audio.android.StickyPlayerListener;
import com.cx.audio.android.StickyPlayerOptions;
import com.cx.audio.android.StickyPlayerPosition;
import com.cx.audio.android.StickyPlayerRequest;

final class GameAdController {
    interface StatusListener { void onStatus(String status); }

    private final Activity activity;
    private final StatusListener statusListener;
    private StickyPlayerHandle stickyHandle;
    private FullscreenInterstitialHandle interstitialHandle;
    private Runnable interstitialFinished;

    GameAdController(Activity activity, StatusListener statusListener) {
        this.activity = activity;
        this.statusListener = statusListener;
        CxAudio.initialize(activity);
    }

    void showReadySticky() {
        if (stickyHandle != null || interstitialHandle != null) return;
        try {
            stickyHandle = AndroidStickyPlayer.show(
                    activity,
                    StickyPlayerRequest.builder(activity, request()).readTimeoutMs(8000).build(),
                    StickyPlayerOptions.builder().position(StickyPlayerPosition.BOTTOM)
                            .showCloseButton(true).volume(.14f).minDeviceVolume(0).build(),
                    stickyListener);
            status("Ready-screen ad loading");
        } catch (Throwable error) {
            stickyHandle = null;
            status("Ready-screen ad failed: " + error.getMessage());
        }
    }

    void dismissReadySticky() {
        StickyPlayerHandle handle = stickyHandle;
        stickyHandle = null;
        if (handle != null) handle.dismiss();
    }

    void showInterstitial(Runnable onFinished) {
        dismissReadySticky();
        if (interstitialHandle != null) {
            Runnable existing = interstitialFinished;
            interstitialFinished = () -> { if (existing != null) existing.run(); onFinished.run(); };
            return;
        }
        interstitialFinished = onFinished;
        try {
            interstitialHandle = AndroidFullscreenInterstitial.show(
                    activity,
                    FullscreenInterstitialRequest.builder(activity, request()).readTimeoutMs(8000).build(),
                    FullscreenInterstitialOptions.builder().showCloseButton(true)
                            .autoPlayVideo(true).volume(.18f).minDeviceVolume(0).build(),
                    interstitialListener);
            status("Interstitial loading");
        } catch (Throwable error) {
            finishInterstitial("Interstitial request failed: " + error.getMessage());
        }
    }

    void stop() {
        dismissReadySticky();
        FullscreenInterstitialHandle handle = interstitialHandle;
        interstitialHandle = null;
        if (handle != null) handle.dismiss();
        finishInterstitial("Ads stopped");
    }

    private ConsumableAdRequest request() {
        String placementHash = BuildConfig.CX_PLACEMENT_HASH.trim();
        if (placementHash.isEmpty() || "YOUR_PLACEMENT_HASH".equals(placementHash)) {
            throw new IllegalStateException("Set -PcxPlacementHash before requesting an ad");
        }
        return ConsumableAdRequest.builder(placementHash)
                .limitAdTracking(true)
                .appName("Flappy Blob")
                .appBundle(activity.getPackageName())
                .contentGenre("game")
                .contentTitle("Flappy Blob")
                .build();
    }

    private final StickyPlayerListener stickyListener = new StickyPlayerListener() {
        @Override public void onStickyPlayerLoaded(StickyPlayerHandle handle) { stickyHandle = handle; status("Ready-screen ad showing"); }
        @Override public void onStickyPlayerAudioStarted(StickyPlayerHandle handle) { stickyHandle = handle; status("Ready-screen ad playing"); }
        @Override public void onStickyPlayerAudioCompleted() { status("Ready-screen ad complete"); }
        @Override public void onStickyPlayerClicked(String url) { status("Ready-screen ad clicked"); }
        @Override public void onStickyPlayerDismissed() { stickyHandle = null; status("Ready-screen ad dismissed"); }
        @Override public void onStickyPlayerFailed(Throwable error) { stickyHandle = null; status("Ready-screen ad failed: " + error.getMessage()); }
    };

    private final FullscreenInterstitialListener interstitialListener = new FullscreenInterstitialListener() {
        @Override public void onFullscreenInterstitialLoaded(FullscreenInterstitialHandle handle) { interstitialHandle = handle; status("Interstitial showing"); }
        @Override public void onFullscreenInterstitialAudioStarted(FullscreenInterstitialHandle handle) { interstitialHandle = handle; status("Interstitial playing"); }
        @Override public void onFullscreenInterstitialCompleted() { status("Interstitial complete"); }
        @Override public void onFullscreenInterstitialClicked(String url) { status("Interstitial clicked"); }
        @Override public void onFullscreenInterstitialDismissed() { finishInterstitial("Interstitial dismissed"); }
        @Override public void onFullscreenInterstitialFailed(Throwable error) { finishInterstitial("Interstitial failed: " + error.getMessage()); }
    };

    private void finishInterstitial(String message) {
        interstitialHandle = null;
        Runnable finished = interstitialFinished;
        interstitialFinished = null;
        status(message);
        if (finished != null) finished.run();
    }

    private void status(String status) {
        activity.runOnUiThread(() -> statusListener.onStatus(status));
    }
}
