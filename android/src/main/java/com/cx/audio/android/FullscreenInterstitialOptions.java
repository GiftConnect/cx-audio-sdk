package com.cx.audio.android;

public final class FullscreenInterstitialOptions {
    private final boolean showCloseButton;
    private final boolean autoPlayVideo;
    private final float volume;
    private final float minDeviceVolume;

    private FullscreenInterstitialOptions(Builder builder) {
        this.showCloseButton = builder.showCloseButton;
        this.autoPlayVideo = builder.autoPlayVideo;
        this.volume = builder.volume;
        this.minDeviceVolume = builder.minDeviceVolume;
    }

    public boolean shouldShowCloseButton() {
        return showCloseButton;
    }

    public boolean shouldAutoPlayVideo() {
        return autoPlayVideo;
    }

    public float getVolume() {
        return volume;
    }

    public float getMinDeviceVolume() {
        return minDeviceVolume;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean showCloseButton = true;
        private boolean autoPlayVideo = true;
        private float volume = 0.2f;
        private float minDeviceVolume = 0.1f;

        public Builder showCloseButton(boolean showCloseButton) {
            this.showCloseButton = showCloseButton;
            return this;
        }

        public Builder autoPlayVideo(boolean autoPlayVideo) {
            this.autoPlayVideo = autoPlayVideo;
            return this;
        }

        public Builder volume(float volume) {
            validateVolume(volume, "volume");
            this.volume = volume;
            return this;
        }

        public Builder minDeviceVolume(float minDeviceVolume) {
            validateVolume(minDeviceVolume, "minDeviceVolume");
            this.minDeviceVolume = minDeviceVolume;
            return this;
        }

        public FullscreenInterstitialOptions build() {
            return new FullscreenInterstitialOptions(this);
        }

        private static void validateVolume(float volume, String name) {
            if (Float.isNaN(volume) || volume < 0.0f || volume > 1.0f) {
                throw new IllegalArgumentException(name + " must be between 0.0 and 1.0");
            }
        }
    }
}
