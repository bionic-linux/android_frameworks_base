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
import java.util.List;
import java.util.Objects;

/**
 * A class representing an APN configuration.
 */
public class ApnSetting implements Parcelable {

    static final String LOG_TAG = "ApnSetting";
    private static final boolean VDBG = false;

    private String mCarrier;
    private String mApn;
    private String mProxy;
    private String mPort;
    private String mMmsc;
    private String mMmsProxy;
    private String mMmsPort;
    private String mUser;
    private String mPassword;
    private int mAuthType;
    private String[] mTypes;
    private int mTypesBitmap;
    private int mId;
    private String mNumeric;
    private String mProtocol;
    private String mRoamingProtocol;
    private int mMtu;

    private boolean mCarrierEnabled;
    private int mBearer;
    private int mBearerBitmask;

    private int mProfileId;

    private boolean mModemCognitive;
    private int mMaxConns;
    private int mWaitTime;
    private int mMaxConnsTime;

    private String mMvnoType;
    private String mMvnoMatchData;

    private boolean mPermanentFailed = false;

    /**
     * Return the types bitmap of the APN.
     *
     * @return types bitmap of the APN
     * @hide
     */
    public int getTypesBitmap() {
        return mTypesBitmap;
    }

    /**
     * Set the types bitmap of the APN.
     *
     * @param typesBitmap the types bitmap to set for the APN
     * @hide
     */
    public void setTypesBitmap(int typesBitmap) {
        mTypesBitmap = typesBitmap;
    }

    /**
     * Return the MTU size of the mobile interface to  which the APN connected.
     *
     * @return the MTU size of the APN
     * @hide
     */
    public int getMtu() {
        return mMtu;
    }

    /**
     * Set the MTU size of the mobile interface to  which the APN connected.
     *
     * @param mtu the MTU size to set for the APN
     * @hide
     */
    public void setMtu(int mtu) {
        mMtu = mtu;
    }

    /**
     * Radio Access Technology info.
     * To check what values can hold, refer to ServiceState.java.
     * This should be spread to other technologies,
     * but currently only used for LTE(14) and EHRPD(13).
     *
     * @return the bearer info of the APN
     * @hide
     */
    public int getBearer() {
        return mBearer;
    }

    /**
     * Set bearer info.
     *
     * @param bearer the bearer info to set for the APN
     * @hide
     */
    public void setBearer(int bearer) {
        mBearer = bearer;
    }

    /**
     * Get the profile id to which the APN saved in modem.
     *
     * @return the profile id of the APN
     * @hide
     */
    public int getProfileId() {
        return mProfileId;
    }

    /**
     * Set the profile id to which the APN saved in modem.
     *
     * @param profileId the profile id to set for the APN
     * @hide
     */
    public void setProfileId(int profileId) { mProfileId = profileId;
    }

    /**
     * Get if the APN setting is to be set in modem.
     *
     * @return is the APN setting to be set in modem
     * @hide
     */
    public boolean getModemCognitive() {
        return mModemCognitive;
    }

    /**
     * Set if the APN setting is to be set in modem.
     *
     * @param modemCognitive if the APN setting is to be set in modem
     * @hide
     */
    public void setModemCognitive(boolean modemCognitive) {
        mModemCognitive = modemCognitive;
    }

    /**
     * Get the max connections of this APN.
     *
     * @return the max connections of this APN
     * @hide
     */
    public int getMaxConns() {
        return mMaxConns;
    }

    /**
     * Set the max connections of this APN.
     *
     * @param maxConns the max connections of this APN
     * @hide
     */
    public void setMaxConns(int maxConns) {
        mMaxConns = maxConns;
    }

    /**
     * Get the wait time for retry of the APN.
     *
     * @return the wait time for retry of the APN
     * @hide
     */
    public int getWaitTime() {
        return mWaitTime;
    }

    /**
     * Set the wait time for retry of the APN.
     *
     * @param waitTime the wait time for retry of the APN
     * @hide
     */
    public void setWaitTime(int waitTime) {
        mWaitTime = waitTime;
    }

    /**
     * Get the time to limit max connection for the APN.
     *
     * @return the time to limit max connection for the APN
     * @hide
     */
    public int getMaxConnsTime() {
        return mMaxConnsTime;
    }

    /**
     * Set the time to limit max connection for the APN.
     *
     * @param maxConnsTime the time to limit max connection for the APN
     * @hide
     */
    public void setMaxConnsTime(int maxConnsTime) {
        mMaxConnsTime = maxConnsTime;
    }

