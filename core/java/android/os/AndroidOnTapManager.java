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
package android.os;

import android.annotation.SystemApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Slog;

import java.lang.ref.WeakReference;


/**
 * This class contains methods and constants used to start AndroidOnTap
 * installation, and a listener for progress update.
 * @hide
 */
@SystemApi
public class AndroidOnTapManager {

    private static final String TAG = "AotManager";

    private static final long DEFAULT_USERDATA_SIZE = (10L << 30);


    /** Listener for installation status update. */
    public interface OnStatusChangedListener {
        /**
         * This callback is called when installation status is changed, and when the
         * client is {@link #bind} to AndroidOnTap installation service.
         *
         * @param status status code, also defined in {@code AndroidOnTapManager}.
         * @param cause cause code, also defined in {@code AndroidOnTapManager}.
         * @param progress number of bytes installed.
         */
        void onStatusChanged(int status, int cause, long progress);
    }

    /*
     * Status codes
     */
    /** We are not bound to AndroidOnTapService. */
    public static final int STATUS_NOT_BOUND = 0;

    /** Installation is not started yet. */
    public static final int STATUS_UNINIT = 1;

    /** Installation is in progress. */
    public static final int STATUS_IN_PROGRESS = 2;

    /** Installation is finished but the user has not launched it. */
    public static final int STATUS_READY = 3;

    /** Device is running in AOT. */
    public static final int STATUS_IN_USE = 4;

    /*
     * Causes
     */
    /** Cause is not specified. This means the status is not changed. */
    public static final int CAUSE_NOT_SPECIFIED = 0;

    /** Status changed because installation is completed. */
    public static final int CAUSE_INSTALL_COMPLETED = 1;

    /** Status changed because installation is cancelled. */
    public static final int CAUSE_INSTALL_CANCELLED = 2;

    /** Installation failed due to IOException. */
    public static final int CAUSE_ERROR_IO = 3;

    /** Installation failed because the image URL source is not supported. */
    public static final int CAUSE_ERROR_INVALID_URL = 4;

    /** Installation failed due to IPC error. */
    public static final int CAUSE_ERROR_IPC = 5;

    /** Installation failed due to unhandled exception. */
    public static final int CAUSE_ERROR_EXCEPTION = 6;

    /*
     * IPC Messages
     */
    /** Message to register listener. */
    public static final int MSG_REGISTER_LISTENER = 1;

    /** Message to unregister listener. */
    public static final int MSG_UNREGISTER_LISTENER = 2;

    /** Message for status update. */
    public static final int MSG_POST_STATUS = 3;

    /*
     * Messages keys
     */
    /** Message key, for progress update. */
    public static final String KEY_INSTALLED_SIZE = "KEY_INSTALLED_SIZE";

    /*
     * Intent Actions
     */
    /** Intent action: start AOT installation. */
    public static final String ACTION_START_INSTALL =
            "android.os.androidontap.action.START_INSTALL";

    /** Intent action: notify user if we are currently running in AOT. */
    public static final String ACTION_NOTIFY_IF_IN_USE =
            "android.os.androidontap.action.NOTIFY_IF_IN_USE";

    /*
     * Intent Keys
     */
    /** Intent key: URL to system image. */
    public static final String KEY_SYSTEM_URL = "KEY_SYSTEM_URL";

    /** Intent key: Size of system image, in bytes. */
    public static final String KEY_SYSTEM_SIZE = "KEY_SYSTEM_SIZE";

    /** Intent key: Number of bytes to reserve for userdata. */
    public static final String KEY_USERDATA_SIZE = "KEY_USERDATA_SIZE";


    private static class IncomingHandler extends Handler {
        private final WeakReference<AndroidOnTapManager> mWeakManager;

        IncomingHandler(AndroidOnTapManager service) {
            mWeakManager = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            AndroidOnTapManager service = mWeakManager.get();

            if (service != null) {
                service.handleMessage(msg);
            }
        }
    }

    private class AndroidOnTapServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Slog.i(TAG, "AndroidOnTapService connected");

            mService = new Messenger(service);

            try {
                Message msg = Message.obtain(null, MSG_REGISTER_LISTENER);
                msg.replyTo = mMessenger;

                mService.send(msg);
            } catch (RemoteException e) {
                mListener.onStatusChanged(STATUS_NOT_BOUND, CAUSE_ERROR_IPC, 0);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Slog.i(TAG, "AndroidOnTapService disconnected");
            mService = null;
        }
    }

    private final Context mContext;
    private final OnStatusChangedListener mListener;
    private final AndroidOnTapServiceConnection mConnection;
    private final Messenger mMessenger;

    private boolean mBound;
    private Messenger mService;


    public AndroidOnTapManager(Context context, OnStatusChangedListener listener) {
        mContext = context;
        mListener = listener;
        mConnection = new AndroidOnTapServiceConnection();
        mMessenger = new Messenger(new IncomingHandler(this));
    }

    /**
     * Bind to AndroidOnTapInstallationService.
     */
    public void bind() {
        Intent intent = new Intent();
        intent.setClassName("com.android.androidontap",
                "com.android.androidontap.AndroidOnTapInstallationService");

        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        mBound = true;
    }

    /**
     * Unbind from AndroidOnTapInstallationService.
     */
    public void unbind() {
        if (!mBound) {
            return;
        }

        if (mService != null) {
            try {
                Message msg = Message.obtain(null, MSG_UNREGISTER_LISTENER);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // do nothing
            }
        }

        // Detach our existing connection.
        mContext.unbindService(mConnection);

        mBound = false;
    }

    /**
     * Start installing AndroidOnTap from URL with default userdata size.
     *
     * @param systemUrl URL to system image file.
     * @param systemSize size of system image.
     */
    public void start(String systemUrl, long systemSize) {
        start(systemUrl, systemSize, DEFAULT_USERDATA_SIZE);
    }

    /**
     * Start installing AndroidOnTap from URL.
     *
     * @param systemUrl URL to system image file.
     * @param systemSize size of system image.
     * @param userdataSize bytes reserved for userdata.
     */
    public void start(String systemUrl, long systemSize, long userdataSize) {
        Intent intent = new Intent();

        intent.setClassName("com.android.androidontap",
                "com.android.androidontap.VerificationActivity");

        intent.setAction(ACTION_START_INSTALL);

        intent.putExtra(KEY_SYSTEM_URL, systemUrl);
        intent.putExtra(KEY_SYSTEM_SIZE, systemSize);
        intent.putExtra(KEY_USERDATA_SIZE, userdataSize);

        mContext.startActivity(intent);
    }

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_POST_STATUS:
                int status = msg.arg1;
                int cause = msg.arg2;
                // obj is non-null
                long progress = ((Bundle) msg.obj).getLong(KEY_INSTALLED_SIZE);

                mListener.onStatusChanged(status, cause, progress);

                break;
            default:
                // do nothing

        }
    }
}
