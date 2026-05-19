package com.cx.audio.android;

public final class FullscreenInterstitialOptions {
    private final boolean showCloseButton;
    private final int companionHeightDp;
    private final int companionMaxWidthDp;
    private final boolean autoPlayVideo;

    private FullscreenInterstitialOptions(Builder builder) {
        this.showCloseButton = builder.showCloseButton;
        this.companionHeightDp = builder.companionHeightDp;
        this.companionMaxWidthDp = builder.companionMaxWidthDp;
        this.autoPlayVideo = builder.autoPlayVideo;
    }

    public boolean shouldShowCloseButton() {
        return showCloseButton;
    }

    public int getCompanionHeightDp() {
        return companionHeightDp;
    }

    public int getCompanionMaxWidthDp() {
        return companionMaxWidthDp;
    }

    public boolean shouldAutoPlayVideo() {
        return autoPlayVideo;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean showCloseButton = true;
        private int companionHeightDp = 250;
        private int companionMaxWidthDp = 300;
        private boolean autoPlayVideo = true;

        public Builder showCloseButton(boolean showCloseButton) {
            this.showCloseButton = showCloseButton;
            return this;
        }

        public Builder companionHeightDp(int companionHeightDp) {
            if (companionHeightDp < 48) {
                throw new IllegalArgumentException("companionHeightDp must be at least 48");
            }
            this.companionHeightDp = companionHeightDp;
            return this;
        }

        public Builder companionMaxWidthDp(int companionMaxWidthDp) {
            if (companionMaxWidthDp < 48) {
                throw new IllegalArgumentException("companionMaxWidthDp must be at least 48");
            }
            this.companionMaxWidthDp = companionMaxWidthDp;
            return this;
        }

        public Builder autoPlayVideo(boolean autoPlayVideo) {
            this.autoPlayVideo = autoPlayVideo;
            return this;
        }

        public FullscreenInterstitialOptions build() {
            return new FullscreenInterstitialOptions(this);
        }
    }
}