    /**
     * Get the MVNO data. Examples:
     *   "spn": A MOBILE, BEN NL
     *   "imsi": 302720x94, 2060188
     *   "gid": 4E, 33
     *   "iccid": 898603 etc..
     *
     * @return the mvno match data
     * @hide
     */
    public String getMvnoMatchData() {
        return mMvnoMatchData;
    }

    /**
     * Set the MVNO match data for the APN.
     *
     * @param mvnoMatchData the MVNO match data for the APN
     * @hide
     */
    public void setMvnoMatchData(String mvnoMatchData) {
        mMvnoMatchData = mvnoMatchData;
    }

    /**
     * Indicates this APN setting is permanently failed and cannot be
     * retried by the retry manager anymore.
     *
     * @return if this APN setting is permanently failed
     * @hide
     */
    public boolean getPermanentFailed() {
        return mPermanentFailed;
    }

    /**
     * Set if this APN setting is permanently failed.
     *
     * @param permanentFailed if this APN setting is permanently failed
     * @hide
     */
    public void setPermanentFailed(boolean permanentFailed) {
        mPermanentFailed = permanentFailed;
    }

    /**
     * Return the entry name of the APN.
     *
     * @see android.provider.Telephony.Carriers#Name
     * @return the entry name for the APN
     */
    public String getCarrier() {
        return mCarrier;
    }

    /**
     * Set the entry name of the APN.
     *
     * @see android.provider.Telephony.Carriers#Name
     * @param carrier the entry name to set for the APN
     */
    public void setCarrier(String carrier) {
        mCarrier = carrier;
    }

    /**
     * Return the name of the APN.
     *
     * @see android.provider.Telephony.Carriers#APN
     * @return APN name
     */
    public String getApn() {
        return mApn;
    }

    /**
     * Set the name of the APN.
     *
     * @see android.provider.Telephony.Carriers#APN
     * @param apn the name to set for the APN
     */
    public void setApn(String apn) {
        mApn = apn;
    }

    /**
     * Return the proxy address of the APN.
     *
     * @see android.provider.Telephony.Carriers#PROXY
     * @return proxy address.
     */
    public String getProxy() {
        return mProxy;
    }

    /**
     * Set the proxy address of the APN.
     *
     * @see android.provider.Telephony.Carriers#PROXY
     * @param proxy the proxy address to set for the APN
     */
    public void setProxy(String proxy) {
        mProxy = proxy;
    }

    /**
     * Return the proxy port of the APN.
     *
     * @see android.provider.Telephony.Carriers#PORT
     * @return proxy port
     */
    public String getPort() {
        return mPort;
    }

    /**
     * Set the proxy port of the APN.
     *
     * @see android.provider.Telephony.Carriers#PORT
     * @param port the proxy port to set for the APN
     */
    public void setPort(String port) {
        mPort = port;
    }

    /**
     * Return the MMSC URL of the APN.
     *
     * @see android.provider.Telephony.Carriers#MMSC
     * @return MMSC URL
     */
    public String getMmsc() {
        return mMmsc;
    }

    /**
     * Set the MMSC URL of the APN.
     *
     * @see android.provider.Telephony.Carriers#MMSC
     * @param mmsc the MMSC URL to set for the APN
     */
    public void setMmsc(String mmsc) {
        mMmsc = mmsc;
    }

    /**
     * Return the MMS proxy address of the APN.
     *
     * @see android.provider.Telephony.Carriers#MMSPROXY
     * @return MMS proxy address
     */
    public String getMmsProxy() {
        return mMmsProxy;
    }

    /**
     * Set the MMS proxy address of the APN.
     *
     * @see android.provider.Telephony.Carriers#MMSPROXY
     * @param mmsProxy the MMS proxy address to set for the APN
     */
    public void setMmsProxy(String mmsProxy) {
        mMmsProxy = mmsProxy;
    }

    /**
     * Return the MMS proxy port of the APN.
     *
     * @see android.provider.Telephony.Carriers#MMSPORT
     * @return MMS proxy port
     */
    public String getMmsPort() {
        return mMmsPort;
    }

    /**
     * Set the MMS proxy port of the APN.
     *
     * @see android.provider.Telephony.Carriers#MMSPORT
     * @param mmsPort the MMS proxy port to set for the APN
     */
    public void setMmsPort(String mmsPort) {
        mMmsPort = mmsPort;
    }

