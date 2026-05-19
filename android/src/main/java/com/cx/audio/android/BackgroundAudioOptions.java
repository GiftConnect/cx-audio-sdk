package com.cx.audio.android;

public final class BackgroundAudioOptions {
    private final float volume;

    private BackgroundAudioOptions(Builder builder) {
        this.volume = builder.volume;
    }

    public float getVolume() {
        return volume;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float volume = 0.2f;

        public Builder volume(float volume) {
            validateVolume(volume);
            this.volume = volume;
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
