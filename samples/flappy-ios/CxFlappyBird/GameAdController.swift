import CxAudioSDK
import Foundation
import UIKit

final class GameAdController: NSObject, FullscreenInterstitialListener, StickyPlayerListener {
    private let placementHash = "b0816e0aa072e446e7737dd92dd34d10"
    private let statusHandler: (String) -> Void
    private var interstitialHandle: FullscreenInterstitialHandle?
    private var stickyHandle: StickyPlayerHandle?
    private var interstitialFinishHandler: (() -> Void)?

    init(statusHandler: @escaping (String) -> Void) {
        self.statusHandler = statusHandler
    }

    func showReadySticky(in viewController: UIViewController) {
        guard stickyHandle == nil, interstitialHandle == nil else {
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
        let activeHandle = stickyHandle
        stickyHandle = nil
        activeHandle?.dismiss()
    }

    func showInterstitial(from viewController: UIViewController, onFinished: @escaping () -> Void) {
        dismissReadySticky()

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
            finishInterstitial(status: "Interstitial request failed")
        }
    }

    func stop() {
        dismissReadySticky()

        let activeInterstitial = interstitialHandle
        interstitialHandle = nil
        activeInterstitial?.dismiss()
        finishInterstitial(status: "Ads stopped")
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
        finishInterstitial(status: "Interstitial failed")
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
        statusHandler("Ready-screen ad failed")
    }

    private func request(for placement: ConsumablePlacement) throws -> VastRequest {
        let consumableRequest = try ConsumableAdRequest(
            placementHash: placementHash,
            limitAdTracking: true,
            appName: "Flappy Blob",
            appBundle: Bundle.main.bundleIdentifier,
            gdpr: false,
            contentGenre: ["game"],
            contentTitle: "Flappy Blob"
        )
        return try VastRequest(
            consumableAdRequest: consumableRequest,
            placement: placement,
            timeoutInterval: 8
        )
    }

    private func appendInterstitialFinishHandler(_ handler: @escaping () -> Void) {
        let existingHandler = interstitialFinishHandler
        interstitialFinishHandler = {
            existingHandler?()
            handler()
        }
    }

    private func finishInterstitial(status: String) {
        interstitialHandle = nil
        let handler = interstitialFinishHandler
        interstitialFinishHandler = nil
        statusHandler(status)
        handler?()
    }
}
