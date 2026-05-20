using System.Threading;

#if UNITY_ANDROID && !UNITY_EDITOR
using UnityEngine;
#endif

namespace Cx.Audio.Unity
{
    public static class FullscreenInterstitial
    {
        public static FullscreenInterstitialHandle Show(
            VastRequest request,
            FullscreenInterstitialOptions options = null,
            IFullscreenInterstitialListener listener = null)
        {
            if (request == null)
            {
                throw new System.ArgumentNullException("request");
            }

            options = options ?? new FullscreenInterstitialOptions();
            request.Validate();
            options.Validate();

#if UNITY_ANDROID && !UNITY_EDITOR
            SynchronizationContext callbackContext = SynchronizationContext.Current;
            FullscreenInterstitialHandle handle = null;
            FullscreenInterstitialListenerProxy listenerProxy =
                new FullscreenInterstitialListenerProxy(listener, callbackContext, () => handle);

            using (AndroidJavaObject activity = CxAudioAndroid.CurrentActivity())
            using (AndroidJavaObject javaRequest = CxAudioAndroid.BuildFullscreenInterstitialRequest(request))
            using (AndroidJavaObject javaOptions = CxAudioAndroid.BuildFullscreenInterstitialOptions(options))
            using (AndroidJavaClass fullscreenInterstitialClass =
                new AndroidJavaClass("com.cx.audio.android.AndroidFullscreenInterstitial"))
            {
                AndroidJavaObject nativeHandle = fullscreenInterstitialClass.CallStatic<AndroidJavaObject>(
                    "show",
                    activity,
                    javaRequest,
                    javaOptions,
                    listenerProxy);
                handle = new FullscreenInterstitialHandle(nativeHandle, listenerProxy);
                return handle;
            }
#else
            throw CxAudioRuntime.UnsupportedPlatformException();
#endif
        }
    }
}
