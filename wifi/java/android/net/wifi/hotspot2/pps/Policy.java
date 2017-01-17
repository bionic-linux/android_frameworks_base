/**
 * Copyright (c) 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.wifi.hotspot2.pps;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class representing Policy subtree in PerProviderSubscription (PPS)
 * Management Object (MO) tree.
 *
 * For more info, refer to Hotspot 2.0 PPS MO defined in section 9.1 of the Hotspot 2.0
 * Release 2 Technical Specification.
 *
 * @hide
 */
public final class Policy implements Parcelable {
    private static final String TAG = "Policy";

    /**
     * Value indicating policy update is not applicable.  Thus, never check with policy server
     * for updates.
     */
    public static final long UPDATE_CHECK_INTERVAL_NEVER = 0xFFFFFFFFL;

    public static final int PREFERRED_ROAMING_PARTNER_DEFAULT_PRIORITY = 128;

    public static final String UPDATE_METHOD_OMADM = "OMA-DM-ClientInitiated";
    public static final String UPDATE_METHOD_SSP = "SSP-ClientInitiated";

    public static final String UPDATE_RESTRICTION_HOMESP = "HomeSP";
    public static final String UPDATE_RESTRICTION_ROAMING_PARTNER = "RoamingPartner";
    public static final String UPDATE_RESTRICTION_UNRESTRICTED = "Unrestricted";

    /**
     * Maximum bytes for URI string.
     */
    private static final int MAX_URI_BYTES = 1023;

    /**
     * Maximum bytes for username.
     */
    private static final int MAX_USERNAME_BYTES = 63;

    /**
     * Maximum bytes for password.
     */
    private static final int MAX_PASSWORD_BYTES = 255;

    /**
     * Number of bytes for certificate SHA-256 fingerprint byte array.
     */
    private static final int CERTIFICATE_SHA256_BYTES = 32;

    /**
     * Maximum number of SSIDs in the exclusion list.
     */
    private static final int MAX_EXCLUSION_SSIDS = 128;

    /**
     * Maximum byte for SSID.
     */
    private static final int MAX_SSID_BYTES = 32;

    /**
     * Maximum bytes for port string in {@link #requiredProtoPortMap}.
     */
    private static final int MAX_PORT_STRING_BYTES = 64;

    /**
     * Minimum available downlink/uplink bandwidth (in kilobits per second) required when
     * selecting a network from home providers.
     *
     * The bandwidth is calculated as the LinkSpeed * (1 – LinkLoad/255), where LinkSpeed
     * and LinkLoad parameters are drawn from the WAN Metrics ANQP element at that hotspot.
     *
     * Using Long.MIN_VALUE to indicate unset value.
     */
    public long minHomeDownlinkBandwidth = Long.MIN_VALUE;
    public long minHomeUplinkBandwidth = Long.MIN_VALUE;

    /**
     * Minimum available downlink/uplink bandwidth (in kilobits per second) required when
     * selecting a network from roaming providers.
     *
     * The bandwidth is calculated as the LinkSpeed * (1 – LinkLoad/255), where LinkSpeed
     * and LinkLoad parameters are drawn from the WAN Metrics ANQP element at that hotspot.
     *
     * Using Long.MIN_VALUE to indicate unset value.
     */
    public long minRoamingDownlinkBandwidth = Long.MIN_VALUE;
    public long minRoamingUplinkBandwidth = Long.MIN_VALUE;

    /**
     * This specifies how often the mobile device shall check with policy server for updates.
     *
     * Using Long.MIN_VALUE to indicate unset value.
     */
    public long updateIntervalInMinutes = Long.MIN_VALUE;

    /**
     * The method used to update the policy.  Permitted values are "OMA-DM-ClientInitiated"
     * and "SPP-ClientInitiated".
     */
    public String updateMethod = null;

    /**
     * This specifies the hotspots at which the subscription update is permitted.  Permitted
     * values are "HomeSP", "RoamingPartner", or "Unrestricted";
     */
    public String restriction = null;

