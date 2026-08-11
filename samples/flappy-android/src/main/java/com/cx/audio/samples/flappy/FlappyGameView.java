package com.cx.audio.samples.flappy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

final class FlappyGameView extends View {
    enum State { READY, PLAYING, GAME_OVER }
    enum Difficulty { EASY, NORMAL, HARD }

    interface Listener {
        void onScoreChanged(int score);
        void onStateChanged(State state);
    }

    private static final float BIRD_RADIUS = 20f;
    private static final float PIPE_WIDTH = 72f;
    private static final float PIPE_GAP = 170f;
    private static final float GRAVITY = 1050f;
    private static final float GROUND_HEIGHT = 74f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Pipe> pipes = new ArrayList<>();
    private final Random random = new Random();
    private final float density;
    private Listener listener;
    private State state = State.READY;
    private Difficulty difficulty = Difficulty.NORMAL;
    private float tapLift = 345f;
    private float birdX;
    private float birdY;
    private float velocity;
    private float deathVelocity;
    private long lastFrameMs;
    private int score;
    private boolean replayEnabled = true;

    FlappyGameView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        strokePaint.setStyle(Paint.Style.STROKE);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setContentDescription("Flappy Blob game. Tap to swim.");
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    State getState() {
        return state;
    }

    void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    void setTapLift(float tapLift) {
        this.tapLift = tapLift;
    }

    void setReplayEnabled(boolean replayEnabled) {
        this.replayEnabled = replayEnabled;
        invalidate();
    }

    void reset() {
        removeCallbacks(frameRunnable);
        score = 0;
        velocity = 0;
        deathVelocity = 0;
        birdX = Math.max(dp(76), getWidth() * 0.28f);
        birdY = Math.max(dp(120), getHeight() * 0.42f);
        pipes.clear();
        setState(State.READY);
        if (listener != null) listener.onScoreChanged(score);
        invalidate();
    }

