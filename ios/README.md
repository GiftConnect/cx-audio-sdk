# CxAudioSDK iOS

Native iOS implementation for the CX audio ad SDK.

Built by [Consumable](https://consumable.com/). View the
[source on GitHub](https://github.com/GiftConnect/cx-audio-sdk).

## Requirements

- iOS 15 or newer.
- Xcode 15 or newer.
- Swift 5.9 or newer through Xcode.
- `Foundation`, `UIKit`, and `AVFoundation`.

## Installation

### Swift Package Manager

Add the Swift package from the repository root:

1. In Xcode, choose **File > Add Package Dependencies**.
2. Select this repository URL or local package checkout.
3. Add the `CxAudioSDK` product to the host iOS app target.

### CocoaPods

The native `CxAudioSDK` pod contains only the iOS SDK. Add the released version
to the application's `Podfile`:

```ruby
pod 'CxAudioSDK', '~> 0.1'
```

## Placements

The iOS SDK exposes three placement types:

- Background audio.
- Sticky player.
- Full screen interstitial.

All placements fetch a VAST tag URL, resolve up to five wrappers, and select the best usable creative for the placement.

## Request Setup

```swift
import CxAudioSDK

let consumableRequest = try ConsumableAdRequest(
    placementHash: "YOUR_PLACEMENT_HASH",
    appStoreURL: "https://apps.apple.com/app/id123",
    appName: "Example Game",
    appBundle: "com.example.game"
)

let request = try VastRequest(
    consumableAdRequest: consumableRequest,
    placement: .backgroundAudio,
    timeoutInterval: 8
)
```

Always pass the placement that will consume the request. The SDK automatically maps background audio, sticky player, and fullscreen interstitial to the internal Consumable placement codes `1`, `2`, and `3`. Code `3` requests a video-compatible response for the interstitial renderer. This protocol value is internal and is not publisher-configurable. Omitting `placement` remains backward-compatible and defaults to `.backgroundAudio`.

Normal integrations should not manually parse consent strings or retrieve
advertising identifiers. See [Privacy and consent](https://github.com/GiftConnect/cx-audio-sdk/blob/0.1.3/docs/ADVANCED_USAGE.md#privacy-and-consent)
for the current production-readiness constraints.

Use the raw VAST URL initializer for QA tags or non-Consumable ad servers:

```swift
let request = try VastRequest(
    vastTagURLString: "https://ads.example.com/vast.xml",
    timeoutInterval: 8
)
```

## Privacy and Consent

The host application owns its CMP, privacy notices, age-related declarations,
location permission, and any App Tracking Transparency prompt. The SDK does not
display consent UI.

The SDK reads supported standard privacy signals, resolves them conservatively,
and sanitizes outbound requests. COPPA and underage restrictions override every
other signal. Missing, malformed, stale, unresolved, or conflicting signals
remain restrictive.

Explicit privacy and advertising-identifier fields are limited to custom CMP
integrations, controlled testing, and approved enterprise cases. Do not use
fingerprinting, IDFV, hashed email, or another persistent fallback identifier
for advertising.

## SDK Identity, User Agent, and IP Parameters

When a `ConsumableAdRequest` is converted into a `VastRequest`, the SDK automatically supplies its internal user agent as the `u` query parameter.

Structured requests also include `sdk=Consumable Audio` and the current SDK
build version in `sdkv`. Both values are percent-encoded by the request
builder. An approved branded or mirrored distribution can override the SDK
name without changing the SDK version:

```swift
let consumableRequest = try ConsumableAdRequest(
    placementHash: "YOUR_PLACEMENT_HASH",
    sdkName: "Radio.com Audio"
)
```

The SDK does not compute the `i` query parameter on device. iOS can expose local network interface addresses, but it cannot reliably know the public/NAT IP seen by an ad server without an external lookup. Prefer server-side inference from the request source address at the Consumable endpoint or proxy. Pass `ipAddress` only when the host app has a trusted server-provided value.

## Background Audio

```swift
final class AudioAdListener: BackgroundAudioListener {
    func backgroundAudioDidStart(_ handle: BackgroundAudioHandle) {}
    func backgroundAudioDidComplete() {}
    func backgroundAudioDidStop() {}
    func backgroundAudioDidFail(_ error: Error) {}
}

let listener = AudioAdListener()
let handle = CxBackgroundAudio.play(
    request: request,
    options: BackgroundAudioOptions(volume: 0.2, minDeviceVolume: 0.1),
    listener: listener
)

handle.setVolume(0.1)
handle.stop()
```

The SDK selects an audio `MediaFile` and streams it through `AVPlayer`. `minDeviceVolume` defaults to `0.1`; set it to `0.0` to disable the pre-request device volume gate.

## Sticky Player

```swift
final class StickyAdListener: StickyPlayerListener {
    func stickyPlayerDidLoad(_ handle: StickyPlayerHandle) {}
    func stickyPlayerWasClicked(_ clickThroughURL: URL?) {}
    func stickyPlayerDidDismiss() {}
    func stickyPlayerDidFail(_ error: Error) {}
}

let listener = StickyAdListener()
let handle = CxStickyPlayer.show(
    in: viewController,
    request: request,
    options: StickyPlayerOptions(
        position: .bottom,
        showCloseButton: true,
        volume: 0.2,
        minDeviceVolume: 0.1
    ),
    listener: listener
)

handle.dismiss()
```

The SDK selects the image companion closest to 300x250 and the best audio media file. It downloads the companion, renders it in a top or bottom safe-area overlay, then starts audio playback. The companion keeps its aspect ratio with a dimmed cropped background and centered foreground creative. Companion clicks open through `UIApplication.shared.open`, and dismissing the sticky player stops audio playback.

## Full Screen Interstitial

```swift
final class InterstitialAdListener: FullscreenInterstitialListener {
    func fullscreenInterstitialDidLoad(_ handle: FullscreenInterstitialHandle) {}
    func fullscreenInterstitialWasClicked(_ clickThroughURL: URL?) {}
    func fullscreenInterstitialDidDismiss() {}
    func fullscreenInterstitialDidFail(_ error: Error) {}
}

let listener = InterstitialAdListener()
let handle = CxFullscreenInterstitial.show(
    from: viewController,
    request: request,
    options: FullscreenInterstitialOptions(
        showCloseButton: true,
        autoPlayVideo: true,
        volume: 0.2,
        minDeviceVolume: 0.1
    ),
    listener: listener
)

handle.dismiss()
```

The SDK selects an image or video `MediaFile` for the main area and adds a companion CTA area when a companion image is present. For audio-only or image/slate interstitial responses, audio starts after the overlay is visible. Video creatives use the video player audio track. Dismissing the interstitial stops audio playback.

## Validation

The platform-neutral VAST parser/model files can be typechecked from the repository root:

```sh
swiftc -typecheck ios/Sources/CxAudioSDK/CxAudioRequest.swift ios/Sources/CxAudioSDK/DeviceVolumeMonitor.swift ios/Sources/CxAudioSDK/Privacy.swift ios/Sources/CxAudioSDK/PrivacyEnforcement.swift ios/Sources/CxAudioSDK/PrivacyNetworkClient.swift ios/Sources/CxAudioSDK/VastModels.swift ios/Sources/CxAudioSDK/VastParser.swift ios/Sources/CxAudioSDK/VastTracking.swift
```

Full iOS validation requires full Xcode because the package imports Apple platform frameworks such as `UIKit`, `AVFoundation`, and `AVAudioSession`:

```sh
xcodebuild -scheme CxAudioSDK -destination 'platform=iOS Simulator,name=iPhone 15' build
xcodebuild -workspace samples/flappy-ios/CxFlappyBird.xcworkspace -scheme CxFlappyBird -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
```

Then smoke-test the Flappy Blob placement lifecycle with known-good VAST tags:

- Sticky player renders while the game is idle and dismisses when play starts.
- Full screen interstitial renders after eligible game-over transitions and dismisses cleanly.