    /**
     * The URI of the policy server.
     */
    public String policyServerUri = null;

    /**
     * Username used to authenticate with the policy server.
     */
    public String username = null;

    /**
     * Base64 encoded password used to authenticate with the policy server.
     */
    public String base64EncodedPassword = null;

    /**
     * HTTPS URL for retrieving certificate for trust root.  The trust root is used to validate
     * policy server's identity.
     */
    public String trustRootCertUrl = null;

    /**
     * SHA-256 fingerprint of the certificate located at {@link #trustRootCertUrl}
     */
    public byte[] trustRootCertSha256Fingerprint = null;

    /**
     * List of SSIDs that are not preferred by the Home SP.
     */
    public String[] excludedSsidList = null;

    /**
     * List of IP protocol and port number required by one or more operator supported application.
     * The port string contained one or more port numbers delimited by ",".
     */
    public Map<Integer, String> requiredProtoPortMap = null;

    /**
     * This specifies the maximum acceptable BSS load policy.  This is used to prevent device
     * from joining an AP whose channel is overly congested with traffic.
     * Using Integer.MIN_VALUE to indicate unset value.
     */
    public int maximumBssLoadValue = Integer.MIN_VALUE;

    /**
     * Policy associated with a roaming provider.  This specifies a priority associated
     * with a roaming provider for given list of countries.
     *
     * Contains field under PerProviderSubscription/Policy/PreferredRoamingPartnerList.
     */
    public static final class RoamingPartner implements Parcelable {
        /**
         * FQDN of the roaming partner.
         */
        public String fqdn = null;

        /**
         * Flag indicating the exact match of FQDN is required for FQDN matching.
         *
         * When this flag is set to false, sub-domain matching is used.  For example, when
         * {@link #fqdn} s set to "example.com", "host.example.com" would be a match.
         */
        public boolean fqdnExactMatch = false;

        /**
         * Priority associated with this roaming partner policy.
         */
        public int priority = PREFERRED_ROAMING_PARTNER_DEFAULT_PRIORITY;

        /**
         * A string contained One or more, comma delimited (i.e., ",") ISO/IEC 3166-1 two
         * character country strings or the country-independent value, "*".
         */
        public String countries = null;

        public RoamingPartner() {}

        public RoamingPartner(RoamingPartner source) {
            if (source != null) {
                fqdn = source.fqdn;
                fqdnExactMatch = source.fqdnExactMatch;
                priority = source.priority;
                countries = source.countries;
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(fqdn);
            dest.writeInt(fqdnExactMatch ? 1 : 0);
            dest.writeInt(priority);
            dest.writeString(countries);
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (!(thatObject instanceof RoamingPartner)) {
                return false;
            }

            RoamingPartner that = (RoamingPartner) thatObject;
            return TextUtils.equals(fqdn, that.fqdn)
                    && fqdnExactMatch == that.fqdnExactMatch
                    && priority == that.priority
                    && TextUtils.equals(countries, that.countries);
        }

        /**
         * Validate RoamingParnter data.
         *
         * @return true on success
         */
        public boolean validate() {
            if (TextUtils.isEmpty(fqdn)) {
                Log.d(TAG, "Missing FQDN");
                return false;
            }
            if (TextUtils.isEmpty(countries)) {
                Log.d(TAG, "Missing countries");
                return false;
            }
            return true;
        }

        public static final Creator<RoamingPartner> CREATOR =
            new Creator<RoamingPartner>() {
                @Override
                public RoamingPartner createFromParcel(Parcel in) {
                    RoamingPartner roamingPartner = new RoamingPartner();
                    roamingPartner.fqdn = in.readString();
                    roamingPartner.fqdnExactMatch = in.readInt() != 0;
                    roamingPartner.priority = in.readInt();
                    roamingPartner.countries = in.readString();
                    return roamingPartner;
                }

                @Override
                public RoamingPartner[] newArray(int size) {
                    return new RoamingPartner[size];
                }
            };
    }
    public List<RoamingPartner> preferredRoamingPartnerList = null;

