package com.cx.audio.android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.cx.audio.android.internal.StickyPlayerOverlayView;
import com.cx.audio.android.internal.VastAdLoader;
import com.cx.audio.android.internal.VastCompanion;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AndroidStickyPlayer {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private AndroidStickyPlayer() {
    }

    public static StickyPlayerHandle show(
            Activity activity,
            StickyPlayerRequest request,
            StickyPlayerOptions options,
            StickyPlayerListener listener) {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(listener, "listener");

        StickyPlayerOptions resolvedOptions =
                options == null ? StickyPlayerOptions.builder().build() : options;
        Handler mainHandler = new Handler(Looper.getMainLooper());
        PendingStickyPlayerHandle handle = new PendingStickyPlayerHandle(listener);

        EXECUTOR.execute(() -> {
            try {
                VastCompanion companion = VastAdLoader.loadCompanion(request);
                Bitmap bitmap = VastAdLoader.loadBitmap(companion.getStaticResourceUrl(), request);
                mainHandler.post(() -> attachOverlay(
                        activity,
                        companion,
                        bitmap,
                        resolvedOptions,
                        listener,
                        handle));
            } catch (Throwable error) {
                mainHandler.post(() -> {
                    if (!handle.isDismissed()) {
                        listener.onStickyPlayerFailed(error);
                    }
                });
            }
        });

        return handle;
    }

    public static StickyPlayerHandle show(
            Activity activity,
            StickyPlayerRequest request,
            StickyPlayerListener listener) {
        return show(activity, request, StickyPlayerOptions.builder().build(), listener);
    }

    private static void attachOverlay(
            Activity activity,
            VastCompanion companion,
            Bitmap bitmap,
            StickyPlayerOptions options,
            StickyPlayerListener listener,
            PendingStickyPlayerHandle handle) {
        if (handle.isDismissed() || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        FrameLayout decorView = activity.findViewById(android.R.id.content);
        if (decorView == null) {
            listener.onStickyPlayerFailed(new IllegalStateException("Activity content view is unavailable"));
            return;
        }

        StickyPlayerOverlayView overlayView = new StickyPlayerOverlayView(activity);
        overlayView.bind(companion, bitmap, options);
        overlayView.setOnClickListener(view -> {
            listener.onStickyPlayerClicked(companion.getClickThroughUrl());
            try {
                VastAdLoader.openClickThrough(activity, companion.getClickThroughUrl());
            } catch (Throwable error) {
                listener.onStickyPlayerFailed(error);
            }
        });
        overlayView.setOnDismissListener(() -> {
            handle.markDismissed();
            listener.onStickyPlayerDismissed();
        });

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, options.getHeightDp()));
        layoutParams.gravity = options.getPosition() == StickyPlayerPosition.TOP
                ? Gravity.TOP
                : Gravity.BOTTOM;

        decorView.addView(overlayView, layoutParams);
        handle.setOverlayView(overlayView);
        listener.onStickyPlayerLoaded(handle);
    }

    private static int dp(Activity activity, int value) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private static final class PendingStickyPlayerHandle implements StickyPlayerHandle {
        private final StickyPlayerListener listener;
        private StickyPlayerOverlayView overlayView;
        private boolean dismissed;

        private PendingStickyPlayerHandle(StickyPlayerListener listener) {
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
            listener.onStickyPlayerDismissed();
        }

        private boolean isDismissed() {
            return dismissed;
        }

        private void markDismissed() {
            dismissed = true;
        }

        private void setOverlayView(StickyPlayerOverlayView overlayView) {
            this.overlayView = overlayView;
        }
    }
}
