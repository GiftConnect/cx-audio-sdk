using System.Threading;

#if UNITY_ANDROID && !UNITY_EDITOR
using UnityEngine;
#endif

namespace Cx.Audio.Unity
{
    public static class BackgroundAudio
    {
        public static BackgroundAudioHandle Play(
            VastRequest request,
            BackgroundAudioOptions options = null,
            IBackgroundAudioListener listener = null)
        {
            if (request == null)
            {
                throw new System.ArgumentNullException("request");
            }

            options = options ?? new BackgroundAudioOptions();
            request.Validate();
            options.Validate();

#if UNITY_ANDROID && !UNITY_EDITOR
            SynchronizationContext callbackContext = SynchronizationContext.Current;
            BackgroundAudioHandle handle = null;
            BackgroundAudioListenerProxy listenerProxy =
                new BackgroundAudioListenerProxy(listener, callbackContext, () => handle);

            using (AndroidJavaObject activity = CxAudioAndroid.CurrentActivity())
            using (AndroidJavaObject javaRequest = CxAudioAndroid.BuildBackgroundAudioRequest(request))
            using (AndroidJavaObject javaOptions = CxAudioAndroid.BuildBackgroundAudioOptions(options))
            using (AndroidJavaClass backgroundAudioClass =
                new AndroidJavaClass("com.cx.audio.android.AndroidBackgroundAudio"))
            {
                AndroidJavaObject nativeHandle = backgroundAudioClass.CallStatic<AndroidJavaObject>(
                    "play",
                    activity,
                    javaRequest,
                    javaOptions,
                    listenerProxy);
                handle = new BackgroundAudioHandle(nativeHandle, listenerProxy);
                return handle;
            }
#else
            throw CxAudioRuntime.UnsupportedPlatformException();
#endif
        }
    }
}
