using System;
using System.Collections;
using System.Collections.Generic;
using Cx.Audio.Unity;
using UnityEngine;

public sealed class FlappyBlobGame : MonoBehaviour, IStickyPlayerListener, IFullscreenInterstitialListener
{
    private const string PlaceholderHash = "YOUR_PLACEMENT_HASH";
    private const float WorldWidth = 390f;
    private const float GroundHeight = 82f;
    private const float BlobRadius = 19f;
    private const float PipeWidth = 66f;
    private const float PipeGap = 176f;
    private const float Gravity = 920f;
    private const float FlapVelocity = -325f;
    private const float PipeSpeed = 122f;
    private const float ReadyStickyDelay = 8f;
    private const float MinimumInterstitialInterval = 90f;
    private const float LongSessionInterval = 180f;

    [SerializeField] private string placementHash = PlaceholderHash;
    [Header("Debug QA advertising identity")]
    [Tooltip("QA only: a host-supplied GAID/AAID collected after the applicable consent flow.")]
    [SerializeField] private string advertisingId = "";
    [SerializeField] private bool advertisingIdentifierAllowed;

    private readonly List<Pipe> pipes = new List<Pipe>();
    private GameState state = GameState.Ready;
    private StickyPlayerHandle stickyHandle;
    private FullscreenInterstitialHandle interstitialHandle;
    private Texture2D whiteTexture;
    private Texture2D blobTexture;
    private GUIStyle titleStyle;
    private GUIStyle scoreStyle;
    private GUIStyle bodyStyle;
    private GUIStyle buttonStyle;
    private float blobY = 330f;
    private float velocity;
    private float spawnTimer;
    private float sessionStartedAt;
    private float lastInterstitialAt = -1000f;
    private int score;
    private int bestScore;
    private int completedRounds;
    private int roundsSinceInterstitial;
    private bool replayLocked;
    private bool earnedHighScore;
    private string adStatus = "Ad idle";

    private enum GameState
    {
        Ready,
        Playing,
        GameOver
    }

    private sealed class Pipe
    {
        internal float X;
        internal float GapY;
        internal bool Scored;
    }

    private void Awake()
    {
        Application.targetFrameRate = 60;
        sessionStartedAt = Time.realtimeSinceStartup;
        InitializeAds();
        CreateTextures();
    }

    private void Start()
    {
        StartCoroutine(ShowReadyStickyAfterDelay());
    }

    private void Update()
    {
        if (state == GameState.Playing)
        {
            UpdateGame(Time.deltaTime);
        }

        if (Pressed())
        {
            if (state == GameState.Ready)
            {
                StartRound();
            }
            else if (state == GameState.Playing)
            {
                velocity = FlapVelocity;
            }
            else if (!replayLocked)
            {
                ResetRound();
            }
        }
    }

    private void UpdateGame(float deltaTime)
    {
        float worldHeight = LogicalHeight();
        velocity += Gravity * deltaTime;
        blobY += velocity * deltaTime;
        spawnTimer -= deltaTime;

        if (spawnTimer <= 0f)
        {
            spawnTimer = 1.55f;
            float minimumGapCenter = 145f;
            float maximumGapCenter = worldHeight - GroundHeight - 145f;
            pipes.Add(new Pipe
            {
                X = WorldWidth + PipeWidth,
                GapY = UnityEngine.Random.Range(minimumGapCenter, maximumGapCenter)
            });
        }

        for (int index = pipes.Count - 1; index >= 0; index--)
        {
            Pipe pipe = pipes[index];
            pipe.X -= PipeSpeed * deltaTime;
            if (!pipe.Scored && pipe.X + PipeWidth < BlobX())
            {
                pipe.Scored = true;
                score++;
                if (score > bestScore)
                {
                    bestScore = score;
                    earnedHighScore = true;
                }
            }

            if (pipe.X + PipeWidth < -10f)
            {
                pipes.RemoveAt(index);
            }
        }

        if (blobY - BlobRadius <= 0f || blobY + BlobRadius >= worldHeight - GroundHeight || HitsPipe())
        {
            EndRound();
        }
    }

    private bool HitsPipe()
    {
        float left = BlobX() - BlobRadius;
        float right = BlobX() + BlobRadius;
        float top = blobY - BlobRadius;
        float bottom = blobY + BlobRadius;
        foreach (Pipe pipe in pipes)
        {
            if (right < pipe.X || left > pipe.X + PipeWidth)
            {
                continue;
            }

            if (top < pipe.GapY - PipeGap * 0.5f || bottom > pipe.GapY + PipeGap * 0.5f)
            {
                return true;
            }
        }
        return false;
    }

