package com.cx.audio.samples.flappy;

import android.app.Activity;

import com.cx.audio.android.AndroidFullscreenInterstitial;
import com.cx.audio.android.AndroidStickyPlayer;
import com.cx.audio.android.ConsumableAdRequest;
import com.cx.audio.android.CxAgeTreatment;
import com.cx.audio.android.CxAudio;
import com.cx.audio.android.CxAudioConfiguration;
import com.cx.audio.android.CxPrivacyOverride;
import com.cx.audio.android.FullscreenInterstitialHandle;
import com.cx.audio.android.FullscreenInterstitialListener;
import com.cx.audio.android.FullscreenInterstitialOptions;
import com.cx.audio.android.FullscreenInterstitialRequest;
import com.cx.audio.android.StickyPlayerHandle;
import com.cx.audio.android.StickyPlayerListener;
import com.cx.audio.android.StickyPlayerOptions;
import com.cx.audio.android.StickyPlayerPosition;
import com.cx.audio.android.StickyPlayerRequest;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class GameAdController {
    interface StatusListener { void onStatus(String status); }

    private final Activity activity;
    private final StatusListener statusListener;
    private StickyPlayerHandle stickyHandle;
    private FullscreenInterstitialHandle interstitialHandle;
    private Runnable interstitialFinished;
    private final ExecutorService identityExecutor = Executors.newSingleThreadExecutor();
    private String advertisingId;
    private boolean privacyResolutionComplete;
    private volatile boolean destroyed;
    private boolean pendingReadySticky;
    private Runnable pendingInterstitialFinished;

    GameAdController(Activity activity, StatusListener statusListener) {
        this.activity = activity;
        this.statusListener = statusListener;
        CxAudio.initialize(activity, configuration(false));
        resolveAdvertisingIdentity();
    }

    void showReadySticky() {
        if (stickyHandle != null || interstitialHandle != null) return;
        if (!privacyResolutionComplete) {
            pendingReadySticky = true;
            status("Ready-screen ad waiting for privacy resolution");
            return;
        }
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
        pendingReadySticky = false;
        StickyPlayerHandle handle = stickyHandle;
        stickyHandle = null;
        if (handle != null) handle.dismiss();
    }

    void showInterstitial(Runnable onFinished) {
        dismissReadySticky();
        if (!privacyResolutionComplete) {
            pendingInterstitialFinished = onFinished;
            status("Interstitial waiting for privacy resolution");
            return;
        }
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
        Runnable pendingFinished = pendingInterstitialFinished;
        pendingInterstitialFinished = null;
        FullscreenInterstitialHandle handle = interstitialHandle;
        interstitialHandle = null;
        if (handle != null) handle.dismiss();
        finishInterstitial("Ads stopped");
        if (pendingFinished != null) pendingFinished.run();
    }

    void destroy() {
        destroyed = true;
        stop();
        identityExecutor.shutdownNow();
    }

    private ConsumableAdRequest request() {
        String placementHash = BuildConfig.CX_PLACEMENT_HASH.trim();
        if (placementHash.isEmpty() || "YOUR_PLACEMENT_HASH".equals(placementHash)) {
            throw new IllegalStateException("Set -PcxPlacementHash before requesting an ad");
        }
        String permittedId = CxAudio.getPrivacyState().isDeviceIdentifiersAllowed()
                ? advertisingId : null;
        ConsumableAdRequest.Builder builder = ConsumableAdRequest.builder(placementHash)
                .limitAdTracking(permittedId == null)
                .appName("Flappy Blob")
                .appBundle(activity.getPackageName())
                .contentGenre("game")
                .contentTitle("Flappy Blob");
        if (permittedId != null) builder.gadid(permittedId).ifa(permittedId);
        return builder.build();
    }

    private void resolveAdvertisingIdentity() {
        identityExecutor.execute(() -> {
            String resolvedId = null;
            try {
                AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(activity);
                if (info != null && !info.isLimitAdTrackingEnabled()) {
                    resolvedId = usableAdvertisingId(info.getId());
                }
            } catch (Throwable ignored) {
                // Missing Play services, deleted IDs, and restricted access all fail closed.
            }
            String finalId = resolvedId;
            activity.runOnUiThread(() -> {
                if (destroyed) return;
                advertisingId = finalId;
                if (BuildConfig.DEBUG) {
                    CxAudio.updatePrivacyOverride(qaPrivacyOverride(advertisingId != null));
                }
                privacyResolutionComplete = true;
                resumePendingAdRequest();
            });
        });
    }

    private void resumePendingAdRequest() {
        if (pendingInterstitialFinished != null) {
            Runnable finished = pendingInterstitialFinished;
            pendingInterstitialFinished = null;
            showInterstitial(finished);
            return;
        }
        if (pendingReadySticky) {
            pendingReadySticky = false;
            showReadySticky();
        }
    }

    private CxAudioConfiguration configuration(boolean personalizedAdsAllowed) {
        CxAudioConfiguration.Builder builder = CxAudioConfiguration.builder()
                .ageTreatment(CxAgeTreatment.GENERAL_AUDIENCE);
        if (BuildConfig.DEBUG) builder.privacyOverride(qaPrivacyOverride(personalizedAdsAllowed));
        return builder.build();
    }

    private CxPrivacyOverride qaPrivacyOverride(boolean personalizedAdsAllowed) {
        return CxPrivacyOverride.builder()
                .gdprApplies(false)
                .personalizedAdsAllowed(personalizedAdsAllowed)
                .usPrivacyOptOut(false)
                .usPrivacyString("1YNN")
                .build();
    }

    private static String usableAdvertisingId(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()
                || "00000000-0000-0000-0000-000000000000".equals(normalized)) return null;
        return normalized;
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
