package com.cx.audio.android.internal;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

public final class VastParser {
    private static final int TARGET_COMPANION_WIDTH = 300;
    private static final int TARGET_COMPANION_HEIGHT = 250;
    private static final int TARGET_INTERSTITIAL_WIDTH = 1280;
    private static final int TARGET_INTERSTITIAL_HEIGHT = 720;

    public VastCompanion parseBestCompanion(String vastXml) throws Exception {
        Document document = parseDocument(vastXml);
        NodeList companions = document.getElementsByTagName("Companion");

        VastCompanion bestCompanion = null;
        int bestScore = Integer.MAX_VALUE;
        for (int i = 0; i < companions.getLength(); i++) {
            Node node = companions.item(i);
            if (!(node instanceof Element)) {
                continue;
            }

            Element companionElement = (Element) node;
            Element staticResourceElement = firstChildElement(companionElement, "StaticResource");
            String staticResourceUrl = textOf(staticResourceElement);
            if (staticResourceUrl == null || !isImageResource(staticResourceElement, staticResourceUrl)) {
                continue;
            }

            int width = parseInt(companionElement.getAttribute("width"));
            int height = parseInt(companionElement.getAttribute("height"));
            String clickThroughUrl = textOf(firstChildElement(companionElement, "CompanionClickThrough"));
            int score = Math.abs(TARGET_COMPANION_WIDTH - width)
                    + Math.abs(TARGET_COMPANION_HEIGHT - height);

            if (bestCompanion == null || score < bestScore) {
                bestScore = score;
                bestCompanion = new VastCompanion(staticResourceUrl, clickThroughUrl, width, height);
            }
        }

        if (bestCompanion == null) {
            throw new IllegalArgumentException("VAST response did not include an image companion ad");
        }
        return bestCompanion;
    }

    public VastMediaFile parseBestAudioMediaFile(String vastXml) throws Exception {
        Document document = parseDocument(vastXml);
        NodeList mediaFiles = document.getElementsByTagName("MediaFile");

        VastMediaFile bestMediaFile = null;
        int bestScore = Integer.MAX_VALUE;
        for (int i = 0; i < mediaFiles.getLength(); i++) {
            Node node = mediaFiles.item(i);
            if (!(node instanceof Element)) {
                continue;
            }

            Element mediaFileElement = (Element) node;
            String mediaFileUrl = textOf(mediaFileElement);
            String type = normalizedAttribute(mediaFileElement, "type");
            if (mediaFileUrl == null || !isAudioResource(type, mediaFileUrl)) {
                continue;
            }

            String delivery = normalizedAttribute(mediaFileElement, "delivery");
            int bitrate = parseBitrate(mediaFileElement);
            int score = scoreAudioMediaFile(type, delivery, bitrate);
            if (bestMediaFile == null || score < bestScore) {
                bestScore = score;
                bestMediaFile = new VastMediaFile(mediaFileUrl, type, delivery, bitrate);
            }
        }

        if (bestMediaFile == null) {
            throw new IllegalArgumentException("VAST response did not include an audio media file");
        }
        return bestMediaFile;
    }

    public VastMediaFile parseBestInterstitialMediaFile(String vastXml) throws Exception {
        Document document = parseDocument(vastXml);
        NodeList mediaFiles = document.getElementsByTagName("MediaFile");

        VastMediaFile bestMediaFile = null;
        int bestScore = Integer.MAX_VALUE;
        for (int i = 0; i < mediaFiles.getLength(); i++) {
            Node node = mediaFiles.item(i);
            if (!(node instanceof Element)) {
                continue;
            }

            Element mediaFileElement = (Element) node;
            String mediaFileUrl = textOf(mediaFileElement);
            String type = normalizedAttribute(mediaFileElement, "type");
            if (mediaFileUrl == null || !isVisualResource(type, mediaFileUrl)) {
                continue;
            }

            String delivery = normalizedAttribute(mediaFileElement, "delivery");
            int bitrate = parseBitrate(mediaFileElement);
            int width = parseInt(mediaFileElement.getAttribute("width"));
            int height = parseInt(mediaFileElement.getAttribute("height"));
            int score = scoreInterstitialMediaFile(type, delivery, width, height);
            if (bestMediaFile == null || score < bestScore) {
                bestScore = score;
                bestMediaFile = new VastMediaFile(mediaFileUrl, type, delivery, bitrate, width, height);
            }
        }

        if (bestMediaFile == null) {
            throw new IllegalArgumentException("VAST response did not include an interstitial media file");
        }
        return bestMediaFile;
    }

