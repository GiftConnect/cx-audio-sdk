import Foundation
import CxAudioSDK
import React
import UIKit

private let eventName = "CxAudioEvent"

@objc(CxAudioReactNative)
final class CxAudioReactNative: RCTEventEmitter {
    private var nextHandleID = 1
    private var backgroundHandles: [String: BackgroundAudioHandle] = [:]
    private var backgroundListeners: [String: BackgroundListener] = [:]
    private var stickyHandles: [String: StickyPlayerHandle] = [:]
    private var stickyListeners: [String: StickyListener] = [:]
    private var fullscreenHandles: [String: FullscreenInterstitialHandle] = [:]
    private var fullscreenListeners: [String: FullscreenListener] = [:]

    override init() {
        super.init()
        CxAudio.initialize()
    }

    override static func requiresMainQueueSetup() -> Bool {
        true
    }

    override func supportedEvents() -> [String]! {
        [eventName]
    }

    @objc(initialize:resolver:rejecter:)
    func initialize(
        _ configurationMap: NSDictionary,
        resolver resolve: RCTPromiseResolveBlock,
        rejecter reject: RCTPromiseRejectBlock
    ) {
        do {
            CxAudio.initialize(configuration: try makeAudioConfiguration(configurationMap))
            resolve(nil)
        } catch {
            reject("E_INITIALIZE", error.localizedDescription, error)
        }
    }

    @objc(updatePrivacyOverride:resolver:rejecter:)
    func updatePrivacyOverride(
        _ privacyOverrideMap: NSDictionary?,
        resolver resolve: RCTPromiseResolveBlock,
        rejecter reject: RCTPromiseRejectBlock
    ) {
        CxAudio.updatePrivacyOverride(privacyOverrideMap.map(makePrivacyOverride))
        resolve(nil)
    }

    @objc(playBackgroundAudio:options:resolver:rejecter:)
    func playBackgroundAudio(
        _ requestMap: NSDictionary,
        options optionsMap: NSDictionary,
        resolver resolve: RCTPromiseResolveBlock,
        rejecter reject: RCTPromiseRejectBlock
    ) {
        do {
            let handleID = newHandleID(placement: "backgroundAudio")
            let listener = BackgroundListener(handleID: handleID, bridge: self)
            let handle = CxBackgroundAudio.play(
                request: try makeVastRequest(requestMap),
                options: makeBackgroundOptions(optionsMap),
                listener: listener
            )
            backgroundListeners[handleID] = listener
            backgroundHandles[handleID] = handle
            resolve(handleID)
        } catch {
            reject("E_BACKGROUND_AUDIO", error.localizedDescription, error)
        }
    }

    @objc(stopBackgroundAudio:resolver:rejecter:)
    func stopBackgroundAudio(
        _ handleID: String,
        resolver resolve: RCTPromiseResolveBlock,
        rejecter reject: RCTPromiseRejectBlock
    ) {
        guard let handle = backgroundHandles.removeValue(forKey: handleID) else {
            reject("E_HANDLE_NOT_FOUND", "No background audio handle for id \(handleID)", nil)
            return
        }
        handle.stop()
        resolve(nil)
    }

    @objc(setBackgroundAudioVolume:volume:resolver:rejecter:)
    func setBackgroundAudioVolume(
        _ handleID: String,
        volume: NSNumber,
        resolver resolve: RCTPromiseResolveBlock,
        rejecter reject: RCTPromiseRejectBlock
    ) {
        guard let handle = backgroundHandles[handleID] else {
            reject("E_HANDLE_NOT_FOUND", "No background audio handle for id \(handleID)", nil)
            return
        }
        handle.setVolume(volume.floatValue)
        resolve(nil)
    }

