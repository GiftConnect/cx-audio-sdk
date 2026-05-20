import Foundation

internal struct VastDocument {
    let mediaFiles: [VastMediaFileCandidate]
    let companions: [VastCompanionCandidate]
    let wrapperURLString: String?

    func bestCompanion(baseURL: URL) -> VastCompanion? {
        companions
            .filter { $0.isImageResource }
            .min { $0.companionScore < $1.companionScore }
            .flatMap { $0.resolve(baseURL: baseURL) }
    }

    func bestAudioMediaFile(baseURL: URL) -> VastMediaFile? {
        mediaFiles
            .filter { $0.isAudioResource }
            .min { $0.audioScore < $1.audioScore }
            .flatMap { $0.resolve(baseURL: baseURL) }
    }

    func bestInterstitialMediaFile(baseURL: URL) -> VastMediaFile? {
        mediaFiles
            .filter { $0.isVisualResource }
            .min { $0.interstitialScore < $1.interstitialScore }
            .flatMap { $0.resolve(baseURL: baseURL) }
    }

    func interstitialAd(baseURL: URL) -> VastInterstitialAd? {
        let mediaFile = bestInterstitialMediaFile(baseURL: baseURL)
        let companion = bestCompanion(baseURL: baseURL)
        guard mediaFile != nil || companion != nil else {
            return nil
        }
        return VastInterstitialAd(mediaFile: mediaFile, companion: companion)
    }
}

internal struct VastMediaFile {
    let url: URL
    let type: String?
    let delivery: String?
    let bitrate: Int
    let width: Int
    let height: Int

    var isAudio: Bool {
        matches(typePrefix: "audio/", extensions: ["mp3", "m4a", "aac", "ogg", "oga", "wav"])
    }

    var isImage: Bool {
        matches(typePrefix: "image/", extensions: ["jpg", "jpeg", "png", "gif", "webp"])
    }

    var isVideo: Bool {
        matches(typePrefix: "video/", extensions: ["mp4", "m4v", "webm", "3gp"])
    }

    private func matches(typePrefix: String, extensions: Set<String>) -> Bool {
        if let type, type.lowercased().hasPrefix(typePrefix) {
            return true
        }
        let pathExtension = url.pathExtension.lowercased()
        return extensions.contains(pathExtension)
    }
}

internal struct VastCompanion {
    let staticResourceURL: URL
    let clickThroughURL: URL?
    let width: Int
    let height: Int
}

internal struct VastInterstitialAd {
    let mediaFile: VastMediaFile?
    let companion: VastCompanion?
}

internal struct VastMediaFileCandidate {
    let urlString: String
    let type: String?
    let delivery: String?
    let bitrate: Int
    let width: Int
    let height: Int

    var isAudioResource: Bool {
        if let type, type.lowercased().hasPrefix("audio/") {
            return true
        }
        return hasExtension(["mp3", "m4a", "aac", "ogg", "oga", "wav"])
    }

    var isVisualResource: Bool {
        if let type {
            let normalizedType = type.lowercased()
            if normalizedType.hasPrefix("video/") || normalizedType.hasPrefix("image/") {
                return true
            }
        }
        return hasExtension(["mp4", "m4v", "webm", "3gp", "jpg", "jpeg", "png", "gif", "webp"])
    }

    var audioScore: Int {
        var score = delivery == "progressive" ? 0 : 50
        switch type {
        case "audio/mpeg":
            score += 0
        case "audio/mp4", "audio/aac":
            score += 5
        case "audio/ogg":
            score += 20
        case "audio/wav", "audio/x-wav":
            score += 30
        case nil:
            score += 15
        default:
            score += 25
        }
        score += bitrate > 0 ? abs(128 - bitrate) / 8 : 10
        return score
    }

    var interstitialScore: Int {
        var score = delivery == "progressive" ? 0 : 50
        switch type {
        case "video/mp4":
            score += 0
        case "image/jpeg", "image/png":
            score += 10
        default:
            if let type, type.hasPrefix("video/") {
                score += 15
            } else if let type, type.hasPrefix("image/") {
                score += 20
            } else if type == nil {
                score += 25
            } else {
                score += 30
            }
        }
        if width > 0 && height > 0 {
            score += abs(1280 - width) / 80
            score += abs(720 - height) / 45
        } else {
            score += 20
        }
        return score
    }

    func resolve(baseURL: URL) -> VastMediaFile? {
        guard let url = URL(string: urlString, relativeTo: baseURL)?.absoluteURL else {
            return nil
        }
        return VastMediaFile(
            url: url,
            type: type,
            delivery: delivery,
            bitrate: bitrate,
            width: width,
            height: height
        )
    }

    private func hasExtension(_ extensions: Set<String>) -> Bool {
        guard let url = URL(string: urlString) else {
            return false
        }
        return extensions.contains(url.pathExtension.lowercased())
    }
}

internal struct VastCompanionCandidate {
    let staticResourceURLString: String
    let staticResourceCreativeType: String?
    let clickThroughURLString: String?
    let width: Int
    let height: Int

    var isImageResource: Bool {
        if let staticResourceCreativeType,
           staticResourceCreativeType.lowercased().hasPrefix("image/") {
            return true
        }
        guard let url = URL(string: staticResourceURLString) else {
            return false
        }
        return ["jpg", "jpeg", "png", "gif", "webp"].contains(url.pathExtension.lowercased())
    }

    var companionScore: Int {
        abs(300 - width) + abs(250 - height)
    }

    func resolve(baseURL: URL) -> VastCompanion? {
        guard let staticResourceURL = URL(
            string: staticResourceURLString,
            relativeTo: baseURL
        )?.absoluteURL else {
            return nil
        }
        let clickThroughURL = clickThroughURLString.flatMap {
            URL(string: $0, relativeTo: baseURL)?.absoluteURL
        }
        return VastCompanion(
            staticResourceURL: staticResourceURL,
            clickThroughURL: clickThroughURL,
            width: width,
            height: height
        )
    }
}
