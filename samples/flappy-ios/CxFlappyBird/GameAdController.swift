import CxAudioSDK
import Foundation
import UIKit
#if canImport(AdSupport)
import AdSupport
#endif
#if canImport(AppTrackingTransparency)
import AppTrackingTransparency
#endif

final class GameAdController: NSObject, FullscreenInterstitialListener, StickyPlayerListener {
    private let placementHash: String
    private let statusHandler: (String) -> Void
    private var interstitialHandle: FullscreenInterstitialHandle?
    private var stickyHandle: StickyPlayerHandle?
    private var interstitialFinishHandler: (() -> Void)?
    private var advertisingIdentifier: String?
    private var privacyResolutionComplete = false
    private weak var pendingReadyViewController: UIViewController?
    private weak var pendingInterstitialViewController: UIViewController?
    private var pendingInterstitialFinishHandler: (() -> Void)?

    init(statusHandler: @escaping (String) -> Void) {
        placementHash =
            Bundle.main.object(forInfoDictionaryKey: "CXPlacementHash") as? String
            ?? ""
        self.statusHandler = statusHandler
        super.init()
        #if DEBUG
        CxAudio.initialize(
            configuration: .init(
                ageTreatment: .generalAudience,
                privacyOverride: .init(
                    gdprApplies: false,
                    personalizedAdsAllowed: false,
                    usPrivacyOptOut: false,
                    usPrivacyString: "1YNN"
                )
            )
        )
        #else
        CxAudio.initialize(configuration: .init(ageTreatment: .generalAudience))
        #endif
        requestAdvertisingIdentifier()
    }

    func showReadySticky(in viewController: UIViewController) {
        guard stickyHandle == nil, interstitialHandle == nil else {
            return
        }
        guard privacyResolutionComplete else {
            pendingReadyViewController = viewController
            statusHandler("Ready-screen ad waiting for privacy choice")
            return
        }

        do {
            stickyHandle = CxStickyPlayer.show(
                in: viewController,
                request: try request(for: .stickyPlayer),
                options: StickyPlayerOptions(
                    position: .bottom,
                    showCloseButton: true,
                    volume: 0.14,
                    minDeviceVolume: 0.0
                ),
                listener: self
            )
            statusHandler("Ready-screen ad loading")
        } catch {
            stickyHandle = nil
            statusHandler("Ready-screen ad failed")
        }
    }

    func dismissReadySticky() {
        pendingReadyViewController = nil
        let activeHandle = stickyHandle
        stickyHandle = nil
        activeHandle?.dismiss()
    }

    func showInterstitial(from viewController: UIViewController, onFinished: @escaping () -> Void) {
        dismissReadySticky()

        guard privacyResolutionComplete else {
            pendingInterstitialViewController = viewController
            pendingInterstitialFinishHandler = onFinished
            statusHandler("Interstitial waiting for privacy choice")
            return
        }

        if interstitialHandle != nil {
            appendInterstitialFinishHandler(onFinished)
            statusHandler(
                interstitialHandle?.isShowing == true
                    ? "Interstitial showing"
                    : "Interstitial loading"
            )
            return
        }

        interstitialFinishHandler = onFinished

        do {
            interstitialHandle = CxFullscreenInterstitial.show(
                from: viewController,
                request: try request(for: .fullscreenInterstitial),
                options: FullscreenInterstitialOptions(
                    showCloseButton: true,
                    autoPlayVideo: true,
                    volume: 0.18,
                    minDeviceVolume: 0.0
                ),
                listener: self
            )
            statusHandler("Interstitial loading")
        } catch {
            finishInterstitial(status: failureStatus("Interstitial request failed", error: error))
        }
    }

    func stop() {
        dismissReadySticky()
        pendingInterstitialViewController = nil
        let pendingFinishHandler = pendingInterstitialFinishHandler
        pendingInterstitialFinishHandler = nil

        let activeInterstitial = interstitialHandle
        interstitialHandle = nil
        activeInterstitial?.dismiss()
        finishInterstitial(status: "Ads stopped")
        pendingFinishHandler?()
    }

    func fullscreenInterstitialDidLoad(_ handle: FullscreenInterstitialHandle) {
        interstitialHandle = handle
        statusHandler("Interstitial showing")
    }

    func fullscreenInterstitialDidStartAudio(_ handle: FullscreenInterstitialHandle) {
        interstitialHandle = handle
        statusHandler("Interstitial playing")
    }

    func fullscreenInterstitialDidCompleteAudio() {
        statusHandler("Interstitial complete")
    }

    func fullscreenInterstitialWasClicked(_ clickThroughURL: URL?) {
        statusHandler("Interstitial clicked")
    }

    func fullscreenInterstitialDidDismiss() {
        finishInterstitial(status: "Interstitial dismissed")
    }

    func fullscreenInterstitialDidFail(_ error: Error) {
        finishInterstitial(status: failureStatus("Interstitial failed", error: error))
    }

    func stickyPlayerDidLoad(_ handle: StickyPlayerHandle) {
        stickyHandle = handle
        statusHandler("Ready-screen ad showing")
    }

