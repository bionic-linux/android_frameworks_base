/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.bluetooth;

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Public API to control Hands Free Profile (HFP role only).
 * <p>
 * This class defines methods that shall be used by application to manage profile
 * connection, calls states and calls actions.
 * <p>
 *
 * @hide
 */
@SystemApi
public final class BluetoothHeadsetClient implements BluetoothProfile {
    private static final String TAG = "BluetoothHeadsetClient";
    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    /**
     * Intent sent whenever connection to remote changes.
     *
     * <p>It includes two extras:
     * <code>BluetoothProfile.EXTRA_PREVIOUS_STATE</code>
     * and <code>BluetoothProfile.EXTRA_STATE</code>, which
     * are mandatory.
     * <p>There are also non mandatory feature extras:
     * {@link #EXTRA_AG_FEATURE_3WAY_CALLING},
     * {@link #EXTRA_AG_FEATURE_VOICE_RECOGNITION},
     * {@link #EXTRA_AG_FEATURE_ATTACH_NUMBER_TO_VT},
     * {@link #EXTRA_AG_FEATURE_REJECT_CALL},
     * {@link #EXTRA_AG_FEATURE_ECC},
     * {@link #EXTRA_AG_FEATURE_RESPONSE_AND_HOLD},
     * {@link #EXTRA_AG_FEATURE_ACCEPT_HELD_OR_WAITING_CALL},
     * {@link #EXTRA_AG_FEATURE_RELEASE_HELD_OR_WAITING_CALL},
     * {@link #EXTRA_AG_FEATURE_RELEASE_AND_ACCEPT},
     * {@link #EXTRA_AG_FEATURE_MERGE},
     * {@link #EXTRA_AG_FEATURE_MERGE_AND_DETACH},
     * sent as boolean values only when <code>EXTRA_STATE</code>
     * is set to <code>STATE_CONNECTED</code>.</p>
     *
     * <p>Note that features supported by AG are being sent as
     * booleans with value <code>true</code>,
     * and not supported ones are <strong>not</strong> being sent at all.</p>
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED";

    /**
     * Intent sent whenever audio state changes.
     *
     * <p>It includes two mandatory extras:
     * {@link BluetoothProfile#EXTRA_STATE},
     * {@link BluetoothProfile#EXTRA_PREVIOUS_STATE},
     * with possible values:
     * {@link #STATE_AUDIO_CONNECTING},
     * {@link #STATE_AUDIO_CONNECTED},
     * {@link #STATE_AUDIO_DISCONNECTED}</p>
     * <p>When <code>EXTRA_STATE</code> is set
     * to </code>STATE_AUDIO_CONNECTED</code>,
     * it also includes {@link #EXTRA_AUDIO_WBS}
     * indicating wide band speech support.</p>
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String ACTION_AUDIO_STATE_CHANGED =
            "android.bluetooth.headsetclient.profile.action.AUDIO_STATE_CHANGED";

    /**
     * Intent sending updates of the Audio Gateway state.
     * Each extra is being sent only when value it
     * represents has been changed recently on AG.
     * <p>It can contain one or more of the following extras:
     * {@link #EXTRA_NETWORK_STATUS},
     * {@link #EXTRA_NETWORK_SIGNAL_STRENGTH},
     * {@link #EXTRA_NETWORK_ROAMING},
     * {@link #EXTRA_BATTERY_LEVEL},
     * {@link #EXTRA_OPERATOR_NAME},
     * {@link #EXTRA_VOICE_RECOGNITION},
     * {@link #EXTRA_IN_BAND_RING}</p>
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String ACTION_AG_EVENT =
            "android.bluetooth.headsetclient.profile.action.AG_EVENT";

    /**
     * Intent sent whenever state of a call changes.
     *
     * <p>It includes:
     * {@link #EXTRA_CALL},
     * with value of {@link BluetoothHeadsetClientCall} instance,
     * representing actual call state.</p>
     *
     * @hide
     */
    public static final String ACTION_CALL_CHANGED =
            "android.bluetooth.headsetclient.profile.action.AG_CALL_CHANGED";

    /**
     * Intent that notifies about the result of the last issued action.
     * Please note that not every action results in explicit action result code being sent.
     * Instead other notifications about new Audio Gateway state might be sent,
     * like <code>ACTION_AG_EVENT</code> with <code>EXTRA_VOICE_RECOGNITION</code> value
     * when for example user started voice recognition from HF unit.
     *
     * @hide
     */
    public static final String ACTION_RESULT =
            "android.bluetooth.headsetclient.profile.action.RESULT";

