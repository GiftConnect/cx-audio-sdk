package com.cx.audio.android.internal;

public final class VastStickyPlayerAd {
    private final VastMediaFile mediaFile;
    private final VastCompanion companion;

    public VastStickyPlayerAd(VastMediaFile mediaFile, VastCompanion companion) {
        this.mediaFile = mediaFile;
        this.companion = companion;
    }

    public VastMediaFile getMediaFile() {
        return mediaFile;
    }

    public VastCompanion getCompanion() {
        return companion;
    }
}
