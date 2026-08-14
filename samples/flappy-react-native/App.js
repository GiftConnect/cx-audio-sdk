import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  AppState,
  Platform,
  Pressable,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {
  getAdvertisingId,
  requestTrackingPermissionsAsync,
} from 'expo-tracking-transparency';
import {
  AgeTreatment,
  addCxAudioListener,
  createConsumableVastRequest,
  FullscreenInterstitial,
  initialize as initializeCxAudio,
  StickyPlayer,
  StickyPlayerPosition,
} from 'cx-audio-sdk';

const READY_STICKY_DELAY_MS = 8_000;
const MIN_INTERSTITIAL_INTERVAL_MS = 90_000;
const LONG_SESSION_INTERVAL_MS = 180_000;
const PIPE_WIDTH = 72;
const PIPE_GAP = 170;
const GROUND_HEIGHT = 74;
const BIRD_RADIUS = 20;
const GRAVITY = 1050;
const PLACEMENT_HASH = process.env.EXPO_PUBLIC_CX_PLACEMENT_HASH || 'YOUR_PLACEMENT_HASH';

function usableAdvertisingId(value) {
  return typeof value === 'string'
    && value.trim().length > 0
    && value.toLowerCase() !== '00000000-0000-0000-0000-000000000000';
}

function buildRequest(placement, advertisingId) {
  if (!PLACEMENT_HASH.trim() || PLACEMENT_HASH === 'YOUR_PLACEMENT_HASH') {
    throw new Error('Set EXPO_PUBLIC_CX_PLACEMENT_HASH before requesting an ad');
  }
  return createConsumableVastRequest(
    {
      placementHash: PLACEMENT_HASH,
      placement,
      limitAdTracking: !advertisingId,
      idfa: Platform.OS === 'ios' ? advertisingId : undefined,
      gadid: Platform.OS === 'android' ? advertisingId : undefined,
      ifa: advertisingId,
      appName: 'Flappy Blob RN',
      appBundle: 'com.cx.audio.samples.flappyreactnative',
      contentGenre: ['game'],
      contentTitle: 'Flappy Blob',
    },
    { readTimeoutMs: 8000 }
  );
}

