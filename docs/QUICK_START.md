# CX Audio SDK quick start

This guide gets one background audio placement running with the normal
Consumable request flow. For sticky and fullscreen placements, request
metadata, options, lifecycle details, and QA tags, see
[Advanced usage](ADVANCED_USAGE.md).

## Before you begin

You need:

- A placement hash issued by CX.
- A supported host: Android 6.0/API 23+, iOS 15+, React Native 0.71+ on
  Android or iOS, or Unity 2021.3+ with an Android player build.
- The host application's consent flow completed before it requests an ad.

The host application owns its CMP, privacy prompts, ATT prompt, age-related
declarations, location permission, and app-level audio policy. The SDK does
not display consent UI.

Initialize the SDK before the first ad request. Initialization reads supported
standard privacy signals and starts from a restrictive state until resolution
completes. See [Privacy and consent](ADVANCED_USAGE.md#privacy-and-consent) for
age-related declarations and approved custom-CMP overrides.

## Install the SDK

- **Android:** after the first Maven Central release, add
  `io.github.giftconnect:cx-audio-sdk:0.1.0` from `mavenCentral()`. Until that
  release is available, add the `:android` module to the Gradle build or build
  an AAR with `./gradlew :android:assemble` and add it to the app.
- **iOS:** add the repository root as a Swift Package and select the
  `CxAudioSDK` product, or install the native `CxAudioSDK` CocoaPod. Until the
  pod is published, reference the repository checkout with `:path`.
- **React Native:** add the repository/package as a dependency, then rebuild
  the Android or iOS app so the native module is linked. React Native 0.71+
  is supported. iOS autolinking uses `cx-audio-sdk.podspec`, whose bridge
  depends on the separate native `CxAudioSDK` pod. Until the native pod is
  published, add `pod 'CxAudioSDK', :path => '../node_modules/cx-audio-sdk'`
  to the host Podfile.
- **Unity:** add the `unity` directory as a local Unity package, build the
  Android AAR with `./gradlew :android:assembleRelease`, and place
  `android/build/outputs/aar/android-release.aar` under
  `Assets/Plugins/Android/`.

Android release packaging and CocoaPods Trunk instructions are documented in
[Publishing](PUBLISHING.md).

## Android

Create a structured request, play it, retain the handle, and clear it on every
terminal callback:

```java
import android.app.Activity;
import android.os.Bundle;

import com.cx.audio.android.CxAudio;
import com.cx.audio.android.AndroidBackgroundAudio;
import com.cx.audio.android.BackgroundAudioHandle;
import com.cx.audio.android.BackgroundAudioListener;
import com.cx.audio.android.BackgroundAudioOptions;
import com.cx.audio.android.BackgroundAudioRequest;
import com.cx.audio.android.ConsumableAdRequest;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CxAudio.initialize(this);
    }

    private BackgroundAudioHandle backgroundAd;

    private void playAudioAd() {
        ConsumableAdRequest adRequest = ConsumableAdRequest
                .builder("YOUR_PLACEMENT_HASH")
                .build();

        BackgroundAudioRequest request = BackgroundAudioRequest
                .builder(this, adRequest)
                .build();

        backgroundAd = AndroidBackgroundAudio.play(
                this,
                request,
                BackgroundAudioOptions.builder().build(),
                new BackgroundAudioListener() {
                    @Override
                    public void onBackgroundAudioStarted(BackgroundAudioHandle handle) {
                        // Playback started.
                    }

                    @Override
                    public void onBackgroundAudioCompleted() {
                        backgroundAd = null;
                    }

                    @Override
                    public void onBackgroundAudioStopped() {
                        backgroundAd = null;
                    }

                    @Override
                    public void onBackgroundAudioFailed(Throwable error) {
                        backgroundAd = null;
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (backgroundAd != null) {
            backgroundAd.stop();
            backgroundAd = null;
        }
        super.onDestroy();
    }
}
```

Passing the `Activity` to `BackgroundAudioRequest.builder(...)` lets the SDK
add the application name, package identifier, locale, and browser user agent
when they are available.

## iOS

Call UI placements from the main thread. Asynchronous iOS load and render
callbacks are delivered on the main queue.

```swift
import CxAudioSDK

final class AudioAdController: BackgroundAudioListener {
    private var backgroundAd: BackgroundAudioHandle?

    init() {
        CxAudio.initialize()
    }

    func playAudioAd() throws {
        let adRequest = try ConsumableAdRequest(
            placementHash: "YOUR_PLACEMENT_HASH"
        )
        let request = try VastRequest(
            consumableAdRequest: adRequest,
            placement: .backgroundAudio
        )

        backgroundAd = CxBackgroundAudio.play(
            request: request,
            listener: self
        )
    }

    func stopAudioAd() {
        backgroundAd?.stop()
        backgroundAd = nil
    }

    func backgroundAudioDidStart(_ handle: BackgroundAudioHandle) {}

    func backgroundAudioDidComplete() {
        backgroundAd = nil
    }

    func backgroundAudioDidStop() {
        backgroundAd = nil
    }

    func backgroundAudioDidFail(_ error: Error) {
        backgroundAd = nil
    }
}
```

The iOS request adds the bundle identifier, display name, locale, and SDK user
agent when available.

## React Native

React Native bindings support Android and iOS hosts. Subscribe before starting
a placement, match events by `handleId`, and remove the subscription when the
owning component unmounts.

```js
import {
  addCxAudioListener,
  BackgroundAudio,
  createConsumableVastRequest,
} from 'cx-audio-sdk';

let backgroundAdHandle;

const subscription = addCxAudioListener((event) => {
  const terminal = ['completed', 'stopped', 'failed'].includes(event.type);
  if (
    event.placement === 'backgroundAudio' &&
    event.handleId === backgroundAdHandle &&
    terminal
  ) {
    backgroundAdHandle = undefined;
  }
});

export async function playAudioAd() {
  const request = createConsumableVastRequest({
    placementHash: 'YOUR_PLACEMENT_HASH',
    placement: 'backgroundAudio',
  });
  backgroundAdHandle = await BackgroundAudio.play(request);
}

export async function disposeAudioAds() {
  subscription.remove();
  if (backgroundAdHandle) {
    const handle = backgroundAdHandle;
    backgroundAdHandle = undefined;
    await BackgroundAudio.stop(handle);
  }
}
```

## Unity

Unity bindings currently run in Android player builds only. Retain the native
handle for the placement lifetime and dispose it after stopping or receiving a
terminal callback.

```csharp
using Cx.Audio.Unity;
using UnityEngine;

public sealed class AudioAds : MonoBehaviour, IBackgroundAudioListener
{
    private BackgroundAudioHandle backgroundAd;

    public void PlayAudioAd()
    {
        ConsumableAdRequest adRequest =
            new ConsumableAdRequest("YOUR_PLACEMENT_HASH");
        VastRequest request = new VastRequest(
            adRequest,
            ConsumablePlacement.BackgroundAudio);

        backgroundAd = BackgroundAudio.Play(
            request,
            new BackgroundAudioOptions(),
            this);
    }

    private void OnDestroy()
    {
        if (backgroundAd != null)
        {
            backgroundAd.Stop();
            ReleaseHandle();
        }
    }

    public void OnBackgroundAudioStarted(BackgroundAudioHandle handle) {}
    public void OnBackgroundAudioCompleted() { ReleaseHandle(); }
    public void OnBackgroundAudioStopped() { ReleaseHandle(); }
    public void OnBackgroundAudioFailed(CxAudioError error) { ReleaseHandle(); }

    private void ReleaseHandle()
    {
        if (backgroundAd == null)
        {
            return;
        }
        backgroundAd.Dispose();
        backgroundAd = null;
    }
}
```

## Next steps

- Choose a placement and configure its supported options in
  [Advanced usage](ADVANCED_USAGE.md).
- Add app, content, category, or brand-safety metadata with the
  [structured request fields](ADVANCED_USAGE.md#structured-request-fields).
- Review the [lifecycle and cleanup rules](ADVANCED_USAGE.md#lifecycle-and-cleanup).
- Run the black-box samples under [`samples`](../samples).
