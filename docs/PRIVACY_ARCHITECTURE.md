# Privacy and Consent Architecture

## Status

Accepted architectural constraints. These rules are mandatory for all SDK design and implementation work unless explicitly superseded by a later architectural decision.

## Ownership Boundary

The SDK must not implement, bundle, or display its own Consent Management Platform (CMP) or any user-facing consent dialog.

The host application owns:

* Selecting and integrating a CMP.
* Collecting consent and displaying GDPR, US-state, and other regional privacy notices.
* Presenting Apple's App Tracking Transparency (ATT) prompt when appropriate.
* Declaring COPPA, child-directed treatment, and under-age-of-consent status using semantically correct APIs.
* Requesting location permission.
* Configuring any required App Group or equivalent shared-storage capability.
* Providing publisher IDs, placement IDs, and other business-level configuration.
* Initializing the SDK.
* Configuring the application's global audio session and app-level audio behavior.

The SDK owns:

* Privacy-signal ingestion and change observation.
* Resolution into one authoritative privacy state.
* Enforcement and request sanitization.
* OpenRTB, VAST, and other downstream privacy signaling.
* Updating active privacy behavior when signals change without requiring an application restart.

A normal integration must not require the host application to parse or manually forward standard IAB consent strings.

## CMP-Agnostic Signal Ingestion

The SDK must be CMP-agnostic. Compatibility must be based primarily on standards and stored signals, not vendor-specific UI or SDK APIs. Do not add a hard dependency on a particular CMP unless unavoidable and explicitly approved.

Automatically read and observe applicable standard and platform values, including:

* IAB TCF: `IABTCF_gdprApplies`, `IABTCF_TCString`, `IABTCF_PurposeConsents`, `IABTCF_AddtlConsent`, and `IABTCF_PurposeOneTreatment`.
* Legacy TCF fallback: `IABConsent_SubjectToGDPR` and `IABConsent_ConsentString`.
* IAB GPP: `IABGPP_HDR_Version`, `IABGPP_HDR_Sections`, `IABGPP_HDR_GppString`, `IABGPP_GppSID`, and supported section keys such as `IABGPP_2_String`. New GPP sections must be addable without redesigning the public API.
* Legacy US Privacy: `IABUSPrivacy_String`.
* Additional Consent and relevant Google values, including `gad_rdp` and `gad_has_consent_for_cookies` where applicable.
* Apple ATT state through `ATTrackingManager.trackingAuthorizationStatus`.
* Relevant platform privacy values on each supported platform.
* Explicit, semantically correct COPPA, child-directed, and under-age-of-consent declarations.

Do not infer child or underage status from unrelated privacy flags.

The SDK may interoperate automatically with CMPs such as Google UMP, OneTrust, Sourcepoint, Didomi, Usercentrics, Quantcast Choice, TrustArc, and Admiral when they correctly write supported standard values. This list does not justify vendor coupling or imply certification.

## Independent Privacy Dimensions and Resolution

ATT, GDPR consent, US privacy consent, and COPPA or underage restrictions are independent inputs. Never treat one as proof of another.

Examples:

* ATT authorized with GDPR consent denied must not enable personalization or tracking.
* GDPR consent granted with ATT denied must not enable IDFA access.
* COPPA or underage treatment remains restrictive even when every other signal grants consent.

Resolve inputs conceptually in this order:

1. COPPA and underage restrictions.
2. Explicit publisher declarations or supported overrides.
3. GPP.
4. TCF v2.x and Additional Consent.
5. Legacy TCF and US Privacy values.
6. A conservative default.

COPPA and underage restrictions are absolute and override all other signals. Programmatic overrides must never bypass COPPA, underage restrictions, platform restrictions, or mandatory sanitization.

When valid inputs conflict, apply the most restrictive applicable result. When required information is absent, malformed, stale, contradictory, or unresolved, remain privacy-restrictive until it is resolved.

## Normalized Privacy State

Convert all privacy inputs into one normalized, immutable state before constructing any request. It should model, at minimum:

