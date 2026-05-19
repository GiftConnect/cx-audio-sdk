package com.cx.audio.android.internal;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.cx.audio.android.BackgroundAudioRequest;
import com.cx.audio.android.FullscreenInterstitialRequest;
import com.cx.audio.android.StickyPlayerRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public final class VastAdLoader {
    private static final int MAX_VAST_BYTES = 1024 * 1024;
    private static final int MAX_WRAPPER_DEPTH = 5;
    private static final VastParser VAST_PARSER = new VastParser();

    private VastAdLoader() {
    }

    public static VastCompanion loadCompanion(StickyPlayerRequest request) throws Exception {
        return loadCompanion(RequestConfig.from(request));
    }

    public static VastMediaFile loadAudioMediaFile(BackgroundAudioRequest request) throws Exception {
        return loadAudioMediaFile(RequestConfig.from(request));
    }

    public static VastInterstitialAd loadInterstitial(FullscreenInterstitialRequest request) throws Exception {
        return loadInterstitial(RequestConfig.from(request));
    }

    public static Bitmap loadBitmap(String imageUrl, StickyPlayerRequest request) throws IOException {
        HttpURLConnection connection = openConnection(new URL(imageUrl), RequestConfig.from(request));
        requireSuccessfulResponse(connection);
        try (InputStream inputStream = connection.getInputStream()) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                throw new IOException("Companion image could not be decoded");
            }
            return bitmap;
        } finally {
            connection.disconnect();
        }
    }

    public static Bitmap loadBitmap(String imageUrl, FullscreenInterstitialRequest request)
            throws IOException {
        HttpURLConnection connection = openConnection(new URL(imageUrl), RequestConfig.from(request));
        requireSuccessfulResponse(connection);
        try (InputStream inputStream = connection.getInputStream()) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                throw new IOException("Image media could not be decoded");
            }
            return bitmap;
        } finally {
            connection.disconnect();
        }
    }

    public static void openClickThrough(Context context, String clickThroughUrl) {
        if (clickThroughUrl == null || clickThroughUrl.trim().isEmpty()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickThroughUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static VastCompanion loadCompanion(RequestConfig request) throws Exception {
        URL currentUrl = request.vastTagUrl;
        for (int depth = 0; depth <= MAX_WRAPPER_DEPTH; depth++) {
            String vastXml = readUrl(currentUrl, request, MAX_VAST_BYTES);
            try {
                return VAST_PARSER.parseBestCompanion(vastXml);
            } catch (IllegalArgumentException noCompanion) {
                String wrapperUrl = VAST_PARSER.parseWrapperUrl(vastXml);
                if (wrapperUrl == null) {
                    throw noCompanion;
                }
                currentUrl = new URL(currentUrl, wrapperUrl);
            }
        }
        throw new IOException("VAST wrapper depth exceeded " + MAX_WRAPPER_DEPTH);
    }

    private static VastMediaFile loadAudioMediaFile(RequestConfig request) throws Exception {
        URL currentUrl = request.vastTagUrl;
        for (int depth = 0; depth <= MAX_WRAPPER_DEPTH; depth++) {
            String vastXml = readUrl(currentUrl, request, MAX_VAST_BYTES);
            try {
                VastMediaFile mediaFile = VAST_PARSER.parseBestAudioMediaFile(vastXml);
                return mediaFile.withUrl(new URL(currentUrl, mediaFile.getUrl()).toString());
            } catch (IllegalArgumentException noMediaFile) {
                String wrapperUrl = VAST_PARSER.parseWrapperUrl(vastXml);
                if (wrapperUrl == null) {
                    throw noMediaFile;
                }
                currentUrl = new URL(currentUrl, wrapperUrl);
            }
        }
        throw new IOException("VAST wrapper depth exceeded " + MAX_WRAPPER_DEPTH);
    }

    private static VastInterstitialAd loadInterstitial(RequestConfig request) throws Exception {
        URL currentUrl = request.vastTagUrl;
        for (int depth = 0; depth <= MAX_WRAPPER_DEPTH; depth++) {
            String vastXml = readUrl(currentUrl, request, MAX_VAST_BYTES);
            VastMediaFile mediaFile = parseOptionalInterstitialMediaFile(vastXml, currentUrl);
            VastCompanion companion = parseOptionalCompanion(vastXml, currentUrl);
            if (mediaFile != null || companion != null) {
                return new VastInterstitialAd(mediaFile, companion);
            }

            String wrapperUrl = VAST_PARSER.parseWrapperUrl(vastXml);
            if (wrapperUrl == null) {
                throw new IllegalArgumentException(
                        "VAST response did not include interstitial media or companion");
            }
            currentUrl = new URL(currentUrl, wrapperUrl);
        }
        throw new IOException("VAST wrapper depth exceeded " + MAX_WRAPPER_DEPTH);
    }

    private static VastMediaFile parseOptionalInterstitialMediaFile(String vastXml, URL baseUrl)
            throws Exception {
        try {
            VastMediaFile mediaFile = VAST_PARSER.parseBestInterstitialMediaFile(vastXml);
            return mediaFile.withUrl(new URL(baseUrl, mediaFile.getUrl()).toString());
        } catch (IllegalArgumentException noMediaFile) {
            return null;
        }
    }

    private static VastCompanion parseOptionalCompanion(String vastXml, URL baseUrl)
            throws Exception {
        try {
            VastCompanion companion = VAST_PARSER.parseBestCompanion(vastXml);
            return companion.withStaticResourceUrl(
                    new URL(baseUrl, companion.getStaticResourceUrl()).toString());
        } catch (IllegalArgumentException noCompanion) {
            return null;
        }
    }

    private static String readUrl(URL url, RequestConfig request, int maxBytes) throws IOException {
        HttpURLConnection connection = openConnection(url, request);
        requireSuccessfulResponse(connection);
        try (InputStream inputStream = connection.getInputStream()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int totalBytes = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                totalBytes += read;
                if (totalBytes > maxBytes) {
                    throw new IOException("VAST response exceeded " + maxBytes + " bytes");
                }
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection openConnection(URL url, RequestConfig request) throws IOException {
        URLConnection rawConnection = url.openConnection();
        if (!(rawConnection instanceof HttpURLConnection)) {
            throw new IOException("Unsupported URL protocol: " + url.getProtocol());
        }
        HttpURLConnection connection = (HttpURLConnection) rawConnection;
        connection.setConnectTimeout(request.connectTimeoutMs);
        connection.setReadTimeout(request.readTimeoutMs);
        connection.setRequestProperty("User-Agent", request.userAgent);
        connection.setInstanceFollowRedirects(true);
        return connection;
    }

    private static void requireSuccessfulResponse(HttpURLConnection connection) throws IOException {
        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("HTTP " + statusCode + " loading " + connection.getURL());
        }
    }

    private static final class RequestConfig {
        private final URL vastTagUrl;
        private final int connectTimeoutMs;
        private final int readTimeoutMs;
        private final String userAgent;

        private RequestConfig(
                URL vastTagUrl,
                int connectTimeoutMs,
                int readTimeoutMs,
                String userAgent) {
            this.vastTagUrl = vastTagUrl;
            this.connectTimeoutMs = connectTimeoutMs;
            this.readTimeoutMs = readTimeoutMs;
            this.userAgent = userAgent;
        }

        private static RequestConfig from(StickyPlayerRequest request) {
            return new RequestConfig(
                    request.getVastTagUrl(),
                    request.getConnectTimeoutMs(),
                    request.getReadTimeoutMs(),
                    request.getUserAgent());
        }

        private static RequestConfig from(BackgroundAudioRequest request) {
            return new RequestConfig(
                    request.getVastTagUrl(),
                    request.getConnectTimeoutMs(),
                    request.getReadTimeoutMs(),
                    request.getUserAgent());
        }

        private static RequestConfig from(FullscreenInterstitialRequest request) {
            return new RequestConfig(
                    request.getVastTagUrl(),
                    request.getConnectTimeoutMs(),
                    request.getReadTimeoutMs(),
                    request.getUserAgent());
        }
    }
}
