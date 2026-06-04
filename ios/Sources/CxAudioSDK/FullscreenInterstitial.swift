import AVFoundation
import UIKit

public struct FullscreenInterstitialOptions: Equatable {
    public let showCloseButton: Bool
    public let companionHeight: CGFloat
    public let companionMaxWidth: CGFloat
    public let autoPlayVideo: Bool
    public let volume: Float
    public let minDeviceVolume: Float

    public init(
        showCloseButton: Bool = true,
        companionHeight: CGFloat = 250,
        companionMaxWidth: CGFloat = 300,
        autoPlayVideo: Bool = true,
        volume: Float = 0.2,
        minDeviceVolume: Float = 0.1
    ) {
        self.showCloseButton = showCloseButton
        self.companionHeight = companionHeight
        self.companionMaxWidth = companionMaxWidth
        self.autoPlayVideo = autoPlayVideo
        self.volume = volume
        self.minDeviceVolume = minDeviceVolume
    }
}

public protocol FullscreenInterstitialHandle: AnyObject {
    var isShowing: Bool { get }

    func dismiss()
}

public protocol FullscreenInterstitialListener: AnyObject {
    func fullscreenInterstitialDidLoad(_ handle: FullscreenInterstitialHandle)

    func fullscreenInterstitialDidStartAudio(_ handle: FullscreenInterstitialHandle)

    func fullscreenInterstitialDidCompleteAudio()

    func fullscreenInterstitialWasClicked(_ clickThroughURL: URL?)

    func fullscreenInterstitialDidDismiss()

    func fullscreenInterstitialDidFail(_ error: Error)
}

public extension FullscreenInterstitialListener {
    func fullscreenInterstitialDidStartAudio(_ handle: FullscreenInterstitialHandle) {}

    func fullscreenInterstitialDidCompleteAudio() {}
}

public enum CxFullscreenInterstitial {
    @discardableResult
    public static func show(
        from viewController: UIViewController,
        request: VastRequest,
        options: FullscreenInterstitialOptions = FullscreenInterstitialOptions(),
        listener: FullscreenInterstitialListener
    ) -> FullscreenInterstitialHandle {
        let handle = FullscreenInterstitialHandleImpl(listener: listener)

        guard !options.volume.isNaN && options.volume >= 0 && options.volume <= 1 else {
            DispatchQueue.main.async {
                listener.fullscreenInterstitialDidFail(
                    CxAudioError.invalidOption("volume must be between 0.0 and 1.0")
                )
            }
            return handle
        }

        guard !options.minDeviceVolume.isNaN
            && options.minDeviceVolume >= 0
            && options.minDeviceVolume <= 1 else {
            DispatchQueue.main.async {
                listener.fullscreenInterstitialDidFail(
                    CxAudioError.invalidOption("minDeviceVolume must be between 0.0 and 1.0")
                )
            }
            return handle
        }

        guard DeviceVolumeMonitor.isDeviceVolumeAllowed(
            minDeviceVolume: options.minDeviceVolume
        ) else {
            DispatchQueue.main.async {
                listener.fullscreenInterstitialDidFail(CxAudioError.volumeTooLow(options.minDeviceVolume))
            }
            return handle
        }

        VastAdLoader.shared.loadInterstitial(request: request) { result in
            switch result {
            case .success(let interstitialAd):
                loadImages(for: interstitialAd, request: request) { imageResult in
                    DispatchQueue.main.async {
                        guard !handle.isDismissed else {
                            return
                        }

                        switch imageResult {
                        case .success(let images):
                            attachOverlay(
                                from: viewController,
                                interstitialAd: interstitialAd,
                                mediaImage: images.mediaImage,
                                companionImage: images.companionImage,
                                options: options,
                                listener: listener,
                                handle: handle
                            )
                        case .failure(let error):
                            listener.fullscreenInterstitialDidFail(error)
                        }
                    }
                }
            case .failure(let error):
                DispatchQueue.main.async {
                    listener.fullscreenInterstitialDidFail(error)
                }
            }
        }

        return handle
    }

