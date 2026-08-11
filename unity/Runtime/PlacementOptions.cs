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
            ShowCloseButton = true;
            Volume = 0.2f;
            MinDeviceVolume = 0.1f;
        }

        public StickyPlayerPosition Position { get; set; }

        public bool ShowCloseButton { get; set; }

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

    public sealed class FullscreenInterstitialOptions
    {
        public FullscreenInterstitialOptions()
        {
            ShowCloseButton = true;
            AutoPlayVideo = true;
            Volume = 0.2f;
            MinDeviceVolume = 0.1f;
        }

        public bool ShowCloseButton { get; set; }

        public bool AutoPlayVideo { get; set; }

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
}
