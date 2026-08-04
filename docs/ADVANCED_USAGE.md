# CX Audio SDK advanced usage

This guide documents the supported placements, structured request fields,
options, callbacks, cleanup, privacy responsibilities, error handling, and
QA-only raw VAST path across Android, iOS, React Native, and Unity.

Start with the [quick start](QUICK_START.md) if you have not played a first ad.

## Platform support

| Host | Minimum | Background audio | Sticky player | Fullscreen interstitial |
| --- | --- | --- | --- | --- |
| Android | API 23 | Yes | Yes | Yes |
| iOS | iOS 15 | Yes | Yes | Yes |
| React Native | React Native 0.71+, Android or iOS host | Yes | Yes | Yes |
| Unity | Unity 2021.3+, Android player | Yes | Yes | Yes |

React Native includes native Android and iOS bridges. Unity code can be
compiled in the Editor, but placement calls require an Android player build.

## Choosing a placement

| Placement | Use it when | Creative behavior | Host ends it with |
| --- | --- | --- | --- |
| Background audio | The app can continue its normal visual experience while a low-volume audio ad plays. | Requires a playable audio media file. No SDK visual surface is shown. | `stop()` |
| Sticky player | A persistent top or bottom audio placement is appropriate. | Requires playable audio and an image companion. The companion is clickable. | `dismiss()` |
| Fullscreen interstitial | The host has a natural full-screen break. | Supports usable video, image, or audio/companion interstitial media. Companion content can be clickable. | `dismiss()` |

The SDK owns fixed creative sizing and layout metrics. Publishers can choose
the documented options below, but cannot configure internal companion size,
sticky height, padding, protocol lists, demand endpoint, or secure transport.

### Placement-specific structured requests

Always pair the request with the placement you intend to render. That lets the
SDK add the correct demand signal and creative capabilities.

**Android**

```java
ConsumableAdRequest adRequest =
        ConsumableAdRequest.builder("YOUR_PLACEMENT_HASH").build();

BackgroundAudioRequest backgroundRequest =
        BackgroundAudioRequest.builder(activity, adRequest).build();
StickyPlayerRequest stickyRequest =
        StickyPlayerRequest.builder(activity, adRequest).build();
FullscreenInterstitialRequest fullscreenRequest =
        FullscreenInterstitialRequest.builder(activity, adRequest).build();
```

**iOS**

```swift
let adRequest = try ConsumableAdRequest(
    placementHash: "YOUR_PLACEMENT_HASH"
)

let backgroundRequest = try VastRequest(
    consumableAdRequest: adRequest,
    placement: .backgroundAudio
)
let stickyRequest = try VastRequest(
    consumableAdRequest: adRequest,
    placement: .stickyPlayer
)
let fullscreenRequest = try VastRequest(
    consumableAdRequest: adRequest,
    placement: .fullscreenInterstitial
)
```

**React Native**

```js
const backgroundRequest = createConsumableVastRequest({
  placementHash: 'YOUR_PLACEMENT_HASH',
  placement: 'backgroundAudio',
});
const stickyRequest = createConsumableVastRequest({
  placementHash: 'YOUR_PLACEMENT_HASH',
  placement: 'stickyPlayer',
});
const fullscreenRequest = createConsumableVastRequest({
  placementHash: 'YOUR_PLACEMENT_HASH',
  placement: 'fullscreenInterstitial',
});
```

**Unity**

```csharp
ConsumableAdRequest adRequest =
    new ConsumableAdRequest("YOUR_PLACEMENT_HASH");

VastRequest backgroundRequest =
    new VastRequest(adRequest, ConsumablePlacement.BackgroundAudio);
VastRequest stickyRequest =
    new VastRequest(adRequest, ConsumablePlacement.StickyPlayer);
VastRequest fullscreenRequest =
    new VastRequest(adRequest, ConsumablePlacement.FullscreenInterstitial);
```

## Structured request fields

Only the placement hash is required. Add optional values when they are known,
truthful, and permitted by the resolved privacy state. Blank strings and empty
lists are omitted.

Names below use a neutral form. Platform spellings follow normal conventions:
Android builder methods are lower camel case, Swift initializer labels are
lower camel case, React Native uses object properties, and Unity uses Pascal
case methods.

### App and supply metadata