    public String parseWrapperUrl(String vastXml) throws Exception {
        Document document = parseDocument(vastXml);
        String wrapperUrl = textOf(firstElement(document, "VASTAdTagURI"));
        if (wrapperUrl == null) {
            return null;
        }
        return wrapperUrl;
    }

    public VastTracking parseTracking(String vastXml) throws Exception {
        Document document = parseDocument(vastXml);
        List<String> impressionUrls = new ArrayList<>();
        Map<TrackingEvent, List<String>> eventUrls = new EnumMap<>(TrackingEvent.class);
        List<String> clickTrackingUrls = new ArrayList<>();

        collectElementText(document.getElementsByTagName("Impression"), impressionUrls);
        collectClickTracking(document.getElementsByTagName("ClickTracking"), clickTrackingUrls);
        collectTrackingEvents(document.getElementsByTagName("Tracking"), eventUrls);

        return new VastTracking(
                firstAdIdentifier(document),
                impressionUrls,
                eventUrls,
                clickTrackingUrls);
    }

    private static Document parseDocument(String vastXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(false);
        trySetXIncludeAware(factory, false);
        trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);

        return factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(vastXml)));
    }

    private static void trySetFeature(
            DocumentBuilderFactory factory,
            String feature,
            boolean enabled) {
        try {
            factory.setFeature(feature, enabled);
        } catch (Exception ignored) {
            // Some Android XML parsers do not expose every hardening feature.
        }
    }

    private static void trySetXIncludeAware(DocumentBuilderFactory factory, boolean aware) {
        try {
            factory.setXIncludeAware(aware);
        } catch (Exception ignored) {
            // Some Android XML parsers report unsupported XInclude configuration.
        }
    }

    private static Element firstChildElement(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                return (Element) node;
            }
        }
        return null;
    }

    private static Element firstElement(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                return (Element) node;
            }
        }
        return null;
    }

    private static String firstAdIdentifier(Document document) {
        NodeList nodes = document.getElementsByTagName("Ad");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            String id = ((Element) node).getAttribute("id");
            if (id != null) {
                id = id.trim();
                if (!id.isEmpty()) {
                    return id;
                }
            }
        }
        return null;
    }

    private static void collectElementText(NodeList nodes, List<String> output) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                String text = textOf((Element) node);
                if (text != null) {
                    output.add(text);
                }
            }
        }
    }

    private static void collectClickTracking(NodeList nodes, List<String> output) {
        collectElementText(nodes, output);
    }

    private static void collectTrackingEvents(
            NodeList nodes,
            Map<TrackingEvent, List<String>> output) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }

            Element element = (Element) node;
            TrackingEvent event = TrackingEvent.fromVastEvent(element.getAttribute("event"));
            String url = textOf(element);
            if (event == null || url == null) {
                continue;
            }
            List<String> urls = output.get(event);
            if (urls == null) {
                urls = new ArrayList<>();
                output.put(event, urls);
            }
            urls.add(url);
        }
    }

    private static String textOf(Element element) {
        if (element == null) {
            return null;
        }
        String text = element.getTextContent();
        if (text == null) {
            return null;
        }
        text = text.trim();
        return text.isEmpty() ? null : text;
    }

    private static boolean isImageResource(Element staticResourceElement, String staticResourceUrl) {
        String creativeType = staticResourceElement.getAttribute("creativeType");
        if (creativeType != null && creativeType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }

        String url = staticResourceUrl.toLowerCase(Locale.ROOT);
        int queryStart = url.indexOf('?');
        if (queryStart >= 0) {
            url = url.substring(0, queryStart);
        }
        int fragmentStart = url.indexOf('#');
        if (fragmentStart >= 0) {
            url = url.substring(0, fragmentStart);
        }
        return url.endsWith(".jpg")
                || url.endsWith(".jpeg")
                || url.endsWith(".png")
                || url.endsWith(".gif")
                || url.endsWith(".webp");
    }

    private static boolean isAudioResource(String type, String mediaFileUrl) {
        if (type != null && type.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            return true;
        }

        String url = mediaFileUrl.toLowerCase(Locale.ROOT);
        int queryStart = url.indexOf('?');
        if (queryStart >= 0) {
            url = url.substring(0, queryStart);
        }
        int fragmentStart = url.indexOf('#');
        if (fragmentStart >= 0) {
            url = url.substring(0, fragmentStart);
        }
        return url.endsWith(".mp3")
                || url.endsWith(".m4a")
                || url.endsWith(".aac")
                || url.endsWith(".ogg")
                || url.endsWith(".oga")
                || url.endsWith(".wav");
    }

    private static boolean isVisualResource(String type, String mediaFileUrl) {
        if (type != null) {
            String normalizedType = type.toLowerCase(Locale.ROOT);
            if (normalizedType.startsWith("video/") || normalizedType.startsWith("image/")) {
                return true;
            }
        }

        String url = mediaFileUrl.toLowerCase(Locale.ROOT);
        int queryStart = url.indexOf('?');
        if (queryStart >= 0) {
            url = url.substring(0, queryStart);
        }
        int fragmentStart = url.indexOf('#');
        if (fragmentStart >= 0) {
            url = url.substring(0, fragmentStart);
        }
        return url.endsWith(".mp4")
                || url.endsWith(".m4v")
                || url.endsWith(".webm")
                || url.endsWith(".3gp")
                || url.endsWith(".jpg")
                || url.endsWith(".jpeg")
                || url.endsWith(".png")
                || url.endsWith(".gif")
                || url.endsWith(".webp");
    }

    private static int scoreAudioMediaFile(String type, String delivery, int bitrate) {
        int score = 0;
        if (!"progressive".equals(delivery)) {
            score += 50;
        }

        if ("audio/mpeg".equals(type)) {
            score += 0;
        } else if ("audio/mp4".equals(type) || "audio/aac".equals(type)) {
            score += 5;
        } else if ("audio/ogg".equals(type)) {
            score += 20;
        } else if ("audio/wav".equals(type) || "audio/x-wav".equals(type)) {
            score += 30;
        } else if (type == null) {
            score += 15;
        } else {
            score += 25;
        }

        if (bitrate > 0) {
            score += Math.abs(128 - bitrate) / 8;
        } else {
            score += 10;
        }
        return score;
    }

    private static int scoreInterstitialMediaFile(String type, String delivery, int width, int height) {
        int score = 0;
        if (!"progressive".equals(delivery)) {
            score += 50;
        }

        if ("video/mp4".equals(type)) {
            score += 0;
        } else if ("image/jpeg".equals(type) || "image/png".equals(type)) {
            score += 10;
        } else if (type != null && type.startsWith("video/")) {
            score += 15;
        } else if (type != null && type.startsWith("image/")) {
            score += 20;
        } else if (type == null) {
            score += 25;
        } else {
            score += 30;
        }

        if (width > 0 && height > 0) {
            score += Math.abs(TARGET_INTERSTITIAL_WIDTH - width) / 80;
            score += Math.abs(TARGET_INTERSTITIAL_HEIGHT - height) / 45;
        } else {
            score += 20;
        }
        return score;
    }

    private static String normalizedAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value.toLowerCase(Locale.ROOT);
    }

    private static int parseBitrate(Element mediaFileElement) {
        int bitrate = parseInt(mediaFileElement.getAttribute("bitrate"));
        if (bitrate > 0) {
            return bitrate;
        }

        int minBitrate = parseInt(mediaFileElement.getAttribute("minBitrate"));
        int maxBitrate = parseInt(mediaFileElement.getAttribute("maxBitrate"));
        if (minBitrate > 0 && maxBitrate > 0) {
            return (minBitrate + maxBitrate) / 2;
        }
        if (minBitrate > 0) {
            return minBitrate;
        }
        return maxBitrate;
    }

    private static int parseInt(String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
