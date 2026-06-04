@testable import CxAudioSDK
import XCTest

final class VastParserTests: XCTestCase {
    private let parser = VastParser()
    private let baseURL = URL(string: "https://ads.example.com/vast/root.xml")!

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
