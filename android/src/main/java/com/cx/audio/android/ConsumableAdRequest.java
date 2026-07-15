package com.cx.audio.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConsumableAdRequest {
    private static final String DEFAULT_ENDPOINT = "https://2.e.serverbid.com/mpb/get";

    private final LinkedHashMap<String, String> parameters;

    private ConsumableAdRequest(Builder builder) {
        this.parameters = new LinkedHashMap<>(builder.parameters);
        parameters.put("cb", defaultCacheBuster());
    }

    URL toURL() {
        try {
            return new URL(toURLString());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Consumable request produced an invalid URL", e);
        }
    }

    String toURLString() {
        return toURLString(null);
    }

    String toURLString(PlacementType placementType) {
        return toURLString(placementType, null);
    }

    String toURLString(PlacementType placementType, Context context) {
        return toURLString(placementType, context, null);
    }

    String toURLString(
            PlacementType placementType,
            Context context,
            String userAgentParameter) {
        LinkedHashMap<String, String> params = new LinkedHashMap<>(parameters);
        applyAutomaticParameters(params, placementType, context, userAgentParameter);

        StringBuilder builder = new StringBuilder(DEFAULT_ENDPOINT);
        builder.append(DEFAULT_ENDPOINT.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().trim().isEmpty()) {
                continue;
            }
            if (!first) {
                builder.append("&");
            }
            builder.append(encode(entry.getKey()));
            builder.append("=");
            builder.append(encode(entry.getValue()));
            first = false;
        }
        return builder.toString();
    }

    private static void applyAutomaticParameters(
            LinkedHashMap<String, String> parameters,
            PlacementType placementType,
            Context context,
            String userAgentParameter) {
        putAutomatic(parameters, "u", userAgentParameter);
        putAutomatic(parameters, "locale", Locale.getDefault().toLanguageTag());
        if (context != null) {
            putAutomatic(parameters, "appbundle", context.getPackageName());
            putAutomatic(parameters, "appname", applicationName(context));
        }
        putAutomatic(parameters, "v_delivery", "2");
        putAutomatic(parameters, "v_delay", "-1");
        putAutomatic(parameters, "v_pm", "1");
        putAutomatic(parameters, "v_max_dur", "90");
        putAutomatic(parameters, "v_mimes",
                "audio/mpeg,audio/mp4,audio/aac,audio/ogg,audio/wav,audio/x-wav,"
                        + "video/mp4,image/jpeg,image/png,image/gif,image/webp");
        putAutomatic(parameters, "v_cmp_types", "1");
        putAutomatic(parameters, "v_cmp_mimes", "image/jpeg,image/png,image/gif,image/webp");
        putAutomatic(parameters, "v_cmp_sizes", "300x250");
        putAutomatic(parameters, "v_protocols", "2,3,5,6,7,8");
        putAutomatic(parameters, "c_audio_feed_type", "6");

        if (placementType == PlacementType.FULLSCREEN_INTERSTITIAL) {
            putAutomatic(parameters, "sdkpt", "3");
            putAutomatic(parameters, "v_plcmt", "3");
            putAutomatic(parameters, "v_linearity", "1");
            putAutomatic(parameters, "v_min_bitrate", "64");
            putAutomatic(parameters, "v_max_bitrate", "2500");
        } else if (placementType == PlacementType.STICKY_PLAYER) {
            putAutomatic(parameters, "sdkpt", "2");
            putAutomatic(parameters, "v_plcmt", "2");
            putAutomatic(parameters, "v_linearity", "2");
            putAutomatic(parameters, "v_min_bitrate", "64");
            putAutomatic(parameters, "v_max_bitrate", "320");
        } else if (placementType == PlacementType.BACKGROUND_AUDIO) {
            putAutomatic(parameters, "sdkpt", "1");
            putAutomatic(parameters, "v_plcmt", "2");
            putAutomatic(parameters, "v_linearity", "2");
            putAutomatic(parameters, "v_min_bitrate", "64");
            putAutomatic(parameters, "v_max_bitrate", "320");
        } else {
            putAutomatic(parameters, "v_min_bitrate", "64");
            putAutomatic(parameters, "v_max_bitrate", "320");
        }
    }

    private static void putAutomatic(LinkedHashMap<String, String> parameters, String name, String value) {
        if (!parameters.containsKey(name) && value != null && !value.trim().isEmpty()) {
            parameters.put(name, value);
        }
    }

    private static String applicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        CharSequence label = context.getPackageManager().getApplicationLabel(applicationInfo);
        return label == null ? null : label.toString();
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is unavailable", e);
        }
    }

    private static String defaultCacheBuster() {
        return String.valueOf(System.currentTimeMillis());
    }

    public static Builder builder(String placementHash) {
        return new Builder(placementHash);
    }

    enum PlacementType {
        BACKGROUND_AUDIO,
        STICKY_PLAYER,
        FULLSCREEN_INTERSTITIAL
    }

    public static final class Builder {
        private final LinkedHashMap<String, String> parameters = new LinkedHashMap<>();

        private Builder(String placementHash) {
            putRequired("s", placementHash, "placementHash");
            parameter("rt", "vast");
            parameter("secure", true);
        }

        public Builder ipAddress(String ipAddress) {
            return parameter("i", ipAddress);
        }

        public Builder locale(String locale) {
            return parameter("locale", locale);
        }

        public Builder contentCategories(String... categories) {
            return parameter("cat", commaJoin(categories));
        }

        public Builder blockedCategories(String... categories) {
            return parameter("bcat", commaJoin(categories));
        }

        public Builder blockedAdvertisers(String... domains) {
            return parameter("badv", commaJoin(domains));
        }

        public Builder limitAdTracking(boolean value) {
            return parameter("lmt", value);
        }

        public Builder doNotTrack(boolean value) {
            return parameter("dnt", value);
        }

        public Builder appStoreUrl(String appStoreUrl) {
            return parameter("appstoreurl", appStoreUrl);
        }

        public Builder appName(String appName) {
            return parameter("appname", appName);
        }

        public Builder appVersion(String appVersion) {
            return parameter("appversion", appVersion);
        }

        public Builder appBundle(String appBundle) {
            return parameter("appbundle", appBundle);
        }

        public Builder idfa(String idfa) {
            return parameter("idfa", idfa);
        }

        public Builder aid(String aid) {
            return parameter("aid", aid);
        }

        public Builder gadid(String gadid) {
            return parameter("gadid", gadid);
        }

        public Builder ifa(String ifa) {
            return parameter("ifa", ifa);
        }

        public Builder keywords(String... keywords) {
            return parameter("kw", commaJoin(keywords));
        }

        public Builder longitude(double longitude) {
            return parameter("long", formatNumber(longitude));
        }

        public Builder latitude(double latitude) {
            return parameter("lat", formatNumber(latitude));
        }

        public Builder coppa(boolean value) {
            return parameter("coppa", value);
        }

        public Builder gdpr(boolean value) {
            return parameter("gdpr", value);
        }

        public Builder gdprConsent(String consent) {
            return parameter("gdpr_consent", consent);
        }

        public Builder usPrivacy(String usPrivacy) {
            return parameter("us_privacy", usPrivacy);
        }

        public Builder gpp(String gpp) {
            return parameter("gpp", gpp);
        }

        public Builder gppSectionIds(String... sectionIds) {
            return parameter("gpp_sid", commaJoin(sectionIds));
        }

        public Builder contentId(String contentId) {
            return parameter("c_id", contentId);
        }

        public Builder contentGenre(String... genres) {
            return parameter("c_genre", commaJoin(genres));
        }

        public Builder contentTitle(String title) {
            return parameter("c_title", title);
        }

        public Builder contentDurationSeconds(int seconds) {
            return parameter("c_dur", String.valueOf(seconds));
        }

        public Builder contentUrl(String url) {
            return parameter("c_url", url);
        }

        public ConsumableAdRequest build() {
            return new ConsumableAdRequest(this);
        }

        private Builder parameter(String name, boolean value) {
            return parameter(name, value ? "1" : "0");
        }

        private Builder parameter(String name, String value) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("parameter name must not be blank");
            }
            if (value == null || value.trim().isEmpty()) {
                parameters.remove(name);
            } else {
                parameters.put(name, value);
            }
            return this;
        }

        private void putRequired(String name, String value, String label) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(label + " must not be blank");
            }
            parameters.put(name, value);
        }

        private static String commaJoin(String... values) {
            if (values == null || values.length == 0) {
                return null;
            }
            List<String> cleaned = new ArrayList<>();
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    cleaned.add(value);
                }
            }
            if (cleaned.isEmpty()) {
                return null;
            }
            return String.join(",", cleaned);
        }

        private static String formatNumber(double value) {
            return String.format(Locale.US, "%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
    }
}
