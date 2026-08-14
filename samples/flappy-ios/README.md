# Flappy Blob iOS Sample

Flappy Blob is a small host app demonstrating how an iOS game can connect CX ad placements to gameplay state without exposing SDK controls to the player.

## Where the Placement Code Lives

| What to inspect | File |
| --- | --- |
| Placement configuration, `ConsumableAdRequest`, and `VastRequest` construction | [`GameAdController.request(for:)`](CxFlappyBird/GameAdController.swift#L142) |
| Ready-screen sticky placement | [`GameAdController.showReadySticky(in:)`](CxFlappyBird/GameAdController.swift#L16) |
| Post-game fullscreen interstitial | [`GameAdController.showInterstitial(from:onFinished:)`](CxFlappyBird/GameAdController.swift#L46) |
| Sticky and interstitial listener callbacks and handle cleanup | [`GameAdController.swift`](CxFlappyBird/GameAdController.swift) |
| Gameplay-to-ad state policy and frequency limits | [`GameViewController.swift`](CxFlappyBird/GameViewController.swift#L229) |
| Replay lock while an interstitial is active | [`FlappyGameView.isReplayEnabled`](CxFlappyBird/FlappyGameView.swift#L43) |

Start with [`GameAdController.swift`](CxFlappyBird/GameAdController.swift). It contains the actual public SDK calls used by this sample:

```swift
stickyHandle = CxStickyPlayer.show(
    in: viewController,
    request: try request(for: .stickyPlayer),
    options: StickyPlayerOptions(position: .bottom),
    listener: self
)

interstitialHandle = CxFullscreenInterstitial.show(
    from: viewController,
    request: try request(for: .fullscreenInterstitial),
    options: FullscreenInterstitialOptions(),
    listener: self
)
```

The shared request builder converts the publisher placement hash and app metadata into the appropriate request for each placement:

```swift
private func request(for placement: ConsumablePlacement) throws -> VastRequest {
    let consumableRequest = try ConsumableAdRequest(
        placementHash: placementHash,
        limitAdTracking: true,
        appName: "Flappy Blob",
        appBundle: Bundle.main.bundleIdentifier,
        gdpr: false,
        contentGenre: ["game"],
        contentTitle: "Flappy Blob"
    )
    return try VastRequest(
        consumableAdRequest: consumableRequest,
        placement: placement,
        timeoutInterval: 8
    )
}
```

Replace `YOUR_PLACEMENT_HASH` for the `CXPlacementHash` key in
[`Info.plist`](CxFlappyBird/Info.plist) with the value supplied by CX. The
sample refuses to request an ad while the value is blank or still the
placeholder.

The `placement` argument is the only placement-routing choice made by the sample. The SDK converts it to its internal Consumable request code automatically: background audio is `1`, sticky audio is `2`, and fullscreen interstitial is `3`. The interstitial code asks the Consumable endpoint for a video-compatible response. The sample does not construct or expose that protocol parameter itself.

## State-Driven Ad Flow

| Game state or event | Ad behavior |
| --- | --- |
| Ready for 8 seconds | Requests one bottom sticky placement per app session. |
| Play begins | Cancels the pending sticky request and dismisses an active sticky placement. |
| Active run | Does not request or display a new ad. |
| First game over | Does not show an interstitial. |
| Later game over | May show an interstitial after a new high score, every third round, or three minutes without an interstitial. |
| Interstitial active | Blocks replay until dismissal, completion, or failure. |
| View disappears | Cancels the ready timer and dismisses active placements. |

Interstitial requests are separated by at least 90 seconds. Sticky and interstitial handles are owned by one controller so the two placements cannot overlap.

## Run the Sample

Install the native SDK through the local `CxAudioSDK` CocoaPod before opening
the workspace:

```sh
cd samples/flappy-ios
pod install
```

The sample's [`Podfile`](Podfile) references the repository root, so it tests
the native pod without requiring a published CocoaPods release or React
Native. Open [`CxFlappyBird.xcworkspace`](CxFlappyBird.xcworkspace) in Xcode
and run the `CxFlappyBird` scheme, or build from the repository root after
running `pod install`:

```sh
xcodebuild \
  -workspace samples/flappy-ios/CxFlappyBird.xcworkspace \
  -scheme CxFlappyBird \
  -destination 'generic/platform=iOS Simulator' \
  build CODE_SIGNING_ALLOWED=NO
```

## Production Privacy Note

This sample requests App Tracking Transparency authorization and uses IDFA only
when authorization succeeds and the identifier is nonzero. Otherwise it sets
`limitAdTracking: true` and omits the identifier. The explicit regional privacy
override is compiled only into debug builds for controlled local QA; release
builds rely on standard host-provided privacy signals. Ad loading waits until
the ATT choice resolves so the first request does not incorrectly go out as a
limited-tracking request while the prompt is still visible.

iOS Simulator commonly returns an all-zero IDFA. In that case the sample
correctly remains in limited-tracking mode, and a demand source that has no
non-personalized inventory can still return an empty VAST response. Use a real
device with ATT allowed to validate the personalized test-ad path.

A production host app must collect consent through its own CMP or privacy UI
and pass only privacy and identifier values permitted by the resolved state. Do
not copy the sample's debug-only override into production. See
[Privacy and consent](../../docs/PRIVACY.md).
