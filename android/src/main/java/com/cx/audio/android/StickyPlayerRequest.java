package com.cx.audio.android;

import android.content.Context;

import java.net.MalformedURLException;
import java.net.URL;

public final class StickyPlayerRequest {
    private final URL vastTagUrl;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final String userAgent;

    private StickyPlayerRequest(Builder builder) {
        this.vastTagUrl = builder.vastTagUrl;
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.readTimeoutMs = builder.readTimeoutMs;
        this.userAgent = builder.userAgent;
    }

    public URL getVastTagUrl() {
        return vastTagUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public static Builder builder(String vastTagUrl) {
        return new Builder(vastTagUrl);
    }

    public static Builder builder(ConsumableAdRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return new Builder(request.toURLString(ConsumableAdRequest.PlacementType.STICKY_PLAYER));
    }

    public static Builder builder(Context context, ConsumableAdRequest request) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return new Builder(request.toURLString(
                ConsumableAdRequest.PlacementType.STICKY_PLAYER,
                context.getApplicationContext()));
    }

    public static final class Builder {
        private final URL vastTagUrl;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 8000;
        private String userAgent = "cx-audio-sdk-android/0.1";

        private Builder(String vastTagUrl) {
            if (vastTagUrl == null || vastTagUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("vastTagUrl must not be blank");
            }
            try {
                this.vastTagUrl = new URL(vastTagUrl);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("vastTagUrl must be a valid URL", e);
            }
            String protocol = this.vastTagUrl.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new IllegalArgumentException("vastTagUrl must use HTTP or HTTPS");
            }
        }

        public Builder connectTimeoutMs(int connectTimeoutMs) {
            if (connectTimeoutMs <= 0) {
                throw new IllegalArgumentException("connectTimeoutMs must be > 0");
            }
            this.connectTimeoutMs = connectTimeoutMs;
            return this;
        }

        public Builder readTimeoutMs(int readTimeoutMs) {
            if (readTimeoutMs <= 0) {
                throw new IllegalArgumentException("readTimeoutMs must be > 0");
            }
            this.readTimeoutMs = readTimeoutMs;
            return this;
        }

        public Builder userAgent(String userAgent) {
            if (userAgent == null || userAgent.trim().isEmpty()) {
                throw new IllegalArgumentException("userAgent must not be blank");
            }
            this.userAgent = userAgent;
            return this;
        }

        public StickyPlayerRequest build() {
            return new StickyPlayerRequest(this);
        }
    }
}
