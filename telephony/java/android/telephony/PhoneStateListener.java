/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.CellLocation;
import android.telephony.CellInfo;
import android.telephony.Rlog;

import com.android.internal.telephony.IPhoneStateListener;
import com.android.internal.telephony.PhoneConstants;

import java.util.List;

/**
 * A listener class for monitoring changes in specific telephony states
 * on the device, including service state, signal strength, message
 * waiting indicator (voicemail), and others.
 * <p>
 * Override the methods for the state that you wish to receive updates for, and
 * pass your PhoneStateListener object, along with bitwise-or of the LISTEN_
 * flags to {@link TelephonyManager#listen TelephonyManager.listen()}.
 * <p>
 * Note that access to some telephony information is
 * permission-protected. Your application won't receive updates for protected
 * information unless it has the appropriate permissions declared in
 * its manifest file. Where permissions apply, they are noted in the
 * appropriate LISTEN_ flags.
 */
public class PhoneStateListener {

    /**
     * Stop listening for updates.
     */
    public static final int LISTEN_NONE = 0;

    /**
     *  Listen for changes to the network service state (cellular).
     *
     *  @see #onServiceStateChanged
     *  @see ServiceState
     */
    public static final int LISTEN_SERVICE_STATE                            = 0x00000001;

    /**
     * Listen for changes to the network signal strength (cellular).
     * {@more}
     * Requires Permission: {@link android.Manifest.permission#READ_PHONE_STATE
     * READ_PHONE_STATE}
     * <p>
     *
     * @see #onSignalStrengthChanged
     *
     * @deprecated by {@link #LISTEN_SIGNAL_STRENGTHS}
     */
    @Deprecated
    public static final int LISTEN_SIGNAL_STRENGTH                          = 0x00000002;

    /**
     * Listen for changes to the message-waiting indicator.
     * {@more}
     * Requires Permission: {@link android.Manifest.permission#READ_PHONE_STATE
     * READ_PHONE_STATE}
     * <p>
     * Example: The status bar uses this to determine when to display the
     * voicemail icon.
     *
     * @see #onMessageWaitingIndicatorChanged
     */
    public static final int LISTEN_MESSAGE_WAITING_INDICATOR                = 0x00000004;

    /**
     * Listen for changes to the call-forwarding indicator.
     * {@more}
     * Requires Permission: {@link android.Manifest.permission#READ_PHONE_STATE
     * READ_PHONE_STATE}
     * @see #onCallForwardingIndicatorChanged
     */
    public static final int LISTEN_CALL_FORWARDING_INDICATOR                = 0x00000008;

    /**
     * Listen for changes to the device's cell location. Note that
     * this will result in frequent callbacks to the listener.
     * {@more}
     * Requires Permission: {@link android.Manifest.permission#ACCESS_COARSE_LOCATION
     * ACCESS_COARSE_LOCATION}
     * <p>
     * If you need regular location updates but want more control over
     * the update interval or location precision, you can set up a listener
     * through the {@link android.location.LocationManager location manager}
     * instead.
     *
     * @see #onCellLocationChanged
     */
    public static final int LISTEN_CELL_LOCATION                            = 0x00000010;

    /**
     * Listen for changes to the device call state.
     * {@more}
     * Requires Permission: {@link android.Manifest.permission#READ_PHONE_STATE
     * READ_PHONE_STATE}
     * @see #onCallStateChanged
     */
    public static final int LISTEN_CALL_STATE                               = 0x00000020;

    /**
     * Listen for changes to the data connection state (cellular).
     *
     * @see #onDataConnectionStateChanged
     */
    public static final int LISTEN_DATA_CONNECTION_STATE                    = 0x00000040;

    /**
     * Listen for changes to the direction of data traffic on the data
     * connection (cellular).
     * {@more}
     * Requires Permission: {@link android.Manifest.permission#READ_PHONE_STATE
     * READ_PHONE_STATE}
     * Example: The status bar uses this to display the appropriate
     * data-traffic icon.
     *
     * @see #onDataActivity
     */
    public static final int LISTEN_DATA_ACTIVITY                            = 0x00000080;

