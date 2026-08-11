import Foundation

internal enum PrivacyRequestPurpose: Sendable {
    case adRequest
    case vastWrapper
    case creativeAsset
    case trackingBeacon
    case clickThrough
}

internal enum PrivacyEnforcer {
    private static let identifierKeys: Set<String> = [
        "idfa", "ifa", "gadid", "aid", "buyeruid", "buyer_id", "user_id", "userid"
    ]
    private static let locationKeys: Set<String> = ["lat", "long", "lon", "latitude", "longitude", "i", "ip"]
    private static let controlledKeys: Set<String> = [
        "lmt", "dnt", "coppa", "gdpr", "gdpr_consent", "us_privacy", "gpp", "gpp_sid"
    ]

    static func enforce(
        _ url: URL,
        purpose: PrivacyRequestPurpose,
        state: CxPrivacyState = PrivacyManager.shared.snapshot
    ) throws -> URL {
        guard url.scheme?.lowercased() == "https" else {
            throw CxAudioError.privacyBlocked("Outbound SDK requests must use HTTPS")
        }
        guard var components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            throw CxAudioError.invalidURL(url.absoluteString)
        }
        var items = components.queryItems ?? []
        items = items.compactMap { item in
            let key = item.name.lowercased()
            if controlledKeys.contains(key) { return nil }
            if !state.deviceIdentifiersAllowed && identifierKeys.contains(key) { return nil }
            if !state.preciseLocationAllowed && locationKeys.contains(key) { return nil }
            return URLQueryItem(name: item.name, value: resolveMacros(item.value, state: state))
        }
        if purpose == .adRequest || purpose == .vastWrapper || purpose == .trackingBeacon {
            set(&items, name: "lmt", value: state.deviceIdentifiersAllowed ? "0" : "1")
            set(&items, name: "dnt", value: state.personalizedAdsAllowed ? "0" : "1")
            if state.ageTreatment == .childDirected || state.ageTreatment == .underAgeOfConsent {
                set(&items, name: "coppa", value: "1")
            }
            if let gdprApplies = state.gdprApplies {
                set(&items, name: "gdpr", value: gdprApplies ? "1" : "0")
            }
            setOptional(&items, name: "gdpr_consent", value: state.tcfString)
            setOptional(&items, name: "us_privacy", value: state.usPrivacyString)
            setOptional(&items, name: "gpp", value: state.gppString)
            if !state.gppSectionIds.isEmpty {
                set(&items, name: "gpp_sid", value: state.gppSectionIds.map(String.init).joined(separator: ","))
            }
        }
        components.queryItems = items.isEmpty ? nil : items
        guard let result = components.url else { throw CxAudioError.invalidURL(url.absoluteString) }
        return result
    }

    private static func resolveMacros(_ value: String?, state: CxPrivacyState) -> String? {
        guard var value else { return nil }
        let replacements: [String: String] = [
            "[GDPR]": state.gdprApplies == true ? "1" : "0",
            "[GDPR_CONSENT]": state.tcfString ?? "",
            "[US_PRIVACY]": state.usPrivacyString ?? "",
            "[GPP_STRING_OMID]": state.gppString ?? "",
            "[GPP_SID]": state.gppSectionIds.map(String.init).joined(separator: ",")
        ]
        for (macro, replacement) in replacements { value = value.replacingOccurrences(of: macro, with: replacement) }
        return value
    }

    private static func set(_ items: inout [URLQueryItem], name: String, value: String) {
        items.removeAll { $0.name.caseInsensitiveCompare(name) == .orderedSame }
        items.append(URLQueryItem(name: name, value: value))
    }

    private static func setOptional(_ items: inout [URLQueryItem], name: String, value: String?) {
        items.removeAll { $0.name.caseInsensitiveCompare(name) == .orderedSame }
        if let value, !value.isEmpty { items.append(URLQueryItem(name: name, value: value)) }
    }
}
