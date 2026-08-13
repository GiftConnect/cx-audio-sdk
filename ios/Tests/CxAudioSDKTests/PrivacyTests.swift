import XCTest
@testable import CxAudioSDK

final class PrivacyTests: XCTestCase {
    func testUnresolvedStateIsRestrictive() {
        let state = CxPrivacyState.restrictive()
        XCTAssertEqual(state.status, .unresolved)
        XCTAssertFalse(state.deviceIdentifiersAllowed)
        XCTAssertFalse(state.personalizedAdsAllowed)
        XCTAssertFalse(state.preciseLocationAllowed)
    }

    func testChildDirectedTreatmentOverridesPermissiveSignals() throws {
        let defaults = try temporaryDefaults()
        defaults.set(0, forKey: "IABTCF_gdprApplies")
        defaults.set("1NNN", forKey: "IABUSPrivacy_String")
        let manager = PrivacyManager()
        manager.initialize(configuration: CxAudioConfiguration(
            ageTreatment: .childDirected,
            privacyOverride: CxPrivacyOverride(personalizedAdsAllowed: true)
        ))
        manager.resolve(defaults: defaults, tracking: .authorized)
        let state = manager.snapshot
        XCTAssertEqual(state.status, .restricted)
        XCTAssertFalse(state.deviceIdentifiersAllowed)
        XCTAssertFalse(state.personalizedAdsAllowed)
        XCTAssertFalse(state.preciseLocationAllowed)
    }

    func testMissingRegionalSignalsRemainRestrictive() throws {
        let manager = PrivacyManager()
        manager.initialize(configuration: CxAudioConfiguration(ageTreatment: .generalAudience))
        manager.resolve(defaults: try temporaryDefaults(), tracking: .authorized)
        XCTAssertFalse(manager.snapshot.personalizedAdsAllowed)
    }

    func testIndependentAttDenialBlocksIdentifiers() throws {
        let defaults = try temporaryDefaults()
        defaults.set(0, forKey: "IABTCF_gdprApplies")
        defaults.set("1NNN", forKey: "IABUSPrivacy_String")
        let manager = PrivacyManager()
        manager.initialize(configuration: CxAudioConfiguration(ageTreatment: .generalAudience))
        manager.resolve(defaults: defaults, tracking: .denied)
        XCTAssertFalse(manager.snapshot.deviceIdentifiersAllowed)
        XCTAssertTrue(manager.snapshot.personalizedAdsAllowed)
        XCTAssertFalse(manager.snapshot.preciseLocationAllowed)
    }

    func testAttNotDeterminedKeepsResolutionUnresolved() throws {
        let defaults = try temporaryDefaults()
        defaults.set(0, forKey: "IABTCF_gdprApplies")
        defaults.set("1NNN", forKey: "IABUSPrivacy_String")
        let manager = PrivacyManager()
        manager.initialize(configuration: CxAudioConfiguration(ageTreatment: .generalAudience))
        manager.resolve(defaults: defaults, tracking: .notDetermined)
        XCTAssertEqual(manager.snapshot.status, .unresolved)
        XCTAssertFalse(manager.snapshot.deviceIdentifiersAllowed)
    }

    func testMalformedStandardStringsAreNotPublished() throws {
        let defaults = try temporaryDefaults()
        defaults.set(1, forKey: "IABTCF_gdprApplies")
        defaults.set("not valid!", forKey: "IABTCF_TCString")
        defaults.set("1111", forKey: "IABTCF_PurposeConsents")
        defaults.set("bad consent", forKey: "IABTCF_AddtlConsent")
        defaults.set("bad gpp!", forKey: "IABGPP_HDR_GppString")
        defaults.set("bogus", forKey: "IABUSPrivacy_String")
        let manager = PrivacyManager()
        manager.initialize(configuration: CxAudioConfiguration(ageTreatment: .generalAudience))
        manager.resolve(defaults: defaults, tracking: .authorized)
        XCTAssertEqual(manager.snapshot.status, .unresolved)
        XCTAssertNil(manager.snapshot.tcfString)
        XCTAssertNil(manager.snapshot.additionalConsent)
        XCTAssertNil(manager.snapshot.gppString)
        XCTAssertNil(manager.snapshot.usPrivacyString)
        XCTAssertFalse(manager.snapshot.personalizedAdsAllowed)
    }

