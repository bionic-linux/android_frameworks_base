/*
 * Copyright 2019 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

/**
 * Connector for Bluetooth profile proxies to bind manager service and
 * profile services
 * @hide
 */
public final class BluetoothProfileConnector {
    private int mProfileId;
    private BluetoothProfile.ServiceListener mServiceListener;
    private BluetoothProfile mProfileProxy;
    private Context mContext;
    private volatile Object mService;

    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback =
            new IBluetoothStateChangeCallback.Stub() {
        public void onBluetoothStateChange(boolean up) {
            logDebug("onBluetoothStateChange: up=" + up);
            if (up) {
                doBind();
            } else {
                doUnbind();
            }
        }
    };

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            logDebug("Proxy object connected");
            mService = getServiceInterface(mProfileId, service);

            if (mServiceListener != null) {
                mServiceListener.onServiceConnected(mProfileId, mProfileProxy);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            logDebug("Proxy object disconnected");
            doUnbind();
            if (mServiceListener != null) {
                mServiceListener.onServiceDisconnected(BluetoothProfile.A2DP);
            }
        }
    };

    BluetoothProfileConnector(int profileId, BluetoothProfile profile) {
        mProfileId = profileId;
        mProfileProxy = profile;
    }

    private boolean doBind() {
        synchronized (mConnection) {
            if (mService == null) {
                logDebug("Binding service...");
                try {
                    Intent intent = new Intent(getServiceName(mProfileId));
                    ComponentName comp = intent.resolveSystemService(
                            mContext.getPackageManager(), 0);
                    intent.setComponent(comp);
                    if (comp == null || !mContext.bindServiceAsUser(intent, mConnection, 0,
                            UserHandle.CURRENT_OR_SELF)) {
                        logError("Could not bind to Bluetooth Service with " + intent);
                        return false;
                    }
                } catch (SecurityException se) {
                    logError("Failed to bind service. " + se);
                    return false;
                }
            }
        }
        return true;
    }

    private void doUnbind() {
        synchronized (mConnection) {
            if (mService != null) {
                logDebug("Unbinding service...");
                try {
                    mContext.unbindService(mConnection);
                } catch (IllegalArgumentException ie) {
                    logError("Unable to unbind service: " + ie);
                } finally {
                    mService = null;
                }
            }
        }
    }

    void connect(Context context, BluetoothProfile.ServiceListener listener) {
        mContext = context;
        mServiceListener = listener;
        IBluetoothManager mgr = BluetoothAdapter.getDefaultAdapter().getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException re) {
                logError("Failed to register state change callback. " + re);
            }
        }
        doBind();
    }

    void disconnect() {
        mServiceListener = null;
        IBluetoothManager mgr = BluetoothAdapter.getDefaultAdapter().getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.unregisterStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException re) {
                logError("Failed to unregister state change callback" + re);
            }
        }
        doUnbind();
    }

    Object getService() {
        return mService;
    }

    private static Object getServiceInterface(int profile, IBinder service) {
        switch (profile) {
            case BluetoothProfile.A2DP:
                return IBluetoothA2dp.Stub.asInterface(Binder.allowBlocking(service));
            case BluetoothProfile.A2DP_SINK:
                return IBluetoothA2dpSink.Stub.asInterface(Binder.allowBlocking(service));
            case BluetoothProfile.AVRCP_CONTROLLER:
                return IBluetoothAvrcpController.Stub.asInterface(Binder.allowBlocking(service));
            case BluetoothProfile.HEADSET:
                return IBluetoothHeadset.Stub.asInterface(Binder.allowBlocking(service));
            case BluetoothProfile.HEADSET_CLIENT:
                return IBluetoothHeadsetClient.Stub.asInterface(Binder.allowBlocking(service));
            case BluetoothProfile.HID_HOST:
                return IBluetoothHidHost.Stub.asInterface(Binder.allowBlocking(service));
            case BluetoothProfile.PAN:
                return IBluetoothPan.Stub.asInterface(Binder.allowBlocking(service));
            case BluetoothProfile.PBAP:
                return IBluetoothPbap.Stub.asInterface(Binder.allowBlocking(service));
            case BluetoothProfile.PBAP_CLIENT:
                return IBluetoothPbapClient.Stub.asInterface(Binder.allowBlocking(service));
            case BluetoothProfile.MAP:
                return IBluetoothMap.Stub.asInterface(Binder.allowBlocking(service));
            case BluetoothProfile.MAP_CLIENT:
                return IBluetoothMapClient.Stub.asInterface(Binder.allowBlocking(service));
            case BluetoothProfile.HID_DEVICE:
                return IBluetoothHidDevice.Stub.asInterface(Binder.allowBlocking(service));
            case BluetoothProfile.SAP:
                return IBluetoothSap.Stub.asInterface(Binder.allowBlocking(service));
            case BluetoothProfile.HEARING_AID:
                return IBluetoothHearingAid.Stub.asInterface(Binder.allowBlocking(service));
            default:
                throw new IllegalArgumentException("invalid profile: " + profile);
        }
    }

    private static String getServiceName(int profile) {
        switch (profile) {
            case BluetoothProfile.A2DP:
                return IBluetoothA2dp.class.getName();
            case BluetoothProfile.A2DP_SINK:
                return IBluetoothA2dpSink.class.getName();
            case BluetoothProfile.AVRCP_CONTROLLER:
                return IBluetoothAvrcpController.class.getName();
            case BluetoothProfile.HEADSET:
                return IBluetoothHeadset.class.getName();
            case BluetoothProfile.HEADSET_CLIENT:
                return IBluetoothHeadsetClient.class.getName();
            case BluetoothProfile.HID_HOST:
                return IBluetoothHidHost.class.getName();
            case BluetoothProfile.PAN:
                return IBluetoothPan.class.getName();
            case BluetoothProfile.PBAP:
                return IBluetoothPbap.class.getName();
            case BluetoothProfile.PBAP_CLIENT:
                return IBluetoothPbapClient.class.getName();
            case BluetoothProfile.MAP:
                return IBluetoothMap.class.getName();
            case BluetoothProfile.MAP_CLIENT:
                return IBluetoothMapClient.class.getName();
            case BluetoothProfile.HID_DEVICE:
                return IBluetoothHidDevice.class.getName();
            case BluetoothProfile.SAP:
                return IBluetoothSap.class.getName();
            case BluetoothProfile.HEARING_AID:
                return IBluetoothHearingAid.class.getName();
            default:
                throw new IllegalArgumentException("invalid profile: " + profile);
        }
    }

    private static String getProfileName(int profile) {
        switch (profile) {
            case BluetoothProfile.A2DP:
                return "BluetoothA2dp";
            case BluetoothProfile.A2DP_SINK:
                return "BluetoothA2dpSink";
            case BluetoothProfile.AVRCP_CONTROLLER:
                return "BluetoothAvrcpController";
            case BluetoothProfile.HEADSET:
                return "BluetoothHeadset";
            case BluetoothProfile.HEADSET_CLIENT:
                return "BluetoothHeadsetClient";
            case BluetoothProfile.HID_HOST:
                return "BluetoothHidHost";
            case BluetoothProfile.PAN:
                return "BluetoothPan";
            case BluetoothProfile.PBAP:
                return "BluetoothPbap";
            case BluetoothProfile.PBAP_CLIENT:
                return "BluetoothPbapClient";
            case BluetoothProfile.MAP:
                return "BluetoothMap";
            case BluetoothProfile.MAP_CLIENT:
                return "BluetoothMapClient";
            case BluetoothProfile.HID_DEVICE:
                return "BluetoothHidDevice";
            case BluetoothProfile.SAP:
                return "BluetoothSap";
            case BluetoothProfile.HEARING_AID:
                return "BluetoothHearingAid";
            default:
                throw new IllegalArgumentException("invalid profile: " + profile);
        }
    }

    private void logDebug(String log) {
        Log.d(getProfileName(mProfileId), log);
    }

    private void logInfo(String log) {
        Log.i(getProfileName(mProfileId), log);
    }

    private void logWarrn(String log) {
        Log.w(getProfileName(mProfileId), log);
    }

    private void logError(String log) {
        Log.e(getProfileName(mProfileId), log);
    }
}
