using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;
using UnityEngine;

namespace Cx.Audio.Unity
{
    public enum ConsumablePlacement
    {
        BackgroundAudio,
        StickyPlayer,
        FullscreenInterstitial
    }

    public sealed class ConsumableAdRequest
    {
        private const string DefaultEndpointUrl = "https://2.e.serverbid.com/mpb/get";
        private static readonly DateTime UnixEpoch =
            new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
        private readonly Dictionary<string, string> parameters = new Dictionary<string, string>();

        public ConsumableAdRequest(string placementHash)
        {
            if (string.IsNullOrEmpty(placementHash) || placementHash.Trim().Length == 0)
            {
                throw new ArgumentException("placementHash must not be blank", "placementHash");
            }

            Set("s", placementHash);
            Set("rt", "vast");
            Set("secure", true);
            Set("cb", DefaultCacheBuster());
        }

        public ConsumableAdRequest IpAddress(string value) { return Set("i", value); }
        public ConsumableAdRequest Locale(string value) { return Set("locale", value); }
        public ConsumableAdRequest ContentCategories(params string[] values) { return Set("cat", Join(values)); }
        public ConsumableAdRequest BlockedCategories(params string[] values) { return Set("bcat", Join(values)); }
        public ConsumableAdRequest BlockedAdvertisers(params string[] values) { return Set("badv", Join(values)); }
        public ConsumableAdRequest LimitAdTracking(bool value) { return Set("lmt", value); }
        public ConsumableAdRequest DoNotTrack(bool value) { return Set("dnt", value); }
        public ConsumableAdRequest AppStoreUrl(string value) { return Set("appstoreurl", value); }
        public ConsumableAdRequest AppName(string value) { return Set("appname", value); }
        public ConsumableAdRequest AppVersion(string value) { return Set("appversion", value); }
        public ConsumableAdRequest AppBundle(string value) { return Set("appbundle", value); }
        public ConsumableAdRequest Idfa(string value) { return Set("idfa", value); }
        public ConsumableAdRequest Aid(string value) { return Set("aid", value); }
        public ConsumableAdRequest Gadid(string value) { return Set("gadid", value); }
        public ConsumableAdRequest Ifa(string value) { return Set("ifa", value); }
        public ConsumableAdRequest Keywords(params string[] values) { return Set("kw", Join(values)); }
        public ConsumableAdRequest Longitude(double value) { return Set("long", FormatNumber(value)); }
        public ConsumableAdRequest Latitude(double value) { return Set("lat", FormatNumber(value)); }
        public ConsumableAdRequest Coppa(bool value) { return Set("coppa", value); }
        public ConsumableAdRequest Gdpr(bool value) { return Set("gdpr", value); }
        public ConsumableAdRequest GdprConsent(string value) { return Set("gdpr_consent", value); }
        public ConsumableAdRequest UsPrivacy(string value) { return Set("us_privacy", value); }
        public ConsumableAdRequest Gpp(string value) { return Set("gpp", value); }
        public ConsumableAdRequest GppSectionIds(params string[] values) { return Set("gpp_sid", Join(values)); }
        public ConsumableAdRequest ContentId(string value) { return Set("c_id", value); }
        public ConsumableAdRequest ContentGenre(params string[] values) { return Set("c_genre", Join(values)); }
        public ConsumableAdRequest ContentTitle(string value) { return Set("c_title", value); }
        public ConsumableAdRequest ContentDurationSeconds(int value) { return Set("c_dur", value.ToString()); }
        public ConsumableAdRequest ContentUrl(string value) { return Set("c_url", value); }

        internal string ToVastTagUrl()
        {
            return ToVastTagUrl(null);
        }

        internal string ToVastTagUrl(ConsumablePlacement? placement)
        {
            return ToVastTagUrl(placement, null);
        }

        internal string ToVastTagUrl(ConsumablePlacement? placement, string userAgentParameter)
        {
            Dictionary<string, string> requestParameters = AutomaticParameters(placement, userAgentParameter);
            StringBuilder builder = new StringBuilder(DefaultEndpointUrl);
            builder.Append(DefaultEndpointUrl.IndexOf("?", StringComparison.Ordinal) >= 0 ? "&" : "?");
            bool first = true;
            foreach (KeyValuePair<string, string> pair in requestParameters)
            {
                if (string.IsNullOrEmpty(pair.Value) || pair.Value.Trim().Length == 0)
                {
                    continue;
                }
                if (!first)
                {
                    builder.Append("&");
                }
                builder.Append(Uri.EscapeDataString(pair.Key));
                builder.Append("=");
                builder.Append(Uri.EscapeDataString(pair.Value));
                first = false;
            }
            return builder.ToString();
        }