    /**
     * Return the APN username of the APN.
     *
     * @see android.provider.Telephony.Carriers#USER
     * @return APN username
     */
    public String getUser() {
        return mUser;
    }

    /**
     * Set the APN username of the APN.
     *
     * @see android.provider.Telephony.Carriers#USER
     * @param user the APN username to set for the APN
     */
    public void setUser(String user) {
        mUser = user;
    }

    /**
     * Return the APN password of the APN.
     *
     * @see android.provider.Telephony.Carriers#PASSWORD
     * @return APN password
     */
    public String getPassword() {
        return mPassword;
    }

    /**
     * Set the APN password of the APN.
     *
     * @see android.provider.Telephony.Carriers#PASSWORD
     * @param password the APN password to set for the APN
     */
    public void setPassword(String password) {
        mPassword = password;
    }

    /**
     * Return the authentication type of the APN.
     *
     * @see android.provider.Telephony.Carriers#AUTH_TYPE
     * @return uuthentication type
     */
    public int getAuthType() {
        return mAuthType;
    }

    /**
     * Set the authentication type of the APN.
     *
     * The authentication type can have 4 values: 0 (None), 1 (PAP), 2 (CHAP), 3 (PAP or CHAP).
     *
     * @see android.provider.Telephony.Carriers#AUTH_TYPE
     * @param authType the authentication type to set for the APN
     */
    public void setAuthType(int authType) {
        mAuthType = authType;
    }

    /**
     * Return the list of APN types of the APN.
     *
     * @see android.provider.Telephony.Carriers#TYPE
     * @return the list of APN types
     */
    public List<String> getTypes() {
        return Arrays.asList(mTypes);
    }

    /**
     * Set the list of APN types of the APN. Possible values can be found in {@link Type}.
     *
     * @see #Type
     * @see android.provider.Telephony.Carriers#TYPE
     * @param types the list of APN types to set for the APN
     */
    public void setTypes(List<String> types) {
        mTypes = types.toArray(new String[0]);
    }

    /**
     * Return the unique database id for this entry.
     *
     * @see android.provider.Telephony.Carriers#_ID
     * @return the unique database id
     */
    public int getId() {
        return mId;
    }

    /**
     * Set the unique database id for this entry.
     *
     * @see android.provider.Telephony.Carriers#_ID
     * @param id the unique database id to set for this entry
     */
    public void setId(int id) {
        mId = id;
    }

    /**
     * Return the numeric operator ID for the APN. Usually
     * {@link android.provider.Telephony.Carriers#MCC} +
     * {@link android.provider.Telephony.Carriers#MNC}.
     *
     * @see android.provider.Telephony.Carriers#NUMERIC
     * @return the numeric operator ID
     */
    public String getNumeric() {
        return mNumeric;
    }

    /**
     * Set the numeric operator ID for the APN.
     *
     * @see android.provider.Telephony.Carriers#NUMERIC
     * @param numeric the numeric operator ID to set for this entry
     */
    public void setNumeric(String numeric) {
        mNumeric = numeric;
    }

    /**
     * Return the protocol to use to connect to this APN.
     *
     * One of the {@code PDP_type} values in TS 27.007 section 10.1.1.
     * For example: {@code IP}, {@code IPV6}, {@code IPV4V6}, or {@code PPP}.
     *
     * @see android.provider.Telephony.Carriers#PROTOCOL
     * @return the protocol
     */
    public String getProtocol() {
        return mProtocol;
    }

    /**
     * Set the protocol to use to connect to this APN.
     *
     * @see android.provider.Telephony.Carriers#PROTOCOL
     * @param protocol the protocol to set to use to connect to this APN
     */
    public void setProtocol(String protocol) {
        mProtocol = protocol;
    }

    /**
     * Return the protocol to use to connect to this APN when roaming.
     *
     * The syntax is the same as {@link android.provider.Telephony.Carriers#PROTOCOL}.
     *
     * @see android.provider.Telephony.Carriers#ROAMING_PROTOCOL
     * @return the roaming protocol
     */
    public String getRoamingProtocol() {
        return mRoamingProtocol;
    }

    /**
     * Set the protocol to use to connect to this APN when roaming.
     *
     * @see android.provider.Telephony.Carriers#ROAMING_PROTOCOL
     * @param roamingProtocol the protocol to set to use to connect to this APN when roaming
     */
    public void setRoadmingProtocol(String roamingProtocol) {
        mRoamingProtocol = roamingProtocol;
    }

