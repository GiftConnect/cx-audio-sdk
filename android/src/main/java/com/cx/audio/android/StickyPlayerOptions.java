package com.cx.audio.android;

public final class StickyPlayerOptions {
    private final StickyPlayerPosition position;
    private final int heightDp;
    private final int horizontalPaddingDp;
    private final boolean showCloseButton;

    private StickyPlayerOptions(Builder builder) {
        this.position = builder.position;
        this.heightDp = builder.heightDp;
        this.horizontalPaddingDp = builder.horizontalPaddingDp;
        this.showCloseButton = builder.showCloseButton;
    }

    public StickyPlayerPosition getPosition() {
        return position;
    }

    public int getHeightDp() {
        return heightDp;
    }

    public int getHorizontalPaddingDp() {
        return horizontalPaddingDp;
    }

    public boolean shouldShowCloseButton() {
        return showCloseButton;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private StickyPlayerPosition position = StickyPlayerPosition.BOTTOM;
        private int heightDp = 72;
        private int horizontalPaddingDp = 12;
        private boolean showCloseButton = true;

        public Builder position(StickyPlayerPosition position) {
            if (position == null) {
                throw new IllegalArgumentException("position must not be null");
            }
            this.position = position;
            return this;
        }

        public Builder heightDp(int heightDp) {
            if (heightDp < 48) {
                throw new IllegalArgumentException("heightDp must be at least 48");
            }
            this.heightDp = heightDp;
            return this;
        }

        public Builder horizontalPaddingDp(int horizontalPaddingDp) {
            if (horizontalPaddingDp < 0) {
                throw new IllegalArgumentException("horizontalPaddingDp must be >= 0");
            }
            this.horizontalPaddingDp = horizontalPaddingDp;
            return this;
        }

        public Builder showCloseButton(boolean showCloseButton) {
            this.showCloseButton = showCloseButton;
            return this;
        }

        public StickyPlayerOptions build() {
            return new StickyPlayerOptions(this);
        }
    }
}
