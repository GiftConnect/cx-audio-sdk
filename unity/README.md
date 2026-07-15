# CX Audio SDK for Unity

Unity package source for the CX audio ad SDK.

The Unity bindings expose the same three placement types as Android:

- Background audio
- Sticky player
- Fullscreen interstitial

This package currently supports Android player builds. It delegates to the native Android SDK through Unity's Android JNI bridge, so the Android AAR must be included in the Unity project for Android builds.

## Installation

Add this package with Unity Package Manager from the repository's `unity` folder.

Build the Android SDK AAR from the repository root:

```sh
./gradlew :android:assembleRelease
```

Then include `android/build/outputs/aar/android-release.aar` as an Android plugin in the Unity project, for example under `Assets/Plugins/Android/`.

## Advertising Identifiers

The Unity package does not automatically collect advertising identifiers. For Android player builds, the host game is responsible for retrieving the Google advertising ID when available and permitted, declaring `com.google.android.gms.permission.AD_ID` for Android 13/API 33 or newer targets, and applying any required consent or privacy controls.

Pass the advertising ID as both `Gadid(...)` and `Ifa(...)` when the ad endpoint expects OpenRTB-style IFA. If the ID is deleted, limited, unavailable, or all zeros, omit `Gadid(...)` and `Ifa(...)` and call `LimitAdTracking(true)`. Do not fall back to fingerprinting or another persistent device identifier for advertising.

## User Agent and IP Parameters

When a `ConsumableAdRequest` is converted into a `VastRequest`, the Unity wrapper automatically supplies the SDK user agent as the `u` query parameter.

The package does not compute the `i` query parameter on device. Android can expose local network interface addresses, but it cannot reliably know the public/NAT IP seen by an ad server without an external lookup. Prefer server-side inference from the request source address at the Consumable endpoint or proxy. Pass `IpAddress(...)` only when the host game has a trusted server-provided value.

## Placement Request Routing

Pass the matching `ConsumablePlacement` when constructing a request. The wrapper automatically maps background audio, sticky player, and fullscreen interstitial to the internal Consumable placement codes `1`, `2`, and `3`. Code `3` requests a video-compatible response for the interstitial renderer. The code is an internal protocol detail and is not publisher-configurable. The one-argument structured constructor defaults to background audio for compatibility; raw VAST URL constructors remain unchanged.

## Background Audio

```csharp
using Cx.Audio.Unity;
using UnityEngine;

public sealed class AudioAds : MonoBehaviour, IBackgroundAudioListener
{
    private BackgroundAudioHandle backgroundAudio;

    public void PlayBackgroundAudio()
    {
        string advertisingId = null; // Host game supplies GAID/AAID when available and permitted.
        bool limitAdTracking = string.IsNullOrEmpty(advertisingId);

        ConsumableAdRequest consumableRequest = new ConsumableAdRequest("b0816e0aa072e446e7737dd92dd34d10")
            .AppName("Example Game")
            .AppBundle("com.example.game")
            .AppStoreUrl("https://play.google.com/store/apps/details?id=com.example.game")
            .Gadid(advertisingId)
            .Ifa(advertisingId)
            .LimitAdTracking(limitAdTracking)
            .Gdpr(false);

        backgroundAudio = BackgroundAudio.Play(
            new VastRequest(consumableRequest, ConsumablePlacement.BackgroundAudio),
            new BackgroundAudioOptions
            {
                Volume = 0.2f,
                MinDeviceVolume = 0.1f
            },
            this);
    }

    public void StopBackgroundAudio()
    {
        if (backgroundAudio != null)
        {
            backgroundAudio.Stop();
            backgroundAudio.Dispose();
            backgroundAudio = null;
        }
    }

    public void OnBackgroundAudioStarted(BackgroundAudioHandle handle) {}
    public void OnBackgroundAudioCompleted() {}
    public void OnBackgroundAudioStopped() {}
    public void OnBackgroundAudioFailed(CxAudioError error) {}
}
```

Use `new VastRequest("https://...")` for QA tags or non-Consumable ad servers. Use `new VastRequest(new ConsumableAdRequest(...), ConsumablePlacement.BackgroundAudio)` when the SDK should generate the Consumable API URL.

## Sticky Player

```csharp
using Cx.Audio.Unity;
using UnityEngine;

public sealed class StickyAd : MonoBehaviour, IStickyPlayerListener
{
    private StickyPlayerHandle stickyPlayer;

    public void ShowStickyPlayer()
    {
        ConsumableAdRequest request = new ConsumableAdRequest("YOUR_PLACEMENT_HASH");
        stickyPlayer = StickyPlayer.Show(
            new VastRequest(request, ConsumablePlacement.StickyPlayer),
            new StickyPlayerOptions
            {
                Position = StickyPlayerPosition.Bottom,
                ShowCloseButton = true,
                Volume = 0.2f,
                MinDeviceVolume = 0.1f
            },
            this);
    }

    public void DismissStickyPlayer()
    {
        if (stickyPlayer != null)
        {
            stickyPlayer.Dismiss();
            stickyPlayer.Dispose();
            stickyPlayer = null;
        }
    }

    public void OnStickyPlayerLoaded(StickyPlayerHandle handle) {}
    public void OnStickyPlayerAudioStarted(StickyPlayerHandle handle) {}
    public void OnStickyPlayerAudioCompleted() {}
    public void OnStickyPlayerClicked(string clickThroughUrl) {}
    public void OnStickyPlayerDismissed() {}
    public void OnStickyPlayerFailed(CxAudioError error) {}
}
```

## Fullscreen Interstitial

```csharp
using Cx.Audio.Unity;
using UnityEngine;

public sealed class InterstitialAd : MonoBehaviour, IFullscreenInterstitialListener
{
    private FullscreenInterstitialHandle interstitial;

    public void ShowInterstitial()
    {
        ConsumableAdRequest request = new ConsumableAdRequest("YOUR_PLACEMENT_HASH");
        interstitial = FullscreenInterstitial.Show(
            new VastRequest(request, ConsumablePlacement.FullscreenInterstitial),
            new FullscreenInterstitialOptions
            {
                ShowCloseButton = true,
                AutoPlayVideo = true,
                Volume = 0.2f,
                MinDeviceVolume = 0.1f
            },
            this);
    }

    public void DismissInterstitial()
    {
        if (interstitial != null)
        {
            interstitial.Dismiss();
            interstitial.Dispose();
            interstitial = null;
        }
    }

    public void OnFullscreenInterstitialLoaded(FullscreenInterstitialHandle handle) {}
    public void OnFullscreenInterstitialAudioStarted(FullscreenInterstitialHandle handle) {}
    public void OnFullscreenInterstitialAudioCompleted() {}
    public void OnFullscreenInterstitialClicked(string clickThroughUrl) {}
    public void OnFullscreenInterstitialDismissed() {}
    public void OnFullscreenInterstitialFailed(CxAudioError error) {}
}
```

## Validation

Validate the package in a Unity project with Android Build Support installed. Build an Android player and smoke-test with known-good VAST tags:

- Background audio starts, stops, completes, and applies volume changes.
- Sticky player renders at top and bottom, dismisses, and opens companion click-through URLs.
- Fullscreen interstitial renders image or video media, shows the companion area when present, dismisses, and opens companion click-through URLs.
