package com.cx.audio.android.internal;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

public final class VastParser {
    private static final int TARGET_COMPANION_WIDTH = 300;
    private static final int TARGET_COMPANION_HEIGHT = 250;

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

    public String parseWrapperUrl(String vastXml) throws Exception {
        Document document = parseDocument(vastXml);
        String wrapperUrl = textOf(firstElement(document, "VASTAdTagURI"));
        if (wrapperUrl == null) {
            return null;
        }
        return wrapperUrl;
    }

    private static Document parseDocument(String vastXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);
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
        if (creativeType != null && creativeType.toLowerCase().startsWith("image/")) {
            return true;
        }

        String url = staticResourceUrl.toLowerCase();
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
