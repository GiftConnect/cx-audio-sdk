@testable import CxAudioSDK
import XCTest

final class VastParserTests: XCTestCase {
    private let parser = VastParser()
    private let baseURL = URL(string: "https://ads.example.com/vast/root.xml")!

    func testConsumableAdRequestBuildsEncodedVastURL() throws {
        let consumableRequest = try ConsumableAdRequest(
            placementHash: "abc123",
            limitAdTracking: false,
            appStoreURL: "https://apps.apple.com/app/id123",
            appName: "Test App",
            appBundle: "com.example.app",
            gdpr: true,
            gdprConsent: "consent string"
        )

        let url = try consumableRequest.url()
        let components = try XCTUnwrap(URLComponents(url: url, resolvingAgainstBaseURL: false))
        let items = Dictionary<String, String>(
            uniqueKeysWithValues: (components.queryItems ?? [URLQueryItem]()).map { ($0.name, $0.value ?? "") }
        )

        XCTAssertEqual(url.scheme, "https")
        XCTAssertEqual(url.host, "2.e.serverbid.com")
        XCTAssertEqual(items["s"], "abc123")
        XCTAssertEqual(items["rt"], "vast")
        XCTAssertEqual(items["secure"], "1")
        let cacheBuster = try XCTUnwrap(items["cb"])
        XCTAssertNotNil(cacheBuster.range(of: #"^\d+$"#, options: .regularExpression))
        XCTAssertEqual(items["appname"], "Test App")
        XCTAssertEqual(items["appbundle"], "com.example.app")
        XCTAssertEqual(items["appstoreurl"], "https://apps.apple.com/app/id123")
        XCTAssertEqual(items["lmt"], "0")
        XCTAssertEqual(items["gdpr"], "1")
        XCTAssertEqual(items["gdpr_consent"], "consent string")
        XCTAssertEqual(items["sdk"], "Consumable Audio")
        XCTAssertEqual(items["sdkv"], "0.1.0")
        XCTAssertEqual(items["v_delay"], "-1")
        XCTAssertEqual(items["v_delivery"], "2")
        XCTAssertEqual(items["v_mimes"], "audio/mpeg,audio/mp4,audio/aac,audio/ogg,audio/wav,audio/x-wav,video/mp4,image/jpeg,image/png,image/gif,image/webp")
        XCTAssertEqual(items["v_cmp_sizes"], "300x250")
        XCTAssertEqual(items["v_pm"], "1")
        XCTAssertEqual(items["c_audio_feed_type"], "6")

        let vastRequest = try VastRequest(consumableAdRequest: consumableRequest)
        let vastItems = queryItems(from: vastRequest.vastTagURL)
        XCTAssertEqual(vastItems["u"], "cx-audio-sdk-ios/0.1")
        XCTAssertEqual(vastItems["sdkpt"], "1")

        let legacyNilPlacement = try VastRequest(
            consumableAdRequest: consumableRequest,
            placement: nil
        )
        XCTAssertEqual(queryItems(from: legacyNilPlacement.vastTagURL)["sdkpt"], "1")
    }

    func testConsumableAdRequestEncodesConfiguredSDKNameAndAutomaticVersion() throws {
        let consumableRequest = try ConsumableAdRequest(
            placementHash: "abc123",
            sdkName: "Radio.com Audio / SDK"
        )

        let url = try consumableRequest.url()
        let items = queryItems(from: url)

        XCTAssertEqual(items["sdk"], "Radio.com Audio / SDK")
        XCTAssertEqual(items["sdkv"], "0.1.0")
        XCTAssertTrue(url.absoluteString.contains("sdk=Radio.com%20Audio%20%2F%20SDK"))
    }

    func testConsumableAdRequestDefaultsCacheBusterToMillisecondTimestamp() throws {
        let consumableRequest = try ConsumableAdRequest(placementHash: "abc123")

        let url = try consumableRequest.url()
        let items = queryItems(from: url)
        let cacheBuster = try XCTUnwrap(items["cb"])

        XCTAssertNotNil(cacheBuster.range(of: #"^\d+$"#, options: .regularExpression))
    }

    func testConsumableAdRequestAppliesPlacementAutomaticParameters() throws {
        let consumableRequest = try ConsumableAdRequest(placementHash: "abc123")

        let backgroundURL = try VastRequest(
            consumableAdRequest: consumableRequest,
            placement: .backgroundAudio
        ).vastTagURL
        XCTAssertEqual(queryItems(from: backgroundURL)["sdkpt"], "1")

        let stickyURL = try VastRequest(
            consumableAdRequest: consumableRequest,
            placement: .stickyPlayer
        ).vastTagURL
        let stickyItems = queryItems(from: stickyURL)
        XCTAssertEqual(stickyItems["sdkpt"], "2")
        XCTAssertEqual(stickyItems["v_plcmt"], "2")
        XCTAssertEqual(stickyItems["v_linearity"], "2")
        XCTAssertEqual(stickyItems["v_delivery"], "2")
        XCTAssertEqual(stickyItems["v_delay"], "-1")
        XCTAssertEqual(stickyItems["v_pm"], "1")
        XCTAssertEqual(stickyItems["v_max_dur"], "90")
        XCTAssertEqual(stickyItems["v_min_bitrate"], "64")
        XCTAssertEqual(stickyItems["v_max_bitrate"], "320")
        XCTAssertEqual(stickyItems["v_cmp_types"], "1")
        XCTAssertEqual(stickyItems["v_cmp_sizes"], "300x250")
        XCTAssertEqual(stickyItems["v_protocols"], "2,3,5,6,7,8")
        XCTAssertEqual(stickyItems["c_audio_feed_type"], "6")

        let interstitialURL = try VastRequest(
            consumableAdRequest: consumableRequest,
            placement: .fullscreenInterstitial
        ).vastTagURL
        let interstitialItems = queryItems(from: interstitialURL)
        XCTAssertEqual(interstitialItems["sdkpt"], "3")
        XCTAssertEqual(interstitialItems["v_plcmt"], "3")
        XCTAssertEqual(interstitialItems["v_linearity"], "1")
        XCTAssertEqual(interstitialItems["v_min_bitrate"], "64")
        XCTAssertEqual(interstitialItems["v_max_bitrate"], "2500")
    }

    func testRawVastRequestDoesNotModifyURLWithSdkPlacementType() throws {
        let rawURL = "https://ads.example.com/vast.xml?custom=1"

        let request = try VastRequest(vastTagURLString: rawURL)

        XCTAssertEqual(request.vastTagURL.absoluteString, rawURL)
        XCTAssertNil(queryItems(from: request.vastTagURL)["sdkpt"])
    }

    func testCompanionParserPrefersClosestThreeHundredByTwoFiftyImage() throws {
        let document = try parser.parse(data: vastWithCompanions(
            companion("/small.png", creativeType: "image/png", width: 120, height: 90),
            companion("/target.jpg", creativeType: "image/jpeg", width: 300, height: 250),
            companion("/wide.gif", creativeType: "image/gif", width: 728, height: 90)
        ))

        let selected = try XCTUnwrap(document.bestCompanion(baseURL: baseURL))

        XCTAssertEqual(selected.staticResourceURL.absoluteString, "https://ads.example.com/target.jpg")
        XCTAssertEqual(selected.width, 300)
        XCTAssertEqual(selected.height, 250)
        XCTAssertEqual(selected.clickThroughURL?.absoluteString, "https://click.example.com")
    }

    func testBackgroundAudioParserPrefersProgressiveCommonAudioMedia() throws {
        let document = try parser.parse(data: vastWithMediaFiles(
            mediaFile("/audio.ogg", type: "audio/ogg", delivery: "streaming", bitrate: 128),
            mediaFile("/audio.mp3", type: "audio/mpeg", delivery: "progressive", bitrate: 128),
            mediaFile("/audio.wav", type: "audio/wav", delivery: "progressive", bitrate: 256)
        ))

        let selected = try XCTUnwrap(document.bestAudioMediaFile(baseURL: baseURL))

        XCTAssertEqual(selected.url.absoluteString, "https://ads.example.com/audio.mp3")
        XCTAssertEqual(selected.type, "audio/mpeg")
        XCTAssertEqual(selected.delivery, "progressive")
        XCTAssertEqual(selected.bitrate, 128)
    }

    func testFullscreenInterstitialParserPrefersProgressiveMp4VisualMedia() throws {
        let document = try parser.parse(data: vastWithMediaFiles(
            mediaFile("/slate.png", type: "image/png", delivery: "progressive", width: 1280, height: 720),
            mediaFile("/ad.webm", type: "video/webm", delivery: "streaming", bitrate: 1000, width: 1280, height: 720),
            mediaFile("/ad.mp4", type: "video/mp4", delivery: "progressive", bitrate: 1000, width: 1280, height: 720)
        ))

        let selected = try XCTUnwrap(document.bestInterstitialMediaFile(baseURL: baseURL))

        XCTAssertEqual(selected.url.absoluteString, "https://ads.example.com/ad.mp4")
        XCTAssertEqual(selected.type, "video/mp4")
        XCTAssertEqual(selected.width, 1280)
        XCTAssertEqual(selected.height, 720)
        XCTAssertTrue(selected.isVideo)
    }

    func testWrapperUrlIsParsed() throws {
        let document = try parser.parse(data: Data("""
        <VAST><Ad><Wrapper><VASTAdTagURI><![CDATA[https://ads.example.com/next.xml]]></VASTAdTagURI></Wrapper></Ad></VAST>
        """.utf8))

        XCTAssertEqual(document.wrapperURLString, "https://ads.example.com/next.xml")
    }

    func testTrackingParserExtractsMultipleImpressionsEventsAndClickTracking() throws {
        let document = try parser.parse(data: Data("""
        <VAST><Ad id="ad-42"><InLine>
        <Impression>https://track.example.com/imp1</Impression>
        <Impression><![CDATA[https://track.example.com/imp2?a=1&b=2]]></Impression>
        <Creatives><Creative><Linear>
        <TrackingEvents>
        <Tracking event="start">https://track.example.com/start</Tracking>
        <Tracking event="firstQuartile">https://track.example.com/q1</Tracking>
        <Tracking event="firstQuartile">https://track.example.com/q1b</Tracking>
        <Tracking event="midpoint">https://track.example.com/q2</Tracking>
        <Tracking event="thirdQuartile">https://track.example.com/q3</Tracking>
        <Tracking event="complete">https://track.example.com/q4</Tracking>
        </TrackingEvents>
        <VideoClicks>
        <ClickThrough>https://click.example.com/open</ClickThrough>
        <ClickTracking>https://track.example.com/click1</ClickTracking>
        <ClickTracking>https://track.example.com/click2</ClickTracking>
        </VideoClicks>
        </Linear></Creative></Creatives>
        </InLine></Ad></VAST>
        """.utf8))

        XCTAssertEqual(document.tracking.adIdentifier, "ad-42")
        XCTAssertEqual(document.tracking.impressionURLStrings, [
            "https://track.example.com/imp1",
            "https://track.example.com/imp2?a=1&b=2"
        ])
        XCTAssertEqual(document.tracking.urls(for: .firstQuartile), [
            "https://track.example.com/q1",
            "https://track.example.com/q1b"
        ])
        XCTAssertEqual(document.tracking.clickTrackingURLStrings, [
            "https://track.example.com/click1",
            "https://track.example.com/click2"
        ])
    }

    private func vastWithCompanions(_ companions: String...) -> Data {
        Data("""
        <VAST><Ad><InLine><Creatives><Creative><CompanionAds>
        \(companions.joined())
        </CompanionAds></Creative></Creatives></InLine></Ad></VAST>
        """.utf8)
    }

    private func queryItems(from url: URL) -> [String: String] {
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        return Dictionary<String, String>(
            uniqueKeysWithValues: (components?.queryItems ?? [URLQueryItem]()).map { ($0.name, $0.value ?? "") }
        )
    }

    private func vastWithMediaFiles(_ mediaFiles: String...) -> Data {
        Data("""
        <VAST><Ad><InLine><Creatives><Creative><Linear><MediaFiles>
        \(mediaFiles.joined())
        </MediaFiles></Linear></Creative></Creatives></InLine></Ad></VAST>
        """.utf8)
    }

    private func companion(
        _ url: String,
        creativeType: String,
        width: Int,
        height: Int
    ) -> String {
        """
        <Companion width="\(width)" height="\(height)">
        <StaticResource creativeType="\(creativeType)">\(url)</StaticResource>
        <CompanionClickThrough>https://click.example.com</CompanionClickThrough>
        </Companion>
        """
    }

    private func mediaFile(
        _ url: String,
        type: String,
        delivery: String,
        bitrate: Int = 0,
        width: Int = 0,
        height: Int = 0
    ) -> String {
        """
        <MediaFile type="\(type)" delivery="\(delivery)" bitrate="\(bitrate)" width="\(width)" height="\(height)">\(url)</MediaFile>
        """
    }
}
