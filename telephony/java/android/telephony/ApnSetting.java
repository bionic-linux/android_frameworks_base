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
 * limitations under the License.
 */

package android.telephony;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.hardware.radio.V1_0.ApnTypes;
import android.net.NetworkUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Telephony;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * This class represents a apn setting for create PDP link
 */
public class ApnSetting implements Parcelable{

    /** @hide */
    static final String LOG_TAG = "ApnSetting";
    /** @hide */
    private static final boolean VDBG = false;

    public final String carrier;
    public final String apn;
    public final String proxy;
    public final String port;
    public final String mmsc;
    public final String mmsProxy;
    public final String mmsPort;
    public final String user;
    public final String password;
    public final int authType;
    public final String[] types;
    /** @hide */
    public final int typesBitmap;
    public final int id;
    public final String numeric;
    public final String protocol;
    public final String roamingProtocol;
    /** @hide */
    public final int mtu;

    /**
     * Current status of APN
     * true : enabled APN, false : disabled APN.
     */
    public final boolean carrierEnabled;
    /**
     * Radio Access Technology info
     * To check what values can hold, refer to ServiceState.java.
     * This should be spread to other technologies,
     * but currently only used for LTE(14) and EHRPD(13).
     * @hide
     */
    private final int bearer;
    /**
     * Radio Access Technology info
     * To check what values can hold, refer to ServiceState.java. This is a bitmask of radio
     * technologies in ServiceState.
     * This should be spread to other technologies,
     * but currently only used for LTE(14) and EHRPD(13).
     */
    public final int bearerBitmask;

    /* ID of the profile in the modem */
    /** @hide */
    public final int profileId;
    /** @hide */
    public final boolean modemCognitive;
    /** @hide */
    public final int maxConns;
    /** @hide */
    public final int waitTime;
    /** @hide */
    public final int maxConnsTime;

    /**
     * MVNO match type. Possible values:
     *   "spn": Service provider name.
     *   "imsi": IMSI.
     *   "gid": Group identifier level 1.
     *   "iccid": ICCID
     */
    public final String mvnoType;
    /**
     * MVNO data. Examples:
     *   "spn": A MOBILE, BEN NL
     *   "imsi": 302720x94, 2060188
     *   "gid": 4E, 33
     *   "iccid": 898603 etc.
     * @hide
     */
    public final String mvnoMatchData;

    /**
     * Indicates this APN setting is permanently failed and cannot be
     * retried by the retry manager anymore.
     * @hide
     */
    public boolean permanentFailed = false;

    /** @hide */
    public ApnSetting(int id, String numeric, String carrier, String apn,
            String proxy, String port,
            String mmsc, String mmsProxy, String mmsPort,
            String user, String password, int authType, String[] types,
            String protocol, String roamingProtocol, boolean carrierEnabled, int bearer,
            int bearerBitmask, int profileId, boolean modemCognitive, int maxConns,
            int waitTime, int maxConnsTime, int mtu, String mvnoType,
            String mvnoMatchData) {
        this.id = id;
        this.numeric = numeric;
        this.carrier = carrier;
        this.apn = apn;
        this.proxy = proxy;
        this.port = port;
        this.mmsc = mmsc;
        this.mmsProxy = mmsProxy;
        this.mmsPort = mmsPort;
        this.user = user;
        this.password = password;
        this.authType = authType;
        this.types = new String[types.length];
        int apnBitmap = 0;
        for (int i = 0; i < types.length; i++) {
            this.types[i] = types[i].toLowerCase();
            apnBitmap |= getApnBitmask(this.types[i]);
        }
        this.typesBitmap = apnBitmap;
        this.protocol = protocol;
        this.roamingProtocol = roamingProtocol;
        this.carrierEnabled = carrierEnabled;
        this.bearer = bearer;
        this.bearerBitmask = (bearerBitmask | ServiceState.getBitmaskForTech(bearer));
        this.profileId = profileId;
        this.modemCognitive = modemCognitive;
        this.maxConns = maxConns;
        this.waitTime = waitTime;
        this.maxConnsTime = maxConnsTime;
        this.mtu = mtu;
        this.mvnoType = mvnoType;
        this.mvnoMatchData = mvnoMatchData;
    }

