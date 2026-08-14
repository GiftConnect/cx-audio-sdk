#if UNITY_ANDROID && !UNITY_EDITOR
using UnityEngine;

namespace Cx.Audio.Unity
{
    internal static class CxAudioAndroid
    {
        private static readonly object InitializationLock = new object();
        private static bool initialized;

        internal static void Initialize(CxAudioConfiguration configuration)
        {
            if (configuration == null)
            {
                throw new System.ArgumentNullException("configuration");
            }

            lock (InitializationLock)
            {
                using (AndroidJavaObject activity = GetCurrentActivity())
                using (AndroidJavaObject nativeConfiguration = BuildConfiguration(configuration))
                using (AndroidJavaClass cxAudio = new AndroidJavaClass("com.cx.audio.android.CxAudio"))
                {
                    cxAudio.CallStatic("initialize", activity, nativeConfiguration);
                }
                initialized = true;
            }
        }

        internal static AndroidJavaObject CurrentActivity()
        {
            AndroidJavaObject activity = GetCurrentActivity();
            lock (InitializationLock)
            {
                if (!initialized)
                {
                    using (AndroidJavaClass cxAudio =
                        new AndroidJavaClass("com.cx.audio.android.CxAudio"))
                    {
                        cxAudio.CallStatic("initialize", activity);
                    }
                    initialized = true;
                }
            }
            return activity;
        }

        private static AndroidJavaObject GetCurrentActivity()
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

        private static AndroidJavaObject BuildConfiguration(CxAudioConfiguration configuration)
        {
            AndroidJavaObject builder = null;
            try
            {
                using (AndroidJavaClass configurationClass =
                    new AndroidJavaClass("com.cx.audio.android.CxAudioConfiguration"))
                {
                    builder = configurationClass.CallStatic<AndroidJavaObject>("builder");
                }

                using (AndroidJavaObject ageTreatment = GetAgeTreatment(configuration.AgeTreatment))
                {
                    CallBuilder(builder, "ageTreatment", ageTreatment);
                }
                if (configuration.PrivacyOverride != null)
                {
                    using (AndroidJavaObject privacyOverride = BuildPrivacyOverride(configuration.PrivacyOverride))
                    {
                        CallBuilder(builder, "privacyOverride", privacyOverride);
                    }
                }
                return builder.Call<AndroidJavaObject>("build");
            }
            finally
            {
                if (builder != null) builder.Dispose();
            }
        }

        private static AndroidJavaObject BuildPrivacyOverride(CxPrivacyOverride privacyOverride)
        {
            AndroidJavaObject builder = null;
            try
            {
                using (AndroidJavaClass overrideClass =
                    new AndroidJavaClass("com.cx.audio.android.CxPrivacyOverride"))
                {
                    builder = overrideClass.CallStatic<AndroidJavaObject>("builder");
                }

                CallNullableBoolean(builder, "gdprApplies", privacyOverride.GdprApplies);
                CallNullableBoolean(builder, "personalizedAdsAllowed", privacyOverride.PersonalizedAdsAllowed);
                CallNullableBoolean(builder, "usPrivacyOptOut", privacyOverride.UsPrivacyOptOut);
                CallOptionalString(builder, "tcfString", privacyOverride.TcfString);
                CallOptionalString(builder, "additionalConsent", privacyOverride.AdditionalConsent);
                CallOptionalString(builder, "gppString", privacyOverride.GppString);
                CallOptionalString(builder, "usPrivacyString", privacyOverride.UsPrivacyString);
                return builder.Call<AndroidJavaObject>("build");
            }
            finally
            {
                if (builder != null) builder.Dispose();
            }
        }

        private static AndroidJavaObject GetAgeTreatment(CxAgeTreatment ageTreatment)
        {
            string fieldName;
            switch (ageTreatment)
            {
                case CxAgeTreatment.GeneralAudience: fieldName = "GENERAL_AUDIENCE"; break;
                case CxAgeTreatment.ChildDirected: fieldName = "CHILD_DIRECTED"; break;
                case CxAgeTreatment.UnderAgeOfConsent: fieldName = "UNDER_AGE_OF_CONSENT"; break;
                default: fieldName = "UNSPECIFIED"; break;
            }
            using (AndroidJavaClass ageClass =
                new AndroidJavaClass("com.cx.audio.android.CxAgeTreatment"))
            {
                return ageClass.GetStatic<AndroidJavaObject>(fieldName);
            }
        }

        private static void CallNullableBoolean(
            AndroidJavaObject builder,
            string methodName,
            bool? value)
        {
            if (!value.HasValue) return;
            using (AndroidJavaObject boxed = new AndroidJavaObject("java.lang.Boolean", value.Value))
            {
                CallBuilder(builder, methodName, boxed);
            }
        }

        private static void CallOptionalString(
            AndroidJavaObject builder,
            string methodName,
            string value)
        {
            if (!string.IsNullOrEmpty(value)) CallBuilder(builder, methodName, value);
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
                CallBuilder(builder, "autoPlayVideo", options.AutoPlayVideo);
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

        private static AndroidJavaObject BuildRequest(string requestClassName, VastRequest request)
        {
            AndroidJavaObject builder = null;
            try
            {
                using (AndroidJavaClass requestClass = new AndroidJavaClass(requestClassName))
                using (AndroidJavaObject activity = GetCurrentActivity())
                {
                    builder = requestClass.CallStatic<AndroidJavaObject>(
                        "builder",
                        activity,
                        request.VastTagUrl);
                }

                if (request.ConnectTimeoutMs > 0)
                {
                    CallBuilder(builder, "connectTimeoutMs", request.ConnectTimeoutMs);
                }

                if (request.ReadTimeoutMs > 0)
                {
                    CallBuilder(builder, "readTimeoutMs", request.ReadTimeoutMs);
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
