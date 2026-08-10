package com.cx.audio.android;

public final class CxAudioConfiguration {
    private final CxAgeTreatment ageTreatment;
    private final CxPrivacyOverride privacyOverride;

    private CxAudioConfiguration(Builder builder) {
        ageTreatment = builder.ageTreatment;
        privacyOverride = builder.privacyOverride;
    }

    public static Builder builder() { return new Builder(); }
    public CxAgeTreatment getAgeTreatment() { return ageTreatment; }
    public CxPrivacyOverride getPrivacyOverride() { return privacyOverride; }

    public static final class Builder {
        private CxAgeTreatment ageTreatment = CxAgeTreatment.UNSPECIFIED;
        private CxPrivacyOverride privacyOverride;

        public Builder ageTreatment(CxAgeTreatment value) {
            ageTreatment = value == null ? CxAgeTreatment.UNSPECIFIED : value;
            return this;
        }
        public Builder privacyOverride(CxPrivacyOverride value) { privacyOverride = value; return this; }
        public CxAudioConfiguration build() { return new CxAudioConfiguration(this); }
    }
}
