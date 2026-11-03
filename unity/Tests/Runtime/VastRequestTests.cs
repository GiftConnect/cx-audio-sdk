using NUnit.Framework;

namespace Cx.Audio.Unity.Tests
{
    public sealed class VastRequestTests
    {
        [TestCase(ConsumablePlacement.BackgroundAudio, "1")]
        [TestCase(ConsumablePlacement.StickyPlayer, "2")]
        [TestCase(ConsumablePlacement.FullscreenInterstitial, "3")]
        public void StructuredRequestMapsPlacementToSdkpt(ConsumablePlacement placement, string code)
        {
            VastRequest request = new VastRequest(new ConsumableAdRequest("abc123"), placement);

            StringAssert.Contains("sdkpt=" + code, request.VastTagUrl);
        }

        [Test]
        public void StructuredRequestDefaultsToBackgroundAudioSdkpt()
        {
            VastRequest request = new VastRequest(new ConsumableAdRequest("abc123"));

            StringAssert.Contains("sdkpt=1", request.VastTagUrl);
            StringAssert.Contains("sdk=Consumable%20Audio", request.VastTagUrl);
            StringAssert.Contains("sdkv=0.1.3", request.VastTagUrl);
        }

        [Test]
        public void StructuredRequestEncodesConfiguredSdkNameAndAutomaticVersion()
        {
            ConsumableAdRequest consumableRequest = new ConsumableAdRequest("abc123")
                .SdkName("Radio.com Audio / SDK");

            VastRequest request = new VastRequest(consumableRequest);

            StringAssert.Contains("sdk=Radio.com%20Audio%20%2F%20SDK", request.VastTagUrl);
            StringAssert.Contains("sdkv=0.1.3", request.VastTagUrl);
        }

        [Test]
        public void RawVastRequestDoesNotModifyUrl()
        {
            const string rawUrl = "https://ads.example.com/vast.xml?custom=1";

            VastRequest request = new VastRequest(rawUrl);

            Assert.AreEqual(rawUrl, request.VastTagUrl);
            StringAssert.DoesNotContain("sdkpt=", request.VastTagUrl);
        }
    }
}