    @objc(showStickyPlayer:options:resolver:rejecter:)
    func showStickyPlayer(
        _ requestMap: NSDictionary,
        options optionsMap: NSDictionary,
        resolver resolve: RCTPromiseResolveBlock,
        rejecter reject: RCTPromiseRejectBlock
    ) {
        do {
            guard let viewController = RCTPresentedViewController() else {
                throw CxAudioError.activityUnavailable
            }
            let handleID = newHandleID(placement: "stickyPlayer")
            let listener = StickyListener(handleID: handleID, bridge: self)
            let handle = CxStickyPlayer.show(
                in: viewController,
                request: try makeVastRequest(requestMap),
                options: try makeStickyOptions(optionsMap),
                listener: listener
            )
            stickyListeners[handleID] = listener
            stickyHandles[handleID] = handle
            resolve(handleID)
        } catch {
            reject("E_STICKY_PLAYER", error.localizedDescription, error)
        }
    }

    @objc(dismissStickyPlayer:resolver:rejecter:)
    func dismissStickyPlayer(
        _ handleID: String,
        resolver resolve: RCTPromiseResolveBlock,
        rejecter reject: RCTPromiseRejectBlock
    ) {
        guard let handle = stickyHandles.removeValue(forKey: handleID) else {
            reject("E_HANDLE_NOT_FOUND", "No sticky player handle for id \(handleID)", nil)
            return
        }
        handle.dismiss()
        resolve(nil)
    }

    @objc(showFullscreenInterstitial:options:resolver:rejecter:)
    func showFullscreenInterstitial(
        _ requestMap: NSDictionary,
        options optionsMap: NSDictionary,
        resolver resolve: RCTPromiseResolveBlock,
        rejecter reject: RCTPromiseRejectBlock
    ) {
        do {
            guard let viewController = RCTPresentedViewController() else {
                throw CxAudioError.activityUnavailable
            }
            let handleID = newHandleID(placement: "fullscreenInterstitial")
            let listener = FullscreenListener(handleID: handleID, bridge: self)
            let handle = CxFullscreenInterstitial.show(
                from: viewController,
                request: try makeVastRequest(requestMap),
                options: makeFullscreenOptions(optionsMap),
                listener: listener
            )
            fullscreenListeners[handleID] = listener
            fullscreenHandles[handleID] = handle
            resolve(handleID)
        } catch {
            reject("E_FULLSCREEN_INTERSTITIAL", error.localizedDescription, error)
        }
    }

    @objc(dismissFullscreenInterstitial:resolver:rejecter:)
    func dismissFullscreenInterstitial(
        _ handleID: String,
        resolver resolve: RCTPromiseResolveBlock,
        rejecter reject: RCTPromiseRejectBlock
    ) {
        guard let handle = fullscreenHandles.removeValue(forKey: handleID) else {
            reject(
                "E_HANDLE_NOT_FOUND",
                "No fullscreen interstitial handle for id \(handleID)",
                nil
            )
            return
        }
        handle.dismiss()
        resolve(nil)
    }

    fileprivate func emit(
        type: String,
        placement: String,
        handleID: String,
        clickThroughURL: URL? = nil,
        error: Error? = nil
    ) {
        var event: [String: Any] = [
            "type": type,
            "placement": placement,
            "handleId": handleID
        ]
        if let clickThroughURL {
            event["clickThroughUrl"] = clickThroughURL.absoluteString
        }
        if let error {
            event["message"] = error.localizedDescription
            event["errorClass"] = String(reflecting: Swift.type(of: error))
        }
        sendEvent(withName: eventName, body: event)
    }

    fileprivate func removeBackgroundHandle(_ handleID: String) {
        backgroundHandles.removeValue(forKey: handleID)
        backgroundListeners.removeValue(forKey: handleID)
    }

    fileprivate func removeStickyHandle(_ handleID: String) {
        stickyHandles.removeValue(forKey: handleID)
        stickyListeners.removeValue(forKey: handleID)
    }

    fileprivate func removeFullscreenHandle(_ handleID: String) {
        fullscreenHandles.removeValue(forKey: handleID)
        fullscreenListeners.removeValue(forKey: handleID)
    }

    private func newHandleID(placement: String) -> String {
        defer { nextHandleID += 1 }
        return "\(placement)-\(nextHandleID)"
    }

