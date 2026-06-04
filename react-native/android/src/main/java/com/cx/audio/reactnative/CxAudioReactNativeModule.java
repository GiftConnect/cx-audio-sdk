package com.cx.audio.reactnative;

import android.app.Activity;

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
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class CxAudioReactNativeModule extends ReactContextBaseJavaModule {
    private static final String MODULE_NAME = "CxAudioReactNative";
    private static final String EVENT_NAME = "CxAudioEvent";
    private static final String BACKGROUND_AUDIO = "backgroundAudio";
    private static final String STICKY_PLAYER = "stickyPlayer";
    private static final String FULLSCREEN_INTERSTITIAL = "fullscreenInterstitial";

    private final ReactApplicationContext reactContext;
    private final AtomicInteger nextHandleId = new AtomicInteger(1);
    private final Map<String, BackgroundAudioHandle> backgroundAudioHandles =
            new ConcurrentHashMap<>();
    private final Map<String, StickyPlayerHandle> stickyPlayerHandles =
            new ConcurrentHashMap<>();
    private final Map<String, FullscreenInterstitialHandle> fullscreenInterstitialHandles =
            new ConcurrentHashMap<>();

    public CxAudioReactNativeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @ReactMethod
    public void playBackgroundAudio(
            ReadableMap requestMap,
            ReadableMap optionsMap,
            Promise promise) {
        String handleId = newHandleId(BACKGROUND_AUDIO);
        try {
            BackgroundAudioRequest request = readBackgroundAudioRequest(requestMap);
            BackgroundAudioOptions options = readBackgroundAudioOptions(optionsMap);
            BackgroundAudioHandle handle = AndroidBackgroundAudio.play(
                    reactContext,
                    request,
                    options,
                    new BackgroundAudioListener() {
                        @Override
                        public void onBackgroundAudioStarted(BackgroundAudioHandle handle) {
                            sendEvent("started", BACKGROUND_AUDIO, handleId, null, null);
                        }

                        @Override
                        public void onBackgroundAudioCompleted() {
                            backgroundAudioHandles.remove(handleId);
                            sendEvent("completed", BACKGROUND_AUDIO, handleId, null, null);
                        }

                        @Override
                        public void onBackgroundAudioStopped() {
                            backgroundAudioHandles.remove(handleId);
                            sendEvent("stopped", BACKGROUND_AUDIO, handleId, null, null);
                        }

                        @Override
                        public void onBackgroundAudioFailed(Throwable error) {
                            backgroundAudioHandles.remove(handleId);
                            sendEvent("failed", BACKGROUND_AUDIO, handleId, null, error);
                        }
                    });
            backgroundAudioHandles.put(handleId, handle);
            promise.resolve(handleId);
        } catch (Throwable error) {
            promise.reject("E_BACKGROUND_AUDIO", error);
        }
    }

    @ReactMethod
    public void stopBackgroundAudio(String handleId, Promise promise) {
        BackgroundAudioHandle handle = backgroundAudioHandles.remove(handleId);
        if (handle == null) {
            promise.reject("E_HANDLE_NOT_FOUND", "No background audio handle for id " + handleId);
            return;
        }

        handle.stop();
        promise.resolve(null);
    }

    @ReactMethod
    public void setBackgroundAudioVolume(String handleId, double volume, Promise promise) {
        BackgroundAudioHandle handle = backgroundAudioHandles.get(handleId);
        if (handle == null) {
            promise.reject("E_HANDLE_NOT_FOUND", "No background audio handle for id " + handleId);
            return;
        }

        try {
            handle.setVolume((float) volume);
            promise.resolve(null);
        } catch (Throwable error) {
            promise.reject("E_BACKGROUND_AUDIO_VOLUME", error);
        }
    }

    @ReactMethod
    public void showStickyPlayer(
            ReadableMap requestMap,
            ReadableMap optionsMap,
            Promise promise) {
        Activity activity = currentActivityOrReject(promise);
        if (activity == null) {
            return;
        }

        String handleId = newHandleId(STICKY_PLAYER);
        try {
            StickyPlayerRequest request = readStickyPlayerRequest(requestMap);
            StickyPlayerOptions options = readStickyPlayerOptions(optionsMap);
            StickyPlayerHandle handle = AndroidStickyPlayer.show(
                    activity,
                    request,
                    options,
                    new StickyPlayerListener() {
                        @Override
                        public void onStickyPlayerLoaded(StickyPlayerHandle handle) {
                            sendEvent("loaded", STICKY_PLAYER, handleId, null, null);
                        }

                        @Override
                        public void onStickyPlayerClicked(String clickThroughUrl) {
                            sendEvent("clicked", STICKY_PLAYER, handleId, clickThroughUrl, null);
                        }

                        @Override
                        public void onStickyPlayerDismissed() {
                            stickyPlayerHandles.remove(handleId);
                            sendEvent("dismissed", STICKY_PLAYER, handleId, null, null);
                        }

                        @Override
                        public void onStickyPlayerFailed(Throwable error) {
                            stickyPlayerHandles.remove(handleId);
                            sendEvent("failed", STICKY_PLAYER, handleId, null, error);
                        }
                    });
            stickyPlayerHandles.put(handleId, handle);
            promise.resolve(handleId);
        } catch (Throwable error) {
            promise.reject("E_STICKY_PLAYER", error);
        }
    }

    @ReactMethod
    public void dismissStickyPlayer(String handleId, Promise promise) {
        StickyPlayerHandle handle = stickyPlayerHandles.remove(handleId);
        if (handle == null) {
            promise.reject("E_HANDLE_NOT_FOUND", "No sticky player handle for id " + handleId);
            return;
        }

        handle.dismiss();
        promise.resolve(null);
    }

    @ReactMethod
    public void showFullscreenInterstitial(
            ReadableMap requestMap,
            ReadableMap optionsMap,
            Promise promise) {
        Activity activity = currentActivityOrReject(promise);
        if (activity == null) {
            return;
        }

        String handleId = newHandleId(FULLSCREEN_INTERSTITIAL);
        try {
            FullscreenInterstitialRequest request = readFullscreenInterstitialRequest(requestMap);
            FullscreenInterstitialOptions options = readFullscreenInterstitialOptions(optionsMap);
            FullscreenInterstitialHandle handle = AndroidFullscreenInterstitial.show(
                    activity,
                    request,
                    options,
                    new FullscreenInterstitialListener() {
                        @Override
                        public void onFullscreenInterstitialLoaded(
                                FullscreenInterstitialHandle handle) {
                            sendEvent("loaded", FULLSCREEN_INTERSTITIAL, handleId, null, null);
                        }

                        @Override
                        public void onFullscreenInterstitialClicked(String clickThroughUrl) {
                            sendEvent(
                                    "clicked",
                                    FULLSCREEN_INTERSTITIAL,
                                    handleId,
                                    clickThroughUrl,
                                    null);
                        }

                        @Override
                        public void onFullscreenInterstitialDismissed() {
                            fullscreenInterstitialHandles.remove(handleId);
                            sendEvent("dismissed", FULLSCREEN_INTERSTITIAL, handleId, null, null);
                        }

                        @Override
                        public void onFullscreenInterstitialFailed(Throwable error) {
                            fullscreenInterstitialHandles.remove(handleId);
                            sendEvent("failed", FULLSCREEN_INTERSTITIAL, handleId, null, error);
                        }
                    });
            fullscreenInterstitialHandles.put(handleId, handle);
            promise.resolve(handleId);
        } catch (Throwable error) {
            promise.reject("E_FULLSCREEN_INTERSTITIAL", error);
        }
    }

    @ReactMethod
    public void dismissFullscreenInterstitial(String handleId, Promise promise) {
        FullscreenInterstitialHandle handle = fullscreenInterstitialHandles.remove(handleId);
        if (handle == null) {
            promise.reject(
                    "E_HANDLE_NOT_FOUND",
                    "No fullscreen interstitial handle for id " + handleId);
            return;
        }

        handle.dismiss();
        promise.resolve(null);
    }

    @ReactMethod
    public void addListener(String eventName) {
        // Required by NativeEventEmitter.
    }

    @ReactMethod
    public void removeListeners(Integer count) {
        // Required by NativeEventEmitter.
    }

    private Activity currentActivityOrReject(Promise promise) {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            promise.reject(
                    "E_NO_ACTIVITY",
                    "A foreground Activity is required for this placement");
        }
        return activity;
    }

    private String newHandleId(String placement) {
        return placement + "-" + nextHandleId.getAndIncrement();
    }

    private void sendEvent(
            String type,
            String placement,
            String handleId,
            String clickThroughUrl,
            Throwable error) {
        WritableMap event = Arguments.createMap();
        event.putString("type", type);
        event.putString("placement", placement);
        if (handleId != null) {
            event.putString("handleId", handleId);
        }
        if (clickThroughUrl != null) {
            event.putString("clickThroughUrl", clickThroughUrl);
        }
        if (error != null) {
            event.putString("message", error.getMessage());
            event.putString("errorClass", error.getClass().getName());
        }

        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(EVENT_NAME, event);
    }

    private static BackgroundAudioRequest readBackgroundAudioRequest(ReadableMap requestMap) {
        BackgroundAudioRequest.Builder builder =
                BackgroundAudioRequest.builder(requiredString(requestMap, "vastTagUrl"));
        applyBackgroundAudioNetworkOptions(builder, requestMap);
        return builder.build();
    }

    private static StickyPlayerRequest readStickyPlayerRequest(ReadableMap requestMap) {
        StickyPlayerRequest.Builder builder =
                StickyPlayerRequest.builder(requiredString(requestMap, "vastTagUrl"));
        applyStickyPlayerNetworkOptions(builder, requestMap);
        return builder.build();
    }

    private static FullscreenInterstitialRequest readFullscreenInterstitialRequest(
            ReadableMap requestMap) {
        FullscreenInterstitialRequest.Builder builder =
                FullscreenInterstitialRequest.builder(requiredString(requestMap, "vastTagUrl"));
        applyFullscreenInterstitialNetworkOptions(builder, requestMap);
        return builder.build();
    }

    private static void applyBackgroundAudioNetworkOptions(
            BackgroundAudioRequest.Builder builder,
            ReadableMap requestMap) {
        if (hasValue(requestMap, "connectTimeoutMs")) {
            builder.connectTimeoutMs(intValue(requestMap, "connectTimeoutMs"));
        }
        if (hasValue(requestMap, "readTimeoutMs")) {
            builder.readTimeoutMs(intValue(requestMap, "readTimeoutMs"));
        }
        if (hasValue(requestMap, "userAgent")) {
            builder.userAgent(requestMap.getString("userAgent"));
        }
    }

    private static void applyStickyPlayerNetworkOptions(
            StickyPlayerRequest.Builder builder,
            ReadableMap requestMap) {
        if (hasValue(requestMap, "connectTimeoutMs")) {
            builder.connectTimeoutMs(intValue(requestMap, "connectTimeoutMs"));
        }
        if (hasValue(requestMap, "readTimeoutMs")) {
            builder.readTimeoutMs(intValue(requestMap, "readTimeoutMs"));
        }
        if (hasValue(requestMap, "userAgent")) {
            builder.userAgent(requestMap.getString("userAgent"));
        }
    }

    private static void applyFullscreenInterstitialNetworkOptions(
            FullscreenInterstitialRequest.Builder builder,
            ReadableMap requestMap) {
        if (hasValue(requestMap, "connectTimeoutMs")) {
            builder.connectTimeoutMs(intValue(requestMap, "connectTimeoutMs"));
        }
        if (hasValue(requestMap, "readTimeoutMs")) {
            builder.readTimeoutMs(intValue(requestMap, "readTimeoutMs"));
        }
        if (hasValue(requestMap, "userAgent")) {
            builder.userAgent(requestMap.getString("userAgent"));
        }
    }

    private static BackgroundAudioOptions readBackgroundAudioOptions(ReadableMap optionsMap) {
        BackgroundAudioOptions.Builder builder = BackgroundAudioOptions.builder();
        if (hasValue(optionsMap, "volume")) {
            builder.volume((float) optionsMap.getDouble("volume"));
        }
        if (hasValue(optionsMap, "minDeviceVolume")) {
            builder.minDeviceVolume((float) optionsMap.getDouble("minDeviceVolume"));
        }
        return builder.build();
    }

    private static StickyPlayerOptions readStickyPlayerOptions(ReadableMap optionsMap) {
        StickyPlayerOptions.Builder builder = StickyPlayerOptions.builder();
        if (hasValue(optionsMap, "position")) {
            builder.position(readStickyPlayerPosition(optionsMap.getString("position")));
        }
        if (hasValue(optionsMap, "heightDp")) {
            builder.heightDp(intValue(optionsMap, "heightDp"));
        }
        if (hasValue(optionsMap, "horizontalPaddingDp")) {
            builder.horizontalPaddingDp(intValue(optionsMap, "horizontalPaddingDp"));
        }
        if (hasValue(optionsMap, "showCloseButton")) {
            builder.showCloseButton(optionsMap.getBoolean("showCloseButton"));
        }
        return builder.build();
    }

    private static FullscreenInterstitialOptions readFullscreenInterstitialOptions(
            ReadableMap optionsMap) {
        FullscreenInterstitialOptions.Builder builder = FullscreenInterstitialOptions.builder();
        if (hasValue(optionsMap, "showCloseButton")) {
            builder.showCloseButton(optionsMap.getBoolean("showCloseButton"));
        }
        if (hasValue(optionsMap, "companionHeightDp")) {
            builder.companionHeightDp(intValue(optionsMap, "companionHeightDp"));
        }
        if (hasValue(optionsMap, "companionMaxWidthDp")) {
            builder.companionMaxWidthDp(intValue(optionsMap, "companionMaxWidthDp"));
        }
        if (hasValue(optionsMap, "autoPlayVideo")) {
            builder.autoPlayVideo(optionsMap.getBoolean("autoPlayVideo"));
        }
        return builder.build();
    }

    private static StickyPlayerPosition readStickyPlayerPosition(String position) {
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }

        String normalizedPosition = position.toLowerCase(Locale.US);
        if ("top".equals(normalizedPosition)) {
            return StickyPlayerPosition.TOP;
        }
        if ("bottom".equals(normalizedPosition)) {
            return StickyPlayerPosition.BOTTOM;
        }
        throw new IllegalArgumentException("position must be top or bottom");
    }

    private static String requiredString(ReadableMap map, String key) {
        if (!hasValue(map, key)) {
            throw new IllegalArgumentException(key + " is required");
        }

        String value = map.getString(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(key + " must not be blank");
        }
        return value;
    }

    private static int intValue(ReadableMap map, String key) {
        return (int) Math.round(map.getDouble(key));
    }

    private static boolean hasValue(ReadableMap map, String key) {
        return map != null && map.hasKey(key) && !map.isNull(key);
    }
}
