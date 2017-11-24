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

    private String carrier;
    private String apn;
    private String proxy;
    private String port;
    private String mmsc;
    private String mmsProxy;
    private String mmsPort;
    private String user;
    private String password;
    private int authType;
    private String[] types;
    private int typesBitmap;
    private int id;
    private String numeric;
    private String protocol;
    private String roamingProtocol;
    private int mtu;

    private boolean carrierEnabled;
    private int bearer;
    private int bearerBitmask;

    private int profileId;

    private boolean modemCognitive;
    private int maxConns;
    private int waitTime;
    private int maxConnsTime;

    private String mvnoType;
    private String mvnoMatchData;

    private boolean permanentFailed = false;

    /**
     * Return the types bitmap of the APN.
     *
     * @return types bitmap of the APN
     * @hide
     */
    public int getTypesBitmap() {
        return typesBitmap;
    }

    /**
     * Set the types bitmap of the APN.
     *
     * @param typesBitmap the types bitmap to set for the APN
     * @hide
     */
    public void setTypesBitmap(int typesBitmap) {
        this.typesBitmap = typesBitmap;
    }

    /**
     * Return the MTU size of the mobile interface to  which the APN connected.
     *
     * @return the MTU size of the APN
     * @hide
     */
    public int getMtu() {
        return mtu;
    }

    /**
     * Set the MTU size of the mobile interface to  which the APN connected.
     *
     * @param mtu the MTU size to set for the APN
     * @hide
     */
    public void setMtu(int mtu) {
        this.mtu = mtu;
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
        return bearer;
    }

    /**
     * Set bearer info.
     *
     * @param bearer the bearer info to set for the APN
     * @hide
     */
    public void setBearer(int bearer) {
        this.bearer = bearer;
    }

    /**
     * Get the profile id to which the APN saved in modem.
     *
     * @return the profile id of the APN
     * @hide
     */
    public int getProfileId() {
        return profileId;
    }

    /**
     * Set the profile id to which the APN saved in modem.
     *
     * @param profileId the profile id to set for the APN
     * @hide
     */
    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    /**
     * Get if the APN setting is to be set in modem.
     *
     * @return is the APN setting to be set in modem
     * @hide
     */
    public boolean getModemCognitive() {
        return modemCognitive;
    }

    /**
     * Set if the APN setting is to be set in modem.
     *
     * @param modemCognitive if the APN setting is to be set in modem
     * @hide
     */
    public void setModemCognitive(boolean modemCognitive) {
        this.modemCognitive = modemCognitive;
    }

    /**
     * Get the max connections of this APN.
     *
     * @return the max connections of this APN
     * @hide
     */
    public int getMaxConns() {
        return maxConns;
    }

    /**
     * Set the max connections of this APN.
     *
     * @param maxConns the max connections of this APN
     * @hide
     */
    public void setMaxConns(int maxConns) {
        this.maxConns = maxConns;
    }

    /**
     * Get the wait time for retry of the APN.
     *
     * @return the wait time for retry of the APN
     * @hide
     */
    public int getWaitTime() {
        return waitTime;
    }

    /**
     * Set the wait time for retry of the APN.
     *
     * @param waitTime the wait time for retry of the APN
     * @hide
     */
    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    /**
     * Get the time to limit max connection for the APN.
     *
     * @return the time to limit max connection for the APN
     * @hide
     */
    public int getMaxConnsTime() {
        return maxConnsTime;
    }

    /**
     * Set the time to limit max connection for the APN.
     *
     * @param maxConnsTime the time to limit max connection for the APN
     * @hide
     */
    public void setMaxConnsTime(int maxConnsTime) {
        this.maxConnsTime = maxConnsTime;
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
        return mvnoMatchData;
    }

    /**
     * Set the MVNO match data for the APN.
     *
     * @param mvnoMatchData the MVNO match data for the APN
     * @hide
     */
    public void setMvnoMatchData(String mvnoMatchData) {
        this.mvnoMatchData = mvnoMatchData;
    }

    /**
     * Indicates this APN setting is permanently failed and cannot be
     * retried by the retry manager anymore.
     *
     * @return if this APN setting is permanently failed
     * @hide
     */
    public boolean getPermanentFailed() {
        return permanentFailed;
    }

    /**
     * Set if this APN setting is permanently failed.
     *
     * @param permanentFailed if this APN setting is permanently failed
     * @hide
     */
    public void setPermanentFailed(boolean permanentFailed) {
        this.permanentFailed = permanentFailed;
    }

    /**
     * Return the entry name of the APN.
     *
     * @see android.provider.Telephony.Carriers#Name
     * @return the entry name for the APN
     */
    public String getCarrier() {
        return carrier;
    }

    /**
     * Set the entry name of the APN.
     *
     * @see android.provider.Telephony.Carriers#Name
     * @param carrier the entry name to set for the APN
     */
    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    /**
     * Return the name of the APN.
     *
     * @see android.provider.Telephony.Carriers#APN
     * @return APN name
     */
    public String getApn() {
        return apn;
    }

    /**
     * Set the name of the APN.
     *
     * @see android.provider.Telephony.Carriers#APN
     * @param apn the name to set for the APN
     */
    public void setApn(String apn) {
        this.apn = apn;
    }

    /**
     * Return the proxy address of the APN.
     *
     * @see android.provider.Telephony.Carriers#PROXY
     * @return proxy address.
     */
    public String getProxy() {
        return proxy;
    }

    /**
     * Set the proxy address of the APN.
     *
     * @see android.provider.Telephony.Carriers#PROXY
     * @param proxy the proxy address to set for the APN
     */
    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    /**
     * Return the proxy port of the APN.
     *
     * @see android.provider.Telephony.Carriers#PORT
     * @return proxy port
     */
    public String getPort() {
        return port;
    }

    /**
     * Set the proxy port of the APN.
     *
     * @see android.provider.Telephony.Carriers#PORT
     * @param port the proxy port to set for the APN
     */
    public void setPort(String port) {
        this.port = port;
    }

    /**
     * Return the MMSC URL of the APN.
     *
     * @see android.provider.Telephony.Carriers#MMSC
     * @return MMSC URL
     */
    public String getMmsc() {
        return mmsc;
    }

    /**
     * Set the MMSC URL of the APN.
     *
     * @see android.provider.Telephony.Carriers#MMSC
     * @param mmsc the MMSC URL to set for the APN
     */
    public void setMmsc(String mmsc) {
        this.mmsc = mmsc;
    }

    /**
     * Return the MMS proxy address of the APN.
     *
     * @see android.provider.Telephony.Carriers#MMSPROXY
     * @return MMS proxy address
     */
    public String getMmsProxy() {
        return mmsProxy;
    }

    /**
     * Set the MMS proxy address of the APN.
     *
     * @see android.provider.Telephony.Carriers#MMSPROXY
     * @param mmsProxy the MMS proxy address to set for the APN
     */
    public void setMmsProxy(String mmsProxy) {
        this.mmsProxy = mmsProxy;
    }

    /**
     * Return the MMS proxy port of the APN.
     *
     * @see android.provider.Telephony.Carriers#MMSPORT
     * @return MMS proxy port
     */
    public String getMmsPort() {
        return mmsPort;
    }

    /**
     * Set the MMS proxy port of the APN.
     *
     * @see android.provider.Telephony.Carriers#MMSPORT
     * @param mmsPort the MMS proxy port to set for the APN
     */
    public void setMmsPort(String mmsPort) {
        this.mmsPort = mmsPort;
    }

    /**
     * Return the APN username of the APN.
     *
     * @see android.provider.Telephony.Carriers#USER
     * @return APN username
     */
    public String getUser() {
        return user;
    }

    /**
     * Set the APN username of the APN.
     *
     * @see android.provider.Telephony.Carriers#USER
     * @param user the APN username to set for the APN
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Return the APN password of the APN.
     *
     * @see android.provider.Telephony.Carriers#PASSWORD
     * @return APN password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the APN password of the APN.
     *
     * @see android.provider.Telephony.Carriers#PASSWORD
     * @param password the APN password to set for the APN
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Return the authentication type of the APN.
     *
     * @see android.provider.Telephony.Carriers#AUTH_TYPE
     * @return uuthentication type
     */
    public int getAuthType() {
        return authType;
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
        this.authType = authType;
    }

    /**
     * Return the list of APN types of the APN.
     *
     * @see android.provider.Telephony.Carriers#TYPE
     * @return the list of APN types
     */
    public List<String> getTypes() {
        return Arrays.asList(types);
    }

    /**
     * Set the list of APN types of the APN. Possible values can be found in {@link Type}.
     *
     * @see #Type
     * @see android.provider.Telephony.Carriers#TYPE
     * @param types the list of APN types to set for the APN
     */
    public void setTypes(List<String> types) {
        this.types = types.toArray(new String[0]);
    }

    /**
     * Return the unique database id for this entry.
     *
     * @see android.provider.Telephony.Carriers#_ID
     * @return the unique database id
     */
    public int getId() {
        return id;
    }

    /**
     * Set the unique database id for this entry.
     *
     * @see android.provider.Telephony.Carriers#_ID
     * @param id the unique database id to set for this entry
     */
    public void setId(int id) {
        this.id = id;
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
        return numeric;
    }

    /**
     * Set the numeric operator ID for the APN.
     *
     * @see android.provider.Telephony.Carriers#NUMERIC
     * @param numeric the numeric operator ID to set for this entry
     */
    public void setNumeric(String numeric) {
        this.numeric = numeric;
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
        return protocol;
    }

    /**
     * Set the protocol to use to connect to this APN.
     *
     * @see android.provider.Telephony.Carriers#PROTOCOL
     * @param protocol the protocol to set to use to connect to this APN
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
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
        return roamingProtocol;
    }

    /**
     * Set the protocol to use to connect to this APN when roaming.
     *
     * @see android.provider.Telephony.Carriers#ROAMING_PROTOCOL
     * @param roamingProtocol the protocol to set to use to connect to this APN when roaming
     */
    public void setRoadmingProtocol(String roamingProtocol) {
        this.roamingProtocol = roamingProtocol;
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
        return carrierEnabled;
    }

    /**
     * Set the current status of APN.
     *
     * @see android.provider.Telephony.Carriers#CARRIER_ENABLED
     * @param carrierEnabled the current status to set for this APN
     */
    public void setCarrierEnabled(boolean carrierEnabled) {
        this.carrierEnabled = carrierEnabled;
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
        return bearerBitmask;
    }

    /**
     * Set the radio access technology bitmask for this APN.
     *
     * @see android.provider.Telephony.Carriers#CARRIER_ENABLED
     * @param bearerBitmask the radio access technology bitmask to set for this APN
     */
    public void setBearerBitmask(int bearerBitmask) {
        this.bearerBitmask = bearerBitmask;
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
        return mvnoType;
    }

    /**
     * Set the MVNO match type for this APN.
     *
     * @see android.provider.Telephony.Carriers#MVNO_TYPE
     * @param mvnoType the MVNO match type to set for this APN
     */
    public void setMvnoType(String mvnoType) {
        this.mvnoType = mvnoType;
    }

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
        this(0, null, null, null, null, null, null, null, null,
        null, null, 0, new String[]{}, null, null, true, 0, 0, 0,
        false, 0, 0, 0, 0, null, null);
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
        if (Type.IA.equalsIgnoreCase(type)) wildcardable = false;
        for (String t : types) {
            // DEFAULT handles all, and HIPRI is handled by DEFAULT
            if (t.equalsIgnoreCase(type) ||
            (wildcardable && t.equalsIgnoreCase(Type.ALL)) ||
            (t.equalsIgnoreCase(Type.DEFAULT) &&
            type.equalsIgnoreCase(Type.HIPRI))) {
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
                if (first.types[index1].equals(ApnSetting.Type.ALL)
                || second.types[index2].equals(ApnSetting.Type.ALL)
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
        && Objects.equals(id, other.id)
        && numeric.equals(other.numeric)
        && apn.equals(other.apn)
        && proxy.equals(other.proxy)
        && mmsc.equals(other.mmsc)
        && mmsProxy.equals(other.mmsProxy)
        && TextUtils.equals(mmsPort, other.mmsPort)
        && port.equals(other.port)
        && TextUtils.equals(user, other.user)
        && TextUtils.equals(password, other.password)
        && Objects.equals(authType, other.authType)
        && Arrays.deepEquals(types, other.types)
        && Objects.equals(typesBitmap, other.typesBitmap)
        && protocol.equals(other.protocol)
        && roamingProtocol.equals(other.roamingProtocol)
        && Objects.equals(carrierEnabled, other.carrierEnabled)
        && Objects.equals(bearer, other.bearer)
        && Objects.equals(bearerBitmask, other.bearerBitmask)
        && Objects.equals(profileId, other.profileId)
        && Objects.equals(modemCognitive, other.modemCognitive)
        && Objects.equals(maxConns, other.maxConns)
        && Objects.equals(waitTime, other.waitTime)
        && Objects.equals(maxConnsTime, other.maxConnsTime)
        && Objects.equals(mtu, other.mtu)
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
        && Objects.equals(authType, other.authType)
        && Arrays.deepEquals(types, other.types)
        && Objects.equals(typesBitmap, other.typesBitmap)
        && (isDataRoaming || protocol.equals(other.protocol))
        && (!isDataRoaming || roamingProtocol.equals(other.roamingProtocol))
        && Objects.equals(carrierEnabled, other.carrierEnabled)
        && Objects.equals(profileId, other.profileId)
        && Objects.equals(modemCognitive, other.modemCognitive)
        && Objects.equals(maxConns, other.maxConns)
        && Objects.equals(waitTime, other.waitTime)
        && Objects.equals(maxConnsTime, other.maxConnsTime)
        && Objects.equals(mtu, other.mtu)
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
        return (!this.canHandleType(Type.DUN)
        && !other.canHandleType(Type.DUN)
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

