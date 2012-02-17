/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.internal.telephony;

import java.util.List;

import android.net.LinkCapabilities;
import android.net.LinkProperties;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.PreciseCallState;
import android.util.Log;

import com.android.internal.telephony.ITelephonyRegistry;

/**
 * broadcast intents
 */
public class DefaultPhoneNotifier implements PhoneNotifier {

    static final String LOG_TAG = "GSM";
    private static final boolean DBG = true;
    public static final String UNKNOWN_ADDRESS = "";
    private ITelephonyRegistry mRegistry;

    /*package*/
    DefaultPhoneNotifier() {
        mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService(
                    "telephony.registry"));
    }

    public void notifyPhoneState(Phone sender) {
        Call ringingCall = sender.getRingingCall();
        String incomingNumber = "";
        if (ringingCall != null && ringingCall.getEarliestConnection() != null){
            incomingNumber = ringingCall.getEarliestConnection().getAddress();
        }
        try {
            mRegistry.notifyCallState(convertCallState(sender.getState()), incomingNumber);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyPreciseCallStateChanged(Phone sender) {
        Call ringingCall = sender.getRingingCall();
        Call foregroundCall = sender.getForegroundCall();
        Call backgroundCall = sender.getBackgroundCall();
        int currentRingingCallState = convertPreciseCallState(ringingCall.getState());
        int currentForegroundCallState = convertPreciseCallState(foregroundCall.getState());
        int currentBackgroundCallState = convertPreciseCallState(backgroundCall.getState());

        try {
            mRegistry.notifyPreciseCallState(
                    new PreciseCallState(currentRingingCallState,
                            getAddressesFromCall(ringingCall)),
                    new PreciseCallState(currentForegroundCallState,
                            getAddressesFromCall(foregroundCall)),
                    new PreciseCallState(currentBackgroundCallState,
                            getAddressesFromCall(backgroundCall)));
        } catch (RemoteException ex) {
          // system process is dead
        }
    }

    public void notifyServiceState(Phone sender) {
        ServiceState ss = sender.getServiceState();
        if (ss == null) {
            ss = new ServiceState();
            ss.setStateOutOfService();
        }
        try {
            mRegistry.notifyServiceState(ss);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifySignalStrength(Phone sender) {
        try {
            mRegistry.notifySignalStrength(sender.getSignalStrength());
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyMessageWaitingChanged(Phone sender) {
        try {
            mRegistry.notifyMessageWaitingChanged(sender.getMessageWaitingIndicator());
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyCallForwardingChanged(Phone sender) {
        try {
            mRegistry.notifyCallForwardingChanged(sender.getCallForwardingIndicator());
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyDataActivity(Phone sender) {
        try {
            mRegistry.notifyDataActivity(convertDataActivityState(sender.getDataActivityState()));
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyDataConnection(Phone sender, String reason, String apnType,
            Phone.DataState state) {
        doNotifyDataConnection(sender, reason, apnType, state);
    }

    private void doNotifyDataConnection(Phone sender, String reason, String apnType,
            Phone.DataState state) {
        // TODO
        // use apnType as the key to which connection we're talking about.
        // pass apnType back up to fetch particular for this one.
        TelephonyManager telephony = TelephonyManager.getDefault();
        LinkProperties linkProperties = null;
        LinkCapabilities linkCapabilities = null;
        boolean roaming = false;

        if (state == Phone.DataState.CONNECTED) {
            linkProperties = sender.getLinkProperties(apnType);
            linkCapabilities = sender.getLinkCapabilities(apnType);
        }
        ServiceState ss = sender.getServiceState();
        if (ss != null) roaming = ss.getRoaming();

        try {
            mRegistry.notifyDataConnection(
                    convertDataState(state),
                    sender.isDataConnectivityPossible(apnType), reason,
                    sender.getActiveApnHost(apnType),
                    apnType,
                    linkProperties,
                    linkCapabilities,
                    ((telephony!=null) ? telephony.getNetworkType() :
                    TelephonyManager.NETWORK_TYPE_UNKNOWN),
                    roaming);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyDataConnectionFailed(Phone sender, String reason, String apnType) {
        try {
            mRegistry.notifyDataConnectionFailed(reason, apnType);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyCellLocation(Phone sender) {
        Bundle data = new Bundle();
        sender.getCellLocation().fillInNotifierBundle(data);
        try {
            mRegistry.notifyCellLocation(data);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyOtaspChanged(Phone sender, int otaspMode) {
        try {
            mRegistry.notifyOtaspChanged(otaspMode);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    private void log(String s) {
        Log.d(LOG_TAG, "[PhoneNotifier] " + s);
    }

    /**
     * Convert the {@link State} enum into the TelephonyManager.CALL_STATE_* constants
     * for the public API.
     */
    public static int convertCallState(Phone.State state) {
        switch (state) {
            case RINGING:
                return TelephonyManager.CALL_STATE_RINGING;
            case OFFHOOK:
                return TelephonyManager.CALL_STATE_OFFHOOK;
            default:
                return TelephonyManager.CALL_STATE_IDLE;
        }
    }

    /**
     * Convert the TelephonyManager.CALL_STATE_* constants into the {@link State} enum
     * for the public API.
     */
    public static Phone.State convertCallState(int state) {
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                return Phone.State.RINGING;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                return Phone.State.OFFHOOK;
            default:
                return Phone.State.IDLE;
        }
    }

    /**
     * Convert the {@link Call.State} enum into the TelephonyManager.CALL_DETAILED_STATE_* constants
     * for the public API.
     */
    public static int convertPreciseCallState(Call.State state) {
        switch (state) {
            case ACTIVE:
                return TelephonyManager.CALL_PRECISE_STATE_ACTIVE;
            case HOLDING:
                return TelephonyManager.CALL_PRECISE_STATE_HOLDING;
            case DIALING:
                return TelephonyManager.CALL_PRECISE_STATE_DIALING;
            case ALERTING:
                return TelephonyManager.CALL_PRECISE_STATE_ALERTING;
            case INCOMING:
                return TelephonyManager.CALL_PRECISE_STATE_INCOMING;
            case WAITING:
                return TelephonyManager.CALL_PRECISE_STATE_WAITING;
            case DISCONNECTED:
                return TelephonyManager.CALL_PRECISE_STATE_DISCONNECTED;
            case DISCONNECTING:
                return TelephonyManager.CALL_PRECISE_STATE_DISCONNECTING;
            default:
                return TelephonyManager.CALL_PRECISE_STATE_IDLE;
        }
    }

    /**
     * Convert the {@link DataState} enum into the TelephonyManager.DATA_* constants
     * for the public API.
     */
    public static int convertDataState(Phone.DataState state) {
        switch (state) {
            case CONNECTING:
                return TelephonyManager.DATA_CONNECTING;
            case CONNECTED:
                return TelephonyManager.DATA_CONNECTED;
            case SUSPENDED:
                return TelephonyManager.DATA_SUSPENDED;
            default:
                return TelephonyManager.DATA_DISCONNECTED;
        }
    }

    /**
     * Convert the TelephonyManager.DATA_* constants into {@link DataState} enum
     * for the public API.
     */
    public static Phone.DataState convertDataState(int state) {
        switch (state) {
            case TelephonyManager.DATA_CONNECTING:
                return Phone.DataState.CONNECTING;
            case TelephonyManager.DATA_CONNECTED:
                return Phone.DataState.CONNECTED;
            case TelephonyManager.DATA_SUSPENDED:
                return Phone.DataState.SUSPENDED;
            default:
                return Phone.DataState.DISCONNECTED;
        }
    }

    /**
     * Convert the {@link DataState} enum into the TelephonyManager.DATA_* constants
     * for the public API.
     */
    public static int convertDataActivityState(Phone.DataActivityState state) {
        switch (state) {
            case DATAIN:
                return TelephonyManager.DATA_ACTIVITY_IN;
            case DATAOUT:
                return TelephonyManager.DATA_ACTIVITY_OUT;
            case DATAINANDOUT:
                return TelephonyManager.DATA_ACTIVITY_INOUT;
            case DORMANT:
                return TelephonyManager.DATA_ACTIVITY_DORMANT;
            default:
                return TelephonyManager.DATA_ACTIVITY_NONE;
        }
    }

    /**
     * Convert the TelephonyManager.DATA_* constants into the {@link DataState} enum
     * for the public API.
     */
    public static Phone.DataActivityState convertDataActivityState(int state) {
        switch (state) {
            case TelephonyManager.DATA_ACTIVITY_IN:
                return Phone.DataActivityState.DATAIN;
            case TelephonyManager.DATA_ACTIVITY_OUT:
                return Phone.DataActivityState.DATAOUT;
            case TelephonyManager.DATA_ACTIVITY_INOUT:
                return Phone.DataActivityState.DATAINANDOUT;
            case TelephonyManager.DATA_ACTIVITY_DORMANT:
                return Phone.DataActivityState.DORMANT;
            default:
                return Phone.DataActivityState.NONE;
        }
    }

    /**
     * Get connection addresses from a call. If no address is
     * specified, returns a zero element array. If an address is
     * null, an {@link UNKNOWN_ADDRESS} constant inserted into
     * the array for the address.
     */
    private String[] getAddressesFromCall (Call call) {
        List<Connection> connections = call.getConnections();
        if (connections == null || connections.size() == 0) {
            return new String[0];
        } else {
            String[] addresses = new String[connections.size()];
            for (int i = 0; i != connections.size(); i++) {
                String address = connections.get(i).getAddress();
                if(address == null) {
                    addresses[i] = UNKNOWN_ADDRESS;
                } else {
                    addresses[i] = address;
                }
            }
            return addresses;
        }
    }
}
