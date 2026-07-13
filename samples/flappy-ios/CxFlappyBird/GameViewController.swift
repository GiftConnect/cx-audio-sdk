import UIKit

final class GameViewController: UIViewController {
    private let gameView = FlappyGameView()
    private let scoreLabel = UILabel()
    private let bestLabel = UILabel()
    private let adStatusLabel = UILabel()
    private let primaryButton = UIButton(type: .system)
    private let adButton = UIButton(type: .system)
    private lazy var audioAds = MinimalAudioAdController { [weak self] status in
        self?.setAdStatus(status)
    }

    private var score = 0
    private var bestScore = 0

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red: 17 / 255, green: 24 / 255, blue: 39 / 255, alpha: 1)
        buildInterface()
        gameView.delegate = self
        gameView.reset()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        audioAds.stop()
    }

    private func buildInterface() {
        let topBar = UIStackView()
        topBar.axis = .horizontal
        topBar.alignment = .center
        topBar.distribution = .equalSpacing
        topBar.layoutMargins = UIEdgeInsets(top: 14, left: 18, bottom: 10, right: 18)
        topBar.isLayoutMarginsRelativeArrangement = true
        topBar.translatesAutoresizingMaskIntoConstraints = false

        configureMetricLabel(scoreLabel, title: "Score", value: "0")
        configureMetricLabel(bestLabel, title: "Best", value: "0")

        let titleLabel = UILabel()
        titleLabel.text = "Flappy CX"
        titleLabel.textColor = .white
        titleLabel.font = .systemFont(ofSize: 24, weight: .black)
        titleLabel.textAlignment = .center
        titleLabel.adjustsFontSizeToFitWidth = true

        topBar.addArrangedSubview(scoreLabel)
        topBar.addArrangedSubview(titleLabel)
        topBar.addArrangedSubview(bestLabel)

        gameView.translatesAutoresizingMaskIntoConstraints = false

        let bottomBar = UIStackView()
        bottomBar.axis = .vertical
        bottomBar.spacing = 10
        bottomBar.layoutMargins = UIEdgeInsets(top: 12, left: 18, bottom: 18, right: 18)
        bottomBar.isLayoutMarginsRelativeArrangement = true
        bottomBar.translatesAutoresizingMaskIntoConstraints = false

        let buttonRow = UIStackView()
        buttonRow.axis = .horizontal
        buttonRow.spacing = 10
        buttonRow.distribution = .fillEqually

        configureButton(primaryButton, title: "Start", color: UIColor(red: 249 / 255, green: 115 / 255, blue: 22 / 255, alpha: 1))
        primaryButton.addTarget(self, action: #selector(primaryButtonTapped), for: .touchUpInside)

        configureButton(adButton, title: "Audio Ad", color: UIColor(red: 20 / 255, green: 184 / 255, blue: 166 / 255, alpha: 1))
        adButton.addTarget(self, action: #selector(adButtonTapped), for: .touchUpInside)

        buttonRow.addArrangedSubview(primaryButton)
        buttonRow.addArrangedSubview(adButton)

        adStatusLabel.text = "Ad idle"
        adStatusLabel.textColor = UIColor(red: 203 / 255, green: 213 / 255, blue: 225 / 255, alpha: 1)
        adStatusLabel.font = .systemFont(ofSize: 13, weight: .semibold)
        adStatusLabel.textAlignment = .center
        adStatusLabel.numberOfLines = 1
        adStatusLabel.adjustsFontSizeToFitWidth = true
        adStatusLabel.minimumScaleFactor = 0.75

        bottomBar.addArrangedSubview(buttonRow)
        bottomBar.addArrangedSubview(adStatusLabel)

        view.addSubview(topBar)
        view.addSubview(gameView)
        view.addSubview(bottomBar)

        NSLayoutConstraint.activate([
            topBar.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor),
            topBar.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor),
            topBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),

            gameView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            gameView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            gameView.topAnchor.constraint(equalTo: topBar.bottomAnchor),
            gameView.bottomAnchor.constraint(equalTo: bottomBar.topAnchor),

            bottomBar.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor),
            bottomBar.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor),
            bottomBar.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),

            primaryButton.heightAnchor.constraint(equalToConstant: 48),
            adButton.heightAnchor.constraint(equalToConstant: 48)
        ])
    }

    private func configureMetricLabel(_ label: UILabel, title: String, value: String) {
        label.text = "\(title)\n\(value)"
        label.textColor = UIColor(red: 226 / 255, green: 232 / 255, blue: 240 / 255, alpha: 1)
        label.font = .systemFont(ofSize: 14, weight: .bold)
        label.textAlignment = .center
        label.numberOfLines = 2
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = 0.75
        label.widthAnchor.constraint(equalToConstant: 74).isActive = true
    }

    private func configureButton(_ button: UIButton, title: String, color: UIColor) {
        var configuration = UIButton.Configuration.filled()
        configuration.title = title
        configuration.baseBackgroundColor = color
        configuration.baseForegroundColor = .white
        configuration.cornerStyle = .medium
        configuration.contentInsets = NSDirectionalEdgeInsets(top: 8, leading: 10, bottom: 8, trailing: 10)
        button.configuration = configuration
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .bold)
    }

    @objc private func primaryButtonTapped() {
        gameView.flapOrStart()
    }

    @objc private func adButtonTapped() {
        audioAds.playPostGameAudioAd()
    }

    private func updateButtons(for state: FlappyGameView.State) {
        let title: String
        switch state {
        case .ready:
            title = "Start"
        case .playing:
            title = "Flap"
        case .gameOver:
            title = "Restart"
        }
        primaryButton.configuration?.title = title
    }

    private func updateScoreLabels() {
        scoreLabel.text = "Score\n\(score)"
        bestLabel.text = "Best\n\(bestScore)"
    }

    private func setAdStatus(_ status: String) {
        DispatchQueue.main.async { [weak self] in
            self?.adStatusLabel.text = status
        }
    }
}

extension GameViewController: FlappyGameViewDelegate {
    func flappyGameView(_ view: FlappyGameView, didUpdateScore score: Int) {
        self.score = score
        bestScore = max(bestScore, score)
        updateScoreLabels()
    }

    func flappyGameView(_ view: FlappyGameView, didChangeState state: FlappyGameView.State) {
        updateButtons(for: state)
        if state == .gameOver {
            bestScore = max(bestScore, score)
            updateScoreLabels()
            audioAds.playPostGameAudioAd()
        }
    }
}
