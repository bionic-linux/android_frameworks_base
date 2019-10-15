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

package com.android.server.adb;

import android.content.Context;
import android.content.Intent;
import android.debug.AdbManager;
import android.debug.PairDevice;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;

import com.android.server.FgThread;
import com.android.server.adb.AdbKeyStoreProto.Key;
import com.android.server.adb.AdbKeyStoreProto.KeyStore;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Provides communication to the Android Debug Daemon to allow, deny, query,
 * or configure the adb wireless debugging.
 */

public class WirelessDebuggingManager {
    private static final String TAG = "WirelessDebuggingManager";
    //private static final boolean DEBUG = false;
    private static final boolean DEBUG = true;

    private static final String ADBDWIFI_SOCKET = "adbdwifi";
    private static final String ADBDWIFI_KEYS = "/data/misc/adb/adb_wifi_keys";
    private static final int BUFFER_SIZE = 4096;
    private static final String WIRELESS_DEBUG_PERSISTENT_CONFIG_PROPERTY =
            "persist.sys.wireless.debug";
    private final Context mContext;
    private final Handler mHandler;
    private WirelessDebuggingThread mThread;
    private PairingThread mPairingThread = null;
    private boolean mAdbWirelessEnabled = false;
    private int mState;
    private String mDeviceName;
    HashMap<String, PairDevice> mPairedDevices = new HashMap<String, PairDevice>();
    HashMap<String, PairDevice> mPairingDevices = new HashMap<String, PairDevice>();
    private static final int PAIRING_CODE_LENGTH = 6;
    private byte[] mOurKey;
    private byte[] mTheirKey;

    private static native boolean native_keystore_init();

    public WirelessDebuggingManager(Context context) {
        mHandler = new WirelessDebuggingHandler(FgThread.get().getLooper());
        mContext = context;
    }

    class PairingThread extends Thread {
        private String mGuid;
        private String mName;
        private String mPairingCode;

        private native boolean native_pairing_start(String password);
        private native void native_pairing_cancel();

        PairingThread(String pairingCode) {
            super(TAG);
            mPairingCode = pairingCode;
        }

        @Override
        public void run() {
            boolean paired = native_pairing_start(mPairingCode);
            if (DEBUG) {
                if (mGuid != null && mName != null) {
                    Slog.i(TAG, "Pairing succeeded guid=" + mGuid + " name=" + mName);
                } else {
                    Slog.i(TAG, "Pairing failed");
                }
            }
            Bundle bundle = new Bundle();
            bundle.putString("guid", paired ? mGuid : null);
            bundle.putString("name", paired ? mName : null);
            Message message = Message.obtain(mHandler,
                                             WirelessDebuggingHandler.MSG_RESPONSE_PAIRING_RESULT,
                                             bundle);
            mHandler.sendMessage(message);
        }

        public void cancelPairing() {
            native_pairing_cancel();
        }
    }

    class WirelessDebuggingThread extends Thread {
        private boolean mStopped;
        private LocalSocket mSocket;
        private OutputStream mOutputStream;
        private InputStream mInputStream;

        WirelessDebuggingThread() {
            super(TAG);
        }

        @Override
        public void run() {
            while (true) {
                synchronized (this) {
                    if (mStopped) {
                        if (DEBUG) Slog.d(TAG, "Exiting thread");
                        return;
                    }
                    try {
                        openSocketLocked();
                    } catch (Exception e) {
                        /* Don't loop too fast if adbd dies, before init restarts it */
                        SystemClock.sleep(1000);
                        continue;
                    }
                }
                try {
                    listenToSocket();
                } catch (Exception e) {
                    /* Don't loop too fast if adbd dies, before init restarts it */
                    SystemClock.sleep(1000);
                }
            }
        }

