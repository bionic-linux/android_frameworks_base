/*
 * Copyright 2019 Codecoup
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
import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SystemApi;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.CloseGuard;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * This class provides the public APIs to control the Bluetooth CSIS service.
 *
 * <p>BluetoothCsisClient is a proxy object for controlling the Bluetooth VC
 * Service via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get
 * the BluetoothCsisClient proxy object.
 *
 */
public final class BluetoothCsisClient implements BluetoothProfile, AutoCloseable {
    private static final String TAG = "BluetoothCsisClient";
    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    private CloseGuard mCloseGuard;

    /**
     * @hide
     */
    @SystemApi
    public static interface ClientLockCallback {
        /**
         * @hide
         */
        @SystemApi
        public abstract void onGroupLockSet(int group_id, int op_status, boolean is_locked);
    }

    private static class BluetoothCsisClientLockCallbackDelegate
            extends IBluetoothCsisClientLockCallback.Stub {
        private final ClientLockCallback mCallback;
        private final Executor mExecutor;

        BluetoothCsisClientLockCallbackDelegate(Executor executor, ClientLockCallback callback) {
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        public void onGroupLockSet(int group_id, int op_status, boolean is_locked) {
            mExecutor.execute(() -> mCallback.onGroupLockSet(group_id, op_status, is_locked));
        }
    };

    /**
     * Intent used to broadcast the change in connection state of the CSIS
     * Client.
     *
     * <p>This intent will have 3 extras:
     * <ul>
     * <li> {@link #EXTRA_STATE} - The current state of the profile. </li>
     * <li> {@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.</li>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * </ul>
     *
     * <p>{@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of
     * {@link #STATE_DISCONNECTED}, {@link #STATE_CONNECTING},
     * {@link #STATE_CONNECTED}, {@link #STATE_DISCONNECTING}.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission to
     * receive.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CSIS_CONNECTION_STATE_CHANGED =
            "android.bluetooth.action.CSIS_CONNECTION_STATE_CHANGED";

    /**
     * Intent used to expose broadcast receiving device.
     *
     * <p>This intent will have 2 extras:
     * <ul>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote Broadcast receiver device. </li>
     * <li> {@link #EXTRA_CSIS_GROUP_ID} - Group identifier. </li>
     * <li> {@link #EXTRA_CSIS_GROUP_SIZE} - Group size. </li>
     * <li> {@link #EXTRA_CSIS_GROUP_TYPE_UUID} - Group type UUID. </li>
     * </ul>
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission to receive.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CSIS_DEVICE_AVAILABLE =
            "android.bluetooth.action.CSIS_DEVICE_AVAILABLE";

    /**
     * Used as an extra field in {@link #ACTION_CSIS_DEVICE_AVAILABLE} intent.
     * Contains the group id.
     */
    public static final String EXTRA_CSIS_GROUP_ID = "android.bluetooth.extra.CSIS_GROUP_ID";

    /**
     * Group size as int extra field in {@link #ACTION_CSIS_DEVICE_AVAILABLE} intent.
     * @hide
     */
    public static final String EXTRA_CSIS_GROUP_SIZE = "android.bluetooth.extra.CSIS_GROUP_SIZE";

    /**
     * Group type uuid extra field in {@link #ACTION_CSIS_DEVICE_AVAILABLE} intent.
     * @hide
     */
    public static final String EXTRA_CSIS_GROUP_TYPE_UUID =
            "android.bluetooth.extra.CSIS_GROUP_TYPE_UUID";

    /**
     * Intent used to broadcast information about identified set member
     * ready to connect.
     *
     * <p>This intent will have one extra:
     * <ul>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. It can
     * be null if no device is active. </li>
     * <li>  {@link #EXTRA_CSIS_GROUP_ID} - Group identifier. </li>
     * </ul>
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission to
     * receive.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CSIS_SET_MEMBER_AVAILABLE =
            "android.bluetooth.action.CSIS_SET_MEMBER_AVAILABLE";

    /**
     * This represents an invalid group ID.
     *
     * @hide
     */
    public static final int GROUP_ID_INVALID = IBluetoothCsisClient.CSIS_GROUP_ID_INVALID;

    /**
     * Indicating that group was locked with success.
     * @hide
     */
    public static final int GROUP_LOCK_SUCCESS = 0;

    /**
     * Indicating that group locked failed due to invalid group ID.
     * @hide
     */
    public static final int GROUP_LOCK_FAILED_INVALID_GROUP = 1;

    /**
     * Indicating that group locked failed due to empty group.
     * @hide
     */
    public static final int GROUP_LOCK_FAILED_GROUP_EMPTY = 2;

    /**
     * Indicating that group locked failed due to group members being disconnected.
     * @hide
     */
    public static final int GROUP_LOCK_FAILED_GROUP_NOT_CONNECTED = 3;

    /**
     * Indicating that group locked failed due to group member being already locked.
     * @hide
     */
    public static final int GROUP_LOCK_FAILED_LOCKED_BY_OTHER = 4;

    /**
     * Indicating that group locked failed due to other reason.
     * @hide
     */
    public static final int GROUP_LOCK_FAILED_OTHER_REASON = 5;

    /**
     * Indicating that group member in locked state was lost.
     * @hide
     */
    public static final int LOCKED_GROUP_MEMBER_LOST = 6;

    private BluetoothAdapter mAdapter;
    private final BluetoothProfileConnector<IBluetoothCsisClient> mProfileConnector =
            new BluetoothProfileConnector(
                    this, BluetoothProfile.CSIS_CLIENT, TAG, IBluetoothCsisClient.class.getName()) {
                @Override
                public IBluetoothCsisClient getServiceInterface(IBinder service) {
                    return IBluetoothCsisClient.Stub.asInterface(Binder.allowBlocking(service));
                }
            };

    /**
     * Create a BluetoothCsisClient proxy object for interacting with the local
     * Bluetooth CSIS service.
     */
    /*package*/ BluetoothCsisClient(Context context, ServiceListener listener) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mProfileConnector.connect(context, listener);
        mCloseGuard = new CloseGuard();
        mCloseGuard.open("close");
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    protected void finalize() {
        if (mCloseGuard != null) {
            mCloseGuard.warnIfOpen();
        }
        close();
    }

    /**
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    public void close() {
        mProfileConnector.disconnect();
    }

    private IBluetoothCsisClient getService() { return mProfileConnector.getService(); }

    /**
     * Initiate connection to a profile of the remote bluetooth device.
     *
     * <p> This API returns false in scenarios like the profile on the
     * device is already connected or Bluetooth is not turned on.
     * When this API returns true, it is guaranteed that
     * connection state intent for the profile will be broadcasted with
     * the state. Users can get the connection state of the profile
     * from this intent.
     *
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error, true otherwise
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    public boolean connect(@Nullable BluetoothDevice device) {
        if (DBG)
            log("connect(" + device + ")");
        final IBluetoothCsisClient service = getService();
        try {
            if (service != null && isEnabled() && isValidDevice(device)) {
                return service.connect(device);
            }
            if (service == null)
                Log.w(TAG, "Proxy not attached to service");
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
        }
        return false;
    }

    /**
     * Initiate disconnection from a profile
     *
     * <p> This API will return false in scenarios like the profile on the
     * Bluetooth device is not in connected state etc. When this API returns,
     * true, it is guaranteed that the connection state change
     * intent will be broadcasted with the state. Users can get the
     * disconnection state of the profile from this intent.
     *
     * <p> If the disconnection is initiated by a remote device, the state
     * will transition from {@link #STATE_CONNECTED} to
     * {@link #STATE_DISCONNECTED}. If the disconnect is initiated by the
     * host (local) device the state will transition from
     * {@link #STATE_CONNECTED} to state {@link #STATE_DISCONNECTING} to
     * state {@link #STATE_DISCONNECTED}. The transition to
     * {@link #STATE_DISCONNECTING} can be used to distinguish between the
     * two scenarios.
     *
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error, true otherwise
     */

    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    public boolean disconnect(@Nullable BluetoothDevice device) {
        final IBluetoothCsisClient service = getService();
        try {
            if (service != null && isEnabled() && isValidDevice(device)) {
                return service.disconnect(device);
            }
            if (service == null)
                Log.w(TAG, "Proxy not attached to service");
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
        }
        return false;
    }

    /**
     * Lock the set.
     * @param groupId group ID to lock,
     * @param executor callback executor,
     * @param cb callback to report lock and unlock events - stays valid until the app unlocks
     *           using the returned lock identifier or the lock timeouts on the remote side,
     *           as per CSIS specification,
     * @return unique lock identifier used for unlocking or null if lock has failed.
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    public @Nullable UUID groupLock(int groupId, @Nullable @CallbackExecutor Executor executor,
            @Nullable ClientLockCallback cb) {
        if (VDBG)
            log("groupLockSet()");
        final IBluetoothCsisClient service = getService();
        try {
            if (service != null && isEnabled()) {
                IBluetoothCsisClientLockCallback delegate = null;
                if ((executor != null) && (cb != null)) {
                    delegate = new BluetoothCsisClientLockCallbackDelegate(executor, cb);
                }
                return service.groupLock(groupId, delegate).getUuid();
            }
            if (service == null)
                Log.w(TAG, "Proxy not attached to service");
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return null;
        }
    }

    /**
     * Unlock the set.
     * @param lockUuid unique lock identifier
     * @return lock identifier or null if lock failed
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    public boolean groupUnlock(@NonNull UUID lockUuid) {
        if (VDBG)
            log("groupLockSet()");
        if (lockUuid == null)
            return false;

        final IBluetoothCsisClient service = getService();
        try {
            if (service != null && isEnabled()) {
                service.groupUnlock(new ParcelUuid(lockUuid));
                return true;
            }
            if (service == null)
                Log.w(TAG, "Proxy not attached to service");
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        }
    }

    /**
     * Get device's groups.
     * @param device the active device
     * @return Map of groups ids and related UUIDs
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public @NonNull Map getDeviceGroups(@Nullable BluetoothDevice device) {
        if (VDBG)
            log("getDeviceGroups()");
        final IBluetoothCsisClient service = getService();
        try {
            if (service != null && isEnabled()) {
                return service.getDeviceGroups(device);
            }
            if (service == null)
                Log.w(TAG, "Proxy not attached to service");
            return new HashMap<>();
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return new HashMap<>();
        }
    }

    /**
     * Get group id for the given UUID
     * @param uuid
     * @return list of group IDs
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public @NonNull List<Integer> getAllGroupIds(@Nullable ParcelUuid uuid) {
        if (VDBG)
            log("getAllGroupIds()");
        final IBluetoothCsisClient service = getService();
        try {
            if (service != null && isEnabled()) {
                return service.getAllGroupIds(uuid);
            }
            if (service == null)
                Log.w(TAG, "Proxy not attached to service");
            return new ArrayList<Integer>();
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return new ArrayList<Integer>();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull List<BluetoothDevice> getConnectedDevices() {
        if (VDBG)
            log("getConnectedDevices()");
        final IBluetoothCsisClient service = getService();
        if (service != null && isEnabled()) {
            try {
                return service.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        if (service == null)
            Log.w(TAG, "Proxy not attached to service");
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull List<BluetoothDevice> getDevicesMatchingConnectionStates(
            @NonNull int[] states) {
        if (VDBG)
            log("getDevicesMatchingStates(states=" + Arrays.toString(states) + ")");
        final IBluetoothCsisClient service = getService();
        if (service != null && isEnabled()) {
            try {
                return service.getDevicesMatchingConnectionStates(states);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        if (service == null)
            Log.w(TAG, "Proxy not attached to service");
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @BluetoothProfile.BtProfileState int getConnectionState(
            @Nullable BluetoothDevice device) {
        if (VDBG)
            log("getState(" + device + ")");
        final IBluetoothCsisClient service = getService();
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return BluetoothProfile.STATE_DISCONNECTED;
            }
        }
        if (service == null)
            Log.w(TAG, "Proxy not attached to service");
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * Set connection policy of the profile
     *
     * <p> The device should already be paired.
     * Connection policy can be one of {@link #CONNECTION_POLICY_ALLOWED},
     * {@link #CONNECTION_POLICY_FORBIDDEN}, {@link #CONNECTION_POLICY_UNKNOWN}
     *
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    public boolean setConnectionPolicy(
            @Nullable BluetoothDevice device, @ConnectionPolicy int connectionPolicy) {
        if (DBG)
            log("setConnectionPolicy(" + device + ", " + connectionPolicy + ")");
        final IBluetoothCsisClient service = getService();
        try {
            if (service != null && isEnabled() && isValidDevice(device)) {
                if (connectionPolicy != BluetoothProfile.CONNECTION_POLICY_FORBIDDEN
                        && connectionPolicy != BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
                    return false;
                }
                return service.setConnectionPolicy(device, connectionPolicy);
            }
            if (service == null)
                Log.w(TAG, "Proxy not attached to service");
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        }
    }

    /**
     * Set priority of the profile
     *
     * <p> The device should already be paired.
     * Priority can be one of {@link #PRIORITY_ON} orgetBluetoothManager
     * {@link #PRIORITY_OFF},
     *
     *
     * @param device Paired bluetooth device
     * @param priority
     * @return true if priority is set, false on error
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public boolean setPriority(@Nullable BluetoothDevice device, int priority) {
        if (DBG)
            log("setPriority(" + device + ", " + priority + ")");
        return setConnectionPolicy(device, BluetoothAdapter.priorityToConnectionPolicy(priority));
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p> The connection policy can be any of:
     * {@link #CONNECTION_POLICY_ALLOWED}, {@link #CONNECTION_POLICY_FORBIDDEN},
     * {@link #CONNECTION_POLICY_UNKNOWN}
     *
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
    public @ConnectionPolicy int getConnectionPolicy(@Nullable BluetoothDevice device) {
        if (VDBG)
            log("getConnectionPolicy(" + device + ")");
        final IBluetoothCsisClient service = getService();
        try {
            if (service != null && isEnabled() && isValidDevice(device)) {
                return service.getConnectionPolicy(device);
            }
            if (service == null)
                Log.w(TAG, "Proxy not attached to service");
            return BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
        }
    }

    /**
     * Get the priority of the profile.
     *
     * <p> The priority can be any of:
     * {@link #PRIORITY_AUTO_CONNECT}, {@link #PRIORITY_OFF},
     * {@link #PRIORITY_ON}, {@link #PRIORITY_UNDEFINED}
     *
     * @param device Bluetooth device
     * @return priority of the device
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public int getPriority(@Nullable BluetoothDevice device) {
        if (VDBG)
            log("getPriority(" + device + ")");
        return BluetoothAdapter.connectionPolicyToPriority(getConnectionPolicy(device));
    }

    private boolean isEnabled() { return mAdapter.getState() == BluetoothAdapter.STATE_ON; }

    private static boolean isValidDevice(@Nullable BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

    private static void log(String msg) { Log.d(TAG, msg); }
}
