import UIKit

protocol FlappyGameViewDelegate: AnyObject {
    func flappyGameView(_ view: FlappyGameView, didUpdateScore score: Int)

    func flappyGameView(_ view: FlappyGameView, didChangeState state: FlappyGameView.State)
}

final class FlappyGameView: UIView {
    enum Difficulty {
        case easy
        case normal
        case hard

        fileprivate var scrollSpeed: CGFloat {
            switch self {
            case .easy:
                return 115
            case .normal:
                return 152
            case .hard:
                return 205
            }
        }
    }

    enum State {
        case ready
        case playing
        case gameOver
    }

    private struct Pipe {
        var x: CGFloat
        var gapY: CGFloat
        var scored: Bool
    }

    weak var delegate: FlappyGameViewDelegate?

    var difficulty: Difficulty = .normal

    private(set) var state: State = .ready {
        didSet {
            if oldValue != state {
                delegate?.flappyGameView(self, didChangeState: state)
            }
        }
    }

    private let birdRadius: CGFloat = 20
    private let pipeWidth: CGFloat = 72
    private let pipeGap: CGFloat = 170
    private let gravity: CGFloat = 1_050
    private let flapVelocity: CGFloat = -345
    private let groundHeight: CGFloat = 74
    private let blobfishFrownFinsDownImage = UIImage(named: "blobfish")
    private let blobfishFrownFinsUpImage = UIImage(named: "blobfish-frown-fins-up")
    private let blobfishSmileFinsDownImage = UIImage(named: "blobfish-smile-fins-down")
    private let blobfishSmileFinsUpImage = UIImage(named: "blobfish-smile-fins-up")

    private var displayLink: CADisplayLink?
    private var lastTimestamp: CFTimeInterval = 0
    private var birdX: CGFloat = 88
    private var birdY: CGFloat = 180
    private var velocity: CGFloat = 0
    private var swimAnimationTime: CGFloat = 0
    private var pipes: [Pipe] = []
    private var score = 0
    private var lastKnownSize = CGSize.zero

