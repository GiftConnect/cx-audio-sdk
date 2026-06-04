import CxAudioSDK
import UIKit

final class SampleViewController: UIViewController {
    private let defaultVastTagURL = "https://ads.example.com/vast.xml"
    private let blue = UIColor(red: 25 / 255, green: 111 / 255, blue: 215 / 255, alpha: 1)
    private let green = UIColor(red: 36 / 255, green: 172 / 255, blue: 54 / 255, alpha: 1)
    private let purple = UIColor(red: 111 / 255, green: 78 / 255, blue: 198 / 255, alpha: 1)
    private let ink = UIColor(red: 12 / 255, green: 28 / 255, blue: 58 / 255, alpha: 1)

    private let vastTagField = UITextView()
    private let statusLabel = UILabel()

    private var backgroundAudioHandle: BackgroundAudioHandle?
    private var stickyPlayerHandle: StickyPlayerHandle?
    private var fullscreenInterstitialHandle: FullscreenInterstitialHandle?

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "CX Audio SDK"
        view.backgroundColor = UIColor(red: 244 / 255, green: 248 / 255, blue: 252 / 255, alpha: 1)
        buildInterface()
        updateStatus("Ready. Replace the VAST tag URL with a known-good test tag before running placements.")
    }

    private func buildInterface() {
        let scrollView = UIScrollView()
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(scrollView)

        let content = UIStackView()
        content.axis = .vertical
        content.spacing = 12
        content.translatesAutoresizingMaskIntoConstraints = false
        scrollView.addSubview(content)

        NSLayoutConstraint.activate([
            scrollView.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor),
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),

            content.leadingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.leadingAnchor, constant: 20),
            content.trailingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.trailingAnchor, constant: -20),
            content.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor, constant: 24),
            content.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor, constant: -24),
            content.widthAnchor.constraint(equalTo: scrollView.frameLayoutGuide.widthAnchor, constant: -40)
        ])

        content.addArrangedSubview(headerView())

        let fieldLabel = UILabel()
        fieldLabel.text = "Test VAST tag URL"
        fieldLabel.font = .preferredFont(forTextStyle: .subheadline)
        fieldLabel.textColor = ink
        content.addArrangedSubview(fieldLabel)

        vastTagField.text = defaultVastTagURL
        vastTagField.font = .preferredFont(forTextStyle: .body)
        vastTagField.autocapitalizationType = .none
        vastTagField.autocorrectionType = .no
        vastTagField.keyboardType = .URL
        vastTagField.layer.borderColor = UIColor.separator.cgColor
        vastTagField.layer.borderWidth = 1
        vastTagField.layer.cornerRadius = 10
        vastTagField.backgroundColor = .white
        vastTagField.textContainerInset = UIEdgeInsets(top: 10, left: 8, bottom: 10, right: 8)
        content.addArrangedSubview(vastTagField)
        vastTagField.heightAnchor.constraint(greaterThanOrEqualToConstant: 76).isActive = true

        let placementTitle = UILabel()
        placementTitle.text = "3 PLACEMENT TYPES. MAXIMUM IMPACT."
        placementTitle.font = .preferredFont(forTextStyle: .headline)
        placementTitle.textAlignment = .center
        placementTitle.textColor = ink
        content.addArrangedSubview(placementTitle)

        content.addArrangedSubview(placementCard(
            title: "Background Audio",
            subtitle: "Audio ads play at low volume during gameplay or app usage.",
            badge: "AUDIO",
            accentColor: blue,
            bullets: [
                "VAST 4.x audio support",
                "Streams audio with low-latency playback",
                "Volume control and lifecycle callbacks"
            ],
            primaryTitle: "Play background audio",
            primaryAction: #selector(playBackgroundAudio),
            secondaryTitle: "Stop",
            secondaryAction: #selector(stopBackgroundAudio)
        ))

        content.addArrangedSubview(placementCard(
            title: "Sticky Player",
            subtitle: "Clickable companion banner stays at the top or bottom while users engage.",
            badge: "STICKY",
            accentColor: green,
            bullets: [
                "Shows scaled 300x250 companion creative",
                "Top or bottom placement options",
                "Clickable and dismissible"
            ],
            primaryTitle: "Show sticky player",
            primaryAction: #selector(showStickyPlayer),
            secondaryTitle: "Dismiss",
            secondaryAction: #selector(dismissStickyPlayer)
        ))

        content.addArrangedSubview(placementCard(
            title: "Full Screen / Interstitial",
            subtitle: "Engaging full screen experience with optional companion banner action.",
            badge: "FULL",
            accentColor: purple,
            bullets: [
                "Supports image and video media",
                "Optional 300x250 companion ad",
                "Close button and programmatic dismiss"
            ],
            primaryTitle: "Show interstitial",
            primaryAction: #selector(showFullscreenInterstitial),
            secondaryTitle: "Dismiss",
            secondaryAction: #selector(dismissFullscreenInterstitial)
        ))

        statusLabel.numberOfLines = 0
        statusLabel.font = .preferredFont(forTextStyle: .body)
        statusLabel.textColor = ink
        statusLabel.backgroundColor = .white
        statusLabel.layer.borderColor = UIColor.separator.cgColor
        statusLabel.layer.borderWidth = 1
        statusLabel.layer.cornerRadius = 10
        statusLabel.layer.masksToBounds = true
        statusLabel.layoutMargins = UIEdgeInsets(top: 12, left: 12, bottom: 12, right: 12)
        content.addArrangedSubview(statusLabel)
    }

    private func headerView() -> UIView {
        let header = UIStackView()
        header.axis = .vertical
        header.spacing = 8
        header.backgroundColor = UIColor(red: 2 / 255, green: 6 / 255, blue: 12 / 255, alpha: 1)
        header.layer.cornerRadius = 12
        header.layoutMargins = UIEdgeInsets(top: 18, left: 18, bottom: 18, right: 18)
        header.isLayoutMarginsRelativeArrangement = true

        let brand = UILabel()
        brand.text = "CX  AUDIO SDK"
        brand.font = .systemFont(ofSize: 30, weight: .bold)
        brand.textColor = .white
        header.addArrangedSubview(brand)

        let tagline = UILabel()
        tagline.text = "Integrate audio ads. Drive results."
        tagline.font = .systemFont(ofSize: 18, weight: .bold)
        tagline.textColor = UIColor(red: 69 / 255, green: 151 / 255, blue: 255 / 255, alpha: 1)
        tagline.numberOfLines = 0
        header.addArrangedSubview(tagline)

        let summary = UILabel()
        summary.text = "A mobile SDK demo for VAST 4.x audio ad placements."
        summary.font = .preferredFont(forTextStyle: .body)
        summary.textColor = UIColor(red: 225 / 255, green: 231 / 255, blue: 240 / 255, alpha: 1)
        summary.numberOfLines = 0
        header.addArrangedSubview(summary)

        return header
    }

    private func placementCard(
        title: String,
        subtitle: String,
        badge: String,
        accentColor: UIColor,
        bullets: [String],
        primaryTitle: String,
        primaryAction: Selector,
        secondaryTitle: String,
        secondaryAction: Selector
    ) -> UIView {
        let card = UIStackView()
        card.axis = .vertical
        card.spacing = 8
        card.backgroundColor = .white
        card.layer.borderColor = UIColor(red: 210 / 255, green: 222 / 255, blue: 236 / 255, alpha: 1).cgColor
        card.layer.borderWidth = 1
        card.layer.cornerRadius = 12
        card.layoutMargins = UIEdgeInsets(top: 16, left: 16, bottom: 16, right: 16)
        card.isLayoutMarginsRelativeArrangement = true

        let badgeLabel = UILabel()
        badgeLabel.text = badge
        badgeLabel.textColor = .white
        badgeLabel.font = .systemFont(ofSize: 12, weight: .bold)
        badgeLabel.textAlignment = .center
        badgeLabel.backgroundColor = accentColor
        badgeLabel.layer.cornerRadius = 14
        badgeLabel.layer.masksToBounds = true
        badgeLabel.widthAnchor.constraint(greaterThanOrEqualToConstant: 72).isActive = true
        badgeLabel.heightAnchor.constraint(equalToConstant: 28).isActive = true
        card.addArrangedSubview(wrapLeading(badgeLabel))

        let titleLabel = UILabel()
        titleLabel.text = title
        titleLabel.textColor = accentColor
        titleLabel.font = .systemFont(ofSize: 22, weight: .bold)
        titleLabel.numberOfLines = 0
        card.addArrangedSubview(titleLabel)

        let subtitleLabel = UILabel()
        subtitleLabel.text = subtitle
        subtitleLabel.textColor = .label
        subtitleLabel.font = .preferredFont(forTextStyle: .body)
        subtitleLabel.numberOfLines = 0
        card.addArrangedSubview(subtitleLabel)

        for bullet in bullets {
            let bulletLabel = UILabel()
            bulletLabel.text = "✓  \(bullet)"
            bulletLabel.textColor = .label
            bulletLabel.font = .preferredFont(forTextStyle: .subheadline)
            bulletLabel.numberOfLines = 0
            card.addArrangedSubview(bulletLabel)
        }

        let actions = UIStackView()
        actions.axis = .horizontal
        actions.spacing = 10
        actions.distribution = .fillEqually
        actions.addArrangedSubview(button(primaryTitle, color: accentColor, action: primaryAction))
        actions.addArrangedSubview(button(secondaryTitle, color: UIColor(red: 89 / 255, green: 104 / 255, blue: 123 / 255, alpha: 1), action: secondaryAction))
        card.addArrangedSubview(actions)

        return card
    }

    private func wrapLeading(_ view: UIView) -> UIView {
        let wrapper = UIStackView(arrangedSubviews: [view, UIView()])
        wrapper.axis = .horizontal
        return wrapper
    }

    private func button(_ title: String, color: UIColor, action: Selector) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(title, for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .preferredFont(forTextStyle: .subheadline)
        button.titleLabel?.numberOfLines = 2
        button.titleLabel?.textAlignment = .center
        button.backgroundColor = color
        button.layer.cornerRadius = 18
        button.contentEdgeInsets = UIEdgeInsets(top: 9, left: 10, bottom: 9, right: 10)
        button.addTarget(self, action: action, for: .touchUpInside)
        return button
    }

    @objc private func playBackgroundAudio() {
        do {
            let request = try vastRequest()
            backgroundAudioHandle = CxBackgroundAudio.play(
                request: request,
                options: BackgroundAudioOptions(volume: 0.2),
                listener: self
            )
            updateStatus("Loading background audio...")
        } catch {
            updateStatus("Background audio request failed: \(error.localizedDescription)")
        }
    }

    @objc private func stopBackgroundAudio() {
        guard let backgroundAudioHandle else {
            updateStatus("No background audio handle is active.")
            return
        }
        backgroundAudioHandle.stop()
    }

    @objc private func showStickyPlayer() {
        do {
            stickyPlayerHandle = CxStickyPlayer.show(
                in: self,
                request: try vastRequest(),
                options: StickyPlayerOptions(position: .bottom, height: 96, showCloseButton: true),
                listener: self
            )
            updateStatus("Loading sticky player...")
        } catch {
            updateStatus("Sticky player request failed: \(error.localizedDescription)")
        }
    }

    @objc private func dismissStickyPlayer() {
        guard let stickyPlayerHandle else {
            updateStatus("No sticky player handle is active.")
            return
        }
        stickyPlayerHandle.dismiss()
    }

    @objc private func showFullscreenInterstitial() {
        do {
            fullscreenInterstitialHandle = CxFullscreenInterstitial.show(
                from: self,
                request: try vastRequest(),
                options: FullscreenInterstitialOptions(
                    showCloseButton: true,
                    companionHeight: 250,
                    companionMaxWidth: 300,
                    autoPlayVideo: true
                ),
                listener: self
            )
            updateStatus("Loading fullscreen interstitial...")
        } catch {
            updateStatus("Fullscreen interstitial request failed: \(error.localizedDescription)")
        }
    }

    @objc private func dismissFullscreenInterstitial() {
        guard let fullscreenInterstitialHandle else {
            updateStatus("No fullscreen interstitial handle is active.")
            return
        }
        fullscreenInterstitialHandle.dismiss()
    }

    private func vastRequest() throws -> VastRequest {
        try VastRequest(vastTagURLString: vastTagField.text.trimmingCharacters(in: .whitespacesAndNewlines))
    }

    private func updateStatus(_ status: String) {
        statusLabel.text = status
    }
}