function useGameAds(unlockReplay) {
  const [status, setStatus] = useState('Ad idle');
  const stickyHandle = useRef(null);
  const interstitialHandle = useRef(null);
  const privacy = useRef({ ready: false, advertisingId: null });
  const pendingSticky = useRef(false);
  const pendingInterstitial = useRef(false);

  const dismissSticky = useCallback(async () => {
    pendingSticky.current = false;
    const handle = stickyHandle.current;
    stickyHandle.current = null;
    if (handle) {
      try { await StickyPlayer.dismiss(handle); } catch (_) { /* Already terminal. */ }
    }
  }, []);

  const showSticky = useCallback(async () => {
    if (stickyHandle.current || interstitialHandle.current) return;
    if (!privacy.current.ready) {
      pendingSticky.current = true;
      setStatus('Waiting for privacy settings');
      return;
    }
    pendingSticky.current = false;
    try {
      setStatus('Ready-screen ad loading');
      stickyHandle.current = await StickyPlayer.show(
        buildRequest('stickyPlayer', privacy.current.advertisingId),
        { position: StickyPlayerPosition.BOTTOM, showCloseButton: true, volume: 0.14, minDeviceVolume: 0 }
      );
    } catch (error) {
      stickyHandle.current = null;
      setStatus(`Ready-screen ad failed: ${error.message}`);
    }
  }, []);

  const showInterstitial = useCallback(async () => {
    await dismissSticky();
    if (interstitialHandle.current) return;
    if (!privacy.current.ready) {
      pendingInterstitial.current = true;
      setStatus('Waiting for privacy settings');
      return;
    }
    pendingInterstitial.current = false;
    try {
      setStatus('Interstitial loading');
      interstitialHandle.current = await FullscreenInterstitial.show(
        buildRequest('fullscreenInterstitial', privacy.current.advertisingId),
        { showCloseButton: true, autoPlayVideo: true, volume: 0.18, minDeviceVolume: 0 }
      );
    } catch (error) {
      interstitialHandle.current = null;
      setStatus(`Interstitial failed: ${error.message}`);
      unlockReplay();
    }
  }, [dismissSticky, unlockReplay]);

  const stop = useCallback(async () => {
    pendingSticky.current = false;
    pendingInterstitial.current = false;
    await dismissSticky();
    const handle = interstitialHandle.current;
    interstitialHandle.current = null;
    if (handle) {
      try { await FullscreenInterstitial.dismiss(handle); } catch (_) { /* Already terminal. */ }
    }
    unlockReplay();
  }, [dismissSticky, unlockReplay]);

  useEffect(() => {
    let active = true;

    const resolvePrivacy = async () => {
      let advertisingId = null;
      try {
        const { status: trackingStatus } = await requestTrackingPermissionsAsync();
        const candidate = trackingStatus === 'granted' ? getAdvertisingId() : null;
        advertisingId = usableAdvertisingId(candidate) ? candidate : null;
      } catch (_) {
        // Missing or unresolved permission and identifier state stays restrictive.
      }

      const configuration = { ageTreatment: AgeTreatment.GENERAL_AUDIENCE };
      if (__DEV__) {
        // QA placement inventory is region-specific. Production apps should rely
        // on CMP/standard signals instead of supplying this regional override.
        configuration.privacyOverride = {
          gdprApplies: false,
          personalizedAdsAllowed: Boolean(advertisingId),
          usPrivacyOptOut: false,
          usPrivacyString: '1YNN',
        };
      }
      await initializeCxAudio(configuration);

      if (!active) return;
      privacy.current = { ready: true, advertisingId };
      if (pendingInterstitial.current) {
        showInterstitial();
      } else if (pendingSticky.current) {
        showSticky();
      } else {
        setStatus(advertisingId ? 'Ad ready' : 'Ad ready (limited tracking)');
      }
    };

    resolvePrivacy().catch((error) => {
      if (!active) return;
      privacy.current = { ready: true, advertisingId: null };
      setStatus(`Privacy initialization failed: ${error.message}`);
      unlockReplay();
    });
    return () => { active = false; };
  }, [showInterstitial, showSticky, unlockReplay]);

  useEffect(() => {
    const subscription = addCxAudioListener((event) => {
      if (event.placement === 'stickyPlayer' && event.handleId === stickyHandle.current) {
        if (event.type === 'loaded') setStatus('Ready-screen ad showing');
        if (event.type === 'started') setStatus('Ready-screen ad playing');
        if (event.type === 'clicked') setStatus('Ready-screen ad clicked');
        if (event.type === 'completed') setStatus('Ready-screen ad complete');
        if (event.type === 'dismissed' || event.type === 'failed') {
          stickyHandle.current = null;
          setStatus(event.type === 'failed' ? `Ready-screen ad failed: ${event.message || 'unknown error'}` : 'Ready-screen ad dismissed');
        }
      }
      if (event.placement === 'fullscreenInterstitial' && event.handleId === interstitialHandle.current) {
        if (event.type === 'loaded') setStatus('Interstitial showing');
        if (event.type === 'started') setStatus('Interstitial playing');
        if (event.type === 'clicked') setStatus('Interstitial clicked');
        if (event.type === 'completed') setStatus('Interstitial complete');
        if (event.type === 'dismissed' || event.type === 'failed') {
          interstitialHandle.current = null;
          setStatus(event.type === 'failed' ? `Interstitial failed: ${event.message || 'unknown error'}` : 'Interstitial dismissed');
          unlockReplay();
        }
      }
    });
    return () => subscription.remove();
  }, [unlockReplay]);

  useEffect(() => {
    const subscription = AppState.addEventListener('change', (next) => {
      if (next !== 'active') stop();
    });
    return () => { subscription.remove(); stop(); };
  }, [stop]);

  return { status, showSticky, dismissSticky, showInterstitial };
}

