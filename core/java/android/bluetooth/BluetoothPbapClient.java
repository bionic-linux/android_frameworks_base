/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the APIs to control the Bluetooth PBAP Client Profile.
 *
 * @hide
 */
public final class BluetoothPbapClient implements BluetoothProfile {

    private static final String TAG = "BluetoothPbapClient";
    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.pbapclient.profile.action.CONNECTION_STATE_CHANGED";

    /** There was an error trying to obtain the state */
    public static final int STATE_ERROR = -1;

    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;
    /** Connection canceled before completion. */
    public static final int RESULT_CANCELED = 2;

    /**
     * Broadcast Action: Indicates the status of downloading contacts from a remote device.
     * <p>Always contains the extra fields: {@link BluetoothDevice#EXTRA_DEVICE},
     * {@link #EXTRA_PHONEBOOK_PATH}, and {@link #EXTRA_STATUS_CODE}.
     * <p>If {@link #EXTRA_STATUS_CODE} == {@link #DOWNLOAD_STATUS_SUCCESS}, then will also
     * contain the extra fields: {@link #EXTRA_NUMBER_DOWNLOADED} and
     * {@link #EXTRA_NUMBER_EXPECTED}.
     * <p>Requires {@link android.Manifest.permission#READ_CONTACTS} to receive.
     *
     * @hide
     */
    public static final String ACTION_DOWNLOAD_STATUS =
            "android.bluetooth.pbapclient.profile.action.DOWNLOAD_STATUS";

    /**
     * Used as a String extra field in {@link #ACTION_DOWNLOAD_STATUS} intent.
     * It contains the phonebook being downloaded (e.g., favorites).
     * <p>Possible values are:
     * {@link com.android.bluetooth.pbapclient.PbapClientConnectionHandler#PB_PATH}
     * {@link com.android.bluetooth.pbapclient.PbapClientConnectionHandler#FAV_PATH}
     * {@link com.android.bluetooth.pbapclient.PbapClientConnectionHandler#MCH_PATH}
     * {@link com.android.bluetooth.pbapclient.PbapClientConnectionHandler#ICH_PATH}
     * {@link com.android.bluetooth.pbapclient.PbapClientConnectionHandler#OCH_PATH}
     * {@link com.android.bluetooth.pbapclient.PbapClientConnectionHandler#SIM_PB_PATH}
     * {@link com.android.bluetooth.pbapclient.PbapClientConnectionHandler#SIM_MCH_PATH}
     * {@link com.android.bluetooth.pbapclient.PbapClientConnectionHandler#SIM_ICH_PATH}
     * {@link com.android.bluetooth.pbapclient.PbapClientConnectionHandler#SIM_OCH_PATH}
     */
    public static final String EXTRA_PHONEBOOK_PATH =
            "android.bluetooth.pbapclient.profile.extra.PHONEBOOK_PATH";

    /**
     * Used as an Integer extra field in {@link #ACTION_DOWNLOAD_STATUS} intent.
     * It contains the download status code.
     * <p>Possible values are:
     * {@link #DOWNLOAD_STATUS_UNKNOWN} when the value is unknown.
     * {@link #DOWNLOAD_STATUS_ERROR} when there is an error.
     * {@link #DOWNLOAD_STATUS_SUCCESS} when contacts have been successfully downloaded.
     *
     * @hide
     */
    public static final String EXTRA_STATUS_CODE =
            "android.bluetooth.pbapclient.profile.extra.STATUS_CODE";

    /**
     * Used as the download status code for {@link #EXTRA_STATUS_CODE}
     *
     * @hide
     */
    public static final int DOWNLOAD_STATUS_UNKNOWN = -1;
    public static final int DOWNLOAD_STATUS_ERROR = 0;
    public static final int DOWNLOAD_STATUS_SUCCESS = 1;

    /**
     * Used as an Integer extra field in {@link #ACTION_DOWNLOAD_STATUS} intent.
     * It contains the number of contacts that have been downloaded.
     *
     * @hide
     */
    public static final String EXTRA_NUMBER_DOWNLOADED =
            "android.bluetooth.pbapclient.profile.extra.NUM_DOWNLOADED";

