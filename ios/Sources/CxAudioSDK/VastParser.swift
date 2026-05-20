import Foundation

internal final class VastParser {
    func parse(data: Data) throws -> VastDocument {
        let delegate = VastXMLParserDelegate()
        let parser = XMLParser(data: data)
        parser.delegate = delegate

        guard parser.parse() else {
            throw parser.parserError ?? CxAudioError.invalidOption("VAST XML could not be parsed")
        }

        return VastDocument(
            mediaFiles: delegate.mediaFiles,
            companions: delegate.companions,
            wrapperURLString: delegate.wrapperURLString
        )
    }
}

private final class VastXMLParserDelegate: NSObject, XMLParserDelegate {
    private(set) var mediaFiles: [VastMediaFileCandidate] = []
    private(set) var companions: [VastCompanionCandidate] = []
    private(set) var wrapperURLString: String?

    private var currentText = ""
    private var mediaFileAttributes: [String: String]?
    private var companionAttributes: [String: String]?
    private var staticResourceCreativeType: String?
    private var staticResourceURLString: String?
    private var clickThroughURLString: String?

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?,
        attributes attributeDict: [String: String] = [:]
    ) {
        currentText = ""

        switch elementName {
        case "MediaFile":
            mediaFileAttributes = normalizedAttributes(attributeDict)
        case "Companion":
            companionAttributes = normalizedAttributes(attributeDict)
            staticResourceCreativeType = nil
            staticResourceURLString = nil
            clickThroughURLString = nil
        case "StaticResource":
            if companionAttributes != nil {
                staticResourceCreativeType = attributeDict["creativeType"]?.lowercased()
            }
        default:
            break
        }
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        currentText += string
    }

    func parser(
        _ parser: XMLParser,
        didEndElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?
    ) {
        let text = currentText.trimmingCharacters(in: .whitespacesAndNewlines)

        switch elementName {
        case "MediaFile":
            if let mediaFileAttributes, !text.isEmpty {
                mediaFiles.append(VastMediaFileCandidate(
                    urlString: text,
                    type: mediaFileAttributes["type"]?.lowercased(),
                    delivery: mediaFileAttributes["delivery"]?.lowercased(),
                    bitrate: intValue(mediaFileAttributes["bitrate"]),
                    width: intValue(mediaFileAttributes["width"]),
                    height: intValue(mediaFileAttributes["height"])
                ))
            }
            mediaFileAttributes = nil
        case "StaticResource":
            if companionAttributes != nil, !text.isEmpty {
                staticResourceURLString = text
            }
        case "CompanionClickThrough":
            if companionAttributes != nil, !text.isEmpty {
                clickThroughURLString = text
            }
        case "Companion":
            if let companionAttributes, let staticResourceURLString {
                companions.append(VastCompanionCandidate(
                    staticResourceURLString: staticResourceURLString,
                    staticResourceCreativeType: staticResourceCreativeType,
                    clickThroughURLString: clickThroughURLString,
                    width: intValue(companionAttributes["width"]),
                    height: intValue(companionAttributes["height"])
                ))
            }
            companionAttributes = nil
            staticResourceCreativeType = nil
            staticResourceURLString = nil
            clickThroughURLString = nil
        case "VASTAdTagURI":
            if !text.isEmpty {
                wrapperURLString = text
            }
        default:
            break
        }

        currentText = ""
    }

    private func normalizedAttributes(_ attributes: [String: String]) -> [String: String] {
        attributes.reduce(into: [:]) { result, entry in
            result[entry.key] = entry.value.trimmingCharacters(in: .whitespacesAndNewlines)
        }
    }

    private func intValue(_ value: String?) -> Int {
        guard let value else {
            return 0
        }
        return Int(value) ?? 0
    }
}
