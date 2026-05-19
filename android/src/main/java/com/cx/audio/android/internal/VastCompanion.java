package com.cx.audio.android.internal;

public final class VastCompanion {
    private final String staticResourceUrl;
    private final String clickThroughUrl;
    private final int width;
    private final int height;

    VastCompanion(String staticResourceUrl, String clickThroughUrl, int width, int height) {
        this.staticResourceUrl = staticResourceUrl;
        this.clickThroughUrl = clickThroughUrl;
        this.width = width;
        this.height = height;
    }

    public String getStaticResourceUrl() {
        return staticResourceUrl;
    }

    public String getClickThroughUrl() {
        return clickThroughUrl;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
