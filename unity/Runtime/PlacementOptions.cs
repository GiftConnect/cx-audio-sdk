using System;

namespace Cx.Audio.Unity
{
    public sealed class BackgroundAudioOptions
    {
        public BackgroundAudioOptions()
        {
            Volume = 0.2f;
            MinDeviceVolume = 0.1f;
        }

        public float Volume { get; set; }

        public float MinDeviceVolume { get; set; }

        internal void Validate()
        {
            if (float.IsNaN(Volume) || Volume < 0.0f || Volume > 1.0f)
            {
                throw new ArgumentOutOfRangeException("Volume", "volume must be between 0.0 and 1.0");
            }

            if (float.IsNaN(MinDeviceVolume) || MinDeviceVolume < 0.0f || MinDeviceVolume > 1.0f)
            {
                throw new ArgumentOutOfRangeException(
                    "MinDeviceVolume",
                    "minDeviceVolume must be between 0.0 and 1.0");
            }
        }
    }

    public enum StickyPlayerPosition
    {
        Top,
        Bottom
    }

    public sealed class StickyPlayerOptions
    {
        public StickyPlayerOptions()
        {
            Position = StickyPlayerPosition.Bottom;
            HeightDp = 72;
            HorizontalPaddingDp = 12;
            ShowCloseButton = true;
        }

        public StickyPlayerPosition Position { get; set; }

        public int HeightDp { get; set; }

        public int HorizontalPaddingDp { get; set; }

        public bool ShowCloseButton { get; set; }

        internal void Validate()
        {
            if (HeightDp < 48)
            {
                throw new ArgumentOutOfRangeException("HeightDp", "heightDp must be at least 48");
            }

            if (HorizontalPaddingDp < 0)
            {
                throw new ArgumentOutOfRangeException("HorizontalPaddingDp", "horizontalPaddingDp must be >= 0");
            }
        }
    }

    public sealed class FullscreenInterstitialOptions
    {
        public FullscreenInterstitialOptions()
        {
            ShowCloseButton = true;
            CompanionHeightDp = 250;
            CompanionMaxWidthDp = 300;
            AutoPlayVideo = true;
        }

        public bool ShowCloseButton { get; set; }

        public int CompanionHeightDp { get; set; }

        public int CompanionMaxWidthDp { get; set; }

        public bool AutoPlayVideo { get; set; }

        internal void Validate()
        {
            if (CompanionHeightDp < 48)
            {
                throw new ArgumentOutOfRangeException("CompanionHeightDp", "companionHeightDp must be at least 48");
            }

            if (CompanionMaxWidthDp < 48)
            {
                throw new ArgumentOutOfRangeException("CompanionMaxWidthDp", "companionMaxWidthDp must be at least 48");
            }
        }
    }
}
