import Foundation

public struct ConsumableAdRequest: Equatable {
    private static let endpointURL = URL(string: "https://e.serverbid.com/mpb/get")!
    private static let defaultSDKName = "Consumable Audio"
    private static let sdkVersion = "0.1.3"

    let parameters: [String: String]

    public init(
        placementHash: String,
        ipAddress: String? = nil,
        locale: String? = nil,
        sdkName: String? = nil,
        contentCategories: [String] = [],
        blockedCategories: [String] = [],
        blockedAdvertisers: [String] = [],
        limitAdTracking: Bool? = nil,
        doNotTrack: Bool? = nil,
        appStoreURL: String? = nil,
        appName: String? = nil,
        appVersion: String? = nil,
        appBundle: String? = nil,
        idfa: String? = nil,
        aid: String? = nil,
        gadid: String? = nil,
        ifa: String? = nil,
        keywords: [String] = [],
        longitude: Double? = nil,
        latitude: Double? = nil,
        coppa: Bool? = nil,
        gdpr: Bool? = nil,
        gdprConsent: String? = nil,
        usPrivacy: String? = nil,
        gpp: String? = nil,
        gppSectionIds: [String] = [],
        contentId: String? = nil,
        contentGenre: [String] = [],
        contentTitle: String? = nil,
        contentDurationSeconds: Int? = nil,
        contentURL: String? = nil
    ) throws {
        guard !placementHash.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw CxAudioError.invalidOption("placementHash must not be blank")
        }
        var params: [String: String] = [
            "s": placementHash,
            "rt": "vast",
            "secure": "1",
            "cb": String(Int64(Date().timeIntervalSince1970 * 1000))
        ]

        put(&params, "i", ipAddress)
        put(&params, "locale", locale)
        put(&params, "sdk", sdkName)
        put(&params, "cat", joined(contentCategories))
        put(&params, "bcat", joined(blockedCategories))
        put(&params, "badv", joined(blockedAdvertisers))
        put(&params, "lmt", boolString(limitAdTracking))
        put(&params, "dnt", boolString(doNotTrack))
        put(&params, "appstoreurl", appStoreURL)
        put(&params, "appname", appName)
        put(&params, "appversion", appVersion)
        put(&params, "appbundle", appBundle)
        put(&params, "idfa", idfa)
        put(&params, "aid", aid)
        put(&params, "gadid", gadid)
        put(&params, "ifa", ifa)
        put(&params, "kw", joined(keywords))
        put(&params, "long", longitude.map(formatNumber))
        put(&params, "lat", latitude.map(formatNumber))
        put(&params, "coppa", boolString(coppa))
        put(&params, "gdpr", boolString(gdpr))
        put(&params, "gdpr_consent", gdprConsent)
        put(&params, "us_privacy", usPrivacy)
        put(&params, "gpp", gpp)
        put(&params, "gpp_sid", joined(gppSectionIds))
        put(&params, "c_id", contentId)
        put(&params, "c_genre", joined(contentGenre))
        put(&params, "c_title", contentTitle)
        put(&params, "c_dur", contentDurationSeconds.map(String.init))
        put(&params, "c_url", contentURL)
        self.parameters = params
    }

    func url(
        placement: ConsumablePlacement? = nil,
        userAgentParameter: String? = nil
    ) throws -> URL {
        var components = URLComponents(url: Self.endpointURL, resolvingAgainstBaseURL: false)
        components?.percentEncodedQuery = parametersWithAutomaticValues(
            placement: placement,
            userAgentParameter: userAgentParameter
        )
            .filter { !$0.value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .sorted { $0.key < $1.key }
            .map { "\(percentEncodeQueryComponent($0.key))=\(percentEncodeQueryComponent($0.value))" }
            .joined(separator: "&")
        guard let url = components?.url else {
            throw CxAudioError.invalidURL(Self.endpointURL.absoluteString)
        }
        return url
    }

    private func parametersWithAutomaticValues(
        placement: ConsumablePlacement?,
        userAgentParameter: String?
    ) -> [String: String] {
        var params = parameters
        putAutomatic(&params, "u", userAgentParameter)
        putAutomatic(&params, "sdk", Self.defaultSDKName)
        putAutomatic(&params, "sdkv", Self.sdkVersion)
        putAutomatic(&params, "locale", Locale.current.identifier.replacingOccurrences(of: "_", with: "-"))
        putAutomatic(&params, "appbundle", Bundle.main.bundleIdentifier)
        putAutomatic(&params, "appname", Bundle.main.displayName)
        putAutomatic(&params, "v_delivery", "2")
        putAutomatic(&params, "v_delay", "-1")
        putAutomatic(&params, "v_pm", "1")
        putAutomatic(&params, "v_max_dur", "90")
        putAutomatic(
            &params,
            "v_mimes",
            "audio/mpeg,audio/mp4,audio/aac,audio/ogg,audio/wav,audio/x-wav,"
                + "video/mp4,image/jpeg,image/png,image/gif,image/webp"
        )
        putAutomatic(&params, "v_cmp_types", "1")
        putAutomatic(&params, "v_cmp_mimes", "image/jpeg,image/png,image/gif,image/webp")
        putAutomatic(&params, "v_cmp_sizes", "300x250")
        putAutomatic(&params, "v_protocols", "2,3,5,6,7,8")
        putAutomatic(&params, "c_audio_feed_type", "6")
        if let placement {
            putAutomatic(&params, "sdkpt", placement.sdkPlacementTypeCode)
        }

        switch placement {
        case .backgroundAudio, .stickyPlayer:
            putAutomatic(&params, "v_plcmt", "2")
            putAutomatic(&params, "v_linearity", "2")
            putAutomatic(&params, "v_min_bitrate", "64")
            putAutomatic(&params, "v_max_bitrate", "320")
        case .fullscreenInterstitial:
            putAutomatic(&params, "v_plcmt", "3")
            putAutomatic(&params, "v_linearity", "1")
            putAutomatic(&params, "v_min_bitrate", "64")
            putAutomatic(&params, "v_max_bitrate", "2500")
        case nil:
            putAutomatic(&params, "v_min_bitrate", "64")
            putAutomatic(&params, "v_max_bitrate", "320")
            break
        }
        return params
    }

    private func percentEncodeQueryComponent(_ value: String) -> String {
        let unreserved = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-._~"))
        return value.addingPercentEncoding(withAllowedCharacters: unreserved) ?? ""
    }
}

