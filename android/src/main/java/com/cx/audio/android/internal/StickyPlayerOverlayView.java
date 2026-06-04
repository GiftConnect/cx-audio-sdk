package com.cx.audio.android.internal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.cx.audio.android.StickyPlayerOptions;

public final class StickyPlayerOverlayView extends FrameLayout {
    private Runnable onDismissListener;

    public StickyPlayerOverlayView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
        setBackgroundColor(Color.rgb(18, 18, 18));
        setElevation(dp(8));
    }

    public void bind(
            VastCompanion companion,
            Bitmap bitmap,
            StickyPlayerOptions options) {
        removeAllViews();
        setContentDescription("Advertisement");

        ImageView backgroundImage = new ImageView(getContext());
        backgroundImage.setImageBitmap(bitmap);
        backgroundImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backgroundImage.setAlpha(0.32f);
        addView(backgroundImage, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        ImageView companionImage = new ImageView(getContext());
        companionImage.setImageBitmap(bitmap);
        companionImage.setAdjustViewBounds(true);
        companionImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        int aspectWidthDp = companion.getWidth() > 0 && companion.getHeight() > 0
                ? Math.round(options.getHeightDp() * (companion.getWidth() / (float) companion.getHeight()))
                : 86;
        int imageWidthDp = Math.min(
                Math.max(86, aspectWidthDp),
                Math.max(86, Math.round(options.getHeightDp() * 1.35f)));
        LayoutParams imageParams = new LayoutParams(
                Math.max(dp(56), dp(imageWidthDp)),
                LayoutParams.MATCH_PARENT);
        imageParams.gravity = Gravity.CENTER;
        int horizontalPadding = dp(options.getHorizontalPaddingDp());
        companionImage.setPadding(horizontalPadding, dp(6), horizontalPadding, dp(6));
        addView(companionImage, imageParams);

        if (options.shouldShowCloseButton()) {
            ImageButton closeButton = new ImageButton(getContext());
            closeButton.setBackgroundColor(Color.TRANSPARENT);
            closeButton.setColorFilter(Color.WHITE);
            closeButton.setContentDescription("Close advertisement");
            closeButton.setImageDrawable(new CloseIconDrawable(Color.WHITE, dp(18), dp(2)));
            closeButton.setOnClickListener(view -> {
                ViewParent parent = getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(this);
                }
                if (onDismissListener != null) {
                    onDismissListener.run();
                }
            });

            LayoutParams closeParams = new LayoutParams(dp(44), dp(44));
            closeParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
            closeParams.rightMargin = dp(4);
            addView(closeButton, closeParams);
        }
    }

    public void setOnDismissListener(Runnable onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