extension SampleViewController: BackgroundAudioListener {
    func backgroundAudioDidStart(_ handle: BackgroundAudioHandle) {
        updateStatus("Background audio started.")
    }

    func backgroundAudioDidComplete() {
        backgroundAudioHandle = nil
        updateStatus("Background audio completed.")
    }

    func backgroundAudioDidStop() {
        backgroundAudioHandle = nil
        updateStatus("Background audio stopped.")
    }

    func backgroundAudioDidFail(_ error: Error) {
        backgroundAudioHandle = nil
        updateStatus("Background audio failed: \(error.localizedDescription)")
    }
}

extension SampleViewController: StickyPlayerListener {
    func stickyPlayerDidLoad(_ handle: StickyPlayerHandle) {
        updateStatus("Sticky player loaded.")
    }

    func stickyPlayerWasClicked(_ clickThroughURL: URL?) {
        updateStatus("Sticky player clicked: \(clickThroughURL?.absoluteString ?? "no click-through URL")")
    }

    func stickyPlayerDidDismiss() {
        stickyPlayerHandle = nil
        updateStatus("Sticky player dismissed.")
    }

    func stickyPlayerDidFail(_ error: Error) {
        stickyPlayerHandle = nil
        updateStatus("Sticky player failed: \(error.localizedDescription)")
    }
}

extension SampleViewController: FullscreenInterstitialListener {
    func fullscreenInterstitialDidLoad(_ handle: FullscreenInterstitialHandle) {
        updateStatus("Fullscreen interstitial loaded.")
    }

    func fullscreenInterstitialWasClicked(_ clickThroughURL: URL?) {
        updateStatus("Fullscreen interstitial clicked: \(clickThroughURL?.absoluteString ?? "no click-through URL")")
    }

    func fullscreenInterstitialDidDismiss() {
        fullscreenInterstitialHandle = nil
        updateStatus("Fullscreen interstitial dismissed.")
    }

    func fullscreenInterstitialDidFail(_ error: Error) {
        fullscreenInterstitialHandle = nil
        updateStatus("Fullscreen interstitial failed: \(error.localizedDescription)")
    }
}
