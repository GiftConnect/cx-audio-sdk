package com.cx.audio.samples.flappy;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

public final class MainActivity extends Activity implements FlappyGameView.Listener {
    private static final long READY_STICKY_DELAY_MS = 8_000;
    private static final long MIN_INTERSTITIAL_INTERVAL_MS = 90_000;
    private static final long LONG_SESSION_INTERVAL_MS = 180_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final long sessionStartedAt = System.currentTimeMillis();
    private FlappyGameView gameView;
    private TextView scoreView;
    private TextView bestView;
    private TextView adStatusView;
    private GameAdController ads;
    private int score;
    private int bestScore;
    private int completedRounds;
    private int roundsSinceInterstitial;
    private boolean earnedHighScore;
    private boolean requestedReadySticky;
    private boolean replayLocked;
    private long lastInterstitialAt;

    private final Runnable readySticky = () -> {
        if (!requestedReadySticky && gameView.getState() == FlappyGameView.State.READY) {
            requestedReadySticky = true;
            ads.showReadySticky();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        ads = new GameAdController(this, status -> adStatusView.setText(status));
        setContentView(buildContent());
        gameView.setListener(this);
        gameView.reset();
    }

    @Override protected void onResume() {
        super.onResume();
        if (gameView.getState() == FlappyGameView.State.READY) scheduleReadySticky();
    }

    @Override protected void onPause() {
        cancelReadySticky();
        ads.stop();
        super.onPause();
    }

    @Override protected void onDestroy() {
        gameView.stop();
        super.onDestroy();
    }

    @Override public void onScoreChanged(int nextScore) {
        score = nextScore;
        if (score > bestScore) earnedHighScore = true;
        bestScore = Math.max(bestScore, score);
        scoreView.setText("Score\n" + score);
        bestView.setText("Best\n" + bestScore);
    }

    @Override public void onStateChanged(FlappyGameView.State state) {
        if (state == FlappyGameView.State.READY) {
            scheduleReadySticky();
        } else if (state == FlappyGameView.State.PLAYING) {
            cancelReadySticky();
            ads.dismissReadySticky();
            earnedHighScore = false;
        } else {
            cancelReadySticky();
            ads.dismissReadySticky();
            handleGameOver();
        }
    }

    private void handleGameOver() {
        completedRounds++;
        roundsSinceInterstitial++;
        long now = System.currentTimeMillis();
        if (completedRounds < 2) return;
        if (lastInterstitialAt != 0 && now - lastInterstitialAt < MIN_INTERSTITIAL_INTERVAL_MS) return;
        long reference = lastInterstitialAt == 0 ? sessionStartedAt : lastInterstitialAt;
        if (!earnedHighScore && roundsSinceInterstitial < 3 && now - reference < LONG_SESSION_INTERVAL_MS) return;
        lastInterstitialAt = now;
        roundsSinceInterstitial = 0;
        replayLocked = true;
        gameView.setReplayEnabled(false);
        ads.showInterstitial(() -> {
            replayLocked = false;
            gameView.setReplayEnabled(true);
        });
    }

    private void scheduleReadySticky() {
        cancelReadySticky();
        if (!requestedReadySticky) handler.postDelayed(readySticky, READY_STICKY_DELAY_MS);
    }

    private void cancelReadySticky() {
        handler.removeCallbacks(readySticky);
    }

    private LinearLayout buildContent() {
        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(2, 31, 58));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER);
        top.setPadding(pad, dp(12), pad, dp(8));
        scoreView = metric("Score\n0");
        bestView = metric("Best\n0");
        TextView title = text("Flappy Blob", 24, Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        top.addView(scoreView, new LinearLayout.LayoutParams(0, dp(54), 1));
        top.addView(title, new LinearLayout.LayoutParams(0, dp(54), 2));
        top.addView(bestView, new LinearLayout.LayoutParams(0, dp(54), 1));
        root.addView(top);

        gameView = new FlappyGameView(this);
        root.addView(gameView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(pad, dp(8), pad, dp(16));
        Spinner difficulty = new Spinner(this);
        difficulty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Easy", "Normal", "Hard"}));
        difficulty.setSelection(1);
        difficulty.setOnItemSelectedListener(new SimpleItemSelectedListener(position -> gameView.setDifficulty(position == 0 ? FlappyGameView.Difficulty.EASY : position == 2 ? FlappyGameView.Difficulty.HARD : FlappyGameView.Difficulty.NORMAL)));
        controls.addView(difficulty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        TextView liftLabel = text("Tap lift: 345", 13, 0xFFCBD5E1);
        controls.addView(liftLabel);
        SeekBar lift = new SeekBar(this);
        lift.setMax(240);
        lift.setProgress(105);
        lift.setOnSeekBarChangeListener(new SimpleSeekListener(value -> {
            int amount = 240 + value;
            gameView.setTapLift(amount);
            liftLabel.setText("Tap lift: " + amount);
        }));
        controls.addView(lift);
        adStatusView = text("Ad idle", 13, 0xFFCBD5E1);
        adStatusView.setGravity(Gravity.CENTER);
        controls.addView(adStatusView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));
        root.addView(controls);
        return root;
    }

    private TextView metric(String value) {
        TextView view = text(value, 14, 0xFFE2E8F0);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static final class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        interface Callback { void selected(int position); }
        private final Callback callback;
        SimpleItemSelectedListener(Callback callback) { this.callback = callback; }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) { callback.selected(position); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
    }

    private static final class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        interface Callback { void changed(int value); }
        private final Callback callback;
        SimpleSeekListener(Callback callback) { this.callback = callback; }
        @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { callback.changed(progress); }
        @Override public void onStartTrackingTouch(SeekBar seekBar) { }
        @Override public void onStopTrackingTouch(SeekBar seekBar) { }
    }
}
