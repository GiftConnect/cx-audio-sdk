package com.cx.audio.android.internal;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

@SuppressWarnings("deprecation")
final class CloseIconDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int sizePx;

    CloseIconDrawable(int color, int sizePx, int strokeWidthPx) {
        this.sizePx = sizePx;
        paint.setColor(color);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(strokeWidthPx);
    }

    @Override
    public void draw(Canvas canvas) {
        int boundsWidth = getBounds().width();
        int boundsHeight = getBounds().height();
        float left = (boundsWidth - sizePx) / 2f;
        float top = (boundsHeight - sizePx) / 2f;
        float right = left + sizePx;
        float bottom = top + sizePx;
        canvas.drawLine(left, top, right, bottom, paint);
        canvas.drawLine(right, top, left, bottom, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
