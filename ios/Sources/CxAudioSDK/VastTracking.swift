import Foundation

internal enum TrackingEvent: String, CaseIterable {
    case impression
    case start
    case firstQuartile
    case midpoint
    case thirdQuartile
    case complete
    case click

    var logName: String {
        switch self {
        case .click:
            return "clickTracking"
        default:
            return rawValue
        }
    }

    static func vastEvent(_ value: String?) -> TrackingEvent? {
        switch value?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "start":
            return .start
        case "firstquartile":
            return .firstQuartile
        case "midpoint":
            return .midpoint
        case "thirdquartile":
            return .thirdQuartile
        case "complete":
            return .complete
        default:
            return nil
        }
    }
}

internal struct VastTracking: Equatable {
    static let empty = VastTracking()

    let adIdentifier: String?
    let impressionURLStrings: [String]
    let eventURLStrings: [TrackingEvent: [String]]
    let clickTrackingURLStrings: [String]

    init(
        adIdentifier: String? = nil,
        impressionURLStrings: [String] = [],
        eventURLStrings: [TrackingEvent: [String]] = [:],
        clickTrackingURLStrings: [String] = []
    ) {
        self.adIdentifier = Self.normalized(adIdentifier)
        self.impressionURLStrings = Self.normalized(impressionURLStrings)
        self.eventURLStrings = eventURLStrings.reduce(into: [:]) { result, entry in
            let values = Self.normalized(entry.value)
            if !values.isEmpty {
                result[entry.key] = values
            }
        }
        self.clickTrackingURLStrings = Self.normalized(clickTrackingURLStrings)
    }

    func urls(for event: TrackingEvent) -> [String] {
        switch event {
        case .impression:
            return impressionURLStrings
        case .click:
            return clickTrackingURLStrings
        default:
            return eventURLStrings[event] ?? []
        }
    }

    func merged(with other: VastTracking) -> VastTracking {
        var events = eventURLStrings
        for event in TrackingEvent.allCases where event != .impression && event != .click {
            events[event, default: []].append(contentsOf: other.urls(for: event))
        }

        return VastTracking(
            adIdentifier: adIdentifier ?? other.adIdentifier,
            impressionURLStrings: impressionURLStrings + other.impressionURLStrings,
            eventURLStrings: events,
            clickTrackingURLStrings: clickTrackingURLStrings + other.clickTrackingURLStrings
        )
    }

    func resolved(baseURL: URL) -> VastTracking {
        let resolvedEvents = eventURLStrings.reduce(into: [TrackingEvent: [String]]()) { result, entry in
            let urls = Self.resolved(entry.value, baseURL: baseURL)
            if !urls.isEmpty {
                result[entry.key] = urls
            }
        }

        return VastTracking(
            adIdentifier: adIdentifier,
            impressionURLStrings: Self.resolved(impressionURLStrings, baseURL: baseURL),
            eventURLStrings: resolvedEvents,
            clickTrackingURLStrings: Self.resolved(clickTrackingURLStrings, baseURL: baseURL)
        )
    }

    private static func normalized(_ values: [String]) -> [String] {
        values.compactMap(normalized)
    }

    private static func normalized(_ value: String?) -> String? {
        guard let value else {
            return nil
        }
        let normalized = value.trimmingCharacters(in: .whitespacesAndNewlines)
        return normalized.isEmpty ? nil : normalized
    }

    private static func resolved(_ values: [String], baseURL: URL) -> [String] {
        values.compactMap { value in
            guard let url = URL(string: value, relativeTo: baseURL)?.absoluteURL else {
                return nil
            }
            return url.absoluteString
        }
    }
}

internal final class PerAdTrackingState {
    private let tracking: VastTracking
    private let dispatcher: TrackingBeaconDispatcher
    private let lock = NSLock()
    private var firedEvents = Set<TrackingEvent>()

    init(
        tracking: VastTracking,
        dispatcher: TrackingBeaconDispatcher = .shared
    ) {
        self.tracking = tracking
        self.dispatcher = dispatcher
    }

    func fireImpression() {
        fireOnce(.impression, urls: tracking.impressionURLStrings)
    }

    func fireStart() {
        fireOnce(.start, urls: tracking.urls(for: .start))
    }

    func fireProgress(currentPosition: TimeInterval, duration: TimeInterval) {
        guard duration > 0, currentPosition >= 0 else {
            return
        }

        let progress = min(1, currentPosition / duration)
        if progress >= 0.25 {
            fireOnce(.firstQuartile, urls: tracking.urls(for: .firstQuartile))
        }
        if progress >= 0.50 {
            fireOnce(.midpoint, urls: tracking.urls(for: .midpoint))
        }
        if progress >= 0.75 {
            fireOnce(.thirdQuartile, urls: tracking.urls(for: .thirdQuartile))
        }
        if progress >= 1.0 {
            fireOnce(.complete, urls: tracking.urls(for: .complete))
        }
    }