        private void openSocketLocked() throws IOException {
            try {
                LocalSocketAddress address = new LocalSocketAddress(ADBDWIFI_SOCKET,
                        LocalSocketAddress.Namespace.RESERVED);
                mInputStream = null;

                if (DEBUG) Slog.d(TAG, "Creating socket");
                mSocket = new LocalSocket(LocalSocket.SOCKET_SEQPACKET);
                mSocket.connect(address);

                mOutputStream = mSocket.getOutputStream();
                mInputStream = mSocket.getInputStream();
            } catch (IOException ioe) {
                Slog.w(TAG, "Failed to connect to " + ADBDWIFI_SOCKET + " socket.");
                Slog.w(TAG, "Error (" + ioe + ")");
                closeSocketLocked();
                throw ioe;
            }
        }

        private void handleAdbdMsg(int count, byte[]  buffer, int op) {
            // Msg is in the following format:
            //  1) Opcode (2 bytes)
            //  2) <content>
            byte[] content = Arrays.copyOf(buffer, count);
            Message msg = mHandler.obtainMessage(op);
            msg.obj = content;
            mHandler.sendMessage(msg);
        }

        private class AdbdWifiMsg {
            public final String code;
            public final int msgInt;
            public AdbdWifiMsg(String c, int m) {
                code = c;
                msgInt = m;
            }
        }
        private final List<AdbdWifiMsg> mAdbdWifiMsgs = Collections.unmodifiableList(
                Arrays.asList(
                    new AdbdWifiMsg("PD", WirelessDebuggingHandler.MSG_RESPONSE_PAIRED_DEVICES),
                    new AdbdWifiMsg("PI", WirelessDebuggingHandler.MSG_RESPONSE_PAIRING_DEVICES),
                    new AdbdWifiMsg("OK", WirelessDebuggingHandler.MSG_RESULT_OK),
                    new AdbdWifiMsg("FA", WirelessDebuggingHandler.MSG_RESULT_FAILED),
                    new AdbdWifiMsg("CA", WirelessDebuggingHandler.MSG_RESULT_CANCELLED)));

        private void listenToSocket() throws IOException {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (true) {
                    int count = mInputStream.read(buffer);
                    if (count < 2) {
                        Slog.w(TAG, "Read failed with count " + count);
                        break;
                    }
                    if (DEBUG) {
                        Slog.i(TAG, "listenToSocket received " + count + " :"
                                + new String(Arrays.copyOfRange(buffer, 0, count)));
                    }
                    boolean handled = false;
                    for (AdbdWifiMsg adbdWifiMsg : mAdbdWifiMsgs) {
                        if (adbdWifiMsg.code.charAt(0) == buffer[0] &&
                            adbdWifiMsg.code.charAt(1) == buffer[1]) {
                            handleAdbdMsg(count, buffer, adbdWifiMsg.msgInt);
                            handled = true;
                            break;
                        }
                    }
                    if (!handled) {
                        Slog.e(TAG, "Unexpected operator: " + buffer[0]
                                + buffer[1]);
                        break;
                    }
                }
            } finally {
                synchronized (this) {
                    closeSocketLocked();
                }
            }
        }

        private void closeSocketLocked() {
            if (DEBUG) Slog.d(TAG, "Closing socket");
            try {
                if (mOutputStream != null) {
                    mOutputStream.close();
                    mOutputStream = null;
                }
            } catch (IOException e) {
                Slog.e(TAG, "Failed closing output stream: " + e);
            }

            try {
                if (mSocket != null) {
                    mSocket.close();
                    mSocket = null;
                }
            } catch (IOException ex) {
                Slog.e(TAG, "Failed closing socket: " + ex);
            }
        }

        /** Call to stop listening on the socket and exit the thread. */
        void stopListening() {
            synchronized (this) {
                mStopped = true;
                closeSocketLocked();
            }
        }

