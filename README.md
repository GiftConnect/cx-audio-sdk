# cx-audio-sdk
An SDK for mobile app developers to integrate audio ads.  Calls out to a VAST tag for a VAST 4.x audio ad response.

## Target Platforms
- Android
- iOS
- React Native
- Unity

## Placement Types
| Placement | Description |
| --- | --- |
| Background audio | Plays at low volume during gameplay or app usage. |
| Sticky player | Clickable small horizontal banner at the top or bottom of the screen showing a scaled-down version of the 300x250 companion banner ad, if present. |
| Full screen / interstitial | Static slate image or video on the top, with a clickable 300x250 companion banner ad on the bottom, if present. |

## Implementation Matrix
| Placement | Android | iOS | React Native | Unity |
| --- | --- | --- | --- | --- |
| Background audio | Supported | Planned | Supported on Android hosts | Planned |
| Sticky player | Supported | Planned | Supported on Android hosts | Planned |
| Full screen / interstitial | Supported | Planned | Supported on Android hosts | Planned |

## Android Build
Android module: `:android`

Minimum SDK: 23

Runtime dependencies: none beyond the Android platform.

Prerequisites:

- JDK 17 or 21 for running Gradle.
- Android SDK installed and discoverable through `ANDROID_HOME` or `local.properties`.
- Android SDK platform 35 installed.

Build the Android library:

```sh
./gradlew :android:assemble
```

If the default `java` on the machine is newer than Gradle supports, set `JAVA_HOME` to a supported JDK before running the wrapper.

## Android Background Audio
The Android background audio placement:

- Fetches a VAST tag URL over HTTP(S).
- Resolves up to five VAST wrappers.
- Parses the best audio `MediaFile`, preferring progressive delivery and common mobile audio MIME types.
- Streams the ad audio through Android `MediaPlayer`.
- Defaults to low-volume playback suitable for gameplay or app usage.

### Usage
```java
BackgroundAudioRequest request = BackgroundAudioRequest
        .builder("https://ads.example.com/vast.xml")
        .build();

BackgroundAudioOptions options = BackgroundAudioOptions.builder()
        .volume(0.2f)
        .build();

BackgroundAudioHandle handle = AndroidBackgroundAudio.play(
        this,
        request,
        options,
        new BackgroundAudioListener() {
            @Override
            public void onBackgroundAudioStarted(BackgroundAudioHandle handle) {
                // The background audio ad is playing.
            }

            @Override
            public void onBackgroundAudioCompleted() {
                // The ad audio reached the end.
            }

            @Override
            public void onBackgroundAudioStopped() {
                // The host app stopped playback programmatically.
            }

            @Override
            public void onBackgroundAudioFailed(Throwable error) {
                // No audio media file was found or playback failed.
            }
        });
```

Call `handle.stop()` from the host app when playback should end programmatically. Call `handle.setVolume(0.0f)` through `handle.setVolume(1.0f)` to adjust playback volume after starting.

## Android Full Screen Interstitial
The Android full screen interstitial placement:

- Fetches a VAST tag URL over HTTP(S).
- Resolves up to five VAST wrappers.
- Parses a visual `MediaFile` for the main full-screen area.
- Parses an optional 300x250 companion image for the lower call-to-action area.
- Supports image or video media in the main full-screen area.
- Opens `CompanionClickThrough` URLs using an Android `ACTION_VIEW` intent.

### Usage
```java
FullscreenInterstitialRequest request = FullscreenInterstitialRequest
        .builder("https://ads.example.com/vast.xml")
        .build();

FullscreenInterstitialOptions options = FullscreenInterstitialOptions.builder()
        .showCloseButton(true)
        .companionHeightDp(250)
        .companionMaxWidthDp(300)
        .autoPlayVideo(true)
        .build();

FullscreenInterstitialHandle handle = AndroidFullscreenInterstitial.show(
        this,
        request,
        options,
        new FullscreenInterstitialListener() {
            @Override
            public void onFullscreenInterstitialLoaded(FullscreenInterstitialHandle handle) {
                // The interstitial is visible.
            }

            @Override
            public void onFullscreenInterstitialClicked(String clickThroughUrl) {
                // Track click analytics here.
            }

            @Override
            public void onFullscreenInterstitialDismissed() {
                // The user or host app dismissed the placement.
            }

            @Override
            public void onFullscreenInterstitialFailed(Throwable error) {
                // No usable media was found or rendering failed.
            }
        });
```