    /**
     * Intent that notifies about vendor specific event arrival. Events not defined in
     * HFP spec will be matched with supported vendor event list and this intent will
     * be broadcasted upon a match. Supported vendor events are of format of
     * of "+eventCode" or "+eventCode=xxxx" or "+eventCode:=xxxx".
     * Vendor event can be a response to an vendor specific command or unsolicited.
     *
     * @hide
     */
    public static final String ACTION_VENDOR_SPECIFIC_HEADSETCLIENT_EVENT =
            "android.bluetooth.headsetclient.profile.action.VENDOR_SPECIFIC_EVENT";

    /**
     * Intent that notifies about the number attached to the last voice tag
     * recorded on AG.
     *
     * <p>It contains:
     * {@link #EXTRA_NUMBER},
     * with a <code>String</code> value representing phone number.</p>
     *
     * @hide
     */
    public static final String ACTION_LAST_VTAG =
            "android.bluetooth.headsetclient.profile.action.LAST_VTAG";

    /** @hide */
    @SystemApi
    public static final int STATE_AUDIO_DISCONNECTED = 0;
    /** @hide */
    public static final int STATE_AUDIO_CONNECTING = 1;
    /** @hide */
    @SystemApi
    public static final int STATE_AUDIO_CONNECTED = 2;

    /**
     * Extra with information if connected audio is WBS.
     * <p>Possible values: <code>true</code>,
     * <code>false</code>.</p>
     *
     * @hide
     */
    public static final String EXTRA_AUDIO_WBS =
            "android.bluetooth.headsetclient.extra.AUDIO_WBS";

    /**
     * Extra for AG_EVENT indicates network status.
     * <p>Value: 0 - network unavailable,
     * 1 - network available </p>
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String EXTRA_NETWORK_STATUS =
            "android.bluetooth.headsetclient.extra.NETWORK_STATUS";
    /**
     * Extra for AG_EVENT intent indicates network signal strength.
     * <p>Value: <code>Integer</code> representing signal strength.</p>
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String EXTRA_NETWORK_SIGNAL_STRENGTH =
            "android.bluetooth.headsetclient.extra.NETWORK_SIGNAL_STRENGTH";
    /**
     * Extra for AG_EVENT intent indicates roaming state.
     * <p>Value: 0 - no roaming
     * 1 - active roaming</p>
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String EXTRA_NETWORK_ROAMING =
            "android.bluetooth.headsetclient.extra.NETWORK_ROAMING";
    /**
     * Extra for AG_EVENT intent indicates the battery level.
     * <p>Value: <code>Integer</code> representing signal strength.</p>
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String EXTRA_BATTERY_LEVEL =
            "android.bluetooth.headsetclient.extra.BATTERY_LEVEL";
    /**
     * Extra for AG_EVENT intent indicates operator name.
     * <p>Value: <code>String</code> representing operator name.</p>
     *
     * @hide
     */
    public static final String EXTRA_OPERATOR_NAME =
            "android.bluetooth.headsetclient.extra.OPERATOR_NAME";
    /**
     * Extra for AG_EVENT intent indicates voice recognition state.
     * <p>Value:
     * 0 - voice recognition stopped,
     * 1 - voice recognition started.</p>
     *
     * @hide
     */
    public static final String EXTRA_VOICE_RECOGNITION =
            "android.bluetooth.headsetclient.extra.VOICE_RECOGNITION";
    /**
     * Extra for AG_EVENT intent indicates in band ring state.
     * <p>Value:
     * 0 - in band ring tone not supported, or
     * 1 - in band ring tone supported.</p>
     *
     * @hide
     */
    public static final String EXTRA_IN_BAND_RING =
            "android.bluetooth.headsetclient.extra.IN_BAND_RING";

    /**
     * Extra for AG_EVENT intent indicates subscriber info.
     * <p>Value: <code>String</code> containing subscriber information.</p>
     *
     * @hide
     */
    public static final String EXTRA_SUBSCRIBER_INFO =
            "android.bluetooth.headsetclient.extra.SUBSCRIBER_INFO";

