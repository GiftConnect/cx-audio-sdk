package com.cx.audio.android.internal;

public final class VastInterstitialAd {
    private final VastMediaFile mediaFile;
    private final VastCompanion companion;

    VastInterstitialAd(VastMediaFile mediaFile, VastCompanion companion) {
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
