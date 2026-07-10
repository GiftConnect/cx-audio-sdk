package com.cx.audio.android.internal;

public final class VastAudioAd {
    private final VastMediaFile mediaFile;
    private final VastTracking tracking;

    VastAudioAd(VastMediaFile mediaFile, VastTracking tracking) {
        this.mediaFile = mediaFile;
        this.tracking = tracking == null ? VastTracking.empty() : tracking;
    }

    public VastMediaFile getMediaFile() {
        return mediaFile;
    }

    public VastTracking getTracking() {
        return tracking;
    }
}
