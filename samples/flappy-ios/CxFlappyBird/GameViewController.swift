import UIKit

final class GameViewController: UIViewController {
    private let gameView = FlappyGameView()
    private let scoreLabel = UILabel()
    private let bestLabel = UILabel()
    private let adStatusLabel = UILabel()
    private let difficultyControl = UISegmentedControl(items: ["Easy", "Normal", "Hard"])
    private let tapLiftSlider = UISlider()
    private let tapLiftValueLabel = UILabel()
    private lazy var gameAds = GameAdController { [weak self] status in
        self?.setAdStatus(status)
    }

    private let sessionStartedAt = Date()
    private let readyStickyDelay: TimeInterval = 8
    private let minimumInterstitialInterval: TimeInterval = 90
    private let longSessionInterstitialInterval: TimeInterval = 180
    private var score = 0
    private var bestScore = 0
    private var isReplayLocked = false
    private var completedRounds = 0
    private var roundsSinceInterstitial = 0
    private var earnedNewHighScoreThisRound = false
    private var hasRequestedReadySticky = false
    private var lastInterstitialRequestAt: Date?
    private var readyStickyWorkItem: DispatchWorkItem?

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red: 2 / 255, green: 32 / 255, blue: 58 / 255, alpha: 1)
        buildInterface()
        gameView.delegate = self
        gameView.reset()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        if gameView.state == .ready {
            scheduleReadySticky()
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        cancelReadySticky()
        gameAds.stop()
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
        titleLabel.text = "Flappy Blob"
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

        adStatusLabel.text = "Ad idle"
        adStatusLabel.textColor = UIColor(red: 203 / 255, green: 213 / 255, blue: 225 / 255, alpha: 1)
        adStatusLabel.font = .systemFont(ofSize: 13, weight: .semibold)
        adStatusLabel.textAlignment = .center
        adStatusLabel.numberOfLines = 1
        adStatusLabel.adjustsFontSizeToFitWidth = true
        adStatusLabel.minimumScaleFactor = 0.75

        let difficultyRow = UIStackView()
        difficultyRow.axis = .horizontal
        difficultyRow.alignment = .center
        difficultyRow.spacing = 12

        let difficultyLabel = UILabel()
        difficultyLabel.text = "Difficulty"
        difficultyLabel.textColor = UIColor(red: 203 / 255, green: 213 / 255, blue: 225 / 255, alpha: 1)
        difficultyLabel.font = .systemFont(ofSize: 13, weight: .bold)

        difficultyControl.selectedSegmentIndex = 1
        difficultyControl.selectedSegmentTintColor = UIColor(red: 14 / 255, green: 165 / 255, blue: 233 / 255, alpha: 1)
        difficultyControl.setTitleTextAttributes([.foregroundColor: UIColor.white], for: .selected)
        difficultyControl.setTitleTextAttributes(
            [.foregroundColor: UIColor(red: 148 / 255, green: 163 / 255, blue: 184 / 255, alpha: 1)],
            for: .normal
        )
        difficultyControl.addTarget(self, action: #selector(difficultyChanged), for: .valueChanged)

        difficultyRow.addArrangedSubview(difficultyLabel)
        difficultyRow.addArrangedSubview(difficultyControl)
        difficultyControl.setContentHuggingPriority(.defaultLow, for: .horizontal)

        let tapLiftRow = UIStackView()
        tapLiftRow.axis = .horizontal
        tapLiftRow.alignment = .center
        tapLiftRow.spacing = 10

        let tapLiftLabel = UILabel()
        tapLiftLabel.text = "Tap lift"
        tapLiftLabel.textColor = difficultyLabel.textColor
        tapLiftLabel.font = difficultyLabel.font

        tapLiftSlider.minimumValue = 240
        tapLiftSlider.maximumValue = 480
        tapLiftSlider.value = 345
        tapLiftSlider.minimumTrackTintColor = UIColor(red: 14 / 255, green: 165 / 255, blue: 233 / 255, alpha: 1)
        tapLiftSlider.maximumTrackTintColor = UIColor(red: 30 / 255, green: 64 / 255, blue: 93 / 255, alpha: 1)
        tapLiftSlider.addTarget(self, action: #selector(tapLiftChanged), for: .valueChanged)

        tapLiftValueLabel.text = "345"
        tapLiftValueLabel.textColor = .white
        tapLiftValueLabel.font = .monospacedDigitSystemFont(ofSize: 13, weight: .bold)
        tapLiftValueLabel.textAlignment = .right
        tapLiftValueLabel.widthAnchor.constraint(equalToConstant: 34).isActive = true

        tapLiftRow.addArrangedSubview(tapLiftLabel)
        tapLiftRow.addArrangedSubview(tapLiftSlider)
        tapLiftRow.addArrangedSubview(tapLiftValueLabel)

        bottomBar.addArrangedSubview(difficultyRow)
        bottomBar.addArrangedSubview(tapLiftRow)
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

            difficultyControl.heightAnchor.constraint(equalToConstant: 32)
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

    @objc private func difficultyChanged() {
        switch difficultyControl.selectedSegmentIndex {
        case 0:
            gameView.difficulty = .easy
        case 2:
            gameView.difficulty = .hard
        default:
            gameView.difficulty = .normal
        }
    }

    @objc private func tapLiftChanged() {
        let lift = Int(tapLiftSlider.value.rounded())
        gameView.tapLift = CGFloat(lift)
        tapLiftValueLabel.text = "\(lift)"
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

    private func showPostGameInterstitial() {
        guard !isReplayLocked else {
            return
        }

        isReplayLocked = true
        gameView.isReplayEnabled = false

        gameAds.showInterstitial(from: self) { [weak self] in
            DispatchQueue.main.async {
                self?.unlockReplay()
            }
        }
    }

    private func unlockReplay() {
        guard isReplayLocked else {
            return
        }

        isReplayLocked = false
        gameView.isReplayEnabled = true
    }

    private func scheduleReadySticky() {
        cancelReadySticky()
        guard !hasRequestedReadySticky else {
            return
        }
        let workItem = DispatchWorkItem { [weak self] in
            guard let self, self.gameView.state == .ready else {
                return
            }
            self.hasRequestedReadySticky = true
            self.gameAds.showReadySticky(in: self)
        }
        readyStickyWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + readyStickyDelay, execute: workItem)
    }

    private func cancelReadySticky() {
        readyStickyWorkItem?.cancel()
        readyStickyWorkItem = nil
    }

    private func shouldShowInterstitial(at now: Date) -> Bool {
        guard completedRounds >= 2 else {
            return false
        }

        if let lastInterstitialRequestAt,
           now.timeIntervalSince(lastInterstitialRequestAt) < minimumInterstitialInterval {
            return false
        }

        let intervalReference = lastInterstitialRequestAt ?? sessionStartedAt
        let longSessionIsDue = now.timeIntervalSince(intervalReference) >= longSessionInterstitialInterval
        return earnedNewHighScoreThisRound || roundsSinceInterstitial >= 3 || longSessionIsDue
    }

    private func handleGameOver() {
        completedRounds += 1
        roundsSinceInterstitial += 1
        let now = Date()

        guard shouldShowInterstitial(at: now) else {
            return
        }

        lastInterstitialRequestAt = now
        roundsSinceInterstitial = 0
        showPostGameInterstitial()
    }
}

extension GameViewController: FlappyGameViewDelegate {
    func flappyGameView(_ view: FlappyGameView, didUpdateScore score: Int) {
        self.score = score
        if score > bestScore {
            earnedNewHighScoreThisRound = true
        }
        bestScore = max(bestScore, score)
        updateScoreLabels()
    }

    func flappyGameView(_ view: FlappyGameView, didChangeState state: FlappyGameView.State) {
        switch state {
        case .ready:
            scheduleReadySticky()
        case .playing:
            cancelReadySticky()
            gameAds.dismissReadySticky()
            earnedNewHighScoreThisRound = false
        case .gameOver:
            cancelReadySticky()
            gameAds.dismissReadySticky()
            bestScore = max(bestScore, score)
            updateScoreLabels()
            handleGameOver()
        }
    }
}