    func fireComplete() {
        fireOnce(.firstQuartile, urls: tracking.urls(for: .firstQuartile))
        fireOnce(.midpoint, urls: tracking.urls(for: .midpoint))
        fireOnce(.thirdQuartile, urls: tracking.urls(for: .thirdQuartile))
        fireOnce(.complete, urls: tracking.urls(for: .complete))
    }

    func fireClick() {
        fireOnce(.click, urls: tracking.clickTrackingURLStrings)
    }

    private func fireOnce(_ event: TrackingEvent, urls: [String]) {
        let dedupedURLs: [String]
        lock.lock()
        if firedEvents.contains(event) {
            lock.unlock()
            return
        }
        firedEvents.insert(event)
        dedupedURLs = Self.deduped(urls)
        lock.unlock()

        TrackingBeaconDispatcher.logEventFired(
            event: event,
            adIdentifier: tracking.adIdentifier,
            beaconCount: dedupedURLs.count
        )
        dispatcher.dispatch(event: event, adIdentifier: tracking.adIdentifier, urlStrings: dedupedURLs)
    }

    private static func deduped(_ urls: [String]) -> [String] {
        var seen = Set<String>()
        var result: [String] = []
        for url in urls {
            let normalized = url.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !normalized.isEmpty, !seen.contains(normalized) else {
                continue
            }
            seen.insert(normalized)
            result.append(normalized)
        }
        return result
    }
}

internal final class TrackingBeaconDispatcher {
    typealias Completion = (Result<Void, Error>) -> Void
    typealias BeaconSender = (TrackingEvent, String?, URL, @escaping Completion) -> Void

    static let shared = TrackingBeaconDispatcher()

    private let queue: DispatchQueue
    private let sender: BeaconSender

    init(
        queue: DispatchQueue = DispatchQueue(label: "com.cx.audio.tracking", qos: .utility),
        sender: @escaping BeaconSender = TrackingBeaconDispatcher.httpSender
    ) {
        self.queue = queue
        self.sender = sender
    }

    func dispatch(event: TrackingEvent, adIdentifier: String?, urlStrings: [String]) {
        guard !urlStrings.isEmpty else {
            return
        }

        for urlString in urlStrings {
            guard let url = Self.validHTTPURL(urlString) else {
                Self.log(
                    event: event,
                    adIdentifier: adIdentifier,
                    target: Self.fingerprint(urlString),
                    result: "skipped",
                    error: "MalformedURLException"
                )
                continue
            }

            let target = Self.fingerprint(urlString)
            queue.async { [sender] in
                sender(event, adIdentifier, url) { result in
                    switch result {
                    case .success:
                        Self.log(event: event, adIdentifier: adIdentifier, target: target, result: "success")
                    case .failure(let error):
                        Self.log(
                            event: event,
                            adIdentifier: adIdentifier,
                            target: target,
                            result: "failure",
                            error: String(describing: type(of: error))
                        )
                    }
                }
            }
        }
    }

    static func logEventFired(event: TrackingEvent, adIdentifier: String?, beaconCount: Int) {
        NSLog(
            "CxAudioTracking event=%@ ad=%@ result=fired beacons=%d",
            event.logName,
            safeAdIdentifier(adIdentifier),
            beaconCount
        )
    }

    private static func httpSender(
        event: TrackingEvent,
        adIdentifier: String?,
        url: URL,
        completion: @escaping Completion
    ) {
        var request = URLRequest(url: url, timeoutInterval: 2)
        request.httpMethod = "GET"
        URLSession.shared.dataTask(with: request) { _, response, error in
            if let error {
                completion(.failure(error))
                return
            }
            if let response = response as? HTTPURLResponse,
               !(200..<400).contains(response.statusCode) {
                completion(.failure(CxAudioError.httpStatus(response.statusCode, url)))
                return
            }
            completion(.success(()))
        }.resume()
    }

    private static func validHTTPURL(_ value: String) -> URL? {
        guard let url = URL(string: value.trimmingCharacters(in: .whitespacesAndNewlines)),
              let scheme = url.scheme?.lowercased(),
              scheme == "http" || scheme == "https" else {
            return nil
        }
        return url
    }

    private static func log(
        event: TrackingEvent,
        adIdentifier: String?,
        target: String,
        result: String,
        error: String? = nil
    ) {
        var message = "CxAudioTracking event=\(event.logName) ad=\(safeAdIdentifier(adIdentifier)) target=\(target) result=\(result)"
        if let error {
            message += " error=\(error)"
        }
        NSLog("%@", message)
    }

    private static func safeAdIdentifier(_ adIdentifier: String?) -> String {
        let normalized = adIdentifier?.trimmingCharacters(in: .whitespacesAndNewlines)
        return normalized?.isEmpty == false ? normalized! : "unknown"
    }

    private static func fingerprint(_ value: String) -> String {
        let bytes = [UInt8](value.utf8)
        var hash: UInt64 = 14_695_981_039_346_656_037
        for byte in bytes {
            hash ^= UInt64(byte)
            hash &*= 1_099_511_628_211
        }
        return String(format: "%012llx", hash & 0xffffffffffff)
    }
}
