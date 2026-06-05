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

## Background Audio

```csharp
using Cx.Audio.Unity;
using UnityEngine;

public sealed class AudioAds : MonoBehaviour, IBackgroundAudioListener
{
    private BackgroundAudioHandle backgroundAudio;

    public void PlayBackgroundAudio()
    {
        ConsumableAdRequest consumableRequest = new ConsumableAdRequest("b0816e0aa072e446e7737dd92dd34d10")
            .AppName("Example Game")
            .AppBundle("com.example.game")
            .AppStoreUrl("https://play.google.com/store/apps/details?id=com.example.game")
            .LimitAdTracking(false)
            .Gdpr(false)
            .Position("preroll")
            .Delivery(2)
            .MaxDurationSeconds(30)
            .MediaMimes("audio/mpeg", "audio/mp4")
            .CompanionSizes("300x250", "320x50")
            .Protocols("2", "3", "5", "6", "7", "8");

        backgroundAudio = BackgroundAudio.Play(
            new VastRequest(consumableRequest),
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

Use `new VastRequest("https://...")` for QA tags or non-Consumable ad servers. Use `new VastRequest(new ConsumableAdRequest(...))` when the SDK should generate the Consumable API URL.

## Sticky Player

```csharp
using Cx.Audio.Unity;
using UnityEngine;

public sealed class StickyAd : MonoBehaviour, IStickyPlayerListener
{
    private StickyPlayerHandle stickyPlayer;

    public void ShowStickyPlayer()
    {
        stickyPlayer = StickyPlayer.Show(
            new VastRequest("https://ads.example.com/vast.xml"),
            new StickyPlayerOptions
            {
                Position = StickyPlayerPosition.Bottom,
                HeightDp = 72,
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
        interstitial = FullscreenInterstitial.Show(
            new VastRequest("https://ads.example.com/vast.xml"),
            new FullscreenInterstitialOptions
            {
                ShowCloseButton = true,
                CompanionHeightDp = 250,
                CompanionMaxWidthDp = 300,
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
