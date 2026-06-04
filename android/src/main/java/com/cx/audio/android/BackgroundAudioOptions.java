package com.cx.audio.android;

public final class BackgroundAudioOptions {
    private final float volume;
    private final float minDeviceVolume;

    private BackgroundAudioOptions(Builder builder) {
        this.volume = builder.volume;
        this.minDeviceVolume = builder.minDeviceVolume;
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
        private float volume = 0.2f;
        private float minDeviceVolume = 0.1f;

        public Builder volume(float volume) {
            validateVolume(volume);
            this.volume = volume;
            return this;
        }

        public Builder minDeviceVolume(float minDeviceVolume) {
            validateVolume(minDeviceVolume);
            this.minDeviceVolume = minDeviceVolume;
            return this;
        }

        public BackgroundAudioOptions build() {
            return new BackgroundAudioOptions(this);
        }

        private static void validateVolume(float volume) {
            if (Float.isNaN(volume) || volume < 0.0f || volume > 1.0f) {
                throw new IllegalArgumentException("volume must be between 0.0 and 1.0");
            }
        }
    }
}
