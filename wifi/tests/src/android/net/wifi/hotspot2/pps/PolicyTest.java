/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.net.wifi.hotspot2.pps;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Parcel;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@link android.net.wifi.hotspot2.pps.Policy}.
 */
@SmallTest
public class PolicyTest {
    private static final int MAX_URI_BYTES = 1023;
    private static final int MAX_USERNAME_BYTES = 63;
    private static final int MAX_PASSWORD_BYTES = 255;
    private static final int CERTIFICATE_SHA256_BYTES = 32;
    private static final int MAX_NUMBER_OF_EXCLUDED_SSIDS = 128;
    private static final int MAX_SSID_BYTES = 32;
    private static final int MAX_PORT_STRING_BYTES = 64;

    /**
     * Helper function for creating a {@link Policy} for testing.
     *
     * @return {@link Policy}
     */
    private static Policy createPolicy() {
        Policy policy = new Policy();
        policy.minHomeDownlinkBandwidth = 123;
        policy.minHomeUplinkBandwidth = 345;
        policy.minRoamingDownlinkBandwidth = 567;
        policy.minRoamingUplinkBandwidth = 789;
        policy.updateIntervalInMinutes = 1712;
        policy.updateMethod = Policy.UPDATE_METHOD_OMADM;
        policy.restriction = Policy.UPDATE_RESTRICTION_HOMESP;
        policy.policyServerUri = "policy.update.com";
        policy.username = "username";
        policy.base64EncodedPassword =
                Base64.encodeToString("password".getBytes(), Base64.DEFAULT);
        policy.maximumBssLoadValue = 12;
        policy.trustRootCertUrl = "trust.cert.com";
        policy.trustRootCertSha256Fingerprint = new byte[32];
        policy.excludedSsidList = new String[] {"ssid1", "ssid2"};
        policy.requiredProtoPortMap = new HashMap<>();
        policy.requiredProtoPortMap.put(12, "23,342,123");
        policy.requiredProtoPortMap.put(23, "789,372,1235");

        policy.preferredRoamingPartnerList = new ArrayList<>();
        Policy.RoamingPartner partner1 = new Policy.RoamingPartner();
        partner1.fqdn = "partner1.com";
        partner1.fqdnExactMatch = true;
        partner1.priority = 12;
        partner1.countries = "us,jp";
        Policy.RoamingPartner partner2 = new Policy.RoamingPartner();
        partner2.fqdn = "partner2.com";
        partner2.fqdnExactMatch = false;
        partner2.priority = 42;
        partner2.countries = "ca,fr";
        policy.preferredRoamingPartnerList.add(partner1);
        policy.preferredRoamingPartnerList.add(partner2);
        return policy;
    }

