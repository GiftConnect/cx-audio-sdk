namespace Cx.Audio.Unity
{
    public interface IBackgroundAudioListener
    {
        void OnBackgroundAudioStarted(BackgroundAudioHandle handle);

        void OnBackgroundAudioCompleted();

        void OnBackgroundAudioStopped();

        void OnBackgroundAudioFailed(CxAudioError error);
    }

    public interface IStickyPlayerListener
    {
        void OnStickyPlayerLoaded(StickyPlayerHandle handle);

        void OnStickyPlayerClicked(string clickThroughUrl);

        void OnStickyPlayerDismissed();

        void OnStickyPlayerFailed(CxAudioError error);
    }

    public interface IFullscreenInterstitialListener
    {
        void OnFullscreenInterstitialLoaded(FullscreenInterstitialHandle handle);

        void OnFullscreenInterstitialClicked(string clickThroughUrl);

        void OnFullscreenInterstitialDismissed();

        void OnFullscreenInterstitialFailed(CxAudioError error);
    }
}