Call `handle.dismiss()` from the host app when the interstitial should be removed programmatically.

## Android Sticky Player
The Android sticky player:

- Fetches a VAST tag URL over HTTP(S).
- Resolves up to five VAST wrappers.
- Parses the best image companion ad, preferring dimensions closest to 300x250.
- Downloads the companion image.
- Renders a clickable sticky overlay at the top or bottom of the host `Activity`.
- Opens `CompanionClickThrough` URLs using an Android `ACTION_VIEW` intent.

### Usage
```java
StickyPlayerRequest request = StickyPlayerRequest
        .builder("https://ads.example.com/vast.xml")
        .build();

StickyPlayerOptions options = StickyPlayerOptions.builder()
        .position(StickyPlayerPosition.BOTTOM)
        .heightDp(72)
        .showCloseButton(true)
        .build();

StickyPlayerHandle handle = AndroidStickyPlayer.show(
        this,
        request,
        options,
        new StickyPlayerListener() {
            @Override
            public void onStickyPlayerLoaded(StickyPlayerHandle handle) {
                // The sticky player is visible.
            }

            @Override
            public void onStickyPlayerClicked(String clickThroughUrl) {
                // Track click analytics here.
            }

            @Override
            public void onStickyPlayerDismissed() {
                // The user or host app dismissed the placement.
            }

            @Override
            public void onStickyPlayerFailed(Throwable error) {
                // No image companion was found or the network request failed.
            }
        });
```

Call `handle.dismiss()` from the host app when the placement should be removed programmatically.

## React Native
The React Native package exposes all three placement types and delegates to the Android native implementation. iOS hosts are not implemented yet.

Package entry point: `cx-audio-sdk`

Android native module: `react-native/android`

Runtime peer dependencies: `react` and `react-native`

The React Native Android bridge compiles against the React Native Android SDK artifact `com.facebook.react:react-android`, which is normally supplied by a React Native app build.

### Events
```js
import { addCxAudioListener } from 'cx-audio-sdk';

const subscription = addCxAudioListener((event) => {
  // event.type: started, loaded, clicked, completed, stopped, dismissed, or failed
  // event.placement: backgroundAudio, stickyPlayer, or fullscreenInterstitial
});

subscription.remove();
```

### Background Audio
```js
import { BackgroundAudio } from 'cx-audio-sdk';

const handleId = await BackgroundAudio.play(
  { vastTagUrl: 'https://ads.example.com/vast.xml' },
  { volume: 0.2 }
);

await BackgroundAudio.setVolume(handleId, 0.1);
await BackgroundAudio.stop(handleId);
```

### Sticky Player
```js
import { StickyPlayer, StickyPlayerPosition } from 'cx-audio-sdk';

const handleId = await StickyPlayer.show(
  { vastTagUrl: 'https://ads.example.com/vast.xml' },
  {
    position: StickyPlayerPosition.BOTTOM,
    heightDp: 72,
    showCloseButton: true,
  }
);

await StickyPlayer.dismiss(handleId);
```

### Full Screen Interstitial
```js
import { FullscreenInterstitial } from 'cx-audio-sdk';

const handleId = await FullscreenInterstitial.show(
  { vastTagUrl: 'https://ads.example.com/vast.xml' },
  {
    showCloseButton: true,
    companionHeightDp: 250,
    companionMaxWidthDp: 300,
    autoPlayVideo: true,
  }
);

await FullscreenInterstitial.dismiss(handleId);
```

## TODO
- Investigate implementing this as a mediation layer in AdMob SDK.
