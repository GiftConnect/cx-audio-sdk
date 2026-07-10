package com.cx.audio.android.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class VastTracking {
    private static final VastTracking EMPTY = new VastTracking(
            null,
            Collections.emptyList(),
            Collections.emptyMap(),
            Collections.emptyList());

    private final String adIdentifier;
    private final List<String> impressionUrls;
    private final Map<TrackingEvent, List<String>> eventUrls;
    private final List<String> clickTrackingUrls;

    VastTracking(
            String adIdentifier,
            List<String> impressionUrls,
            Map<TrackingEvent, List<String>> eventUrls,
            List<String> clickTrackingUrls) {
        this.adIdentifier = normalize(adIdentifier);
        this.impressionUrls = immutableCopy(impressionUrls);
        this.eventUrls = immutableEventMap(eventUrls);
        this.clickTrackingUrls = immutableCopy(clickTrackingUrls);
    }

    public static VastTracking empty() {
        return EMPTY;
    }

    public String getAdIdentifier() {
        return adIdentifier;
    }

    public List<String> getImpressionUrls() {
        return impressionUrls;
    }

    public List<String> getUrls(TrackingEvent event) {
        if (event == TrackingEvent.IMPRESSION) {
            return impressionUrls;
        }
        if (event == TrackingEvent.CLICK) {
            return clickTrackingUrls;
        }
        List<String> urls = eventUrls.get(event);
        return urls == null ? Collections.emptyList() : urls;
    }

    public List<String> getClickTrackingUrls() {
        return clickTrackingUrls;
    }

    VastTracking merge(VastTracking other) {
        if (other == null || other == EMPTY) {
            return this;
        }
        if (this == EMPTY) {
            return other;
        }

        Map<TrackingEvent, List<String>> mergedEvents = new EnumMap<>(TrackingEvent.class);
        for (TrackingEvent event : TrackingEvent.values()) {
            if (event == TrackingEvent.IMPRESSION || event == TrackingEvent.CLICK) {
                continue;
            }
            List<String> urls = new ArrayList<>();
            urls.addAll(getUrls(event));
            urls.addAll(other.getUrls(event));
            if (!urls.isEmpty()) {
                mergedEvents.put(event, urls);
            }
        }

        List<String> impressions = new ArrayList<>(impressionUrls);
        impressions.addAll(other.impressionUrls);

        List<String> clickTracking = new ArrayList<>(clickTrackingUrls);
        clickTracking.addAll(other.clickTrackingUrls);

        return new VastTracking(
                adIdentifier == null ? other.adIdentifier : adIdentifier,
                impressions,
                mergedEvents,
                clickTracking);
    }

    VastTracking resolve(URL baseUrl) {
        Map<TrackingEvent, List<String>> resolvedEvents = new EnumMap<>(TrackingEvent.class);
        for (Map.Entry<TrackingEvent, List<String>> entry : eventUrls.entrySet()) {
            List<String> resolved = resolveUrls(baseUrl, entry.getValue());
            if (!resolved.isEmpty()) {
                resolvedEvents.put(entry.getKey(), resolved);
            }
        }

        return new VastTracking(
                adIdentifier,
                resolveUrls(baseUrl, impressionUrls),
                resolvedEvents,
                resolveUrls(baseUrl, clickTrackingUrls));
    }

    private static List<String> resolveUrls(URL baseUrl, List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> resolved = new ArrayList<>();
        for (String url : urls) {
            String normalized = normalize(url);
            if (normalized == null) {
                continue;
            }
            try {
                resolved.add(new URL(baseUrl, normalized).toString());
            } catch (MalformedURLException ignored) {
                // Malformed tracking URLs must not make the ad unloadable.
            }
        }
        return resolved;
    }

    private static List<String> immutableCopy(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> copy = new ArrayList<>();
        for (String url : urls) {
            String normalized = normalize(url);
            if (normalized != null) {
                copy.add(normalized);
            }
        }
        return copy.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(copy);
    }

    private static Map<TrackingEvent, List<String>> immutableEventMap(
            Map<TrackingEvent, List<String>> urls) {
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<TrackingEvent, List<String>> copy = new EnumMap<>(TrackingEvent.class);
        for (Map.Entry<TrackingEvent, List<String>> entry : urls.entrySet()) {
            if (entry.getKey() == null
                    || entry.getKey() == TrackingEvent.IMPRESSION
                    || entry.getKey() == TrackingEvent.CLICK) {
                continue;
            }
            List<String> values = immutableCopy(entry.getValue());
            if (!values.isEmpty()) {
                copy.put(entry.getKey(), values);
            }
        }
        return copy.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(copy);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
