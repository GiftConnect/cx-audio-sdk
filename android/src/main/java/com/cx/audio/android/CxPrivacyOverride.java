package com.cx.audio.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CxPrivacyOverride {
    private final Boolean gdprApplies;
    private final Boolean personalizedAdsAllowed;
    private final Boolean usPrivacyOptOut;
    private final String tcfString;
    private final String additionalConsent;
    private final String gppString;
    private final List<Integer> gppSectionIds;
    private final String usPrivacyString;

    private CxPrivacyOverride(Builder builder) {
        gdprApplies = builder.gdprApplies;
        personalizedAdsAllowed = builder.personalizedAdsAllowed;
        usPrivacyOptOut = builder.usPrivacyOptOut;
        tcfString = builder.tcfString;
        additionalConsent = builder.additionalConsent;
        gppString = builder.gppString;
        gppSectionIds = Collections.unmodifiableList(new ArrayList<>(builder.gppSectionIds));
        usPrivacyString = builder.usPrivacyString;
    }

    public static Builder builder() { return new Builder(); }
    public Boolean getGdprApplies() { return gdprApplies; }
    public Boolean getPersonalizedAdsAllowed() { return personalizedAdsAllowed; }
    public Boolean getUsPrivacyOptOut() { return usPrivacyOptOut; }
    public String getTcfString() { return tcfString; }
    public String getAdditionalConsent() { return additionalConsent; }
    public String getGppString() { return gppString; }
    public List<Integer> getGppSectionIds() { return gppSectionIds; }
    public String getUsPrivacyString() { return usPrivacyString; }

    public static final class Builder {
        private Boolean gdprApplies;
        private Boolean personalizedAdsAllowed;
        private Boolean usPrivacyOptOut;
        private String tcfString;
        private String additionalConsent;
        private String gppString;
        private List<Integer> gppSectionIds = Collections.emptyList();
        private String usPrivacyString;

        public Builder gdprApplies(Boolean value) { gdprApplies = value; return this; }
        public Builder personalizedAdsAllowed(Boolean value) { personalizedAdsAllowed = value; return this; }
        public Builder usPrivacyOptOut(Boolean value) { usPrivacyOptOut = value; return this; }
        public Builder tcfString(String value) { tcfString = value; return this; }
        public Builder additionalConsent(String value) { additionalConsent = value; return this; }
        public Builder gppString(String value) { gppString = value; return this; }
        public Builder gppSectionIds(List<Integer> value) {
            gppSectionIds = value == null ? Collections.emptyList() : value;
            return this;
        }
        public Builder usPrivacyString(String value) { usPrivacyString = value; return this; }
        public CxPrivacyOverride build() { return new CxPrivacyOverride(this); }
    }
}
