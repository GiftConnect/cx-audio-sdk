package com.cx.audio.android.internal;

import java.util.Locale;

public final class VastMediaFile {
    private final String url;
    private final String type;
    private final String delivery;
    private final int bitrate;
    private final int width;
    private final int height;

    VastMediaFile(String url, String type, String delivery, int bitrate) {
        this(url, type, delivery, bitrate, 0, 0);
    }

    VastMediaFile(String url, String type, String delivery, int bitrate, int width, int height) {
        this.url = url;
        this.type = type;
        this.delivery = delivery;
        this.bitrate = bitrate;
        this.width = width;
        this.height = height;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public String getDelivery() {
        return delivery;
    }

    public int getBitrate() {
        return bitrate;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isImage() {
        return matchesTypeOrExtension("image/", ".jpg", ".jpeg", ".png", ".gif", ".webp");
    }

    public boolean isVideo() {
        return matchesTypeOrExtension("video/", ".mp4", ".m4v", ".webm", ".3gp");
    }

    public VastMediaFile withUrl(String url) {
        return new VastMediaFile(url, type, delivery, bitrate, width, height);
    }

    private boolean matchesTypeOrExtension(String typePrefix, String... extensions) {
        if (type != null && type.toLowerCase(Locale.ROOT).startsWith(typePrefix)) {
            return true;
        }

        String normalizedUrl = url.toLowerCase(Locale.ROOT);
        int queryStart = normalizedUrl.indexOf('?');
        if (queryStart >= 0) {
            normalizedUrl = normalizedUrl.substring(0, queryStart);
        }
        int fragmentStart = normalizedUrl.indexOf('#');
        if (fragmentStart >= 0) {
            normalizedUrl = normalizedUrl.substring(0, fragmentStart);
        }
        for (String extension : extensions) {
            if (normalizedUrl.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
