import UIKit

public enum StickyPlayerPosition: Equatable {
    case top
    case bottom
}

public struct StickyPlayerOptions: Equatable {
    public let position: StickyPlayerPosition
    public let height: CGFloat
    public let horizontalPadding: CGFloat
    public let showCloseButton: Bool

    public init(
        position: StickyPlayerPosition = .bottom,
        height: CGFloat = 72,
        horizontalPadding: CGFloat = 12,
        showCloseButton: Bool = true
    ) {
        self.position = position
        self.height = height
        self.horizontalPadding = horizontalPadding
        self.showCloseButton = showCloseButton
    }
}

public protocol StickyPlayerHandle: AnyObject {
    var isShowing: Bool { get }

    func dismiss()
}

public protocol StickyPlayerListener: AnyObject {
    func stickyPlayerDidLoad(_ handle: StickyPlayerHandle)

    func stickyPlayerWasClicked(_ clickThroughURL: URL?)

    func stickyPlayerDidDismiss()

    func stickyPlayerDidFail(_ error: Error)
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

        VastAdLoader.shared.loadCompanion(request: request) { result in
            switch result {
            case .success(let companion):
                VastAdLoader.shared.loadImage(
                    url: companion.staticResourceURL,
                    request: request
                ) { imageResult in
                    DispatchQueue.main.async {
                        guard !handle.isDismissed else {
                            return
                        }

                        switch imageResult {
                        case .success(let image):
                            attachOverlay(
                                in: viewController,
                                companion: companion,
                                image: image,
                                options: options,
                                listener: listener,
                                handle: handle
                            )
                        case .failure(let error):
                            listener.stickyPlayerDidFail(error)
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
        companion: VastCompanion,
        image: UIImage,
        options: StickyPlayerOptions,
        listener: StickyPlayerListener,
        handle: StickyPlayerHandleImpl
    ) {
        guard viewController.isViewLoaded else {
            listener.stickyPlayerDidFail(CxAudioError.activityUnavailable)
            return
        }

        let overlay = StickyPlayerOverlayView(
            companion: companion,
            image: image,
            options: options,
            onClick: {
                listener.stickyPlayerWasClicked(companion.clickThroughURL)
                if let clickThroughURL = companion.clickThroughURL {
                    UIApplication.shared.open(clickThroughURL)
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
            overlay.heightAnchor.constraint(equalToConstant: options.height)
        ]

        switch options.position {
        case .top:
            constraints.append(overlay.topAnchor.constraint(equalTo: viewController.view.safeAreaLayoutGuide.topAnchor))
        case .bottom:
            constraints.append(overlay.bottomAnchor.constraint(equalTo: viewController.view.safeAreaLayoutGuide.bottomAnchor))
        }

        NSLayoutConstraint.activate(constraints)
        handle.attach(overlay: overlay)
        listener.stickyPlayerDidLoad(handle)
    }
}

private final class StickyPlayerHandleImpl: StickyPlayerHandle {
    private let listener: StickyPlayerListener
    private weak var overlay: UIView?
    private(set) var isDismissed = false

    init(listener: StickyPlayerListener) {
        self.listener = listener
    }

    var isShowing: Bool {
        overlay?.superview != nil && !isDismissed
    }

    func dismiss() {
        guard !isDismissed else {
            return
        }

        isDismissed = true
        overlay?.removeFromSuperview()
        listener.stickyPlayerDidDismiss()
    }

    func attach(overlay: UIView) {
        guard !isDismissed else {
            overlay.removeFromSuperview()
            return
        }
        self.overlay = overlay
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
            aspectWidth = options.height * CGFloat(companion.width) / CGFloat(companion.height)
        } else {
            aspectWidth = 86
        }
        let imageWidth = min(max(86, aspectWidth), max(86, options.height * 1.35))
        NSLayoutConstraint.activate([
            imageView.centerXAnchor.constraint(equalTo: centerXAnchor),
            imageView.topAnchor.constraint(equalTo: topAnchor, constant: 6),
            imageView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -6),
            imageView.widthAnchor.constraint(equalToConstant: max(56, imageWidth)),
            imageView.leadingAnchor.constraint(greaterThanOrEqualTo: leadingAnchor, constant: options.horizontalPadding),
            imageView.trailingAnchor.constraint(lessThanOrEqualTo: trailingAnchor, constant: -options.horizontalPadding)
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
