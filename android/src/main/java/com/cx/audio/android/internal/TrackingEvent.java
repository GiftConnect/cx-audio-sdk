package com.cx.audio.android.internal;

import java.util.Locale;

public enum TrackingEvent {
    IMPRESSION("impression"),
    START("start"),
    FIRST_QUARTILE("firstQuartile"),
    MIDPOINT("midpoint"),
    THIRD_QUARTILE("thirdQuartile"),
    COMPLETE("complete"),
    CLICK("clickTracking");

    private final String logName;

    TrackingEvent(String logName) {
        this.logName = logName;
    }

    public String getLogName() {
        return logName;
    }

    static TrackingEvent fromVastEvent(String eventName) {
        if (eventName == null) {
            return null;
        }

        switch (eventName.trim().toLowerCase(Locale.US)) {
            case "start":
                return START;
            case "firstquartile":
                return FIRST_QUARTILE;
            case "midpoint":
                return MIDPOINT;
            case "thirdquartile":
                return THIRD_QUARTILE;
            case "complete":
                return COMPLETE;
            default:
                return null;
        }
    }
}
