@testable import CxAudioSDK
import XCTest

final class VastTrackingTests: XCTestCase {
    func testImpressionFiresAllValidURLsOnce() {
        let harness = TrackingHarness()
        let state = harness.state(tracking: tracking(
            impressions: [
                "https://track.example.com/imp1",
                "https://track.example.com/imp2",
                "http://[malformed"
            ]
        ))

        state.fireImpression()
        state.fireImpression()
        harness.drain()

        XCTAssertEqual(harness.urls(for: .impression), [
            "https://track.example.com/imp1",
            "https://track.example.com/imp2"
        ])
    }

    func testQuartilesFireOnceAtThresholdCrossings() {
        let harness = TrackingHarness()
        let state = harness.state(tracking: defaultQuartileTracking())

        state.fireProgress(currentPosition: 0.25, duration: 1)
        state.fireProgress(currentPosition: 0.50, duration: 1)
        state.fireProgress(currentPosition: 0.75, duration: 1)
        state.fireProgress(currentPosition: 1.00, duration: 1)
        state.fireProgress(currentPosition: 1.00, duration: 1)
        harness.drain()

        XCTAssertEqual(harness.urls(for: .firstQuartile), ["https://track.example.com/q1"])
        XCTAssertEqual(harness.urls(for: .midpoint), ["https://track.example.com/q2"])
        XCTAssertEqual(harness.urls(for: .thirdQuartile), ["https://track.example.com/q3"])
        XCTAssertEqual(harness.urls(for: .complete), ["https://track.example.com/q4"])
    }

    func testCoarseProgressJumpFiresMissedQuartilesInOrder() {
        let harness = TrackingHarness()
        let state = harness.state(tracking: defaultQuartileTracking())

        state.fireProgress(currentPosition: 0.55, duration: 1)
        harness.drain()

        XCTAssertEqual(harness.events, [.firstQuartile, .midpoint])
    }

    func testSeekPauseAndResumeDoNotDuplicateAlreadyFiredQuartiles() {
        let harness = TrackingHarness()
        let state = harness.state(tracking: defaultQuartileTracking())

        state.fireProgress(currentPosition: 0.80, duration: 1)
        state.fireProgress(currentPosition: 0.30, duration: 1)
        state.fireProgress(currentPosition: 0.80, duration: 1)
        harness.drain()

        XCTAssertEqual(harness.urls(for: .firstQuartile), ["https://track.example.com/q1"])
        XCTAssertEqual(harness.urls(for: .midpoint), ["https://track.example.com/q2"])
        XCTAssertEqual(harness.urls(for: .thirdQuartile), ["https://track.example.com/q3"])
    }

    func testClickFiresAllValidClickTrackingURLsOnce() {
        let harness = TrackingHarness()
        let state = harness.state(tracking: tracking(clicks: [
            "https://track.example.com/click1",
            "https://track.example.com/click2",
            "not-a-url"
        ]))

        state.fireClick()
        state.fireClick()
        harness.drain()

        XCTAssertEqual(harness.urls(for: .click), [
            "https://track.example.com/click1",
            "https://track.example.com/click2"
        ])
    }

    func testDuplicateURLsAreDedupedPerEventByExactTrimmedURL() {
        let harness = TrackingHarness()
        let state = harness.state(tracking: tracking(impressions: [
            "https://track.example.com/imp",
            " https://track.example.com/imp ",
            "https://track.example.com/imp?distinct=1"
        ]))

        state.fireImpression()
        harness.drain()

        XCTAssertEqual(harness.urls(for: .impression), [
            "https://track.example.com/imp",
            "https://track.example.com/imp?distinct=1"
        ])
    }

    private func defaultQuartileTracking() -> VastTracking {
        tracking(events: [
            .firstQuartile: ["https://track.example.com/q1"],
            .midpoint: ["https://track.example.com/q2"],
            .thirdQuartile: ["https://track.example.com/q3"],
            .complete: ["https://track.example.com/q4"]
        ])
    }

    private func tracking(
        impressions: [String] = [],
        events: [TrackingEvent: [String]] = [:],
        clicks: [String] = []
    ) -> VastTracking {
        VastTracking(
            adIdentifier: "ad-1",
            impressionURLStrings: impressions,
            eventURLStrings: events,
            clickTrackingURLStrings: clicks
        )
    }
}

private final class TrackingHarness {
    private let queue = DispatchQueue(label: "com.cx.audio.tracking.tests")
    private var urlsByEvent: [TrackingEvent: [String]] = [:]
    private(set) var events: [TrackingEvent] = []

    func state(tracking: VastTracking) -> PerAdTrackingState {
        PerAdTrackingState(
            tracking: tracking,
            dispatcher: TrackingBeaconDispatcher(queue: queue) { [weak self] event, _, url, completion in
                self?.events.append(event)
                self?.urlsByEvent[event, default: []].append(url.absoluteString)
                completion(.success(()))
            }
        )
    }

    func drain() {
        queue.sync {}
    }

    func urls(for event: TrackingEvent) -> [String] {
        urlsByEvent[event] ?? []
    }
}
