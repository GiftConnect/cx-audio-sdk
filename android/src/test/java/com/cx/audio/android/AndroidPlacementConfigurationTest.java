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
                .userAgent("test-agent")
                .build();

        assertEquals("https://ads.example.com/vast.xml", request.getVastTagUrl().toString());
        assertEquals(1000, request.getConnectTimeoutMs());
        assertEquals(2000, request.getReadTimeoutMs());
        assertEquals("test-agent", request.getUserAgent());
    }

    @Test
    public void backgroundAudioRequestAppliesNetworkConfiguration() {
        BackgroundAudioRequest request = BackgroundAudioRequest.builder("http://ads.example.com/vast.xml")
                .connectTimeoutMs(1500)
                .readTimeoutMs(2500)
                .userAgent("background-test-agent")
                .build();

        assertEquals("http://ads.example.com/vast.xml", request.getVastTagUrl().toString());
        assertEquals(1500, request.getConnectTimeoutMs());
        assertEquals(2500, request.getReadTimeoutMs());
        assertEquals("background-test-agent", request.getUserAgent());
    }

    @Test
    public void fullscreenInterstitialRequestAppliesNetworkConfiguration() {
        FullscreenInterstitialRequest request = FullscreenInterstitialRequest
                .builder("https://ads.example.com/interstitial.xml")
                .connectTimeoutMs(1200)
                .readTimeoutMs(2200)
                .userAgent("fullscreen-test-agent")
                .build();

        assertEquals("https://ads.example.com/interstitial.xml", request.getVastTagUrl().toString());
        assertEquals(1200, request.getConnectTimeoutMs());
        assertEquals(2200, request.getReadTimeoutMs());
        assertEquals("fullscreen-test-agent", request.getUserAgent());
    }

    @Test
    public void requestsRejectBlankAndUnsupportedUrls() {
        assertThrows(IllegalArgumentException.class, () -> StickyPlayerRequest.builder(" "));
        assertThrows(IllegalArgumentException.class, () -> BackgroundAudioRequest.builder("file:///tmp/vast.xml"));
        assertThrows(IllegalArgumentException.class, () -> FullscreenInterstitialRequest.builder("ftp://example.com/vast.xml"));
    }

    @Test
    public void stickyPlayerOptionsValidatePlacementControls() {
        StickyPlayerOptions options = StickyPlayerOptions.builder()
                .position(StickyPlayerPosition.TOP)
                .heightDp(96)
                .horizontalPaddingDp(16)
                .showCloseButton(false)
                .build();

        assertEquals(StickyPlayerPosition.TOP, options.getPosition());
        assertEquals(96, options.getHeightDp());
        assertEquals(16, options.getHorizontalPaddingDp());
        assertFalse(options.shouldShowCloseButton());
        assertThrows(IllegalArgumentException.class, () -> StickyPlayerOptions.builder().heightDp(47));
        assertThrows(IllegalArgumentException.class, () -> StickyPlayerOptions.builder().horizontalPaddingDp(-1));
    }

    @Test
    public void backgroundAudioOptionsValidateVolume() {
        BackgroundAudioOptions options = BackgroundAudioOptions.builder()
                .volume(0.4f)
                .build();

        assertEquals(0.4f, options.getVolume(), 0.0f);
        assertThrows(IllegalArgumentException.class, () -> BackgroundAudioOptions.builder().volume(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> BackgroundAudioOptions.builder().volume(1.1f));
        assertThrows(IllegalArgumentException.class, () -> BackgroundAudioOptions.builder().volume(Float.NaN));
    }

    @Test
    public void fullscreenInterstitialOptionsValidateLayoutControls() {
        FullscreenInterstitialOptions options = FullscreenInterstitialOptions.builder()
                .showCloseButton(false)
                .companionHeightDp(180)
                .companionMaxWidthDp(320)
                .autoPlayVideo(false)
                .build();

        assertFalse(options.shouldShowCloseButton());
        assertEquals(180, options.getCompanionHeightDp());
        assertEquals(320, options.getCompanionMaxWidthDp());
        assertFalse(options.shouldAutoPlayVideo());

        FullscreenInterstitialOptions defaults = FullscreenInterstitialOptions.builder().build();
        assertTrue(defaults.shouldShowCloseButton());
        assertEquals(250, defaults.getCompanionHeightDp());
        assertEquals(300, defaults.getCompanionMaxWidthDp());
        assertTrue(defaults.shouldAutoPlayVideo());

        assertThrows(
                IllegalArgumentException.class,
                () -> FullscreenInterstitialOptions.builder().companionHeightDp(47));
        assertThrows(
                IllegalArgumentException.class,
                () -> FullscreenInterstitialOptions.builder().companionMaxWidthDp(47));
    }
}
