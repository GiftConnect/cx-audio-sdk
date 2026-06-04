import Foundation

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
