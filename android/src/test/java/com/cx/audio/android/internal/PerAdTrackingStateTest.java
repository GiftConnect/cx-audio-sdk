package com.cx.audio.android.internal;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public final class PerAdTrackingStateTest {
    @Test
    public void impressionFiresAllValidUrlsOnce() {
        RecordingSender sender = new RecordingSender();
        PerAdTrackingState state = state(
                tracking(
                        Arrays.asList(
                                "https://track.example.com/imp1",
                                "https://track.example.com/imp2",
                                "http://[malformed"),
                        Collections.emptyMap(),
                        Collections.emptyList()),
                sender);

        state.fireImpression();
        state.fireImpression();

        assertEquals(
                Arrays.asList("https://track.example.com/imp1", "https://track.example.com/imp2"),
                sender.urlsFor(TrackingEvent.IMPRESSION));
    }

    @Test
    public void quartilesFireOnceAtThresholdCrossings() {
        RecordingSender sender = new RecordingSender();
        PerAdTrackingState state = state(defaultQuartileTracking(), sender);

        state.fireProgress(250, 1000);
        state.fireProgress(500, 1000);
        state.fireProgress(750, 1000);
        state.fireProgress(1000, 1000);
        state.fireProgress(1000, 1000);

        assertEquals(Collections.singletonList("https://track.example.com/q1"),
                sender.urlsFor(TrackingEvent.FIRST_QUARTILE));
        assertEquals(Collections.singletonList("https://track.example.com/q2"),
                sender.urlsFor(TrackingEvent.MIDPOINT));
        assertEquals(Collections.singletonList("https://track.example.com/q3"),
                sender.urlsFor(TrackingEvent.THIRD_QUARTILE));
        assertEquals(Collections.singletonList("https://track.example.com/q4"),
                sender.urlsFor(TrackingEvent.COMPLETE));
    }

    @Test
    public void coarseProgressJumpFiresMissedQuartilesInOrder() {
        RecordingSender sender = new RecordingSender();
        PerAdTrackingState state = state(defaultQuartileTracking(), sender);

        state.fireProgress(550, 1000);

        assertEquals(
                Arrays.asList(TrackingEvent.FIRST_QUARTILE, TrackingEvent.MIDPOINT),
                sender.events());
    }

    @Test
    public void seekPauseAndResumeDoNotDuplicateAlreadyFiredQuartiles() {
        RecordingSender sender = new RecordingSender();
        PerAdTrackingState state = state(defaultQuartileTracking(), sender);

        state.fireProgress(800, 1000);
        state.fireProgress(300, 1000);
        state.fireProgress(800, 1000);

        assertEquals(Collections.singletonList("https://track.example.com/q1"),
                sender.urlsFor(TrackingEvent.FIRST_QUARTILE));
        assertEquals(Collections.singletonList("https://track.example.com/q2"),
                sender.urlsFor(TrackingEvent.MIDPOINT));
        assertEquals(Collections.singletonList("https://track.example.com/q3"),
                sender.urlsFor(TrackingEvent.THIRD_QUARTILE));
    }

    @Test
    public void clickFiresAllValidClickTrackingUrlsOnce() {
        RecordingSender sender = new RecordingSender();
        PerAdTrackingState state = state(
                tracking(
                        Collections.emptyList(),
                        Collections.emptyMap(),
                        Arrays.asList(
                                "https://track.example.com/click1",
                                "https://track.example.com/click2",
                                "not-a-url")),
                sender);

        state.fireClick();
        state.fireClick();

        assertEquals(
                Arrays.asList("https://track.example.com/click1", "https://track.example.com/click2"),
                sender.urlsFor(TrackingEvent.CLICK));
    }

    @Test
    public void duplicateUrlsAreDedupedPerEventByExactTrimmedUrl() {
        RecordingSender sender = new RecordingSender();
        PerAdTrackingState state = state(
                tracking(
                        Arrays.asList(
                                "https://track.example.com/imp",
                                " https://track.example.com/imp ",
                                "https://track.example.com/imp?distinct=1"),
                        Collections.emptyMap(),
                        Collections.emptyList()),
                sender);

        state.fireImpression();

        assertEquals(
                Arrays.asList(
                        "https://track.example.com/imp",
                        "https://track.example.com/imp?distinct=1"),
                sender.urlsFor(TrackingEvent.IMPRESSION));
    }

    private static PerAdTrackingState state(VastTracking tracking, RecordingSender sender) {
        return new PerAdTrackingState(
                tracking,
                new TrackingBeaconDispatcher(Runnable::run, sender));
    }

    private static VastTracking defaultQuartileTracking() {
        Map<TrackingEvent, List<String>> events = new EnumMap<>(TrackingEvent.class);
        events.put(TrackingEvent.FIRST_QUARTILE,
                Collections.singletonList("https://track.example.com/q1"));
        events.put(TrackingEvent.MIDPOINT,
                Collections.singletonList("https://track.example.com/q2"));
        events.put(TrackingEvent.THIRD_QUARTILE,
                Collections.singletonList("https://track.example.com/q3"));
        events.put(TrackingEvent.COMPLETE,
                Collections.singletonList("https://track.example.com/q4"));
        return tracking(Collections.emptyList(), events, Collections.emptyList());
    }

    private static VastTracking tracking(
            List<String> impressions,
            Map<TrackingEvent, List<String>> events,
            List<String> clicks) {
        return new VastTracking("ad-1", impressions, events, clicks);
    }

    private static final class RecordingSender implements TrackingBeaconDispatcher.BeaconSender {
        private final List<TrackingEvent> events = new ArrayList<>();
        private final Map<TrackingEvent, List<String>> urls = new EnumMap<>(TrackingEvent.class);

        @Override
        public void send(TrackingEvent event, String adIdentifier, URL url) {
            events.add(event);
            List<String> eventUrls = urls.get(event);
            if (eventUrls == null) {
                eventUrls = new ArrayList<>();
                urls.put(event, eventUrls);
            }
            eventUrls.add(url.toString());
        }

        private List<TrackingEvent> events() {
            return events;
        }

        private List<String> urlsFor(TrackingEvent event) {
            List<String> eventUrls = urls.get(event);
            return eventUrls == null ? Collections.emptyList() : eventUrls;
        }
    }
}
