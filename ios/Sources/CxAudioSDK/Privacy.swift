import Foundation
#if canImport(UIKit)
import UIKit
#endif
#if canImport(AppTrackingTransparency)
import AppTrackingTransparency
#endif

public enum CxAgeTreatment: String, Sendable {
    case unspecified
    case generalAudience
    case childDirected
    case underAgeOfConsent
}

public enum CxPrivacyResolutionStatus: String, Sendable {
    case unresolved
    case resolved
    case restricted
}

public enum CxTrackingAuthorization: String, Sendable {
    case authorized
    case denied
    case restricted
    case notDetermined
    case unavailable
}

public struct CxPrivacyOverride: Equatable, Sendable {
    public var gdprApplies: Bool?
    public var personalizedAdsAllowed: Bool?
    public var usPrivacyOptOut: Bool?
    public var tcfString: String?
    public var additionalConsent: String?
    public var gppString: String?
    public var gppSectionIds: [Int]?
    public var usPrivacyString: String?

    public init(
        gdprApplies: Bool? = nil,
        personalizedAdsAllowed: Bool? = nil,
        usPrivacyOptOut: Bool? = nil,
        tcfString: String? = nil,
        additionalConsent: String? = nil,
        gppString: String? = nil,
        gppSectionIds: [Int]? = nil,
        usPrivacyString: String? = nil
    ) {
        self.gdprApplies = gdprApplies
        self.personalizedAdsAllowed = personalizedAdsAllowed
        self.usPrivacyOptOut = usPrivacyOptOut
        self.tcfString = tcfString
        self.additionalConsent = additionalConsent
        self.gppString = gppString
        self.gppSectionIds = gppSectionIds
        self.usPrivacyString = usPrivacyString
    }
}

public struct CxAudioConfiguration: Equatable, Sendable {
    public var ageTreatment: CxAgeTreatment
    public var appGroupIdentifier: String?
    public var privacyOverride: CxPrivacyOverride?

    public init(
        ageTreatment: CxAgeTreatment = .unspecified,
        appGroupIdentifier: String? = nil,
        privacyOverride: CxPrivacyOverride? = nil
    ) {
        self.ageTreatment = ageTreatment
        self.appGroupIdentifier = appGroupIdentifier
        self.privacyOverride = privacyOverride
    }
}

public struct CxPrivacyState: Equatable, Sendable {
    public let status: CxPrivacyResolutionStatus
    public let ageTreatment: CxAgeTreatment
    public let gdprApplies: Bool?
    public let usPrivacyOptOut: Bool?
    public let trackingAuthorization: CxTrackingAuthorization
    public let deviceIdentifiersAllowed: Bool
    public let personalizedAdsAllowed: Bool
    public let preciseLocationAllowed: Bool
    public let tcfString: String?
    public let additionalConsent: String?
    public let gppString: String?
    public let gppSectionIds: [Int]
    public let usPrivacyString: String?
    public let generation: UInt64
    public let resolvedAt: Date

    static func restrictive(generation: UInt64 = 0) -> CxPrivacyState {
        CxPrivacyState(
            status: .unresolved,
            ageTreatment: .unspecified,
            gdprApplies: nil,
            usPrivacyOptOut: nil,
            trackingAuthorization: .notDetermined,
            deviceIdentifiersAllowed: false,
            personalizedAdsAllowed: false,
            preciseLocationAllowed: false,
            tcfString: nil,
            additionalConsent: nil,
            gppString: nil,
            gppSectionIds: [],
            usPrivacyString: nil,
            generation: generation,
            resolvedAt: Date()
        )
    }
}

public enum CxAudio {
    public static func initialize(configuration: CxAudioConfiguration = CxAudioConfiguration()) {
        PrivacyManager.shared.initialize(configuration: configuration)
    }

    public static func updatePrivacyOverride(_ privacyOverride: CxPrivacyOverride?) {
        PrivacyManager.shared.updatePrivacyOverride(privacyOverride)
    }

    public static var privacyState: CxPrivacyState {
        PrivacyManager.shared.snapshot
    }

    /// Reads a precomputed snapshot for an app extension. Invalid, incompatible, or stale
    /// snapshots fail closed and return an unresolved restrictive state.
    public static func sharedPrivacyState(
        appGroupIdentifier: String,
        maxAge: TimeInterval = 300
    ) -> CxPrivacyState {
        PrivacyManager.sharedSnapshot(appGroupIdentifier: appGroupIdentifier, maxAge: maxAge)
    }
}

internal final class PrivacyManager: @unchecked Sendable {
    static let shared = PrivacyManager()