    /**
     * Used as an Integer extra field in {@link #ACTION_DOWNLOAD_STATUS} intent.
     * It contains the number of contacts in the phonebook.
     *
     * @hide
     */
    public static final String EXTRA_NUMBER_EXPECTED =
            "android.bluetooth.pbapclient.profile.extra.NUM_EXPECTED";

    private BluetoothAdapter mAdapter;
    private final BluetoothProfileConnector<IBluetoothPbapClient> mProfileConnector =
            new BluetoothProfileConnector(this, BluetoothProfile.PBAP_CLIENT,
                    "BluetoothPbapClient", IBluetoothPbapClient.class.getName()) {
                @Override
                public IBluetoothPbapClient getServiceInterface(IBinder service) {
                    return IBluetoothPbapClient.Stub.asInterface(Binder.allowBlocking(service));
                }
    };

    /**
     * Create a BluetoothPbapClient proxy object.
     */
    BluetoothPbapClient(Context context, ServiceListener listener) {
        if (DBG) {
            Log.d(TAG, "Create BluetoothPbapClient proxy object");
        }
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mProfileConnector.connect(context, listener);
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    /**
     * Close the connection to the backing service.
     * Other public functions of BluetoothPbapClient will return default error
     * results once close() has been called. Multiple invocations of close()
     * are ok.
     */
    public synchronized void close() {
        mProfileConnector.disconnect();
    }

    private IBluetoothPbapClient getService() {
        return mProfileConnector.getService();
    }

    /**
     * Initiate connection.
     * Upon successful connection to remote PBAP server the Client will
     * attempt to automatically download the users phonebook and call log.
     *
     * @param device a remote device we want connect to
     * @return <code>true</code> if command has been issued successfully; <code>false</code>
     * otherwise;
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    public boolean connect(BluetoothDevice device) {
        if (DBG) {
            log("connect(" + device + ") for PBAP Client.");
        }
        final IBluetoothPbapClient service = getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.connect(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    /**
     * Initiate disconnect.
     *
     * @param device Remote Bluetooth Device
     * @return false on error, true otherwise
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    public boolean disconnect(BluetoothDevice device) {
        if (DBG) {
            log("disconnect(" + device + ")" + new Exception());
        }
        final IBluetoothPbapClient service = getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                service.disconnect(device);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    /**
     * Get the list of connected devices.
     * Currently at most one.
     *
     * @return list of connected devices
     */
    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        if (DBG) {
            log("getConnectedDevices()");
        }
        final IBluetoothPbapClient service = getService();
        if (service != null && isEnabled()) {
            try {
                return service.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * Get the list of devices matching specified states. Currently at most one.
     *
     * @return list of matching devices
     */
    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        if (DBG) {
            log("getDevicesMatchingStates()");
        }
        final IBluetoothPbapClient service = getService();
        if (service != null && isEnabled()) {
            try {
                return service.getDevicesMatchingConnectionStates(states);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * Get connection state of device
     *
     * @return device connection state
     */
    @Override
    public int getConnectionState(BluetoothDevice device) {
        if (DBG) {
            log("getConnectionState(" + device + ")");
        }
        final IBluetoothPbapClient service = getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return BluetoothProfile.STATE_DISCONNECTED;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private boolean isEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.getState() == BluetoothAdapter.STATE_ON) {
            return true;
        }
        log("Bluetooth is Not enabled");
        return false;
    }

    private static boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

    /**
     * Set priority of the profile
     *
     * <p> The device should already be paired.
     * Priority can be one of {@link #PRIORITY_ON} or {@link #PRIORITY_OFF},
     *
     * @param device Paired bluetooth device
     * @param priority
     * @return true if priority is set, false on error
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
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
    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    public boolean setConnectionPolicy(@NonNull BluetoothDevice device,
            @ConnectionPolicy int connectionPolicy) {
        if (DBG) {
            log("setConnectionPolicy(" + device + ", " + connectionPolicy + ")");
        }
        final IBluetoothPbapClient service = getService();
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
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
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
    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
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
    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    public @ConnectionPolicy int getConnectionPolicy(@NonNull BluetoothDevice device) {
        if (VDBG) {
            log("getConnectionPolicy(" + device + ")");
        }
        final IBluetoothPbapClient service = getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionPolicy(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
    }
}