    private void StartRound()
    {
        DismissSticky();
        state = GameState.Playing;
        velocity = FlapVelocity;
        spawnTimer = 0.65f;
        earnedHighScore = false;
    }

    private void EndRound()
    {
        state = GameState.GameOver;
        completedRounds++;
        roundsSinceInterstitial++;

        float now = Time.realtimeSinceStartup;
        float reference = lastInterstitialAt > 0f ? lastInterstitialAt : sessionStartedAt;
        bool intervalElapsed = now - lastInterstitialAt >= MinimumInterstitialInterval;
        bool shouldShow = completedRounds >= 2 && intervalElapsed &&
            (earnedHighScore || roundsSinceInterstitial >= 3 || now - reference >= LongSessionInterval);
        if (shouldShow)
        {
            lastInterstitialAt = now;
            roundsSinceInterstitial = 0;
            replayLocked = true;
            ShowInterstitial();
        }
    }

    private void ResetRound()
    {
        state = GameState.Ready;
        pipes.Clear();
        score = 0;
        velocity = 0f;
        blobY = LogicalHeight() * 0.42f;
        StartCoroutine(ShowReadyStickyAfterDelay());
    }

    private IEnumerator ShowReadyStickyAfterDelay()
    {
        yield return new WaitForSecondsRealtime(ReadyStickyDelay);
        if (state == GameState.Ready && stickyHandle == null && interstitialHandle == null)
        {
            ShowSticky();
        }
    }

    private VastRequest CreateRequest(ConsumablePlacement placement)
    {
        if (string.IsNullOrWhiteSpace(placementHash) || placementHash == PlaceholderHash)
        {
            throw new InvalidOperationException("Set Placement Hash on the FlappyBlobGame component.");
        }

        string permittedId = PermittedAdvertisingId();
        ConsumableAdRequest request = new ConsumableAdRequest(placementHash)
            .LimitAdTracking(permittedId == null)
            .AppName("Flappy Blob Unity")
            .AppBundle(Application.identifier)
            .ContentGenre("game")
            .ContentTitle("Flappy Blob");
        if (permittedId != null)
        {
            request.Gadid(permittedId).Ifa(permittedId);
        }
        return new VastRequest(request, placement);
    }

    private void InitializeAds()
    {
        string permittedId = PermittedAdvertisingId();
        CxAudioConfiguration configuration = new CxAudioConfiguration
        {
            AgeTreatment = CxAgeTreatment.GeneralAudience
        };
        if (Debug.isDebugBuild)
        {
            configuration.PrivacyOverride = new CxPrivacyOverride
            {
                GdprApplies = false,
                PersonalizedAdsAllowed = permittedId != null,
                UsPrivacyOptOut = false,
                UsPrivacyString = "1YNN"
            };
        }
        CxAudio.Initialize(configuration);
        adStatus = permittedId == null ? "Ad ready (limited tracking)" : "Ad ready";
    }

    private string PermittedAdvertisingId()
    {
        if (!advertisingIdentifierAllowed || string.IsNullOrWhiteSpace(advertisingId)) return null;
        string normalized = advertisingId.Trim();
        return normalized == "00000000-0000-0000-0000-000000000000" ? null : normalized;
    }

    private void ShowSticky()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            adStatus = "Ready-screen ad loading";
            stickyHandle = StickyPlayer.Show(
                CreateRequest(ConsumablePlacement.StickyPlayer),
                new StickyPlayerOptions
                {
                    Position = StickyPlayerPosition.Bottom,
                    ShowCloseButton = true,
                    Volume = 0.14f,
                    MinDeviceVolume = 0f
                },
                this);
        }
        catch (Exception error)
        {
            DisposeSticky();
            adStatus = "Ready-screen ad failed: " + error.Message;
        }
#else
        adStatus = "Ads run in an Android player build";
#endif
    }

    private void ShowInterstitial()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            DismissSticky();
            adStatus = "Interstitial loading";
            interstitialHandle = FullscreenInterstitial.Show(
                CreateRequest(ConsumablePlacement.FullscreenInterstitial),
                new FullscreenInterstitialOptions
                {
                    ShowCloseButton = true,
                    AutoPlayVideo = true,
                    Volume = 0.18f,
                    MinDeviceVolume = 0f
                },
                this);
        }
        catch (Exception error)
        {
            DisposeInterstitial();
            replayLocked = false;
            adStatus = "Interstitial failed: " + error.Message;
        }
