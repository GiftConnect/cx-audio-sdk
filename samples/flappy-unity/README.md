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
7. Switch the build target to Android and build or run the included scene.

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

The sample intentionally omits advertising identifiers and requests limited
ad tracking. Production hosts own their CMP and privacy UI; see
[`../../docs/PRIVACY.md`](../../docs/PRIVACY.md).
