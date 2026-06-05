using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;

namespace Cx.Audio.Unity
{
    public sealed class ConsumableAdRequest
    {
        private const string DefaultEndpointUrl = "https://2.e.serverbid.com/mpb/get";
        private readonly Dictionary<string, string> parameters = new Dictionary<string, string>();

        public ConsumableAdRequest(string placementHash)
        {
            if (string.IsNullOrEmpty(placementHash) || placementHash.Trim().Length == 0)
            {
                throw new ArgumentException("placementHash must not be blank", "placementHash");
            }

            EndpointUrl = DefaultEndpointUrl;
            Set("s", placementHash);
            Set("rt", "vast");
            Set("secure", true);
            Set("cb", Guid.NewGuid().ToString("N"));
        }

        public string EndpointUrl { get; set; }

        public ConsumableAdRequest ResponseType(string value) { return Set("rt", value); }
        public ConsumableAdRequest UserAgentParameter(string value) { return Set("u", value); }
        public ConsumableAdRequest IpAddress(string value) { return Set("i", value); }
        public ConsumableAdRequest Locale(string value) { return Set("locale", value); }
        public ConsumableAdRequest ContentCategories(params string[] values) { return Set("cat", Join(values)); }
        public ConsumableAdRequest BlockedCategories(params string[] values) { return Set("bcat", Join(values)); }
        public ConsumableAdRequest BlockedAdvertisers(params string[] values) { return Set("badv", Join(values)); }
        public ConsumableAdRequest LimitAdTracking(bool value) { return Set("lmt", value); }
        public ConsumableAdRequest DoNotTrack(bool value) { return Set("dnt", value); }
        public ConsumableAdRequest Secure(bool value) { return Set("secure", value); }
        public ConsumableAdRequest SupplyChain(string value) { return Set("schain", value); }
        public ConsumableAdRequest CacheBuster(string value) { return Set("cb", value); }
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
        public ConsumableAdRequest PlayerSize(int width, int height) { Set("v_w", width.ToString()); return Set("v_h", height.ToString()); }
        public ConsumableAdRequest VideoPlacement(int value) { return Set("v_plcmt", value.ToString()); }
        public ConsumableAdRequest Delivery(int value) { return Set("v_delivery", value.ToString()); }
        public ConsumableAdRequest StartDelaySeconds(int value) { parameters.Remove("v_position"); return Set("v_delay", value.ToString()); }
        public ConsumableAdRequest Position(string value) { parameters.Remove("v_delay"); return Set("v_position", value); }
        public ConsumableAdRequest MaxDurationSeconds(int value) { return Set("v_max_dur", value.ToString()); }
        public ConsumableAdRequest MediaMimes(params string[] values) { return Set("v_mimes", Join(values)); }
        public ConsumableAdRequest Skippable(bool value) { return Set("v_skip", value); }
        public ConsumableAdRequest PlaybackMethod(int value) { return Set("v_pm", value.ToString()); }
        public ConsumableAdRequest MaxBitrateKbps(int value) { return Set("v_max_bitrate", value.ToString()); }
        public ConsumableAdRequest MinBitrateKbps(int value) { return Set("v_min_bitrate", value.ToString()); }
        public ConsumableAdRequest CompanionTypes(params string[] values) { return Set("v_cmp_types", Join(values)); }
        public ConsumableAdRequest CompanionMimes(params string[] values) { return Set("v_cmp_mimes", Join(values)); }
        public ConsumableAdRequest CompanionSizes(params string[] values) { return Set("v_cmp_sizes", Join(values)); }
        public ConsumableAdRequest Protocols(params string[] values) { return Set("v_protocols", Join(values)); }
        public ConsumableAdRequest ContentId(string value) { return Set("c_id", value); }
        public ConsumableAdRequest ContentGenre(params string[] values) { return Set("c_genre", Join(values)); }
        public ConsumableAdRequest ContentTitle(string value) { return Set("c_title", value); }
        public ConsumableAdRequest ContentDurationSeconds(int value) { return Set("c_dur", value.ToString()); }
        public ConsumableAdRequest ContentUrl(string value) { return Set("c_url", value); }
        public ConsumableAdRequest ContentAudioFeedType(int value) { return Set("c_audio_feed_type", value.ToString()); }
        public ConsumableAdRequest CustomParameter(string name, string value) { return Set(name, value); }

        public string ToVastTagUrl()
        {
            if (string.IsNullOrEmpty(EndpointUrl) || EndpointUrl.Trim().Length == 0)
            {
                throw new ArgumentException("EndpointUrl must not be blank", "EndpointUrl");
            }

            Uri endpoint;
            if (!Uri.TryCreate(EndpointUrl, UriKind.Absolute, out endpoint) ||
                (endpoint.Scheme != Uri.UriSchemeHttp && endpoint.Scheme != Uri.UriSchemeHttps))
            {
                throw new ArgumentException("EndpointUrl must be an absolute HTTP or HTTPS URL", "EndpointUrl");
            }

            StringBuilder builder = new StringBuilder(EndpointUrl);
            builder.Append(EndpointUrl.IndexOf("?", StringComparison.Ordinal) >= 0 ? "&" : "?");
            bool first = true;
            foreach (KeyValuePair<string, string> pair in parameters)
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
        public VastRequest(string vastTagUrl)
        {
            VastTagUrl = vastTagUrl;
        }

        public VastRequest(ConsumableAdRequest request)
        {
            if (request == null)
            {
                throw new ArgumentNullException("request");
            }
            VastTagUrl = request.ToVastTagUrl();
        }

        public string VastTagUrl { get; set; }

        public int ConnectTimeoutMs { get; set; }

        public int ReadTimeoutMs { get; set; }

        public string UserAgent { get; set; }

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
