package com.cx.audio.android.internal;

public final class VastMediaFile {
    private final String url;
    private final String type;
    private final String delivery;
    private final int bitrate;

    VastMediaFile(String url, String type, String delivery, int bitrate) {
        this.url = url;
        this.type = type;
        this.delivery = delivery;
        this.bitrate = bitrate;
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

    VastMediaFile withUrl(String url) {
        return new VastMediaFile(url, type, delivery, bitrate);
    }
}