    private enum Key {
        static let gdprApplies = "IABTCF_gdprApplies"
        static let tcString = "IABTCF_TCString"
        static let purposeConsents = "IABTCF_PurposeConsents"
        static let additionalConsent = "IABTCF_AddtlConsent"
        static let purposeOneTreatment = "IABTCF_PurposeOneTreatment"
        static let legacyGdpr = "IABConsent_SubjectToGDPR"
        static let legacyConsent = "IABConsent_ConsentString"
        static let gppString = "IABGPP_HDR_GppString"
        static let gppSid = "IABGPP_GppSID"
        static let usPrivacy = "IABUSPrivacy_String"
        static let googleRdp = "gad_rdp"
        static let googleCookiesConsent = "gad_has_consent_for_cookies"
    }

    private let lock = NSLock()
    private var state = CxPrivacyState.restrictive()
    private var configuration = CxAudioConfiguration()
    private var generation: UInt64 = 0
    private var initialized = false
    private var observer: NSObjectProtocol?
    private var activationObserver: NSObjectProtocol?

    var snapshot: CxPrivacyState {
        lock.withLock { state }
    }

    func initialize(configuration: CxAudioConfiguration) {
        lock.withLock {
            self.configuration = configuration
            initialized = true
            if observer == nil {
                observer = NotificationCenter.default.addObserver(
                    forName: UserDefaults.didChangeNotification,
                    object: nil,
                    queue: nil
                ) { [weak self] _ in self?.resolve() }
            }
            #if canImport(UIKit)
            if activationObserver == nil {
                activationObserver = NotificationCenter.default.addObserver(
                    forName: UIApplication.didBecomeActiveNotification,
                    object: nil,
                    queue: nil
                ) { [weak self] _ in self?.resolve() }
            }
            #endif
        }
        resolve()
    }

    func updatePrivacyOverride(_ privacyOverride: CxPrivacyOverride?) {
        lock.withLock { configuration.privacyOverride = privacyOverride }
        resolve()
    }

    func resolve(defaults: UserDefaults = .standard, tracking: CxTrackingAuthorization? = nil) {
        let config = lock.withLock { configuration }
        let isInitialized = lock.withLock { initialized }
        guard isInitialized else { return }

        let override = config.privacyOverride
        let gdprApplies = override?.gdprApplies
            ?? Self.bool(defaults.object(forKey: Key.gdprApplies))
            ?? Self.bool(defaults.object(forKey: Key.legacyGdpr))
        let tcString = Self.consentString(override?.tcfString)
            ?? Self.consentString(defaults.string(forKey: Key.tcString))
            ?? Self.consentString(defaults.string(forKey: Key.legacyConsent))
        let purposeConsents = Self.binary(defaults.string(forKey: Key.purposeConsents))
        let purposeOneTreatment = Self.bool(defaults.object(forKey: Key.purposeOneTreatment)) ?? false
        let additionalConsent = Self.additionalConsent(override?.additionalConsent)
            ?? Self.additionalConsent(defaults.string(forKey: Key.additionalConsent))
        let gppString = Self.gppString(override?.gppString)
            ?? Self.gppString(defaults.string(forKey: Key.gppString))
        let gppSectionIds = override?.gppSectionIds
            ?? Self.sectionIds(defaults.object(forKey: Key.gppSid))
        let usPrivacyString = Self.usPrivacyString(override?.usPrivacyString)
            ?? Self.usPrivacyString(defaults.string(forKey: Key.usPrivacy))
        let usOptOut = override?.usPrivacyOptOut
            ?? Self.usOptOut(usPrivacyString)
            ?? Self.bool(defaults.object(forKey: Key.googleRdp))
        let googleCookiesConsent = Self.bool(defaults.object(forKey: Key.googleCookiesConsent))
        let att = tracking ?? Self.currentTrackingAuthorization()
        let ageRestricted = config.ageTreatment == .childDirected
            || config.ageTreatment == .underAgeOfConsent

        let tcfAllowsPersonalization: Bool
        if gdprApplies == true {
            // Purpose 1 and the common personalization purposes must be granted.
            tcfAllowsPersonalization = tcString != nil
                && purposeConsents.map { $0.count >= 4 && $0.prefix(4).allSatisfy { $0 == "1" } } == true
                && !purposeOneTreatment
        } else {
            tcfAllowsPersonalization = true
        }

        let explicitAllows = override?.personalizedAdsAllowed ?? true
        let platformAllowsIdentifiers = att == .authorized || att == .unavailable
        let ageResolved = config.ageTreatment != .unspecified
        let regionalSignalsResolved = gdprApplies != nil && usOptOut != nil
        let personalized = ageResolved
            && !ageRestricted
            && regionalSignalsResolved
            && explicitAllows
            && tcfAllowsPersonalization
            && usOptOut != true
            && googleCookiesConsent != false
        let platformResolved = att != .notDetermined
        let resolved = ageResolved && regionalSignalsResolved && platformResolved
        let status: CxPrivacyResolutionStatus = ageRestricted ? .restricted : (resolved ? .resolved : .unresolved)

        let newState: CxPrivacyState = lock.withLock {
            generation &+= 1
            let resolvedState = CxPrivacyState(
                status: status,
                ageTreatment: config.ageTreatment,
                gdprApplies: gdprApplies,
                usPrivacyOptOut: usOptOut,
                trackingAuthorization: att,
                deviceIdentifiersAllowed: personalized && platformAllowsIdentifiers,
                personalizedAdsAllowed: personalized,
                // Location permission is not consent to use precise location for advertising.
                // No normal SDK API currently establishes that separate purpose, so fail closed.
                preciseLocationAllowed: false,
                tcfString: tcString,
                additionalConsent: additionalConsent,
                gppString: gppString,
                gppSectionIds: gppSectionIds,
                usPrivacyString: usPrivacyString,
                generation: generation,
                resolvedAt: Date()
            )
            state = resolvedState
            return resolvedState
        }
        persistSnapshotIfNeeded(newState, appGroupIdentifier: config.appGroupIdentifier)
    }

