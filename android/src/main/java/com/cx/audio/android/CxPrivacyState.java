package com.cx.audio.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CxPrivacyState {
    private final CxPrivacyResolutionStatus status;
    private final CxAgeTreatment ageTreatment;
    private final Boolean gdprApplies;
    private final Boolean usPrivacyOptOut;
    private final boolean deviceIdentifiersAllowed;
    private final boolean personalizedAdsAllowed;
    private final boolean preciseLocationAllowed;
    private final String tcfString;
    private final String additionalConsent;
    private final String gppString;
    private final List<Integer> gppSectionIds;
    private final String usPrivacyString;
    private final long generation;
    private final long resolvedAtMillis;

    public CxPrivacyState(
            CxPrivacyResolutionStatus status,
            CxAgeTreatment ageTreatment,
            Boolean gdprApplies,
            Boolean usPrivacyOptOut,
            boolean deviceIdentifiersAllowed,
            boolean personalizedAdsAllowed,
            boolean preciseLocationAllowed,
            String tcfString,
            String additionalConsent,
            String gppString,
            List<Integer> gppSectionIds,
            String usPrivacyString,
            long generation,
            long resolvedAtMillis) {
        this.status = status;
        this.ageTreatment = ageTreatment;
        this.gdprApplies = gdprApplies;
        this.usPrivacyOptOut = usPrivacyOptOut;
        this.deviceIdentifiersAllowed = deviceIdentifiersAllowed;
        this.personalizedAdsAllowed = personalizedAdsAllowed;
        this.preciseLocationAllowed = preciseLocationAllowed;
        this.tcfString = tcfString;
        this.additionalConsent = additionalConsent;
        this.gppString = gppString;
        this.gppSectionIds = Collections.unmodifiableList(new ArrayList<>(gppSectionIds));
        this.usPrivacyString = usPrivacyString;
        this.generation = generation;
        this.resolvedAtMillis = resolvedAtMillis;
    }

    public static CxPrivacyState restrictive(long generation) {
        return new CxPrivacyState(CxPrivacyResolutionStatus.UNRESOLVED, CxAgeTreatment.UNSPECIFIED,
                null, null, false, false, false, null, null, null,
                Collections.emptyList(), null, generation, System.currentTimeMillis());
    }

    public CxPrivacyResolutionStatus getStatus() { return status; }
    public CxAgeTreatment getAgeTreatment() { return ageTreatment; }
    public Boolean getGdprApplies() { return gdprApplies; }
    public Boolean getUsPrivacyOptOut() { return usPrivacyOptOut; }
    public boolean isDeviceIdentifiersAllowed() { return deviceIdentifiersAllowed; }
    public boolean isPersonalizedAdsAllowed() { return personalizedAdsAllowed; }
    public boolean isPreciseLocationAllowed() { return preciseLocationAllowed; }
    public String getTcfString() { return tcfString; }
    public String getAdditionalConsent() { return additionalConsent; }
    public String getGppString() { return gppString; }
    public List<Integer> getGppSectionIds() { return gppSectionIds; }
    public String getUsPrivacyString() { return usPrivacyString; }
    public long getGeneration() { return generation; }
    public long getResolvedAtMillis() { return resolvedAtMillis; }
}
