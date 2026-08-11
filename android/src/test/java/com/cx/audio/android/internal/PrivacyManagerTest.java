package com.cx.audio.android.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cx.audio.android.CxAgeTreatment;
import com.cx.audio.android.CxAudioConfiguration;
import com.cx.audio.android.CxPrivacyOverride;
import com.cx.audio.android.CxPrivacyResolutionStatus;
import com.cx.audio.android.CxPrivacyState;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public final class PrivacyManagerTest {
    @Test
    public void missingSignalsRemainUnresolvedAndRestrictive() {
        CxPrivacyState state = resolve(CxAgeTreatment.GENERAL_AUDIENCE, new HashMap<>());
        assertEquals(CxPrivacyResolutionStatus.UNRESOLVED, state.getStatus());
        assertFalse(state.isPersonalizedAdsAllowed());
        assertFalse(state.isDeviceIdentifiersAllowed());
        assertFalse(state.isPreciseLocationAllowed());
    }

    @Test
    public void completeRegionalSignalsCanAllowPersonalizationButNotIdentifiersOrLocation() {
        Map<String, Object> values = permissiveSignals();
        CxPrivacyState state = resolve(CxAgeTreatment.GENERAL_AUDIENCE, values);
        assertEquals(CxPrivacyResolutionStatus.RESOLVED, state.getStatus());
        assertTrue(state.isPersonalizedAdsAllowed());
        assertFalse(state.isDeviceIdentifiersAllowed());
        assertFalse(state.isPreciseLocationAllowed());
    }

    @Test
    public void childTreatmentOverridesPermissiveOverride() {
        CxAudioConfiguration config = CxAudioConfiguration.builder()
                .ageTreatment(CxAgeTreatment.CHILD_DIRECTED)
                .privacyOverride(CxPrivacyOverride.builder().personalizedAdsAllowed(true).build())
                .build();
        CxPrivacyState state = PrivacyManager.resolve(config, permissiveSignals(), 3, 10);
        assertEquals(CxPrivacyResolutionStatus.RESTRICTED, state.getStatus());
        assertFalse(state.isPersonalizedAdsAllowed());
    }

    @Test
    public void malformedStoredValuesDoNotCrashOrReachSnapshot() {
        Map<String, Object> values = new HashMap<>();
        values.put("IABTCF_gdprApplies", true);
        values.put("IABTCF_TCString", "not valid!");
        values.put("IABTCF_PurposeConsents", "1111");
        values.put("IABTCF_AddtlConsent", "bad consent");
        values.put("IABGPP_HDR_GppString", "bad gpp!");
        values.put("IABUSPrivacy_String", 42);
        CxPrivacyState state = resolve(CxAgeTreatment.GENERAL_AUDIENCE, values);
        assertEquals(CxPrivacyResolutionStatus.UNRESOLVED, state.getStatus());
        assertNull(state.getTcfString());
        assertNull(state.getAdditionalConsent());
        assertNull(state.getGppString());
        assertNull(state.getUsPrivacyString());
        assertFalse(state.isPersonalizedAdsAllowed());
    }

    @Test
    public void gdprPurposeDenialBlocksPersonalization() {
        Map<String, Object> values = permissiveSignals();
        values.put("IABTCF_gdprApplies", true);
        values.put("IABTCF_TCString", "COwK6gaOwK6gaFmAAAENAPCAAAAAAAAAAAAAAAAAAAAA.IFMsv_Z_G____bvQXQ1f9eY1f9_z_q7ff9_3-8_v9v9zLv9____39nP___9v-_1fn9____z-j_3_f__9P");
        values.put("IABTCF_PurposeConsents", "1011");
        assertFalse(resolve(CxAgeTreatment.GENERAL_AUDIENCE, values).isPersonalizedAdsAllowed());
    }

    private static CxPrivacyState resolve(CxAgeTreatment age, Map<String, ?> values) {
        return PrivacyManager.resolve(CxAudioConfiguration.builder().ageTreatment(age).build(), values, 1, 1);
    }

    private static Map<String, Object> permissiveSignals() {
        Map<String, Object> values = new HashMap<>();
        values.put("IABTCF_gdprApplies", false);
        values.put("IABUSPrivacy_String", "1NNN");
        values.put("gad_has_consent_for_cookies", true);
        return values;
    }
}