    private func persistSnapshotIfNeeded(_ state: CxPrivacyState, appGroupIdentifier: String?) {
        guard let appGroupIdentifier = Self.clean(appGroupIdentifier),
              let defaults = UserDefaults(suiteName: appGroupIdentifier) else { return }
        defaults.set(1, forKey: "CxAudioPrivacySnapshotVersion")
        defaults.set(state.generation, forKey: "CxAudioPrivacySnapshotGeneration")
        defaults.set(state.resolvedAt.timeIntervalSince1970, forKey: "CxAudioPrivacySnapshotTimestamp")
        defaults.set(state.status.rawValue, forKey: "CxAudioPrivacySnapshotStatus")
        defaults.set(state.deviceIdentifiersAllowed, forKey: "CxAudioPrivacySnapshotIdentifiersAllowed")
        defaults.set(state.personalizedAdsAllowed, forKey: "CxAudioPrivacySnapshotPersonalizedAdsAllowed")
        defaults.set(state.preciseLocationAllowed, forKey: "CxAudioPrivacySnapshotPreciseLocationAllowed")
        defaults.set(state.ageTreatment.rawValue, forKey: "CxAudioPrivacySnapshotAgeTreatment")
        defaults.set(state.gdprApplies, forKey: "CxAudioPrivacySnapshotGdprApplies")
        defaults.set(state.usPrivacyOptOut, forKey: "CxAudioPrivacySnapshotUsPrivacyOptOut")
        defaults.set(state.trackingAuthorization.rawValue, forKey: "CxAudioPrivacySnapshotTrackingAuthorization")
        defaults.set(state.tcfString, forKey: "CxAudioPrivacySnapshotTcfString")
        defaults.set(state.additionalConsent, forKey: "CxAudioPrivacySnapshotAdditionalConsent")
        defaults.set(state.gppString, forKey: "CxAudioPrivacySnapshotGppString")
        defaults.set(state.gppSectionIds, forKey: "CxAudioPrivacySnapshotGppSectionIds")
        defaults.set(state.usPrivacyString, forKey: "CxAudioPrivacySnapshotUsPrivacyString")
    }

