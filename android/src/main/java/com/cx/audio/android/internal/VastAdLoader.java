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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public final class VastAdLoader {
    private static final int MAX_VAST_BYTES = 1024 * 1024;
    private static final int MAX_CREATIVE_BYTES = 100 * 1024 * 1024;
    private static final int MAX_WRAPPER_DEPTH = 5;
    private static final VastParser VAST_PARSER = new VastParser();

    private VastAdLoader() {
    }

    public static VastCompanion loadCompanion(StickyPlayerRequest request) throws Exception {
        return loadCompanion(RequestConfig.from(request));
    }

    public static VastStickyPlayerAd loadStickyPlayerAd(StickyPlayerRequest request) throws Exception {
        return loadStickyPlayerAd(RequestConfig.from(request));
    }

    public static VastAudioAd loadAudioAd(BackgroundAudioRequest request) throws Exception {
        return loadAudioAd(RequestConfig.from(request));
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

    public static File downloadMedia(Context context, String mediaUrl, BackgroundAudioRequest request)
            throws IOException {
        return downloadMedia(context, mediaUrl, RequestConfig.from(request));
    }

    public static File downloadMedia(Context context, String mediaUrl, StickyPlayerRequest request)
            throws IOException {
        return downloadMedia(context, mediaUrl, RequestConfig.from(request));
    }

    public static File downloadMedia(Context context, String mediaUrl, FullscreenInterstitialRequest request)
            throws IOException {
        return downloadMedia(context, mediaUrl, RequestConfig.from(request));
    }

    private static File downloadMedia(Context context, String mediaUrl, RequestConfig request)
            throws IOException {
        URL source = PrivacyEnforcer.enforce(new URL(mediaUrl), PrivacyRequestPurpose.CREATIVE_ASSET);
        HttpURLConnection connection = openConnection(source, request, PrivacyRequestPurpose.CREATIVE_ASSET);
        requireSuccessfulResponse(connection);
        File destination = File.createTempFile("cx-audio-", suffix(source), context.getCacheDir());
        boolean complete = false;
        try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_CREATIVE_BYTES) throw new IOException("Creative exceeded maximum size");
                output.write(buffer, 0, read);
            }
            complete = true;
            return destination;
        } finally {
            connection.disconnect();
            if (!complete) destination.delete();
        }
    }

    private static String suffix(URL source) {
        String path = source.getPath();
        int dot = path.lastIndexOf('.');
        String suffix = dot >= 0 ? path.substring(dot) : ".media";
        return suffix.length() > 10 ? ".media" : suffix;
    }

    public static void openClickThrough(Context context, String clickThroughUrl) {
        if (clickThroughUrl == null || clickThroughUrl.trim().isEmpty()) {
            return;
        }
        final URL enforced;
        try {
            enforced = PrivacyEnforcer.enforce(new URL(clickThroughUrl), PrivacyRequestPurpose.CLICK_THROUGH);
        } catch (Exception blocked) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(enforced.toString()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static VastCompanion loadCompanion(RequestConfig request) throws Exception {
        URL currentUrl = request.vastTagUrl;
        VastTracking tracking = VastTracking.empty();
        for (int depth = 0; depth <= MAX_WRAPPER_DEPTH; depth++) {
            String vastXml = readUrl(currentUrl, request, MAX_VAST_BYTES);
            tracking = tracking.merge(parseTracking(vastXml, currentUrl));
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

    private static VastStickyPlayerAd loadStickyPlayerAd(RequestConfig request) throws Exception {
        URL currentUrl = request.vastTagUrl;
        VastTracking tracking = VastTracking.empty();
        for (int depth = 0; depth <= MAX_WRAPPER_DEPTH; depth++) {
            String vastXml = readUrl(currentUrl, request, MAX_VAST_BYTES);
            tracking = tracking.merge(parseTracking(vastXml, currentUrl));
            VastMediaFile mediaFile = parseOptionalAudioMediaFile(vastXml, currentUrl);
            VastCompanion companion = parseOptionalCompanion(vastXml, currentUrl);
            if (mediaFile != null && companion != null) {
                return new VastStickyPlayerAd(mediaFile, companion, tracking);
            }

            String wrapperUrl = VAST_PARSER.parseWrapperUrl(vastXml);
            if (wrapperUrl == null) {
                throw new IllegalArgumentException(
                        "VAST response did not include both audio media and an image companion");
            }
            currentUrl = new URL(currentUrl, wrapperUrl);
        }
        throw new IOException("VAST wrapper depth exceeded " + MAX_WRAPPER_DEPTH);
    }

    private static VastAudioAd loadAudioAd(RequestConfig request) throws Exception {
        URL currentUrl = request.vastTagUrl;
        VastTracking tracking = VastTracking.empty();
        for (int depth = 0; depth <= MAX_WRAPPER_DEPTH; depth++) {
            String vastXml = readUrl(currentUrl, request, MAX_VAST_BYTES);
            tracking = tracking.merge(parseTracking(vastXml, currentUrl));
            try {
                VastMediaFile mediaFile = VAST_PARSER.parseBestAudioMediaFile(vastXml);
                return new VastAudioAd(
                        mediaFile.withUrl(new URL(currentUrl, mediaFile.getUrl()).toString()),
                        tracking);
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
        VastTracking tracking = VastTracking.empty();
        for (int depth = 0; depth <= MAX_WRAPPER_DEPTH; depth++) {
            String vastXml = readUrl(currentUrl, request, MAX_VAST_BYTES);
            tracking = tracking.merge(parseTracking(vastXml, currentUrl));
            VastMediaFile mediaFile = parseOptionalInterstitialMediaFile(vastXml, currentUrl);
            VastMediaFile audioMediaFile = parseOptionalAudioMediaFile(vastXml, currentUrl);
            VastCompanion companion = parseOptionalCompanion(vastXml, currentUrl);
            if (mediaFile != null || audioMediaFile != null || companion != null) {
                return new VastInterstitialAd(mediaFile, audioMediaFile, companion, tracking);
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

    private static VastMediaFile parseOptionalAudioMediaFile(String vastXml, URL baseUrl)
            throws Exception {
        try {
            VastMediaFile mediaFile = VAST_PARSER.parseBestAudioMediaFile(vastXml);
            return mediaFile.withUrl(new URL(baseUrl, mediaFile.getUrl()).toString());
        } catch (IllegalArgumentException noMediaFile) {
            return null;
        }
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

    private static VastTracking parseTracking(String vastXml, URL baseUrl) {
        try {
            return VAST_PARSER.parseTracking(vastXml).resolve(baseUrl);
        } catch (Exception ignored) {
            return VastTracking.empty();
        }
    }

    private static String readUrl(URL url, RequestConfig request, int maxBytes) throws IOException {
        HttpURLConnection connection = openConnection(url, request, PrivacyRequestPurpose.VAST_WRAPPER);
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
        return openConnection(url, request, PrivacyRequestPurpose.CREATIVE_ASSET);
    }

    private static HttpURLConnection openConnection(
            URL url, RequestConfig request, PrivacyRequestPurpose purpose) throws IOException {
        URL current = url;
        for (int redirects = 0; redirects <= 5; redirects++) {
            URL enforcedUrl = PrivacyEnforcer.enforce(current, purpose);
            URLConnection rawConnection = enforcedUrl.openConnection();
            if (!(rawConnection instanceof HttpURLConnection)) {
                throw new IOException("Unsupported URL protocol: " + current.getProtocol());
            }
            HttpURLConnection connection = (HttpURLConnection) rawConnection;
            connection.setConnectTimeout(request.connectTimeoutMs);
            connection.setReadTimeout(request.readTimeoutMs);
            connection.setRequestProperty("User-Agent", request.userAgent);
            connection.setInstanceFollowRedirects(false);
            int status = connection.getResponseCode();
            if (status < 300 || status >= 400) return connection;
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null || location.trim().isEmpty()) throw new IOException("Redirect missing Location");
            current = new URL(enforcedUrl, location);
        }
        throw new IOException("HTTP redirect depth exceeded");
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
