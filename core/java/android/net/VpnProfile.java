/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License.
 */

package android.net;

import android.annotation.UnsupportedAppUsage;
import android.net.ProxyInfo;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parcel-like entity class for VPN profiles. To keep things simple, all
 * fields are package private. Methods are provided for serialization, so
 * storage can be implemented easily. Two rules are set for this class.
 * First, all fields must be kept non-null. Second, always make a copy
 * using clone() before modifying.
 */
public class VpnProfile implements Cloneable, Parcelable {
    private static final String TAG = "VpnProfile";

    // Match these constants with R.array.vpn_types.
    public static final int TYPE_PPTP = 0;
    public static final int TYPE_L2TP_IPSEC_PSK = 1;
    public static final int TYPE_L2TP_IPSEC_RSA = 2;
    public static final int TYPE_IPSEC_XAUTH_PSK = 3;
    public static final int TYPE_IPSEC_XAUTH_RSA = 4;
    public static final int TYPE_IPSEC_HYBRID_RSA = 5;
    public static final int TYPE_IKEV2_IPSEC_USER_PASS = 6;
    public static final int TYPE_IKEV2_IPSEC_PSK = 7;
    public static final int TYPE_IKEV2_IPSEC_RSA = 8;
    public static final int TYPE_MAX = 8;

    // Match these constants with R.array.vpn_proxy_settings.
    /** @hide */
    public static final int PROXY_NONE = 0;
    /** @hide */
    public static final int PROXY_MANUAL = 1;

    // Entity fields.
    /** @hide */
    @UnsupportedAppUsage(
            maxTargetSdk = Build.VERSION_CODES.Q,
            publicAlternatives = "Use {@link #getKey()} instead")
    public final String key;                                   // -1
    /** @hide */
    @UnsupportedAppUsage(
            maxTargetSdk = Build.VERSION_CODES.Q,
            publicAlternatives = "Use {@link #getName()} and {@link #setName()} instead")
    public String name = "";                                   // 0
    /** @hide */
    @UnsupportedAppUsage(
            maxTargetSdk = Build.VERSION_CODES.Q,
            publicAlternatives = "Use {@link #getType()} and {@link #setType()} instead")
    public int type = TYPE_PPTP;                               // 1
    /** @hide */
    @UnsupportedAppUsage(
            maxTargetSdk = Build.VERSION_CODES.P,
            trackingBug = 115609023,
            publicAlternatives = "Use {@link #getServer()} and {@link #setServer()} instead")
    public String server = "";                                 // 2
    /** @hide */
    @UnsupportedAppUsage(
            maxTargetSdk = Build.VERSION_CODES.Q,
            publicAlternatives = "Use {@link #getUsername()} and {@link #setUsername()} instead")
    public String username = "";                               // 3
    /** @hide */
    public String password = "";                               // 4
    /** @hide */
    public String dnsServers = "";                             // 5
    /** @hide */
    public String searchDomains = "";                          // 6
    /** @hide */
    public String routes = "";                                 // 7
    /** @hide */
    public boolean mppe = true;                                // 8
    /** @hide */
    public String l2tpSecret = "";                             // 9
    /** @hide */
    public String ipsecIdentifier = "";                        // 10

    /**
     * The RSA private key used for digital signature authentication
     *
     * <p>For IKEv2 VPN types, this String will be a base64-encoded {@link java.security.PrivateKey}
     *
     * @hide
     */
    public String ipsecSecret = "";                            // 11

    /**
     * The RSA certificate to be used for digital signature authentication
     *
     * <p>For IKEv2 VPN types, this String will be a base64-encoded {@link
     * java.security.X509Certificate}
     *
     * @hide
     */
    public String ipsecUserCert = "";                          // 12

    /**
     * The RSA certificate that should be used to verify the server's end/target certificate
     *
     * <p>For IKEv2 VPN types, this String will be a base64-encoded {@link
     * java.security.X509Certificate}
     *
     * @hide
     */
    public String ipsecCaCert = "";                            // 13
    /** @hide */
    public String ipsecServerCert = "";                        // 14
    /** @hide */
    public ProxyInfo proxy = null;                             // 15~18
    /** @hide */
    public List<String> allowedAlgorithms = new ArrayList<>(); // 19
    /** @hide */
    public boolean isBypassable = false;                       // 20
    /** @hide */
    public boolean isMetered = false;                          // 21
    /** @hide */
    public int maxMtu = 1400;                                  // 22
    /** @hide */
    public boolean authParamsInline = false;                   // 23

