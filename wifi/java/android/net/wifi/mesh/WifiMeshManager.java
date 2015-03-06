/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.net.wifi.mesh;

import java.util.HashMap;

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.util.AsyncChannel;
import com.android.internal.util.Protocol;

/**
 * This class provides the API for managing Wi-Fi mesh connectivity. This lets an
 * application discover available mesh peers, join the mesh group.
 *
 * <p> The API is asynchronous and responses to requests from an application are on listener
 * callbacks provided by the application. The application needs to do an initialization with
 * {@link #initialize} before doing any mesh operation.
 *
 * <p> Most application calls need a {@link ActionListener} instance for receiving callbacks
 * {@link ActionListener#onSuccess} or {@link ActionListener#onFailure}. Action callbacks
 * indicate whether the initiation of the action was a success or a failure.
 * Upon failure, the reason of failure can be one of {@link #ERROR}, {@link #MESH_UNSUPPORTED}
 * or {@link #BUSY}.
 */
public class WifiMeshManager {

    private static final String TAG = "WifiMeshManager";

    /**
     * Broadcast intent action to indicate whether Wi-Fi mesh is enabled or disabled. An
     * extra {@link #EXTRA_WIFI_MESH_STATE} provides the state information as int.
     *
     * @see #EXTRA_WIFI_MESH_STATE
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String WIFI_MESH_STATE_CHANGED_ACTION =
        "android.net.wifi.mesh.STATE_CHANGED";

    /**
     * An access point scan has completed, and results are available from the supplicant.
     * Call {@link #requestScanResults(Channel, ScanResultsListener)}
     * to obtain the results.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String WIFI_MESH_SCAN_RESULTS_AVAILABLE_ACTION =
        "android.net.wifi.mesh.SCAN_RESULTS_AVAILABLE";

    /**
     * Broadcast intent action indicating that remembered group configurations have
     * become available or changed.
     * Call {@link #requestGroupConfigs(Channel, GroupConfigsListenrer)}
     * to obtain the group configurations.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String WIFI_MESH_GROUP_CONFIG_AVAILABLE_ACTION =
        "android.net.wifi.mesh.GROUP_CONFIG_AVAILABLE";

    /**
     * Broadcast intent action indicating that a new peer link has been established.
     * An extra {@link #EXTRA_WIFI_MESH_ADDR} provides the peer mac address as String.
     *
     * @see #EXTRA_WIFI_MESH_ADDR
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String WIFI_MESH_PEER_CONNECTED_ACTION =
        "android.net.wifi.mesh.PEER_CONNECTED";

    /**
     * Broadcast intent action indicating that a peer link has been disconnected.
     * An extra {@link #EXTRA_WIFI_MESH_ADDR} provides the peer mac address as String.
     *
     * @see #EXTRA_WIFI_MESH_ADDR
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String WIFI_MESH_PEER_DISCONNECTED_ACTION =
        "android.net.wifi.mesh.PEER_DISCONNECTED";

    /**
     * Broadcast intent action indicating that the state of Wi-Fi mesh connectivity
     * has changed.
     * An extra {@link #EXTRA_WIFI_MESH_GROUP} provides the mesh group information as
     * {@link android.net.wifi.mesh.WifiMeshGroup}.
     * If it's null, it means you left the group.
     *
     * @see #EXTRA_WIFI_MESH_GROUP
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String WIFI_MESH_CONNECTION_CHANGED_ACTION =
        "android.net.wifi.mesh.CONNECTION_STATE_CHANGE";

    /**
     * The lookup key for an int that indicates whether Wi-Fi mesh is enabled or disabled.
     * Retrieve it with {@link android.content.Intent#getIntExtra(String,int)}.
     *
     * @see #WIFI_MESH_STATE_DISABLING
     * @see #WIFI_MESH_STATE_DISABLED
     * @see #WIFI_MESH_STATE_ENABLING
     * @see #WIFI_MESH_STATE_ENABLED
     */
    public static final String EXTRA_WIFI_MESH_STATE = "wifi_mesh_state";

    /**
     * The lookup key for an String that indicates the mesh node address.
     * Retrieve it with {@link android.content.Intent#getStringExtra(String)}.
     */
    public static final String EXTRA_WIFI_MESH_ADDR = "wifi_mesh_addr";

    /**
     * The lookup key for {@link android.net.wifi.mesh.WifiMeshGroup} that
     * indicates the mesh group.
     * Retrieve it with {@link android.content.Intent#getParcelableExtra(String)}.
     */
    public static final String EXTRA_WIFI_MESH_GROUP = "wifi_mesh_group";

