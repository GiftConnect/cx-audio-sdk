# Flappy Blob Android Sample

This native Android app mirrors the iOS Flappy Blob gameplay and placement lifecycle. It uses the local `:android` SDK module.

Set the publisher placement hash at build time, then assemble or install the app:

```sh
./gradlew :sample-flappy-android:installDebug -PcxPlacementHash=YOUR_PLACEMENT_HASH
```

The sample waits eight seconds on the ready screen before requesting one bottom sticky placement. Starting a run dismisses it. Eligible game-over transitions request a fullscreen interstitial and lock replay until dismissal, completion, or failure.

The sample resolves Google advertising ID off the UI thread and waits for that
result before requesting an ad. It forwards GAID/IFA only when the identifier
is available, the user has not limited ad tracking, and the SDK's normalized
privacy state permits identifiers. Otherwise it omits the identifier and sets
limited-ad-tracking. The explicit regional override is debug-only for controlled
QA; production hosts must use their own CMP/privacy UI. See
[`../../docs/PRIVACY.md`](../../docs/PRIVACY.md).