    /**
     * Constructor for creating Policy with default values.
     */
    public Policy() {}

    /**
     * Copy constructor.
     *
     * @param source The source to copy from
     */
    public Policy(Policy source) {
        if (source == null) {
            return;
        }
        minHomeDownlinkBandwidth = source.minHomeDownlinkBandwidth;
        minHomeUplinkBandwidth = source.minHomeUplinkBandwidth;
        minRoamingDownlinkBandwidth = source.minRoamingDownlinkBandwidth;
        minRoamingUplinkBandwidth = source.minRoamingUplinkBandwidth;
        updateIntervalInMinutes = source.updateIntervalInMinutes;
        updateMethod = source.updateMethod;
        restriction = source.restriction;
        policyServerUri = source.policyServerUri;
        username = source.username;
        base64EncodedPassword = source.base64EncodedPassword;
        trustRootCertUrl = source.trustRootCertUrl;
        maximumBssLoadValue = source.maximumBssLoadValue;
        if (source.trustRootCertSha256Fingerprint != null) {
            trustRootCertSha256Fingerprint = Arrays.copyOf(source.trustRootCertSha256Fingerprint,
                    source.trustRootCertSha256Fingerprint.length);
        }
        if (source.excludedSsidList != null) {
            excludedSsidList = Arrays.copyOf(source.excludedSsidList,
                    source.excludedSsidList.length);
        }
        if (source.requiredProtoPortMap != null) {
            requiredProtoPortMap = Collections.unmodifiableMap(source.requiredProtoPortMap);
        }
        if (source.preferredRoamingPartnerList != null) {
            preferredRoamingPartnerList = Collections.unmodifiableList(
                    source.preferredRoamingPartnerList);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(minHomeDownlinkBandwidth);
        dest.writeLong(minHomeUplinkBandwidth);
        dest.writeLong(minRoamingDownlinkBandwidth);
        dest.writeLong(minRoamingUplinkBandwidth);
        dest.writeLong(updateIntervalInMinutes);
        dest.writeString(updateMethod);
        dest.writeString(restriction);
        dest.writeString(policyServerUri);
        dest.writeString(username);
        dest.writeString(base64EncodedPassword);
        dest.writeString(trustRootCertUrl);
        dest.writeByteArray(trustRootCertSha256Fingerprint);
        dest.writeStringArray(excludedSsidList);
        writeProtoPortMap(dest, requiredProtoPortMap);
        dest.writeInt(maximumBssLoadValue);
        writeRoamingPartnerList(dest, flags, preferredRoamingPartnerList);
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof Policy)) {
            return false;
        }
        Policy that = (Policy) thatObject;

        return minHomeDownlinkBandwidth == that.minHomeDownlinkBandwidth
                && minHomeUplinkBandwidth == that.minHomeUplinkBandwidth
                && minRoamingDownlinkBandwidth == that.minRoamingDownlinkBandwidth
                && minRoamingUplinkBandwidth == that.minRoamingUplinkBandwidth
                && updateIntervalInMinutes == that.updateIntervalInMinutes
                && TextUtils.equals(updateMethod, that.updateMethod)
                && TextUtils.equals(restriction, that.restriction)
                && TextUtils.equals(policyServerUri, that.policyServerUri)
                && TextUtils.equals(username, that.username)
                && TextUtils.equals(base64EncodedPassword, that.base64EncodedPassword)
                && TextUtils.equals(trustRootCertUrl, that.trustRootCertUrl)
                && Arrays.equals(trustRootCertSha256Fingerprint,
                        that.trustRootCertSha256Fingerprint)
                && Arrays.equals(excludedSsidList, that.excludedSsidList)
                && (requiredProtoPortMap == null) ? that.requiredProtoPortMap == null
                        : requiredProtoPortMap.equals(that.requiredProtoPortMap)
                && maximumBssLoadValue == that.maximumBssLoadValue
                && (preferredRoamingPartnerList == null) ? that.preferredRoamingPartnerList == null
                        : preferredRoamingPartnerList.equals(that.preferredRoamingPartnerList);
    }

