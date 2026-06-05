package com.cx.audio.android;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ConsumableAdRequest {
    private static final String DEFAULT_ENDPOINT = "https://2.e.serverbid.com/mpb/get";

    private final String endpointUrl;
    private final LinkedHashMap<String, String> parameters;

    private ConsumableAdRequest(Builder builder) {
        this.endpointUrl = builder.endpointUrl;
        this.parameters = new LinkedHashMap<>(builder.parameters);
        if (!parameters.containsKey("cb")) {
            parameters.put("cb", UUID.randomUUID().toString());
        }
    }

    public URL toURL() {
        try {
            return new URL(toURLString());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Consumable request produced an invalid URL", e);
        }
    }

    public String toURLString() {
        StringBuilder builder = new StringBuilder(endpointUrl);
        builder.append(endpointUrl.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
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

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is unavailable", e);
        }
    }

    public static Builder builder(String placementHash) {
        return new Builder(placementHash);
    }

    public static final class Builder {
        private String endpointUrl = DEFAULT_ENDPOINT;
        private final LinkedHashMap<String, String> parameters = new LinkedHashMap<>();

        private Builder(String placementHash) {
            putRequired("s", placementHash, "placementHash");
            parameter("rt", "vast");
            parameter("secure", true);
        }

        public Builder endpointUrl(String endpointUrl) {
            if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("endpointUrl must not be blank");
            }
            try {
                URL url = new URL(endpointUrl);
                String protocol = url.getProtocol();
                if (!"http".equals(protocol) && !"https".equals(protocol)) {
                    throw new IllegalArgumentException("endpointUrl must use HTTP or HTTPS");
                }
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("endpointUrl must be a valid URL", e);
            }
            this.endpointUrl = endpointUrl;
            return this;
        }

        public Builder responseType(String responseType) {
            return parameter("rt", responseType);
        }

        public Builder userAgentParameter(String userAgent) {
            return parameter("u", userAgent);
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

        public Builder secure(boolean value) {
            return parameter("secure", value);
        }

        public Builder supplyChain(String supplyChain) {
            return parameter("schain", supplyChain);
        }

        public Builder cacheBuster(String cacheBuster) {
            return parameter("cb", cacheBuster);
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

        public Builder playerSize(int width, int height) {
            parameter("v_w", String.valueOf(width));
            return parameter("v_h", String.valueOf(height));
        }

        public Builder videoPlacement(int placement) {
            return parameter("v_plcmt", String.valueOf(placement));
        }

        public Builder delivery(int delivery) {
            return parameter("v_delivery", String.valueOf(delivery));
        }

        public Builder startDelaySeconds(int seconds) {
            parameters.remove("v_position");
            return parameter("v_delay", String.valueOf(seconds));
        }

        public Builder position(String position) {
            parameters.remove("v_delay");
            return parameter("v_position", position);
        }

        public Builder maxDurationSeconds(int seconds) {
            return parameter("v_max_dur", String.valueOf(seconds));
        }

        public Builder mediaMimes(String... mimes) {
            return parameter("v_mimes", commaJoin(mimes));
        }

        public Builder skippable(boolean skippable) {
            return parameter("v_skip", skippable);
        }

        public Builder playbackMethod(int playbackMethod) {
            return parameter("v_pm", String.valueOf(playbackMethod));
        }

        public Builder maxBitrateKbps(int bitrate) {
            return parameter("v_max_bitrate", String.valueOf(bitrate));
        }

        public Builder minBitrateKbps(int bitrate) {
            return parameter("v_min_bitrate", String.valueOf(bitrate));
        }

        public Builder companionTypes(String... types) {
            return parameter("v_cmp_types", commaJoin(types));
        }

        public Builder companionMimes(String... mimes) {
            return parameter("v_cmp_mimes", commaJoin(mimes));
        }

        public Builder companionSizes(String... sizes) {
            return parameter("v_cmp_sizes", commaJoin(sizes));
        }

        public Builder protocols(String... protocols) {
            return parameter("v_protocols", commaJoin(protocols));
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

        public Builder contentAudioFeedType(int feedType) {
            return parameter("c_audio_feed_type", String.valueOf(feedType));
        }

        public Builder customParameter(String name, String value) {
            return parameter(name, value);
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