        private Dictionary<string, string> AutomaticParameters(
            ConsumablePlacement? placement,
            string userAgentParameter)
        {
            Dictionary<string, string> requestParameters = new Dictionary<string, string>(parameters);
            SetAutomatic(requestParameters, "u", userAgentParameter);
            SetAutomatic(requestParameters, "locale", CultureInfo.CurrentCulture.Name);
            SetAutomatic(requestParameters, "appname", Application.productName);
            SetAutomatic(requestParameters, "appbundle", Application.identifier);
            SetAutomatic(requestParameters, "v_delivery", "2");
            SetAutomatic(requestParameters, "v_delay", "-1");
            SetAutomatic(requestParameters, "v_pm", "1");
            SetAutomatic(requestParameters, "v_max_dur", "90");
            SetAutomatic(
                requestParameters,
                "v_mimes",
                "audio/mpeg,audio/mp4,audio/aac,audio/ogg,audio/wav,audio/x-wav,video/mp4,image/jpeg,image/png,image/gif,image/webp");
            SetAutomatic(requestParameters, "v_cmp_types", "1");
            SetAutomatic(requestParameters, "v_cmp_mimes", "image/jpeg,image/png,image/gif,image/webp");
            SetAutomatic(requestParameters, "v_cmp_sizes", "300x250");
            SetAutomatic(requestParameters, "v_protocols", "2,3,5,6,7,8");
            SetAutomatic(requestParameters, "c_audio_feed_type", "6");

            if (placement == ConsumablePlacement.FullscreenInterstitial)
            {
                SetAutomatic(requestParameters, "sdkpt", "3");
                SetAutomatic(requestParameters, "v_plcmt", "3");
                SetAutomatic(requestParameters, "v_linearity", "1");
                SetAutomatic(requestParameters, "v_min_bitrate", "64");
                SetAutomatic(requestParameters, "v_max_bitrate", "2500");
            }
            else if (placement == ConsumablePlacement.StickyPlayer)
            {
                SetAutomatic(requestParameters, "sdkpt", "2");
                SetAutomatic(requestParameters, "v_plcmt", "2");
                SetAutomatic(requestParameters, "v_linearity", "2");
                SetAutomatic(requestParameters, "v_min_bitrate", "64");
                SetAutomatic(requestParameters, "v_max_bitrate", "320");
            }
            else if (placement == ConsumablePlacement.BackgroundAudio)
            {
                SetAutomatic(requestParameters, "sdkpt", "1");
                SetAutomatic(requestParameters, "v_plcmt", "2");
                SetAutomatic(requestParameters, "v_linearity", "2");
                SetAutomatic(requestParameters, "v_min_bitrate", "64");
                SetAutomatic(requestParameters, "v_max_bitrate", "320");
            }
            else
            {
                SetAutomatic(requestParameters, "v_min_bitrate", "64");
                SetAutomatic(requestParameters, "v_max_bitrate", "320");
            }
            return requestParameters;
        }

        private static void SetAutomatic(Dictionary<string, string> requestParameters, string name, string value)
        {
            if (!requestParameters.ContainsKey(name) && !string.IsNullOrEmpty(value) && value.Trim().Length > 0)
            {
                requestParameters[name] = value;
            }
        }

        private static string DefaultCacheBuster()
        {
            return ((long)(DateTime.UtcNow - UnixEpoch).TotalMilliseconds).ToString(CultureInfo.InvariantCulture);
        }

        private ConsumableAdRequest Set(string name, bool value)
        {
            return Set(name, value ? "1" : "0");
        }

        private ConsumableAdRequest Set(string name, string value)
        {
            if (string.IsNullOrEmpty(name) || name.Trim().Length == 0)
            {
                throw new ArgumentException("parameter name must not be blank", "name");
            }
            if (string.IsNullOrEmpty(value) || value.Trim().Length == 0)
            {
                parameters.Remove(name);
            }
            else
            {
                parameters[name] = value;
            }
            return this;
        }

        private static string Join(string[] values)
        {
            if (values == null || values.Length == 0)
            {
                return null;
            }
            List<string> cleaned = new List<string>();
            foreach (string value in values)
            {
                if (!string.IsNullOrEmpty(value) && value.Trim().Length > 0)
                {
                    cleaned.Add(value);
                }
            }
            return cleaned.Count == 0 ? null : string.Join(",", cleaned.ToArray());
        }

        private static string FormatNumber(double value)
        {
            return value.ToString("0.######", CultureInfo.InvariantCulture);
        }
    }

    public sealed class VastRequest
    {
        public const string DefaultUserAgent = "cx-audio-sdk-unity/0.1";

        public VastRequest(string vastTagUrl)
        {
            VastTagUrl = vastTagUrl;
            UserAgent = DefaultUserAgent;
        }

        public VastRequest(ConsumableAdRequest request)
            : this(request, ConsumablePlacement.BackgroundAudio)
        {
        }

        public VastRequest(ConsumableAdRequest request, ConsumablePlacement? placement)
        {
            if (request == null)
            {
                throw new ArgumentNullException("request");
            }
            UserAgent = DefaultUserAgent;
            ConsumablePlacement resolvedPlacement = placement ?? ConsumablePlacement.BackgroundAudio;
            VastTagUrl = request.ToVastTagUrl(resolvedPlacement, UserAgent);
        }

        public string VastTagUrl { get; private set; }

        public int ConnectTimeoutMs { get; set; }

        public int ReadTimeoutMs { get; set; }

        internal string UserAgent { get; private set; }

        internal void Validate()
        {
            if (string.IsNullOrEmpty(VastTagUrl) || VastTagUrl.Trim().Length == 0)
            {
                throw new ArgumentException("vastTagUrl must not be blank", "VastTagUrl");
            }

            Uri uri;
            if (!Uri.TryCreate(VastTagUrl, UriKind.Absolute, out uri) ||
                (uri.Scheme != Uri.UriSchemeHttp && uri.Scheme != Uri.UriSchemeHttps))
            {
                throw new ArgumentException("vastTagUrl must be an absolute HTTP or HTTPS URL", "VastTagUrl");
            }

            if (ConnectTimeoutMs < 0)
            {
                throw new ArgumentOutOfRangeException("ConnectTimeoutMs", "connectTimeoutMs must be >= 0");
            }

            if (ReadTimeoutMs < 0)
            {
                throw new ArgumentOutOfRangeException("ReadTimeoutMs", "readTimeoutMs must be >= 0");
            }

            if (UserAgent != null && UserAgent.Trim().Length == 0)
            {
                throw new ArgumentException("userAgent must not be blank", "UserAgent");
            }
        }
    }
}
