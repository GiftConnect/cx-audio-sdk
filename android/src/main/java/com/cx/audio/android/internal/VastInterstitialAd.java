package com.cx.audio.android.internal;

public final class VastInterstitialAd {
    private final VastMediaFile mediaFile;
    private final VastMediaFile audioMediaFile;
    private final VastCompanion companion;

    VastInterstitialAd(VastMediaFile mediaFile, VastCompanion companion) {
        this(mediaFile, null, companion);
    }

    VastInterstitialAd(VastMediaFile mediaFile, VastMediaFile audioMediaFile, VastCompanion companion) {
        this.mediaFile = mediaFile;
        this.audioMediaFile = audioMediaFile;
        this.companion = companion;
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
}
