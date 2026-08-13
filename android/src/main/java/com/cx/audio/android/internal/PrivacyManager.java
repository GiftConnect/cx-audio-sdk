package com.cx.audio.android.internal;

import android.content.Context;
import android.content.SharedPreferences;

import com.cx.audio.android.CxAgeTreatment;
import com.cx.audio.android.CxAudioConfiguration;
import com.cx.audio.android.CxPrivacyOverride;
import com.cx.audio.android.CxPrivacyResolutionStatus;
import com.cx.audio.android.CxPrivacyState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PrivacyManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final PrivacyManager INSTANCE = new PrivacyManager();
    private final Object lock = new Object();
    private volatile CxPrivacyState snapshot = CxPrivacyState.restrictive(0);
    private CxAudioConfiguration configuration = CxAudioConfiguration.builder().build();
    private SharedPreferences preferences;
    private long generation;

    public static PrivacyManager getInstance() { return INSTANCE; }
    private PrivacyManager() {}

    public void initialize(Context context, CxAudioConfiguration configuration) {
        if (context == null) throw new IllegalArgumentException("context must not be null");
        Context app = context.getApplicationContext();
        synchronized (lock) {
            this.configuration = configuration == null
                    ? CxAudioConfiguration.builder().build() : configuration;
            if (preferences != null) preferences.unregisterOnSharedPreferenceChangeListener(this);
            preferences = app.getSharedPreferences(app.getPackageName() + "_preferences", Context.MODE_PRIVATE);
            preferences.registerOnSharedPreferenceChangeListener(this);
        }
        resolve();
    }

    public CxPrivacyState getSnapshot() { return snapshot; }

    public void updatePrivacyOverride(CxPrivacyOverride privacyOverride) {
        synchronized (lock) {
            configuration = CxAudioConfiguration.builder()
                    .ageTreatment(configuration.getAgeTreatment())
                    .privacyOverride(privacyOverride)
                    .build();
        }
        resolve();
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null && (key.startsWith("IAB") || key.startsWith("gad_"))) resolve();
    }

    public void resolve() {
        final SharedPreferences prefs;
        final CxAudioConfiguration config;
        synchronized (lock) { prefs = preferences; config = configuration; }
        if (prefs == null) return;
        Map<String, ?> values = prefs.getAll();
        final long nextGeneration;
        synchronized (lock) { nextGeneration = ++generation; }
        CxPrivacyState resolvedState = resolve(
                config, values, nextGeneration, System.currentTimeMillis());
        snapshot = resolvedState;
    }

    static CxPrivacyState resolve(
            CxAudioConfiguration config, Map<String, ?> values, long generation, long resolvedAtMillis) {
        CxPrivacyOverride override = config.getPrivacyOverride();
        Boolean gdpr = first(override == null ? null : override.getGdprApplies(),
                bool(values.get("IABTCF_gdprApplies")), bool(values.get("IABConsent_SubjectToGDPR")));
        String tc = firstConsent(override == null ? null : override.getTcfString(),
                string(values.get("IABTCF_TCString")), string(values.get("IABConsent_ConsentString")));
        String purposes = binary(string(values.get("IABTCF_PurposeConsents")));
        boolean purposeOneTreatment = Boolean.TRUE.equals(bool(values.get("IABTCF_PurposeOneTreatment")));
        String additional = firstAdditional(override == null ? null : override.getAdditionalConsent(),
                string(values.get("IABTCF_AddtlConsent")));
        String gpp = firstGpp(override == null ? null : override.getGppString(),
                string(values.get("IABGPP_HDR_GppString")));
        List<Integer> sids = override != null && !override.getGppSectionIds().isEmpty()
                ? sectionIds(override.getGppSectionIds()) : sectionIds(values.get("IABGPP_GppSID"));
        String usp = firstUsPrivacy(override == null ? null : override.getUsPrivacyString(),
                string(values.get("IABUSPrivacy_String")));
        Boolean usOptOut = first(override == null ? null : override.getUsPrivacyOptOut(),
                usOptOut(usp), bool(values.get("gad_rdp")));
        Boolean cookies = bool(values.get("gad_has_consent_for_cookies"));
        boolean ageRestricted = config.getAgeTreatment() == CxAgeTreatment.CHILD_DIRECTED
                || config.getAgeTreatment() == CxAgeTreatment.UNDER_AGE_OF_CONSENT;
        boolean tcfAllows = gdpr != Boolean.TRUE || (tc != null && purposes != null
                && purposes.length() >= 4 && "1111".equals(purposes.substring(0, 4)) && !purposeOneTreatment);
        boolean explicitAllows = override == null || override.getPersonalizedAdsAllowed() == null
                || override.getPersonalizedAdsAllowed();
        boolean ageResolved = config.getAgeTreatment() != CxAgeTreatment.UNSPECIFIED;
        boolean regionalSignalsResolved = gdpr != null && usOptOut != null;
        boolean personalized = ageResolved && !ageRestricted && regionalSignalsResolved && explicitAllows && tcfAllows
                && !Boolean.TRUE.equals(usOptOut) && !Boolean.FALSE.equals(cookies);
        boolean resolved = ageResolved && regionalSignalsResolved;
        CxPrivacyResolutionStatus status = ageRestricted ? CxPrivacyResolutionStatus.RESTRICTED
                : (resolved ? CxPrivacyResolutionStatus.RESOLVED : CxPrivacyResolutionStatus.UNRESOLVED);
        // Android has no ATT-equivalent platform gate. A host-supplied advertising identifier
        // may be forwarded only after age and regional privacy signals resolve permissively.
        // The core still does not collect identifiers itself.
        return new CxPrivacyState(status, config.getAgeTreatment(), gdpr, usOptOut,
                personalized, personalized, false, tc, additional, gpp, sids, usp,
                generation, resolvedAtMillis);
    }

    @SafeVarargs private static <T> T first(T... values) {
        for (T value : values) if (value != null) return value;
        return null;
    }
    private static String firstConsent(String... values) {
        for (String value : values) { String cleaned = consent(value); if (cleaned != null) return cleaned; }
        return null;
    }
    private static String firstAdditional(String... values) {
        for (String value : values) { String cleaned = additional(value); if (cleaned != null) return cleaned; }
        return null;
    }
    private static String firstGpp(String... values) {
        for (String value : values) { String cleaned = gpp(value); if (cleaned != null) return cleaned; }
        return null;
    }
    private static String firstUsPrivacy(String... values) {
        for (String value : values) { String cleaned = usPrivacy(value); if (cleaned != null) return cleaned; }
        return null;
    }
    private static String clean(String value) {
        if (value == null) return null;
        String result = value.trim(); return result.isEmpty() ? null : result;
    }
    private static String binary(String value) {
        value = clean(value); return value != null && value.matches("[01]+") ? value : null;
    }
    private static String string(Object value) { return value instanceof String ? (String) value : null; }
    private static String consent(String value) {
        value = clean(value);
        return value != null && value.length() <= 8192 && value.matches("[A-Za-z0-9_.-]+") ? value : null;
    }
    private static String additional(String value) {
        value = clean(value);
        return value != null && value.length() <= 8192 && value.contains("~")
                && value.matches("[0-9.~]+") ? value : null;
    }
    private static String gpp(String value) {
        value = clean(value);
        return value != null && value.length() <= 8192 && value.matches("[A-Za-z0-9_.~-]+") ? value : null;
    }
    private static Boolean bool(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        if (value instanceof String) {
            switch (((String) value).trim().toLowerCase(Locale.US)) {
                case "1": case "true": case "yes": case "y": return true;
                case "0": case "false": case "no": case "n": return false;
                default: return null;
            }
        }
        return null;
    }
    private static Boolean usOptOut(String value) {
        value = usPrivacy(value);
        if (value == null) return null;
        return Character.toUpperCase(value.charAt(2)) == 'Y';
    }
    private static String usPrivacy(String value) {
        value = clean(value);
        if (value == null) return null;
        value = value.toUpperCase(Locale.US);
        return value.length() == 4 && value.charAt(0) == '1' && value.matches("[1YNA-]{4}") ? value : null;
    }
    private static List<Integer> sectionIds(Object value) {
        if (value instanceof List) {
            List<Integer> result = new ArrayList<>();
            for (Object item : (List<?>) value) if (item instanceof Number && ((Number) item).intValue() > 0)
                result.add(((Number) item).intValue());
            return result;
        }
        String text = value instanceof String ? (String) value : null;
        if (text == null) return Collections.emptyList();
        List<Integer> result = new ArrayList<>();
        for (String item : text.split("[_,]")) try { int id = Integer.parseInt(item); if (id > 0) result.add(id); }
        catch (NumberFormatException ignored) {}
        return result;
    }
}
