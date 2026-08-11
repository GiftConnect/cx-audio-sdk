import AVFoundation
import UIKit

private let stickyPlayerHeight: CGFloat = 72
private let stickyPlayerHorizontalPadding: CGFloat = 12

public enum StickyPlayerPosition: Equatable {
    case top
    case bottom
}

public struct StickyPlayerOptions: Equatable {
    public let position: StickyPlayerPosition
    public let showCloseButton: Bool
    public let volume: Float
    public let minDeviceVolume: Float

    public init(
        position: StickyPlayerPosition = .bottom,
        showCloseButton: Bool = true,
        volume: Float = 0.2,
        minDeviceVolume: Float = 0.1
    ) {
        self.position = position
        self.showCloseButton = showCloseButton
        self.volume = volume
        self.minDeviceVolume = minDeviceVolume
    }
}

public protocol StickyPlayerHandle: AnyObject {
    var isShowing: Bool { get }

    func dismiss()
}

public protocol StickyPlayerListener: AnyObject {
    func stickyPlayerDidLoad(_ handle: StickyPlayerHandle)

    func stickyPlayerDidStartAudio(_ handle: StickyPlayerHandle)

    func stickyPlayerDidCompleteAudio()

    func stickyPlayerWasClicked(_ clickThroughURL: URL?)

    func stickyPlayerDidDismiss()

    func stickyPlayerDidFail(_ error: Error)
}

public extension StickyPlayerListener {
    func stickyPlayerDidStartAudio(_ handle: StickyPlayerHandle) {}

    func stickyPlayerDidCompleteAudio() {}
}

public enum CxStickyPlayer {
    @discardableResult
    public static func show(
        in viewController: UIViewController,
        request: VastRequest,
        options: StickyPlayerOptions = StickyPlayerOptions(),
        listener: StickyPlayerListener
    ) -> StickyPlayerHandle {
        let handle = StickyPlayerHandleImpl(listener: listener)

        guard !options.volume.isNaN && options.volume >= 0 && options.volume <= 1 else {
            DispatchQueue.main.async {
                listener.stickyPlayerDidFail(
                    CxAudioError.invalidOption("volume must be between 0.0 and 1.0")
                )
            }
            return handle
        }

        guard !options.minDeviceVolume.isNaN
            && options.minDeviceVolume >= 0
            && options.minDeviceVolume <= 1 else {
            DispatchQueue.main.async {
                listener.stickyPlayerDidFail(
                    CxAudioError.invalidOption("minDeviceVolume must be between 0.0 and 1.0")
                )
            }
            return handle
        }

        guard DeviceVolumeMonitor.isDeviceVolumeAllowed(
            minDeviceVolume: options.minDeviceVolume
        ) else {
            DispatchQueue.main.async {
                listener.stickyPlayerDidFail(CxAudioError.volumeTooLow(options.minDeviceVolume))
            }
            return handle
        }

        VastAdLoader.shared.loadStickyPlayerAd(request: request) { result in
            switch result {
            case .success(let stickyAd):
                VastAdLoader.shared.loadMedia(url: stickyAd.mediaFile.url, request: request) { mediaResult in
                    guard case .success(let localMediaURL) = mediaResult else {
                        if case .failure(let error) = mediaResult {
                            DispatchQueue.main.async { listener.stickyPlayerDidFail(error) }
                        }
                        return
                    }
                    VastAdLoader.shared.loadImage(
                    url: stickyAd.companion.staticResourceURL,
                    request: request
                ) { imageResult in
                    DispatchQueue.main.async {
                        guard !handle.isDismissed else {
                            try? FileManager.default.removeItem(at: localMediaURL)
                            return
                        }

                        switch imageResult {
                        case .success(let image):
                            attachOverlay(
                                in: viewController,
                                stickyAd: stickyAd,
                                localMediaURL: localMediaURL,
                                image: image,
                                options: options,
                                listener: listener,
                                handle: handle
                            )
                        case .failure(let error):
                            try? FileManager.default.removeItem(at: localMediaURL)
                            listener.stickyPlayerDidFail(error)
                        }
                    }
                }
                }
            case .failure(let error):
                DispatchQueue.main.async {
                    listener.stickyPlayerDidFail(error)
                }
            }
        }

        return handle
    }