export default function App() {
  const [layout, setLayout] = useState({ width: 0, height: 0 });
  const [gameState, setGameState] = useState('ready');
  const [bird, setBird] = useState({ x: 88, y: 180 });
  const [pipes, setPipes] = useState([]);
  const [score, setScore] = useState(0);
  const [best, setBest] = useState(0);
  const [difficulty, setDifficulty] = useState('normal');
  const [tapLift, setTapLift] = useState(345);
  const [replayEnabled, setReplayEnabled] = useState(true);
  const frame = useRef(null);
  const lastFrame = useRef(0);
  const velocity = useRef(0);
  const gameStateRef = useRef('ready');
  const pipesRef = useRef([]);
  const birdRef = useRef({ x: 88, y: 180 });
  const scoreRef = useRef(0);
  const layoutRef = useRef(layout);
  const difficultyRef = useRef(difficulty);
  const tapLiftRef = useRef(tapLift);
  const replayEnabledRef = useRef(true);
  const sessionStartedAt = useRef(Date.now());
  const completedRounds = useRef(0);
  const roundsSinceInterstitial = useRef(0);
  const earnedHighScore = useRef(false);
  const lastInterstitialAt = useRef(0);
  const requestedSticky = useRef(false);
  const stickyTimer = useRef(null);

  const unlockReplay = useCallback(() => {
    replayEnabledRef.current = true;
    setReplayEnabled(true);
  }, []);
  const {
    status: adStatus,
    showSticky,
    dismissSticky,
    showInterstitial,
  } = useGameAds(unlockReplay);

  const changeState = useCallback((next) => {
    gameStateRef.current = next;
    setGameState(next);
  }, []);

  const reset = useCallback(() => {
    if (frame.current) cancelAnimationFrame(frame.current);
    const nextBird = { x: Math.max(76, layoutRef.current.width * 0.28), y: Math.max(120, layoutRef.current.height * 0.42) };
    birdRef.current = nextBird;
    pipesRef.current = [];
    velocity.current = 0;
    scoreRef.current = 0;
    setBird(nextBird);
    setPipes([]);
    setScore(0);
    changeState('ready');
  }, [changeState]);

  const shouldShowInterstitial = useCallback((now) => {
    if (completedRounds.current < 2) return false;
    if (lastInterstitialAt.current && now - lastInterstitialAt.current < MIN_INTERSTITIAL_INTERVAL_MS) return false;
    const reference = lastInterstitialAt.current || sessionStartedAt.current;
    return earnedHighScore.current || roundsSinceInterstitial.current >= 3 || now - reference >= LONG_SESSION_INTERVAL_MS;
  }, []);

  const endGame = useCallback(() => {
    changeState('gameOver');
    completedRounds.current += 1;
    roundsSinceInterstitial.current += 1;
    const now = Date.now();
    if (shouldShowInterstitial(now)) {
      lastInterstitialAt.current = now;
      roundsSinceInterstitial.current = 0;
      replayEnabledRef.current = false;
      setReplayEnabled(false);
      showInterstitial();
    }
  }, [changeState, shouldShowInterstitial, showInterstitial]);

  const tick = useCallback((timestamp) => {
    if (gameStateRef.current !== 'playing') return;
    const delta = Math.min((timestamp - lastFrame.current) / 1000, 0.035);
    lastFrame.current = timestamp;
    const { width, height } = layoutRef.current;
    const groundTop = height - GROUND_HEIGHT;
    velocity.current += GRAVITY * delta;
    const nextBird = { ...birdRef.current, y: birdRef.current.y + velocity.current * delta };
    const speed = difficultyRef.current === 'easy' ? 115 : difficultyRef.current === 'hard' ? 205 : 152;
    let nextPipes = pipesRef.current.map((pipe) => ({ ...pipe, x: pipe.x - speed * delta })).filter((pipe) => pipe.x + PIPE_WIDTH > -20);
    if (!nextPipes.length || width - nextPipes[nextPipes.length - 1].x > Math.max(190, width * 0.58)) {
      const min = 94 + PIPE_GAP / 2;
      const max = Math.max(min, groundTop - PIPE_GAP / 2 - 38);
      nextPipes.push({ x: width + 24, gapY: min + Math.random() * (max - min), scored: false });
    }
    nextPipes = nextPipes.map((pipe) => {
      if (!pipe.scored && pipe.x + PIPE_WIDTH < nextBird.x) {
        const nextScore = scoreRef.current + 1;
        scoreRef.current = nextScore;
        earnedHighScore.current = nextScore > best;
        setScore(nextScore);
        setBest((current) => Math.max(current, nextScore));
        return { ...pipe, scored: true };
      }
      return pipe;
    });
    const birdBox = { left: nextBird.x-BIRD_RADIUS, right: nextBird.x+BIRD_RADIUS, top: nextBird.y-BIRD_RADIUS, bottom: nextBird.y+BIRD_RADIUS };
    const hitPipe = nextPipes.some((pipe) => birdBox.right > pipe.x && birdBox.left < pipe.x+PIPE_WIDTH && (birdBox.top < pipe.gapY-PIPE_GAP/2 || birdBox.bottom > pipe.gapY+PIPE_GAP/2));
    birdRef.current = nextBird;
    pipesRef.current = nextPipes;
    setBird(nextBird);
    setPipes(nextPipes);
    if (birdBox.top <= 0 || birdBox.bottom >= groundTop || hitPipe) {
      endGame();
      return;
    }
    frame.current = requestAnimationFrame(tick);
  }, [best, endGame]);

  const tap = useCallback(() => {
    if (!layoutRef.current.width) return;
    if (gameStateRef.current === 'ready') {
      if (stickyTimer.current) clearTimeout(stickyTimer.current);
      dismissSticky();
      earnedHighScore.current = false;
      scoreRef.current = 0;
      setScore(0);
      const min = 94 + PIPE_GAP / 2;
      const max = Math.max(min, layoutRef.current.height-GROUND_HEIGHT-PIPE_GAP/2-38);
      const initial = [{ x: layoutRef.current.width+40, gapY: min+Math.random()*(max-min), scored: false }];
      pipesRef.current = initial;
      setPipes(initial);
      velocity.current = -tapLiftRef.current;
      changeState('playing');
      lastFrame.current = performance.now();
      frame.current = requestAnimationFrame(tick);
    } else if (gameStateRef.current === 'playing') {
      velocity.current = -tapLiftRef.current;
    } else if (replayEnabledRef.current) {
      reset();
    }
  }, [changeState, dismissSticky, reset, tick]);

  useEffect(() => {
    layoutRef.current = layout;
    if (layout.width && gameStateRef.current === 'ready') reset();
  }, [layout, reset]);
  useEffect(() => { difficultyRef.current = difficulty; }, [difficulty]);
  useEffect(() => { tapLiftRef.current = tapLift; }, [tapLift]);
  useEffect(() => {
    if (gameState === 'ready' && !requestedSticky.current) {
      stickyTimer.current = setTimeout(() => {
        if (gameStateRef.current === 'ready') {
          requestedSticky.current = true;
          showSticky();
        }
      }, READY_STICKY_DELAY_MS);
    }
    return () => { if (stickyTimer.current) clearTimeout(stickyTimer.current); };
  }, [gameState, showSticky]);
  useEffect(() => () => { if (frame.current) cancelAnimationFrame(frame.current); }, []);

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="light-content" />
      <View style={styles.header}>
        <Metric label="Score" value={score} />
        <Text style={styles.title}>Flappy Blob</Text>
        <Metric label="Best" value={best} />
      </View>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Flappy Blob game. Tap to swim."
        style={styles.game}
        onLayout={(event) => setLayout(event.nativeEvent.layout)}
        onPress={tap}
      >
        <View style={styles.water} />
        {pipes.map((pipe, index) => (
          <React.Fragment key={index}>
            <View style={[styles.pipe, { left: pipe.x, top: 0, height: Math.max(0, pipe.gapY-PIPE_GAP/2) }]} />
            <View style={[styles.pipe, { left: pipe.x, top: pipe.gapY+PIPE_GAP/2, bottom: GROUND_HEIGHT }]} />
          </React.Fragment>
        ))}
        <View style={[styles.blob, gameState === 'gameOver' && styles.sadBlob, { left: bird.x-31, top: bird.y-25 }]}>
          <View style={[styles.eye, { left: 9 }]}><View style={styles.pupil} /></View>
          <View style={[styles.eye, { right: 9 }]}><View style={styles.pupil} /></View>
          <View style={[styles.mouth, gameState === 'gameOver' && styles.sadMouth]} />
        </View>
        <View style={[styles.ground, { height: GROUND_HEIGHT }]} />
        {gameState !== 'playing' && (
          <View pointerEvents="none" style={styles.overlay}>
            <Text style={styles.overlayTitle}>{gameState === 'ready' ? 'Flappy Blob' : 'Dive Over'}</Text>
            {gameState === 'gameOver' && <Text style={styles.overlayScore}>{score}</Text>}
            <Text style={styles.overlayHint}>{gameState === 'ready' ? 'Tap to swim' : replayEnabled ? 'Tap to replay' : 'Ad loading…'}</Text>
          </View>
        )}
      </Pressable>
      <View style={styles.controls}>
        <View style={styles.row}>
          {['easy','normal','hard'].map((value) => <Pressable key={value} onPress={() => setDifficulty(value)} style={[styles.choice, difficulty === value && styles.choiceActive]}><Text style={styles.choiceText}>{value[0].toUpperCase()+value.slice(1)}</Text></Pressable>)}
        </View>
        <View style={styles.row}>
          <Text style={styles.controlLabel}>Tap lift</Text>
          <Pressable style={styles.adjust} onPress={() => setTapLift((value) => Math.max(240, value-15))}><Text style={styles.adjustText}>−</Text></Pressable>
          <Text style={styles.liftValue}>{tapLift}</Text>
          <Pressable style={styles.adjust} onPress={() => setTapLift((value) => Math.min(480, value+15))}><Text style={styles.adjustText}>+</Text></Pressable>
        </View>
        <Text numberOfLines={1} style={styles.adStatus}>{adStatus}</Text>
      </View>
    </SafeAreaView>
  );
}

