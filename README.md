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
| Background audio | Planned | Planned | Planned | Planned |
| Sticky player | Supported | Planned | Planned | Planned |
| Full screen / interstitial | Planned | Planned | Planned | Planned |

The matrix is represented in code by `PlacementMatrix`, `PlacementType`, `TargetPlatform`, and `PlacementSupportStatus`.

## Android Sticky Player
The Android sticky player is the first supported placement. It:

- Fetches a VAST tag URL over HTTP(S).
- Resolves up to five VAST wrappers.
- Parses the best image companion ad, preferring dimensions closest to 300x250.
- Downloads the companion image.
- Renders a clickable sticky overlay at the top or bottom of the host `Activity`.
- Opens `CompanionClickThrough` URLs using an Android `ACTION_VIEW` intent.

Android module: `:android`

Minimum SDK: 23

Runtime dependencies: none beyond the Android platform.

### Build
Prerequisites:

- JDK 17 or 21 for running Gradle.
- Android SDK installed and discoverable through `ANDROID_HOME` or `local.properties`.
- Android SDK platform 35 installed.

Build the Android library:

```sh
./gradlew :android:assemble
```

If the default `java` on the machine is newer than Gradle supports, set `JAVA_HOME` to a supported JDK before running the wrapper.

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

## TODO
- Investigate implementing this as a mediation layer in AdMob SDK.