    private func makeVastRequest(_ map: NSDictionary) throws -> VastRequest {
        guard let rawURL = nonBlankString(map["vastTagUrl"]) else {
            throw CxAudioError.invalidOption("vastTagUrl is required")
        }
        let timeoutMilliseconds =
            number(map["readTimeoutMs"])?.doubleValue
            ?? number(map["connectTimeoutMs"])?.doubleValue
        let timeout = timeoutMilliseconds.map { $0 / 1_000 } ?? 8
        return try VastRequest(vastTagURLString: rawURL, timeoutInterval: timeout)
    }

    private func makeAudioConfiguration(_ map: NSDictionary) throws -> CxAudioConfiguration {
        let ageTreatment: CxAgeTreatment
        switch nonBlankString(map["ageTreatment"]) ?? "unspecified" {
        case "unspecified":
            ageTreatment = .unspecified
        case "generalAudience":
            ageTreatment = .generalAudience
        case "childDirected":
            ageTreatment = .childDirected
        case "underAgeOfConsent":
            ageTreatment = .underAgeOfConsent
        case let value:
            throw CxAudioError.invalidOption("Unsupported ageTreatment: \(value)")
        }

        return CxAudioConfiguration(
            ageTreatment: ageTreatment,
            privacyOverride: (map["privacyOverride"] as? NSDictionary).map(makePrivacyOverride)
        )
    }

    private func makePrivacyOverride(_ map: NSDictionary) -> CxPrivacyOverride {
        CxPrivacyOverride(
            gdprApplies: optionalBool(map["gdprApplies"]),
            personalizedAdsAllowed: optionalBool(map["personalizedAdsAllowed"]),
            usPrivacyOptOut: optionalBool(map["usPrivacyOptOut"]),
            tcfString: nonBlankString(map["tcfString"]),
            additionalConsent: nonBlankString(map["additionalConsent"]),
            gppString: nonBlankString(map["gppString"]),
            gppSectionIds: (map["gppSectionIds"] as? [NSNumber])?.map(\.intValue),
            usPrivacyString: nonBlankString(map["usPrivacyString"])
        )
    }

    private func makeBackgroundOptions(_ map: NSDictionary) -> BackgroundAudioOptions {
        BackgroundAudioOptions(
            volume: float(map["volume"], default: 0.2),
            minDeviceVolume: float(map["minDeviceVolume"], default: 0.1)
        )
    }

    private func makeStickyOptions(_ map: NSDictionary) throws -> StickyPlayerOptions {
        let position: StickyPlayerPosition
        switch nonBlankString(map["position"])?.lowercased() ?? "bottom" {
        case "top":
            position = .top
        case "bottom":
            position = .bottom
        default:
            throw CxAudioError.invalidOption("position must be top or bottom")
        }
        return StickyPlayerOptions(
            position: position,
            showCloseButton: bool(map["showCloseButton"], default: true),
            volume: float(map["volume"], default: 0.2),
            minDeviceVolume: float(map["minDeviceVolume"], default: 0.1)
        )
    }

    private func makeFullscreenOptions(_ map: NSDictionary) -> FullscreenInterstitialOptions {
        FullscreenInterstitialOptions(
            showCloseButton: bool(map["showCloseButton"], default: true),
            autoPlayVideo: bool(map["autoPlayVideo"], default: true),
            volume: float(map["volume"], default: 0.2),
            minDeviceVolume: float(map["minDeviceVolume"], default: 0.1)
        )
    }

    private func number(_ value: Any?) -> NSNumber? {
        value as? NSNumber
    }

    private func float(_ value: Any?, default defaultValue: Float) -> Float {
        number(value)?.floatValue ?? defaultValue
    }

    private func bool(_ value: Any?, default defaultValue: Bool) -> Bool {
        number(value)?.boolValue ?? defaultValue
    }

    private func optionalBool(_ value: Any?) -> Bool? {
        number(value)?.boolValue
    }

    private func nonBlankString(_ value: Any?) -> String? {
        guard let string = value as? String else {
            return nil
        }
        let normalized = string.trimmingCharacters(in: .whitespacesAndNewlines)
        return normalized.isEmpty ? nil : normalized
    }
}

private final class BackgroundListener: BackgroundAudioListener {
    private let handleID: String
    private weak var bridge: CxAudioReactNative?