    private static func attachOverlay(
        in viewController: UIViewController,
        stickyAd: VastStickyPlayerAd,
        localMediaURL: URL,
        image: UIImage,
        options: StickyPlayerOptions,
        listener: StickyPlayerListener,
        handle: StickyPlayerHandleImpl
    ) {
        guard viewController.isViewLoaded else {
            try? FileManager.default.removeItem(at: localMediaURL)
            listener.stickyPlayerDidFail(CxAudioError.activityUnavailable)
            return
        }

        let overlay = StickyPlayerOverlayView(
            companion: stickyAd.companion,
            image: image,
            options: options,
            onClick: {
                handle.fireClickTracking()
                listener.stickyPlayerWasClicked(stickyAd.companion.clickThroughURL)
                if let clickThroughURL = stickyAd.companion.clickThroughURL {
                    if let enforcedURL = try? PrivacyEnforcer.enforce(clickThroughURL, purpose: .clickThrough) {
                        UIApplication.shared.open(enforcedURL)
                    }
                }
            },
            onDismiss: {
                handle.dismiss()
            }
        )

        overlay.translatesAutoresizingMaskIntoConstraints = false
        viewController.view.addSubview(overlay)

        var constraints = [
            overlay.leadingAnchor.constraint(equalTo: viewController.view.leadingAnchor),
            overlay.trailingAnchor.constraint(equalTo: viewController.view.trailingAnchor),
            overlay.heightAnchor.constraint(equalToConstant: stickyPlayerHeight)
        ]

        switch options.position {
        case .top:
            constraints.append(overlay.topAnchor.constraint(equalTo: viewController.view.safeAreaLayoutGuide.topAnchor))
        case .bottom:
            constraints.append(overlay.bottomAnchor.constraint(equalTo: viewController.view.safeAreaLayoutGuide.bottomAnchor))
        }

        NSLayoutConstraint.activate(constraints)
        let player = AVPlayer(url: localMediaURL)
        player.volume = options.volume
        handle.attach(overlay: overlay)
        handle.attach(
            player: player,
            volume: options.volume,
            trackingState: PerAdTrackingState(tracking: stickyAd.tracking),
            temporaryMediaURL: localMediaURL
        )
        listener.stickyPlayerDidLoad(handle)
        player.play()
        handle.notifyAudioStarted()
        listener.stickyPlayerDidStartAudio(handle)
    }
}

private final class StickyPlayerHandleImpl: StickyPlayerHandle {
    private let listener: StickyPlayerListener
    private weak var overlay: UIView?
    private var player: AVPlayer?
    private var completionObserver: NSObjectProtocol?
    private var progressObserver: Any?
    private var trackingState: PerAdTrackingState?
    private var temporaryMediaURL: URL?
    private(set) var isDismissed = false
    private var completed = false
    private var failed = false
    private var audioStarted = false

    init(listener: StickyPlayerListener) {
        self.listener = listener
    }

    var isShowing: Bool {
        overlay?.superview != nil && !isInactive
    }

    private var isInactive: Bool {
        isDismissed || completed || failed
    }

    func dismiss() {
        guard !isInactive else {
            return
        }

        isDismissed = true
        releasePlayer()
        overlay?.removeFromSuperview()
        listener.stickyPlayerDidDismiss()
    }

    func attach(overlay: UIView) {
        guard !isInactive else {
            overlay.removeFromSuperview()
            return
        }
        self.overlay = overlay
    }

