/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.net.ConnectivityManager.TETHER_ERROR_NO_ERROR;

import android.net.ConnectivityManager.OnTetheringEventCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.ArrayMap;

import java.io.PrintWriter;
import java.util.StringJoiner;
import java.util.concurrent.Executor;

/**
 * Service used to communicate with the tethering, which is running in a separate module.
 * @hide
 */
public class TetheringManager {
    private static final String TAG = TetheringManager.class.getSimpleName();

    private static TetheringManager sInstance;

    private ITetheringConnector mConnector;
    private ITetheringEventCallback mCallback;
    private Network mTetherUpstream;
    private TetheringConfigurationParcel mTetheringConfiguration;
    private TetherStatesParcel mTetherStatesParcel;

    private final ArrayMap<OnTetheringEventCallback, ITetheringEventCallback>
            mTetheringEventCallbacks = new ArrayMap<>();


    private TetheringManager(ITetheringConnector connector) {
        mConnector = connector;
        mCallback = new TetheringEventCallback();
        try {
            mConnector.registerTetheringEventCallback(mCallback);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private class TetheringEventCallback extends ITetheringEventCallback.Stub {
        @Override
        public void onUpstreamChanged(Network network) {
            reportUpstreamChanged(network);
        }

        @Override
        public void onConfigurationChanged(TetheringConfigurationParcel config) {
            mTetheringConfiguration = config;
        }

        @Override
        public void onTetherStatesChanged(TetherStatesParcel states) {
            mTetherStatesParcel = states;
        }
    }

    private void reportUpstreamChanged(Network network) {
        synchronized (mTetheringEventCallbacks) {
            mTetherUpstream = network;
            for (int i = 0; i < mTetheringEventCallbacks.size(); i++) {
                try {
                    mTetheringEventCallbacks.valueAt(i).onUpstreamChanged(mTetherUpstream);
                } catch (RemoteException e) {
                    // Not really very much to do here.
                }
            }
        }
    }

    /**
     * Attempt to tether the named interface.  This will setup a dhcp server
     * on the interface, forward and NAT IP v4 packets and forward DNS requests
     * to the best active upstream network interface.  Note that if no upstream
     * IP network interface is available, dhcp will still run and traffic will be
     * allowed between the tethered devices and this device, though upstream net
     * access will of course fail until an upstream network interface becomes
     * active. Note: return value do not have any meaning. It is better to use
     * #getTetherableIfaces() to ensure corresponding interface is available for
     * tethering before calling #tether().
     *
     * TODO: Deprecate this API. The only usages should be in PanService and Wifi P2P which
     * need direct access.
     *
     * {@hide}
     */
    public int tether(String iface) {
        try {
            mConnector.tether(iface);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        return TETHER_ERROR_NO_ERROR;
    }

    /**
     * Stop tethering the named interface.
     *
     * {@hide}
     */
    public int untether(String iface) {
        try {
            mConnector.untether(iface);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        return TETHER_ERROR_NO_ERROR;
    }

    /**
     * Attempt to both alter the mode of USB and Tethering of USB. WARNING: New client should not
     * use this API anymore. All clients should use #startTethering or #stopTethering which
     * encapsulate proper entitlement logic. Using this API while entitlement check is needed, usb
     * tethering would not have upstream if entitlement check is not lunched.
     *
     * {@hide}
     */
    public int setUsbTethering(boolean enable) {
        try {
            mConnector.setUsbTethering(enable);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        return TETHER_ERROR_NO_ERROR;
    }

    /**
     * Starts tethering and runs tether provisioning for the given type if needed. If provisioning
     * fail, tethering would be closed automatically.
     *
     * {@hide}
     */
    public void startTethering(int type, ResultReceiver receiver, boolean showProvisioningUi) {
        try {
            mConnector.startTethering(type, receiver, showProvisioningUi);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stops tethering for the given type. Also cancels any provisioning rechecks for that type if
     * applicable.
     *
     * {@hide}
     */
    public void stopTethering(int type) {
        try {
            mConnector.stopTethering(type);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the latest value of the tethering entitlement check.
     *
     * Note: Allow privileged apps who have TETHER_PRIVILEGED permission to access. If it turns
     * out some such apps are observed to abuse this API, change to per-UID limits on this API
     * if it's really needed.
     */
    public void getLatestTetheringEntitlementResult(int type, ResultReceiver receiver,
            boolean showEntitlementUi) {
        try {
            mConnector.getLatestTetheringEntitlementResult(type, receiver, showEntitlementUi);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Register tethering event callback.
     *
     * {@hide}
     */
    public void registerTetheringEventCallback(Executor executor,
            OnTetheringEventCallback callback) {
        synchronized (mTetheringEventCallbacks) {
            if (!mTetheringEventCallbacks.containsKey(callback)) {
                throw new IllegalArgumentException("callback was already registered.");
            }
            ITetheringEventCallback relayedCallback = new ITetheringEventCallback.Stub() {
                @Override
                public void onUpstreamChanged(Network network) throws RemoteException {
                    executor.execute(() -> {
                        callback.onUpstreamChanged(network);
                    });
                }

                @Override
                public void onConfigurationChanged(TetheringConfigurationParcel config) { }

                @Override
                public void onTetherStatesChanged(TetherStatesParcel states) { }
            };
            callback.onUpstreamChanged(mTetherUpstream);
            mTetheringEventCallbacks.put(callback, relayedCallback);
        }
    }

    /**
     * Unregister tethering event callback.
     *
     * {@hide}
     */
    public void unregisterTetheringEventCallback(OnTetheringEventCallback callback) {
        synchronized (mTetheringEventCallbacks) {
            ITetheringEventCallback relayedCallback = mTetheringEventCallbacks.remove(callback);
            if (relayedCallback == null) {
                throw new IllegalArgumentException("callback was not registered.");
            }
        }
    }

    /**
     * Get a more detailed error code after a Tethering or Untethering
     * request asynchronously failed.
     *
     * {@hide}
     */
    public int getLastTetherError(String iface) {
        if (mTetherStatesParcel == null) return TETHER_ERROR_NO_ERROR;
        int i = 0;
        for (String errored : mTetherStatesParcel.erroredIfaceList) {
            if (iface.equals(errored)) return mTetherStatesParcel.lastErrorList[i];

            i++;
        }
        return TETHER_ERROR_NO_ERROR;
    }

    /**
     * Get the list of regular expressions that define any tetherable
     * USB network interfaces.  If USB tethering is not supported by the
     * device, this list should be empty.
     *
     * {@hide}
     */
    public String[] getTetherableUsbRegexs() {
        if (mTetheringConfiguration == null) return new String[0];
        return mTetheringConfiguration.tetherableUsbRegexs;
    }

    /**
     * Get the list of regular expressions that define any tetherable
     * Wifi network interfaces.  If Wifi tethering is not supported by the
     * device, this list should be empty.
     *
     * {@hide}
     */
    public String[] getTetherableWifiRegexs() {
        if (mTetheringConfiguration == null) return new String[0];
        return mTetheringConfiguration.tetherableWifiRegexs;
    }

    /**
     * Get the list of regular expressions that define any tetherable
     * Bluetooth network interfaces.  If Bluetooth tethering is not supported by the
     * device, this list should be empty.
     *
     * {@hide}
     */
    public String[] getTetherableBluetoothRegexs() {
        if (mTetheringConfiguration == null) return new String[0];
        return mTetheringConfiguration.tetherableBluetoothRegexs;
    }

    /**
     * Get the set of tetherable, available interfaces.  This list is limited by
     * device configuration and current interface existence.
     *
     * {@hide}
     */
    public String[] getTetherableIfaces() {
        if (mTetherStatesParcel == null) return new String[0];
        return mTetherStatesParcel.availableList;
    }

    /**
     * Get the set of tethered interfaces.
     *
     * {@hide}
     */
    public String[] getTetheredIfaces() {
        if (mTetherStatesParcel == null) return new String[0];
        return mTetherStatesParcel.tetheredList;
    }

    /**
     * Get the set of interface names which attempted to tether but
     * failed.
     *
     * {@hide}
     */
    public String[] getTetheringErroredIfaces() {
        if (mTetherStatesParcel == null) return new String[0];
        return mTetherStatesParcel.erroredIfaceList;
    }

    /**
     * Get the set of tethered dhcp ranges.
     *
     * {@hide}
     */
    public String[] getTetheredDhcpRanges() {
        if (mTetheringConfiguration == null) return new String[0];
        return mTetheringConfiguration.legacyDhcpRanges;
    }

    /**
     * Check if the device allows for tethering.
     *
     * {@hide}
     */
    public boolean hasTetherableConfiguration() {
        if (mTetheringConfiguration == null) return false;
        final boolean hasDownstreamConfiguration =
                (mTetheringConfiguration.tetherableUsbRegexs.length != 0)
                || (mTetheringConfiguration.tetherableWifiRegexs.length != 0)
                || (mTetheringConfiguration.tetherableBluetoothRegexs.length != 0);
        final boolean hasUpstreamConfiguration =
                (mTetheringConfiguration.preferredUpstreamIfaceTypes.length != 0)
                || mTetheringConfiguration.chooseUpstreamAutomatically;

        return hasDownstreamConfiguration && hasUpstreamConfiguration;
    }
}