    // Helper fields.
    /** @hide */
    @UnsupportedAppUsage(
            maxTargetSdk = Build.VERSION_CODES.Q,
            publicAlternatives = "Use {@link #isLoginSaved()} and {@link #setSaveLogin()} instead")
    public boolean saveLogin = false;

    /** @hide */
    public VpnProfile(String key) {
        this.key = key;
    }

    @NonNull
    public String getKey() {
        return key;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setServer(@NonNull String server) {
        this.server = server;
    }

    @NonNull
    public String getServer() {
        return server;
    }

    public void setUsername(@NonNull String username) {
        this.username = username;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public void setSaveLogin(boolean saveLogin) {
        this.saveLogin = saveLogin;
    }

    public boolean isLoginSaved() {
        return saveLogin;
    }

    /** @hide */
    @UnsupportedAppUsage
    public VpnProfile(Parcel in) {
        key = in.readString();
        name = in.readString();
        type = in.readInt();
        server = in.readString();
        username = in.readString();
        password = in.readString();
        dnsServers = in.readString();
        searchDomains = in.readString();
        routes = in.readString();
        mppe = in.readInt() != 0;
        l2tpSecret = in.readString();
        ipsecIdentifier = in.readString();
        ipsecSecret = in.readString();
        ipsecUserCert = in.readString();
        ipsecCaCert = in.readString();
        ipsecServerCert = in.readString();
        saveLogin = in.readInt() != 0;
        proxy = in.readParcelable(null);

        allowedAlgorithms = new ArrayList<>();
        in.readList(allowedAlgorithms, null);
        isBypassable = in.readBoolean();
        isMetered = in.readBoolean();
        maxMtu = in.readInt();
        authParamsInline = in.readBoolean();
    }

    /** @hide */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(key);
        out.writeString(name);
        out.writeInt(type);
        out.writeString(server);
        out.writeString(username);
        out.writeString(password);
        out.writeString(dnsServers);
        out.writeString(searchDomains);
        out.writeString(routes);
        out.writeInt(mppe ? 1 : 0);
        out.writeString(l2tpSecret);
        out.writeString(ipsecIdentifier);
        out.writeString(ipsecSecret);
        out.writeString(ipsecUserCert);
        out.writeString(ipsecCaCert);
        out.writeString(ipsecServerCert);
        out.writeInt(saveLogin ? 1 : 0);
        out.writeParcelable(proxy, flags);
        out.writeList(allowedAlgorithms);
        out.writeBoolean(isBypassable);
        out.writeBoolean(isMetered);
        out.writeInt(maxMtu);
        out.writeBoolean(authParamsInline);
    }

    /**
     * Decodes a VpnProfile instance from the encoded byte array
     *
     * <p>See {@link #encode()}
     *
     * @hide
     */
    @UnsupportedAppUsage
    public static VpnProfile decode(String key, byte[] value) {
        try {
            if (key == null) {
                return null;
            }

            String[] values = new String(value, StandardCharsets.UTF_8).split("\0", -1);
            // There can be 14 - 24 items in the encoded VpnProfile.
            if (values.length < 14 || values.length > 24) {
                return null;
            }

            VpnProfile profile = new VpnProfile(key);
            profile.name = values[0];
            profile.type = Integer.parseInt(values[1]);
            if (profile.type < 0 || profile.type > TYPE_MAX) {
                return null;
            }
            profile.server = values[2];
            profile.username = values[3];
            profile.password = values[4];
            profile.dnsServers = values[5];
            profile.searchDomains = values[6];
            profile.routes = values[7];
            profile.mppe = Boolean.parseBoolean(values[8]);
            profile.l2tpSecret = values[9];
            profile.ipsecIdentifier = values[10];
            profile.ipsecSecret = values[11];
            profile.ipsecUserCert = values[12];
            profile.ipsecCaCert = values[13];
            profile.ipsecServerCert = (values.length > 14) ? values[14] : "";
            if (values.length > 15) {
                String host = (values.length > 15) ? values[15] : "";
                String port = (values.length > 16) ? values[16] : "";
                String exclList = (values.length > 17) ? values[17] : "";
                String pacFileUrl = (values.length > 18) ? values[18] : "";
                if (pacFileUrl.isEmpty()) {
                    profile.proxy = new ProxyInfo(host, port.isEmpty() ?
                            0 : Integer.parseInt(port), exclList);
                } else {
                    profile.proxy = new ProxyInfo(pacFileUrl);
                }
            } // else profle.proxy = null

            if (values.length > 19) {
                profile.allowedAlgorithms = Arrays.asList(values[19].split(","));
            }
            if (values.length > 20) profile.isBypassable = Boolean.parseBoolean(values[20]);
            if (values.length > 21) profile.isMetered = Boolean.parseBoolean(values[21]);
            if (values.length > 22) profile.maxMtu = Integer.parseInt(values[22]);
            if (values.length > 23) profile.authParamsInline = Boolean.parseBoolean(values[23]);

            profile.saveLogin = !profile.username.isEmpty() || !profile.password.isEmpty();
            return profile;
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * Encodes a VpnProfile instance to a byte array for storage.
     *
     * <p>See {@link #decode(String, byte[])}
     *
     * @hide
     */
    public byte[] encode() {
        StringBuilder builder = new StringBuilder(name);
        builder.append('\0').append(type);
        builder.append('\0').append(server);
        builder.append('\0').append(saveLogin ? username : "");
        builder.append('\0').append(saveLogin ? password : "");
        builder.append('\0').append(dnsServers);
        builder.append('\0').append(searchDomains);
        builder.append('\0').append(routes);
        builder.append('\0').append(mppe);
        builder.append('\0').append(l2tpSecret);
        builder.append('\0').append(ipsecIdentifier);
        builder.append('\0').append(ipsecSecret);
        builder.append('\0').append(ipsecUserCert);
        builder.append('\0').append(ipsecCaCert);
        builder.append('\0').append(ipsecServerCert);
        if (proxy != null) {
            builder.append('\0').append(proxy.getHost() != null ? proxy.getHost() : "");
            builder.append('\0').append(proxy.getPort());
            builder.append('\0').append(proxy.getExclusionListAsString() != null ?
                    proxy.getExclusionListAsString() : "");
            builder.append('\0').append(proxy.getPacFileUrl().toString());
        } else {
            builder.append("\0\0\0\0");
        }

        builder.append('\0').append(String.join(",", allowedAlgorithms));
        builder.append('\0').append(isBypassable);
        builder.append('\0').append(isMetered);
        builder.append('\0').append(Integer.toString(maxMtu));
        builder.append('\0').append(authParamsInline);

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Tests if profile is valid for lockdown, which requires IPv4 address for
     * both server and DNS. Server hostnames would require using DNS before
     * connection.
     *
     * @hide
     */
    public boolean isValidLockdownProfile() {
        return isTypeValidForLockdown()
                && isServerAddressNumeric()
                && hasDns()
                && areDnsAddressesNumeric();
    }

    /**
     * Returns {@code true} if the VPN type is valid for lockdown.
     *
     * @hide
     */
    public boolean isTypeValidForLockdown() {
        // b/7064069: lockdown firewall blocks ports used for PPTP
        return type != TYPE_PPTP;
    }

    /**
     * Returns {@code true} if the server address is numeric, e.g. 8.8.8.8
     *
     * @hide
     */
    public boolean isServerAddressNumeric() {
        try {
            InetAddress.parseNumericAddress(server);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /**
     * Returns {@code true} if one or more DNS servers are specified.
     *
     * @hide
     */
    public boolean hasDns() {
        return !TextUtils.isEmpty(dnsServers);
    }

    /**
     * Returns {@code true} if all DNS servers have numeric addresses,
     * e.g. 8.8.8.8
     *
     * @hide
     */
    public boolean areDnsAddressesNumeric() {
        try {
            for (String dnsServer : dnsServers.split(" +")) {
                InetAddress.parseNumericAddress(dnsServer);
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /** @hide */
    public static final Creator<VpnProfile> CREATOR = new Creator<VpnProfile>() {
        @Override
        public VpnProfile createFromParcel(Parcel in) {
            return new VpnProfile(in);
        }

        @Override
        public VpnProfile[] newArray(int size) {
            return new VpnProfile[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