    /**
     * Helper function for verifying Policy after parcel write then read.
     * @param policyToWrite
     * @throws Exception
     */
    private static void verifyParcel(Policy policyToWrite) throws Exception {
        Parcel parcel = Parcel.obtain();
        policyToWrite.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);    // Rewind data position back to the beginning for read.
        Policy policyFromRead = Policy.CREATOR.createFromParcel(parcel);
        assertTrue(policyFromRead.equals(policyToWrite));
    }

    /**
     * Verify parcel read/write for an empty Policy.
     *
     * @throws Exception
     */
    @Test
    public void verifyParcelWithEmptyPolicy() throws Exception {
        verifyParcel(new Policy());
    }

    /**
     * Verify parcel read/write for a Policy with all fields set.
     *
     * @throws Exception
     */
    @Test
    public void verifyParcelWithFullPolicy() throws Exception {
        verifyParcel(createPolicy());
    }

    /**
     * Verify parcel read/write for a Policy without protocol port map.
     *
     * @throws Exception
     */
    @Test
    public void verifyParcelWithoutProtoPortMap() throws Exception {
        Policy policy = createPolicy();
        policy.requiredProtoPortMap = null;
        verifyParcel(policy);
    }

    /**
     * Verify parcel read/write for a Policy without preferred roaming partner list.
     *
     * @throws Exception
     */
    @Test
    public void verifyParcelWithoutPreferredRoamingPartnerList() throws Exception {
        Policy policy = createPolicy();
        policy.preferredRoamingPartnerList = null;
        verifyParcel(policy);
    }

    /**
     * Verify that policy created using copy constructor with null source should be the same
     * as the policy created using default constructor.
     *
     * @throws Exception
     */
    @Test
    public void verifyCopyConstructionWithNullSource() throws Exception {
        Policy copyPolicy = new Policy(null);
        Policy defaultPolicy = new Policy();
        assertTrue(defaultPolicy.equals(copyPolicy));
    }

    /**
     * Verify that policy created using copy constructor with a valid source should be the
     * same as the source.
     *
     * @throws Exception
     */
    @Test
    public void verifyCopyConstructionWithFullPolicy() throws Exception {
        Policy policy = createPolicy();
        Policy copyPolicy = new Policy(policy);
        assertTrue(policy.equals(copyPolicy));
    }

    /**
     * Verify that a default policy (with no informatio) is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithDefault() throws Exception {
        Policy policy = new Policy();
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy created using {@link #createPolicy} is valid, since all fields are
     * filled in with valid values.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithFullPolicy() throws Exception {
        assertTrue(createPolicy().validate());
    }

    /**
     * Verify that a policy with an unknown update method is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithUnknowMethod() throws Exception {
        Policy policy = createPolicy();
        policy.updateMethod = "adsfasd";
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy with an unknown restriction is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithUnknowRestriction() throws Exception {
        Policy policy = createPolicy();
        policy.restriction = "adsfasd";
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy with an username exceeding maximum size is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithUsernameExceedingMaxSize() throws Exception {
        Policy policy = createPolicy();
        byte[] rawUsernameBytes = new byte[MAX_USERNAME_BYTES + 1];
        Arrays.fill(rawUsernameBytes, (byte) 'a');
        policy.username = new String(rawUsernameBytes, StandardCharsets.UTF_8);
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy with an empty username is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithEmptyUsername() throws Exception {
        Policy policy = createPolicy();
        policy.username = null;
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy with a password exceeding maximum size is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithPasswordExceedingMaxSize() throws Exception {
        Policy policy = createPolicy();
        byte[] rawPasswordBytes = new byte[MAX_PASSWORD_BYTES + 1];
        Arrays.fill(rawPasswordBytes, (byte) 'a');
        policy.base64EncodedPassword = new String(rawPasswordBytes, StandardCharsets.UTF_8);
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy with an empty password is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithEmptyPassword() throws Exception {
        Policy policy = createPolicy();
        policy.base64EncodedPassword = null;
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy with a Base64 encoded password that contained invalid padding
     * is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithPasswordContainedInvalidPadding() throws Exception {
        Policy policy = createPolicy();
        policy.base64EncodedPassword = policy.base64EncodedPassword + "=";
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy without trust root certificate URL is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithoutTrustRootCertUrl() throws Exception {
        Policy policy = createPolicy();
        policy.trustRootCertUrl = null;
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy without trust root certificate SHA-256 fingerprint is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithouttrustRootCertSha256Fingerprint() throws Exception {
        Policy policy = createPolicy();
        policy.trustRootCertSha256Fingerprint = null;
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy with an incorrect size trust root certificate SHA-256 fingerprint is
     * invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithInvalidtrustRootCertSha256Fingerprint() throws Exception {
        Policy policy = createPolicy();
        policy.trustRootCertSha256Fingerprint = new byte[CERTIFICATE_SHA256_BYTES + 1];
        assertFalse(policy.validate());

        policy.trustRootCertSha256Fingerprint = new byte[CERTIFICATE_SHA256_BYTES - 1];
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy without policy server URI is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithoutServerUri() throws Exception {
        Policy policy = createPolicy();
        byte[] rawUriBytes = new byte[MAX_URI_BYTES + 1];
        policy.policyServerUri = new String(rawUriBytes, StandardCharsets.UTF_8);
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy with an invalid server URI is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithInvalidServerUri() throws Exception {
        Policy policy = createPolicy();
        byte[] rawUriBytes = new byte[MAX_URI_BYTES + 1];
        policy.policyServerUri = new String(rawUriBytes, StandardCharsets.UTF_8);
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy with policy update interval set to "never", will not parameters related
     * to the update server (e.g. policyServerUri, trustRootCertUrl, and etc).
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithNoServerCheck() throws Exception {
        Policy policy = createPolicy();
        policy.updateIntervalInMinutes = Policy.UPDATE_CHECK_INTERVAL_NEVER;
        policy.username = null;
        policy.base64EncodedPassword = null;
        policy.updateMethod = null;
        policy.restriction = null;
        policy.policyServerUri = null;
        policy.trustRootCertUrl = null;
        policy.trustRootCertSha256Fingerprint = null;
        assertTrue(policy.validate());
    }

    /**
     * Verify that a policy with a preferred roaming partner with FQDN not specified is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithRoamingPartnerWithoutFQDN() throws Exception {
        Policy policy = createPolicy();
        Policy.RoamingPartner partner = new Policy.RoamingPartner();
        partner.fqdnExactMatch = true;
        partner.priority = 12;
        partner.countries = "us,jp";
        policy.preferredRoamingPartnerList.add(partner);
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy with a preferred roaming partner with countries not specified is
     * invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithRoamingPartnerWithoutCountries() throws Exception {
        Policy policy = createPolicy();
        Policy.RoamingPartner partner = new Policy.RoamingPartner();
        partner.fqdn = "test.com";
        partner.fqdnExactMatch = true;
        partner.priority = 12;
        policy.preferredRoamingPartnerList.add(partner);
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy with a proto-port tuple that contains an invalid port string is
     * invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithInvalidPortStringInProtoPortMap() throws Exception {
        Policy policy = createPolicy();
        byte[] rawPortBytes = new byte[MAX_PORT_STRING_BYTES + 1];
        policy.requiredProtoPortMap.put(324, new String(rawPortBytes, StandardCharsets.UTF_8));
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy with number of excluded SSIDs exceeded the max is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithSsidExclusionListSizeExceededMax() throws Exception {
        Policy policy = createPolicy();
        policy.excludedSsidList = new String[MAX_NUMBER_OF_EXCLUDED_SSIDS + 1];
        Arrays.fill(policy.excludedSsidList, "ssid");
        assertFalse(policy.validate());
    }

    /**
     * Verify that a policy with an invalid SSID in the excluded SSID list is invalid.
     *
     * @throws Exception
     */
    @Test
    public void validatePolicyWithInvalidSsid() throws Exception {
        Policy policy = createPolicy();
        byte[] rawSsidBytes = new byte[MAX_SSID_BYTES + 1];
        Arrays.fill(rawSsidBytes, (byte) 'a');
        policy.excludedSsidList = new String[] {new String(rawSsidBytes, StandardCharsets.UTF_8)};
        assertFalse(policy.validate());
    }
}