    /**
     * Wi-Fi mesh is currently being disabled. The state will change to
     * {@link #WIFI_MESH_STATE_DISABLED} if it finishes successfully.
     *
     * @see #WIFI_MESH_STATE_CHANGED_ACTION
     */
    public static final int WIFI_MESH_STATE_DISABLING = 1;

    /**
     * Wi-Fi mesh is disabled.
     *
     * @see #WIFI_MESH_STATE_CHANGED_ACTION
     */
    public static final int WIFI_MESH_STATE_DISABLED = 2;

    /**
     * Wi-Fi mesh is currently being enabled. The state will change to
     * {@link #WIFI_MESH_STATE_ENABLED} if it finishes successfully.
     *
     * @see #WIFI_MESH_STATE_CHANGED_ACTION
     */
    public static final int WIFI_MESH_STATE_ENABLING = 3;

    /**
     * Wi-Fi mesh is enabled.
     *
     * @see #WIFI_MESH_STATE_CHANGED_ACTION
     */
    public static final int WIFI_MESH_STATE_ENABLED = 4;

    /**
     * Wi-Fi mesh is in a failed state. This state will occur when an error occurs during
     * enabling or disabling
     *
     * @see #WIFI_MESH_STATE_CHANGED_ACTION
     */
    public static final int WIFI_MESH_STATE_FAILED = 5;

    IWifiMeshManager mService;

    private static final int BASE = Protocol.BASE_WIFI_MESH_MANAGER;

    /** @hide */
    public static final int SCAN                          = BASE + 1;
    /** @hide */
    public static final int SCAN_FAILED                   = BASE + 2;
    /** @hide */
    public static final int SCAN_SUCCEEDED                = BASE + 3;

    /** @hide */
    public static final int CONNECT_NETWORK               = BASE + 3;
    /** @hide */
    public static final int CONNECT_NETWORK_FAILED        = BASE + 4;
    /** @hide */
    public static final int CONNECT_NETWORK_SUCCEEDED     = BASE + 5;

    /** @hide */
    public static final int DISCONNECT_NETWORK            = BASE + 6;
    /** @hide */
    public static final int DISCONNECT_NETWORK_FAILED     = BASE + 7;
    /** @hide */
    public static final int DISCONNECT_NETWORK_SUCCEEDED  = BASE + 8;

    /** @hide */
    public static final int FORGET_NETWORK                = BASE + 9;
    /** @hide */
    public static final int FORGET_NETWORK_FAILED         = BASE + 10;
    /** @hide */
    public static final int FORGET_NETWORK_SUCCEEDED      = BASE + 11;

    /** @hide */
    public static final int REQUEST_SCAN_RESULTS          = BASE + 12;
    /** @hide */
    public static final int RESPONSE_SCAN_RESULTS         = BASE + 13;

    /** @hide */
    public static final int REQUEST_GROUP_CONFIGS         = BASE + 14;
    /** @hide */
    public static final int RESPONSE_GROUP_CONFIGS        = BASE + 15;

    /**
     * Create a new WifiMeshManager instance. Applications use
     * {@link android.content.Context#getSystemService Context.getSystemService()} to retrieve
     * the standard {@link android.content.Context#WIFI_MESH_SERVICE Context.WIFI_MESH_SERVICE}.
     * @param service the Binder interface
     * @hide - hide this because it takes in a parameter of type IWifiMeshManager, which
     * is a system private class.
     */
    public WifiMeshManager(IWifiMeshManager service) {
        mService = service;
    }

    /**
     * Passed with {@link ActionListener#onFailure}.
     * Indicates that the operation failed due to an internal error.
     */
    public static final int ERROR               = 0;

    /**
     * Passed with {@link ActionListener#onFailure}.
     * Indicates that the operation failed because mesh is unsupported on the device.
     */
    public static final int MESH_UNSUPPORTED     = 1;

    /**
     * Passed with {@link ActionListener#onFailure}.
     * Indicates that the operation failed because the framework is busy and
     * unable to service the request
     */
    public static final int BUSY                = 2;

    /** Interface for callback invocation when framework channel is lost */
    public interface ChannelListener {
        /**
         * The channel to the framework has been disconnected.
         * Application could try re-initializing using {@link #initialize}
         */
        public void onChannelDisconnected();
    }

    /** Interface for callback invocation on an application action */
    public interface ActionListener {
        /** The operation succeeded */
        public void onSuccess();
        /**
         * The operation failed
         * @param reason The reason for failure could be one of {@link #MESH_UNSUPPORTED},
         * {@link #ERROR} or {@link #BUSY}
         */
        public void onFailure(int reason);
    }

    /** Interface for callback invocation when peer list is available */
    public interface ScanResultsListener {
        /**
         * The requested scan list is available
         * @param results List of available mesh BSSs
         */
        public void onScanResuls(WifiMeshScanResults results);
    }

