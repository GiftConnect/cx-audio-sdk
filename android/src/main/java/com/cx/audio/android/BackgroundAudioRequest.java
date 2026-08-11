package com.cx.audio.android;

import android.content.Context;
import android.webkit.WebSettings;

import java.net.MalformedURLException;
import java.net.URL;

public final class BackgroundAudioRequest {
    private static final String DEFAULT_USER_AGENT = "cx-audio-sdk-android/0.1";

    private final URL vastTagUrl;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final String userAgent;

    private BackgroundAudioRequest(Builder builder) {
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

    public static Builder builder(Context context, String vastTagUrl) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        Context applicationContext = context.getApplicationContext();
        Builder builder = new Builder(vastTagUrl);
        builder.userAgent = defaultUserAgent(applicationContext);
        return builder;
    }

    public static Builder builder(ConsumableAdRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return new Builder(request.toURLString(
                ConsumableAdRequest.PlacementType.BACKGROUND_AUDIO,
                null,
                DEFAULT_USER_AGENT));
    }

    public static Builder builder(Context context, ConsumableAdRequest request) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        Context applicationContext = context.getApplicationContext();
        String userAgent = defaultUserAgent(applicationContext);
        Builder builder = new Builder(request.toURLString(
                ConsumableAdRequest.PlacementType.BACKGROUND_AUDIO,
                applicationContext,
                userAgent));
        builder.userAgent = userAgent;
        return builder;
    }

    private static String defaultUserAgent(Context context) {
        try {
            String userAgent = WebSettings.getDefaultUserAgent(context);
            return userAgent == null || userAgent.trim().isEmpty() ? DEFAULT_USER_AGENT : userAgent;
        } catch (RuntimeException error) {
            return DEFAULT_USER_AGENT;
        }
    }

    public static final class Builder {
        private final URL vastTagUrl;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 8000;
        private String userAgent = DEFAULT_USER_AGENT;

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

        public BackgroundAudioRequest build() {
            return new BackgroundAudioRequest(this);
        }
    }
}
