package com.cx.audio.android.internal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PerAdTrackingState {
    private final VastTracking tracking;
    private final TrackingBeaconDispatcher dispatcher;
    private final Set<TrackingEvent> firedEvents = EnumSet.noneOf(TrackingEvent.class);

    public PerAdTrackingState(VastTracking tracking) {
        this(tracking, TrackingBeaconDispatcher.getDefault());
    }

    PerAdTrackingState(VastTracking tracking, TrackingBeaconDispatcher dispatcher) {
        this.tracking = tracking == null ? VastTracking.empty() : tracking;
        this.dispatcher = dispatcher;
    }

    public void fireImpression() {
        fireOnce(TrackingEvent.IMPRESSION, tracking.getImpressionUrls());
    }

    public void fireStart() {
        fireOnce(TrackingEvent.START, tracking.getUrls(TrackingEvent.START));
    }

    public void fireProgress(long currentPositionMs, long durationMs) {
        if (durationMs <= 0 || currentPositionMs < 0) {
            return;
        }

        double progress = Math.min(1.0d, currentPositionMs / (double) durationMs);
        List<TrackingEvent> events = new ArrayList<>(4);
        if (progress >= 0.25d) {
            events.add(TrackingEvent.FIRST_QUARTILE);
        }
        if (progress >= 0.50d) {
            events.add(TrackingEvent.MIDPOINT);
        }
        if (progress >= 0.75d) {
            events.add(TrackingEvent.THIRD_QUARTILE);
        }
        if (progress >= 1.0d) {
            events.add(TrackingEvent.COMPLETE);
        }

        for (TrackingEvent event : events) {
            fireOnce(event, tracking.getUrls(event));
        }
    }

    public void fireComplete() {
        fireOnce(TrackingEvent.FIRST_QUARTILE, tracking.getUrls(TrackingEvent.FIRST_QUARTILE));
        fireOnce(TrackingEvent.MIDPOINT, tracking.getUrls(TrackingEvent.MIDPOINT));
        fireOnce(TrackingEvent.THIRD_QUARTILE, tracking.getUrls(TrackingEvent.THIRD_QUARTILE));
        fireOnce(TrackingEvent.COMPLETE, tracking.getUrls(TrackingEvent.COMPLETE));
    }

    public void fireClick() {
        fireOnce(TrackingEvent.CLICK, tracking.getClickTrackingUrls());
    }

    private void fireOnce(TrackingEvent event, List<String> urls) {
        List<String> dedupedUrls;
        synchronized (this) {
            if (firedEvents.contains(event)) {
                return;
            }
            firedEvents.add(event);
            dedupedUrls = dedupe(urls);
        }

        TrackingBeaconDispatcher.logEventFired(
                event,
                tracking.getAdIdentifier(),
                dedupedUrls == null ? 0 : dedupedUrls.size());
        dispatcher.dispatch(event, tracking.getAdIdentifier(), dedupedUrls);
    }

    private static List<String> dedupe(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return urls;
        }

        Set<String> deduped = new LinkedHashSet<>();
        for (String url : urls) {
            if (url == null) {
                continue;
            }
            String normalized = url.trim();
            if (!normalized.isEmpty()) {
                deduped.add(normalized);
            }
        }
        return new ArrayList<>(deduped);
    }
}