    func attach(
        player: AVPlayer,
        volume: Float,
        trackingState: PerAdTrackingState,
        temporaryMediaURL: URL
    ) {
        guard !isInactive else {
            player.pause()
            return
        }

        self.player = player
        self.trackingState = trackingState
        self.temporaryMediaURL = temporaryMediaURL
        player.volume = volume
        completionObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: player.currentItem,
            queue: .main
        ) { [weak self] _ in
            self?.completeAudio()
        }
        progressObserver = player.addPeriodicTimeObserver(
            forInterval: CMTime(seconds: 0.5, preferredTimescale: 600),
            queue: .main
        ) { [weak self] time in
            self?.trackProgress(time)
        }
    }

    func notifyAudioStarted() {
        guard !isInactive, !audioStarted else {
            return
        }

        audioStarted = true
        trackingState?.fireImpression()
        trackingState?.fireStart()
    }

    func fireClickTracking() {
        trackingState?.fireClick()
    }

    private func completeAudio() {
        guard !isInactive else {
            return
        }

        completed = true
        trackingState?.fireComplete()
        releasePlayer()
        listener.stickyPlayerDidCompleteAudio()
        overlay?.removeFromSuperview()
        listener.stickyPlayerDidDismiss()
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
        if let temporaryMediaURL {
            try? FileManager.default.removeItem(at: temporaryMediaURL)
            self.temporaryMediaURL = nil
        }

        if let completionObserver {
            NotificationCenter.default.removeObserver(completionObserver)
            self.completionObserver = nil
        }
    }

}

private final class StickyPlayerOverlayView: UIView {
    private let onClick: () -> Void
    private let onDismiss: () -> Void

    init(
        companion: VastCompanion,
        image: UIImage,
        options: StickyPlayerOptions,
        onClick: @escaping () -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.onClick = onClick
        self.onDismiss = onDismiss

        super.init(frame: .zero)

        backgroundColor = UIColor(white: 0.07, alpha: 1)
        accessibilityLabel = "Advertisement"
        isAccessibilityElement = true

        let backgroundImageView = UIImageView(image: image)
        backgroundImageView.contentMode = .scaleAspectFill
        backgroundImageView.alpha = 0.32
        backgroundImageView.clipsToBounds = true
        backgroundImageView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(backgroundImageView)

        NSLayoutConstraint.activate([
            backgroundImageView.leadingAnchor.constraint(equalTo: leadingAnchor),
            backgroundImageView.trailingAnchor.constraint(equalTo: trailingAnchor),
            backgroundImageView.topAnchor.constraint(equalTo: topAnchor),
            backgroundImageView.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])

        let imageView = UIImageView(image: image)
        imageView.contentMode = .scaleAspectFit
        imageView.clipsToBounds = true
        imageView.isUserInteractionEnabled = true
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(handleClick)))
        addSubview(imageView)

        let aspectWidth: CGFloat
        if companion.width > 0 && companion.height > 0 {
            aspectWidth = stickyPlayerHeight * CGFloat(companion.width) / CGFloat(companion.height)
        } else {
            aspectWidth = 86
        }
        let imageWidth = min(max(86, aspectWidth), max(86, stickyPlayerHeight * 1.35))
        NSLayoutConstraint.activate([
            imageView.centerXAnchor.constraint(equalTo: centerXAnchor),
            imageView.topAnchor.constraint(equalTo: topAnchor, constant: 6),
            imageView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -6),
            imageView.widthAnchor.constraint(equalToConstant: max(56, imageWidth)),
            imageView.leadingAnchor.constraint(greaterThanOrEqualTo: leadingAnchor, constant: stickyPlayerHorizontalPadding),
            imageView.trailingAnchor.constraint(lessThanOrEqualTo: trailingAnchor, constant: -stickyPlayerHorizontalPadding)
        ])

        if options.showCloseButton {
            let closeButton = UIButton(type: .system)
            closeButton.setTitle("×", for: .normal)
            closeButton.tintColor = .white
            closeButton.titleLabel?.font = .systemFont(ofSize: 28, weight: .regular)
            closeButton.accessibilityLabel = "Close advertisement"
            closeButton.translatesAutoresizingMaskIntoConstraints = false
            closeButton.addTarget(self, action: #selector(handleDismiss), for: .touchUpInside)
            addSubview(closeButton)

            NSLayoutConstraint.activate([
                closeButton.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -4),
                closeButton.centerYAnchor.constraint(equalTo: centerYAnchor),
                closeButton.widthAnchor.constraint(equalToConstant: 44),
                closeButton.heightAnchor.constraint(equalToConstant: 44)
            ])
        }
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        return nil
    }

    @objc private func handleClick() {
        onClick()
    }

    @objc private func handleDismiss() {
        onDismiss()
    }
}