    /** Interface for callback invocation when network profile list is
     * available */
    public interface GroupConfigsListenrer {
        /**
         * The requested mesh config group list is available
         * @param list List of available mesh config groups
         */
        public void onGroupConfigsAvailable(WifiMeshGroupList list);
    }

    /**
     * A channel that connects the application to the Wifi mesh framework.
     * Most mesh operations require a Channel as an argument. An instance of Channel is obtained
     * by doing a call on {@link #initialize}
     */
    public static class Channel {
        Channel(Context context, Looper looper, ChannelListener l) {
            mAsyncChannel = new AsyncChannel();
            mHandler = new MeshHandler(looper);
            mChannelListener = l;
            mContext = context;
        }
        private final static int INVALID_LISTENER_KEY = 0;
        private ChannelListener mChannelListener;
        private final HashMap<Integer, Object> mListenerMap = new HashMap<Integer, Object>();
        private final Object mListenerMapLock = new Object();
        private int mListenerKey = 0;

        private final AsyncChannel mAsyncChannel;
        private final MeshHandler mHandler;
        Context mContext;
        class MeshHandler extends Handler {
            MeshHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message message) {
                Object listener = getListener(message.arg2);
                switch (message.what) {
                    case AsyncChannel.CMD_CHANNEL_DISCONNECTED:
                        if (mChannelListener != null) {
                            mChannelListener.onChannelDisconnected();
                            mChannelListener = null;
                        }
                        break;
                    /* ActionListeners grouped together */
                    case SCAN_FAILED:
                    case CONNECT_NETWORK_FAILED:
                    case DISCONNECT_NETWORK_FAILED:
                    case FORGET_NETWORK_FAILED:
                        if (listener != null) {
                            ((ActionListener) listener).onFailure(message.arg1);
                        }
                        break;
                    /* ActionListeners grouped together */
                    case SCAN_SUCCEEDED:
                    case CONNECT_NETWORK_SUCCEEDED:
                    case DISCONNECT_NETWORK_SUCCEEDED:
                    case FORGET_NETWORK_SUCCEEDED:
                        if (listener != null) {
                            ((ActionListener) listener).onSuccess();
                        }
                        break;
                    case RESPONSE_SCAN_RESULTS:
                        if (listener != null) {
                            ((ScanResultsListener) listener).
                            onScanResuls((WifiMeshScanResults)message.obj);
                        }
                        break;
                    case RESPONSE_GROUP_CONFIGS:
                        if (listener != null) {
                            ((GroupConfigsListenrer) listener).
                            onGroupConfigsAvailable((WifiMeshGroupList)message.obj);
                        }
                        break;
                   default:
                        Log.d(TAG, "Ignored " + message);
                        break;
                }
            }
        }

        private int putListener(Object listener) {
            if (listener == null) return INVALID_LISTENER_KEY;
            int key;
            synchronized (mListenerMapLock) {
                do {
                    key = mListenerKey++;
                } while (key == INVALID_LISTENER_KEY);
                mListenerMap.put(key, listener);
            }
            return key;
        }

