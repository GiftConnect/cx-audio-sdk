# CX Audio SDK Agent Guidance

## Repository Scope

This repository contains the CX Audio SDK for Android, iOS, React Native, and Unity. Preserve behavior across supported platforms unless a task is explicitly platform-specific.

Primary implementation locations:

* Android: `android/src/main/java/com/cx/audio/android`
* iOS: `ios/Sources/CxAudioSDK`
* React Native: `react-native/src` and `react-native/android`
* Unity: `unity/Runtime`
* Sample applications: `samples`

Prefer existing request builders and placement APIs when they are consistent with accepted architecture. Preserve backward compatibility unless a breaking change is explicitly approved, but do not perpetuate an API that violates a mandatory privacy constraint without documenting and planning its migration.

Use raw VAST URL constructors only for QA tags or explicitly supported non-Consumable ad servers. Keep placement lifecycle cleanup explicit, and do not perform blocking work in real-time audio callbacks.

## Mandatory Privacy Architecture

[`docs/PRIVACY_ARCHITECTURE.md`](docs/PRIVACY_ARCHITECTURE.md) is an accepted architectural decision, not optional guidance.

Before changing privacy, consent, identifiers, location, initialization, public configuration, request construction, networking, VAST handling, tracking, clicks, measurement, platform bridges, extensions, background behavior, samples, or related documentation, read that document completely and follow it. If current code, documentation, or legacy API behavior conflicts with it, the privacy architecture takes precedence unless a later explicit architectural decision supersedes it.

The non-negotiable constraints are:

* The SDK must never implement or display its own CMP or consent UI; the host application owns consent collection and user-facing privacy prompts.
* The SDK owns standard-signal ingestion, normalized privacy-state resolution, conservative conflict handling, enforcement, request sanitization, and downstream privacy signaling.
* ATT, GDPR, US privacy, and COPPA or underage state are independent. COPPA and underage restrictions override every other signal.
* Missing, malformed, stale, unresolved, or conflicting signals must resolve conservatively.
* Every outbound ad, VAST, OpenRTB, asset, tracking, click, redirect, measurement, and reporting request must pass through one centralized privacy-enforcement layer.
* The normal public API must remain minimal and must not expose internal protocol, endpoint, transport, security, identifier-scrubbing, or enforcement details as publisher configuration.
* Explicit consent APIs are limited to custom CMP integrations, testing, approved enterprise cases, and age-related declarations.
* Initialization and privacy resolution must be thread-safe, idempotent, asynchronous where appropriate, and restrictive until resolution completes.
* App Group synchronization is only for genuine cross-sandbox use cases and must share a versioned, precomputed privacy snapshot.
* A normal integration requires only configuration, initialization, ad loading, and playback; it must not require manual consent-string parsing.

## Validation

Use the narrowest relevant validation and expand it when changing shared behavior:

* Android SDK: `./gradlew :android:test :android:assemble :android:lint`
* Android sample: `./gradlew :sample-android:assembleDebug`
* React Native: `npm run check:react-native` and `./gradlew :react-native-android:assemble :react-native-android:lint`
* iOS: `swift test` for package tests; full validation requires Xcode and an iOS simulator or device.
* Unity: validate package behavior in Unity Editor with Android Build Support.

Report any validation that could not run and why.
