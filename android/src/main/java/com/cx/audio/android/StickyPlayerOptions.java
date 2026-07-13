package com.cx.audio.android;

public final class StickyPlayerOptions {
    private final StickyPlayerPosition position;
    private final boolean showCloseButton;
    private final float volume;
    private final float minDeviceVolume;

    private StickyPlayerOptions(Builder builder) {
        this.position = builder.position;
        this.showCloseButton = builder.showCloseButton;
        this.volume = builder.volume;
        this.minDeviceVolume = builder.minDeviceVolume;
    }

    public StickyPlayerPosition getPosition() {
        return position;
    }

    public boolean shouldShowCloseButton() {
        return showCloseButton;
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
        private StickyPlayerPosition position = StickyPlayerPosition.BOTTOM;
        private boolean showCloseButton = true;
        private float volume = 0.2f;
        private float minDeviceVolume = 0.1f;

        public Builder position(StickyPlayerPosition position) {
            if (position == null) {
                throw new IllegalArgumentException("position must not be null");
            }
            this.position = position;
            return this;
        }

        public Builder showCloseButton(boolean showCloseButton) {
            this.showCloseButton = showCloseButton;
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

        public StickyPlayerOptions build() {
            return new StickyPlayerOptions(this);
        }

        private static void validateVolume(float volume, String name) {
            if (Float.isNaN(volume) || volume < 0.0f || volume > 1.0f) {
                throw new IllegalArgumentException(name + " must be between 0.0 and 1.0");
            }
        }
    }
}
