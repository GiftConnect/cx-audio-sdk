'use strict';

const { NativeEventEmitter, NativeModules, Platform } = require('react-native');

const EVENT_NAME = 'CxAudioEvent';
const NATIVE_MODULE_NAME = 'CxAudioReactNative';
const NativeCxAudio = NativeModules[NATIVE_MODULE_NAME];

const StickyPlayerPosition = Object.freeze({
  TOP: 'top',
  BOTTOM: 'bottom',
});

const CONSUMABLE_ENDPOINT = 'https://2.e.serverbid.com/mpb/get';

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

function createConsumableVastRequest(request, networkOptions) {
  if (!request || !request.placementHash || !String(request.placementHash).trim()) {
    throw new Error('placementHash must not be blank');
  }

  const params = {
    s: request.placementHash,
    rt: request.responseType || 'vast',
    secure: request.secure === false ? '0' : '1',
    cb: request.cacheBuster || randomCacheBuster(),
  };

  put(params, 'u', request.userAgentParameter);
  put(params, 'i', request.ipAddress);
  put(params, 'locale', request.locale);
  putList(params, 'cat', request.contentCategories);
  putList(params, 'bcat', request.blockedCategories);
  putList(params, 'badv', request.blockedAdvertisers);
  putBool(params, 'lmt', request.limitAdTracking);
  putBool(params, 'dnt', request.doNotTrack);
  put(params, 'schain', request.supplyChain);
  put(params, 'appstoreurl', request.appStoreUrl);
  put(params, 'appname', request.appName);
  put(params, 'appversion', request.appVersion);
  put(params, 'appbundle', request.appBundle);
  put(params, 'idfa', request.idfa);
  put(params, 'aid', request.aid);
  put(params, 'gadid', request.gadid);
  put(params, 'ifa', request.ifa);
  putList(params, 'kw', request.keywords);
  put(params, 'long', request.longitude);
  put(params, 'lat', request.latitude);
  putBool(params, 'coppa', request.coppa);
  putBool(params, 'gdpr', request.gdpr);
  put(params, 'gdpr_consent', request.gdprConsent);
  put(params, 'us_privacy', request.usPrivacy);
  put(params, 'gpp', request.gpp);
  putList(params, 'gpp_sid', request.gppSectionIds);
  put(params, 'v_w', request.playerWidth);
  put(params, 'v_h', request.playerHeight);
  put(params, 'v_plcmt', request.videoPlacement);
  put(params, 'v_delivery', request.delivery);
  if (request.startDelaySeconds != null) {
    put(params, 'v_delay', request.startDelaySeconds);
  } else {
    put(params, 'v_position', request.position);
  }
  put(params, 'v_max_dur', request.maxDurationSeconds);
  putList(params, 'v_mimes', request.mediaMimes);
  putBool(params, 'v_skip', request.skippable);
  put(params, 'v_pm', request.playbackMethod);
  put(params, 'v_max_bitrate', request.maxBitrateKbps);
  put(params, 'v_min_bitrate', request.minBitrateKbps);
  putList(params, 'v_cmp_types', request.companionTypes);
  putList(params, 'v_cmp_mimes', request.companionMimes);
  putList(params, 'v_cmp_sizes', request.companionSizes);
  putList(params, 'v_protocols', request.protocols);
  put(params, 'c_id', request.contentId);
  putList(params, 'c_genre', request.contentGenre);
  put(params, 'c_title', request.contentTitle);
  put(params, 'c_dur', request.contentDurationSeconds);
  put(params, 'c_url', request.contentUrl);
  put(params, 'c_audio_feed_type', request.contentAudioFeedType);

  Object.keys(request.customParameters || {}).forEach((key) => put(params, key, request.customParameters[key]));

  return {
    ...(networkOptions || {}),
    vastTagUrl: buildUrl(request.endpointUrl || CONSUMABLE_ENDPOINT, params),
  };
}

function put(params, key, value) {
  if (value == null || String(value).trim() === '') {
    return;
  }
  params[key] = String(value);
}

function putBool(params, key, value) {
  if (value == null) {
    return;
  }
  params[key] = value ? '1' : '0';
}

function putList(params, key, value) {
  if (!Array.isArray(value)) {
    return;
  }
  const cleaned = value.map(String).filter((item) => item.trim() !== '');
  if (cleaned.length > 0) {
    params[key] = cleaned.join(',');
  }
}

function buildUrl(endpoint, params) {
  const query = Object.keys(params)
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&');
  return `${endpoint}${endpoint.includes('?') ? '&' : '?'}${query}`;
}

function randomCacheBuster() {
  return `${Date.now()}-${Math.floor(Math.random() * 1000000)}`;
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
  BackgroundAudio,
  StickyPlayer,
  FullscreenInterstitial,
  StickyPlayerPosition,
};
