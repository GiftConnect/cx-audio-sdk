package com.cx.audio.sample;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
    private static final int BLUE = Color.rgb(25, 111, 215);
    private static final int GREEN = Color.rgb(36, 172, 54);
    private static final int PURPLE = Color.rgb(111, 78, 198);
    private static final int INK = Color.rgb(12, 28, 58);

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
        scrollView.setBackgroundColor(Color.rgb(244, 248, 252));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(24));
        scrollView.addView(content);

        content.addView(header(), matchWidthWrapHeight());
        content.addView(tagInputSection(), matchWidthWrapHeight());
        content.addView(sectionTitle("3 placement types. Maximum impact."), matchWidthWrapHeight());

        content.addView(placementCard(
                "Background Audio",
                "Audio ads play at low volume during gameplay or app usage.",
                "AUDIO",
                BLUE,
                new String[]{
                        "VAST 4.x audio support",
                        "Streams audio with low-latency playback",
                        "Volume control and lifecycle callbacks"
                },
                "Play background audio",
                this::playBackgroundAudio,
                "Stop",
                this::stopBackgroundAudio));

        content.addView(placementCard(
                "Sticky Player",
                "Clickable companion banner stays at the top or bottom while users engage.",
                "STICKY",
                GREEN,
                new String[]{
                        "Shows scaled 300x250 companion creative",
                        "Top or bottom placement options",
                        "Clickable and dismissible"
                },
                "Show sticky player",
                this::showStickyPlayer,
                "Dismiss",
                this::dismissStickyPlayer));

        content.addView(placementCard(
                "Full Screen / Interstitial",
                "Engaging full screen experience with optional companion banner action.",
                "FULL",
                PURPLE,
                new String[]{
                        "Supports image and video media",
                        "Optional 300x250 companion ad",
                        "Close button and programmatic dismiss"
                },
                "Show interstitial",
                this::showFullscreenInterstitial,
                "Dismiss",
                this::dismissFullscreenInterstitial));

        statusText = new TextView(this);
        statusText.setText("Ready. Replace the VAST tag URL with a known-good test tag before running placements.");
        statusText.setTextColor(INK);
        statusText.setTextSize(15);
        statusText.setPadding(dp(14), dp(14), dp(14), dp(14));
        statusText.setBackground(cardBackground(Color.WHITE, Color.rgb(196, 215, 236), dp(1)));
        LinearLayout.LayoutParams statusParams = matchWidthWrapHeight();
        statusParams.topMargin = dp(14);
        content.addView(statusText, statusParams);

        return scrollView;
    }

    private View header() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(18), dp(18), dp(18));
        header.setBackground(cardBackground(Color.rgb(2, 6, 12), Color.TRANSPARENT, 0));

        TextView brand = new TextView(this);
        brand.setText("CX  AUDIO SDK");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(30);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(brand, matchWidthWrapHeight());

        TextView tagline = new TextView(this);
        tagline.setText("Integrate audio ads. Drive results.");
        tagline.setTextColor(Color.rgb(69, 151, 255));
        tagline.setTextSize(18);
        tagline.setTypeface(Typeface.DEFAULT_BOLD);
        tagline.setPadding(0, dp(8), 0, 0);
        header.addView(tagline, matchWidthWrapHeight());

        TextView summary = new TextView(this);
        summary.setText("A mobile SDK demo for VAST 4.x audio ad placements.");
        summary.setTextColor(Color.rgb(225, 231, 240));
        summary.setTextSize(15);
        summary.setPadding(0, dp(8), 0, 0);
        header.addView(summary, matchWidthWrapHeight());

        return header;
    }

    private View tagInputSection() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, dp(16), 0, 0);

        TextView label = new TextView(this);
        label.setText("Test VAST tag URL");
        label.setTextColor(INK);
        label.setTextSize(15);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        section.addView(label, matchWidthWrapHeight());

        vastTagInput = new EditText(this);
        vastTagInput.setSingleLine(false);
        vastTagInput.setMinLines(2);
        vastTagInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        vastTagInput.setText(DEFAULT_VAST_TAG_URL);
        vastTagInput.setTextSize(14);
        vastTagInput.setBackground(cardBackground(Color.WHITE, Color.rgb(196, 215, 236), dp(1)));
        vastTagInput.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams inputParams = matchWidthWrapHeight();
        inputParams.topMargin = dp(6);
        section.addView(vastTagInput, inputParams);

        return section;
    }

    private TextView sectionTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text.toUpperCase());
        title.setTextColor(INK);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(18), 0, dp(6));
        return title;
    }

    private View placementCard(
            String title,
            String subtitle,
            String badge,
            int accentColor,
            String[] bullets,
            String primaryTitle,
            View.OnClickListener primaryAction,
            String secondaryTitle,
            View.OnClickListener secondaryAction) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(cardBackground(Color.WHITE, Color.rgb(210, 222, 236), dp(1)));

        LinearLayout.LayoutParams cardParams = matchWidthWrapHeight();
        cardParams.topMargin = dp(12);
        card.setLayoutParams(cardParams);

        TextView badgeView = new TextView(this);
        badgeView.setText(badge);
        badgeView.setTextColor(Color.WHITE);
        badgeView.setTextSize(12);
        badgeView.setTypeface(Typeface.DEFAULT_BOLD);
        badgeView.setGravity(Gravity.CENTER);
        badgeView.setPadding(dp(10), dp(5), dp(10), dp(5));
        badgeView.setBackground(pillBackground(accentColor));
        card.addView(badgeView, wrapContent());

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(accentColor);
        titleView.setTextSize(22);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setPadding(0, dp(10), 0, 0);
        card.addView(titleView, matchWidthWrapHeight());

        TextView subtitleView = new TextView(this);
        subtitleView.setText(subtitle);
        subtitleView.setTextColor(Color.rgb(23, 32, 45));
        subtitleView.setTextSize(15);
        subtitleView.setPadding(0, dp(4), 0, dp(8));
        card.addView(subtitleView, matchWidthWrapHeight());

        for (String bullet : bullets) {
            TextView bulletView = new TextView(this);
            bulletView.setText("✓  " + bullet);
            bulletView.setTextColor(Color.rgb(31, 43, 57));
            bulletView.setTextSize(14);
            bulletView.setPadding(0, dp(5), 0, 0);
            card.addView(bulletView, matchWidthWrapHeight());
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(12), 0, 0);
        actions.addView(button(primaryTitle, accentColor, primaryAction), weightedButtonParams());
        actions.addView(button(secondaryTitle, Color.rgb(89, 104, 123), secondaryAction), weightedButtonParams());
        card.addView(actions, matchWidthWrapHeight());

        return card;
    }

    private Button button(String title, int color, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextColor(Color.WHITE);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setBackground(pillBackground(color));
        button.setOnClickListener(listener);
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

    private LinearLayout.LayoutParams wrapContent() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weightedButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1);
        params.rightMargin = dp(8);
        return params;
    }

    private GradientDrawable cardBackground(int color, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(12));
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private GradientDrawable pillBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(18));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
