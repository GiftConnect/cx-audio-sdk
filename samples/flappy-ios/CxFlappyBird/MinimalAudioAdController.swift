import CxAudioSDK
import Foundation

final class MinimalAudioAdController: BackgroundAudioListener {
    private let placementHash = "b0816e0aa072e446e7737dd92dd34d10"
    private let statusHandler: (String) -> Void
    private var handle: BackgroundAudioHandle?

    init(statusHandler: @escaping (String) -> Void) {
        self.statusHandler = statusHandler
    }

    func playPostGameAudioAd() {
        if handle != nil {
            statusHandler(handle?.isPlaying == true ? "Ad playing" : "Ad loading")
            return
        }

        do {
            let consumableRequest = try ConsumableAdRequest(
                placementHash: placementHash,
                limitAdTracking: true,
                appName: "Flappy CX",
                appBundle: Bundle.main.bundleIdentifier,
                gdpr: false,
                contentGenre: ["game"],
                contentTitle: "Flappy CX"
            )
            let request = try VastRequest(
                consumableAdRequest: consumableRequest,
                placement: .backgroundAudio,
                timeoutInterval: 8
            )
            handle = CxBackgroundAudio.play(
                request: request,
                options: BackgroundAudioOptions(volume: 0.18, minDeviceVolume: 0.0),
                listener: self
            )
            statusHandler("Ad loading")
        } catch {
            handle = nil
            statusHandler("Ad request failed")
        }
    }

    func stop() {
        handle?.stop()
        handle = nil
    }

    func backgroundAudioDidStart(_ handle: BackgroundAudioHandle) {
        self.handle = handle
        statusHandler("Ad playing")
    }

    func backgroundAudioDidComplete() {
        handle = nil
        statusHandler("Ad complete")
    }

    func backgroundAudioDidStop() {
        handle = nil
        statusHandler("Ad stopped")
    }

    func backgroundAudioDidFail(_ error: Error) {
        handle = nil
        statusHandler("Ad failed")
    }
}
