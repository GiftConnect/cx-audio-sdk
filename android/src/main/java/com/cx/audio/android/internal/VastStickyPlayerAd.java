package com.cx.audio.android.internal;

public final class VastStickyPlayerAd {
    private final VastMediaFile mediaFile;
    private final VastCompanion companion;
    private final VastTracking tracking;

    public VastStickyPlayerAd(VastMediaFile mediaFile, VastCompanion companion) {
        this(mediaFile, companion, VastTracking.empty());
    }

    public VastStickyPlayerAd(
            VastMediaFile mediaFile,
            VastCompanion companion,
            VastTracking tracking) {
        this.mediaFile = mediaFile;
        this.companion = companion;
        this.tracking = tracking == null ? VastTracking.empty() : tracking;
    }

    public VastMediaFile getMediaFile() {
        return mediaFile;
    }

    public VastCompanion getCompanion() {
        return companion;
    }

    public VastTracking getTracking() {
        return tracking;
    }
}
