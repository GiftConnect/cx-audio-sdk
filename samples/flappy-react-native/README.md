# Flappy Blob React Native Sample

This Expo development-build sample runs the shared React Native Flappy Blob game on Android or iOS and exercises the local `cx-audio-sdk` package. Expo Go cannot load custom native modules, so use the included native development-build commands.

Install Node.js 20.19.4 or newer before running the Expo toolchain.

```sh
cd samples/flappy-react-native
npm install
EXPO_PUBLIC_CX_PLACEMENT_HASH=YOUR_PLACEMENT_HASH npm run android
# or
EXPO_PUBLIC_CX_PLACEMENT_HASH=YOUR_PLACEMENT_HASH npm run ios
```

The first platform run generates the native project through Expo prebuild. The local file dependency links the repository's React Native Android bridge. During iOS prebuild, `plugins/withCxAudioSdk.js` also adds the local native `CxAudioSDK` pod required by the React Native bridge.

The placement policy matches the native samples: one delayed ready-screen sticky placement, no new placements during play, and frequency-capped post-game fullscreen interstitials that lock replay until terminal callbacks.

The sample requests the platform tracking permission, rejects unavailable or
all-zero advertising identifiers, and waits for privacy initialization before
loading an ad. It passes an authorized ID as IDFA/GAID and IFA; otherwise it
requests limited tracking. The regional override is debug-only for the QA
placement. Production hosts own their CMP/privacy UI and should rely on the
applicable standard signals. See
[`../../docs/PRIVACY.md`](../../docs/PRIVACY.md).