* COPPA and underage applicability.
* GDPR applicability and consent outcome.
* US privacy applicability and opt-out outcome.
* ATT or equivalent platform tracking authorization.
* Device-identifier access.
* Personalized-ad eligibility.
* Precise-location eligibility.
* TCF, Additional Consent, GPP, GPP section IDs, and legacy US Privacy strings when permitted and needed downstream.
* State version, generation, and resolution status.

The exact platform types may differ, but there must be one authoritative resolved representation with equivalent semantics. Do not duplicate resolution logic across ad formats, bridges, or network clients.

## Centralized Privacy Enforcement

Every outbound ad, tracking, click, VAST, OpenRTB, reporting, measurement, asset, and redirected request must pass through one centralized privacy-enforcement layer. No networking path, platform bridge, ad format, wrapper redirect, media loader, or downstream adapter may bypass it.

This includes:

* Ad and bid requests.
* Initial and wrapper VAST requests.
* Impression, start, quartile, completion, error, companion, and click tracking.
* Click-through navigation or requests controlled by the SDK.
* Reporting and measurement calls.
* Redirected tracking URLs and creative asset requests where privacy-bearing request data can be transmitted.

Based on the resolved state, the enforcement layer must modify actual behavior rather than merely forwarding consent strings. It must, as applicable:

* Remove IDFA, GAID/AAID, IFA, user IDs, buyer IDs, cookies, and prohibited local identifiers.
* Disable tracking identifiers and prevent adapters from reintroducing them.
* Suppress precise location or reduce it to permitted coarse precision.
* Apply IP anonymization when the SDK controls the transmitted IP value.
* Force contextual or non-personalized behavior.
* Populate appropriate OpenRTB privacy fields.
* Resolve, insert, or clear VAST privacy macros.
* Sanitize URLs, headers, payloads, and redirects before transmission.

## App Groups, Extensions, and Background Audio

App Group synchronization is for genuine cross-sandbox use cases, such as an app extension. Ordinary in-process background threads and background audio do not require shared storage merely because they run off the main thread.

When App Group support is configured and required:

* Observe supported values in the host process's standard storage.
* Resolve the normalized privacy state in the host process.
* Persist a versioned, precomputed immutable snapshot into the shared App Group.
* Include a timestamp or generation so stale snapshots are detectable.
* Let extensions read the snapshot without reparsing every raw consent string.
* Fail closed when the shared snapshot is absent, invalid, stale, or incompatible.

Raw values may be mirrored where necessary, but extension and background request paths should prefer the precomputed normalized snapshot.

## Threading, Initialization, and Runtime Updates

Privacy ingestion, resolution, and initialization must be thread-safe. SDK initialization must be idempotent, minimal, predictable, safe to call more than once, asynchronous where disk or network work is involved, and privacy-restrictive until consent has been resolved.

Prefer immutable configuration after initialization while allowing runtime privacy updates. Resolve and cache privacy state outside latency-sensitive playback paths, publish immutable snapshots, avoid blocking the main thread, avoid locks and parsing in real-time audio callbacks, and provide constant-time access to the latest state during request construction.

Initialization must never silently begin personalized tracking before privacy state is available.

## Public API Boundary

The normal integration should require only SDK configuration, initialization, ad loading, and playback. A Hello World integration must not require manual consent-string parsing, OpenRTB construction, VAST URL construction, tracking-pixel delivery, demand-endpoint knowledge, or secure-transport configuration.

Expose only strongly typed values that represent legitimate publisher decisions. Appropriate public configuration may include:

* Publisher and placement identifiers.
* Supported ad format, test mode, logging level, timeout, and approved caching behavior.
* App Group identifier.
* Explicitly supported visual customization.
* COPPA and underage declarations.
* Explicit privacy overrides for custom CMPs, testing, and approved enterprise integrations.

Explicit consent APIs must exist only for custom CMP integrations, testing, enterprise use cases, and age-related declarations. They must be strongly typed, documented, thread-safe, explicit about precedence, and removable without leaving stale state. Avoid generic dictionaries.

Do not expose internal protocol, transport, security, or enforcement details as publisher configuration, including:

