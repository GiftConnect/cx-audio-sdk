package com.cx.audio.android.internal;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

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
        URL currentUrl = request.getVastTagUrl();
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

    public static Bitmap loadBitmap(String imageUrl, StickyPlayerRequest request) throws IOException {
        HttpURLConnection connection = openConnection(new URL(imageUrl), request);
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

    public static void openClickThrough(Context context, String clickThroughUrl) {
        if (clickThroughUrl == null || clickThroughUrl.trim().isEmpty()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickThroughUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static String readUrl(URL url, StickyPlayerRequest request, int maxBytes) throws IOException {
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

    private static HttpURLConnection openConnection(URL url, StickyPlayerRequest request) throws IOException {
        URLConnection rawConnection = url.openConnection();
        if (!(rawConnection instanceof HttpURLConnection)) {
            throw new IOException("Unsupported URL protocol: " + url.getProtocol());
        }
        HttpURLConnection connection = (HttpURLConnection) rawConnection;
        connection.setConnectTimeout(request.getConnectTimeoutMs());
        connection.setReadTimeout(request.getReadTimeoutMs());
        connection.setRequestProperty("User-Agent", request.getUserAgent());
        connection.setInstanceFollowRedirects(true);
        return connection;
    }

    private static void requireSuccessfulResponse(HttpURLConnection connection) throws IOException {
        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("HTTP " + statusCode + " loading " + connection.getURL());
        }
    }
}
