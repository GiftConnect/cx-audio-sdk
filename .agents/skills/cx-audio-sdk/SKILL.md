---
name: cx-audio-sdk
description: Use when integrating, documenting, testing, or modifying the CX Audio SDK in Android, iOS, React Native, or Unity host apps, including ConsumableAdRequest setup, VAST tag usage, placement lifecycle wiring, privacy parameters, and advertising identifier handling.
---

# CX Audio SDK

Use this skill when a task involves integrating this repository's audio ad SDK into an app, updating its usage docs, or validating platform-specific placement behavior.

## First Steps

1. Identify the target host platform: Android, iOS, React Native, Unity, or cross-platform docs.
2. Read the relevant usage docs before changing code:
   - Cross-platform and Android: `README.md`
   - iOS: `ios/README.md`
   - Unity: `unity/README.md`
   - React Native API shape: `react-native/src/index.d.ts`
3. Prefer existing request builders and placement APIs over new abstractions unless the task explicitly asks for new SDK surface.

## Integration Rules

- Use `ConsumableAdRequest` when the host app should build a Consumable API URL from structured parameters.
- Use raw `VastRequest` URL constructors only for QA tags or non-Consumable ad servers.
- Include app metadata when available: app name, bundle/package identifier, app store URL, locale, and privacy strings.
- Let the SDK own the request endpoint, secure transport, cachebuster, user agent, VAST/OpenRTB protocol fields, creative dimensions, and fixed layout metrics.
- Do not compute public IP on device for `i`; prefer server-side inference from the request source address, or use a trusted server-provided value.
- Keep placement lifecycle cleanup explicit: retain handles, call `stop()` or `dismiss()` when the host flow ends, and clear references on terminal callbacks.
- Respect the supported placement options: volume, minimum device volume, sticky position, close button visibility, and video autoplay. Do not re-expose fixed companion sizing, height, or padding.

## Advertising Identifiers

The SDK exposes `idfa`, `gadid`, `ifa`, and `limitAdTracking` request fields, but it must not silently collect advertising identifiers.

- iOS host apps must handle App Tracking Transparency before reading IDFA with `ASIdentifierManager.shared().advertisingIdentifier`. There is not a separate iOS IFA API; use the authorized IDFA value for both `idfa` and `ifa`. If ATT is not authorized or IDFA is all zeros, omit both and set `limitAdTracking` to true.
- Android host apps that retrieve Google advertising ID must handle consent and declare `com.google.android.gms.permission.AD_ID` when targeting Android 13/API 33 or newer. If GAID is unavailable, deleted, limited, or all zeros, omit `gadid` and `ifa` and set `limitAdTracking` to true.
- React Native and Unity Android hosts should pass `gadid`, `ifa`, and `limitAdTracking` only after the host app retrieves the ID through native code, a consent/identity SDK, or the app framework.
- Do not add fingerprinting, Android ID, IDFV-for-ad-tracking, hashed email, or other persistent fallback identifiers for advertising.

## Platform Notes

Android:
- Native package: `android/src/main/java/com/cx/audio/android`
- Request builder: `ConsumableAdRequest.builder(...)`
- Placements: `AndroidBackgroundAudio`, `AndroidStickyPlayer`, `AndroidFullscreenInterstitial`

iOS:
- Swift package entry: `Package.swift`
- Source: `ios/Sources/CxAudioSDK`
- Request setup: `ConsumableAdRequest(...)` then `VastRequest(consumableAdRequest:)`
- UI placement calls should happen from the main thread.

React Native:
- Android-only bridge currently.
- Use `createConsumableVastRequest(...)` for structured Consumable requests.
- Native module delegates to the Android SDK.
- Do not require a specific app framework for advertising ID retrieval; treat the ID as host-supplied.

Unity:
- Android player builds only.
- Runtime namespace: `Cx.Audio.Unity`
- Include the Android AAR under `Assets/Plugins/Android/`.

## Validation

Choose the narrowest relevant verification:

- Android SDK: `./gradlew :android:test :android:assemble :android:lint`
- Android sample: `./gradlew :sample-android:assembleDebug`
- React Native bridge: `npm run check:react-native` and `./gradlew :react-native-android:assemble :react-native-android:lint`
- iOS typecheck: `swiftc -typecheck ios/Sources/CxAudioSDK/CxAudioRequest.swift ios/Sources/CxAudioSDK/DeviceVolumeMonitor.swift ios/Sources/CxAudioSDK/VastParser.swift ios/Sources/CxAudioSDK/VastModels.swift`
- iOS full validation requires Xcode and simulator/device builds.
- Unity package validation requires the Unity Editor with Android Build Support.

If validation cannot run because local tools are missing, report exactly what was skipped and why.