    /**
     * Extra for AG_CALL_CHANGED intent indicates the
     * {@link BluetoothHeadsetClientCall} object that has changed.
     *
     * @hide
     */
    public static final String EXTRA_CALL =
            "android.bluetooth.headsetclient.extra.CALL";

    /**
     * Extra for ACTION_LAST_VTAG intent.
     * <p>Value: <code>String</code> representing phone number
     * corresponding to last voice tag recorded on AG</p>
     *
     * @hide
     */
    public static final String EXTRA_NUMBER =
            "android.bluetooth.headsetclient.extra.NUMBER";

    /**
     * Extra for ACTION_RESULT intent that shows the result code of
     * last issued action.
     * <p>Possible results:
     * {@link #ACTION_RESULT_OK},
     * {@link #ACTION_RESULT_ERROR},
     * {@link #ACTION_RESULT_ERROR_NO_CARRIER},
     * {@link #ACTION_RESULT_ERROR_BUSY},
     * {@link #ACTION_RESULT_ERROR_NO_ANSWER},
     * {@link #ACTION_RESULT_ERROR_DELAYED},
     * {@link #ACTION_RESULT_ERROR_BLACKLISTED},
     * {@link #ACTION_RESULT_ERROR_CME}</p>
     *
     * @hide
     */
    public static final String EXTRA_RESULT_CODE =
            "android.bluetooth.headsetclient.extra.RESULT_CODE";

    /**
     * Extra for ACTION_RESULT intent that shows the extended result code of
     * last issued action.
     * <p>Value: <code>Integer</code> - error code.</p>
     *
     * @hide
     */
    public static final String EXTRA_CME_CODE =
            "android.bluetooth.headsetclient.extra.CME_CODE";

    /**
     * Extra for VENDOR_SPECIFIC_HEADSETCLIENT_EVENT intent that
     * indicates vendor ID.
     *
     * @hide
     */
    public static final String EXTRA_VENDOR_ID =
            "android.bluetooth.headsetclient.extra.VENDOR_ID";

     /**
     * Extra for VENDOR_SPECIFIC_HEADSETCLIENT_EVENT intent that
     * indicates vendor event code.
      *
      * @hide
     */
    public static final String EXTRA_VENDOR_EVENT_CODE =
            "android.bluetooth.headsetclient.extra.VENDOR_EVENT_CODE";

     /**
     * Extra for VENDOR_SPECIFIC_HEADSETCLIENT_EVENT intent that
     * contains full vendor event including event code and full arguments.
      *
      * @hide
     */
    public static final String EXTRA_VENDOR_EVENT_FULL_ARGS =
            "android.bluetooth.headsetclient.extra.VENDOR_EVENT_FULL_ARGS";


    /* Extras for AG_FEATURES, extras type is boolean */
    // TODO verify if all of those are actually useful
    /**
     * AG feature: three way calling.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_3WAY_CALLING =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_3WAY_CALLING";
    /**
     * AG feature: voice recognition.
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("ActionValue")
    public static final String EXTRA_AG_FEATURE_VOICE_RECOGNITION =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_VOICE_RECOGNITION";
    /**
     * AG feature: fetching phone number for voice tagging procedure.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_ATTACH_NUMBER_TO_VT =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_ATTACH_NUMBER_TO_VT";
    /**
     * AG feature: ability to reject incoming call.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_REJECT_CALL =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_REJECT_CALL";
    /**
     * AG feature: enhanced call handling (terminate specific call, private consultation).
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_ECC =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_ECC";
    /**
     * AG feature: response and hold.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_RESPONSE_AND_HOLD =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_RESPONSE_AND_HOLD";
    /**
     * AG call handling feature: accept held or waiting call in three way calling scenarios.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_ACCEPT_HELD_OR_WAITING_CALL =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_ACCEPT_HELD_OR_WAITING_CALL";
    /**
     * AG call handling feature: release held or waiting call in three way calling scenarios.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_RELEASE_HELD_OR_WAITING_CALL =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_RELEASE_HELD_OR_WAITING_CALL";
    /**
     * AG call handling feature: release active call and accept held or waiting call in three way
     * calling scenarios.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_RELEASE_AND_ACCEPT =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_RELEASE_AND_ACCEPT";
    /**
     * AG call handling feature: merge two calls, held and active - multi party conference mode.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_MERGE =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_MERGE";
    /**
     * AG call handling feature: merge calls and disconnect from multi party
     * conversation leaving peers connected to each other.
     * Note that this feature needs to be supported by mobile network operator
     * as it requires connection and billing transfer.
     *
     * @hide
     */
    public static final String EXTRA_AG_FEATURE_MERGE_AND_DETACH =
            "android.bluetooth.headsetclient.extra.EXTRA_AG_FEATURE_MERGE_AND_DETACH";

