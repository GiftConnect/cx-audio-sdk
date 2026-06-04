package com.cx.audio.sample;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cx.audio.android.AndroidBackgroundAudio;
import com.cx.audio.android.AndroidFullscreenInterstitial;
import com.cx.audio.android.AndroidStickyPlayer;
import com.cx.audio.android.BackgroundAudioHandle;
import com.cx.audio.android.BackgroundAudioListener;
import com.cx.audio.android.BackgroundAudioOptions;
import com.cx.audio.android.BackgroundAudioRequest;
import com.cx.audio.android.FullscreenInterstitialHandle;
import com.cx.audio.android.FullscreenInterstitialListener;
import com.cx.audio.android.FullscreenInterstitialOptions;
import com.cx.audio.android.FullscreenInterstitialRequest;
import com.cx.audio.android.StickyPlayerHandle;
import com.cx.audio.android.StickyPlayerListener;
import com.cx.audio.android.StickyPlayerOptions;
import com.cx.audio.android.StickyPlayerPosition;
import com.cx.audio.android.StickyPlayerRequest;

public final class MainActivity extends Activity {
    private static final String DEFAULT_VAST_TAG_URL = "https://ads.example.com/vast.xml";

    private EditText vastTagInput;
    private TextView statusText;
    private BackgroundAudioHandle backgroundAudioHandle;
    private StickyPlayerHandle stickyPlayerHandle;
    private FullscreenInterstitialHandle fullscreenInterstitialHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContentView());
    }

    @Override
    protected void onDestroy() {
        if (backgroundAudioHandle != null) {
            backgroundAudioHandle.stop();
            backgroundAudioHandle = null;
        }
        if (stickyPlayerHandle != null) {
            stickyPlayerHandle.dismiss();
            stickyPlayerHandle = null;
        }
        if (fullscreenInterstitialHandle != null) {
            fullscreenInterstitialHandle.dismiss();
            fullscreenInterstitialHandle = null;
        }
        super.onDestroy();
    }

    private View buildContentView() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(24), dp(20), dp(24));
        scrollView.addView(content);

        TextView title = new TextView(this);
        title.setText("CX Audio SDK Sample");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(22, 33, 38));
        title.setGravity(Gravity.START);
        content.addView(title, matchWidthWrapHeight());

        TextView label = new TextView(this);
        label.setText("VAST tag URL");
        label.setTextColor(Color.rgb(65, 75, 80));
        label.setPadding(0, dp(18), 0, dp(6));
        content.addView(label, matchWidthWrapHeight());

        vastTagInput = new EditText(this);
        vastTagInput.setSingleLine(false);
        vastTagInput.setMinLines(2);
        vastTagInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        vastTagInput.setText(DEFAULT_VAST_TAG_URL);
        content.addView(vastTagInput, matchWidthWrapHeight());

        content.addView(button("Play Background Audio", this::playBackgroundAudio), matchWidthWrapHeight());
        content.addView(button("Stop Background Audio", this::stopBackgroundAudio), matchWidthWrapHeight());
        content.addView(button("Show Sticky Player", this::showStickyPlayer), matchWidthWrapHeight());
        content.addView(button("Dismiss Sticky Player", this::dismissStickyPlayer), matchWidthWrapHeight());
        content.addView(button("Show Fullscreen Interstitial", this::showFullscreenInterstitial), matchWidthWrapHeight());
        content.addView(button("Dismiss Fullscreen Interstitial", this::dismissFullscreenInterstitial), matchWidthWrapHeight());

        statusText = new TextView(this);
        statusText.setText("Ready. Replace the VAST tag URL with a known-good test tag before running placements.");
        statusText.setTextColor(Color.rgb(34, 44, 50));
        statusText.setTextSize(15);
        statusText.setPadding(0, dp(18), 0, 0);
        content.addView(statusText, matchWidthWrapHeight());

        return scrollView;
    }

    private Button button(String title, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(title);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        button.setLayoutParams(params);
        return button;
    }

    private void playBackgroundAudio(View ignored) {
        try {
            BackgroundAudioRequest request = BackgroundAudioRequest.builder(vastTagUrl()).build();
            BackgroundAudioOptions options = BackgroundAudioOptions.builder()
                    .volume(0.2f)
                    .build();
            backgroundAudioHandle = AndroidBackgroundAudio.play(
                    this,
                    request,
                    options,
                    new BackgroundAudioListener() {
                        @Override
                        public void onBackgroundAudioStarted(BackgroundAudioHandle handle) {
                            setStatus("Background audio started.");
                        }

                        @Override
                        public void onBackgroundAudioCompleted() {
                            backgroundAudioHandle = null;
                            setStatus("Background audio completed.");
                        }

                        @Override
                        public void onBackgroundAudioStopped() {
                            backgroundAudioHandle = null;
                            setStatus("Background audio stopped.");
                        }

                        @Override
                        public void onBackgroundAudioFailed(Throwable error) {
                            backgroundAudioHandle = null;
                            setStatus("Background audio failed: " + error.getMessage());
                        }
                    });
            setStatus("Loading background audio...");
        } catch (Throwable error) {
            setStatus("Background audio request failed: " + error.getMessage());
        }
    }

    private void stopBackgroundAudio(View ignored) {
        if (backgroundAudioHandle == null) {
            setStatus("No background audio handle is active.");
            return;
        }
        backgroundAudioHandle.stop();
    }

    private void showStickyPlayer(View ignored) {
        try {
            StickyPlayerRequest request = StickyPlayerRequest.builder(vastTagUrl()).build();
            StickyPlayerOptions options = StickyPlayerOptions.builder()
                    .position(StickyPlayerPosition.BOTTOM)
                    .heightDp(96)
                    .showCloseButton(true)
                    .build();
            stickyPlayerHandle = AndroidStickyPlayer.show(
                    this,
                    request,
                    options,
                    new StickyPlayerListener() {
                        @Override
                        public void onStickyPlayerLoaded(StickyPlayerHandle handle) {
                            setStatus("Sticky player loaded.");
                        }

                        @Override
                        public void onStickyPlayerClicked(String clickThroughUrl) {
                            setStatus("Sticky player clicked: " + clickThroughUrl);
                        }

                        @Override
                        public void onStickyPlayerDismissed() {
                            stickyPlayerHandle = null;
                            setStatus("Sticky player dismissed.");
                        }

                        @Override
                        public void onStickyPlayerFailed(Throwable error) {
                            stickyPlayerHandle = null;
                            setStatus("Sticky player failed: " + error.getMessage());
                        }
                    });
            setStatus("Loading sticky player...");
        } catch (Throwable error) {
            setStatus("Sticky player request failed: " + error.getMessage());
        }
    }

    private void dismissStickyPlayer(View ignored) {
        if (stickyPlayerHandle == null) {
            setStatus("No sticky player handle is active.");
            return;
        }
        stickyPlayerHandle.dismiss();
    }

    private void showFullscreenInterstitial(View ignored) {
        try {
            FullscreenInterstitialRequest request =
                    FullscreenInterstitialRequest.builder(vastTagUrl()).build();
            FullscreenInterstitialOptions options = FullscreenInterstitialOptions.builder()
                    .showCloseButton(true)
                    .companionHeightDp(250)
                    .companionMaxWidthDp(300)
                    .autoPlayVideo(true)
                    .build();
            fullscreenInterstitialHandle = AndroidFullscreenInterstitial.show(
                    this,
                    request,
                    options,
                    new FullscreenInterstitialListener() {
                        @Override
                        public void onFullscreenInterstitialLoaded(
                                FullscreenInterstitialHandle handle) {
                            setStatus("Fullscreen interstitial loaded.");
                        }

                        @Override
                        public void onFullscreenInterstitialClicked(String clickThroughUrl) {
                            setStatus("Fullscreen interstitial clicked: " + clickThroughUrl);
                        }

                        @Override
                        public void onFullscreenInterstitialDismissed() {
                            fullscreenInterstitialHandle = null;
                            setStatus("Fullscreen interstitial dismissed.");
                        }

                        @Override
                        public void onFullscreenInterstitialFailed(Throwable error) {
                            fullscreenInterstitialHandle = null;
                            setStatus("Fullscreen interstitial failed: " + error.getMessage());
                        }
                    });
            setStatus("Loading fullscreen interstitial...");
        } catch (Throwable error) {
            setStatus("Fullscreen interstitial request failed: " + error.getMessage());
        }
    }

    private void dismissFullscreenInterstitial(View ignored) {
        if (fullscreenInterstitialHandle == null) {
            setStatus("No fullscreen interstitial handle is active.");
            return;
        }
        fullscreenInterstitialHandle.dismiss();
    }

    private String vastTagUrl() {
        return vastTagInput.getText().toString().trim();
    }

    private void setStatus(String status) {
        statusText.setText(status);
    }

    private LinearLayout.LayoutParams matchWidthWrapHeight() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
