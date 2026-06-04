import CxAudioSDK
import UIKit

final class SampleViewController: UIViewController {
    private let defaultVastTagURL = "https://ads.example.com/vast.xml"
    private let vastTagField = UITextView()
    private let statusLabel = UILabel()

    private var backgroundAudioHandle: BackgroundAudioHandle?
    private var stickyPlayerHandle: StickyPlayerHandle?
    private var fullscreenInterstitialHandle: FullscreenInterstitialHandle?

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "CX Audio Sample"
        view.backgroundColor = .systemBackground
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

        let titleLabel = UILabel()
        titleLabel.text = "CX Audio SDK Sample"
        titleLabel.font = .preferredFont(forTextStyle: .title1)
        titleLabel.textColor = .label
        content.addArrangedSubview(titleLabel)

        let fieldLabel = UILabel()
        fieldLabel.text = "VAST tag URL"
        fieldLabel.font = .preferredFont(forTextStyle: .subheadline)
        fieldLabel.textColor = .secondaryLabel
        content.addArrangedSubview(fieldLabel)

        vastTagField.text = defaultVastTagURL
        vastTagField.font = .preferredFont(forTextStyle: .body)
        vastTagField.autocapitalizationType = .none
        vastTagField.autocorrectionType = .no
        vastTagField.keyboardType = .URL
        vastTagField.layer.borderColor = UIColor.separator.cgColor
        vastTagField.layer.borderWidth = 1
        vastTagField.layer.cornerRadius = 6
        vastTagField.textContainerInset = UIEdgeInsets(top: 10, left: 8, bottom: 10, right: 8)
        content.addArrangedSubview(vastTagField)
        vastTagField.heightAnchor.constraint(greaterThanOrEqualToConstant: 76).isActive = true

        content.addArrangedSubview(button("Play Background Audio", action: #selector(playBackgroundAudio)))
        content.addArrangedSubview(button("Stop Background Audio", action: #selector(stopBackgroundAudio)))
        content.addArrangedSubview(button("Show Sticky Player", action: #selector(showStickyPlayer)))
        content.addArrangedSubview(button("Dismiss Sticky Player", action: #selector(dismissStickyPlayer)))
        content.addArrangedSubview(button("Show Fullscreen Interstitial", action: #selector(showFullscreenInterstitial)))
        content.addArrangedSubview(button("Dismiss Fullscreen Interstitial", action: #selector(dismissFullscreenInterstitial)))

        statusLabel.numberOfLines = 0
        statusLabel.font = .preferredFont(forTextStyle: .body)
        statusLabel.textColor = .label
        content.addArrangedSubview(statusLabel)
    }

    private func button(_ title: String, action: Selector) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(title, for: .normal)
        button.titleLabel?.font = .preferredFont(forTextStyle: .headline)
        button.contentHorizontalAlignment = .leading
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