    override init(frame: CGRect) {
        super.init(frame: frame)
        isOpaque = true
        contentMode = .redraw
        addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(handleTap)))
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    deinit {
        stopLoop()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        if bounds.size != lastKnownSize {
            lastKnownSize = bounds.size
            if state == .playing {
                birdX = max(76, bounds.width * 0.28)
            } else {
                reset()
            }
        }
    }

    func reset() {
        stopLoop()
        score = 0
        velocity = 0
        swimAnimationTime = 0
        birdX = max(76, bounds.width * 0.28)
        birdY = max(120, bounds.height * 0.42)
        pipes = []
        state = .ready
        delegate?.flappyGameView(self, didUpdateScore: score)
        setNeedsDisplay()
    }

    func flapOrStart() {
        switch state {
        case .ready:
            startGame()
            flap()
        case .playing:
            flap()
        case .gameOver:
            reset()
        }
    }

    private func startGame() {
        guard state != .playing else {
            return
        }
        score = 0
        pipes = []
        spawnPipe(at: bounds.width + 40)
        state = .playing
        delegate?.flappyGameView(self, didUpdateScore: score)
        startLoop()
    }

    private func flap() {
        velocity = flapVelocity
        swimAnimationTime = 0
        setNeedsDisplay()
    }

    @objc private func handleTap() {
        flapOrStart()
    }

    private func startLoop() {
        stopLoop()
        lastTimestamp = 0
        let link = CADisplayLink(target: self, selector: #selector(tick(_:)))
        link.add(to: .main, forMode: .common)
        displayLink = link
    }

    private func stopLoop() {
        displayLink?.invalidate()
        displayLink = nil
        lastTimestamp = 0
    }

    @objc private func tick(_ link: CADisplayLink) {
        guard state == .playing else {
            return
        }

        if lastTimestamp == 0 {
            lastTimestamp = link.timestamp
            return
        }

        let delta = min(CGFloat(link.timestamp - lastTimestamp), 0.035)
        lastTimestamp = link.timestamp
        step(delta: delta)
        setNeedsDisplay()
    }

    private func step(delta: CGFloat) {
        swimAnimationTime += delta
        velocity += gravity * delta
        birdY += velocity * delta

        for index in pipes.indices {
            pipes[index].x -= difficulty.scrollSpeed * delta
        }
        pipes.removeAll { $0.x + pipeWidth < -20 }

        if pipes.isEmpty || (bounds.width - (pipes.last?.x ?? 0)) > pipeSpacing {
            spawnPipe(at: bounds.width + 24)
        }

        for index in pipes.indices where !pipes[index].scored && pipes[index].x + pipeWidth < birdX {
            pipes[index].scored = true
            score += 1
            delegate?.flappyGameView(self, didUpdateScore: score)
        }

        if collidesWithWorld() {
            endGame()
        }
    }

    private var pipeSpacing: CGFloat {
        max(190, bounds.width * 0.58)
    }

    private var groundTop: CGFloat {
        max(0, bounds.height - groundHeight)
    }

    private func spawnPipe(at x: CGFloat) {
        guard bounds.height > groundHeight + pipeGap + 80 else {
            return
        }

        let minGapY = CGFloat(94) + pipeGap / 2
        let maxGapY = groundTop - pipeGap / 2 - 38
        let gapY = CGFloat.random(in: minGapY...max(minGapY, maxGapY))
        pipes.append(Pipe(x: x, gapY: gapY, scored: false))
    }

    private func collidesWithWorld() -> Bool {
        let birdFrame = CGRect(
            x: birdX - birdRadius,
            y: birdY - birdRadius,
            width: birdRadius * 2,
            height: birdRadius * 2
        )

        if birdFrame.minY <= 0 || birdFrame.maxY >= groundTop {
            return true
        }

        for pipe in pipes {
            let topPipe = CGRect(
                x: pipe.x,
                y: 0,
                width: pipeWidth,
                height: pipe.gapY - pipeGap / 2
            )
            let bottomPipe = CGRect(
                x: pipe.x,
                y: pipe.gapY + pipeGap / 2,
                width: pipeWidth,
                height: groundTop - (pipe.gapY + pipeGap / 2)
            )
            if birdFrame.intersects(topPipe) || birdFrame.intersects(bottomPipe) {
                return true
            }
        }

        return false
    }

    private func endGame() {
        stopLoop()
        state = .gameOver
        setNeedsDisplay()
    }

    override func draw(_ rect: CGRect) {
        guard let context = UIGraphicsGetCurrentContext() else {
            return
        }

        drawWater(in: rect, context: context)
        drawLightRays(in: rect, context: context)
        drawBubbles(in: rect)
        drawPipes()
        drawGround()
        drawBird()
        drawOverlayIfNeeded()
    }

    private func drawWater(in rect: CGRect, context: CGContext) {
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let colors = [
            UIColor(red: 14 / 255, green: 165 / 255, blue: 233 / 255, alpha: 1).cgColor,
            UIColor(red: 3 / 255, green: 105 / 255, blue: 161 / 255, alpha: 1).cgColor,
            UIColor(red: 8 / 255, green: 47 / 255, blue: 73 / 255, alpha: 1).cgColor
        ] as CFArray
        let locations: [CGFloat] = [0, 0.48, 1]
        let gradient = CGGradient(colorsSpace: colorSpace, colors: colors, locations: locations)!
        context.drawLinearGradient(
            gradient,
            start: CGPoint(x: rect.midX, y: rect.minY),
            end: CGPoint(x: rect.midX, y: rect.maxY),
            options: []
        )
    }

    private func drawLightRays(in rect: CGRect, context: CGContext) {
        context.saveGState()
        context.setBlendMode(.screen)
        let rayColor = UIColor(red: 186 / 255, green: 230 / 255, blue: 253 / 255, alpha: 0.12)
        rayColor.setFill()
        for index in 0..<5 {
            let startX = rect.width * (0.05 + CGFloat(index) * 0.23)
            let ray = UIBezierPath()
            ray.move(to: CGPoint(x: startX, y: 0))
            ray.addLine(to: CGPoint(x: startX + 34, y: 0))
            ray.addLine(to: CGPoint(x: startX + 116, y: rect.height * 0.72))
            ray.addLine(to: CGPoint(x: startX + 58, y: rect.height * 0.72))
            ray.close()
            ray.fill()
        }
        context.restoreGState()
    }

    private func drawBubbles(in rect: CGRect) {
        let bubbles: [(CGFloat, CGFloat, CGFloat)] = [
            (0.12, 0.18, 7), (0.18, 0.28, 3), (0.76, 0.12, 5),
            (0.83, 0.33, 8), (0.67, 0.48, 3), (0.36, 0.57, 5),
            (0.08, 0.63, 4), (0.92, 0.68, 6)
        ]
        UIColor.white.withAlphaComponent(0.32).setStroke()
        for bubble in bubbles {
            let bubbleRect = CGRect(
                x: rect.width * bubble.0 - bubble.2,
                y: rect.height * bubble.1 - bubble.2,
                width: bubble.2 * 2,
                height: bubble.2 * 2
            )
            let path = UIBezierPath(ovalIn: bubbleRect)
            path.lineWidth = 1.5
            path.stroke()
        }
    }

    private func drawPipes() {
        for pipe in pipes {
            drawReefColumn(
                rect: CGRect(x: pipe.x, y: -8, width: pipeWidth, height: pipe.gapY - pipeGap / 2 + 8),
                opensDownward: true
            )
            drawReefColumn(rect: CGRect(
                x: pipe.x,
                y: pipe.gapY + pipeGap / 2,
                width: pipeWidth,
                height: groundTop - (pipe.gapY + pipeGap / 2)
            ), opensDownward: false)
        }
    }

    private func drawReefColumn(rect: CGRect, opensDownward: Bool) {
        let rock = UIBezierPath(roundedRect: rect, cornerRadius: 18)
        UIColor(red: 30 / 255, green: 64 / 255, blue: 93 / 255, alpha: 1).setFill()
        rock.fill()

        let highlightRect = CGRect(x: rect.minX + 9, y: rect.minY, width: 14, height: rect.height)
        UIColor(red: 51 / 255, green: 95 / 255, blue: 122 / 255, alpha: 0.72).setFill()
        UIBezierPath(roundedRect: highlightRect, cornerRadius: 7).fill()

        let ledgeHeight: CGFloat = 22
        let ledgeY = opensDownward ? rect.maxY - ledgeHeight : rect.minY
        let ledge = CGRect(x: rect.minX - 8, y: ledgeY, width: rect.width + 16, height: ledgeHeight)
        UIColor(red: 15 / 255, green: 118 / 255, blue: 110 / 255, alpha: 1).setFill()
        UIBezierPath(roundedRect: ledge, cornerRadius: 11).fill()

        let coralY = opensDownward ? ledge.minY - 2 : ledge.maxY - 5
        drawCoralCluster(center: CGPoint(x: rect.midX, y: coralY), inverted: opensDownward)
    }

    private func drawCoralCluster(center: CGPoint, inverted: Bool) {
        let direction: CGFloat = inverted ? -1 : 1
        let coral = UIColor(red: 251 / 255, green: 113 / 255, blue: 133 / 255, alpha: 1)
        coral.setStroke()
        for offset in [-20, -7, 8, 21] as [CGFloat] {
            let stem = UIBezierPath()
            stem.move(to: CGPoint(x: center.x + offset, y: center.y))
            stem.addCurve(
                to: CGPoint(x: center.x + offset * 0.72, y: center.y + direction * (18 + abs(offset) * 0.25)),
                controlPoint1: CGPoint(x: center.x + offset + 5, y: center.y + direction * 7),
                controlPoint2: CGPoint(x: center.x + offset - 4, y: center.y + direction * 13)
            )
            stem.lineWidth = 5
            stem.lineCapStyle = .round
            stem.stroke()
        }
    }

    private func drawGround() {
        let seabed = CGRect(x: 0, y: groundTop, width: bounds.width, height: groundHeight)
        UIColor(red: 253 / 255, green: 230 / 255, blue: 138 / 255, alpha: 1).setFill()
        UIBezierPath(rect: seabed).fill()

        UIColor(red: 251 / 255, green: 191 / 255, blue: 36 / 255, alpha: 0.28).setFill()
        for x in stride(from: CGFloat(12), through: bounds.width, by: 37) {
            UIBezierPath(ovalIn: CGRect(x: x, y: groundTop + 18 + (x.truncatingRemainder(dividingBy: 22)), width: 5, height: 3)).fill()
        }

        drawKelp(at: 28, height: 54)
        drawKelp(at: bounds.width - 48, height: 62)
        drawKelp(at: bounds.width * 0.62, height: 42)

        UIColor(red: 244 / 255, green: 114 / 255, blue: 182 / 255, alpha: 1).setStroke()
        let shell = UIBezierPath(arcCenter: CGPoint(x: bounds.width * 0.34, y: groundTop + 38), radius: 10, startAngle: .pi, endAngle: 0, clockwise: true)
        shell.lineWidth = 4
        shell.stroke()
    }

    private func drawKelp(at x: CGFloat, height: CGFloat) {
        UIColor(red: 16 / 255, green: 185 / 255, blue: 129 / 255, alpha: 0.88).setStroke()
        let kelp = UIBezierPath()
        kelp.move(to: CGPoint(x: x, y: bounds.height))
        kelp.addCurve(
            to: CGPoint(x: x + 4, y: bounds.height - height),
            controlPoint1: CGPoint(x: x - 14, y: bounds.height - height * 0.34),
            controlPoint2: CGPoint(x: x + 18, y: bounds.height - height * 0.7)
        )
        kelp.lineWidth = 8
        kelp.lineCapStyle = .round
        kelp.stroke()
    }

    private func drawBird() {
        let fishRect = CGRect(x: birdX - 31, y: birdY - 25, width: 62, height: 50)
        if let blobfishImage = currentBlobfishImage {
            blobfishImage.draw(in: fishRect, blendMode: .normal, alpha: 1)
        } else {
            UIColor(red: 251 / 255, green: 113 / 255, blue: 133 / 255, alpha: 1).setFill()
            UIBezierPath(ovalIn: fishRect).fill()
        }

        UIColor.white.withAlphaComponent(0.55).setStroke()
        for offset in 0..<3 {
            let radius = CGFloat(3 + offset * 2)
            let bubble = UIBezierPath(ovalIn: CGRect(
                x: fishRect.minX - CGFloat(offset * 8) - radius,
                y: fishRect.midY - CGFloat(offset * 9),
                width: radius * 2,
                height: radius * 2
            ))
            bubble.lineWidth = 1
            bubble.stroke()
        }
    }

    private var currentBlobfishImage: UIImage? {
        guard state == .playing else {
            return blobfishFrownFinsDownImage
        }

        let finsAreUp = Int(swimAnimationTime / 0.14).isMultiple(of: 2)
        if velocity < 0 {
            return finsAreUp ? blobfishSmileFinsUpImage : blobfishSmileFinsDownImage
        }
        return finsAreUp ? blobfishFrownFinsUpImage : blobfishFrownFinsDownImage
    }

    private func drawOverlayIfNeeded() {
        switch state {
        case .playing:
            return
        case .ready:
            drawCentered(text: "Flappy Blobfish", y: bounds.height * 0.31, fontSize: 32, color: .white)
            drawCentered(text: "Tap to swim", y: bounds.height * 0.40, fontSize: 18, color: UIColor.white.withAlphaComponent(0.86))
        case .gameOver:
            drawCentered(text: "Dive Over", y: bounds.height * 0.33, fontSize: 32, color: .white)
            drawCentered(text: "\(score)", y: bounds.height * 0.42, fontSize: 42, color: UIColor.white.withAlphaComponent(0.92))
        }
    }

    private func drawCentered(text: String, y: CGFloat, fontSize: CGFloat, color: UIColor) {
        let paragraph = NSMutableParagraphStyle()
        paragraph.alignment = .center
        let attributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: fontSize, weight: .black),
            .foregroundColor: color,
            .paragraphStyle: paragraph,
            .strokeColor: UIColor(red: 15 / 255, green: 23 / 255, blue: 42 / 255, alpha: 0.18),
            .strokeWidth: -2
        ]
        let height = fontSize * 1.35
        text.draw(in: CGRect(x: 16, y: y, width: bounds.width - 32, height: height), withAttributes: attributes)
    }
}
