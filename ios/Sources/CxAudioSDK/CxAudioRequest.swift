import Foundation

public struct ConsumableAdRequest: Equatable {
    public static let defaultEndpointURL = URL(string: "https://2.e.serverbid.com/mpb/get")!

    public let endpointURL: URL
    public let parameters: [String: String]

    public init(
        placementHash: String,
        endpointURL: URL = ConsumableAdRequest.defaultEndpointURL,
        responseType: String = "vast",
        secure: Bool = true,
        cacheBuster: String = String(Int64(Date().timeIntervalSince1970 * 1000)),
        userAgentParameter: String? = nil,
        ipAddress: String? = nil,
        locale: String? = nil,
        contentCategories: [String] = [],
        blockedCategories: [String] = [],
        blockedAdvertisers: [String] = [],
        limitAdTracking: Bool? = nil,
        doNotTrack: Bool? = nil,
        supplyChain: String? = nil,
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
        playerWidth: Int? = nil,
        playerHeight: Int? = nil,
        videoPlacement: Int? = nil,
        delivery: Int? = nil,
        startDelaySeconds: Int? = nil,
        position: String? = nil,
        maxDurationSeconds: Int? = nil,
        mediaMimes: [String] = [],
        skippable: Bool? = nil,
        playbackMethod: Int? = nil,
        maxBitrateKbps: Int? = nil,
        minBitrateKbps: Int? = nil,
        companionTypes: [String] = [],
        companionMimes: [String] = [],
        companionSizes: [String] = [],
        protocols: [String] = [],
        contentId: String? = nil,
        contentGenre: [String] = [],
        contentTitle: String? = nil,
        contentDurationSeconds: Int? = nil,
        contentURL: String? = nil,
        contentAudioFeedType: Int? = nil,
        customParameters: [String: String] = [:]
    ) throws {
        guard !placementHash.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw CxAudioError.invalidOption("placementHash must not be blank")
        }
        let scheme = endpointURL.scheme?.lowercased()
        guard scheme == "http" || scheme == "https" else {
            throw CxAudioError.unsupportedURLScheme(endpointURL.scheme ?? "")
        }

        var params: [String: String] = [
            "s": placementHash,
            "rt": responseType,
            "secure": secure ? "1" : "0",
            "cb": cacheBuster
        ]

        put(&params, "u", userAgentParameter)
        put(&params, "i", ipAddress)
        put(&params, "locale", locale)
        put(&params, "cat", joined(contentCategories))
        put(&params, "bcat", joined(blockedCategories))
        put(&params, "badv", joined(blockedAdvertisers))
        put(&params, "lmt", boolString(limitAdTracking))
        put(&params, "dnt", boolString(doNotTrack))
        put(&params, "schain", supplyChain)
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
        put(&params, "v_w", playerWidth.map(String.init))
        put(&params, "v_h", playerHeight.map(String.init))
        put(&params, "v_plcmt", videoPlacement.map(String.init))
        put(&params, "v_delivery", delivery.map(String.init))
        if let startDelaySeconds {
            put(&params, "v_delay", String(startDelaySeconds))
        } else {
            put(&params, "v_position", position)
        }
        put(&params, "v_max_dur", maxDurationSeconds.map(String.init))
        put(&params, "v_mimes", joined(mediaMimes))
        put(&params, "v_skip", boolString(skippable))
        put(&params, "v_pm", playbackMethod.map(String.init))
        put(&params, "v_max_bitrate", maxBitrateKbps.map(String.init))
        put(&params, "v_min_bitrate", minBitrateKbps.map(String.init))
        put(&params, "v_cmp_types", joined(companionTypes))
        put(&params, "v_cmp_mimes", joined(companionMimes))
        put(&params, "v_cmp_sizes", joined(companionSizes))
        put(&params, "v_protocols", joined(protocols))
        put(&params, "c_id", contentId)
        put(&params, "c_genre", joined(contentGenre))
        put(&params, "c_title", contentTitle)
        put(&params, "c_dur", contentDurationSeconds.map(String.init))
        put(&params, "c_url", contentURL)
        put(&params, "c_audio_feed_type", contentAudioFeedType.map(String.init))
        customParameters.forEach { params[$0.key] = $0.value }

        self.endpointURL = endpointURL
        self.parameters = params
    }

    public func url(placement: ConsumablePlacement? = nil) throws -> URL {
        var components = URLComponents(url: endpointURL, resolvingAgainstBaseURL: false)
        components?.queryItems = parametersWithAutomaticValues(placement: placement)
            .filter { !$0.value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .sorted { $0.key < $1.key }
            .map { URLQueryItem(name: $0.key, value: $0.value) }
        guard let url = components?.url else {
            throw CxAudioError.invalidURL(endpointURL.absoluteString)
        }
        return url
    }

    private func parametersWithAutomaticValues(placement: ConsumablePlacement?) -> [String: String] {
        var params = parameters
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
}

public enum ConsumablePlacement: Equatable {
    case backgroundAudio
    case stickyPlayer
    case fullscreenInterstitial
}

public struct VastRequest: Equatable {
    public let vastTagURL: URL
    public let timeoutInterval: TimeInterval
    public let userAgent: String

    public init(
        vastTagURL: URL,
        timeoutInterval: TimeInterval = 8,
        userAgent: String = "cx-audio-sdk-ios/0.1"
    ) throws {
        let scheme = vastTagURL.scheme?.lowercased()
        guard scheme == "http" || scheme == "https" else {
            throw CxAudioError.unsupportedURLScheme(vastTagURL.scheme ?? "")
        }
        guard timeoutInterval > 0 else {
            throw CxAudioError.invalidOption("timeoutInterval must be > 0")
        }
        guard !userAgent.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw CxAudioError.invalidOption("userAgent must not be blank")
        }

        self.vastTagURL = vastTagURL
        self.timeoutInterval = timeoutInterval
        self.userAgent = userAgent
    }

    public init(
        vastTagURLString: String,
        timeoutInterval: TimeInterval = 8,
        userAgent: String = "cx-audio-sdk-ios/0.1"
    ) throws {
        guard let url = URL(string: vastTagURLString) else {
            throw CxAudioError.invalidURL(vastTagURLString)
        }
        try self.init(
            vastTagURL: url,
            timeoutInterval: timeoutInterval,
            userAgent: userAgent
        )
    }

    public init(
        consumableAdRequest: ConsumableAdRequest,
        placement: ConsumablePlacement? = nil,
        timeoutInterval: TimeInterval = 8,
        userAgent: String = "cx-audio-sdk-ios/0.1"
    ) throws {
        try self.init(
            vastTagURL: consumableAdRequest.url(placement: placement),
            timeoutInterval: timeoutInterval,
            userAgent: userAgent
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