        private Object getListener(int key) {
            if (key == INVALID_LISTENER_KEY) return null;
            synchronized (mListenerMapLock) {
                return mListenerMap.remove(key);
            }
        }
    }

    private static void checkChannel(Channel c) {
        if (c == null) throw new IllegalArgumentException("Channel needs to be initialized");
    }

    /**
     * Registers the application with the Wi-Fi framework. This function
     * must be the first to be called before any mesh operations are performed.
     *
     * @param srcContext is the context of the source
     * @param srcLooper is the Looper on which the callbacks are received
     * @param listener for callback at loss of framework communication. Can be null.
     * @return Channel instance that is necessary for performing any further mesh operations
     */
    public Channel initialize(Context srcContext, Looper srcLooper, ChannelListener listener) {
        Messenger messenger = getMessenger();
        if (messenger == null) return null;

        Channel c = new Channel(srcContext, srcLooper, listener);
        if (c.mAsyncChannel.connectSync(srcContext, c.mHandler, messenger)
                == AsyncChannel.STATUS_SUCCESSFUL) {
            return c;
        } else {
            return null;
        }
    }

    /**
     * Initiate mesh peer discovery. A discovery process involves scanning for
     * available Wi-Fi mesh peers for the purpose of establishing a connection.
     *
     * <p> The function call immediately returns after sending a discovery request
     * to the framework. The application is notified of a success or failure to initiate
     * discovery through listener callbacks {@link ActionListener#onSuccess} or
     * {@link ActionListener#onFailure}.
     *
     * <p> Upon receiving a {@link #WIFI_MESH_SCAN_RESULTS_AVAILABLE_ACTION} intent,
     * an application can request for the list of mesh peers using
     *  {@link #requestScanResults(Channel, ScanResultsListener)}.
     *
     * @param c is the channel created at {@link #initialize}
     * @param listener for callbacks on success or failure. Can be null.
     */
    public void scan(Channel c, ActionListener listener) {
        checkChannel(c);
        Log.i(TAG, "call scan()");
        c.mAsyncChannel.sendMessage(SCAN, 0, c.putListener(listener));
    }

    /**
     * Request the current mesh scan list.
     * The mesh scan list can be retrieved through the callback function.
     *
     * @param c is the channel created at {@link #initialize}
     * @param listener for callback when scan list is available. Can be null.
     */
    public void requestScanResults(Channel c, ScanResultsListener listener) {
        checkChannel(c);
        Log.i(TAG, "call requestScanResults()");
        c.mAsyncChannel.sendMessage(REQUEST_SCAN_RESULTS, 0, c.putListener(listener));
    }

    /**
     * Request the remembered mesh configuration groups.
     * The mesh configuration groups can be retrieved through the callback function.
     *
     * @param c is the channel created at {@link #initialize}
     * @param listener for callback when scan list is available. Can be null.
     */
    public void requestGroupConfigs(Channel c, GroupConfigsListenrer listener) {
        checkChannel(c);
        Log.i(TAG, "call requestGroupConfigs()");
        c.mAsyncChannel.sendMessage(REQUEST_GROUP_CONFIGS, 0,
                c.putListener(listener));
    }

    /**
     * Connect to a network with the given configuration. The network also
     * gets added to the supplicant configuration if success.
     *
     * @param c is the channel created at {@link #initialize}
     * @param config the set of variables that describe the configuration,
     *            contained in a {@link WifiMeshGroup} object.
     * @param listener for callbacks on success or failure. Can be null.
     * @throws IllegalStateException if the WifiManager instance needs to be
     * initialized again
     */
    public void connect(Channel c, WifiMeshGroup config,
            ActionListener listener) {
        checkChannel(c);
        // Use INVALID_NETWORK_ID for arg1 when passing a config object
        // arg1 is used to pass network id when the network already exists
        Log.i(TAG, "call connect() " + config.toString());
        c.mAsyncChannel.sendMessage(CONNECT_NETWORK,
                WifiMeshGroup.INVALID_NETWORK_ID, c.putListener(listener),
                config);
    }

    /**
     * Connect to a network with the given networkId.
     *
     * The network id can be retrieved in {@link WifiMeshGroup} obtained by
     * {@link #requestGroupConfigs}
     *
     * @param c is the channel created at {@link #initialize}
     * @param networkId the network id identifying the network in the
     *                supplicant configuration list
     * @param listener for callbacks on success or failure. Can be null.
     * @throws IllegalStateException if the WifiMeshManager instance needs to be
     * initialized again
     */
    public void connect(Channel c, int networkId, ActionListener listener) {
        checkChannel(c);
        Log.i(TAG, "call connect() " + networkId);
        c.mAsyncChannel.sendMessage(CONNECT_NETWORK, networkId,
                c.putListener(listener));
    }

    /**
     * Leave from the joined mesh group.
     *
     * @param c is the channel created at {@link #initialize}
     * @param listener for callbacks on success or failure. Can be null.
     */
    public void disconnect(Channel c, ActionListener listener) {
        checkChannel(c);
        Log.i(TAG, "call disconnect()");
        c.mAsyncChannel.sendMessage(DISCONNECT_NETWORK, 0,
                c.putListener(listener));
    }

    /**
     * Delete the network in the supplicant config.
     *
     * The network id can be retrieved in {@link WifiMeshGroup} obtained by
     * {@link #requestGroupConfigs}
     *
     * @param c is the channel created at {@link #initialize}
     * @param networkId the network id identifying the network in the
     *                supplicant configuration list
     * @param listener for callbacks on success or failure. Can be null.
     * @throws IllegalStateException if the WifiMeshManager instance needs to be
     * initialized again
     * @hide
     */
    public void forget(Channel c, int networkId, ActionListener listener) {
        checkChannel(c);
        Log.i(TAG, "call forget() " + networkId);
        c.mAsyncChannel.sendMessage(FORGET_NETWORK, networkId,
                c.putListener(listener));
    }

    /**
     * Get a reference to WifiMeshService handler. This is used to establish
     * an AsyncChannel communication with WifiService
     *
     * @return Messenger pointing to the WifiMeshService handler
     * @hide
     */
    public Messenger getMessenger() {
        try {
            return mService.getMessenger();
        } catch (RemoteException e) {
            return null;
        }
    }
}
