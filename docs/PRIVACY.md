# Privacy and consent

The host application owns consent collection and user-facing privacy prompts.
The CX Audio SDK does not provide a Consent Management Platform (CMP), display
privacy notices, or request App Tracking Transparency authorization.

## Host application responsibilities

Before requesting an ad, the host application is responsible for:

- Selecting and integrating its CMP.
- Presenting applicable GDPR, US-state, and other regional notices.
- Presenting Apple's ATT prompt when appropriate.
- Declaring COPPA, child-directed, and under-age-of-consent treatment
  correctly.
- Requesting location permission before supplying permitted location data.
- Configuring the application's audio session and app-level audio behavior.
- Supplying the placement identifier issued by CX.

ATT, GDPR consent, US privacy choices, and child or underage treatment are
independent. Do not treat authorization in one area as consent in another.
Child and underage restrictions override other signals.

## Current SDK status

Automatic standard-signal ingestion, normalized privacy-state resolution, and
one centralized enforcement layer for every outbound request are not fully
implemented in this version.

The explicit privacy and advertising-identifier request fields are intended
only for custom CMP integrations, controlled testing, and approved enterprise
integrations. Their presence does not by itself establish end-to-end privacy
compliance. Treat missing, malformed, stale, unresolved, or conflicting
signals conservatively.

Do not ship this version as production-ready until the integration has passed
the publisher's legal, platform, privacy, and security review.

## Advertising identifiers

The SDK does not silently collect advertising identifiers.

- On iOS, read IDFA only after ATT authorization. When authorization is not
  granted or the value is all zeros, omit `idfa` and `ifa` and set
  `limitAdTracking` to `true`.
- On Android, supply GAID only when the host has the necessary consent and
  platform declaration. When the value is unavailable, deleted, limited, or
  all zeros, omit `gadid` and `ifa` and set `limitAdTracking` to `true`.
- React Native and Unity applications must obtain any permitted identifier
  through their host integration before passing it to the request.

Do not substitute Android ID, IDFV for advertising, fingerprints, hashed
email, or another persistent fallback identifier.

## Location and IP address

Supply location only when the host has an appropriate lawful basis, platform
permission, and privacy state. Omit precise coordinates when they are not
permitted.

Do not perform an external public-IP lookup on the device solely to populate
an ad request. Prefer server-side inference or a trusted server-provided
value.

## Apple privacy manifest

The iOS distribution includes `PrivacyInfo.xcprivacy`. It declares same-app
and optional App Group `UserDefaults` access, the built-in
`2.e.serverbid.com` tracking domain, and the SDK's conditional collection of
host-supplied device identifiers, advertising interaction data, and privacy
signals. Device identifiers remain prohibited unless the resolved privacy
state and ATT permit them.

Publishers that configure raw VAST URLs or additional demand partners are
responsible for declaring any additional tracking domains and reconciling the
SDK manifest with their complete App Store Connect privacy disclosures.

## Raw VAST tags

Raw VAST URLs are supported only for QA tags or explicitly supported
non-Consumable ad servers. Do not place user data, identifiers, consent
strings, credentials, or secrets in a raw tag URL.
