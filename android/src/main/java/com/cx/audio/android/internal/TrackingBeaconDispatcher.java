package com.cx.audio.android.internal;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TrackingBeaconDispatcher {
    private static final String TAG = "CxAudioTracking";
    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final int READ_TIMEOUT_MS = 2000;
    private static final TrackingBeaconDispatcher DEFAULT =
            new TrackingBeaconDispatcher(Executors.newCachedThreadPool(), new HttpBeaconSender());

    private final Executor executor;
    private final BeaconSender sender;

    public static TrackingBeaconDispatcher getDefault() {
        return DEFAULT;
    }

    TrackingBeaconDispatcher(Executor executor, BeaconSender sender) {
        this.executor = executor;
        this.sender = sender;
    }

    void dispatch(TrackingEvent event, String adIdentifier, List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }

        for (String urlString : urls) {
            URL url = validHttpUrl(urlString);
            String fingerprint = fingerprint(urlString);
            if (url == null) {
                debug("event=" + event.getLogName()
                        + " ad=" + safeAdIdentifier(adIdentifier)
                        + " target=" + fingerprint
                        + " result=skipped error=MalformedURLException");
                continue;
            }

            executor.execute(() -> send(event, adIdentifier, url, fingerprint));
        }
    }

    private void send(
            TrackingEvent event,
            String adIdentifier,
            URL url,
            String fingerprint) {
        try {
            sender.send(event, adIdentifier, url);
            debug("event=" + event.getLogName()
                    + " ad=" + safeAdIdentifier(adIdentifier)
                    + " target=" + fingerprint
                    + " result=success");
        } catch (Throwable error) {
            debug("event=" + event.getLogName()
                    + " ad=" + safeAdIdentifier(adIdentifier)
                    + " target=" + fingerprint
                    + " result=failure error=" + error.getClass().getSimpleName());
        }
    }

    static void logEventFired(TrackingEvent event, String adIdentifier, int beaconCount) {
        debug("event=" + event.getLogName()
                + " ad=" + safeAdIdentifier(adIdentifier)
                + " result=fired beacons=" + beaconCount);
    }

    private static void debug(String message) {
        try {
            Log.d(TAG, message);
        } catch (RuntimeException ignored) {
            // Android local unit tests use a stubbed android.jar where Log can throw.
        }
    }

    private static URL validHttpUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return null;
        }

        try {
            URL url = new URL(urlString.trim());
            String protocol = url.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                return null;
            }
            return url;
        } catch (MalformedURLException error) {
            return null;
        }
    }

    private static String safeAdIdentifier(String adIdentifier) {
        if (adIdentifier == null || adIdentifier.trim().isEmpty()) {
            return "unknown";
        }
        return adIdentifier.trim();
    }

    private static String fingerprint(String value) {
        if (value == null) {
            return "null";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes("UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < Math.min(6, bytes.length); i++) {
                builder.append(String.format(Locale.US, "%02x", bytes[i] & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException | IOException error) {
            return Integer.toHexString(value.hashCode());
        }
    }

    interface BeaconSender {
        void send(TrackingEvent event, String adIdentifier, URL url) throws IOException;
    }

    private static final class HttpBeaconSender implements BeaconSender {
        @Override
        public void send(TrackingEvent event, String adIdentifier, URL url) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            try {
                int statusCode = connection.getResponseCode();
                if (statusCode < 200 || statusCode >= 400) {
                    throw new IOException("HTTP " + statusCode);
                }
                try (InputStream inputStream = connection.getInputStream()) {
                    byte[] buffer = new byte[256];
                    while (inputStream.read(buffer) != -1) {
                        // Drain the response so the connection can be reused.
                    }
                }
            } finally {
                connection.disconnect();
            }
        }
    }
}
