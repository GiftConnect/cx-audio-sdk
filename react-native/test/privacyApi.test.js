'use strict';

const assert = require('node:assert/strict');
const Module = require('node:module');
const test = require('node:test');

const calls = [];
const nativeModule = {
  initialize(configuration) {
    calls.push(['initialize', configuration]);
    return Promise.resolve();
  },
  updatePrivacyOverride(privacyOverride) {
    calls.push(['updatePrivacyOverride', privacyOverride]);
    return Promise.resolve();
  },
};

const originalLoad = Module._load;
Module._load = function mockReactNative(request, parent, isMain) {
  if (request === 'react-native') {
    return {
      NativeEventEmitter: class NativeEventEmitter {},
      NativeModules: { CxAudioReactNative: nativeModule },
      Platform: { OS: 'android' },
    };
  }
  return originalLoad.call(this, request, parent, isMain);
};

const sdk = require('../src/index');
Module._load = originalLoad;

test.beforeEach(() => {
  calls.length = 0;
});

test('initializes native privacy with typed host configuration', async () => {
  const configuration = {
    ageTreatment: sdk.AgeTreatment.GENERAL_AUDIENCE,
    privacyOverride: {
      gdprApplies: false,
      usPrivacyOptOut: false,
      personalizedAdsAllowed: true,
    },
  };

  await sdk.initialize(configuration);

  assert.deepEqual(calls, [['initialize', configuration]]);
});

test('uses conservative defaults and allows clearing an override', async () => {
  await sdk.initialize();
  await sdk.updatePrivacyOverride(null);

  assert.deepEqual(calls, [
    ['initialize', {}],
    ['updatePrivacyOverride', null],
  ]);
});
