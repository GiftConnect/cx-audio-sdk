# Flappy Blob Android Sample

This native Android app mirrors the iOS Flappy Blob gameplay and placement lifecycle. It uses the local `:android` SDK module.

Set the publisher placement hash at build time, then assemble or install the app:

```sh
./gradlew :sample-flappy-android:installDebug -PcxPlacementHash=YOUR_PLACEMENT_HASH
```

The sample waits eight seconds on the ready screen before requesting one bottom sticky placement. Starting a run dismisses it. Eligible game-over transitions request a fullscreen interstitial and lock replay until dismissal, completion, or failure.

The sample intentionally omits advertising identifiers and sets limited-ad-tracking. Production hosts must use their own CMP/privacy UI and rely on the SDK's normalized privacy state; see [`../../docs/PRIVACY.md`](../../docs/PRIVACY.md).
