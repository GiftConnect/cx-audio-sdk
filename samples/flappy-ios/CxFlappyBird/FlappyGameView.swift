import UIKit

protocol FlappyGameViewDelegate: AnyObject {
    func flappyGameView(_ view: FlappyGameView, didUpdateScore score: Int)

    func flappyGameView(_ view: FlappyGameView, didChangeState state: FlappyGameView.State)
}

final class FlappyGameView: UIView {
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

    private(set) var state: State = .ready {
        didSet {
            if oldValue != state {
                delegate?.flappyGameView(self, didChangeState: state)
            }
        }
    }

    private let birdRadius: CGFloat = 16
    private let pipeWidth: CGFloat = 68
    private let pipeGap: CGFloat = 158
    private let gravity: CGFloat = 1_550
    private let flapVelocity: CGFloat = -455
    private let scrollSpeed: CGFloat = 174
    private let groundHeight: CGFloat = 68

    private var displayLink: CADisplayLink?
    private var lastTimestamp: CFTimeInterval = 0
    private var birdX: CGFloat = 88
    private var birdY: CGFloat = 180
    private var velocity: CGFloat = 0
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
        velocity += gravity * delta
        birdY += velocity * delta

        for index in pipes.indices {
            pipes[index].x -= scrollSpeed * delta
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

        drawSky(in: rect, context: context)
        drawClouds(in: rect)
        drawPipes()
        drawGround()
        drawBird()
        drawOverlayIfNeeded()
    }

    private func drawSky(in rect: CGRect, context: CGContext) {
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let colors = [
            UIColor(red: 56 / 255, green: 189 / 255, blue: 248 / 255, alpha: 1).cgColor,
            UIColor(red: 125 / 255, green: 211 / 255, blue: 252 / 255, alpha: 1).cgColor,
            UIColor(red: 240 / 255, green: 249 / 255, blue: 255 / 255, alpha: 1).cgColor
        ] as CFArray
        let locations: [CGFloat] = [0, 0.58, 1]
        let gradient = CGGradient(colorsSpace: colorSpace, colors: colors, locations: locations)!
        context.drawLinearGradient(
            gradient,
            start: CGPoint(x: rect.midX, y: rect.minY),
            end: CGPoint(x: rect.midX, y: rect.maxY),
            options: []
        )
    }

    private func drawClouds(in rect: CGRect) {
        UIColor.white.withAlphaComponent(0.78).setFill()
        drawCloud(center: CGPoint(x: rect.width * 0.22, y: rect.height * 0.16), scale: 1.0)
        drawCloud(center: CGPoint(x: rect.width * 0.74, y: rect.height * 0.25), scale: 0.72)
        drawCloud(center: CGPoint(x: rect.width * 0.50, y: rect.height * 0.09), scale: 0.58)
    }

    private func drawCloud(center: CGPoint, scale: CGFloat) {
        let path = UIBezierPath(roundedRect: CGRect(
            x: center.x - 48 * scale,
            y: center.y - 10 * scale,
            width: 96 * scale,
            height: 24 * scale
        ), cornerRadius: 12 * scale)
        path.append(UIBezierPath(ovalIn: CGRect(
            x: center.x - 35 * scale,
            y: center.y - 28 * scale,
            width: 42 * scale,
            height: 42 * scale
        )))
        path.append(UIBezierPath(ovalIn: CGRect(
            x: center.x - 4 * scale,
            y: center.y - 36 * scale,
            width: 52 * scale,
            height: 52 * scale
        )))
        path.fill()
    }

    private func drawPipes() {
        for pipe in pipes {
            drawPipe(rect: CGRect(x: pipe.x, y: -6, width: pipeWidth, height: pipe.gapY - pipeGap / 2 + 6), capAtBottom: true)
            drawPipe(rect: CGRect(
                x: pipe.x,
                y: pipe.gapY + pipeGap / 2,
                width: pipeWidth,
                height: groundTop - (pipe.gapY + pipeGap / 2)
            ), capAtBottom: false)
        }
    }