    func stickyPlayerDidStartAudio(_ handle: StickyPlayerHandle) {
        stickyHandle = handle
        statusHandler("Ready-screen ad playing")
    }

    func stickyPlayerDidCompleteAudio() {
        statusHandler("Ready-screen ad complete")
    }

    func stickyPlayerWasClicked(_ clickThroughURL: URL?) {
        statusHandler("Ready-screen ad clicked")
    }

    func stickyPlayerDidDismiss() {
        stickyHandle = nil
        statusHandler("Ready-screen ad dismissed")
    }

    func stickyPlayerDidFail(_ error: Error) {
        stickyHandle = nil
        statusHandler(failureStatus("Ready-screen ad failed", error: error))
    }

    private func request(for placement: ConsumablePlacement) throws -> VastRequest {
        guard !placementHash.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              placementHash != "YOUR_PLACEMENT_HASH" else {
            throw CxAudioError.invalidOption(
                "Set CXPlacementHash in Info.plist before requesting an ad"
            )
        }
        let permittedIdentifier = CxAudio.privacyState.deviceIdentifiersAllowed
            ? advertisingIdentifier
            : nil
        #if DEBUG
        let privacyState = CxAudio.privacyState
        print(
            "[CxFlappyBird] Request placement=\(placement) "
                + "privacyStatus=\(privacyState.status.rawValue) "
                + "att=\(privacyState.trackingAuthorization.rawValue) "
                + "usableIDFA=\(advertisingIdentifier != nil) "
                + "identifiersAllowed=\(privacyState.deviceIdentifiersAllowed) "
                + "personalizedAds=\(privacyState.personalizedAdsAllowed) "
                + "lmt=\(permittedIdentifier == nil ? 1 : 0)"
        )
        #endif
        let consumableRequest = try ConsumableAdRequest(
            placementHash: placementHash,
            limitAdTracking: permittedIdentifier == nil,
            appName: "Flappy Blob",
            appBundle: Bundle.main.bundleIdentifier,
            idfa: permittedIdentifier,
            ifa: permittedIdentifier,
            contentGenre: ["game"],
            contentTitle: "Flappy Blob"
        )
        return try VastRequest(
            consumableAdRequest: consumableRequest,
            placement: placement,
            timeoutInterval: 8
        )
    }

    private func requestAdvertisingIdentifier() {
        #if canImport(AdSupport) && canImport(AppTrackingTransparency)
        ATTrackingManager.requestTrackingAuthorization { [weak self] status in
            DispatchQueue.main.async {
                guard let self else { return }
                let identifier = status == .authorized
                    ? ASIdentifierManager.shared().advertisingIdentifier.uuidString
                    : nil
                self.advertisingIdentifier = Self.usableAdvertisingIdentifier(identifier)
                #if DEBUG
                CxAudio.updatePrivacyOverride(
                    .init(
                        gdprApplies: false,
                        personalizedAdsAllowed: self.advertisingIdentifier != nil,
                        usPrivacyOptOut: false,
                        usPrivacyString: "1YNN"
                    )
                )
                #endif
                #if DEBUG
                let privacyState = CxAudio.privacyState
                print(
                    "[CxFlappyBird] ATT resolved status=\(status.rawValue) "
                        + "usableIDFA=\(self.advertisingIdentifier != nil) "
                        + "privacyStatus=\(privacyState.status.rawValue) "
                        + "identifiersAllowed=\(privacyState.deviceIdentifiersAllowed) "
                        + "personalizedAds=\(privacyState.personalizedAdsAllowed)"
                )
                #endif
                self.privacyResolutionComplete = true
                self.resumePendingAdRequest()
            }
        }
        #else
        privacyResolutionComplete = true
        #endif
    }

    private func resumePendingAdRequest() {
        if let viewController = pendingInterstitialViewController,
           let finishHandler = pendingInterstitialFinishHandler {
            pendingInterstitialViewController = nil
            pendingInterstitialFinishHandler = nil
            showInterstitial(from: viewController, onFinished: finishHandler)
            return
        }

        if let viewController = pendingReadyViewController {
            pendingReadyViewController = nil
            showReadySticky(in: viewController)
        }
    }

    private static func usableAdvertisingIdentifier(_ value: String?) -> String? {
        guard let value,
              !value.isEmpty,
              value != "00000000-0000-0000-0000-000000000000" else {
            return nil
        }
        return value
    }

    private func appendInterstitialFinishHandler(_ handler: @escaping () -> Void) {
        let existingHandler = interstitialFinishHandler
        interstitialFinishHandler = {
            existingHandler?()
            handler()
        }
    }

    private func failureStatus(_ prefix: String, error: Error) -> String {
        let status = "\(prefix): \(error.localizedDescription)"
        print("[CxFlappyBird] \(status)")
        return status
    }

    private func finishInterstitial(status: String) {
        interstitialHandle = nil
        let handler = interstitialFinishHandler
        interstitialFinishHandler = nil
        statusHandler(status)
        handler?()
    }
}