    /**
     * Listen for changes to the network signal strengths (cellular).
     * <p>
     * Example: The status bar uses this to control the signal-strength
     * icon.
     *
     * @see #onSignalStrengthsChanged
     */
    public static final int LISTEN_SIGNAL_STRENGTHS                         = 0x00000100;

    /**
     * Listen for changes to OTASP mode.
     *
     * @see #onOtaspChanged
     * @hide
     */
    public static final int LISTEN_OTASP_CHANGED                            = 0x00000200;

    /**
     * Listen for changes to observed cell info.
     *
     * @see #onCellInfoChanged
     */
    public static final int LISTEN_CELL_INFO = 0x00000400;

    /**
     * Listen for precise changes to the device call states.
     * {@more}
     * Requires Permission: {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE
     * READ_PRECISE_PHONE_STATE}
     */
    public static final int LISTEN_PRECISE_CALL_STATE                       = 0x00000800;

    /**
     * Listen for device call (cellular) disconnect cause.
     * {@more}
     * Requires Permission: {@link android.Manifest.permission#READ_PHONE_STATE
     * READ_PHONE_STATE}
     */
    public static final int LISTEN_DISCONNECT_CAUSE                         = 0x00001000;

    /**
     * Listen for device call (cellular) precise disconnect cause.
     * {@more}
     * Requires Permission: {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE
     * READ_PRECISE_PHONE_STATE}
     */
    public static final int LISTEN_PRECISE_DISCONNECT_CAUSE                 = 0x00002000;

    /**
     * Listen for precise changes on the data connection (cellular).
     * {@more}
     * Requires Permission: {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE
     * READ_PRECISE_PHONE_STATE}
     * 
     * @see #onPreciseDataConnectionStateChanged
     */
    public static final int LISTEN_PRECISE_DATA_CONNECTION_STATE            = 0x00004000;

    /**
     * Listen for connection errors on the data connection (cellular).
     * {@more}
     * Requires Permission: {@link android.Manifest.permission#READ_PHONE_STATE
     * READ_PHONE_STATE}
     * 
     * @see #onDataConnectionFailed
     */
    public static final int LISTEN_DATA_CONNECTION_FAILED                   = 0x00008000;

    /**
     * Listen for precise connection errors on the data connection (cellular).
     * {@more}
     * Requires Permission: {@link android.Manifest.permission#READ_PRECISE_PHONE_STATE
     * READ_PRECISE_PHONE_STATE}
     * 
     * @see #onPreciseDataConnectionFailed
     */
    public static final int LISTEN_PRECISE_DATA_CONNECTION_FAILED           = 0x00010000;

    public PhoneStateListener() {
    }

    /**
     * Callback invoked when device service state changes.
     *
     * @see ServiceState#STATE_EMERGENCY_ONLY
     * @see ServiceState#STATE_IN_SERVICE
     * @see ServiceState#STATE_OUT_OF_SERVICE
     * @see ServiceState#STATE_POWER_OFF
     */
    public void onServiceStateChanged(ServiceState serviceState) {
        // default implementation empty
    }

    /**
     * Callback invoked when network signal strength changes.
     *
     * @see ServiceState#STATE_EMERGENCY_ONLY
     * @see ServiceState#STATE_IN_SERVICE
     * @see ServiceState#STATE_OUT_OF_SERVICE
     * @see ServiceState#STATE_POWER_OFF
     * @deprecated Use {@link #onSignalStrengthsChanged(SignalStrength)}
     */
    @Deprecated
    public void onSignalStrengthChanged(int asu) {
        // default implementation empty
    }

    /**
     * Callback invoked when the message-waiting indicator changes.
     */
    public void onMessageWaitingIndicatorChanged(boolean mwi) {
        // default implementation empty
    }

