using System;

#if UNITY_ANDROID && !UNITY_EDITOR
using UnityEngine;
#endif

namespace Cx.Audio.Unity
{
    public sealed class BackgroundAudioHandle : IDisposable
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        private AndroidJavaObject nativeHandle;
        private object listenerProxy;

        internal BackgroundAudioHandle(AndroidJavaObject nativeHandle, object listenerProxy)
        {
            if (nativeHandle == null)
            {
                throw new ArgumentNullException("nativeHandle");
            }

            this.nativeHandle = nativeHandle;
            this.listenerProxy = listenerProxy;
        }
#endif

        public bool IsPlaying
        {
            get
            {
#if UNITY_ANDROID && !UNITY_EDITOR
                EnsureNotDisposed();
                return nativeHandle.Call<bool>("isPlaying");
#else
                return false;
#endif
            }
        }

        public void Stop()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            EnsureNotDisposed();
            nativeHandle.Call("stop");
#else
            throw CxAudioRuntime.UnsupportedPlatformException();
#endif
        }

        public void SetVolume(float volume)
        {
            if (float.IsNaN(volume) || volume < 0.0f || volume > 1.0f)
            {
                throw new ArgumentOutOfRangeException("volume", "volume must be between 0.0 and 1.0");
            }

#if UNITY_ANDROID && !UNITY_EDITOR
            EnsureNotDisposed();
            nativeHandle.Call("setVolume", volume);
#else
            throw CxAudioRuntime.UnsupportedPlatformException();
#endif
        }

        public void Dispose()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            if (nativeHandle != null)
            {
                nativeHandle.Dispose();
                nativeHandle = null;
                listenerProxy = null;
            }
#endif
        }

#if UNITY_ANDROID && !UNITY_EDITOR
        private void EnsureNotDisposed()
        {
            if (nativeHandle == null)
            {
                throw new ObjectDisposedException("BackgroundAudioHandle");
            }

            GC.KeepAlive(listenerProxy);
        }
#endif
    }

    public sealed class StickyPlayerHandle : IDisposable
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        private AndroidJavaObject nativeHandle;
        private object listenerProxy;

        internal StickyPlayerHandle(AndroidJavaObject nativeHandle, object listenerProxy)
        {
            if (nativeHandle == null)
            {
                throw new ArgumentNullException("nativeHandle");
            }

            this.nativeHandle = nativeHandle;
            this.listenerProxy = listenerProxy;
        }
#endif

        public bool IsShowing
        {
            get
            {
#if UNITY_ANDROID && !UNITY_EDITOR
                EnsureNotDisposed();
                return nativeHandle.Call<bool>("isShowing");
#else
                return false;
#endif
            }
        }

        public void Dismiss()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            EnsureNotDisposed();
            nativeHandle.Call("dismiss");
#else
            throw CxAudioRuntime.UnsupportedPlatformException();
#endif
        }

        public void Dispose()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            if (nativeHandle != null)
            {
                nativeHandle.Dispose();
                nativeHandle = null;
                listenerProxy = null;
            }
#endif
        }

#if UNITY_ANDROID && !UNITY_EDITOR
        private void EnsureNotDisposed()
        {
            if (nativeHandle == null)
            {
                throw new ObjectDisposedException("StickyPlayerHandle");
            }

            GC.KeepAlive(listenerProxy);
        }
#endif
    }

    public sealed class FullscreenInterstitialHandle : IDisposable
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        private AndroidJavaObject nativeHandle;
        private object listenerProxy;

        internal FullscreenInterstitialHandle(AndroidJavaObject nativeHandle, object listenerProxy)
        {
            if (nativeHandle == null)
            {
                throw new ArgumentNullException("nativeHandle");
            }

            this.nativeHandle = nativeHandle;
            this.listenerProxy = listenerProxy;
        }
#endif

        public bool IsShowing
        {
            get
            {
#if UNITY_ANDROID && !UNITY_EDITOR
                EnsureNotDisposed();
                return nativeHandle.Call<bool>("isShowing");
#else
                return false;
#endif
            }
        }

        public void Dismiss()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            EnsureNotDisposed();
            nativeHandle.Call("dismiss");
#else
            throw CxAudioRuntime.UnsupportedPlatformException();
#endif
        }

        public void Dispose()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            if (nativeHandle != null)
            {
                nativeHandle.Dispose();
                nativeHandle = null;
                listenerProxy = null;
            }
#endif
        }

#if UNITY_ANDROID && !UNITY_EDITOR
        private void EnsureNotDisposed()
        {
            if (nativeHandle == null)
            {
                throw new ObjectDisposedException("FullscreenInterstitialHandle");
            }

            GC.KeepAlive(listenerProxy);
        }
#endif
    }
}