| Field | Type | Meaning |
| --- | --- | --- |
| `placementHash` | String | Required placement identifier issued by CX. |
| `locale` | String | BCP 47-style app/user locale when the automatic value is unsuitable. |
| `sdkName` | String | Optional SDK brand override. Defaults to `Consumable Audio`; intended for approved branded or mirrored SDK distributions. |
| `appStoreUrl` / `appStoreURL` | String | Public store listing URL. |
| `appName` | String | Display name of the host application. |
| `appVersion` | String | Host application version. |
| `appBundle` | String | Android package name or iOS bundle identifier. |
| `ipAddress` | String | Trusted, server-provided address only. Prefer server-side inference. |

Android, iOS, and Unity fill common app metadata and locale when it is
available and not explicitly supplied. React Native fills locale; provide app
metadata explicitly if required. The SDK owns cache busting, secure transport,
user agent, endpoint selection, SDK version, and placement/protocol fields.
Every structured request includes the URL-encoded `sdk` name and the SDK
build version in `sdkv`. Publishers cannot override `sdkv`.

Approved branded distributions can override the default SDK name:

```java
ConsumableAdRequest.builder("YOUR_PLACEMENT_HASH")
        .sdkName("Radio.com Audio")
        .build();
```

```swift
let request = try ConsumableAdRequest(
    placementHash: "YOUR_PLACEMENT_HASH",
    sdkName: "Radio.com Audio"
)
```

```js
const request = createConsumableVastRequest({
  placementHash: 'YOUR_PLACEMENT_HASH',
  sdkName: 'Radio.com Audio',
});
```

```csharp
ConsumableAdRequest request = new ConsumableAdRequest("YOUR_PLACEMENT_HASH")
    .SdkName("Radio.com Audio");
```

Do not discover the public IP address on the device just to populate
`ipAddress`.

### Content and suitability metadata

| Field | Type | Meaning |
| --- | --- | --- |
| `contentCategories` | String list | IAB or agreed content categories describing the host content. |
| `blockedCategories` | String list | Categories that must not be returned. |
| `blockedAdvertisers` | String list | Advertiser domains to exclude. |
| `keywords` | String list | Contextual keywords. Do not include personal data. |
| `contentId` | String | Publisher's non-user content identifier. |
| `contentGenre` | String list | Content genres. |
| `contentTitle` | String | Content title. |
| `contentDurationSeconds` | Integer | Content duration in seconds. |
| `contentUrl` / `contentURL` | String | Canonical URL for the content when applicable. |

Example:

```java
ConsumableAdRequest adRequest = ConsumableAdRequest
        .builder("YOUR_PLACEMENT_HASH")
        .contentCategories("IAB9")
        .blockedCategories("IAB7-28")
        .blockedAdvertisers("example-competitor.test")
        .contentId("episode-42")
        .contentGenre("sports")
        .contentTitle("Postgame Recap")
        .contentDurationSeconds(1800)
        .build();
```

### Location

`latitude` and `longitude` accept numeric coordinates. Supply location only
when the host has a lawful purpose, the required platform permission, and a
resolved privacy state that permits the requested precision. Omit both when
location is unavailable or not allowed.

### Privacy and identifier fields

The current request types expose:

- `coppa`
- `gdpr`
- `gdprConsent`
- `usPrivacy`
- `gpp`
- `gppSectionIds`
- `limitAdTracking`
- `doNotTrack`
- `idfa`, `gadid`, `ifa`, and legacy `aid`

These fields are escape hatches for custom CMP integrations, testing, and
approved enterprise integrations. A normal integration should not parse CMP
storage or manually forward standard IAB strings.