    /* Action result codes */
    /** @hide */
    public static final int ACTION_RESULT_OK = 0;
    /** @hide */
    public static final int ACTION_RESULT_ERROR = 1;
    /** @hide */
    public static final int ACTION_RESULT_ERROR_NO_CARRIER = 2;
    /** @hide */
    public static final int ACTION_RESULT_ERROR_BUSY = 3;
    /** @hide */
    public static final int ACTION_RESULT_ERROR_NO_ANSWER = 4;
    /** @hide */
    public static final int ACTION_RESULT_ERROR_DELAYED = 5;
    /** @hide */
    public static final int ACTION_RESULT_ERROR_BLACKLISTED = 6;
    /** @hide */
    public static final int ACTION_RESULT_ERROR_CME = 7;

    /* Detailed CME error codes */
    /** @hide */
    public static final int CME_PHONE_FAILURE = 0;
    /** @hide */
    public static final int CME_NO_CONNECTION_TO_PHONE = 1;
    /** @hide */
    public static final int CME_OPERATION_NOT_ALLOWED = 3;
    /** @hide */
    public static final int CME_OPERATION_NOT_SUPPORTED = 4;
    /** @hide */
    public static final int CME_PHSIM_PIN_REQUIRED = 5;
    /** @hide */
    public static final int CME_PHFSIM_PIN_REQUIRED = 6;
    /** @hide */
    public static final int CME_PHFSIM_PUK_REQUIRED = 7;
    /** @hide */
    public static final int CME_SIM_NOT_INSERTED = 10;
    /** @hide */
    public static final int CME_SIM_PIN_REQUIRED = 11;
    /** @hide */
    public static final int CME_SIM_PUK_REQUIRED = 12;
    /** @hide */
    public static final int CME_SIM_FAILURE = 13;
    /** @hide */
    public static final int CME_SIM_BUSY = 14;
    /** @hide */
    public static final int CME_SIM_WRONG = 15;
    /** @hide */
    public static final int CME_INCORRECT_PASSWORD = 16;
    /** @hide */
    public static final int CME_SIM_PIN2_REQUIRED = 17;
    /** @hide */
    public static final int CME_SIM_PUK2_REQUIRED = 18;
    /** @hide */
    public static final int CME_MEMORY_FULL = 20;
    /** @hide */
    public static final int CME_INVALID_INDEX = 21;
    /** @hide */
    public static final int CME_NOT_FOUND = 22;
    /** @hide */
    public static final int CME_MEMORY_FAILURE = 23;
    /** @hide */
    public static final int CME_TEXT_STRING_TOO_LONG = 24;
    /** @hide */
    public static final int CME_INVALID_CHARACTER_IN_TEXT_STRING = 25;
    /** @hide */
    public static final int CME_DIAL_STRING_TOO_LONG = 26;
    /** @hide */
    public static final int CME_INVALID_CHARACTER_IN_DIAL_STRING = 27;
    /** @hide */
    public static final int CME_NO_NETWORK_SERVICE = 30;
    /** @hide */
    public static final int CME_NETWORK_TIMEOUT = 31;
    /** @hide */
    public static final int CME_EMERGENCY_SERVICE_ONLY = 32;
    /** @hide */
    public static final int CME_NO_SIMULTANOUS_VOIP_CS_CALLS = 33;
    /** @hide */
    public static final int CME_NOT_SUPPORTED_FOR_VOIP = 34;
    /** @hide */
    public static final int CME_SIP_RESPONSE_CODE = 35;
    /** @hide */
    public static final int CME_NETWORK_PERSONALIZATION_PIN_REQUIRED = 40;
    /** @hide */
    /** @hide */
    public static final int CME_NETWORK_PERSONALIZATION_PUK_REQUIRED = 41;
    /** @hide */
    public static final int CME_NETWORK_SUBSET_PERSONALIZATION_PIN_REQUIRED = 42;
    /** @hide */
    /** @hide */
    /** @hide */
    public static final int CME_NETWORK_SUBSET_PERSONALIZATION_PUK_REQUIRED = 43;
    /** @hide */
    public static final int CME_SERVICE_PROVIDER_PERSONALIZATION_PIN_REQUIRED = 44;
    /** @hide */
    public static final int CME_SERVICE_PROVIDER_PERSONALIZATION_PUK_REQUIRED = 45;
    /** @hide */
    public static final int CME_CORPORATE_PERSONALIZATION_PIN_REQUIRED = 46;
    /** @hide */
    public static final int CME_CORPORATE_PERSONALIZATION_PUK_REQUIRED = 47;
    /** @hide */
    public static final int CME_HIDDEN_KEY_REQUIRED = 48;
    /** @hide */
    public static final int CME_EAP_NOT_SUPPORTED = 49;
    /** @hide */
    public static final int CME_INCORRECT_PARAMETERS = 50;