    /**
     * Return the current status of APN.
     *
     * {@code true} : enabled APN.
     * {@code false} : disabled APN.
     *
     * @see android.provider.Telephony.Carriers#CARRIER_ENABLED
     * @return the current status
     */
    public boolean getCarrierEnabled() {
        return mCarrierEnabled;
    }

    /**
     * Set the current status of APN.
     *
     * @see android.provider.Telephony.Carriers#CARRIER_ENABLED
     * @param carrierEnabled the current status to set for this APN
     */
    public void setCarrierEnabled(boolean carrierEnabled) {
        mCarrierEnabled = carrierEnabled;
    }

    /**
     * Return the radio access technology bitmask for this APN.
     *
     * To check what values can hold, refer to ServiceState.java. This is a bitmask of radio
     * technologies in ServiceState.
     * This should be spread to other technologies,
     * but currently only used for LTE(14) and EHRPD(13).
     *
     * @see android.provider.Telephony.Carriers#BEARER_BITMASK
     * @return the radio access technology bitmask
     */
    public int getBearerBitmask() {
        return mBearerBitmask;
    }

    /**
     * Set the radio access technology bitmask for this APN.
     *
     * @see android.provider.Telephony.Carriers#CARRIER_ENABLED
     * @param bearerBitmask the radio access technology bitmask to set for this APN
     */
    public void setBearerBitmask(int bearerBitmask) {
        mBearerBitmask = bearerBitmask;
    }

    /**
     * Return the MVNO match type for this APN.
     *
     * Possible values:
     *   "spn": Service provider name.
     *   "imsi": IMSI.
     *   "gid": Group identifier level 1.
     *   "iccid": ICCID
     *
     * @see android.provider.Telephony.Carriers#MVNO_TYPE
     * @return the MVNO match type
     */
    public String getMvnoType() {
        return mMvnoType;
    }

    /**
     * Set the MVNO match type for this APN.
     *
     * @see android.provider.Telephony.Carriers#MVNO_TYPE
     * @param mvnoType the MVNO match type to set for this APN
     */
    public void setMvnoType(String mvnoType) {
        mMvnoType = mvnoType;
    }

    /** @hide */
    public ApnSetting(int id, String numeric, String carrier, String apn, String proxy, String port,
            String mmsc, String mmsProxy, String mmsPort,
            String user, String password, int authType, String[] types,
            String protocol, String roamingProtocol, boolean carrierEnabled, int bearer,
            int bearerBitmask, int profileId, boolean modemCognitive, int maxConns,
            int waitTime, int maxConnsTime, int mtu, String mvnoType,
            String mvnoMatchData) {
        this.mId = id;
        this.mNumeric = numeric;
        this.mCarrier = carrier;
        this.mApn = apn;
        this.mProxy = proxy;
        this.mPort = port;
        this.mMmsc = mmsc;
        this.mMmsProxy = mmsProxy;
        this.mMmsPort = mmsPort;
        this.mUser = user;
        this.mPassword = password;
        this.mAuthType = authType;
        this.mTypes = new String[types.length];
        int apnBitmap = 0;
        for (int i = 0; i < types.length; i++) {
            this.mTypes[i] = types[i].toLowerCase();
            apnBitmap |= getApnBitmask(this.mTypes[i]);
        }
        this.mTypesBitmap = apnBitmap;
        this.mProtocol = protocol;
        this.mRoamingProtocol = roamingProtocol;
        this.mCarrierEnabled = carrierEnabled;
        this.mBearer = bearer;
        this.mBearerBitmask = (bearerBitmask | ServiceState.getBitmaskForTech(bearer));
        this.mProfileId = profileId;
        this.mModemCognitive = modemCognitive;
        this.mMaxConns = maxConns;
        this.mWaitTime = waitTime;
        this.mMaxConnsTime = maxConnsTime;
        this.mMtu = mtu;
        this.mMvnoType = mvnoType;
        this.mMvnoMatchData = mvnoMatchData;
    }

    public ApnSetting() {
        this(0, null, null, null, null, null, null, null, null,
        null, null, 0, new String[]{}, null, null, true, 0, 0, 0,
        false, 0, 0, 0, 0, null, null);
    }