    /**
     * Validate Policy data.
     *
     * @return true on success
     */
    public boolean validate() {
        if (updateIntervalInMinutes == Long.MIN_VALUE) {
            Log.d(TAG, "Update interval not specified");
            return false;
        }
        if (updateIntervalInMinutes != UPDATE_CHECK_INTERVAL_NEVER) {
            // Validate parameters for policy update.
            if (!validatePolicyUpdate()) {
                return false;
            }
        }
        // Validate SSID exclusion list.
        if (excludedSsidList != null) {
            if (excludedSsidList.length > MAX_EXCLUSION_SSIDS) {
                Log.d(TAG, "SSID exclusion list size exceeded the max: "
                        + excludedSsidList.length);
                return false;
            }
            for (String ssid : excludedSsidList) {
                if (ssid.getBytes(StandardCharsets.UTF_8).length > MAX_SSID_BYTES) {
                    Log.d(TAG, "Invalid SSID: " + ssid);
                    return false;
                }
            }
        }
        // Validate required protocol to port map.
        if (requiredProtoPortMap != null) {
            for (Map.Entry<Integer, String> entry : requiredProtoPortMap.entrySet()) {
                String portNumber = entry.getValue();
                if (portNumber.getBytes(StandardCharsets.UTF_8).length > MAX_PORT_STRING_BYTES) {
                    Log.d(TAG, "PortNumber string bytes exceeded the max: " + portNumber);
                    return false;
                }
            }
        }
        // Validate preferred roaming partner list.
        if (preferredRoamingPartnerList != null) {
            for (RoamingPartner partner : preferredRoamingPartnerList) {
                if (!partner.validate()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static final Creator<Policy> CREATOR =
        new Creator<Policy>() {
            @Override
            public Policy createFromParcel(Parcel in) {
                Policy policy = new Policy();
                policy.minHomeDownlinkBandwidth = in.readLong();
                policy.minHomeUplinkBandwidth = in.readLong();
                policy.minRoamingDownlinkBandwidth = in.readLong();
                policy.minRoamingUplinkBandwidth = in.readLong();
                policy.updateIntervalInMinutes = in.readLong();
                policy.updateMethod = in.readString();
                policy.restriction = in.readString();
                policy.policyServerUri = in.readString();
                policy.username = in.readString();
                policy.base64EncodedPassword = in.readString();
                policy.trustRootCertUrl = in.readString();
                policy.trustRootCertSha256Fingerprint = in.createByteArray();
                policy.excludedSsidList = in.createStringArray();
                policy.requiredProtoPortMap = readProtoPortMap(in);
                policy.maximumBssLoadValue = in.readInt();
                policy.preferredRoamingPartnerList = readRoamingPartnerList(in);
                return policy;
            }

            @Override
            public Policy[] newArray(int size) {
                return new Policy[size];
            }

            /**
             * Helper function for reading IP Protocol to Port Number map from a Parcel.
             *
             * @param in The Parcel to read from
             * @return Map of IP protocol to port number
             */
            private Map<Integer, String> readProtoPortMap(Parcel in) {
                int size = in.readInt();
                if (size == -1) {
                    return null;
                }
                Map<Integer, String> protoPortMap = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    int key = in.readInt();
                    String value = in.readString();
                    protoPortMap.put(key, value);
                }
                return protoPortMap;
            }

            /**
             * Helper function for reading roaming partner list from a Parcel.
             *
             * @param in The Parcel to read from
             * @return List of roaming partners
             */
            private List<RoamingPartner> readRoamingPartnerList(Parcel in) {
                int size = in.readInt();
                if (size == -1) {
                    return null;
                }
                List<RoamingPartner> partnerList = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    partnerList.add(in.readParcelable(null));
                }
                return partnerList;
            }

        };

    /**
     * Helper function for writing IP Protocol to Port Number map to a Parcel.
     *
     * @param dest The Parcel to write to
     * @param protoPortMap The map to write
     */
    private static void writeProtoPortMap(Parcel dest, Map<Integer, String> protoPortMap) {
        if (protoPortMap == null) {
            dest.writeInt(-1);
            return;
        }
        dest.writeInt(protoPortMap.size());
        for (Map.Entry<Integer, String> entry : protoPortMap.entrySet()) {
            dest.writeInt(entry.getKey());
            dest.writeString(entry.getValue());
        }
    }

    /**
     * Helper function for writing roaming partner list to a Parcel.
     *
     * @param dest The Parcel to write to
     * @param flags The flag about how the object should be written
     * @param partnerList The partner list to write
     */
    private static void writeRoamingPartnerList(Parcel dest, int flags,
            List<RoamingPartner> partnerList) {
        if (partnerList == null) {
            dest.writeInt(-1);
            return;
        }
        dest.writeInt(partnerList.size());
        for (RoamingPartner partner : partnerList) {
            dest.writeParcelable(partner, flags);
        }
    }

    /**
     * Validate parameters used for policy update.
     *
     * @return true on success
     */
    private boolean validatePolicyUpdate() {
        if (!TextUtils.equals(updateMethod, UPDATE_METHOD_OMADM)
                && !TextUtils.equals(updateMethod, UPDATE_METHOD_SSP)) {
            Log.d(TAG, "Unknown update method: " + updateMethod);
            return false;
        }

        if (!TextUtils.equals(restriction, UPDATE_RESTRICTION_HOMESP)
                && !TextUtils.equals(restriction, UPDATE_RESTRICTION_ROAMING_PARTNER)
                && !TextUtils.equals(restriction, UPDATE_RESTRICTION_UNRESTRICTED)) {
            Log.d(TAG, "Unknown restriction: " + restriction);
            return false;
        }

        if (TextUtils.isEmpty(policyServerUri)) {
            Log.d(TAG, "Missing policy server URI");
            return false;
        }
        if (policyServerUri.getBytes(StandardCharsets.UTF_8).length > MAX_URI_BYTES) {
            Log.d(TAG, "URI bytes exceeded the max: "
                    + policyServerUri.getBytes(StandardCharsets.UTF_8).length);
            return false;
        }

        if (TextUtils.isEmpty(username)) {
            Log.d(TAG, "Missing username");
            return false;
        }
        if (username.getBytes(StandardCharsets.UTF_8).length > MAX_USERNAME_BYTES) {
            Log.d(TAG, "Username bytes exceeded the max: "
                    + username.getBytes(StandardCharsets.UTF_8).length);
            return false;
        }

        if (TextUtils.isEmpty(base64EncodedPassword)) {
            Log.d(TAG, "Missing username");
            return false;
        }
        if (base64EncodedPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            Log.d(TAG, "Password bytes exceeded the max: "
                    + base64EncodedPassword.getBytes(StandardCharsets.UTF_8).length);
            return false;
        }
        try {
            Base64.decode(base64EncodedPassword, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Invalid encoding for password: " + base64EncodedPassword);
            return false;
        }

        if (TextUtils.isEmpty(trustRootCertUrl)) {
            Log.d(TAG, "Missing trust root certificate URL");
            return false;
        }
        if (trustRootCertSha256Fingerprint == null) {
            Log.d(TAG, "Missing trust root certificate SHA-256 fingerprint");
            return false;
        }
        if (trustRootCertSha256Fingerprint.length != CERTIFICATE_SHA256_BYTES) {
            Log.d(TAG, "Incorrect size of trust root certificate SHA-256 fingerprint: "
                    + trustRootCertSha256Fingerprint.length);
            return false;
        }
        return true;
    }
}
