#if UNITY_ANDROID && !UNITY_EDITOR
using UnityEngine;

namespace Cx.Audio.Unity
{
    internal static class CxAudioAndroid
    {
        internal static AndroidJavaObject CurrentActivity()
        {
            using (AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            {
                AndroidJavaObject activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
                if (activity == null)
                {
                    throw new System.InvalidOperationException("UnityPlayer.currentActivity is unavailable.");
                }

                return activity;
            }
        }

        internal static AndroidJavaObject BuildBackgroundAudioRequest(VastRequest request)
        {
            return BuildRequest("com.cx.audio.android.BackgroundAudioRequest", request);
        }

        internal static AndroidJavaObject BuildStickyPlayerRequest(VastRequest request)
        {
            return BuildRequest("com.cx.audio.android.StickyPlayerRequest", request);
        }

        internal static AndroidJavaObject BuildFullscreenInterstitialRequest(VastRequest request)
        {
            return BuildRequest("com.cx.audio.android.FullscreenInterstitialRequest", request);
        }

        internal static AndroidJavaObject BuildBackgroundAudioOptions(BackgroundAudioOptions options)
        {
            AndroidJavaObject builder = null;
            try
            {
                using (AndroidJavaClass optionsClass =
                    new AndroidJavaClass("com.cx.audio.android.BackgroundAudioOptions"))
                {
                    builder = optionsClass.CallStatic<AndroidJavaObject>("builder");
                }

                CallBuilder(builder, "volume", options.Volume);
                CallBuilder(builder, "minDeviceVolume", options.MinDeviceVolume);
                return builder.Call<AndroidJavaObject>("build");
            }
            finally
            {
                if (builder != null)
                {
                    builder.Dispose();
                }
            }
        }

        internal static AndroidJavaObject BuildStickyPlayerOptions(StickyPlayerOptions options)
        {
            AndroidJavaObject builder = null;
            try
            {
                using (AndroidJavaClass optionsClass =
                    new AndroidJavaClass("com.cx.audio.android.StickyPlayerOptions"))
                {
                    builder = optionsClass.CallStatic<AndroidJavaObject>("builder");
                }

                using (AndroidJavaObject position = GetStickyPlayerPosition(options.Position))
                {
                    CallBuilder(builder, "position", position);
                }

                CallBuilder(builder, "heightDp", options.HeightDp);
                CallBuilder(builder, "horizontalPaddingDp", options.HorizontalPaddingDp);
                CallBuilder(builder, "showCloseButton", options.ShowCloseButton);
                CallBuilder(builder, "volume", options.Volume);
                CallBuilder(builder, "minDeviceVolume", options.MinDeviceVolume);
                return builder.Call<AndroidJavaObject>("build");
            }
            finally
            {
                if (builder != null)
                {
                    builder.Dispose();
                }
            }
        }

        internal static AndroidJavaObject BuildFullscreenInterstitialOptions(
            FullscreenInterstitialOptions options)
        {
            AndroidJavaObject builder = null;
            try
            {
                using (AndroidJavaClass optionsClass =
                    new AndroidJavaClass("com.cx.audio.android.FullscreenInterstitialOptions"))
                {
                    builder = optionsClass.CallStatic<AndroidJavaObject>("builder");
                }

                CallBuilder(builder, "showCloseButton", options.ShowCloseButton);
                CallBuilder(builder, "companionHeightDp", options.CompanionHeightDp);
                CallBuilder(builder, "companionMaxWidthDp", options.CompanionMaxWidthDp);
                CallBuilder(builder, "autoPlayVideo", options.AutoPlayVideo);
                return builder.Call<AndroidJavaObject>("build");
            }
            finally
            {
                if (builder != null)
                {
                    builder.Dispose();
                }
            }
        }

        private static AndroidJavaObject BuildRequest(string requestClassName, VastRequest request)
        {
            AndroidJavaObject builder = null;
            try
            {
                using (AndroidJavaClass requestClass = new AndroidJavaClass(requestClassName))
                {
                    builder = requestClass.CallStatic<AndroidJavaObject>("builder", request.VastTagUrl);
                }

                if (request.ConnectTimeoutMs > 0)
                {
                    CallBuilder(builder, "connectTimeoutMs", request.ConnectTimeoutMs);
                }

                if (request.ReadTimeoutMs > 0)
                {
                    CallBuilder(builder, "readTimeoutMs", request.ReadTimeoutMs);
                }

                if (!string.IsNullOrEmpty(request.UserAgent))
                {
                    CallBuilder(builder, "userAgent", request.UserAgent);
                }

                return builder.Call<AndroidJavaObject>("build");
            }
            finally
            {
                if (builder != null)
                {
                    builder.Dispose();
                }
            }
        }

        private static AndroidJavaObject GetStickyPlayerPosition(StickyPlayerPosition position)
        {
            string fieldName = position == StickyPlayerPosition.Top ? "TOP" : "BOTTOM";
            using (AndroidJavaClass positionClass =
                new AndroidJavaClass("com.cx.audio.android.StickyPlayerPosition"))
            {
                return positionClass.GetStatic<AndroidJavaObject>(fieldName);
            }
        }

        private static void CallBuilder(AndroidJavaObject builder, string methodName, params object[] args)
        {
            using (AndroidJavaObject ignored = builder.Call<AndroidJavaObject>(methodName, args))
            {
            }
        }
    }
}
#endif
