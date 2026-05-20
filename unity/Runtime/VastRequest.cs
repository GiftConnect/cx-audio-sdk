using System;

namespace Cx.Audio.Unity
{
    public sealed class VastRequest
    {
        public VastRequest(string vastTagUrl)
        {
            VastTagUrl = vastTagUrl;
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