    static func sharedSnapshot(appGroupIdentifier: String, maxAge: TimeInterval) -> CxPrivacyState {
        guard maxAge >= 0,
              let identifier = clean(appGroupIdentifier),
              let defaults = UserDefaults(suiteName: identifier),
              defaults.integer(forKey: "CxAudioPrivacySnapshotVersion") == 1,
              let statusRaw = defaults.string(forKey: "CxAudioPrivacySnapshotStatus"),
              let status = CxPrivacyResolutionStatus(rawValue: statusRaw),
              let ageRaw = defaults.string(forKey: "CxAudioPrivacySnapshotAgeTreatment"),
              let age = CxAgeTreatment(rawValue: ageRaw),
              let trackingRaw = defaults.string(forKey: "CxAudioPrivacySnapshotTrackingAuthorization"),
              let tracking = CxTrackingAuthorization(rawValue: trackingRaw) else {
            return .restrictive()
        }
        let values = defaults.dictionaryRepresentation()
        guard let timestamp = values["CxAudioPrivacySnapshotTimestamp"] as? NSNumber,
              let generation = values["CxAudioPrivacySnapshotGeneration"] as? NSNumber,
              let identifiers = values["CxAudioPrivacySnapshotIdentifiersAllowed"] as? NSNumber,
              let personalized = values["CxAudioPrivacySnapshotPersonalizedAdsAllowed"] as? NSNumber,
              let preciseLocation = values["CxAudioPrivacySnapshotPreciseLocationAllowed"] as? NSNumber else {
            return .restrictive()
        }
        let resolvedAt = Date(timeIntervalSince1970: timestamp.doubleValue)
        let ageSeconds = Date().timeIntervalSince(resolvedAt)
        guard timestamp.doubleValue > 0, ageSeconds >= 0, ageSeconds <= maxAge else {
            return .restrictive(generation: generation.uint64Value)
        }
        return CxPrivacyState(
            status: status,
            ageTreatment: age,
            gdprApplies: bool(values["CxAudioPrivacySnapshotGdprApplies"]),
            usPrivacyOptOut: bool(values["CxAudioPrivacySnapshotUsPrivacyOptOut"]),
            trackingAuthorization: tracking,
            deviceIdentifiersAllowed: identifiers.boolValue,
            personalizedAdsAllowed: personalized.boolValue,
            preciseLocationAllowed: preciseLocation.boolValue,
            tcfString: consentString(values["CxAudioPrivacySnapshotTcfString"] as? String),
            additionalConsent: additionalConsent(values["CxAudioPrivacySnapshotAdditionalConsent"] as? String),
            gppString: gppString(values["CxAudioPrivacySnapshotGppString"] as? String),
            gppSectionIds: sectionIds(values["CxAudioPrivacySnapshotGppSectionIds"]),
            usPrivacyString: usPrivacyString(values["CxAudioPrivacySnapshotUsPrivacyString"] as? String),
            generation: generation.uint64Value,
            resolvedAt: resolvedAt
        )
    }

    private static func currentTrackingAuthorization() -> CxTrackingAuthorization {
        #if canImport(AppTrackingTransparency)
        if #available(iOS 14, *) {
            switch ATTrackingManager.trackingAuthorizationStatus {
            case .authorized: return .authorized
            case .denied: return .denied
            case .restricted: return .restricted
            case .notDetermined: return .notDetermined
            @unknown default: return .restricted
            }
        }
        #endif
        return .unavailable
    }

    private static func bool(_ value: Any?) -> Bool? {
        if let value = value as? Bool { return value }
        if let value = value as? NSNumber { return value.intValue != 0 }
        if let value = value as? String {
            switch value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
            case "1", "true", "yes", "y": return true
            case "0", "false", "no", "n": return false
            default: return nil
            }
        }
        return nil
    }

    private static func clean(_ value: String?) -> String? {
        guard let value else { return nil }
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    private static func consentString(_ value: String?) -> String? {
        validated(value, allowed: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-.")
    }

    private static func additionalConsent(_ value: String?) -> String? {
        guard let value = validated(value, allowed: "0123456789.~"), value.contains("~") else { return nil }
        return value
    }

    private static func gppString(_ value: String?) -> String? {
        validated(value, allowed: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.~-")
    }

    private static func validated(_ value: String?, allowed: String) -> String? {
        guard let value = clean(value), value.count <= 8_192,
              value.allSatisfy({ allowed.contains($0) }) else { return nil }
        return value
    }

    private static func binary(_ value: String?) -> String? {
        guard let value = clean(value), value.allSatisfy({ $0 == "0" || $0 == "1" }) else { return nil }
        return value
    }

    private static func sectionIds(_ value: Any?) -> [Int] {
        if let values = value as? [Int] { return values.filter { $0 > 0 } }
        if let values = value as? [NSNumber] { return values.map(\.intValue).filter { $0 > 0 } }
        if let value = value as? String {
            return value.split(whereSeparator: { $0 == "_" || $0 == "," })
                .compactMap { Int($0) }.filter { $0 > 0 }
        }
        return []
    }

    private static func usOptOut(_ value: String?) -> Bool? {
        guard let value = usPrivacyString(value) else { return nil }
        let characters = Array(value.uppercased())
        return characters[2] == "Y"
    }

    private static func usPrivacyString(_ value: String?) -> String? {
        guard let value = clean(value)?.uppercased(), value.count == 4, value.first == "1",
              value.allSatisfy({ "1YNA-".contains($0) }) else { return nil }
        return value
    }
}

private extension NSLock {
    func withLock<T>(_ body: () -> T) -> T {
        lock()
        defer { unlock() }
        return body()
    }
}
