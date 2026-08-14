# Flappy Blob Unity Sample

This Unity 2021.3+ Android sample runs a compact Flappy Blob game and exercises
the local `com.cx.audio-sdk` Unity package. The game artwork is generated at
runtime, so the project has no duplicated binary assets.

## Setup

1. Install Unity 2021.3 or newer with Android Build Support.
2. From the repository root, build the native Android SDK:

   ```sh
   ./gradlew :android:assembleRelease
   ```

3. Copy `android/build/outputs/aar/android-release.aar` to
   `samples/flappy-unity/Assets/Plugins/Android/cx-audio-sdk.aar`.
4. Open `samples/flappy-unity` as a Unity project.
5. Open `Assets/Scenes/FlappyBlob.unity`.
6. Select the `FlappyBlobGame` object and replace `YOUR_PLACEMENT_HASH` in the
   Inspector with a CX test placement hash.
7. For this controlled QA sample only, after the applicable consent flow
   permits identifier use, supply its GAID/AAID in `Advertising Id` and enable
   `Advertising Identifier Allowed`. Leave both unset for limited tracking and
   do not ship a hard-coded identifier.
8. Switch the build target to Android and build or run the included scene.

The game itself runs in the Editor, but the ad bridge intentionally runs only
in an Android player build.

## Placement policy

- A sticky player is requested after the ready screen has remained visible
  for eight seconds.
- Starting a round dismisses the sticky placement.
- No new placement is requested while a round is active.
- A game-over interstitial is frequency capped and replay remains locked until
  the interstitial is dismissed or fails.
- Handles are dismissed and disposed when the application pauses or the game
  object is destroyed.

The SDK does not collect the identifier or show consent UI. Production hosts
must obtain the GAID/AAID through their consent and identity integration, then
initialize the SDK before requesting an ad. The debug-only regional override
in this sample exists for the QA placement; release builds rely on standard
privacy signals. See [`../../docs/PRIVACY.md`](../../docs/PRIVACY.md).
