'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const { createConsumableVastRequest } = require('../src/consumableRequest');

function query(request) {
  return new URL(request.vastTagUrl).searchParams;
}

test('defaults structured requests to background audio sdkpt', () => {
  const request = createConsumableVastRequest({ placementHash: 'abc123' });

  assert.equal(query(request).get('sdkpt'), '1');
  assert.equal(query(request).get('sdk'), 'Consumable Audio');
  assert.equal(query(request).get('sdkv'), '0.1.1');
});

test('encodes a configured SDK name and the automatic SDK version', () => {
  const request = createConsumableVastRequest({
    placementHash: 'abc123',
    sdkName: 'Radio.com Audio / SDK',
  });

  assert.match(request.vastTagUrl, /sdk=Radio.com%20Audio%20%2F%20SDK/);
  assert.equal(query(request).get('sdk'), 'Radio.com Audio / SDK');
  assert.equal(query(request).get('sdkv'), '0.1.1');
});

test('maps each explicit placement to its internal sdkpt code', () => {
  const mappings = [
    ['backgroundAudio', '1'],
    ['stickyPlayer', '2'],
    ['fullscreenInterstitial', '3'],
  ];

  for (const [placement, code] of mappings) {
    const request = createConsumableVastRequest({ placementHash: 'abc123', placement });
    assert.equal(query(request).get('sdkpt'), code);
  }
});

test('rejects an unsupported placement instead of accepting a raw sdkpt', () => {
  assert.throws(
    () => createConsumableVastRequest({ placementHash: 'abc123', placement: 'custom' }),
    /Unsupported placement/
  );
});

test('preserves network options without exposing sdkpt as configuration', () => {
  const request = createConsumableVastRequest(
    { placementHash: 'abc123', placement: 'stickyPlayer' },
    { connectTimeoutMs: 1200 }
  );

  assert.equal(request.connectTimeoutMs, 1200);
  assert.equal(query(request).get('sdkpt'), '2');
});

test('uses the native platform SDK user agent', () => {
  const android = new URL(
    createConsumableVastRequest(
      { placementHash: 'placement' },
      undefined,
      'android'
    ).vastTagUrl
  );
  const ios = new URL(
    createConsumableVastRequest(
      { placementHash: 'placement' },
      undefined,
      'ios'
    ).vastTagUrl
  );

  assert.equal(android.searchParams.get('u'), 'cx-audio-sdk-android/0.1');
  assert.equal(ios.searchParams.get('u'), 'cx-audio-sdk-ios/0.1');
});