See [Privacy and consent](#privacy-and-consent) before using any of them.

## Placement options

All volume values are linear floats from `0.0` through `1.0`. Invalid values
fail validation. Defaults are the same across supported platforms.

### Background audio

| Option | Default | Meaning |
| --- | ---: | --- |
| `volume` | `0.2` | Playback volume for the ad. |
| `minDeviceVolume` | `0.1` | Refuse playback when device output volume is below this threshold. |

The returned handle exposes:

- Android/iOS: `isPlaying`, `stop()`, and `setVolume(...)`
- React Native: `BackgroundAudio.stop(handleId)` and
  `BackgroundAudio.setVolume(handleId, volume)`
- Unity: `IsPlaying`, `Stop()`, `SetVolume(...)`, and `Dispose()`

Changing placement volume does not change the device's system volume.

### Sticky player

| Option | Default | Meaning |
| --- | ---: | --- |
| `position` | Bottom | Anchor the player to the top or bottom safe edge. |
| `showCloseButton` | `true` | Show the SDK close control. |
| `volume` | `0.2` | Audio playback volume. |
| `minDeviceVolume` | `0.1` | Refuse the placement when device output volume is too low. |

The handle exposes `isShowing`/`IsShowing` and `dismiss()`/`Dismiss()`.
React Native dismisses with `StickyPlayer.dismiss(handleId)`.

### Fullscreen interstitial

| Option | Default | Meaning |
| --- | ---: | --- |
| `showCloseButton` | `true` | Show the SDK close control. |
| `autoPlayVideo` | `true` | Start a returned video creative automatically. |
| `volume` | `0.2` | Audio or video playback volume. |
| `minDeviceVolume` | `0.1` | Refuse the placement when device output volume is too low. |

The handle exposes `isShowing`/`IsShowing` and `dismiss()`/`Dismiss()`.
React Native dismisses with `FullscreenInterstitial.dismiss(handleId)`.

### Options examples

**Android**

```java
StickyPlayerOptions stickyOptions = StickyPlayerOptions.builder()
        .position(StickyPlayerPosition.TOP)
        .showCloseButton(true)
        .volume(0.25f)
        .minDeviceVolume(0.1f)
        .build();

FullscreenInterstitialOptions fullscreenOptions =
        FullscreenInterstitialOptions.builder()
                .showCloseButton(true)
                .autoPlayVideo(false)
                .volume(0.25f)
                .minDeviceVolume(0.1f)
                .build();
```

**iOS**

```swift
let stickyOptions = StickyPlayerOptions(
    position: .top,
    showCloseButton: true,
    volume: 0.25,
    minDeviceVolume: 0.1
)

let fullscreenOptions = FullscreenInterstitialOptions(
    showCloseButton: true,
    autoPlayVideo: false,
    volume: 0.25,
    minDeviceVolume: 0.1
)
```

**React Native**

```js
await StickyPlayer.show(stickyRequest, {
  position: 'top',
  showCloseButton: true,
  volume: 0.25,
  minDeviceVolume: 0.1,
});

await FullscreenInterstitial.show(fullscreenRequest, {
  showCloseButton: true,
  autoPlayVideo: false,
  volume: 0.25,
  minDeviceVolume: 0.1,
});
```

**Unity**

```csharp
StickyPlayerOptions stickyOptions = new StickyPlayerOptions
{
    Position = StickyPlayerPosition.Top,
    ShowCloseButton = true,
    Volume = 0.25f,
    MinDeviceVolume = 0.1f
};

FullscreenInterstitialOptions fullscreenOptions =
    new FullscreenInterstitialOptions
    {
        ShowCloseButton = true,
        AutoPlayVideo = false,
        Volume = 0.25f,
        MinDeviceVolume = 0.1f
    };
```

## Showing visual placements

Visual placements require a currently visible `Activity` or
`UIViewController`.

**Android**

```java
StickyPlayerHandle stickyHandle = AndroidStickyPlayer.show(
        activity,
        stickyRequest,
        stickyOptions,
        stickyListener);

FullscreenInterstitialHandle fullscreenHandle =
        AndroidFullscreenInterstitial.show(
                activity,
                fullscreenRequest,
                fullscreenOptions,
                fullscreenListener);
```

**iOS**

```swift
let stickyHandle = CxStickyPlayer.show(
    in: viewController,
    request: stickyRequest,
    options: stickyOptions,
    listener: stickyListener
)

let fullscreenHandle = CxFullscreenInterstitial.show(
    from: viewController,
    request: fullscreenRequest,
    options: fullscreenOptions,
    listener: fullscreenListener
)
```

**React Native**

```js
const stickyHandleId = await StickyPlayer.show(stickyRequest, stickyOptions);
const fullscreenHandleId = await FullscreenInterstitial.show(
  fullscreenRequest,
  fullscreenOptions
);
```

**Unity**

```csharp
StickyPlayerHandle stickyHandle =
    StickyPlayer.Show(stickyRequest, stickyOptions, stickyListener);
FullscreenInterstitialHandle fullscreenHandle =
    FullscreenInterstitial.Show(
        fullscreenRequest,
        fullscreenOptions,
        fullscreenListener);
```

## Lifecycle and cleanup

Placement loading is asynchronous. Retain the returned handle immediately so
the host can cancel an in-flight placement.

| Event | Background | Sticky | Fullscreen | Host action |
| --- | --- | --- | --- | --- |
| Started | Yes | Yes | Yes for audio | Update UI/analytics if needed. |
| Loaded | No | Yes | Yes | The SDK visual surface is attached. |
| Clicked | No | Yes | Yes | Optional host analytics; the SDK opens supported click-through URLs. |
| Completed | Yes | Audio completion | Audio/video completion | Clear or transition host state as appropriate. |
| Stopped | Yes | No | No | Clear the background handle. |
| Dismissed | No | Yes | Yes | Clear the visual handle. |
| Failed | Yes | Yes | Yes | Clear the handle and continue the host experience. |

Rules:

1. Retain one handle per active placement.
2. Call `stop()` for background audio or `dismiss()` for a visual placement
   when the owning screen, game state, or content session ends early.
3. Clear the retained handle on failure and every terminal callback.
4. Do not block audio or SDK callbacks.
5. On Unity, call `Dispose()` after the placement is terminal. Stopping or
   dismissing and disposing are separate operations.
6. On React Native, remove the event subscription on component teardown and
   match events using both `placement` and `handleId`.

Sticky and fullscreen audio completion does not necessarily dismiss the visual
surface. Dismiss according to the host product flow.

## Network timeouts

| Host | Defaults | Valid custom value |
| --- | --- | --- |
| Android | Connect `5000 ms`; read `8000 ms` | Positive integer |
| iOS | One request timeout of `8 s` | Positive `TimeInterval` |
| React Native | Android defaults when omitted | Positive integer milliseconds |
| Unity | Android defaults when properties remain `0` | Non-negative integer milliseconds; positive values override |

**Android**

```java
BackgroundAudioRequest request = BackgroundAudioRequest
        .builder(activity, adRequest)
        .connectTimeoutMs(7000)
        .readTimeoutMs(10000)
        .build();
```

**iOS**

```swift
let request = try VastRequest(
    consumableAdRequest: adRequest,
    placement: .backgroundAudio,
    timeoutInterval: 10
)
```

**React Native**

```js
const request = createConsumableVastRequest(
  {
    placementHash: 'YOUR_PLACEMENT_HASH',
    placement: 'backgroundAudio',
  },
  {
    connectTimeoutMs: 7000,
    readTimeoutMs: 10000,
  }
);
```

**Unity**

```csharp
VastRequest request = new VastRequest(
    adRequest,
    ConsumablePlacement.BackgroundAudio)
{
    ConnectTimeoutMs = 7000,
    ReadTimeoutMs = 10000
};
```

## Raw VAST requests

Raw VAST URLs are an advanced escape hatch for QA tags or explicitly
supported non-Consumable ad servers. Production Consumable integrations should
use `ConsumableAdRequest` so the SDK owns endpoint selection and
placement-specific request construction.

Only absolute HTTP or HTTPS URLs are accepted.

```java
BackgroundAudioRequest request = BackgroundAudioRequest
        .builder("https://qa.example.test/audio-vast")
        .build();
```

```swift
let request = try VastRequest(
    vastTagURLString: "https://qa.example.test/audio-vast"
)
```

```js
const request = {
  vastTagUrl: 'https://qa.example.test/audio-vast',
};
```

```csharp
VastRequest request =
    new VastRequest("https://qa.example.test/audio-vast");
```

Do not embed user data, identifiers, consent strings, or secrets in a raw tag.
The centralized privacy enforcement required by the accepted architecture is
not fully implemented in this version, so raw tags require extra production
review.

## Privacy and consent

The publisher-facing privacy status and ownership boundary are documented in
[Privacy and consent](PRIVACY.md):

- The host selects and presents its CMP and regional privacy notices.
- The host presents ATT when appropriate and owns location permission.
- The host makes semantically correct COPPA and underage declarations.
- The SDK must ingest standard signals, resolve one conservative privacy
  state, sanitize requests, and enforce that state across every network path.
- ATT, GDPR, US privacy, and child/underage treatment are independent. Child
  and underage restrictions override every other signal.

> [!WARNING]
> Automatic standard-signal ingestion, normalized state resolution, and one
> centralized enforcement layer are not fully implemented in the current SDK.
> Do not treat the presence of request fields as proof of end-to-end privacy
> enforcement. A production release must close that gap and pass the privacy
> production-readiness review.

### Explicit privacy fields

Use explicit privacy fields only for a custom CMP integration, controlled
testing, or an approved enterprise integration. Values must come from the
host's authoritative privacy state. Apply the most restrictive result when
signals are missing, malformed, stale, unresolved, or conflicting.

Never:

- Show a second SDK-specific consent dialog.
- Treat ATT authorization as GDPR or US privacy consent.
- Use a privacy override to bypass COPPA, underage restrictions, ATT, or
  sanitization.
- Parse and forward standard consent strings in an ordinary integration.

### Advertising identifiers

The SDK does not silently collect advertising identifiers.

- On iOS, read IDFA only after ATT is authorized. If authorization is not
  granted or the value is all zeros, omit `idfa` and `ifa` and set
  `limitAdTracking` to `true`.
- On Android, use GAID only when the host has the necessary consent and
  platform declaration. If it is unavailable, deleted, limited, or all zeros,
  omit `gadid` and `ifa` and set `limitAdTracking` to `true`.
- React Native and Unity hosts must obtain any permitted identifier through
  host-native code, an identity/consent SDK, or their framework before passing
  it to the structured request.

Do not substitute Android ID, IDFV for advertising, a fingerprint, hashed
email, or another persistent fallback identifier.

### Location and IP

Omit precise location unless permission and the resolved privacy state allow
it. Do not fetch public IP on device; prefer inference by the receiving server
or a trusted server-provided value.

## Audio and UI integration

- The host owns the global application audio session and audio-focus policy.
- `volume` controls only the ad player; `minDeviceVolume` checks the current
  device output threshold.
- Call iOS visual placement APIs on the main thread.
- Provide a visible Android `Activity` or iOS `UIViewController` for sticky and
  fullscreen placements.
- The SDK owns supported click tracking and opens valid click-through URLs for
  native visual placements.
- Do not do blocking work in playback, tracking, or lifecycle callbacks.

## Errors and troubleshooting

Treat placement failure as a recoverable result: clear the handle, keep the
host experience moving, and record non-sensitive diagnostics.

| Symptom | Likely cause | Check |
| --- | --- | --- |
| Invalid request before loading | Blank placement hash, invalid raw URL, or invalid timeout | Use the structured request and a positive timeout. |
| Volume failure | `volume`/`minDeviceVolume` outside `0...1`, or device output below the threshold | Validate options and test device volume. |
| No audio media | VAST does not contain a supported audio file | Verify demand and use the matching placement. |
| Sticky player fails | Missing usable image companion, audio, or visible host screen | Verify creative assets and current Activity/view controller. |
| Fullscreen fails | No usable interstitial media/companion or visible host screen | Verify the response type and presentation context. |
| React Native module is not linked | App was not rebuilt after installation | Rebuild the Android or iOS native app after installing the package. |
| React Native handle not found | A terminal event already removed it or the wrong ID was used | Match by `placement` and `handleId`; make cleanup idempotent. |
| Unity throws unsupported-platform | Call ran in Editor or a non-Android player | Test in an Android player build with the AAR installed. |
| Click or asset load fails | Bad URL, network error, or privacy/network policy | Inspect sanitized, non-sensitive diagnostics and the test VAST response. |

On Android/iOS, listener failures include the underlying throwable/error.
React Native failed events can include `message` and `errorClass`. Unity
provides `CxAudioError`.

## Samples and validation

The repository samples demonstrate the SDK as a host application:

- [`samples/android`](../samples/android): all Android placements.
- [`samples/ios`](../samples/ios): all iOS placements.
- [Flappy Blob](../samples/flappy-ios/README.md): lifecycle triggers in a
  game-style app.

Relevant repository checks:

```sh
./gradlew :android:test :android:assemble :android:lint
./gradlew :sample-android:assembleDebug
npm run check:react-native
./gradlew :react-native-android:assemble :react-native-android:lint
swift test
```

Unity package behavior requires the Unity Editor with Android Build Support.
Full iOS validation requires Xcode and an iOS simulator or device.
