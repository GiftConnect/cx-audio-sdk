'use strict';

const { NativeEventEmitter, NativeModules, Platform } = require('react-native');

const EVENT_NAME = 'CxAudioEvent';
const NATIVE_MODULE_NAME = 'CxAudioReactNative';
const NativeCxAudio = NativeModules[NATIVE_MODULE_NAME];

const StickyPlayerPosition = Object.freeze({
  TOP: 'top',
  BOTTOM: 'bottom',
});

function getNativeModule() {
  if (Platform.OS !== 'android') {
    throw new Error('cx-audio-sdk React Native bindings currently support Android hosts only.');
  }
  if (!NativeCxAudio) {
    throw new Error(
      'cx-audio-sdk native module is not linked. Rebuild the React Native app after installing the package.'
    );
  }
  return NativeCxAudio;
}

function addCxAudioListener(listener) {
  const nativeModule = getNativeModule();
  return new NativeEventEmitter(nativeModule).addListener(EVENT_NAME, listener);
}

const BackgroundAudio = Object.freeze({
  play(request, options) {
    return getNativeModule().playBackgroundAudio(request, options || {});
  },

  stop(handleId) {
    return getNativeModule().stopBackgroundAudio(handleId);
  },

  setVolume(handleId, volume) {
    return getNativeModule().setBackgroundAudioVolume(handleId, volume);
  },
});

const StickyPlayer = Object.freeze({
  show(request, options) {
    return getNativeModule().showStickyPlayer(request, options || {});
  },

  dismiss(handleId) {
    return getNativeModule().dismissStickyPlayer(handleId);
  },
});

const FullscreenInterstitial = Object.freeze({
  show(request, options) {
    return getNativeModule().showFullscreenInterstitial(request, options || {});
  },

  dismiss(handleId) {
    return getNativeModule().dismissFullscreenInterstitial(handleId);
  },
});

module.exports = {
  addCxAudioListener,
  BackgroundAudio,
  StickyPlayer,
  FullscreenInterstitial,
  StickyPlayerPosition,
};
