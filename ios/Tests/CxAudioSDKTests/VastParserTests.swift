@testable import CxAudioSDK
import XCTest

final class VastParserTests: XCTestCase {
    private let parser = VastParser()
    private let baseURL = URL(string: "https://ads.example.com/vast/root.xml")!

    func testConsumableAdRequestBuildsEncodedVastURL() throws {
        let consumableRequest = try ConsumableAdRequest(
            placementHash: "abc123",
            cacheBuster: "fixed",
            limitAdTracking: false,
            appStoreURL: "https://apps.apple.com/app/id123",
            appName: "Test App",
            appBundle: "com.example.app",
            gdpr: true,
            gdprConsent: "consent string",
            position: "preroll",
            mediaMimes: ["audio/mpeg", "audio/mp4"],
            companionSizes: ["300x250", "320x50"],
            customParameters: ["x_custom": "hello world"]
        )

        let url = try consumableRequest.url()
        let components = try XCTUnwrap(URLComponents(url: url, resolvingAgainstBaseURL: false))
        let queryItems = Dictionary<String, String>(
            uniqueKeysWithValues: (components.queryItems ?? [URLQueryItem]()).map { ($0.name, $0.value ?? "") }
        )

        XCTAssertEqual(url.scheme, "https")
        XCTAssertEqual(url.host, "2.e.serverbid.com")
        XCTAssertEqual(queryItems["s"], "abc123")
        XCTAssertEqual(queryItems["rt"], "vast")
        XCTAssertEqual(queryItems["secure"], "1")
        XCTAssertEqual(queryItems["cb"], "fixed")
        XCTAssertEqual(queryItems["appname"], "Test App")
        XCTAssertEqual(queryItems["appbundle"], "com.example.app")
        XCTAssertEqual(queryItems["appstoreurl"], "https://apps.apple.com/app/id123")
        XCTAssertEqual(queryItems["lmt"], "0")
        XCTAssertEqual(queryItems["gdpr"], "1")
        XCTAssertEqual(queryItems["gdpr_consent"], "consent string")
        XCTAssertEqual(queryItems["v_position"], "preroll")
        XCTAssertEqual(queryItems["v_delay"], "-1")
        XCTAssertEqual(queryItems["v_delivery"], "2")
        XCTAssertEqual(queryItems["v_mimes"], "audio/mpeg,audio/mp4")
        XCTAssertEqual(queryItems["v_cmp_sizes"], "300x250,320x50")
        XCTAssertEqual(queryItems["v_pm"], "1")
        XCTAssertEqual(queryItems["c_audio_feed_type"], "6")
        XCTAssertEqual(queryItems["x_custom"], "hello world")

        let vastRequest = try VastRequest(consumableAdRequest: consumableRequest)
        XCTAssertEqual(vastRequest.vastTagURL, url)
    }

    func testConsumableAdRequestDefaultsCacheBusterToMillisecondTimestamp() throws {
        let consumableRequest = try ConsumableAdRequest(placementHash: "abc123")

        let url = try consumableRequest.url()
        let items = queryItems(from: url)
        let cacheBuster = try XCTUnwrap(items["cb"])

        XCTAssertNotNil(cacheBuster.range(of: #"^\d+$"#, options: .regularExpression))
    }

    func testConsumableAdRequestAppliesPlacementAutomaticParameters() throws {
        let consumableRequest = try ConsumableAdRequest(
            placementHash: "abc123",
            cacheBuster: "fixed"
        )

        let stickyURL = try VastRequest(
            consumableAdRequest: consumableRequest,
            placement: .stickyPlayer
        ).vastTagURL
        let stickyItems = queryItems(from: stickyURL)
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
        XCTAssertEqual(interstitialItems["v_plcmt"], "3")
        XCTAssertEqual(interstitialItems["v_linearity"], "1")
        XCTAssertEqual(interstitialItems["v_min_bitrate"], "64")
        XCTAssertEqual(interstitialItems["v_max_bitrate"], "2500")
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