* HTTPS or secure-transport flags.
* Demand endpoint internals.
* Raw OpenRTB fields.
* Raw VAST construction options outside explicitly supported QA or non-Consumable escape hatches.
* Internal tracking behavior.
* Fixed creative dimensions, protocol lists, or internal padding not part of the supported product API.
* Identifier-scrubbing controls or privacy-enforcement bypasses.

HTTPS and privacy-safe behavior are mandatory implementation details, not options.

## Tracking and Ad-Format Boundaries

Impression and playback tracking are core SDK responsibilities. Supported formats must correctly handle applicable impression, start, first quartile, midpoint, third quartile, complete, click, close or dismiss, pause, resume, mute, unmute, and error events.

Tracking must obey resolved privacy state, support every valid VAST tracking URL, fire at most once where required, survive supported background transitions, avoid duplicates during retry or restoration, and have deterministic tests. Missing impression or quartile tracking is release-blocking.

Each ad format must explicitly define accepted demand or VAST response types, required visual assets, companion and click behavior, close and dismissal behavior, tracking, and missing-asset fallback. Do not assume an audio response is renderable as a video or visual interstitial. Keep multi-mode negotiation internal and tested instead of exposing ambiguous low-level controls.

## Sample Applications and Documentation

Maintain a minimal sample that treats the SDK as a black box and clearly demonstrates configuration, initialization, loading, and playback. Keep raw tags, protocol fields, consent-string inputs, and other low-level controls on a separate advanced or debug path when they remain necessary for QA.

Documentation and primary sample flows must not instruct ordinary publishers to retrieve advertising identifiers, parse CMP storage, manually pass standard consent strings, build VAST URLs, or configure internal transport behavior.

## Apple Privacy Manifest

The iOS SDK must include and maintain an accurate `PrivacyInfo.xcprivacy` describing its actual required-reason API use, tracking behavior and domains, and collected data types where applicable.

Verify every declaration and required-reason code against current Apple documentation and the implementation before release. Do not copy codes from architectural notes, and ensure declared tracking domains match real network behavior.

## Privacy Test Requirements

Privacy behavior requires deterministic automated coverage across native implementations and bridge-level contract tests. At minimum, test:

* COPPA and underage restrictions overriding every other signal.
* GDPR applicability with complete consent, Purpose 1 denied, and personalization purposes denied.
* ATT authorized, denied, restricted, and not determined independently of CMP consent.
* US opt-out and Google limited-data-processing behavior.
* Missing, malformed, stale, and contradictory TCF, Additional Consent, GPP, legacy, and platform values.
* Explicit override precedence and clearing.
* Runtime consent changes.
* App Group snapshot creation, reading, versioning, staleness, and corruption.
* Identifier and location removal.
* OpenRTB mapping and VAST macro resolution.
* Impression, click, and quartile tracking.
* Every outbound networking path passing through the enforcement gateway.

Tests must assert final outbound payloads, headers, and URLs, not only intermediate resolved state.

## Agent Implementation Rules

When modifying this SDK:

* Do not introduce a built-in CMP or user-facing consent UI.
* Do not add or retain a network path that bypasses centralized privacy enforcement.
* Do not duplicate privacy logic inside ad formats or platform bridges.
* Do not treat ATT authorization as GDPR or US privacy consent.
* Do not treat forwarding a consent string as enforcement.
* Do not permit overrides to bypass COPPA, underage restrictions, ATT, or sanitization.
* Do not perform blocking privacy work in audio-render callbacks.
* Do not assume App Groups are necessary for ordinary in-process background work.
* Do not expose transport, protocol, endpoint, or privacy-bypass internals as normal public configuration.
* Do not claim support for a privacy standard until outbound behavior and tests support it.
* Prefer strongly typed APIs, immutable configuration, and immutable resolved snapshots.
* Add or update tests whenever privacy, request construction, tracking, or public API behavior changes.

Use this priority order when constraints conflict:

1. Legal and platform compliance.
2. User privacy.
3. Correct impression and playback measurement.
4. Safe and deterministic behavior.
5. Audio playback stability.
6. Minimal publisher integration.
7. Backward compatibility.
8. Extensibility.
9. Convenience.
