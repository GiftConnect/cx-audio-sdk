package com.cx.audio.android.internal;

public final class VastInterstitialAd {
    private final VastMediaFile mediaFile;
    private final VastMediaFile audioMediaFile;
    private final VastCompanion companion;
    private final VastTracking tracking;

    VastInterstitialAd(VastMediaFile mediaFile, VastCompanion companion) {
        this(mediaFile, null, companion);
    }

    VastInterstitialAd(VastMediaFile mediaFile, VastMediaFile audioMediaFile, VastCompanion companion) {
        this(mediaFile, audioMediaFile, companion, VastTracking.empty());
    }

    VastInterstitialAd(
            VastMediaFile mediaFile,
            VastMediaFile audioMediaFile,
            VastCompanion companion,
            VastTracking tracking) {
        this.mediaFile = mediaFile;
        this.audioMediaFile = audioMediaFile;
        this.companion = companion;
        this.tracking = tracking == null ? VastTracking.empty() : tracking;
    }

    public VastMediaFile getMediaFile() {
        return mediaFile;
    }

    public VastMediaFile getAudioMediaFile() {
        return audioMediaFile;
    }

    public VastCompanion getCompanion() {
        return companion;
    }

    public VastTracking getTracking() {
        return tracking;
    }
}