    private static func attachOverlay(
        from viewController: UIViewController,
        interstitialAd: VastInterstitialAd,
        mediaImage: UIImage?,
        companionImage: UIImage?,
        options: FullscreenInterstitialOptions,
        listener: FullscreenInterstitialListener,
        handle: FullscreenInterstitialHandleImpl
    ) {
        guard viewController.isViewLoaded else {
            listener.fullscreenInterstitialDidFail(CxAudioError.activityUnavailable)
            return
        }

        let overlay = FullscreenInterstitialOverlayView(
            interstitialAd: interstitialAd,
            mediaImage: mediaImage,
            companionImage: companionImage,
            options: options,
            onCompanionClick: {
                listener.fullscreenInterstitialWasClicked(interstitialAd.companion?.clickThroughURL)
                if let clickThroughURL = interstitialAd.companion?.clickThroughURL {
                    UIApplication.shared.open(clickThroughURL)
                }
            },
            onDismiss: {
                handle.dismiss()
            }
        )

        overlay.translatesAutoresizingMaskIntoConstraints = false
        viewController.view.addSubview(overlay)
        NSLayoutConstraint.activate([
            overlay.leadingAnchor.constraint(equalTo: viewController.view.leadingAnchor),
            overlay.trailingAnchor.constraint(equalTo: viewController.view.trailingAnchor),
            overlay.topAnchor.constraint(equalTo: viewController.view.topAnchor),
            overlay.bottomAnchor.constraint(equalTo: viewController.view.bottomAnchor)
        ])
        handle.attach(overlay: overlay)
        listener.fullscreenInterstitialDidLoad(handle)
        if let audioMediaFile = interstitialAd.audioMediaFile,
           interstitialAd.mediaFile?.isVideo != true {
            let player = AVPlayer(url: audioMediaFile.url)
            player.volume = options.volume
            handle.attach(player: player)
            player.play()
            listener.fullscreenInterstitialDidStartAudio(handle)
        }
    }

    private static func loadImages(
        for interstitialAd: VastInterstitialAd,
        request: VastRequest,
        completion: @escaping (Result<(mediaImage: UIImage?, companionImage: UIImage?), Error>) -> Void
    ) {
        let group = DispatchGroup()
        let lock = NSLock()
        var firstError: Error?
        var mediaImage: UIImage?
        var companionImage: UIImage?

        if let mediaFile = interstitialAd.mediaFile, mediaFile.isImage {
            group.enter()
            VastAdLoader.shared.loadImage(url: mediaFile.url, request: request) { result in
                lock.lock()
                defer {
                    lock.unlock()
                    group.leave()
                }

                switch result {
                case .success(let image):
                    mediaImage = image
                case .failure(let error):
                    firstError = firstError ?? error
                }
            }
        }

        if let companion = interstitialAd.companion {
            group.enter()
            VastAdLoader.shared.loadImage(url: companion.staticResourceURL, request: request) { result in
                lock.lock()
                defer {
                    lock.unlock()
                    group.leave()
                }

                switch result {
                case .success(let image):
                    companionImage = image
                case .failure(let error):
                    firstError = firstError ?? error
                }
            }
        }

        group.notify(queue: .global()) {
            if let firstError {
                completion(.failure(firstError))
            } else {
                completion(.success((mediaImage: mediaImage, companionImage: companionImage)))
            }
        }
    }
}

private final class FullscreenInterstitialHandleImpl: FullscreenInterstitialHandle {
    private let listener: FullscreenInterstitialListener
    private weak var overlay: FullscreenInterstitialOverlayView?
    private var player: AVPlayer?
    private var completionObserver: NSObjectProtocol?
    private(set) var isDismissed = false
    private var completed = false
    private var failed = false

    init(listener: FullscreenInterstitialListener) {
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
        overlay?.dismiss()
        listener.fullscreenInterstitialDidDismiss()
    }

    func attach(overlay: FullscreenInterstitialOverlayView) {
        guard !isInactive else {
            overlay.removeFromSuperview()
            return
        }
        self.overlay = overlay
    }

    func attach(player: AVPlayer) {
        guard !isInactive else {
            player.pause()
            return
        }

        self.player = player
        completionObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: player.currentItem,
            queue: .main
        ) { [weak self] _ in
            self?.completeAudio()
        }
    }

    private func completeAudio() {
        guard !isInactive else {
            return
        }

        completed = true
        releasePlayer()
        listener.fullscreenInterstitialDidCompleteAudio()
        overlay?.dismiss()
        listener.fullscreenInterstitialDidDismiss()
    }

    private func releasePlayer() {
        player?.pause()
        player = nil

        if let completionObserver {
            NotificationCenter.default.removeObserver(completionObserver)
            self.completionObserver = nil
        }
    }

}

private final class FullscreenInterstitialOverlayView: UIView {
    private let onCompanionClick: () -> Void
    private let onDismiss: () -> Void
    private var player: AVPlayer?

