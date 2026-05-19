package com.cx.audio.android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.cx.audio.android.internal.FullscreenInterstitialOverlayView;
import com.cx.audio.android.internal.VastAdLoader;
import com.cx.audio.android.internal.VastCompanion;
import com.cx.audio.android.internal.VastInterstitialAd;
import com.cx.audio.android.internal.VastMediaFile;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AndroidFullscreenInterstitial {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private AndroidFullscreenInterstitial() {
    }

    public static FullscreenInterstitialHandle show(
            Activity activity,
            FullscreenInterstitialRequest request,
            FullscreenInterstitialOptions options,
            FullscreenInterstitialListener listener) {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(listener, "listener");

        FullscreenInterstitialOptions resolvedOptions =
                options == null ? FullscreenInterstitialOptions.builder().build() : options;
        Handler mainHandler = new Handler(Looper.getMainLooper());
        PendingFullscreenInterstitialHandle handle =
                new PendingFullscreenInterstitialHandle(listener);

        EXECUTOR.execute(() -> {
            try {
                VastInterstitialAd interstitialAd = VastAdLoader.loadInterstitial(request);
                Bitmap mediaBitmap = loadMediaBitmapIfNeeded(interstitialAd, request);
                Bitmap companionBitmap = loadCompanionBitmapIfNeeded(interstitialAd, request);
                mainHandler.post(() -> attachOverlay(
                        activity,
                        request,
                        interstitialAd,
                        mediaBitmap,
                        companionBitmap,
                        resolvedOptions,
                        listener,
                        handle));
            } catch (Throwable error) {
                mainHandler.post(() -> {
                    if (!handle.isDismissed()) {
                        listener.onFullscreenInterstitialFailed(error);
                    }
                });
            }
        });

        return handle;
    }

    public static FullscreenInterstitialHandle show(
            Activity activity,
            FullscreenInterstitialRequest request,
            FullscreenInterstitialListener listener) {
        return show(activity, request, FullscreenInterstitialOptions.builder().build(), listener);
    }

    private static Bitmap loadMediaBitmapIfNeeded(
            VastInterstitialAd interstitialAd,
            FullscreenInterstitialRequest request) throws Exception {
        VastMediaFile mediaFile = interstitialAd.getMediaFile();
        if (mediaFile == null || !mediaFile.isImage()) {
            return null;
        }
        return VastAdLoader.loadBitmap(mediaFile.getUrl(), request);
    }

    private static Bitmap loadCompanionBitmapIfNeeded(
            VastInterstitialAd interstitialAd,
            FullscreenInterstitialRequest request) throws Exception {
        VastCompanion companion = interstitialAd.getCompanion();
        if (companion == null) {
            return null;
        }
        return VastAdLoader.loadBitmap(companion.getStaticResourceUrl(), request);
    }

    private static void attachOverlay(
            Activity activity,
            FullscreenInterstitialRequest request,
            VastInterstitialAd interstitialAd,
            Bitmap mediaBitmap,
            Bitmap companionBitmap,
            FullscreenInterstitialOptions options,
            FullscreenInterstitialListener listener,
            PendingFullscreenInterstitialHandle handle) {
        if (handle.isDismissed() || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        FrameLayout decorView = activity.findViewById(android.R.id.content);
        if (decorView == null) {
            listener.onFullscreenInterstitialFailed(
                    new IllegalStateException("Activity content view is unavailable"));
            return;
        }

        FullscreenInterstitialOverlayView overlayView =
                new FullscreenInterstitialOverlayView(activity);
        overlayView.bind(
                interstitialAd,
                mediaBitmap,
                companionBitmap,
                options,
                request.getUserAgent(),
                view -> handleCompanionClick(activity, interstitialAd, listener));
        overlayView.setOnDismissListener(() -> {
            handle.markDismissed();
            listener.onFullscreenInterstitialDismissed();
        });

        decorView.addView(overlayView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        handle.setOverlayView(overlayView);
        listener.onFullscreenInterstitialLoaded(handle);
    }

    private static void handleCompanionClick(
            Activity activity,
            VastInterstitialAd interstitialAd,
            FullscreenInterstitialListener listener) {
        VastCompanion companion = interstitialAd.getCompanion();
        if (companion == null) {
            return;
        }

        listener.onFullscreenInterstitialClicked(companion.getClickThroughUrl());
        try {
            VastAdLoader.openClickThrough(activity, companion.getClickThroughUrl());
        } catch (Throwable error) {
            listener.onFullscreenInterstitialFailed(error);
        }
    }

    private static final class PendingFullscreenInterstitialHandle
            implements FullscreenInterstitialHandle {
        private final FullscreenInterstitialListener listener;
        private FullscreenInterstitialOverlayView overlayView;
        private boolean dismissed;

        private PendingFullscreenInterstitialHandle(FullscreenInterstitialListener listener) {
            this.listener = listener;
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
            if (dismissed) {
                return;
            }
            dismissed = true;
            if (overlayView != null) {
                ViewParent parent = overlayView.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(overlayView);
                }
            }
            listener.onFullscreenInterstitialDismissed();
        }

        private boolean isDismissed() {
            return dismissed;
        }

        private void markDismissed() {
            dismissed = true;
        }

        private void setOverlayView(FullscreenInterstitialOverlayView overlayView) {
            this.overlayView = overlayView;
        }
    }
}
