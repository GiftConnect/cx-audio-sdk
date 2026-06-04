export type CxAudioPlacement =
  | 'backgroundAudio'
  | 'stickyPlayer'
  | 'fullscreenInterstitial';

export type CxAudioEventType =
  | 'started'
  | 'loaded'
  | 'clicked'
  | 'completed'
  | 'stopped'
  | 'dismissed'
  | 'failed';

export interface CxAudioEvent {
  type: CxAudioEventType;
  placement: CxAudioPlacement;
  handleId?: string;
  clickThroughUrl?: string;
  message?: string;
  errorClass?: string;
}

export interface CxAudioSubscription {
  remove(): void;
}

export interface VastRequest {
  vastTagUrl: string;
  connectTimeoutMs?: number;
  readTimeoutMs?: number;
  userAgent?: string;
}

export interface BackgroundAudioOptions {
  volume?: number;
  minDeviceVolume?: number;
}

export interface StickyPlayerOptions {
  position?: 'top' | 'bottom';
  heightDp?: number;
  horizontalPaddingDp?: number;
  showCloseButton?: boolean;
  volume?: number;
  minDeviceVolume?: number;
}

export interface FullscreenInterstitialOptions {
  showCloseButton?: boolean;
  companionHeightDp?: number;
  companionMaxWidthDp?: number;
  autoPlayVideo?: boolean;
  volume?: number;
  minDeviceVolume?: number;
}

export declare const StickyPlayerPosition: Readonly<{
  TOP: 'top';
  BOTTOM: 'bottom';
}>;

export declare function addCxAudioListener(
  listener: (event: CxAudioEvent) => void
): CxAudioSubscription;

export declare const BackgroundAudio: Readonly<{
  play(request: VastRequest, options?: BackgroundAudioOptions): Promise<string>;
  stop(handleId: string): Promise<void>;
  setVolume(handleId: string, volume: number): Promise<void>;
}>;

export declare const StickyPlayer: Readonly<{
  show(request: VastRequest, options?: StickyPlayerOptions): Promise<string>;
  dismiss(handleId: string): Promise<void>;
}>;

export declare const FullscreenInterstitial: Readonly<{
  show(request: VastRequest, options?: FullscreenInterstitialOptions): Promise<string>;
  dismiss(handleId: string): Promise<void>;
}>;
