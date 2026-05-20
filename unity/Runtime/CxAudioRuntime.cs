using System;
using System.Threading;
using UnityEngine;

namespace Cx.Audio.Unity
{
    internal static class CxAudioRuntime
    {
        internal const string UnsupportedPlatformMessage =
            "CX Audio Unity bindings currently support Android player builds only.";

        internal static PlatformNotSupportedException UnsupportedPlatformException()
        {
            return new PlatformNotSupportedException(UnsupportedPlatformMessage);
        }

        internal static void Dispatch(SynchronizationContext context, Action action)
        {
            if (action == null)
            {
                return;
            }

            if (context == null || SynchronizationContext.Current == context)
            {
                Invoke(action);
                return;
            }

            context.Post(_ => Invoke(action), null);
        }

        private static void Invoke(Action action)
        {
            try
            {
                action();
            }
            catch (Exception error)
            {
                Debug.LogException(error);
            }
        }
    }
}