    func testEnforcerStripsIdentifiersAndLocationAndForcesRestrictiveSignals() throws {
        let input = try XCTUnwrap(URL(string:
            "https://ads.example.test/vast?idfa=device&ifa=device&lat=42&long=-71&custom=ok&lmt=0"))
        let output = try PrivacyEnforcer.enforce(input, purpose: .adRequest, state: .restrictive())
        let items = Dictionary(uniqueKeysWithValues:
            (URLComponents(url: output, resolvingAgainstBaseURL: false)?.queryItems ?? [])
                .map { ($0.name, $0.value ?? "") })
        XCTAssertNil(items["idfa"])
        XCTAssertNil(items["ifa"])
        XCTAssertNil(items["lat"])
        XCTAssertNil(items["long"])
        XCTAssertEqual(items["custom"], "ok")
        XCTAssertEqual(items["lmt"], "1")
        XCTAssertEqual(items["dnt"], "1")
    }

    func testEnforcerRejectsInsecureTransport() throws {
        let input = try XCTUnwrap(URL(string: "http://ads.example.test/vast"))
        XCTAssertThrowsError(try PrivacyEnforcer.enforce(input, purpose: .adRequest))
    }

    func testPublisherLimitedAdTrackingCannotBeRelaxedByPermissiveState() throws {
        let state = CxPrivacyState(
            status: .resolved,
            ageTreatment: .generalAudience,
            gdprApplies: false,
            usPrivacyOptOut: false,
            trackingAuthorization: .authorized,
            deviceIdentifiersAllowed: true,
            personalizedAdsAllowed: true,
            preciseLocationAllowed: false,
            tcfString: nil,
            additionalConsent: nil,
            gppString: nil,
            gppSectionIds: [],
            usPrivacyString: "1YNN",
            generation: 1,
            resolvedAt: Date()
        )
        let input = try XCTUnwrap(URL(string:
            "https://ads.example.test/vast?idfa=device&ifa=device&lmt=1"))
        let output = try PrivacyEnforcer.enforce(input, purpose: .adRequest, state: state)
        let items = Dictionary(uniqueKeysWithValues:
            (URLComponents(url: output, resolvingAgainstBaseURL: false)?.queryItems ?? [])
                .map { ($0.name, $0.value ?? "") })
        XCTAssertNil(items["idfa"])
        XCTAssertNil(items["ifa"])
        XCTAssertEqual(items["lmt"], "1")
    }

    func testClickCannotRetainPublisherSuppliedPrivacyControls() throws {
        let input = try XCTUnwrap(URL(string:
            "https://click.example.test/path?gdpr=0&gdpr_consent=secret&lmt=0&custom=ok"))
        let output = try PrivacyEnforcer.enforce(input, purpose: .clickThrough, state: .restrictive())
        let names = Set(URLComponents(url: output, resolvingAgainstBaseURL: false)?.queryItems?.map(\.name) ?? [])
        XCTAssertFalse(names.contains("gdpr"))
        XCTAssertFalse(names.contains("gdpr_consent"))
        XCTAssertFalse(names.contains("lmt"))
        XCTAssertTrue(names.contains("custom"))
    }

    func testAppGroupSnapshotRoundTripsAndFailsClosedWhenStaleOrCorrupt() throws {
        let suite = "group.CxAudioPrivacyTests.\(UUID().uuidString)"
        let shared = try XCTUnwrap(UserDefaults(suiteName: suite))
        shared.removePersistentDomain(forName: suite)
        addTeardownBlock { shared.removePersistentDomain(forName: suite) }
        let defaults = try temporaryDefaults()
        defaults.set(0, forKey: "IABTCF_gdprApplies")
        defaults.set("1NNN", forKey: "IABUSPrivacy_String")
        let manager = PrivacyManager()
        manager.initialize(configuration: CxAudioConfiguration(
            ageTreatment: .generalAudience,
            appGroupIdentifier: suite
        ))
        manager.resolve(defaults: defaults, tracking: .authorized)

        let current = PrivacyManager.sharedSnapshot(appGroupIdentifier: suite, maxAge: 300)
        XCTAssertEqual(current.generation, manager.snapshot.generation)
        XCTAssertEqual(current.status, .resolved)

        shared.set(Date().addingTimeInterval(-600).timeIntervalSince1970,
                   forKey: "CxAudioPrivacySnapshotTimestamp")
        XCTAssertEqual(PrivacyManager.sharedSnapshot(appGroupIdentifier: suite, maxAge: 300).status,
                       .unresolved)

        shared.set(99, forKey: "CxAudioPrivacySnapshotVersion")
        XCTAssertEqual(PrivacyManager.sharedSnapshot(appGroupIdentifier: suite, maxAge: 300).status,
                       .unresolved)
    }

    private func temporaryDefaults() throws -> UserDefaults {
        let name = "CxAudioPrivacyTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: name))
        defaults.removePersistentDomain(forName: name)
        addTeardownBlock { defaults.removePersistentDomain(forName: name) }
        return defaults
    }
}
