'use strict';

const CONSUMABLE_ENDPOINT = 'https://e.serverbid.com/mpb/get';
const DEFAULT_SDK_NAME = 'Consumable Audio';
const SDK_VERSION = require('../../package.json').version;
const DEFAULT_USER_AGENTS = Object.freeze({
  android: 'cx-audio-sdk-android/0.1',
  ios: 'cx-audio-sdk-ios/0.1',
});

function createConsumableVastRequest(request, networkOptions, platform = 'android') {
  if (!request || !request.placementHash || !String(request.placementHash).trim()) {
    throw new Error('placementHash must not be blank');
  }

  const placement = request.placement || 'backgroundAudio';
  const params = {
    s: request.placementHash,
    rt: 'vast',
    secure: '1',
    cb: defaultCacheBuster(),
  };

  put(params, 'i', request.ipAddress);
  put(params, 'locale', request.locale);
  put(params, 'sdk', request.sdkName);
  putList(params, 'cat', request.contentCategories);
  putList(params, 'bcat', request.blockedCategories);
  putList(params, 'badv', request.blockedAdvertisers);
  putBool(params, 'lmt', request.limitAdTracking);
  putBool(params, 'dnt', request.doNotTrack);
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
  put(params, 'c_id', request.contentId);
  putList(params, 'c_genre', request.contentGenre);
  put(params, 'c_title', request.contentTitle);
  put(params, 'c_dur', request.contentDurationSeconds);
  put(params, 'c_url', request.contentUrl);
  applyAutomaticParameters(
    params,
    placement,
    DEFAULT_USER_AGENTS[platform] || DEFAULT_USER_AGENTS.android
  );

  return {
    ...(networkOptions || {}),
    vastTagUrl: buildUrl(CONSUMABLE_ENDPOINT, params),
  };
}

function applyAutomaticParameters(params, placement, userAgentParameter) {
  putAutomatic(params, 'u', userAgentParameter);
  putAutomatic(params, 'sdk', DEFAULT_SDK_NAME);
  putAutomatic(params, 'sdkv', SDK_VERSION);
  putAutomatic(params, 'locale', getLocale());
  putAutomatic(params, 'v_delivery', '2');
  putAutomatic(params, 'v_delay', '-1');
  putAutomatic(params, 'v_pm', '1');
  putAutomatic(params, 'v_max_dur', '90');
  putAutomatic(
    params,
    'v_mimes',
    'audio/mpeg,audio/mp4,audio/aac,audio/ogg,audio/wav,audio/x-wav,video/mp4,image/jpeg,image/png,image/gif,image/webp'
  );
  putAutomatic(params, 'v_cmp_types', '1');
  putAutomatic(params, 'v_cmp_mimes', 'image/jpeg,image/png,image/gif,image/webp');
  putAutomatic(params, 'v_cmp_sizes', '300x250');
  putAutomatic(params, 'v_protocols', '2,3,5,6,7,8');
  putAutomatic(params, 'c_audio_feed_type', '6');

  if (placement === 'fullscreenInterstitial') {
    putAutomatic(params, 'sdkpt', '3');
    putAutomatic(params, 'v_plcmt', '3');
    putAutomatic(params, 'v_linearity', '1');
    putAutomatic(params, 'v_min_bitrate', '64');
    putAutomatic(params, 'v_max_bitrate', '2500');
  } else if (placement === 'stickyPlayer') {
    putAutomatic(params, 'sdkpt', '2');
    putAutomatic(params, 'v_plcmt', '2');
    putAutomatic(params, 'v_linearity', '2');
    putAutomatic(params, 'v_min_bitrate', '64');
    putAutomatic(params, 'v_max_bitrate', '320');
  } else if (placement === 'backgroundAudio') {
    putAutomatic(params, 'sdkpt', '1');
    putAutomatic(params, 'v_plcmt', '2');
    putAutomatic(params, 'v_linearity', '2');
    putAutomatic(params, 'v_min_bitrate', '64');
    putAutomatic(params, 'v_max_bitrate', '320');
  } else {
    throw new Error(`Unsupported placement: ${placement}`);
  }
}

function putAutomatic(params, key, value) {
  if (params[key] == null && value != null && String(value).trim() !== '') {
    params[key] = value;
  }
}

function getLocale() {
  if (typeof Intl !== 'undefined' && Intl.DateTimeFormat) {
    const locale = Intl.DateTimeFormat().resolvedOptions().locale;
    if (locale) {
      return locale;
    }
  }
  return null;
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

function defaultCacheBuster() {
  return `${Date.now()}`;
}

module.exports = { createConsumableVastRequest };
