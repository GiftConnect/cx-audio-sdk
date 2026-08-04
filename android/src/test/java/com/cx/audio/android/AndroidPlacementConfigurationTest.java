package com.cx.audio.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AndroidPlacementConfigurationTest {
    @Test
    public void stickyPlayerRequestAppliesNetworkConfiguration() {
        StickyPlayerRequest request = StickyPlayerRequest.builder("https://ads.example.com/vast.xml")
                .connectTimeoutMs(1000)
                .readTimeoutMs(2000)
                .build();

        assertEquals("https://ads.example.com/vast.xml", request.getVastTagUrl().toString());
        assertFalse(request.getVastTagUrl().toString().contains("sdkpt="));
        assertEquals(1000, request.getConnectTimeoutMs());
        assertEquals(2000, request.getReadTimeoutMs());
        assertEquals("cx-audio-sdk-android/0.1", request.getUserAgent());
    }

    @Test
    public void backgroundAudioRequestAppliesNetworkConfiguration() {
        BackgroundAudioRequest request = BackgroundAudioRequest.builder("http://ads.example.com/vast.xml")
                .connectTimeoutMs(1500)
                .readTimeoutMs(2500)
                .build();

        assertEquals("http://ads.example.com/vast.xml", request.getVastTagUrl().toString());
        assertFalse(request.getVastTagUrl().toString().contains("sdkpt="));
        assertEquals(1500, request.getConnectTimeoutMs());
        assertEquals(2500, request.getReadTimeoutMs());
        assertEquals("cx-audio-sdk-android/0.1", request.getUserAgent());
    }

    @Test
    public void fullscreenInterstitialRequestAppliesNetworkConfiguration() {
        FullscreenInterstitialRequest request = FullscreenInterstitialRequest
                .builder("https://ads.example.com/interstitial.xml")
                .connectTimeoutMs(1200)
                .readTimeoutMs(2200)
                .build();

        assertEquals("https://ads.example.com/interstitial.xml", request.getVastTagUrl().toString());
        assertFalse(request.getVastTagUrl().toString().contains("sdkpt="));
        assertEquals(1200, request.getConnectTimeoutMs());
        assertEquals(2200, request.getReadTimeoutMs());
        assertEquals("cx-audio-sdk-android/0.1", request.getUserAgent());
    }

    @Test
    public void requestsRejectBlankAndUnsupportedUrls() {
        assertThrows(IllegalArgumentException.class, () -> StickyPlayerRequest.builder(" "));
        assertThrows(IllegalArgumentException.class, () -> BackgroundAudioRequest.builder("file:///tmp/vast.xml"));
        assertThrows(IllegalArgumentException.class, () -> FullscreenInterstitialRequest.builder("ftp://example.com/vast.xml"));
    }

    @Test
    public void consumableAdRequestBuildsEncodedVastUrl() {
        ConsumableAdRequest consumableRequest = ConsumableAdRequest.builder("abc123")
                .appName("Test App")
                .appBundle("com.example.app")
                .appStoreUrl("https://apps.apple.com/app/id123")
                .limitAdTracking(false)
                .gdpr(true)
                .gdprConsent("consent string")
                .build();

        String url = consumableRequest.toURLString();
        assertTrue(url.startsWith("https://2.e.serverbid.com/mpb/get?"));
        assertTrue(url.contains("s=abc123"));
        assertTrue(url.contains("rt=vast"));
        assertTrue(url.contains("secure=1"));
        assertTrue(url.matches(".*(?:\\?|&)cb=\\d+(?:&.*)?"));
        assertTrue(url.contains("appname=Test%20App"));
        assertTrue(url.contains("appbundle=com.example.app"));
        assertTrue(url.contains("appstoreurl=https%3A%2F%2Fapps.apple.com%2Fapp%2Fid123"));
        assertTrue(url.contains("lmt=0"));
        assertTrue(url.contains("gdpr=1"));
        assertTrue(url.contains("gdpr_consent=consent%20string"));
        assertTrue(url.contains("sdk=Consumable%20Audio"));
        assertTrue(url.contains("sdkv=0.1.0"));
        assertTrue(url.contains("v_delay=-1"));
        assertTrue(url.contains("v_delivery=2"));
        assertTrue(url.contains("v_mimes=audio%2Fmpeg%2Caudio%2Fmp4%2Caudio%2Faac"));
        assertTrue(url.contains("v_cmp_sizes=300x250"));
        assertTrue(url.contains("v_pm=1"));
        assertTrue(url.contains("c_audio_feed_type=6"));

        StickyPlayerRequest request = StickyPlayerRequest.builder(consumableRequest).build();
        String stickyUrl = request.getVastTagUrl().toString();
        assertEquals(
                consumableRequest.toURLString(
                        ConsumableAdRequest.PlacementType.STICKY_PLAYER,
                        null,
                        "cx-audio-sdk-android/0.1"),
                stickyUrl);
        assertTrue(stickyUrl.contains("u=cx-audio-sdk-android%2F0.1"));
        assertTrue(stickyUrl.contains("sdkpt=2"));
        assertTrue(stickyUrl.contains("v_plcmt=2"));
        assertTrue(stickyUrl.contains("v_linearity=2"));
        assertTrue(stickyUrl.contains("v_min_bitrate=64"));
        assertTrue(stickyUrl.contains("v_max_bitrate=320"));
    }

    @Test
    public void consumableAdRequestEncodesConfiguredSdkNameAndAutomaticVersion() {
        ConsumableAdRequest consumableRequest = ConsumableAdRequest.builder("abc123")
                .sdkName("Radio.com Audio / SDK")
                .build();

        String url = consumableRequest.toURLString();

        assertTrue(url.contains("sdk=Radio.com%20Audio%20%2F%20SDK"));
        assertTrue(url.contains("sdkv=0.1.0"));
    }

    @Test
    public void consumableAdRequestDefaultsCacheBusterToMillisecondTimestamp() {
        ConsumableAdRequest consumableRequest = ConsumableAdRequest.builder("abc123")
                .build();

        String url = consumableRequest.toURLString();

        assertTrue(url.matches(".*(?:\\?|&)cb=\\d+(?:&.*)?"));
    }

    @Test
    public void consumableAdRequestAppliesInterstitialAutomaticParameters() {
        ConsumableAdRequest consumableRequest = ConsumableAdRequest.builder("abc123").build();

        FullscreenInterstitialRequest request = FullscreenInterstitialRequest.builder(consumableRequest).build();
        String url = request.getVastTagUrl().toString();

        assertTrue(url.contains("u=cx-audio-sdk-android%2F0.1"));
        assertTrue(url.contains("sdkpt=3"));
        assertTrue(url.contains("v_plcmt=3"));
        assertTrue(url.contains("v_linearity=1"));
        assertTrue(url.contains("v_delivery=2"));
        assertTrue(url.contains("v_delay=-1"));
        assertTrue(url.contains("v_pm=1"));
        assertTrue(url.contains("v_max_dur=90"));
        assertTrue(url.contains("v_min_bitrate=64"));
        assertTrue(url.contains("v_max_bitrate=2500"));
        assertTrue(url.contains("v_cmp_types=1"));
        assertTrue(url.contains("v_cmp_sizes=300x250"));
        assertTrue(url.contains("v_protocols=2%2C3%2C5%2C6%2C7%2C8"));
        assertTrue(url.contains("c_audio_feed_type=6"));
    }

    @Test
    public void consumableAdRequestMapsBackgroundAudioSdkPlacementType() {
        ConsumableAdRequest consumableRequest = ConsumableAdRequest.builder("abc123").build();

        BackgroundAudioRequest request = BackgroundAudioRequest.builder(consumableRequest).build();

        assertTrue(request.getVastTagUrl().toString().contains("sdkpt=1"));
    }

    @Test
    public void stickyPlayerOptionsValidatePlacementControls() {
        StickyPlayerOptions options = StickyPlayerOptions.builder()
                .position(StickyPlayerPosition.TOP)
                .showCloseButton(false)
                .volume(0.35f)
                .minDeviceVolume(0.2f)
                .build();

        assertEquals(StickyPlayerPosition.TOP, options.getPosition());
        assertFalse(options.shouldShowCloseButton());
        assertEquals(0.35f, options.getVolume(), 0.0f);
        assertEquals(0.2f, options.getMinDeviceVolume(), 0.0f);
        assertThrows(IllegalArgumentException.class, () -> StickyPlayerOptions.builder().volume(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> StickyPlayerOptions.builder().volume(1.1f));
        assertThrows(IllegalArgumentException.class, () -> StickyPlayerOptions.builder().volume(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> StickyPlayerOptions.builder().minDeviceVolume(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> StickyPlayerOptions.builder().minDeviceVolume(1.1f));
        assertThrows(IllegalArgumentException.class, () -> StickyPlayerOptions.builder().minDeviceVolume(Float.NaN));
    }

    @Test
    public void backgroundAudioOptionsValidateVolume() {
        BackgroundAudioOptions options = BackgroundAudioOptions.builder()
                .volume(0.4f)
                .minDeviceVolume(0.15f)
                .build();

        assertEquals(0.4f, options.getVolume(), 0.0f);
        assertEquals(0.15f, options.getMinDeviceVolume(), 0.0f);
        assertThrows(IllegalArgumentException.class, () -> BackgroundAudioOptions.builder().volume(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> BackgroundAudioOptions.builder().volume(1.1f));
        assertThrows(IllegalArgumentException.class, () -> BackgroundAudioOptions.builder().volume(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> BackgroundAudioOptions.builder().minDeviceVolume(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> BackgroundAudioOptions.builder().minDeviceVolume(1.1f));
        assertThrows(IllegalArgumentException.class, () -> BackgroundAudioOptions.builder().minDeviceVolume(Float.NaN));
    }

    @Test
    public void fullscreenInterstitialOptionsValidateLayoutControls() {
        FullscreenInterstitialOptions options = FullscreenInterstitialOptions.builder()
                .showCloseButton(false)
                .autoPlayVideo(false)
                .volume(0.45f)
                .minDeviceVolume(0.25f)
                .build();

        assertFalse(options.shouldShowCloseButton());
        assertFalse(options.shouldAutoPlayVideo());
        assertEquals(0.45f, options.getVolume(), 0.0f);
        assertEquals(0.25f, options.getMinDeviceVolume(), 0.0f);

        FullscreenInterstitialOptions defaults = FullscreenInterstitialOptions.builder().build();
        assertTrue(defaults.shouldShowCloseButton());
        assertTrue(defaults.shouldAutoPlayVideo());

        assertThrows(IllegalArgumentException.class, () -> FullscreenInterstitialOptions.builder().volume(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> FullscreenInterstitialOptions.builder().volume(1.1f));
        assertThrows(IllegalArgumentException.class, () -> FullscreenInterstitialOptions.builder().volume(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> FullscreenInterstitialOptions.builder().minDeviceVolume(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> FullscreenInterstitialOptions.builder().minDeviceVolume(1.1f));
        assertThrows(IllegalArgumentException.class, () -> FullscreenInterstitialOptions.builder().minDeviceVolume(Float.NaN));
    }
}
