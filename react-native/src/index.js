'use strict';

const { NativeEventEmitter, NativeModules, Platform } = require('react-native');
const {
  createConsumableVastRequest: createConsumableVastRequestForPlatform,
} = require('./consumableRequest');

const EVENT_NAME = 'CxAudioEvent';
const NATIVE_MODULE_NAME = 'CxAudioReactNative';
const NativeCxAudio = NativeModules[NATIVE_MODULE_NAME];

const StickyPlayerPosition = Object.freeze({
  TOP: 'top',
  BOTTOM: 'bottom',
});

const AgeTreatment = Object.freeze({
  UNSPECIFIED: 'unspecified',
  GENERAL_AUDIENCE: 'generalAudience',
  CHILD_DIRECTED: 'childDirected',
  UNDER_AGE_OF_CONSENT: 'underAgeOfConsent',
});

function getNativeModule() {
  if (Platform.OS !== 'android' && Platform.OS !== 'ios') {
    throw new Error('cx-audio-sdk React Native bindings support Android and iOS hosts.');
  }
  if (!NativeCxAudio) {
    throw new Error(
      'cx-audio-sdk native module is not linked. Rebuild the React Native app after installing the package.'
    );
  }
  return NativeCxAudio;
}

function createConsumableVastRequest(request, networkOptions) {
  return createConsumableVastRequestForPlatform(
    request,
    networkOptions,
    Platform.OS
  );
}

function addCxAudioListener(listener) {
  const nativeModule = getNativeModule();
  return new NativeEventEmitter(nativeModule).addListener(EVENT_NAME, listener);
}

function initialize(configuration) {
  return getNativeModule().initialize(configuration || {});
}

function updatePrivacyOverride(privacyOverride) {
  return getNativeModule().updatePrivacyOverride(privacyOverride || null);
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
  createConsumableVastRequest,
  initialize,
  updatePrivacyOverride,
  BackgroundAudio,
  StickyPlayer,
  FullscreenInterstitial,
  StickyPlayerPosition,
  AgeTreatment,
};