    init(
        interstitialAd: VastInterstitialAd,
        mediaImage: UIImage?,
        companionImage: UIImage?,
        options: FullscreenInterstitialOptions,
        onCompanionClick: @escaping () -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.onCompanionClick = onCompanionClick
        self.onDismiss = onDismiss

        super.init(frame: .zero)

        backgroundColor = .black
        accessibilityLabel = "Advertisement"
        isAccessibilityElement = true

        let contentStack = UIStackView()
        contentStack.axis = .vertical
        contentStack.alignment = .fill
        contentStack.distribution = .fill
        contentStack.translatesAutoresizingMaskIntoConstraints = false
        addSubview(contentStack)

        NSLayoutConstraint.activate([
            contentStack.leadingAnchor.constraint(equalTo: leadingAnchor),
            contentStack.trailingAnchor.constraint(equalTo: trailingAnchor),
            contentStack.topAnchor.constraint(equalTo: topAnchor),
            contentStack.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])

        let mediaView = makeMediaView(
            mediaFile: interstitialAd.mediaFile,
            mediaImage: mediaImage,
            autoPlayVideo: options.autoPlayVideo
        )
        contentStack.addArrangedSubview(mediaView)

        if let companionImage, interstitialAd.companion != nil {
            let companionContainer = makeCompanionView(
                image: companionImage,
                maxWidth: options.companionMaxWidth
            )
            contentStack.addArrangedSubview(companionContainer)
            companionContainer.heightAnchor.constraint(equalToConstant: options.companionHeight).isActive = true
        }

        if options.showCloseButton {
            let closeButton = UIButton(type: .system)
            closeButton.setTitle("×", for: .normal)
            closeButton.tintColor = .white
            closeButton.titleLabel?.font = .systemFont(ofSize: 32, weight: .regular)
            closeButton.accessibilityLabel = "Close advertisement"
            closeButton.translatesAutoresizingMaskIntoConstraints = false
            closeButton.addTarget(self, action: #selector(handleDismiss), for: .touchUpInside)
            addSubview(closeButton)

            NSLayoutConstraint.activate([
                closeButton.topAnchor.constraint(equalTo: safeAreaLayoutGuide.topAnchor, constant: 8),
                closeButton.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -12),
                closeButton.widthAnchor.constraint(equalToConstant: 48),
                closeButton.heightAnchor.constraint(equalToConstant: 48)
            ])
        }
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        return nil
    }

    private func makeMediaView(
        mediaFile: VastMediaFile?,
        mediaImage: UIImage?,
        autoPlayVideo: Bool
    ) -> UIView {
        if let mediaFile, mediaFile.isImage, let mediaImage {
            let imageView = UIImageView(image: mediaImage)
            imageView.contentMode = .scaleAspectFit
            imageView.backgroundColor = .black
            return imageView
        }

        if let mediaFile, mediaFile.isVideo {
            let player = AVPlayer(url: mediaFile.url)
            let videoContainer = VideoPlayerView(player: player)
            self.player = player

            if autoPlayVideo {
                player.play()
            }

            return videoContainer
        }

        let label = UILabel()
        label.text = "Advertisement"
        label.textAlignment = .center
        label.textColor = .white
        label.backgroundColor = .black
        return label
    }

    private func makeCompanionView(image: UIImage, maxWidth: CGFloat) -> UIView {
        let container = UIView()
        container.backgroundColor = UIColor(white: 0.07, alpha: 1)

        let imageView = UIImageView(image: image)
        imageView.contentMode = .scaleAspectFit
        imageView.isUserInteractionEnabled = true
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(handleCompanionClick)))
        container.addSubview(imageView)

        NSLayoutConstraint.activate([
            imageView.centerXAnchor.constraint(equalTo: container.centerXAnchor),
            imageView.topAnchor.constraint(equalTo: container.topAnchor, constant: 8),
            imageView.bottomAnchor.constraint(equalTo: container.bottomAnchor, constant: -8),
            imageView.widthAnchor.constraint(lessThanOrEqualToConstant: maxWidth),
            imageView.leadingAnchor.constraint(greaterThanOrEqualTo: container.leadingAnchor, constant: 8),
            imageView.trailingAnchor.constraint(lessThanOrEqualTo: container.trailingAnchor, constant: -8)
        ])

        return container
    }

    @objc private func handleCompanionClick() {
        onCompanionClick()
    }

    @objc private func handleDismiss() {
        onDismiss()
    }

    func dismiss() {
        player?.pause()
        removeFromSuperview()
    }

    deinit {
        player?.pause()
    }
}

private final class VideoPlayerView: UIView {
    private let videoLayer: AVPlayerLayer

    init(player: AVPlayer) {
        self.videoLayer = AVPlayerLayer(player: player)
        super.init(frame: .zero)
        backgroundColor = .black
        videoLayer.videoGravity = .resizeAspect
        layer.addSublayer(videoLayer)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        videoLayer.frame = bounds
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        return nil
    }
}