        void sendMessage(byte[] msg) {
            synchronized (this) {
                if (!mStopped && mOutputStream != null) {
                    try {
                        mOutputStream.write(msg);
                        mOutputStream.flush();
                    } catch (IOException ex) {
                        Slog.e(TAG, "Failed to write response:", ex);
                    }
                }
            }
        }
    }

    class WirelessDebuggingHandler extends Handler {
        // Ask adbd to enable adbdwifi
        private static final int MSG_ADBDWIFI_ENABLE = 1;
        // Ask adbd to disable adbdwifi
        private static final int MSG_ADBDWIFI_DISABLE = 2;
        // Ask adbd for the list of paired devices
        private static final int MSG_QUERY_PAIRED_DEVICES = 3;
        // Ask adbd for the list of pairing devices
        private static final int MSG_QUERY_PAIRING_DEVICES = 4;
        // TODO: Remove? // Adbd response for the list of paired devices
        private static final int MSG_RESPONSE_PAIRED_DEVICES = 5;
        // Adbd response for the list of pairing devices
        private static final int MSG_RESPONSE_PAIRING_DEVICES = 6;
        // Request adbd to pair a device
        private static final int MSG_REQ_PAIR = 7;
        // Request adbd to unpair a device
        private static final int MSG_REQ_UNPAIR = 8;
        // Request adbd to cancel an in-progress device pairing
        private static final int MSG_REQ_CANCEL_PAIRING = 9;
        // Status code from adbd response (ok)
        private static final int MSG_RESULT_OK = 10;
        // Status code from adbd response (failed)
        private static final int MSG_RESULT_FAILED = 11;
        // Status code from adbd response (cancelled)
        private static final int MSG_RESULT_CANCELLED = 12;
        // Response from the pairing
        private static final int MSG_RESPONSE_PAIRING_RESULT = 13;
        // Ask WirelessDebuggingManager to change the device name
        private static final int MSG_QUERY_SET_DEVICE_NAME = 14;
        // Enable discoverability mode
        private static final int MSG_QUERY_DISCOVER_ENABLE = 15;
        // Disable discoverability mode
        private static final int MSG_QUERY_DISCOVER_DISABLE = 16;

        WirelessDebuggingHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (DEBUG) Slog.i(TAG, "adb wireless handle msg " + msg.what);
            switch (msg.what) {
                case MSG_ADBDWIFI_ENABLE:
                    if (mAdbWirelessEnabled) {
                        break;
                    }

                    mAdbWirelessEnabled = true;
                    if (!native_keystore_init()) {
                        Slog.e(TAG, "Unable to initialize adbwifi keystore");
                        // TODO: Should we re-disable the UI?
                    }
                    // Send the paired devices list to the UI
                    refreshPairedDevicesList(mPairedDevices);
                    sendPairedDevicesToUI(mPairedDevices);
                    SystemProperties.set(WIRELESS_DEBUG_PERSISTENT_CONFIG_PROPERTY,
                            Boolean.toString(mAdbWirelessEnabled));

                    if (DEBUG) Slog.i(TAG, "adb start wireless adb mThread");
                    mThread = new WirelessDebuggingThread();
                    mThread.start();

                    break;
                case MSG_ADBDWIFI_DISABLE:
                    if (!mAdbWirelessEnabled) {
                        break;
                    }

                    mAdbWirelessEnabled = false;
                    SystemProperties.set(WIRELESS_DEBUG_PERSISTENT_CONFIG_PROPERTY,
                            Boolean.toString(mAdbWirelessEnabled));

                    if (mThread != null) {
                        mThread.stopListening();
                        mThread = null;
                    }
                    mPairedDevices.clear();
                    mPairingDevices.clear();
                    break;
                case MSG_QUERY_PAIRED_DEVICES:
                    sendPairedDevicesToUI(mPairedDevices);
                    break;
                case MSG_QUERY_PAIRING_DEVICES:
                    sendPairingDevicesToUI(mPairingDevices);
                    break;
                case MSG_RESPONSE_PAIRED_DEVICES:
                    parseDevices(new String((byte[]) msg.obj), true);
                    sendPairedDevicesToUI(mPairedDevices);
                    break;
                case MSG_RESPONSE_PAIRING_DEVICES:
                    parseDevices(new String((byte[]) msg.obj), false);
                    sendPairingDevicesToUI(mPairingDevices);
                    break;
                case MSG_REQ_PAIR: {
                    Bundle bundle = (Bundle) msg.obj;
                    String cmdStr = "PA"
                                  + bundle.getInt("pairMode") + "\n"
                                  + bundle.getString("guid") + "\n"
                                  + bundle.getString("code");
                    // TODO: Implement/use native code to send this since we
                    // are the only ones with write access to the keystore.
                    break;
                }
                case MSG_REQ_UNPAIR: {
                    // TODO: Use native code to remove from the keystore.
                    String guid = (String) msg.obj;
                    String cmdStr = "UP" + guid;
                    if (mThread != null) {
                        // Tell adbd to break the connection to this device if it's currently
                        // connected.
                        mThread.sendMessage(cmdStr.getBytes());
                    }
                    break;
                }
                case MSG_RESULT_OK:
                    updateUIResult(new String((byte[]) msg.obj), AdbManager.WIRELESS_STATUS_SUCCESS);
                    break;
                case MSG_RESULT_FAILED:
                    updateUIResult(new String((byte[]) msg.obj), AdbManager.WIRELESS_STATUS_FAIL);
                    break;
                case MSG_RESULT_CANCELLED:
                    updateUIResult(new String((byte[]) msg.obj), AdbManager.WIRELESS_STATUS_CANCELLED);
                    break;
                case MSG_RESPONSE_PAIRING_RESULT: {
                    Bundle bundle = (Bundle) msg.obj;
                    String guid = bundle.getString("guid");
                    String name = bundle.getString("name");
                    onPairingResult(guid, name);
                    // Send the updated paired devices list to the UI.
                    refreshPairedDevicesList(mPairedDevices);
                    sendPairedDevicesToUI(mPairedDevices);
                    break;
                }
                case MSG_QUERY_SET_DEVICE_NAME:
                    mDeviceName = new String((byte[]) msg.obj);
                    if (mThread != null) {
                        String cmdStr = "SN" + mDeviceName;
                        mThread.sendMessage(cmdStr.getBytes());
                    }
                    break;
                case MSG_QUERY_DISCOVER_ENABLE: {
                    String pairingCode = createPairingCode(PAIRING_CODE_LENGTH);
                    updateUIPairCode(pairingCode);
                    mPairingThread = new PairingThread(pairingCode);
                    mPairingThread.start();
                    break;
                }
                case MSG_REQ_CANCEL_PAIRING:
                case MSG_QUERY_DISCOVER_DISABLE:
                    if (mPairingThread != null) {
                        mPairingThread.cancelPairing();
                        try {
                            mPairingThread.join();
                        } catch (InterruptedException e) {
                            Slog.w(TAG, "Error while waiting for pairing thread to quit.");
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }

        private static final String ALPHA_NUMERICS = "0123456789";
        // Generates a random string of size |size|.
        private String createPairingCode(int size) {
            String res = "";
            SecureRandom rand = new SecureRandom();
            byte[] data = new byte[size];
            rand.nextBytes(data);
            for (byte a : data) {
                int idx = Byte.toUnsignedInt(a) % ALPHA_NUMERICS.length();
                res += ALPHA_NUMERICS.charAt(idx);
            }

            return res;
        }

        private void onPairingResult(String guid, String name) {
            if (guid == null || name == null) {
                Intent intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);
                intent.putExtra(AdbManager.WIRELESS_STATUS_EXTRA, AdbManager.WIRELESS_STATUS_FAIL);
                mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            } else {
                Intent intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);
                intent.putExtra(AdbManager.WIRELESS_STATUS_EXTRA, AdbManager.WIRELESS_STATUS_SUCCESS);
                PairDevice device = new PairDevice(guid, name, false);
                intent.putExtra(AdbManager.WIRELESS_PAIR_DEVICE_EXTRA, device);
                mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            }
        }

        private void parseDevices(String devices, boolean isPaired) {
            String[] token = devices.split("\n");
            if (token.length < 4 || token.length % 4 != 0) {
                Slog.e(TAG, "wrong format for devices: " + devices);
                return;
            }

            HashMap<String, PairDevice> deviceMap = isPaired ? mPairedDevices : mPairingDevices;
            deviceMap.clear();
            // Format: <guid>\n<shortName>\n<isConnected>
            for (int i = 0; i < token.length; ) {
                String guid = token[i++];
                String name = token[i++];
                int connected = 0;
                try {
                    connected = Integer.parseInt(token[i++]);
                } catch (NumberFormatException e) {
                    Slog.e(TAG, "wrong device connected msg");
                }
                deviceMap.put(guid, new PairDevice(name, guid, connected != 0));
            }
        }

        private void sendPairedDevicesToUI(HashMap<String, PairDevice> devices) {
            sendDevicesToUI(devices, true);
        }

        private void sendPairingDevicesToUI(HashMap<String, PairDevice> devices) {
            sendDevicesToUI(devices, false);
        }

        private void sendDevicesToUI(HashMap<String, PairDevice> devices, boolean isPairedDevices) {
            Intent intent = new Intent(isPairedDevices ? AdbManager.WIRELESS_DEBUG_PAIRED_DEVICES_ACTION
                                                       : AdbManager.WIRELESS_DEBUG_PAIRING_DEVICES_ACTION);
            intent.putExtra(AdbManager.WIRELESS_DEVICES_EXTRA, devices);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void updateUIResult(String content, int resultCode) {
            Intent intent;
            String[] token = content.split("\n");

            if ("PA".equals(token[0])) {
                intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);
            } else if ("UP".equals(token[0])) {
                intent = new Intent(AdbManager.WIRELESS_DEBUG_UNPAIRING_RESULT_ACTION);
            } else if ("ED".equals(token[0])) {
                intent = new Intent(AdbManager.WIRELESS_DEBUG_ENABLE_DISCOVER_ACTION);
                intent.putExtra(AdbManager.WIRELESS_STATUS_EXTRA, resultCode);
                mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                return;
            } else {
                Slog.e(TAG, "Unexpected result: " + content);
                return;
            }

            intent.putExtra(AdbManager.WIRELESS_STATUS_EXTRA, resultCode);
            String guid = token[1];
            if (!TextUtils.isEmpty(guid)) {
                PairDevice device = mPairedDevices.containsKey(guid)
                        ? mPairedDevices.get(guid) : mPairingDevices.get(guid);
                if (device == null) {
                    device = new PairDevice("", "", false);
                    Slog.e(TAG, "Pairing code came with device that left the network");
                }
                intent.putExtra(AdbManager.WIRELESS_PAIR_DEVICE_EXTRA, device);
            }
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            if (DEBUG) {
                Slog.i(TAG, "updateUIResult " + resultCode + " " + token[0] + " " + token[1]);
            }
        }

        private void updateUIPairCode(String code) {
            if (DEBUG) Slog.i(TAG, "updateUIPairCode: " + code);

            Intent intent = new Intent(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);
            intent.putExtra(AdbManager.WIRELESS_PAIRING_CODE_EXTRA, code);
            intent.putExtra(AdbManager.WIRELESS_STATUS_EXTRA,
                    AdbManager.WIRELESS_STATUS_PAIRING_CODE);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void refreshPairedDevicesList(HashMap<String, PairDevice> pairedDevices) {
            pairedDevices.clear();

            try {
                // Read the keystore data from file.
                byte[] data = Files.readAllBytes(Paths.get(ADBDWIFI_KEYS));
                if (data == null || data.length == 0) {
                    Slog.i(TAG, ADBDWIFI_KEYS + " doesn't exist or is empty");
                    return;
                }

                // Parse the keystore proto data and put it in pairedDevices
                KeyStore keyStore = KeyStore.parseFrom(data);
                List<Key> keys = keyStore.getKeysList();
                keys.forEach(key -> {
                    pairedDevices.put(key.getGuid(),
                                      new PairDevice(key.getName(), key.getGuid(), false));
                });
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
                Slog.i(TAG, "Failed to parse " + ADBDWIFI_KEYS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * check and enable wireless debugging when reboot
     */
    public void bootCompleted() {
        boolean enable = SystemProperties.getBoolean(
                WIRELESS_DEBUG_PERSISTENT_CONFIG_PROPERTY, false);
        enableAdbWireless(enable);
        if (DEBUG) Slog.i(TAG, "enable adb wireless on boot completion");
    }

    /**
     * When {@code enabled} is {@code true}, this allows wireless debugging and starts the
     * ADB hanler thread. When {@code enabled} is {@code false}, this disallows wireless
     * debugging and shuts down the handler thread.
     */
    public void enableAdbWireless(boolean enable) {
        if (enable) {
            mHandler.sendEmptyMessage(WirelessDebuggingHandler.MSG_ADBDWIFI_ENABLE);
        } else {
            mHandler.sendEmptyMessage(WirelessDebuggingHandler.MSG_ADBDWIFI_DISABLE);
        }
    }

    /**
     * UI queries the paired devices.
     */
    public void queryPairedDevices() {
        mHandler.sendEmptyMessage(WirelessDebuggingHandler.MSG_QUERY_PAIRED_DEVICES);
    }

    /**
     * UI queries the pairing devices.
     */
    public void queryPairingDevices() {
        mHandler.sendEmptyMessage(WirelessDebuggingHandler.MSG_QUERY_PAIRING_DEVICES);
    }

    /**
     * Pair with device
     */
    public void pairDevice(int pairMode, String guid, String code) {
        Bundle bundle = new Bundle();
        bundle.putInt("pairMode", pairMode);
        bundle.putString("guid", guid);
        bundle.putString("code", code == null ? "" : code);
        Message message = Message.obtain(mHandler,
                                         WirelessDebuggingHandler.MSG_REQ_PAIR,
                                         bundle);
        mHandler.sendMessage(message);
    }

    /**
     * Unpair with device
     */
    public void unpairDevice(String guid) {
        Message message = Message.obtain(mHandler,
                                         WirelessDebuggingHandler.MSG_REQ_UNPAIR,
                                         guid);
        mHandler.sendMessage(message);
    }

    /**
     * Cancel pairing with device
     */
    public void cancelPairing(int pairMode, String guid) {
        Bundle bundle = new Bundle();
        bundle.putInt("pairMode", pairMode);
        bundle.putString("guid", guid);
        Message message = Message.obtain(mHandler,
                                         WirelessDebuggingHandler.MSG_REQ_CANCEL_PAIRING,
                                         bundle);
        mHandler.sendMessage(message);
    }

    /**
     * Status enabled/disabled check
     */
    public boolean isEnabled() {
        return mAdbWirelessEnabled;
    }

    /**
     * Get the device name
     */
    public String getName() {
        return mDeviceName;
    }

    /**
     * Set the device name
     */
    public void setName(String name) {
        Message message = Message.obtain(mHandler,
                                         WirelessDebuggingHandler.MSG_QUERY_SET_DEVICE_NAME,
                                         0, 0, name);
        mHandler.sendMessage(message);
    }

    /**
     * set adbd in discover state
     */
    public void setDiscoverable(int pairMode, boolean enable) {
        if (enable) {
            Message message = Message.obtain(mHandler,
                                             WirelessDebuggingHandler
                                             .MSG_QUERY_DISCOVER_ENABLE,
                                             pairMode, 0);
            mHandler.sendMessage(message);
        } else {
            Message message = Message.obtain(mHandler,
                                             WirelessDebuggingHandler
                                             .MSG_QUERY_DISCOVER_DISABLE,
                                             pairMode, 0);
            mHandler.sendMessage(message);
        }
    }
}
