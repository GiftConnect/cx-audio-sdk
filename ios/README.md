# CxAudioSDK iOS

Native iOS implementation for the CX audio ad SDK.

## Requirements

- iOS 13 or newer.
- Xcode 15 or newer.
- Swift 5.9 or newer through Xcode.
- `Foundation`, `UIKit`, and `AVFoundation`.

## Installation

Add the Swift package from the repository root:

1. In Xcode, choose **File > Add Package Dependencies**.
2. Select this repository URL or local package checkout.
3. Add the `CxAudioSDK` product to the host iOS app target.

## Placements

The iOS SDK exposes the same placement types as Android:

- Background audio.
- Sticky player.
- Full screen interstitial.

All placements fetch a VAST tag URL, resolve up to five wrappers, and select the best usable creative for the placement.

## Request Setup

```swift
import CxAudioSDK

let request = try VastRequest(
    vastTagURLString: "https://ads.example.com/vast.xml",
    timeoutInterval: 8,
    userAgent: "my-ios-app/1.0"
)
```

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
        height: 72,
        horizontalPadding: 12,
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
        companionHeight: 250,
        companionMaxWidth: 300,
        autoPlayVideo: true
    ),
    listener: listener
)

handle.dismiss()
```

The SDK selects an image or video `MediaFile` for the main area and adds a companion CTA area when a companion image is present.

## Validation

The platform-neutral VAST parser/model files can be typechecked from the repository root:

```sh
swiftc -typecheck ios/Sources/CxAudioSDK/CxAudioRequest.swift ios/Sources/CxAudioSDK/DeviceVolumeMonitor.swift ios/Sources/CxAudioSDK/VastParser.swift ios/Sources/CxAudioSDK/VastModels.swift
```

Full iOS validation requires full Xcode because the package imports Apple platform frameworks such as `UIKit`, `AVFoundation`, and `AVAudioSession`:

```sh
xcodebuild -scheme CxAudioSDK -destination 'platform=iOS Simulator,name=iPhone 15' build
xcodebuild -project samples/ios/CxAudioSample.xcodeproj -scheme CxAudioSample -destination 'platform=iOS Simulator,name=iPhone 15' build
```

Then smoke-test all three placements in a host app with known-good VAST tags:

- Background audio starts, stops, completes, and applies volume changes.
- Sticky player renders at top and bottom, dismisses, and opens companion click-through URLs.
- Full screen interstitial renders image or video media, dismisses, and opens companion click-through URLs.
