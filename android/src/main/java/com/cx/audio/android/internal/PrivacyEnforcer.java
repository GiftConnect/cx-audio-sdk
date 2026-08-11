package com.cx.audio.android.internal;

import com.cx.audio.android.CxAgeTreatment;
import com.cx.audio.android.CxPrivacyState;

import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URL;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class PrivacyEnforcer {
    private static final Set<String> IDENTIFIERS = new HashSet<>(Arrays.asList(
            "idfa", "ifa", "gadid", "aid", "buyeruid", "buyer_id", "user_id", "userid"));
    private static final Set<String> LOCATION = new HashSet<>(Arrays.asList(
            "lat", "long", "lon", "latitude", "longitude", "i", "ip"));
    private static final Set<String> CONTROLLED = new HashSet<>(Arrays.asList(
            "lmt", "dnt", "coppa", "gdpr", "gdpr_consent", "us_privacy", "gpp", "gpp_sid"));
    private PrivacyEnforcer() {}

    public static URL enforce(URL url, PrivacyRequestPurpose purpose) throws MalformedURLException {
        return enforce(url, purpose, PrivacyManager.getInstance().getSnapshot());
    }

    static URL enforce(URL url, PrivacyRequestPurpose purpose, CxPrivacyState state)
            throws MalformedURLException {
        if (url == null || !"https".equalsIgnoreCase(url.getProtocol()))
            throw new MalformedURLException("Outbound SDK requests must use HTTPS");
        List<QueryItem> items = parseQuery(url.getQuery());
        List<QueryItem> output = new ArrayList<>();
        for (QueryItem item : items) {
            String lower = item.name.toLowerCase(java.util.Locale.US);
            if (CONTROLLED.contains(lower)) continue;
            if (!state.isDeviceIdentifiersAllowed() && IDENTIFIERS.contains(lower)) continue;
            if (!state.isPreciseLocationAllowed() && LOCATION.contains(lower)) continue;
            output.add(new QueryItem(item.name, macros(item.value, state)));
        }
        if (purpose == PrivacyRequestPurpose.AD_REQUEST || purpose == PrivacyRequestPurpose.VAST_WRAPPER
                || purpose == PrivacyRequestPurpose.TRACKING_BEACON) {
            replace(output, "lmt", state.isDeviceIdentifiersAllowed() ? "0" : "1");
            replace(output, "dnt", state.isPersonalizedAdsAllowed() ? "0" : "1");
            if (state.getAgeTreatment() == CxAgeTreatment.CHILD_DIRECTED
                    || state.getAgeTreatment() == CxAgeTreatment.UNDER_AGE_OF_CONSENT)
                replace(output, "coppa", "1");
            if (state.getGdprApplies() != null) replace(output, "gdpr", state.getGdprApplies() ? "1" : "0");
            append(output, "gdpr_consent", state.getTcfString());
            append(output, "us_privacy", state.getUsPrivacyString());
            append(output, "gpp", state.getGppString());
            if (!state.getGppSectionIds().isEmpty()) append(output, "gpp_sid", join(state.getGppSectionIds()));
        }
        StringBuilder result = new StringBuilder();
        result.append(url.getProtocol()).append("://").append(url.getAuthority()).append(url.getPath());
        if (!output.isEmpty()) {
            result.append('?');
            for (int index = 0; index < output.size(); index++) {
                if (index > 0) result.append('&');
                QueryItem item = output.get(index);
                result.append(encode(item.name));
                if (item.value != null) result.append('=').append(encode(item.value));
            }
        }
        if (url.getRef() != null) result.append('#').append(url.getRef());
        return new URL(result.toString());
    }

    private static void replace(List<QueryItem> items, String name, String value) {
        Iterator<QueryItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().name.equalsIgnoreCase(name)) {
                iterator.remove();
            }
        }
        items.add(new QueryItem(name, value));
    }
    private static void append(List<QueryItem> items, String name, String value) {
        if (value != null && !value.trim().isEmpty()) items.add(new QueryItem(name, value));
    }
    private static String macros(String value, CxPrivacyState state) {
        if (value == null) return null;
        return value.replace("[GDPR]", Boolean.TRUE.equals(state.getGdprApplies()) ? "1" : "0")
                .replace("[GDPR_CONSENT]", value(state.getTcfString()))
                .replace("[US_PRIVACY]", value(state.getUsPrivacyString()))
                .replace("[GPP_STRING_OMID]", value(state.getGppString()))
                .replace("[GPP_SID]", join(state.getGppSectionIds()));
    }
    private static String value(String value) { return value == null ? "" : value; }
    private static String join(List<Integer> values) {
        StringBuilder result = new StringBuilder();
        for (Integer value : values) { if (result.length() > 0) result.append(','); result.append(value); }
        return result.toString();
    }

    private static List<QueryItem> parseQuery(String query) {
        List<QueryItem> items = new ArrayList<>();
        if (query == null || query.isEmpty()) return items;
        for (String part : query.split("&", -1)) {
            int equals = part.indexOf('=');
            items.add(new QueryItem(
                    decode(equals < 0 ? part : part.substring(0, equals)),
                    equals < 0 ? null : decode(part.substring(equals + 1))));
        }
        return items;
    }

    private static String encode(String value) {
        try { return URLEncoder.encode(value, "UTF-8").replace("+", "%20"); }
        catch (UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
    }
    private static String decode(String value) {
        try { return URLDecoder.decode(value, "UTF-8"); }
        catch (UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
    }
    private static final class QueryItem {
        private final String name;
        private final String value;
        private QueryItem(String name, String value) { this.name = name; this.value = value; }
    }
}