    init(handleID: String, bridge: CxAudioReactNative) {
        self.handleID = handleID
        self.bridge = bridge
    }

    func backgroundAudioDidStart(_ handle: BackgroundAudioHandle) {
        bridge?.emit(type: "started", placement: "backgroundAudio", handleID: handleID)
    }

    func backgroundAudioDidComplete() {
        bridge?.removeBackgroundHandle(handleID)
        bridge?.emit(type: "completed", placement: "backgroundAudio", handleID: handleID)
    }

    func backgroundAudioDidStop() {
        bridge?.removeBackgroundHandle(handleID)
        bridge?.emit(type: "stopped", placement: "backgroundAudio", handleID: handleID)
    }

    func backgroundAudioDidFail(_ error: Error) {
        bridge?.removeBackgroundHandle(handleID)
        bridge?.emit(
            type: "failed",
            placement: "backgroundAudio",
            handleID: handleID,
            error: error
        )
    }
}

private final class StickyListener: StickyPlayerListener {
    private let handleID: String
    private weak var bridge: CxAudioReactNative?

    init(handleID: String, bridge: CxAudioReactNative) {
        self.handleID = handleID
        self.bridge = bridge
    }

    func stickyPlayerDidLoad(_ handle: StickyPlayerHandle) {
        bridge?.emit(type: "loaded", placement: "stickyPlayer", handleID: handleID)
    }

    func stickyPlayerDidStartAudio(_ handle: StickyPlayerHandle) {
        bridge?.emit(type: "started", placement: "stickyPlayer", handleID: handleID)
    }

    func stickyPlayerDidCompleteAudio() {
        bridge?.emit(type: "completed", placement: "stickyPlayer", handleID: handleID)
    }

    func stickyPlayerWasClicked(_ clickThroughURL: URL?) {
        bridge?.emit(
            type: "clicked",
            placement: "stickyPlayer",
            handleID: handleID,
            clickThroughURL: clickThroughURL
        )
    }

    func stickyPlayerDidDismiss() {
        bridge?.removeStickyHandle(handleID)
        bridge?.emit(type: "dismissed", placement: "stickyPlayer", handleID: handleID)
    }

    func stickyPlayerDidFail(_ error: Error) {
        bridge?.removeStickyHandle(handleID)
        bridge?.emit(
            type: "failed",
            placement: "stickyPlayer",
            handleID: handleID,
            error: error
        )
    }
}

private final class FullscreenListener: FullscreenInterstitialListener {
    private let handleID: String
    private weak var bridge: CxAudioReactNative?

    init(handleID: String, bridge: CxAudioReactNative) {
        self.handleID = handleID
        self.bridge = bridge
    }

    func fullscreenInterstitialDidLoad(_ handle: FullscreenInterstitialHandle) {
        bridge?.emit(type: "loaded", placement: "fullscreenInterstitial", handleID: handleID)
    }

    func fullscreenInterstitialDidStartAudio(_ handle: FullscreenInterstitialHandle) {
        bridge?.emit(type: "started", placement: "fullscreenInterstitial", handleID: handleID)
    }

    func fullscreenInterstitialDidCompleteAudio() {
    }

    func fullscreenInterstitialDidComplete() {
        bridge?.emit(type: "completed", placement: "fullscreenInterstitial", handleID: handleID)
    }

    func fullscreenInterstitialWasClicked(_ clickThroughURL: URL?) {
        bridge?.emit(
            type: "clicked",
            placement: "fullscreenInterstitial",
            handleID: handleID,
            clickThroughURL: clickThroughURL
        )
    }

    func fullscreenInterstitialDidDismiss() {
        bridge?.removeFullscreenHandle(handleID)
        bridge?.emit(type: "dismissed", placement: "fullscreenInterstitial", handleID: handleID)
    }

    func fullscreenInterstitialDidFail(_ error: Error) {
        bridge?.removeFullscreenHandle(handleID)
        bridge?.emit(
            type: "failed",
            placement: "fullscreenInterstitial",
            handleID: handleID,
            error: error
        )
    }
}
