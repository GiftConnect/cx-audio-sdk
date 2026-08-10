package com.cx.audio.android.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.cx.audio.android.CxAgeTreatment;
import com.cx.audio.android.CxPrivacyResolutionStatus;
import com.cx.audio.android.CxPrivacyState;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

public final class PrivacyEnforcerTest {
    @Test
    public void restrictiveStateStripsIdentifiersAndLocation() throws Exception {
        URL output = PrivacyEnforcer.enforce(
                new URL("https://ads.example.test/vast?idfa=device&ifa=device&lat=42&long=-71&custom=ok&lmt=0"),
                PrivacyRequestPurpose.AD_REQUEST,
                CxPrivacyState.restrictive(0));
        String query = output.getQuery();
        assertFalse(query.contains("idfa="));
        assertFalse(query.contains("ifa="));
        assertFalse(query.contains("lat="));
        assertFalse(query.contains("long="));
        assertFalse(query.contains("lmt=0"));
        org.junit.Assert.assertTrue(query.contains("custom=ok"));
        org.junit.Assert.assertTrue(query.contains("lmt=1"));
        org.junit.Assert.assertTrue(query.contains("dnt=1"));
    }

    @Test(expected = MalformedURLException.class)
    public void insecureTransportIsRejected() throws Exception {
        PrivacyEnforcer.enforce(new URL("http://ads.example.test/vast"),
                PrivacyRequestPurpose.AD_REQUEST, CxPrivacyState.restrictive(0));
    }

    @Test
    public void childStateForcesCoppa() throws Exception {
        CxPrivacyState child = new CxPrivacyState(
                CxPrivacyResolutionStatus.RESTRICTED, CxAgeTreatment.CHILD_DIRECTED,
                false, false, false, false, false, null, null, null,
                Collections.emptyList(), "1NNN", 1, 1);
        URL output = PrivacyEnforcer.enforce(new URL("https://ads.example.test/vast"),
                PrivacyRequestPurpose.AD_REQUEST, child);
        org.junit.Assert.assertTrue(output.getQuery().contains("coppa=1"));
    }

    @Test
    public void clickCannotRetainPublisherSuppliedPrivacyControls() throws Exception {
        URL output = PrivacyEnforcer.enforce(
                new URL("https://click.example.test/path?gdpr=0&gdpr_consent=secret&lmt=0&custom=ok"),
                PrivacyRequestPurpose.CLICK_THROUGH,
                CxPrivacyState.restrictive(0));
        assertFalse(output.getQuery().contains("gdpr="));
        assertFalse(output.getQuery().contains("gdpr_consent="));
        assertFalse(output.getQuery().contains("lmt="));
        org.junit.Assert.assertTrue(output.getQuery().contains("custom=ok"));
    }
}