    /**
     * Callback invoked when the call-forwarding indicator changes.
     */
    public void onCallForwardingIndicatorChanged(boolean cfi) {
        // default implementation empty
    }

    /**
     * Callback invoked when device cell location changes.
     */
    public void onCellLocationChanged(CellLocation location) {
        // default implementation empty
    }

    /**
     * Callback invoked when device call state changes.
     *
     * @see TelephonyManager#CALL_STATE_IDLE
     * @see TelephonyManager#CALL_STATE_RINGING
     * @see TelephonyManager#CALL_STATE_OFFHOOK
     */
    public void onCallStateChanged(int state, String incomingNumber) {
        // default implementation empty
    }

    /**
     * Callback invoked when connection state changes.
     *
     * @see TelephonyManager#DATA_DISCONNECTED
     * @see TelephonyManager#DATA_CONNECTING
     * @see TelephonyManager#DATA_CONNECTED
     * @see TelephonyManager#DATA_SUSPENDED
     */
    public void onDataConnectionStateChanged(int state) {
        // default implementation empty
    }

    /**
     * same as above, but with the network type.  Both called.
     */
    public void onDataConnectionStateChanged(int state, int networkType) {
    }

    /**
     * Callback invoked when data activity state changes.
     *
     * @see TelephonyManager#DATA_ACTIVITY_NONE
     * @see TelephonyManager#DATA_ACTIVITY_IN
     * @see TelephonyManager#DATA_ACTIVITY_OUT
     * @see TelephonyManager#DATA_ACTIVITY_INOUT
     * @see TelephonyManager#DATA_ACTIVITY_DORMANT
     */
    public void onDataActivity(int direction) {
        // default implementation empty
    }

