namespace Cx.Audio.Unity
{
    public sealed class CxAudioError
    {
        public CxAudioError(string message, string errorClass)
        {
            Message = string.IsNullOrEmpty(message) ? "Unknown error" : message;
            ErrorClass = errorClass;
        }

        public string Message { get; private set; }

        public string ErrorClass { get; private set; }

        public override string ToString()
        {
            return string.IsNullOrEmpty(ErrorClass) ? Message : ErrorClass + ": " + Message;
        }

#if UNITY_ANDROID && !UNITY_EDITOR
        internal static CxAudioError FromJavaThrowable(UnityEngine.AndroidJavaObject throwable)
        {
            if (throwable == null)
            {
                return new CxAudioError("Unknown Android error", null);
            }

            string message = throwable.Call<string>("getMessage");
            if (string.IsNullOrEmpty(message))
            {
                message = throwable.Call<string>("toString");
            }

            string errorClass = null;
            using (UnityEngine.AndroidJavaObject javaClass = throwable.Call<UnityEngine.AndroidJavaObject>("getClass"))
            {
                if (javaClass != null)
                {
                    errorClass = javaClass.Call<string>("getName");
                }
            }

            return new CxAudioError(message, errorClass);
        }
#endif
    }
}
