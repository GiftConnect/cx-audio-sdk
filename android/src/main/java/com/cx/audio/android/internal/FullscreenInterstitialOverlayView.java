package com.cx.audio.android.internal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.cx.audio.android.FullscreenInterstitialOptions;

import java.util.Collections;

public final class FullscreenInterstitialOverlayView extends FrameLayout {
    private static final int COMPANION_HEIGHT_DP = 250;
    private static final int COMPANION_MAX_WIDTH_DP = 300;
    private Runnable onDismissListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable videoProgressRunnable;

    public FullscreenInterstitialOverlayView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
        setBackgroundColor(Color.BLACK);
        setContentDescription("Advertisement");
    }

    public void bind(
            VastInterstitialAd interstitialAd,
            Bitmap mediaBitmap,
            Bitmap companionBitmap,
            FullscreenInterstitialOptions options,
            String userAgent,
            OnClickListener companionClickListener,
            VideoPlaybackListener videoPlaybackListener) {
        removeAllViews();
        mainHandler.removeCallbacksAndMessages(null);
        videoProgressRunnable = null;

        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        addView(content, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        View mediaView = createMediaView(
                interstitialAd.getMediaFile(),
                mediaBitmap,
                options,
                userAgent,
                videoPlaybackListener);
        content.addView(mediaView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f));

        if (interstitialAd.getCompanion() != null && companionBitmap != null) {
            FrameLayout companionContainer = createCompanionView(
                    companionBitmap,
                    companionClickListener);
            content.addView(companionContainer, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(COMPANION_HEIGHT_DP)));
        }

        if (options.shouldShowCloseButton()) {
            ImageButton closeButton = new ImageButton(getContext());
            closeButton.setBackgroundColor(Color.TRANSPARENT);
            closeButton.setColorFilter(Color.WHITE);
            closeButton.setContentDescription("Close advertisement");
            closeButton.setImageDrawable(new CloseIconDrawable(Color.WHITE, dp(22), dp(2)));
            closeButton.setOnClickListener(view -> dismissFromUserAction());

            LayoutParams closeParams = new LayoutParams(dp(48), dp(48));
            closeParams.gravity = Gravity.TOP | Gravity.END;
            closeParams.topMargin = dp(12);
            closeParams.rightMargin = dp(12);
            addView(closeButton, closeParams);
        }
    }

    public void setOnDismissListener(Runnable onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    private View createMediaView(
            VastMediaFile mediaFile,
            Bitmap mediaBitmap,
            FullscreenInterstitialOptions options,
            String userAgent,
            VideoPlaybackListener videoPlaybackListener) {
        if (mediaFile != null && mediaBitmap != null && mediaFile.isImage()) {
            ImageView imageView = new ImageView(getContext());
            imageView.setImageBitmap(mediaBitmap);
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setBackgroundColor(Color.BLACK);
            return imageView;
        }

        if (mediaFile != null && mediaFile.isVideo()) {
            VideoView videoView = new VideoView(getContext());
            videoView.setBackgroundColor(Color.BLACK);
            videoView.setVideoURI(
                    Uri.parse(mediaFile.getUrl()),
                    Collections.singletonMap("User-Agent", userAgent));
            if (options.shouldAutoPlayVideo()) {
                videoView.setOnPreparedListener(player -> {
                    videoView.start();
                    if (videoPlaybackListener != null) {
                        videoPlaybackListener.onVideoStarted();
                        scheduleVideoProgress(videoView, videoPlaybackListener);
                    }
                });
            }
            videoView.setOnCompletionListener(player -> {
                mainHandler.removeCallbacksAndMessages(null);
                if (videoPlaybackListener != null) {
                    videoPlaybackListener.onVideoCompleted();
                }
            });
            return videoView;
        }

        TextView slateView = new TextView(getContext());
        slateView.setGravity(Gravity.CENTER);
        slateView.setTextColor(Color.WHITE);
        slateView.setTextSize(18);
        slateView.setText("Advertisement");
        slateView.setBackgroundColor(Color.BLACK);
        return slateView;
    }

    private FrameLayout createCompanionView(
            Bitmap companionBitmap,
            OnClickListener companionClickListener) {
        FrameLayout companionContainer = new FrameLayout(getContext());
        companionContainer.setBackgroundColor(Color.rgb(18, 18, 18));

        ImageView companionImage = new ImageView(getContext());
        companionImage.setImageBitmap(companionBitmap);
        companionImage.setAdjustViewBounds(true);
        companionImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        companionImage.setPadding(dp(8), dp(8), dp(8), dp(8));
        companionImage.setOnClickListener(companionClickListener);
        companionImage.setClickable(true);

        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                dp(COMPANION_MAX_WIDTH_DP),
                FrameLayout.LayoutParams.MATCH_PARENT);
        imageParams.gravity = Gravity.CENTER;
        companionContainer.addView(companionImage, imageParams);
        return companionContainer;
    }

    private void dismissFromUserAction() {
        mainHandler.removeCallbacksAndMessages(null);
        ViewParent parent = getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(this);
        }
        if (onDismissListener != null) {
            onDismissListener.run();
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void scheduleVideoProgress(VideoView videoView, VideoPlaybackListener listener) {
        mainHandler.removeCallbacksAndMessages(null);
        videoProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (videoView.getParent() == null) {
                    return;
                }
                listener.onVideoProgress(videoView.getCurrentPosition(), videoView.getDuration());
                mainHandler.postDelayed(this, 500);
            }
        };
        mainHandler.post(videoProgressRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        mainHandler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }

    public interface VideoPlaybackListener {
        void onVideoStarted();

        void onVideoProgress(int currentPositionMs, int durationMs);

        void onVideoCompleted();
    }
}
