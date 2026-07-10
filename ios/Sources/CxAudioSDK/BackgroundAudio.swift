import AVFoundation
import Foundation

public struct BackgroundAudioOptions: Equatable {
    public let volume: Float
    public let minDeviceVolume: Float

    public init(volume: Float = 0.2, minDeviceVolume: Float = 0.1) {
        self.volume = volume
        self.minDeviceVolume = minDeviceVolume
    }
}

public protocol BackgroundAudioHandle: AnyObject {
    var isPlaying: Bool { get }

    func stop()

    func setVolume(_ volume: Float)
}

public protocol BackgroundAudioListener: AnyObject {
    func backgroundAudioDidStart(_ handle: BackgroundAudioHandle)

    func backgroundAudioDidComplete()

    func backgroundAudioDidStop()

    func backgroundAudioDidFail(_ error: Error)
}

public enum CxBackgroundAudio {
    @discardableResult
    public static func play(
        request: VastRequest,
        options: BackgroundAudioOptions = BackgroundAudioOptions(),
        listener: BackgroundAudioListener
    ) -> BackgroundAudioHandle {
        let handle = BackgroundAudioHandleImpl(listener: listener, volume: options.volume)

        guard !options.volume.isNaN && options.volume >= 0 && options.volume <= 1 else {
            DispatchQueue.main.async {
                listener.backgroundAudioDidFail(
                    CxAudioError.invalidOption("volume must be between 0.0 and 1.0")
                )
            }
            return handle
        }

        guard !options.minDeviceVolume.isNaN
            && options.minDeviceVolume >= 0
            && options.minDeviceVolume <= 1 else {
            DispatchQueue.main.async {
                listener.backgroundAudioDidFail(
                    CxAudioError.invalidOption("minDeviceVolume must be between 0.0 and 1.0")
                )
            }
            return handle
        }

        guard DeviceVolumeMonitor.isDeviceVolumeAllowed(
            minDeviceVolume: options.minDeviceVolume
        ) else {
            DispatchQueue.main.async {
                listener.backgroundAudioDidFail(CxAudioError.volumeTooLow(options.minDeviceVolume))
            }
            return handle
        }

        VastAdLoader.shared.loadAudioAd(request: request) { result in
            DispatchQueue.main.async {
                guard !handle.isInactive else {
                    return
                }

                switch result {
                case .success(let audioAd):
                    let player = AVPlayer(url: audioAd.mediaFile.url)
                    handle.attach(
                        player: player,
                        trackingState: PerAdTrackingState(tracking: audioAd.tracking)
                    )
                    player.volume = options.volume
                    player.play()
                    handle.notifyStarted()
                    listener.backgroundAudioDidStart(handle)
                case .failure(let error):
                    handle.fail(error)
                }
            }
        }

        return handle
    }
}

private final class BackgroundAudioHandleImpl: BackgroundAudioHandle {
    private let listener: BackgroundAudioListener
    private var player: AVPlayer?
    private var completionObserver: NSObjectProtocol?
    private var progressObserver: Any?
    private var trackingState: PerAdTrackingState?
    private var stopped = false
    private var completed = false
    private var failed = false
    private var started = false
    private var volume: Float

    init(listener: BackgroundAudioListener, volume: Float) {
        self.listener = listener
        self.volume = volume
    }

    var isPlaying: Bool {
        player?.timeControlStatus == .playing && !isInactive
    }

    var isInactive: Bool {
        stopped || completed || failed
    }

    func stop() {
        guard !stopped && !completed else {
            return
        }

        stopped = true
        releasePlayer()
        listener.backgroundAudioDidStop()
    }

    func setVolume(_ volume: Float) {
        guard !volume.isNaN && volume >= 0 && volume <= 1 else {
            listener.backgroundAudioDidFail(
                CxAudioError.invalidOption("volume must be between 0.0 and 1.0")
            )
            return
        }

        self.volume = volume
        player?.volume = volume
    }

    func attach(player: AVPlayer, trackingState: PerAdTrackingState) {
        guard !isInactive else {
            return
        }

        self.player = player
        self.trackingState = trackingState
        player.volume = volume
        completionObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: player.currentItem,
            queue: .main
        ) { [weak self] _ in
            self?.complete()
        }
        progressObserver = player.addPeriodicTimeObserver(
            forInterval: CMTime(seconds: 0.5, preferredTimescale: 600),
            queue: .main
        ) { [weak self] time in
            self?.trackProgress(time)
        }
    }

    func notifyStarted() {
        guard !isInactive, !started else {
            return
        }

        started = true
        trackingState?.fireImpression()
        trackingState?.fireStart()
    }

    func fail(_ error: Error) {
        guard !isInactive else {
            return
        }

        failed = true
        releasePlayer()
        listener.backgroundAudioDidFail(error)
    }

    private func complete() {
        guard !isInactive else {
            return
        }

        completed = true
        trackingState?.fireComplete()
        releasePlayer()
        listener.backgroundAudioDidComplete()
    }

    private func trackProgress(_ time: CMTime) {
        guard !isInactive,
              let duration = player?.currentItem?.duration.seconds,
              duration.isFinite,
              duration > 0,
              time.seconds.isFinite else {
            return
        }
        trackingState?.fireProgress(currentPosition: time.seconds, duration: duration)
    }

    private func releasePlayer() {
        if let progressObserver, let player {
            player.removeTimeObserver(progressObserver)
            self.progressObserver = nil
        }

        player?.pause()
        player = nil

        if let completionObserver {
            NotificationCenter.default.removeObserver(completionObserver)
            self.completionObserver = nil
        }
    }
}