    public ApnSetting() {
        this(0, "", "", "", "", "", "", "", "",
                "", "", 0, new String[]{}, "", "", true, 0, 0, 0,
                false, 0, 0, 0, 0, "", "");
    }

    public ApnSetting(ApnSetting apn) {
        this(apn.id, apn.numeric, apn.carrier, apn.apn, apn.proxy, apn.port, apn.mmsc, apn.mmsProxy,
                apn.mmsPort, apn.user, apn.password, apn.authType, apn.types, apn.protocol,
                apn.roamingProtocol, apn.carrierEnabled, apn.bearer, apn.bearerBitmask,
                apn.profileId, apn.modemCognitive, apn.maxConns, apn.waitTime, apn.maxConnsTime,
                apn.mtu, apn.mvnoType, apn.mvnoMatchData);
    }

    /** @hide */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ApnSettingV3] ")
                .append(carrier)
                .append(", ").append(id)
                .append(", ").append(numeric)
                .append(", ").append(apn)
                .append(", ").append(proxy)
                .append(", ").append(mmsc)
                .append(", ").append(mmsProxy)
                .append(", ").append(mmsPort)
                .append(", ").append(port)
                .append(", ").append(authType).append(", ");
        for (int i = 0; i < types.length; i++) {
            sb.append(types[i]);
            if (i < types.length - 1) {
                sb.append(" | ");
            }
        }
        sb.append(", ").append(protocol);
        sb.append(", ").append(roamingProtocol);
        sb.append(", ").append(carrierEnabled);
        sb.append(", ").append(bearer);
        sb.append(", ").append(bearerBitmask);
        sb.append(", ").append(profileId);
        sb.append(", ").append(modemCognitive);
        sb.append(", ").append(maxConns);
        sb.append(", ").append(waitTime);
        sb.append(", ").append(maxConnsTime);
        sb.append(", ").append(mtu);
        sb.append(", ").append(mvnoType);
        sb.append(", ").append(mvnoMatchData);
        sb.append(", ").append(permanentFailed);
        return sb.toString();
    }

    /**
     * Returns true if there are MVNO params specified.
     * @hide
     */
    public boolean hasMvnoParams() {
        return !TextUtils.isEmpty(mvnoType) && !TextUtils.isEmpty(mvnoMatchData);
    }

    /** @hide */
    public boolean canHandleType(String type) {
        if (!carrierEnabled) return false;
        boolean wildcardable = true;
        if (Type.APN_TYPE_IA.equalsIgnoreCase(type)) wildcardable = false;
        for (String t : types) {
            // DEFAULT handles all, and HIPRI is handled by DEFAULT
            if (t.equalsIgnoreCase(type) ||
                    (wildcardable && t.equalsIgnoreCase(Type.APN_TYPE_ALL)) ||
                    (t.equalsIgnoreCase(Type.APN_TYPE_DEFAULT) &&
                            type.equalsIgnoreCase(Type.APN_TYPE_HIPRI))) {
                return true;
            }
        }
        return false;
    }

    // check whether the types of two APN same (even only one type of each APN is same)
    /** @hide */
    private boolean typeSameAny(ApnSetting first, ApnSetting second) {
        if (VDBG) {
            StringBuilder apnType1 = new StringBuilder(first.apn + ": ");
            for (int index1 = 0; index1 < first.types.length; index1++) {
                apnType1.append(first.types[index1]);
                apnType1.append(",");
            }

            StringBuilder apnType2 = new StringBuilder(second.apn + ": ");
            for (int index1 = 0; index1 < second.types.length; index1++) {
                apnType2.append(second.types[index1]);
                apnType2.append(",");
            }
            Rlog.d(LOG_TAG, "APN1: is " + apnType1);
            Rlog.d(LOG_TAG, "APN2: is " + apnType2);
        }

        for (int index1 = 0; index1 < first.types.length; index1++) {
            for (int index2 = 0; index2 < second.types.length; index2++) {
                if (first.types[index1].equals(ApnSetting.Type.APN_TYPE_ALL)
                        || second.types[index2].equals(ApnSetting.Type.APN_TYPE_ALL)
                        || first.types[index1].equals(second.types[index2])) {
                    if (VDBG) Rlog.d(LOG_TAG, "typeSameAny: return true");
                    return true;
                }
            }
        }

        if (VDBG) Rlog.d(LOG_TAG, "typeSameAny: return false");
        return false;
    }

    // TODO - if we have this function we should also have hashCode.
    // Also should handle changes in type order and perhaps case-insensitivity
    /** @hide */
    public boolean equals(Object o) {
        if (o instanceof ApnSetting == false) {
            return false;
        }

        ApnSetting other = (ApnSetting) o;

        return carrier.equals(other.carrier)
                && id == other.id
                && numeric.equals(other.numeric)
                && apn.equals(other.apn)
                && proxy.equals(other.proxy)
                && mmsc.equals(other.mmsc)
                && mmsProxy.equals(other.mmsProxy)
                && TextUtils.equals(mmsPort, other.mmsPort)
                && port.equals(other.port)
                && TextUtils.equals(user, other.user)
                && TextUtils.equals(password, other.password)
                && authType == other.authType
                && Arrays.deepEquals(types, other.types)
                && typesBitmap == other.typesBitmap
                && protocol.equals(other.protocol)
                && roamingProtocol.equals(other.roamingProtocol)
                && carrierEnabled == other.carrierEnabled
                && bearer == other.bearer
                && bearerBitmask == other.bearerBitmask
                && profileId == other.profileId
                && modemCognitive == other.modemCognitive
                && maxConns == other.maxConns
                && waitTime == other.waitTime
                && maxConnsTime == other.maxConnsTime
                && mtu == other.mtu
                && mvnoType.equals(other.mvnoType)
                && mvnoMatchData.equals(other.mvnoMatchData);
    }

    /**
     * Compare two APN settings
     *
     * Note: This method does not compare 'id', 'bearer', 'bearerBitmask'. We only use this for
     * determining if tearing a data call is needed when conditions change. See
     * cleanUpConnectionsOnUpdatedApns in DcTracker.
     *
     * @param o the other object to compare
     * @param isDataRoaming True if the device is on data roaming
     * @return True if the two APN settings are same
     * @hide
     */
    public boolean equals(Object o, boolean isDataRoaming) {
        if (!(o instanceof ApnSetting)) {
            return false;
        }

        ApnSetting other = (ApnSetting) o;

        return carrier.equals(other.carrier)
                && numeric.equals(other.numeric)
                && apn.equals(other.apn)
                && proxy.equals(other.proxy)
                && mmsc.equals(other.mmsc)
                && mmsProxy.equals(other.mmsProxy)
                && TextUtils.equals(mmsPort, other.mmsPort)
                && port.equals(other.port)
                && TextUtils.equals(user, other.user)
                && TextUtils.equals(password, other.password)
                && authType == other.authType
                && Arrays.deepEquals(types, other.types)
                && typesBitmap == other.typesBitmap
                && (isDataRoaming || protocol.equals(other.protocol))
                && (!isDataRoaming || roamingProtocol.equals(other.roamingProtocol))
                && carrierEnabled == other.carrierEnabled
                && profileId == other.profileId
                && modemCognitive == other.modemCognitive
                && maxConns == other.maxConns
                && waitTime == other.waitTime
                && maxConnsTime == other.maxConnsTime
                && mtu == other.mtu
                && mvnoType.equals(other.mvnoType)
                && mvnoMatchData.equals(other.mvnoMatchData);
    }

    /**
     * Check if neither mention DUN and are substantially similar
     *
     * @param other The other APN settings to compare
     * @return True if two APN settings are similar
     * @hide
     */
    public boolean similar(ApnSetting other) {
        return (!this.canHandleType(Type.APN_TYPE_DUN)
                && !other.canHandleType(Type.APN_TYPE_DUN)
                && Objects.equals(this.apn, other.apn)
                && !typeSameAny(this, other)
                && xorEquals(this.proxy, other.proxy)
                && xorEquals(this.port, other.port)
                && xorEquals(this.protocol, other.protocol)
                && xorEquals(this.roamingProtocol, other.roamingProtocol)
                && this.carrierEnabled == other.carrierEnabled
                && this.bearerBitmask == other.bearerBitmask
                && this.profileId == other.profileId
                && Objects.equals(this.mvnoType, other.mvnoType)
                && Objects.equals(this.mvnoMatchData, other.mvnoMatchData)
                && xorEquals(this.mmsc, other.mmsc)
                && xorEquals(this.mmsProxy, other.mmsProxy)
                && xorEquals(this.mmsPort, other.mmsPort));
    }

    // Equal or one is not specified
    /** @hide */
    private boolean xorEquals(String first, String second) {
        return (Objects.equals(first, second)
                || TextUtils.isEmpty(first)
                || TextUtils.isEmpty(second));
    }

    // Helper function to convert APN string into a 32-bit bitmask.
    /** @hide */
    private static int getApnBitmask(String apn) {
        switch (apn) {
            case Type.APN_TYPE_DEFAULT: return ApnTypes.DEFAULT;
            case Type.APN_TYPE_MMS: return ApnTypes.MMS;
            case Type.APN_TYPE_SUPL: return ApnTypes.SUPL;
            case Type.APN_TYPE_DUN: return ApnTypes.DUN;
            case Type.APN_TYPE_HIPRI: return ApnTypes.HIPRI;
            case Type.APN_TYPE_FOTA: return ApnTypes.FOTA;
            case Type.APN_TYPE_IMS: return ApnTypes.IMS;
            case Type.APN_TYPE_CBS: return ApnTypes.CBS;
            case Type.APN_TYPE_IA: return ApnTypes.IA;
            case Type.APN_TYPE_EMERGENCY: return ApnTypes.EMERGENCY;
            case Type.APN_TYPE_ALL: return ApnTypes.ALL;
            default: return ApnTypes.NONE;
        }
    }

    /** @hide */
    String deParseTypes(String[] types) {
        return TextUtils.join(",", types);
    }

    /** @hide */
    public ContentValues toContentValues() {
        ContentValues apnValue = new ContentValues();
        if (numeric != null) {
            apnValue.put(Telephony.Carriers.NUMERIC, numeric);
        }
        if (carrier != null) {
            apnValue.put(Telephony.Carriers.NAME, carrier);
        }
        if (apn != null) {
            apnValue.put(Telephony.Carriers.APN, apn);
        }
        if (proxy != null) {
            apnValue.put(Telephony.Carriers.PROXY, proxy);
        }
        if (port != null) {
            apnValue.put(Telephony.Carriers.PORT, port);
        }
        if (mmsc != null) {
            apnValue.put(Telephony.Carriers.MMSC, mmsc);
        }
        if (mmsPort != null) {
            apnValue.put(Telephony.Carriers.MMSPORT, mmsPort);
        }
        if (mmsProxy != null) {
            apnValue.put(Telephony.Carriers.MMSPROXY, mmsProxy);
        }
        if (user != null) {
            apnValue.put(Telephony.Carriers.USER, user);
        }
        if (password != null) {
            apnValue.put(Telephony.Carriers.PASSWORD, password);
        }
        apnValue.put(Telephony.Carriers.AUTH_TYPE, authType);
        String apnType = deParseTypes(types);
        if (apnType != null) {
            apnValue.put(Telephony.Carriers.TYPE, apnType);
        }
        if (protocol != null) {
            apnValue.put(Telephony.Carriers.PROTOCOL, protocol);
        }
        if (roamingProtocol != null) {
            apnValue.put(Telephony.Carriers.ROAMING_PROTOCOL, roamingProtocol);
        }
        apnValue.put(Telephony.Carriers.CARRIER_ENABLED, carrierEnabled);
        // networkTypeBit.
        apnValue.put(Telephony.Carriers.BEARER_BITMASK, bearerBitmask);
        if (mvnoType != null) {
            apnValue.put(Telephony.Carriers.MVNO_TYPE, mvnoType);
        }

        return apnValue;
    }

    /**
     * @param types comma delimited list of APN types
     * @return array of APN types
     * @hide
     */
    public static String[] parseTypes(String types) {
        String[] result;
        // If unset, set to DEFAULT.
        if (types == null || types.equals("")) {
            result = new String[1];
            result[0] = Type.APN_TYPE_ALL;
        } else {
            result = types.split(",");
        }
        return result;
    }

    /** @hide */
    public static ApnSetting makeApnSetting(Cursor cursor) {
        String[] types = parseTypes(
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.TYPE)));
        ApnSetting apn = new ApnSetting(
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NUMERIC)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.APN)),
                NetworkUtils.trimV4AddrZeros(
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.PROXY))),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PORT)),
                NetworkUtils.trimV4AddrZeros(
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSC))),
                NetworkUtils.trimV4AddrZeros(
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPROXY))),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPORT)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.USER)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PASSWORD)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.AUTH_TYPE)),
                types,
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROTOCOL)),
                cursor.getString(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.ROAMING_PROTOCOL)),
                cursor.getInt(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.CARRIER_ENABLED)) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.BEARER)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.BEARER_BITMASK)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROFILE_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.MODEM_COGNITIVE)) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MAX_CONNS)),
                cursor.getInt(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.WAIT_TIME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MAX_CONNS_TIME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MTU)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MVNO_TYPE)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MVNO_MATCH_DATA)));
        return apn;
    }

    // Implement Parcelable.

    @Override
    /** @hide */
    public int describeContents() {
        return 0;
    }

    @Override
    /** @hide */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(numeric);
        dest.writeString(carrier);
        dest.writeString(apn);
        dest.writeString(proxy);
        dest.writeString(port);
        dest.writeString(mmsc);
        dest.writeString(mmsProxy);
        dest.writeString(mmsPort);
        dest.writeString(user);
        dest.writeString(password);
        dest.writeInt(authType);
        dest.writeStringArray(types);
        dest.writeString(protocol);
        dest.writeString(roamingProtocol);
        dest.writeInt(carrierEnabled ? 1: 0);
        dest.writeInt(bearerBitmask);
        dest.writeString(mvnoType);
    }

    /** @hide */
    private ApnSetting(Parcel in) {
        this(in.readInt(), in.readString(), in.readString(), in.readString(), in.readString(),
                in.readString(), in.readString(), in.readString(), in.readString(), in.readString(),
                in.readString(), in.readInt(), in.readStringArray(), in.readString(),
                in.readString(), in.readInt() > 0, 0, in.readInt(), 0,
                false, 0, 0, 0, 0, in.readString(), "");
    }

    public static final Parcelable.Creator<ApnSetting> CREATOR =
            new Parcelable.Creator<ApnSetting>() {
                @Override
                public ApnSetting createFromParcel(Parcel in) {
                    return new ApnSetting(in);
                }

                @Override
                public ApnSetting[] newArray(int size) {
                    return new ApnSetting[size];
                }
            };

    public static class Type {
        /**
         * APN types for data connections.  These are usage categories for an APN
         * entry.  One APN entry may support multiple APN types, eg, a single APN
         * may service regular internet traffic ("default") as well as MMS-specific
         * connections.<br/>
         * APN_TYPE_ALL is a special type to indicate that this APN entry can
         * service all data connections.
         */
        public static final String APN_TYPE_ALL = "*";
        /** APN type for default data traffic */
        public static final String APN_TYPE_DEFAULT = "default";
        /** APN type for MMS traffic */
        public static final String APN_TYPE_MMS = "mms";
        /** APN type for SUPL assisted GPS */
        public static final String APN_TYPE_SUPL = "supl";
        /** APN type for DUN traffic */
        public static final String APN_TYPE_DUN = "dun";
        /** APN type for HiPri traffic */
        public static final String APN_TYPE_HIPRI = "hipri";
        /** APN type for FOTA */
        public static final String APN_TYPE_FOTA = "fota";
        /** APN type for IMS */
        public static final String APN_TYPE_IMS = "ims";
        /** APN type for CBS */
        public static final String APN_TYPE_CBS = "cbs";
        /** APN type for IA Initial Attach APN */
        public static final String APN_TYPE_IA = "ia";
        /** APN type for Emergency PDN. This is not an IA apn, but is used
         * for access to carrier services in an emergency call situation. */
        public static final String APN_TYPE_EMERGENCY = "emergency";
        /** Array of all APN types */
        public static final String[] APN_TYPES = {APN_TYPE_DEFAULT,
                APN_TYPE_MMS,
                APN_TYPE_SUPL,
                APN_TYPE_DUN,
                APN_TYPE_HIPRI,
                APN_TYPE_FOTA,
                APN_TYPE_IMS,
                APN_TYPE_CBS,
                APN_TYPE_IA,
                APN_TYPE_EMERGENCY
        };
    }
}