    public ApnSetting(ApnSetting apn) {
        this(apn.mId, apn.mNumeric, apn.mCarrier, apn.mApn, apn.mProxy, apn.mPort, apn.mMmsc,
                apn.mMmsProxy, apn.mMmsPort, apn.mUser, apn.mPassword, apn.mAuthType, apn.mTypes,
                apn.mProtocol, apn.mRoamingProtocol, apn.mCarrierEnabled, apn.mBearer,
                apn.mBearerBitmask, apn.mProfileId, apn.mModemCognitive, apn.mMaxConns,
                apn.mWaitTime, apn.mMaxConnsTime, apn.mMtu, apn.mMvnoType, apn.mMvnoMatchData);
    }

    /** @hide */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ApnSettingV3] ")
                .append(mCarrier)
                .append(", ").append(mId)
                .append(", ").append(mNumeric)
                .append(", ").append(mApn)
                .append(", ").append(mProxy)
                .append(", ").append(mMmsc)
                .append(", ").append(mMmsProxy)
                .append(", ").append(mMmsPort)
                .append(", ").append(mPort)
                .append(", ").append(mAuthType).append(", ");
        for (int i = 0; i < mTypes.length; i++) {
            sb.append(mTypes[i]);
            if (i < mTypes.length - 1) {
                sb.append(" | ");
            }
        }
        sb.append(", ").append(mProtocol);
        sb.append(", ").append(mRoamingProtocol);
        sb.append(", ").append(mCarrierEnabled);
        sb.append(", ").append(mBearer);
        sb.append(", ").append(mBearerBitmask);
        sb.append(", ").append(mProfileId);
        sb.append(", ").append(mModemCognitive);
        sb.append(", ").append(mMaxConns);
        sb.append(", ").append(mWaitTime);
        sb.append(", ").append(mMaxConnsTime);
        sb.append(", ").append(mMtu);
        sb.append(", ").append(mMvnoType);
        sb.append(", ").append(mMvnoMatchData);
        sb.append(", ").append(mPermanentFailed);
        return sb.toString();
    }

    /**
     * Returns true if there are MVNO params specified.
     * @hide
     */
    public boolean hasMvnoParams() {
        return !TextUtils.isEmpty(mMvnoType) && !TextUtils.isEmpty(mMvnoMatchData);
    }

    /** @hide */
    public boolean canHandleType(String type) {
        if (!mCarrierEnabled) return false;
        boolean wildcardable = true;
        if (Type.IA.equalsIgnoreCase(type)) wildcardable = false;
        for (String t : mTypes) {
            // DEFAULT handles all, and HIPRI is handled by DEFAULT
            if (t.equalsIgnoreCase(type)
                    || (wildcardable && t.equalsIgnoreCase(Type.ALL))
                    || (t.equalsIgnoreCase(Type.DEFAULT)
                    && type.equalsIgnoreCase(Type.HIPRI))) {
                return true;
            }
        }
        return false;
    }

    // check whether the types of two APN same (even only one type of each APN is same)
    /** @hide */
    private boolean typeSameAny(ApnSetting first, ApnSetting second) {
        if (VDBG) {
            StringBuilder apnType1 = new StringBuilder(first.mApn + ": ");
            for (int index1 = 0; index1 < first.mTypes.length; index1++) {
                apnType1.append(first.mTypes[index1]);
                apnType1.append(",");
            }

            StringBuilder apnType2 = new StringBuilder(second.mApn + ": ");
            for (int index1 = 0; index1 < second.mTypes.length; index1++) {
                apnType2.append(second.mTypes[index1]);
                apnType2.append(",");
            }
            Rlog.d(LOG_TAG, "APN1: is " + apnType1);
            Rlog.d(LOG_TAG, "APN2: is " + apnType2);
        }

        for (int index1 = 0; index1 < first.mTypes.length; index1++) {
            for (int index2 = 0; index2 < second.mTypes.length; index2++) {
                if (first.mTypes[index1].equals(ApnSetting.Type.ALL)
                || second.mTypes[index2].equals(ApnSetting.Type.ALL)
                || first.mTypes[index1].equals(second.mTypes[index2])) {
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

        return mCarrier.equals(other.mCarrier)
        && Objects.equals(mId, other.mId)
        && mNumeric.equals(other.mNumeric)
        && mApn.equals(other.mApn)
        && mProxy.equals(other.mProxy)
        && mMmsc.equals(other.mMmsc)
        && mMmsProxy.equals(other.mMmsProxy)
        && TextUtils.equals(mMmsPort, other.mMmsPort)
        && mPort.equals(other.mPort)
        && TextUtils.equals(mUser, other.mUser)
        && TextUtils.equals(mPassword, other.mPassword)
        && Objects.equals(mAuthType, other.mAuthType)
        && Arrays.deepEquals(mTypes, other.mTypes)
        && Objects.equals(mTypesBitmap, other.mTypesBitmap)
        && mProtocol.equals(other.mProtocol)
        && mRoamingProtocol.equals(other.mRoamingProtocol)
        && Objects.equals(mCarrierEnabled, other.mCarrierEnabled)
        && Objects.equals(mBearer, other.mBearer)
        && Objects.equals(mBearerBitmask, other.mBearerBitmask)
        && Objects.equals(mProfileId, other.mProfileId)
        && Objects.equals(mModemCognitive, other.mModemCognitive)
        && Objects.equals(mMaxConns, other.mMaxConns)
        && Objects.equals(mWaitTime, other.mWaitTime)
        && Objects.equals(mMaxConnsTime, other.mMaxConnsTime)
        && Objects.equals(mMtu, other.mMtu)
        && mMvnoType.equals(other.mMvnoType)
        && mMvnoMatchData.equals(other.mMvnoMatchData);
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

        return mCarrier.equals(other.mCarrier)
        && mNumeric.equals(other.mNumeric)
        && mApn.equals(other.mApn)
        && mProxy.equals(other.mProxy)
        && mMmsc.equals(other.mMmsc)
        && mMmsProxy.equals(other.mMmsProxy)
        && TextUtils.equals(mMmsPort, other.mMmsPort)
        && mPort.equals(other.mPort)
        && TextUtils.equals(mUser, other.mUser)
        && TextUtils.equals(mPassword, other.mPassword)
        && Objects.equals(mAuthType, other.mAuthType)
        && Arrays.deepEquals(mTypes, other.mTypes)
        && Objects.equals(mTypesBitmap, other.mTypesBitmap)
        && (isDataRoaming || mProtocol.equals(other.mProtocol))
        && (!isDataRoaming || mRoamingProtocol.equals(other.mRoamingProtocol))
        && Objects.equals(mCarrierEnabled, other.mCarrierEnabled)
        && Objects.equals(mProfileId, other.mProfileId)
        && Objects.equals(mModemCognitive, other.mModemCognitive)
        && Objects.equals(mMaxConns, other.mMaxConns)
        && Objects.equals(mWaitTime, other.mWaitTime)
        && Objects.equals(mMaxConnsTime, other.mMaxConnsTime)
        && Objects.equals(mMtu, other.mMtu)
        && mMvnoType.equals(other.mMvnoType)
        && mMvnoMatchData.equals(other.mMvnoMatchData);
    }

    /**
     * Check if neither mention DUN and are substantially similar
     *
     * @param other The other APN settings to compare
     * @return True if two APN settings are similar
     * @hide
     */
    public boolean similar(ApnSetting other) {
        return (!this.canHandleType(Type.DUN)
                && !other.canHandleType(Type.DUN)
                && Objects.equals(this.mApn, other.mApn)
                && !typeSameAny(this, other)
                && xorEquals(this.mProxy, other.mProxy)
                && xorEquals(this.mPort, other.mPort)
                && xorEquals(this.mProtocol, other.mProtocol)
                && xorEquals(this.mRoamingProtocol, other.mRoamingProtocol)
                && this.mCarrierEnabled == other.mCarrierEnabled
                && this.mBearerBitmask == other.mBearerBitmask
                && this.mProfileId == other.mProfileId
                && Objects.equals(this.mMvnoType, other.mMvnoType)
                && Objects.equals(this.mMvnoMatchData, other.mMvnoMatchData)
                && xorEquals(this.mMmsc, other.mMmsc)
                && xorEquals(this.mMmsProxy, other.mMmsProxy)
                && xorEquals(this.mMmsPort, other.mMmsPort));
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
            case Type.DEFAULT: return ApnTypes.DEFAULT;
            case Type.MMS: return ApnTypes.MMS;
            case Type.SUPL: return ApnTypes.SUPL;
            case Type.DUN: return ApnTypes.DUN;
            case Type.HIPRI: return ApnTypes.HIPRI;
            case Type.FOTA: return ApnTypes.FOTA;
            case Type.IMS: return ApnTypes.IMS;
            case Type.CBS: return ApnTypes.CBS;
            case Type.IA: return ApnTypes.IA;
            case Type.EMERGENCY: return ApnTypes.EMERGENCY;
            case Type.ALL: return ApnTypes.ALL;
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
        if (mNumeric != null) {
            apnValue.put(Telephony.Carriers.NUMERIC, mNumeric);
        }
        if (mCarrier != null) {
            apnValue.put(Telephony.Carriers.NAME, mCarrier);
        }
        if (mApn != null) {
            apnValue.put(Telephony.Carriers.APN, mApn);
        }
        if (mProxy != null) {
            apnValue.put(Telephony.Carriers.PROXY, mProxy);
        }
        if (mPort != null) {
            apnValue.put(Telephony.Carriers.PORT, mPort);
        }
        if (mMmsc != null) {
            apnValue.put(Telephony.Carriers.MMSC, mMmsc);
        }
        if (mMmsPort != null) {
            apnValue.put(Telephony.Carriers.MMSPORT, mMmsPort);
        }
        if (mMmsProxy != null) {
            apnValue.put(Telephony.Carriers.MMSPROXY, mMmsProxy);
        }
        if (mUser != null) {
            apnValue.put(Telephony.Carriers.USER, mUser);
        }
        if (mPassword != null) {
            apnValue.put(Telephony.Carriers.PASSWORD, mPassword);
        }
        apnValue.put(Telephony.Carriers.AUTH_TYPE, mAuthType);
        String apnType = deParseTypes(mTypes);
        if (apnType != null) {
            apnValue.put(Telephony.Carriers.TYPE, apnType);
        }
        if (mProtocol != null) {
            apnValue.put(Telephony.Carriers.PROTOCOL, mProtocol);
        }
        if (mRoamingProtocol != null) {
            apnValue.put(Telephony.Carriers.ROAMING_PROTOCOL, mRoamingProtocol);
        }
        apnValue.put(Telephony.Carriers.CARRIER_ENABLED, mCarrierEnabled);
        // networkTypeBit.
        apnValue.put(Telephony.Carriers.BEARER_BITMASK, mBearerBitmask);
        if (mMvnoType != null) {
            apnValue.put(Telephony.Carriers.MVNO_TYPE, mMvnoType);
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
            result[0] = Type.ALL;
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
        dest.writeInt(mId);
        dest.writeString(mNumeric);
        dest.writeString(mCarrier);
        dest.writeString(mApn);
        dest.writeString(mProxy);
        dest.writeString(mPort);
        dest.writeString(mMmsc);
        dest.writeString(mMmsProxy);
        dest.writeString(mMmsPort);
        dest.writeString(mUser);
        dest.writeString(mPassword);
        dest.writeInt(mAuthType);
        dest.writeStringArray(mTypes);
        dest.writeString(mProtocol);
        dest.writeString(mRoamingProtocol);
        dest.writeInt(mCarrierEnabled ? 1: 0);
        dest.writeInt(mBearerBitmask);
        dest.writeString(mMvnoType);
    }

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
         * ALL is a special type to indicate that this APN entry can
         * service all data connections.
         */
        public static final String ALL = "*";
        /** APN type for default data traffic */
        public static final String DEFAULT = "default";
        /** APN type for MMS traffic */
        public static final String MMS = "mms";
        /** APN type for SUPL assisted GPS */
        public static final String SUPL = "supl";
        /** APN type for DUN traffic */
        public static final String DUN = "dun";
        /** APN type for HiPri traffic */
        public static final String HIPRI = "hipri";
        /** APN type for FOTA */
        public static final String FOTA = "fota";
        /** APN type for IMS */
        public static final String IMS = "ims";
        /** APN type for CBS */
        public static final String CBS = "cbs";
        /** APN type for IA Initial Attach APN */
        public static final String IA = "ia";
        /** APN type for Emergency PDN. This is not an IA apn, but is used
         * for access to carrier services in an emergency call situation. */
        public static final String EMERGENCY = "emergency";
        /**
         * Array of all APN types
         *
         * @hide
         */
        public static final String[] ALL_TYPES =
        {
            DEFAULT,
            MMS,
            SUPL,
            DUN,
            HIPRI,
            FOTA,
            IMS,
            CBS,
            IA,
            EMERGENCY
        };
    }
}

