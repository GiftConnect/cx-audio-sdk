namespace Cx.Audio.Unity
{
    public enum CxAgeTreatment
    {
        Unspecified,
        GeneralAudience,
        ChildDirected,
        UnderAgeOfConsent
    }

    public sealed class CxPrivacyOverride
    {
        public bool? GdprApplies { get; set; }
        public bool? PersonalizedAdsAllowed { get; set; }
        public bool? UsPrivacyOptOut { get; set; }
        public string TcfString { get; set; }
        public string AdditionalConsent { get; set; }
        public string GppString { get; set; }
        public string UsPrivacyString { get; set; }
    }

    public sealed class CxAudioConfiguration
    {
        public CxAgeTreatment AgeTreatment { get; set; } = CxAgeTreatment.Unspecified;
        public CxPrivacyOverride PrivacyOverride { get; set; }
    }

    public static class CxAudio
    {
        public static void Initialize(CxAudioConfiguration configuration = null)
        {
            configuration = configuration ?? new CxAudioConfiguration();
#if UNITY_ANDROID && !UNITY_EDITOR
            CxAudioAndroid.Initialize(configuration);
#endif
        }
    }
}