    void stop() {
        removeCallbacks(frameRunnable);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        if (state != State.PLAYING) reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;
        performClick();
        if (state == State.READY) {
            startGame();
            flap();
        } else if (state == State.PLAYING) {
            flap();
        } else if (replayEnabled) {
            reset();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void startGame() {
        score = 0;
        pipes.clear();
        spawnPipe(getWidth() + dp(40));
        setState(State.PLAYING);
        if (listener != null) listener.onScoreChanged(score);
        lastFrameMs = SystemClock.uptimeMillis();
        postOnAnimation(frameRunnable);
    }

    private void flap() {
        velocity = -dp(tapLift);
    }

    private final Runnable frameRunnable = new Runnable() {
        @Override
        public void run() {
            if (state == State.READY) return;
            long now = SystemClock.uptimeMillis();
            float delta = Math.min((now - lastFrameMs) / 1000f, 0.035f);
            lastFrameMs = now;
            if (state == State.PLAYING) step(delta); else stepDeath(delta);
            invalidate();
            if (state != State.READY && (state != State.GAME_OVER || birdY - dp(36) <= getHeight())) {
                postOnAnimation(this);
            }
        }
    };

    private void step(float delta) {
        velocity += dp(GRAVITY) * delta;
        birdY += velocity * delta;
        float speed = difficulty == Difficulty.EASY ? 115 : difficulty == Difficulty.HARD ? 205 : 152;
        for (Pipe pipe : pipes) pipe.x -= dp(speed) * delta;
        Iterator<Pipe> iterator = pipes.iterator();
        while (iterator.hasNext()) if (iterator.next().x + dp(PIPE_WIDTH) < -dp(20)) iterator.remove();
        if (pipes.isEmpty() || getWidth() - pipes.get(pipes.size() - 1).x > Math.max(dp(190), getWidth() * 0.58f)) {
            spawnPipe(getWidth() + dp(24));
        }
        for (Pipe pipe : pipes) {
            if (!pipe.scored && pipe.x + dp(PIPE_WIDTH) < birdX) {
                pipe.scored = true;
                score++;
                if (listener != null) listener.onScoreChanged(score);
            }
        }
        if (collides()) {
            deathVelocity = dp(110);
            setState(State.GAME_OVER);
        }
    }

    private void stepDeath(float delta) {
        deathVelocity += dp(820) * delta;
        birdY += deathVelocity * delta;
    }

    private void spawnPipe(float x) {
        float gap = dp(PIPE_GAP);
        float min = dp(94) + gap / 2;
        float max = groundTop() - gap / 2 - dp(38);
        if (max < min) return;
        pipes.add(new Pipe(x, min + random.nextFloat() * (max - min)));
    }

    private boolean collides() {
        RectF bird = new RectF(birdX - dp(BIRD_RADIUS), birdY - dp(BIRD_RADIUS), birdX + dp(BIRD_RADIUS), birdY + dp(BIRD_RADIUS));
        if (bird.top <= 0 || bird.bottom >= groundTop()) return true;
        for (Pipe pipe : pipes) {
            RectF top = new RectF(pipe.x, 0, pipe.x + dp(PIPE_WIDTH), pipe.gapY - dp(PIPE_GAP) / 2);
            RectF bottom = new RectF(pipe.x, pipe.gapY + dp(PIPE_GAP) / 2, pipe.x + dp(PIPE_WIDTH), groundTop());
            if (RectF.intersects(bird, top) || RectF.intersects(bird, bottom)) return true;
        }
        return false;
    }

    private void setState(State next) {
        if (state == next) return;
        state = next;
        if (listener != null) listener.onStateChanged(next);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setShader(new LinearGradient(0, 0, 0, getHeight(), new int[]{Color.rgb(14,165,233), Color.rgb(3,105,161), Color.rgb(8,47,73)}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);
        drawBubbles(canvas);
        for (Pipe pipe : pipes) drawPipe(canvas, pipe);
        drawGround(canvas);
        drawBlob(canvas);
        if (state == State.READY) {
            drawCentered(canvas, "Flappy Blob", getHeight() * .31f, 32, Color.WHITE);
            drawCentered(canvas, "Tap to swim", getHeight() * .40f, 18, 0xDBFFFFFF);
        } else if (state == State.GAME_OVER) {
            drawCentered(canvas, "Dive Over", getHeight() * .33f, 32, Color.WHITE);
            drawCentered(canvas, String.format(Locale.US, "%d", score), getHeight() * .42f, 42, 0xEBFFFFFF);
            drawCentered(canvas, replayEnabled ? "Tap to replay" : "Ad loading…", getHeight() * .51f, 18, 0xDBFFFFFF);
        }
    }

    private void drawBubbles(Canvas canvas) {
        strokePaint.setColor(0x52FFFFFF);
        strokePaint.setStrokeWidth(dp(1.5f));
        float[][] bubbles = {{.12f,.18f,7},{.18f,.28f,3},{.76f,.12f,5},{.83f,.33f,8},{.67f,.48f,3},{.36f,.57f,5},{.08f,.63f,4},{.92f,.68f,6}};
        for (float[] bubble : bubbles) canvas.drawCircle(getWidth()*bubble[0], getHeight()*bubble[1], dp(bubble[2]), strokePaint);
    }

    private void drawPipe(Canvas canvas, Pipe pipe) {
        float halfGap = dp(PIPE_GAP) / 2;
        drawReef(canvas, new RectF(pipe.x, -dp(8), pipe.x + dp(PIPE_WIDTH), pipe.gapY - halfGap), true);
        drawReef(canvas, new RectF(pipe.x, pipe.gapY + halfGap, pipe.x + dp(PIPE_WIDTH), groundTop()), false);
    }

    private void drawReef(Canvas canvas, RectF rect, boolean top) {
        paint.setColor(Color.rgb(30,64,93));
        canvas.drawRoundRect(rect, dp(18), dp(18), paint);
        paint.setColor(Color.rgb(15,118,110));
        float ledgeY = top ? rect.bottom - dp(22) : rect.top;
        canvas.drawRoundRect(new RectF(rect.left-dp(8), ledgeY, rect.right+dp(8), ledgeY+dp(22)), dp(11), dp(11), paint);
    }

    private void drawGround(Canvas canvas) {
        paint.setColor(Color.rgb(253,230,138));
        canvas.drawRect(0, groundTop(), getWidth(), getHeight(), paint);
        paint.setColor(0x4720B981);
        for (float x : new float[]{dp(28), getWidth()-dp(48), getWidth()*.62f}) canvas.drawRoundRect(x, groundTop()+dp(12), x+dp(8), getHeight(), dp(4), dp(4), paint);
    }

    private void drawBlob(Canvas canvas) {
        paint.setColor(state == State.GAME_OVER ? Color.rgb(244,114,182) : Color.rgb(251,113,133));
        RectF body = new RectF(birdX-dp(31), birdY-dp(25), birdX+dp(31), birdY+dp(25));
        canvas.drawOval(body, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(birdX-dp(12), birdY-dp(6), dp(8), paint);
        canvas.drawCircle(birdX+dp(12), birdY-dp(6), dp(8), paint);
        paint.setColor(Color.rgb(15,23,42));
        canvas.drawCircle(birdX-dp(11), birdY-dp(5), dp(3), paint);
        canvas.drawCircle(birdX+dp(11), birdY-dp(5), dp(3), paint);
        strokePaint.setColor(Color.rgb(15,23,42));
        strokePaint.setStrokeWidth(dp(3));
        RectF mouth = new RectF(birdX-dp(11), birdY+dp(4), birdX+dp(11), birdY+dp(17));
        canvas.drawArc(mouth, state == State.GAME_OVER ? 200 : 20, 140, false, strokePaint);
    }

    private void drawCentered(Canvas canvas, String text, float y, float size, int color) {
        paint.setShader(null);
        paint.setColor(color);
        paint.setTextSize(dp(size));
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, getWidth()/2f, y, paint);
    }

    private float groundTop() { return Math.max(0, getHeight() - dp(GROUND_HEIGHT)); }
    private float dp(float value) { return value * density; }

    private static final class Pipe {
        float x;
        final float gapY;
        boolean scored;
        Pipe(float x, float gapY) { this.x = x; this.gapY = gapY; }
    }
}
