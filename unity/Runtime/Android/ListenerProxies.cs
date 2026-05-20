#if UNITY_ANDROID && !UNITY_EDITOR
using System;
using System.Threading;
using UnityEngine;

namespace Cx.Audio.Unity
{
    internal sealed class BackgroundAudioListenerProxy : AndroidJavaProxy
    {
        private readonly IBackgroundAudioListener listener;
        private readonly SynchronizationContext callbackContext;
        private readonly Func<BackgroundAudioHandle> getHandle;

        internal BackgroundAudioListenerProxy(
            IBackgroundAudioListener listener,
            SynchronizationContext callbackContext,
            Func<BackgroundAudioHandle> getHandle)
            : base("com.cx.audio.android.BackgroundAudioListener")
        {
            this.listener = listener;
            this.callbackContext = callbackContext;
            this.getHandle = getHandle;
        }

        public void onBackgroundAudioStarted(AndroidJavaObject nativeHandle)
        {
            CxAudioRuntime.Dispatch(callbackContext, () =>
            {
                if (listener != null)
                {
                    listener.OnBackgroundAudioStarted(getHandle());
                }
            });
        }

        public void onBackgroundAudioCompleted()
        {
            CxAudioRuntime.Dispatch(callbackContext, () =>
            {
                if (listener != null)
                {
                    listener.OnBackgroundAudioCompleted();
                }
            });
        }

        public void onBackgroundAudioStopped()
        {
            CxAudioRuntime.Dispatch(callbackContext, () =>
            {
                if (listener != null)
                {
                    listener.OnBackgroundAudioStopped();
                }
            });
        }

        public void onBackgroundAudioFailed(AndroidJavaObject error)
        {
            CxAudioError cxError = CxAudioError.FromJavaThrowable(error);
            CxAudioRuntime.Dispatch(callbackContext, () =>
            {
                if (listener != null)
                {
                    listener.OnBackgroundAudioFailed(cxError);
                }
            });
        }
    }

    internal sealed class StickyPlayerListenerProxy : AndroidJavaProxy
    {
        private readonly IStickyPlayerListener listener;
        private readonly SynchronizationContext callbackContext;
        private readonly Func<StickyPlayerHandle> getHandle;

        internal StickyPlayerListenerProxy(
            IStickyPlayerListener listener,
            SynchronizationContext callbackContext,
            Func<StickyPlayerHandle> getHandle)
            : base("com.cx.audio.android.StickyPlayerListener")
        {
            this.listener = listener;
            this.callbackContext = callbackContext;
            this.getHandle = getHandle;
        }

        public void onStickyPlayerLoaded(AndroidJavaObject nativeHandle)
        {
            CxAudioRuntime.Dispatch(callbackContext, () =>
            {
                if (listener != null)
                {
                    listener.OnStickyPlayerLoaded(getHandle());
                }
            });
        }

        public void onStickyPlayerClicked(string clickThroughUrl)
        {
            CxAudioRuntime.Dispatch(callbackContext, () =>
            {
                if (listener != null)
                {
                    listener.OnStickyPlayerClicked(clickThroughUrl);
                }
            });
        }

        public void onStickyPlayerDismissed()
        {
            CxAudioRuntime.Dispatch(callbackContext, () =>
            {
                if (listener != null)
                {
                    listener.OnStickyPlayerDismissed();
                }
            });
        }

        public void onStickyPlayerFailed(AndroidJavaObject error)
        {
            CxAudioError cxError = CxAudioError.FromJavaThrowable(error);
            CxAudioRuntime.Dispatch(callbackContext, () =>
            {
                if (listener != null)
                {
                    listener.OnStickyPlayerFailed(cxError);
                }
            });
        }
    }

    internal sealed class FullscreenInterstitialListenerProxy : AndroidJavaProxy
    {
        private readonly IFullscreenInterstitialListener listener;
        private readonly SynchronizationContext callbackContext;
        private readonly Func<FullscreenInterstitialHandle> getHandle;

        internal FullscreenInterstitialListenerProxy(
            IFullscreenInterstitialListener listener,
            SynchronizationContext callbackContext,
            Func<FullscreenInterstitialHandle> getHandle)
            : base("com.cx.audio.android.FullscreenInterstitialListener")
        {
            this.listener = listener;
            this.callbackContext = callbackContext;
            this.getHandle = getHandle;
        }

        public void onFullscreenInterstitialLoaded(AndroidJavaObject nativeHandle)
        {
            CxAudioRuntime.Dispatch(callbackContext, () =>
            {
                if (listener != null)
                {
                    listener.OnFullscreenInterstitialLoaded(getHandle());
                }
            });
        }

        public void onFullscreenInterstitialClicked(string clickThroughUrl)
        {
            CxAudioRuntime.Dispatch(callbackContext, () =>
            {
                if (listener != null)
                {
                    listener.OnFullscreenInterstitialClicked(clickThroughUrl);
                }
            });
        }

        public void onFullscreenInterstitialDismissed()
        {
            CxAudioRuntime.Dispatch(callbackContext, () =>
            {
                if (listener != null)
                {
                    listener.OnFullscreenInterstitialDismissed();
                }
            });
        }

        public void onFullscreenInterstitialFailed(AndroidJavaObject error)
        {
            CxAudioError cxError = CxAudioError.FromJavaThrowable(error);
            CxAudioRuntime.Dispatch(callbackContext, () =>
            {
                if (listener != null)
                {
                    listener.OnFullscreenInterstitialFailed(cxError);
                }
            });
        }
    }
}
#endif