    /**
     * Callback invoked when network signal strengths changes.
     *
     * @see ServiceState#STATE_EMERGENCY_ONLY
     * @see ServiceState#STATE_IN_SERVICE
     * @see ServiceState#STATE_OUT_OF_SERVICE
     * @see ServiceState#STATE_POWER_OFF
     */
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        // default implementation empty
    }


    /**
     * The Over The Air Service Provisioning (OTASP) has changed. Requires
     * the READ_PHONE_STATE permission.
     * @param otaspMode is integer <code>OTASP_UNKNOWN=1<code>
     *   means the value is currently unknown and the system should wait until
     *   <code>OTASP_NEEDED=2<code> or <code>OTASP_NOT_NEEDED=3<code> is received before
     *   making the decisision to perform OTASP or not.
     *
     * @hide
     */
    public void onOtaspChanged(int otaspMode) {
        // default implementation empty
    }

    /**
     * Callback invoked when a observed cell info has changed,
     * or new cells have been added or removed.
     * @param cellInfo is the list of currently visible cells.
     */
    public void onCellInfoChanged(List<CellInfo> cellInfo) {
    }

    /**
     * Callback invoked when precise device call state changes.
     *
     * @see TelephonyManager#PRECISE_CALL_STATE_IDLE
     * @see TelephonyManager#PRECISE_CALL_STATE_ACTIVE
     * @see TelephonyManager#PRECISE_CALL_STATE_HOLDING
     * @see TelephonyManager#PRECISE_CALL_STATE_DIALING
     * @see TelephonyManager#PRECISE_CALL_STATE_ALERTING
     * @see TelephonyManager#PRECISE_CALL_STATE_INCOMING
     * @see TelephonyManager#PRECISE_CALL_STATE_WAITING
     * @see TelephonyManager#PRECISE_CALL_STATE_DISCONNECTED
     * @see TelephonyManager#PRECISE_CALL_STATE_DISCONNECTING
     */
    public void onPreciseCallStateChanged(int ringingCallState, int foregroundCallState, int backgroundCallState) {
        // default implementation empty
    }

    /**
     * Callback invoked when device call (cellular) is disconnected.
     *
     * @see TelephonyManager#DISCONNECT_CAUSE_NOT_DISCONNECTED
     * @see TelephonyManager#DISCONNECT_CAUSE_INCOMING_MISSED
     * @see TelephonyManager#DISCONNECT_CAUSE_NORMAL
     * @see TelephonyManager#DISCONNECT_CAUSE_LOCAL
     * @see TelephonyManager#DISCONNECT_CAUSE_BUSY
     * @see TelephonyManager#DISCONNECT_CAUSE_CONGESTION
     * @see TelephonyManager#DISCONNECT_CAUSE_MMI
     * @see TelephonyManager#DISCONNECT_CAUSE_INVALID_NUMBER
     * @see TelephonyManager#DISCONNECT_CAUSE_NUMBER_UNREACHABLE
     * @see TelephonyManager#DISCONNECT_CAUSE_SERVER_UNREACHABLE
     * @see TelephonyManager#DISCONNECT_CAUSE_INVALID_CREDENTIALS
     * @see TelephonyManager#DISCONNECT_CAUSE_OUT_OF_NETWORK
     * @see TelephonyManager#DISCONNECT_CAUSE_SERVER_ERROR
     * @see TelephonyManager#DISCONNECT_CAUSE_TIMED_OUT
     * @see TelephonyManager#DISCONNECT_CAUSE_LOST_SIGNAL
     * @see TelephonyManager#DISCONNECT_CAUSE_LIMIT_EXCEEDED
     * @see TelephonyManager#DISCONNECT_CAUSE_INCOMING_REJECTED
     * @see TelephonyManager#DISCONNECT_CAUSE_POWER_OFF
     * @see TelephonyManager#DISCONNECT_CAUSE_OUT_OF_SERVICE
     * @see TelephonyManager#DISCONNECT_CAUSE_ICC_ERROR
     * @see TelephonyManager#DISCONNECT_CAUSE_CALL_BARRED
     * @see TelephonyManager#DISCONNECT_CAUSE_FDN_BLOCKED
     * @see TelephonyManager#DISCONNECT_CAUSE_CS_RESTRICTED
     * @see TelephonyManager#DISCONNECT_CAUSE_CS_RESTRICTED_NORMAL
     * @see TelephonyManager#DISCONNECT_CAUSE_CS_RESTRICTED_EMERGENCY
     * @see TelephonyManager#DISCONNECT_CAUSE_UNOBTAINABLE_NUMBER
     * @see TelephonyManager#DISCONNECT_CAUSE_CDMA_LOCKED_UNTIL_POWER_CYCLE
     * @see TelephonyManager#DISCONNECT_CAUSE_CDMA_DROP
     * @see TelephonyManager#DISCONNECT_CAUSE_CDMA_INTERCEPT
     * @see TelephonyManager#DISCONNECT_CAUSE_CDMA_REORDER
     * @see TelephonyManager#DISCONNECT_CAUSE_CDMA_SO_REJECT
     * @see TelephonyManager#DISCONNECT_CAUSE_CDMA_RETRY_ORDER
     * @see TelephonyManager#DISCONNECT_CAUSE_CDMA_ACCESS_FAILURE
     * @see TelephonyManager#DISCONNECT_CAUSE_CDMA_PREEMPTED
     * @see TelephonyManager#DISCONNECT_CAUSE_CDMA_NOT_EMERGENCY
     * @see TelephonyManager#DISCONNECT_CAUSE_CDMA_ACCESS_BLOCKED
     * @see TelephonyManager#DISCONNECT_CAUSE_ERROR_UNSPECIFIED
     */
    public void onDisconnectCause(int disconnectCause) {
        // default implementation empty
    }

    /**
     * Callback invoked when device call (cellular) is disconnected
     * with a detailed disconnect cause.
     *
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_NO_DISCONNECT_CAUSE_AVAILABLE
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_UNOBTAINABLE_NUMBER
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_NORMAL
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_BUSY
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_NUMBER_CHANGED
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_STATUS_ENQUIRY
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_NORMAL_UNSPECIFIED
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_NO_CIRCUIT_AVAIL
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_TEMPORARY_FAILURE
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_SWITCHING_CONGESTION
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_CHANNEL_NOT_AVAIL
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_QOS_NOT_AVAIL
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_BEARER_NOT_AVAIL
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_ACM_LIMIT_EXCEEDED
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_CALL_BARRED
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_FDN_BLOCKED
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_IMSI_UNKNOWN_IN_VLR
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_IMEI_NOT_ACCEPTED
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_CDMA_LOCKED_UNTIL_POWER_CYCLE
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_CDMA_DROP
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_CDMA_INTERCEPT
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_CDMA_REORDER
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_CDMA_SO_REJECT
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_CDMA_RETRY_ORDER
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_CDMA_ACCESS_FAILURE
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_CDMA_PREEMPTED
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_CDMA_NOT_EMERGENCY
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_CDMA_ACCESS_BLOCKED
     * @see TelephonyManager#PRECISE_DISCONNECT_CAUSE_ERROR_UNSPECIFIED
     */
    public void onPreciseDisconnectCause(int preciseCause) {
        // default implementation empty
    }

    /**
     * Callback invoked when precise data connection state changes.
     * Provides the connection state, network type, affected apn type and name, change reason,
     * and string representaions of the link properties and capabilities.
     *
     * @see TelephonyManager#DATA_DISCONNECTED
     * @see TelephonyManager#DATA_CONNECTING
     * @see TelephonyManager#DATA_CONNECTED
     * @see TelephonyManager#DATA_SUSPENDED
     */
    public void onPreciseDataConnectionStateChanged(int state, int networkType, String apnType, String apn, String reason, String iface, String link) {
        // default implementation empty
    }

    /**
     * Callback invoked when data connection fails.
     */
    public void onDataConnectionFailed(String reason, String apnType) {
        // default implementation empty
    }

    /**
     * Callback invoked when data connection fails with precise information.
     */
    public void onPreciseDataConnectionFailed(String reason, String apnType, String apn, String failCause) {
        // default implementation empty
    }

    /**
     * The callback methods need to be called on the handler thread where
     * this object was created.  If the binder did that for us it'd be nice.
     */
    IPhoneStateListener callback = new IPhoneStateListener.Stub() {
        public void onServiceStateChanged(ServiceState serviceState) {
            Message.obtain(mHandler, LISTEN_SERVICE_STATE, 0, 0, serviceState).sendToTarget();
        }

        public void onSignalStrengthChanged(int asu) {
            Message.obtain(mHandler, LISTEN_SIGNAL_STRENGTH, asu, 0, null).sendToTarget();
        }

        public void onMessageWaitingIndicatorChanged(boolean mwi) {
            Message.obtain(mHandler, LISTEN_MESSAGE_WAITING_INDICATOR, mwi ? 1 : 0, 0, null)
                    .sendToTarget();
        }

        public void onCallForwardingIndicatorChanged(boolean cfi) {
            Message.obtain(mHandler, LISTEN_CALL_FORWARDING_INDICATOR, cfi ? 1 : 0, 0, null)
                    .sendToTarget();
        }

        public void onCellLocationChanged(Bundle bundle) {
            CellLocation location = CellLocation.newFromBundle(bundle);
            Message.obtain(mHandler, LISTEN_CELL_LOCATION, 0, 0, location).sendToTarget();
        }

        public void onCallStateChanged(int state, String incomingNumber) {
            Message.obtain(mHandler, LISTEN_CALL_STATE, state, 0, incomingNumber).sendToTarget();
        }

        public void onDataConnectionStateChanged(int state, int networkType) {
            Message.obtain(mHandler, LISTEN_DATA_CONNECTION_STATE, state, networkType).
                    sendToTarget();
        }

        public void onDataActivity(int direction) {
            Message.obtain(mHandler, LISTEN_DATA_ACTIVITY, direction, 0, null).sendToTarget();
        }

        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            Message.obtain(mHandler, LISTEN_SIGNAL_STRENGTHS, 0, 0, signalStrength).sendToTarget();
        }

        public void onOtaspChanged(int otaspMode) {
            Message.obtain(mHandler, LISTEN_OTASP_CHANGED, otaspMode, 0).sendToTarget();
        }

        public void onCellInfoChanged(List<CellInfo> cellInfo) {
            Message.obtain(mHandler, LISTEN_CELL_INFO, 0, 0, cellInfo).sendToTarget();
        }

        public void onPreciseCallStateChanged(int ringingCallState, int foregroundCallState, int backgroundCallState) {
            Bundle data = new Bundle();
            data.putIntArray(PhoneConstants.STATE_KEY, new int[] { ringingCallState, foregroundCallState, backgroundCallState });
            Message m = Message.obtain(mHandler, LISTEN_PRECISE_CALL_STATE);
            m.setData(data);
            m.sendToTarget();
        }

        public void onDisconnectCause(int disconnectCause) {
            Message.obtain(mHandler, LISTEN_DISCONNECT_CAUSE, disconnectCause, 0).sendToTarget();
        }

        public void onPreciseDisconnectCause(int preciseCause) {
            Message.obtain(mHandler, LISTEN_PRECISE_DISCONNECT_CAUSE, preciseCause, 0).sendToTarget();
        }

        public void onPreciseDataConnectionStateChanged(int state, int networkType, String apnType,
                String apn, String reason, String iface, String link) {
            Bundle data = new Bundle();
            data.putInt(PhoneConstants.STATE_KEY, state);
            data.putInt(PhoneConstants.DATA_NETWORK_TYPE_KEY, networkType);
            data.putString(PhoneConstants.DATA_APN_TYPE_KEY, apnType);
            data.putString(PhoneConstants.DATA_APN_KEY, apn);
            data.putString(PhoneConstants.STATE_CHANGE_REASON_KEY, reason);
            data.putString(PhoneConstants.DATA_IFACE_NAME_KEY, iface);
            data.putString(PhoneConstants.DATA_LINK_CAPABILITIES_KEY, link);
            Message m = Message.obtain(mHandler, LISTEN_PRECISE_DATA_CONNECTION_STATE);
            m.setData(data);
            m.sendToTarget();
        }

        public void onDataConnectionFailed(String reason, String apnType) {
            Bundle data = new Bundle();
            data.putString(PhoneConstants.FAILURE_REASON_KEY, reason);
            data.putString(PhoneConstants.DATA_APN_TYPE_KEY, apnType);
            Message m = Message.obtain(mHandler, LISTEN_DATA_CONNECTION_FAILED);
            m.setData(data);
            m.sendToTarget();
        }

        public void onPreciseDataConnectionFailed(String reason, String apnType, String apn, String failCause) {
            Bundle data = new Bundle();
            data.putString(PhoneConstants.FAILURE_REASON_KEY, reason);
            data.putString(PhoneConstants.DATA_APN_TYPE_KEY, apnType);
            data.putString(PhoneConstants.DATA_APN_KEY, apn);
            data.putString(PhoneConstants.DATA_FAILURE_CAUSE_KEY, failCause);
            Message m = Message.obtain(mHandler, LISTEN_PRECISE_DATA_CONNECTION_FAILED);
            m.setData(data);
            m.sendToTarget();
        }
    };

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            //Rlog.d("TelephonyRegistry", "what=0x" + Integer.toHexString(msg.what) + " msg=" + msg);
            switch (msg.what) {
                case LISTEN_SERVICE_STATE:
                    PhoneStateListener.this.onServiceStateChanged((ServiceState)msg.obj);
                    break;
                case LISTEN_SIGNAL_STRENGTH:
                    PhoneStateListener.this.onSignalStrengthChanged(msg.arg1);
                    break;
                case LISTEN_MESSAGE_WAITING_INDICATOR:
                    PhoneStateListener.this.onMessageWaitingIndicatorChanged(msg.arg1 != 0);
                    break;
                case LISTEN_CALL_FORWARDING_INDICATOR:
                    PhoneStateListener.this.onCallForwardingIndicatorChanged(msg.arg1 != 0);
                    break;
                case LISTEN_CELL_LOCATION:
                    PhoneStateListener.this.onCellLocationChanged((CellLocation)msg.obj);
                    break;
                case LISTEN_CALL_STATE:
                    PhoneStateListener.this.onCallStateChanged(msg.arg1, (String)msg.obj);
                    break;
                case LISTEN_DATA_CONNECTION_STATE:
                    PhoneStateListener.this.onDataConnectionStateChanged(msg.arg1, msg.arg2);
                    PhoneStateListener.this.onDataConnectionStateChanged(msg.arg1);
                    break;
                case LISTEN_DATA_ACTIVITY:
                    PhoneStateListener.this.onDataActivity(msg.arg1);
                    break;
                case LISTEN_SIGNAL_STRENGTHS:
                    PhoneStateListener.this.onSignalStrengthsChanged((SignalStrength)msg.obj);
                    break;
                case LISTEN_OTASP_CHANGED:
                    PhoneStateListener.this.onOtaspChanged(msg.arg1);
                    break;
                case LISTEN_CELL_INFO:
                    PhoneStateListener.this.onCellInfoChanged((List<CellInfo>)msg.obj);
                    break;
                case LISTEN_PRECISE_CALL_STATE:
                    int[] callStates = msg.getData().getIntArray(PhoneConstants.STATE_KEY);
                    PhoneStateListener.this.onPreciseCallStateChanged(callStates[0], callStates[1], callStates[2]);
                    break;
                case LISTEN_DISCONNECT_CAUSE:
                    PhoneStateListener.this.onDisconnectCause(msg.arg1);
                    break;
                case LISTEN_PRECISE_DISCONNECT_CAUSE:
                    PhoneStateListener.this.onPreciseDisconnectCause(msg.arg1);
                    break;
                case LISTEN_PRECISE_DATA_CONNECTION_STATE:
                    Bundle data = msg.getData();
                    PhoneStateListener.this.onPreciseDataConnectionStateChanged(
                            data.getInt(PhoneConstants.STATE_KEY),
                            data.getInt(PhoneConstants.DATA_NETWORK_TYPE_KEY),
                            data.getString(PhoneConstants.DATA_APN_TYPE_KEY),
                            data.getString(PhoneConstants.DATA_APN_KEY),
                            data.getString(PhoneConstants.STATE_CHANGE_REASON_KEY),
                            data.getString(PhoneConstants.DATA_IFACE_NAME_KEY),
                            data.getString(PhoneConstants.DATA_LINK_CAPABILITIES_KEY));
                    break;
                case LISTEN_DATA_CONNECTION_FAILED:
                    PhoneStateListener.this.onDataConnectionFailed(
                            msg.getData().getString(PhoneConstants.FAILURE_REASON_KEY),
                            msg.getData().getString(PhoneConstants.DATA_APN_TYPE_KEY));
                    break;
                case LISTEN_PRECISE_DATA_CONNECTION_FAILED:
                    PhoneStateListener.this.onPreciseDataConnectionFailed(
                            msg.getData().getString(PhoneConstants.FAILURE_REASON_KEY),
                            msg.getData().getString(PhoneConstants.DATA_APN_TYPE_KEY),
                            msg.getData().getString(PhoneConstants.DATA_APN_KEY),
                            msg.getData().getString(PhoneConstants.DATA_FAILURE_CAUSE_KEY));
            }
        }
    };
}