public enum ConsumablePlacement: Equatable {
    case backgroundAudio
    case stickyPlayer
    case fullscreenInterstitial

    fileprivate var sdkPlacementTypeCode: String {
        switch self {
        case .backgroundAudio:
            return "1"
        case .stickyPlayer:
            return "2"
        case .fullscreenInterstitial:
            return "3"
        }
    }
}

public struct VastRequest: Equatable {
    private static let defaultUserAgent = "cx-audio-sdk-ios/0.1"

    public let vastTagURL: URL
    public let timeoutInterval: TimeInterval
    let userAgent: String

    public init(
        vastTagURL: URL,
        timeoutInterval: TimeInterval = 8
    ) throws {
        let scheme = vastTagURL.scheme?.lowercased()
        guard scheme == "http" || scheme == "https" else {
            throw CxAudioError.unsupportedURLScheme(vastTagURL.scheme ?? "")
        }
        guard timeoutInterval > 0 else {
            throw CxAudioError.invalidOption("timeoutInterval must be > 0")
        }
        self.vastTagURL = vastTagURL
        self.timeoutInterval = timeoutInterval
        self.userAgent = Self.defaultUserAgent
    }

    public init(
        vastTagURLString: String,
        timeoutInterval: TimeInterval = 8
    ) throws {
        guard let url = URL(string: vastTagURLString) else {
            throw CxAudioError.invalidURL(vastTagURLString)
        }
        try self.init(
            vastTagURL: url,
            timeoutInterval: timeoutInterval
        )
    }

    public init(
        consumableAdRequest: ConsumableAdRequest,
        placement: ConsumablePlacement? = .backgroundAudio,
        timeoutInterval: TimeInterval = 8
    ) throws {
        let resolvedPlacement = placement ?? .backgroundAudio
        try self.init(
            vastTagURL: consumableAdRequest.url(
                placement: resolvedPlacement,
                userAgentParameter: Self.defaultUserAgent
            ),
            timeoutInterval: timeoutInterval
        )
    }
}

public enum CxAudioError: Error, LocalizedError, Equatable {
    case invalidURL(String)
    case unsupportedURLScheme(String)
    case invalidOption(String)
    case noAudioMediaFile
    case noImageCompanion
    case noInterstitialMedia
    case noUsableInterstitial
    case wrapperDepthExceeded(Int)
    case httpStatus(Int, URL)
    case imageDecodeFailed(URL)
    case activityUnavailable
    case volumeTooLow(Float)
    case privacyBlocked(String)
    case playbackFailed(String)

    public var errorDescription: String? {
        switch self {
        case .invalidURL(let value):
            return "Invalid URL: \(value)"
        case .unsupportedURLScheme(let scheme):
            return "VAST URL must use HTTP or HTTPS, got \(scheme)"
        case .invalidOption(let message):
            return message
        case .noAudioMediaFile:
            return "VAST response did not include an audio media file"
        case .noImageCompanion:
            return "VAST response did not include an image companion"
        case .noInterstitialMedia:
            return "VAST response did not include interstitial media"
        case .noUsableInterstitial:
            return "VAST response did not include interstitial media or companion"
        case .wrapperDepthExceeded(let depth):
            return "VAST wrapper depth exceeded \(depth)"
        case .httpStatus(let status, let url):
            return "HTTP \(status) loading \(url.absoluteString)"
        case .imageDecodeFailed(let url):
            return "Image could not be decoded: \(url.absoluteString)"
        case .activityUnavailable:
            return "A visible view controller is required for this placement"
        case .volumeTooLow(let threshold):
            return "Device output volume is below \(threshold)"
        case .privacyBlocked(let message):
            return message
        case .playbackFailed(let message):
            return message
        }
    }
}

private func put(_ parameters: inout [String: String], _ name: String, _ value: String?) {
    guard let value, !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
        return
    }
    parameters[name] = value
}

private func putAutomatic(_ parameters: inout [String: String], _ name: String, _ value: String?) {
    if let value, !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, parameters[name] == nil {
        parameters[name] = value
    }
}

private extension Bundle {
    var displayName: String? {
        object(forInfoDictionaryKey: "CFBundleDisplayName") as? String
            ?? object(forInfoDictionaryKey: "CFBundleName") as? String
    }
}

private func joined(_ values: [String]) -> String? {
    let cleaned = values.filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    return cleaned.isEmpty ? nil : cleaned.joined(separator: ",")
}

private func boolString(_ value: Bool?) -> String? {
    value.map { $0 ? "1" : "0" }
}

private func formatNumber(_ value: Double) -> String {
    String(format: "%.6f", locale: Locale(identifier: "en_US_POSIX"), value)
        .replacingOccurrences(of: #"0+$"#, with: "", options: .regularExpression)
        .replacingOccurrences(of: #"\.$"#, with: "", options: .regularExpression)
}