    private func drawPipe(rect: CGRect, capAtBottom: Bool) {
        let pipeColor = UIColor(red: 16 / 255, green: 185 / 255, blue: 129 / 255, alpha: 1)
        let shadowColor = UIColor(red: 5 / 255, green: 150 / 255, blue: 105 / 255, alpha: 1)

        pipeColor.setFill()
        UIBezierPath(rect: rect).fill()

        shadowColor.setFill()
        UIBezierPath(rect: CGRect(x: rect.maxX - 13, y: rect.minY, width: 13, height: rect.height)).fill()

        let capHeight: CGFloat = 20
        let capY = capAtBottom ? rect.maxY - capHeight : rect.minY
        let capRect = CGRect(x: rect.minX - 8, y: capY, width: rect.width + 16, height: capHeight)
        UIColor(red: 34 / 255, green: 197 / 255, blue: 94 / 255, alpha: 1).setFill()
        UIBezierPath(roundedRect: capRect, cornerRadius: 4).fill()
    }

    private func drawGround() {
        let dirt = CGRect(x: 0, y: groundTop, width: bounds.width, height: groundHeight)
        UIColor(red: 132 / 255, green: 204 / 255, blue: 22 / 255, alpha: 1).setFill()
        UIBezierPath(rect: CGRect(x: 0, y: groundTop, width: bounds.width, height: 12)).fill()

        UIColor(red: 180 / 255, green: 83 / 255, blue: 9 / 255, alpha: 1).setFill()
        UIBezierPath(rect: dirt.offsetBy(dx: 0, dy: 12)).fill()

        UIColor(red: 146 / 255, green: 64 / 255, blue: 14 / 255, alpha: 0.35).setStroke()
        let stripe = UIBezierPath()
        var x: CGFloat = -36
        while x < bounds.width + 40 {
            stripe.move(to: CGPoint(x: x, y: groundTop + 22))
            stripe.addLine(to: CGPoint(x: x + 58, y: bounds.height))
            x += 38
        }
        stripe.lineWidth = 4
        stripe.stroke()
    }

    private func drawBird() {
        let bodyRect = CGRect(
            x: birdX - birdRadius,
            y: birdY - birdRadius,
            width: birdRadius * 2,
            height: birdRadius * 2
        )

        UIColor(red: 250 / 255, green: 204 / 255, blue: 21 / 255, alpha: 1).setFill()
        UIBezierPath(ovalIn: bodyRect).fill()

        UIColor(red: 251 / 255, green: 146 / 255, blue: 60 / 255, alpha: 1).setFill()
        let wing = UIBezierPath()
        wing.move(to: CGPoint(x: birdX - 8, y: birdY + 1))
        wing.addQuadCurve(to: CGPoint(x: birdX + 9, y: birdY + 8), controlPoint: CGPoint(x: birdX - 1, y: birdY + 18))
        wing.addQuadCurve(to: CGPoint(x: birdX + 4, y: birdY - 6), controlPoint: CGPoint(x: birdX + 17, y: birdY - 1))
        wing.close()
        wing.fill()

        UIColor.white.setFill()
        UIBezierPath(ovalIn: CGRect(x: birdX + 5, y: birdY - 10, width: 10, height: 10)).fill()
        UIColor(red: 15 / 255, green: 23 / 255, blue: 42 / 255, alpha: 1).setFill()
        UIBezierPath(ovalIn: CGRect(x: birdX + 9, y: birdY - 7, width: 4, height: 4)).fill()

        UIColor(red: 239 / 255, green: 68 / 255, blue: 68 / 255, alpha: 1).setFill()
        let beak = UIBezierPath()
        beak.move(to: CGPoint(x: birdX + birdRadius - 1, y: birdY - 3))
        beak.addLine(to: CGPoint(x: birdX + birdRadius + 16, y: birdY + 3))
        beak.addLine(to: CGPoint(x: birdX + birdRadius - 1, y: birdY + 8))
        beak.close()
        beak.fill()
    }

    private func drawOverlayIfNeeded() {
        switch state {
        case .playing:
            return
        case .ready:
            drawCentered(text: "Flappy CX", y: bounds.height * 0.34, fontSize: 34, color: .white)
            drawCentered(text: "Tap", y: bounds.height * 0.43, fontSize: 18, color: UIColor(red: 15 / 255, green: 23 / 255, blue: 42 / 255, alpha: 0.82))
        case .gameOver:
            drawCentered(text: "Game Over", y: bounds.height * 0.35, fontSize: 32, color: .white)
            drawCentered(text: "\(score)", y: bounds.height * 0.44, fontSize: 42, color: UIColor(red: 15 / 255, green: 23 / 255, blue: 42 / 255, alpha: 0.9))
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