#else
        replayLocked = false;
        adStatus = "Ads run in an Android player build";
#endif
    }

    private void DismissSticky()
    {
        if (stickyHandle == null)
        {
            return;
        }
        try
        {
            stickyHandle.Dismiss();
        }
        catch (Exception)
        {
            // A terminal callback may already have released the native placement.
        }
        DisposeSticky();
    }

    private void DismissInterstitial()
    {
        if (interstitialHandle == null)
        {
            return;
        }
        try
        {
            interstitialHandle.Dismiss();
        }
        catch (Exception)
        {
            // A terminal callback may already have released the native placement.
        }
        DisposeInterstitial();
    }

    private void DisposeSticky()
    {
        if (stickyHandle != null)
        {
            stickyHandle.Dispose();
            stickyHandle = null;
        }
    }

    private void DisposeInterstitial()
    {
        if (interstitialHandle != null)
        {
            interstitialHandle.Dispose();
            interstitialHandle = null;
        }
    }

    private void OnApplicationPause(bool paused)
    {
        if (paused)
        {
            DismissSticky();
            DismissInterstitial();
            replayLocked = false;
        }
    }

    private void OnDestroy()
    {
        StopAllCoroutines();
        DismissSticky();
        DismissInterstitial();
        if (whiteTexture != null) Destroy(whiteTexture);
        if (blobTexture != null) Destroy(blobTexture);
    }

    public void OnStickyPlayerLoaded(StickyPlayerHandle handle)
    {
        stickyHandle = handle;
        adStatus = "Ready-screen ad showing";
    }

    public void OnStickyPlayerAudioStarted(StickyPlayerHandle handle)
    {
        stickyHandle = handle;
        adStatus = "Ready-screen ad playing";
    }

    public void OnStickyPlayerAudioCompleted() { adStatus = "Ready-screen ad complete"; }
    public void OnStickyPlayerClicked(string clickThroughUrl) { adStatus = "Ready-screen ad clicked"; }

    public void OnStickyPlayerDismissed()
    {
        DisposeSticky();
        adStatus = "Ready-screen ad dismissed";
    }

    public void OnStickyPlayerFailed(CxAudioError error)
    {
        DisposeSticky();
        adStatus = "Ready-screen ad failed: " + error.Message;
    }

    public void OnFullscreenInterstitialLoaded(FullscreenInterstitialHandle handle)
    {
        interstitialHandle = handle;
        adStatus = "Interstitial showing";
    }

    public void OnFullscreenInterstitialAudioStarted(FullscreenInterstitialHandle handle)
    {
        interstitialHandle = handle;
        adStatus = "Interstitial playing";
    }

    public void OnFullscreenInterstitialAudioCompleted() { adStatus = "Interstitial complete"; }
    public void OnFullscreenInterstitialClicked(string clickThroughUrl) { adStatus = "Interstitial clicked"; }

    public void OnFullscreenInterstitialDismissed()
    {
        DisposeInterstitial();
        replayLocked = false;
        adStatus = "Interstitial dismissed";
    }

    public void OnFullscreenInterstitialFailed(CxAudioError error)
    {
        DisposeInterstitial();
        replayLocked = false;
        adStatus = "Interstitial failed: " + error.Message;
    }

    private void OnGUI()
    {
        if (titleStyle == null)
        {
            CreateStyles();
        }

        float scale = Screen.width / WorldWidth;
        float height = Screen.height / scale;
        Matrix4x4 previous = GUI.matrix;
        GUI.matrix = Matrix4x4.Scale(new Vector3(scale, scale, 1f));

        DrawRect(new Rect(0f, 0f, WorldWidth, height), new Color(0.08f, 0.12f, 0.2f));
        DrawRect(new Rect(0f, height - GroundHeight, WorldWidth, GroundHeight), new Color(0.10f, 0.18f, 0.24f));
        DrawRect(new Rect(0f, height - GroundHeight, WorldWidth, 5f), new Color(0.24f, 0.84f, 0.66f));

        foreach (Pipe pipe in pipes)
        {
            float gapTop = pipe.GapY - PipeGap * 0.5f;
            float gapBottom = pipe.GapY + PipeGap * 0.5f;
            DrawPipe(new Rect(pipe.X, 0f, PipeWidth, gapTop));
            DrawPipe(new Rect(pipe.X, gapBottom, PipeWidth, height - GroundHeight - gapBottom));
        }

        GUI.DrawTexture(new Rect(BlobX() - BlobRadius, blobY - BlobRadius, BlobRadius * 2f, BlobRadius * 2f), blobTexture);
        GUI.Label(new Rect(0f, 24f, WorldWidth, 52f), score.ToString(), scoreStyle);
        GUI.Label(new Rect(12f, height - 34f, WorldWidth - 24f, 24f), adStatus, bodyStyle);

        if (state == GameState.Ready)
        {
            GUI.Label(new Rect(0f, height * 0.18f, WorldWidth, 70f), "FLAPPY BLOB", titleStyle);
            GUI.Label(new Rect(0f, height * 0.61f, WorldWidth, 34f), "Tap anywhere to flap", bodyStyle);
            GUI.Label(new Rect(0f, height * 0.66f, WorldWidth, 28f), "Best  " + bestScore, bodyStyle);
        }
        else if (state == GameState.GameOver)
        {
            GUI.Label(new Rect(0f, height * 0.25f, WorldWidth, 64f), "SPLAT!", titleStyle);
            GUI.Label(new Rect(0f, height * 0.36f, WorldWidth, 32f), "Score  " + score + "     Best  " + bestScore, bodyStyle);
            string replayText = replayLocked ? "Advertisement playing…" : "Tap to fly again";
            GUI.Box(new Rect(76f, height * 0.47f, 238f, 54f), replayText, buttonStyle);
        }

        GUI.matrix = previous;
    }

    private void DrawPipe(Rect rect)
    {
        DrawRect(rect, new Color(0.18f, 0.73f, 0.52f));
        DrawRect(new Rect(rect.x + 6f, rect.y, 7f, rect.height), new Color(0.38f, 0.92f, 0.68f));
    }

    private void DrawRect(Rect rect, Color color)
    {
        Color previous = GUI.color;
        GUI.color = color;
        GUI.DrawTexture(rect, whiteTexture);
        GUI.color = previous;
    }

    private void CreateTextures()
    {
        whiteTexture = new Texture2D(1, 1, TextureFormat.RGBA32, false);
        whiteTexture.SetPixel(0, 0, Color.white);
        whiteTexture.Apply();

        const int size = 64;
        blobTexture = new Texture2D(size, size, TextureFormat.RGBA32, false);
        blobTexture.filterMode = FilterMode.Bilinear;
        for (int y = 0; y < size; y++)
        {
            for (int x = 0; x < size; x++)
            {
                float dx = x - size * 0.5f;
                float dy = y - size * 0.5f;
                bool body = dx * dx + dy * dy <= 29f * 29f;
                Color color = body ? new Color(0.95f, 0.48f + y / 420f, 0.60f, 1f) : Color.clear;
                bool eye = (dx > -13f && dx < -5f || dx > 5f && dx < 13f) && dy > 2f && dy < 10f;
                if (eye) color = new Color(0.07f, 0.09f, 0.14f, 1f);
                if (dy < -7f && dy > -11f && Mathf.Abs(dx) < 9f) color = new Color(0.35f, 0.08f, 0.15f, 1f);
                blobTexture.SetPixel(x, y, color);
            }
        }
        blobTexture.Apply();
    }

    private void CreateStyles()
    {
        titleStyle = new GUIStyle(GUI.skin.label)
        {
            alignment = TextAnchor.MiddleCenter,
            fontSize = 34,
            fontStyle = FontStyle.Bold,
            normal = { textColor = new Color(0.98f, 0.82f, 0.38f) }
        };
        scoreStyle = new GUIStyle(titleStyle) { fontSize = 38, normal = { textColor = Color.white } };
        bodyStyle = new GUIStyle(GUI.skin.label)
        {
            alignment = TextAnchor.MiddleCenter,
            fontSize = 17,
            normal = { textColor = new Color(0.86f, 0.91f, 0.96f) }
        };
        buttonStyle = new GUIStyle(GUI.skin.box)
        {
            alignment = TextAnchor.MiddleCenter,
            fontSize = 18,
            fontStyle = FontStyle.Bold,
            normal = { textColor = Color.white }
        };
    }

    private static bool Pressed()
    {
        return Input.GetMouseButtonDown(0) || Input.GetKeyDown(KeyCode.Space);
    }

    private static float BlobX() { return WorldWidth * 0.28f; }
    private static float LogicalHeight() { return Screen.height / (Screen.width / WorldWidth); }
}
