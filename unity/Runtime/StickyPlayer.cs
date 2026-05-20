using System.Threading;

#if UNITY_ANDROID && !UNITY_EDITOR
using UnityEngine;
#endif

namespace Cx.Audio.Unity
{
    public static class StickyPlayer
    {
        public static StickyPlayerHandle Show(
            VastRequest request,
            StickyPlayerOptions options = null,
            IStickyPlayerListener listener = null)
        {
            if (request == null)
            {
                throw new System.ArgumentNullException("request");
            }

            options = options ?? new StickyPlayerOptions();
            request.Validate();
            options.Validate();

#if UNITY_ANDROID && !UNITY_EDITOR
            SynchronizationContext callbackContext = SynchronizationContext.Current;
            StickyPlayerHandle handle = null;
            StickyPlayerListenerProxy listenerProxy =
                new StickyPlayerListenerProxy(listener, callbackContext, () => handle);

            using (AndroidJavaObject activity = CxAudioAndroid.CurrentActivity())
            using (AndroidJavaObject javaRequest = CxAudioAndroid.BuildStickyPlayerRequest(request))
            using (AndroidJavaObject javaOptions = CxAudioAndroid.BuildStickyPlayerOptions(options))
            using (AndroidJavaClass stickyPlayerClass =
                new AndroidJavaClass("com.cx.audio.android.AndroidStickyPlayer"))
            {
                AndroidJavaObject nativeHandle = stickyPlayerClass.CallStatic<AndroidJavaObject>(
                    "show",
                    activity,
                    javaRequest,
                    javaOptions,
                    listenerProxy);
                handle = new StickyPlayerHandle(nativeHandle, listenerProxy);
                return handle;
            }
#else
            throw CxAudioRuntime.UnsupportedPlatformException();
#endif
        }
    }
}