function Metric({ label, value }) {
  return <View style={styles.metric}><Text style={styles.metricLabel}>{label}</Text><Text style={styles.metricValue}>{value}</Text></View>;
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#021f3a' },
  header: { height: 70, paddingHorizontal: 18, flexDirection: 'row', alignItems: 'center' },
  title: { flex: 2, color: 'white', fontSize: 24, fontWeight: '900', textAlign: 'center' },
  metric: { flex: 1, alignItems: 'center' }, metricLabel: { color: '#cbd5e1', fontSize: 12, fontWeight: '700' }, metricValue: { color: '#e2e8f0', fontSize: 18, fontWeight: '800' },
  game: { flex: 1, overflow: 'hidden', backgroundColor: '#0369a1' }, water: { ...StyleSheet.absoluteFillObject, backgroundColor: '#075985' },
  pipe: { position: 'absolute', width: PIPE_WIDTH, borderRadius: 18, backgroundColor: '#1e405d', borderWidth: 9, borderColor: '#0f766e' },
  ground: { position: 'absolute', left: 0, right: 0, bottom: 0, backgroundColor: '#fde68a', borderTopWidth: 6, borderTopColor: '#fbbf24' },
  blob: { position: 'absolute', width: 62, height: 50, borderRadius: 25, backgroundColor: '#fb7185' }, sadBlob: { backgroundColor: '#f472b6' },
  eye: { position: 'absolute', top: 10, width: 16, height: 16, borderRadius: 8, backgroundColor: 'white', alignItems: 'center', justifyContent: 'center' }, pupil: { width: 6, height: 6, borderRadius: 3, backgroundColor: '#0f172a' },
  mouth: { position: 'absolute', left: 22, top: 33, width: 18, height: 7, borderBottomWidth: 3, borderColor: '#0f172a', borderRadius: 9 }, sadMouth: { borderBottomWidth: 0, borderTopWidth: 3 },
  overlay: { ...StyleSheet.absoluteFillObject, alignItems: 'center', paddingTop: '28%' }, overlayTitle: { color: 'white', fontSize: 32, fontWeight: '900' }, overlayScore: { color: 'white', fontSize: 42, fontWeight: '900', marginTop: 8 }, overlayHint: { color: '#e0f2fe', fontSize: 18, fontWeight: '700', marginTop: 14 },
  controls: { paddingHorizontal: 18, paddingTop: 10, paddingBottom: 14, gap: 9 }, row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8 },
  choice: { flex: 1, paddingVertical: 8, borderRadius: 8, backgroundColor: '#1e405d' }, choiceActive: { backgroundColor: '#0ea5e9' }, choiceText: { color: 'white', textAlign: 'center', fontWeight: '700' },
  controlLabel: { color: '#cbd5e1', fontWeight: '700', marginRight: 'auto' }, adjust: { width: 38, height: 32, borderRadius: 8, backgroundColor: '#1e405d', alignItems: 'center', justifyContent: 'center' }, adjustText: { color: 'white', fontSize: 22, fontWeight: '800' }, liftValue: { color: 'white', width: 42, textAlign: 'center', fontWeight: '800' },
  adStatus: { color: '#cbd5e1', fontSize: 13, fontWeight: '600', textAlign: 'center' },
});
