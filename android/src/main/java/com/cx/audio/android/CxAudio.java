package com.cx.audio.android;

import android.content.Context;

import com.cx.audio.android.internal.PrivacyManager;

public final class CxAudio {
    private CxAudio() {}

    public static void initialize(Context context, CxAudioConfiguration configuration) {
        PrivacyManager.getInstance().initialize(context, configuration);
    }

    public static void initialize(Context context) {
        initialize(context, CxAudioConfiguration.builder().build());
    }

    public static void updatePrivacyOverride(CxPrivacyOverride privacyOverride) {
        PrivacyManager.getInstance().updatePrivacyOverride(privacyOverride);
    }

    public static CxPrivacyState getPrivacyState() {
        return PrivacyManager.getInstance().getSnapshot();
    }
}