    /* Action policy for other calls when accepting call */
    /** @hide */
    public static final int CALL_ACCEPT_NONE = 0;
    /** @hide */
    public static final int CALL_ACCEPT_HOLD = 1;
    /** @hide */
    public static final int CALL_ACCEPT_TERMINATE = 2;

    private BluetoothAdapter mAdapter;
    private final BluetoothProfileConnector<IBluetoothHeadsetClient> mProfileConnector =
            new BluetoothProfileConnector(this, BluetoothProfile.HEADSET_CLIENT,
                    "BluetoothHeadsetClient", IBluetoothHeadsetClient.class.getName()) {
                @Override
                public IBluetoothHeadsetClient getServiceInterface(IBinder service) {
                    return IBluetoothHeadsetClient.Stub.asInterface(Binder.allowBlocking(service));
                }
    };

    /**
     * Create a BluetoothHeadsetClient proxy object.
     */
    /*package*/ BluetoothHeadsetClient(Context context, ServiceListener listener) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mProfileConnector.connect(context, listener);
    }

    /**
     * Close the connection to the backing service.
     * Other public functions of BluetoothHeadsetClient will return default error
     * results once close() has been called. Multiple invocations of close()
     * are ok.
     */
    /*package*/ void close() {
        if (VDBG) log("close()");
        mProfileConnector.disconnect();
    }

    private IBluetoothHeadsetClient getService() {
        return mProfileConnector.getService();
    }

    /**
     * Connects to remote device.
     *
     * Currently, the system supports only 1 connection. So, in case of the
     * second connection, this implementation will disconnect already connected
     * device automatically and will process the new one.
     *
     * @param device a remote device we want connect to
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CONNECTION_STATE_CHANGED} intent.
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    @UnsupportedAppUsage
    public boolean connect(BluetoothDevice device) {
        if (DBG) log("connect(" + device + ")");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.connect(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Disconnects remote device
     *
     * @param device a remote device we want disconnect
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CONNECTION_STATE_CHANGED} intent.
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    @UnsupportedAppUsage
    public boolean disconnect(BluetoothDevice device) {
        if (DBG) log("disconnect(" + device + ")");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.disconnect(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Return the list of connected remote devices
     *
     * @return list of connected devices; empty list if nothing is connected.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @NonNull
    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        if (VDBG) log("getConnectedDevices()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled()) {
            try {
                return service.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * Returns list of remote devices in a particular state
     *
     * @param states collection of states
     * @return list of devices that state matches the states listed in <code>states</code>; empty
     * list if nothing matches the <code>states</code>
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @NonNull
    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(@NonNull int[] states) {
        if (VDBG) log("getDevicesMatchingStates()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled()) {
            try {
                return service.getDevicesMatchingConnectionStates(states);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * Returns state of the <code>device</code>
     *
     * @param device a remote device
     * @return the state of connection of the device
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @Override
    public int getConnectionState(@Nullable BluetoothDevice device) {
        if (VDBG) log("getConnectionState(" + device + ")");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return BluetoothProfile.STATE_DISCONNECTED;
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * Set priority of the profile
     *
     * <p> The device should already be paired.
     * Priority can be one of {@link #PRIORITY_ON} or {@link #PRIORITY_OFF}
     *
     * @param device Paired bluetooth device
     * @param priority
     * @return true if priority is set, false on error
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    public boolean setPriority(BluetoothDevice device, int priority) {
        if (DBG) log("setPriority(" + device + ", " + priority + ")");
        return setConnectionPolicy(device, BluetoothAdapter.priorityToConnectionPolicy(priority));
    }

    /**
     * Set connection policy of the profile
     *
     * <p> The device should already be paired.
     * Connection policy can be one of {@link #CONNECTION_POLICY_ALLOWED},
     * {@link #CONNECTION_POLICY_FORBIDDEN}, {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    public boolean setConnectionPolicy(@Nullable BluetoothDevice device, int connectionPolicy) {
        if (DBG) log("setConnectionPolicy(" + device + ", " + connectionPolicy + ")");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            if (connectionPolicy != BluetoothProfile.CONNECTION_POLICY_FORBIDDEN
                    && connectionPolicy != BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
                return false;
            }
            try {
                return service.setConnectionPolicy(device, connectionPolicy);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Get the priority of the profile.
     *
     * <p> The priority can be any of:
     * {@link #PRIORITY_OFF}, {@link #PRIORITY_ON}, {@link #PRIORITY_UNDEFINED}
     *
     * @param device Bluetooth device
     * @return priority of the device
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public int getPriority(BluetoothDevice device) {
        if (VDBG) log("getPriority(" + device + ")");
        return BluetoothAdapter.connectionPolicyToPriority(getConnectionPolicy(device));
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p> The connection policy can be any of:
     * {@link #CONNECTION_POLICY_ALLOWED}, {@link #CONNECTION_POLICY_FORBIDDEN},
     * {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public int getConnectionPolicy(@Nullable BluetoothDevice device) {
        if (VDBG) log("getConnectionPolicy(" + device + ")");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionPolicy(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
    }

    /**
     * Starts voice recognition.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_AG_EVENT} intent.
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_VOICE_RECOGNITION}. This method invocation will fail silently when feature
     * is not supported.</p>
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public boolean startVoiceRecognition(@Nullable BluetoothDevice device) {
        if (DBG) log("startVoiceRecognition()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.startVoiceRecognition(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Send vendor specific AT command.
     *
     * @param device remote device
     * @param vendorId vendor number by Bluetooth SIG
     * @param atCommand command to be sent. It start with + prefix and only one command at one time.
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise.
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public boolean sendVendorAtCommand(@Nullable BluetoothDevice device, int vendorId,
                                             @NonNull String atCommand) {
        if (DBG) log("sendVendorSpecificCommand()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.sendVendorAtCommand(device, vendorId, atCommand);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Stops voice recognition.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_AG_EVENT} intent.
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_VOICE_RECOGNITION}. This method invocation will fail silently when feature
     * is not supported.</p>
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public boolean stopVoiceRecognition(@Nullable BluetoothDevice device) {
        if (DBG) log("stopVoiceRecognition()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.stopVoiceRecognition(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Returns list of all calls in any state.
     *
     * @param device remote device
     * @return list of calls; empty list if none call exists
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public List<BluetoothHeadsetClientCall> getCurrentCalls(BluetoothDevice device) {
        if (DBG) log("getCurrentCalls()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getCurrentCalls(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return null;
    }

    /**
     * Returns list of current values of AG indicators.
     *
     * @param device remote device
     * @return bundle of AG  indicators; null if device is not in CONNECTED state
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public @Nullable Bundle getCurrentAgEvents(@Nullable BluetoothDevice device) {
        if (DBG) log("getCurrentCalls()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getCurrentAgEvents(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return null;
    }

    /**
     * Accepts a call
     *
     * @param device remote device
     * @param flag action policy while accepting a call. Possible values {@link #CALL_ACCEPT_NONE},
     * {@link #CALL_ACCEPT_HOLD}, {@link #CALL_ACCEPT_TERMINATE}
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CALL_CHANGED} intent.
     *
     * @hide
     */
    @UnsupportedAppUsage
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public boolean acceptCall(BluetoothDevice device, int flag) {
        if (DBG) log("acceptCall()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.acceptCall(device, flag);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Holds a call.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CALL_CHANGED} intent.
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public boolean holdCall(BluetoothDevice device) {
        if (DBG) log("holdCall()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.holdCall(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Rejects a call.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CALL_CHANGED} intent.
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_REJECT_CALL}. This method invocation will fail silently when feature is not
     * supported.</p>
     *
     * @hide
     */
    @UnsupportedAppUsage
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public boolean rejectCall(BluetoothDevice device) {
        if (DBG) log("rejectCall()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.rejectCall(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Terminates a specified call.
     *
     * Works only when Extended Call Control is supported by Audio Gateway.
     *
     * @param device remote device
     * @param call Handle of call obtained in {@link #dial(BluetoothDevice, String)} or obtained via
     * {@link #ACTION_CALL_CHANGED}. {@code call} may be null in which case we will hangup all active
     * calls.
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CALL_CHANGED} intent.
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_ECC}. This method invocation will fail silently when feature is not
     * supported.</p>
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public boolean terminateCall(BluetoothDevice device, BluetoothHeadsetClientCall call) {
        if (DBG) log("terminateCall()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.terminateCall(device, call);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Enters private mode with a specified call.
     *
     * Works only when Extended Call Control is supported by Audio Gateway.
     *
     * @param device remote device
     * @param index index of the call to connect in private mode
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CALL_CHANGED} intent.
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_ECC}. This method invocation will fail silently when feature is not
     * supported.</p>
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public boolean enterPrivateMode(BluetoothDevice device, int index) {
        if (DBG) log("enterPrivateMode()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.enterPrivateMode(device, index);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Performs explicit call transfer.
     *
     * That means connect other calls and disconnect.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_CALL_CHANGED} intent.
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_MERGE_AND_DETACH}. This method invocation will fail silently when feature
     * is not supported.</p>
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public boolean explicitCallTransfer(BluetoothDevice device) {
        if (DBG) log("explicitCallTransfer()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.explicitCallTransfer(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Places a call with specified number.
     *
     * @param device remote device
     * @param number valid phone number
     * @return <code>{@link BluetoothHeadsetClientCall} call</code> if command has been issued
     * successfully; <code>{@link null}</code> otherwise; upon completion HFP sends {@link
     * #ACTION_CALL_CHANGED} intent in case of success; {@link #ACTION_RESULT} is sent otherwise;
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public BluetoothHeadsetClientCall dial(BluetoothDevice device, String number) {
        if (DBG) log("dial()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.dial(device, number);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return null;
    }

    /**
     * Sends DTMF code.
     *
     * Possible code values : 0,1,2,3,4,5,6,7,8,9,A,B,C,D,*,#
     *
     * @param device remote device
     * @param code ASCII code
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_RESULT} intent;
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public boolean sendDTMF(BluetoothDevice device, byte code) {
        if (DBG) log("sendDTMF()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.sendDTMF(device, code);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Get a number corresponding to last voice tag recorded on AG.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_LAST_VTAG} or {@link #ACTION_RESULT}
     * intent;
     *
     * <p>Feature required for successful execution is being reported by: {@link
     * #EXTRA_AG_FEATURE_ATTACH_NUMBER_TO_VT}. This method invocation will fail silently when
     * feature is not supported.</p>
     *
     * @hide
     */
    public boolean getLastVoiceTagNumber(BluetoothDevice device) {
        if (DBG) log("getLastVoiceTagNumber()");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getLastVoiceTagNumber(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Returns current audio state of Audio Gateway.
     *
     * Note: This is an internal function and shouldn't be exposed
     *
     * @hide
     */
    @SystemApi
    public int getAudioState(@Nullable BluetoothDevice device) {
        if (VDBG) log("getAudioState");
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled()) {
            try {
                return service.getAudioState(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED;
    }

    /**
     * Initiates a connection of audio channel.
     *
     * It setup SCO channel with remote connected Handsfree AG device.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_AUDIO_STATE_CHANGED} intent;
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    public boolean connectAudio(@Nullable BluetoothDevice device) {
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled()) {
            try {
                return service.connectAudio(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return false;
    }

    /**
     * Disconnects audio channel.
     *
     * It tears down the SCO channel from remote AG device.
     *
     * @param device remote device
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise; upon completion HFP sends {@link #ACTION_AUDIO_STATE_CHANGED} intent;
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    public boolean disconnectAudio(@Nullable BluetoothDevice device) {
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled()) {
            try {
                return service.disconnectAudio(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return false;
    }

    /**
     * Get Audio Gateway features
     *
     * @param device remote device
     * @return bundle of AG features; null if no service or AG not connected
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public @Nullable Bundle getCurrentAgFeatures(@Nullable BluetoothDevice device) {
        final IBluetoothHeadsetClient service =
                getService();
        if (service != null && isEnabled()) {
            try {
                return service.getCurrentAgFeatures(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return null;
    }

    private boolean isEnabled() {
        return mAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    private static boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
