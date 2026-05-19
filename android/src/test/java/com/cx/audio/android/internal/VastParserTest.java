package com.cx.audio.android.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VastParserTest {
    private final VastParser parser = new VastParser();

    @Test
    public void stickyPlayerCompanionParserPrefersClosestThreeHundredByTwoFiftyImage()
            throws Exception {
        VastCompanion companion = parser.parseBestCompanion(
                vastWithCompanions(
                        companion("https://cdn.example.com/small.png", "image/png", 120, 90),
                        companion("https://cdn.example.com/target.jpg", "image/jpeg", 300, 250),
                        companion("https://cdn.example.com/wide.gif", "image/gif", 728, 90)));

        assertEquals("https://cdn.example.com/target.jpg", companion.getStaticResourceUrl());
        assertEquals(300, companion.getWidth());
        assertEquals(250, companion.getHeight());
        assertEquals("https://click.example.com", companion.getClickThroughUrl());
    }

    @Test
    public void backgroundAudioParserPrefersProgressiveCommonAudioMedia() throws Exception {
        VastMediaFile mediaFile = parser.parseBestAudioMediaFile(
                vastWithMediaFiles(
                        mediaFile("https://cdn.example.com/audio.ogg", "audio/ogg", "streaming", 128, 0, 0),
                        mediaFile("https://cdn.example.com/audio.mp3", "audio/mpeg", "progressive", 128, 0, 0),
                        mediaFile("https://cdn.example.com/audio.wav", "audio/wav", "progressive", 256, 0, 0)));

        assertEquals("https://cdn.example.com/audio.mp3", mediaFile.getUrl());
        assertEquals("audio/mpeg", mediaFile.getType());
        assertEquals("progressive", mediaFile.getDelivery());
        assertEquals(128, mediaFile.getBitrate());
    }

    @Test
    public void fullscreenInterstitialParserPrefersProgressiveMp4VisualMedia()
            throws Exception {
        VastMediaFile mediaFile = parser.parseBestInterstitialMediaFile(
                vastWithMediaFiles(
                        mediaFile("https://cdn.example.com/slate.png", "image/png", "progressive", 0, 1280, 720),
                        mediaFile("https://cdn.example.com/ad.webm", "video/webm", "streaming", 1000, 1280, 720),
                        mediaFile("https://cdn.example.com/ad.mp4", "video/mp4", "progressive", 1000, 1280, 720)));

        assertEquals("https://cdn.example.com/ad.mp4", mediaFile.getUrl());
        assertEquals("video/mp4", mediaFile.getType());
        assertEquals(1280, mediaFile.getWidth());
        assertEquals(720, mediaFile.getHeight());
        assertTrue(mediaFile.isVideo());
    }

    private static String vastWithCompanions(String... companions) {
        StringBuilder builder = new StringBuilder();
        builder.append("<VAST><Ad><InLine><Creatives><Creative><CompanionAds>");
        for (String companion : companions) {
            builder.append(companion);
        }
        builder.append("</CompanionAds></Creative></Creatives></InLine></Ad></VAST>");
        return builder.toString();
    }

    private static String vastWithMediaFiles(String... mediaFiles) {
        StringBuilder builder = new StringBuilder();
        builder.append("<VAST><Ad><InLine><Creatives><Creative><Linear><MediaFiles>");
        for (String mediaFile : mediaFiles) {
            builder.append(mediaFile);
        }
        builder.append("</MediaFiles></Linear></Creative></Creatives></InLine></Ad></VAST>");
        return builder.toString();
    }

    private static String companion(String url, String creativeType, int width, int height) {
        return "<Companion width=\"" + width + "\" height=\"" + height + "\">"
                + "<StaticResource creativeType=\"" + creativeType + "\">" + url + "</StaticResource>"
                + "<CompanionClickThrough>https://click.example.com</CompanionClickThrough>"
                + "</Companion>";
    }

    private static String mediaFile(
            String url,
            String type,
            String delivery,
            int bitrate,
            int width,
            int height) {
        return "<MediaFile type=\"" + type + "\" delivery=\"" + delivery + "\" bitrate=\"" + bitrate
                + "\" width=\"" + width + "\" height=\"" + height + "\">"
                + url
                + "</MediaFile>";
    }
}
