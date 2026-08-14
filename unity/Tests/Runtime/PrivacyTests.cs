using NUnit.Framework;

namespace Cx.Audio.Unity.Tests
{
    public sealed class PrivacyTests
    {
        [Test]
        public void ConfigurationDefaultsToUnspecifiedWithoutAnOverride()
        {
            CxAudioConfiguration configuration = new CxAudioConfiguration();

            Assert.AreEqual(CxAgeTreatment.Unspecified, configuration.AgeTreatment);
            Assert.IsNull(configuration.PrivacyOverride);
        }

        [Test]
        public void ConfigurationAcceptsStronglyTypedQaOverride()
        {
            CxPrivacyOverride privacyOverride = new CxPrivacyOverride
            {
                GdprApplies = false,
                PersonalizedAdsAllowed = true,
                UsPrivacyOptOut = false,
                UsPrivacyString = "1YNN"
            };
            CxAudioConfiguration configuration = new CxAudioConfiguration
            {
                AgeTreatment = CxAgeTreatment.GeneralAudience,
                PrivacyOverride = privacyOverride
            };

            Assert.AreEqual(CxAgeTreatment.GeneralAudience, configuration.AgeTreatment);
            Assert.AreSame(privacyOverride, configuration.PrivacyOverride);
        }
    }
}
