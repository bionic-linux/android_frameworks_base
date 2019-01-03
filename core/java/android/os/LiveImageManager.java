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
import android.net.Uri;
import android.util.Slog;

import java.lang.ref.WeakReference;


/** The wrapper class for LiveImageManager */
/** @hide */
@SystemApi
public class LiveImageManager {

    private static final String TAG = "LiveImage";

    private static final long DEFAULT_USERDATA_SIZE = (10L << 30);


    /**
     * LiveImageService on status changed listener.
     */
    public interface OnStatusChangedListener {
        /**
         * Callback when service status changed.
         */
        void onStatusChanged(int status, int cause, long progress);
    }

    /*
     * Status codes
     */
    /** We are not bound to LiveImageService, this is an error status */
    public static final int STATUS_NOT_BOUND = 0;

    /** The live image is not started yet. */
    public static final int STATUS_UNINIT = 1;

    /** Setup in progress. */
    public static final int STATUS_IN_PROGRESS = 2;

    /** The setup is finished but user has not launched it. */
    public static final int STATUS_READY = 3;

    /** Device is running a live image. */
    public static final int STATUS_IN_USE = 4;

    /*
     * Causes
     */
    public static final int CAUSE_NOT_SPECIFIED = 0;
    public static final int CAUSE_INSTALL_COMPLETED = 1;
    public static final int CAUSE_INSTALL_CANCELLED = 2;
    public static final int CAUSE_ERROR_FILE_NOT_FOUND = 3;
    public static final int CAUSE_ERROR_IO = 4;
    public static final int CAUSE_ERROR_NETWORK = 5;
    public static final int CAUSE_ERROR_UNSUPPORTED_IMAGE_SOURCE = 6;
    public static final int CAUSE_ERROR_IPC = 7;
    public static final int CAUSE_ERROR_EXCEPTION = 8;

    /*
     * IPC Messages
     */
    public static final int MSG_REGISTER_LISTENER = 1;
    public static final int MSG_UNREGISTER_LISTENER = 2;
    public static final int MSG_POST_STATUS = 3;

    /*
     * Intent Keys
     */
    public static final String KEY_SYSTEM_SIZE = "KEY_SYSTEM_SIZE";
    public static final String KEY_USERDATA_SIZE = "KEY_USERDATA_SIZE";
    public static final String KEY_INSTALLED_SIZE = "KEY_INSTALLED_SIZE";


    private static class IncomingHandler extends Handler {
        private final WeakReference<LiveImageManager> mWeakManager;

        IncomingHandler(LiveImageManager service) {
            mWeakManager = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            LiveImageManager service = mWeakManager.get();

            if (service != null) {
                service.handleMessage(msg);
            }
        }
    }

    private class LiveImageServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Slog.i(TAG, "LiveImageService connected");

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
            Slog.i(TAG, "LiveImageService disconnected");
            mService = null;
        }
    }

    private final Context mContext;
    private final OnStatusChangedListener mListener;
    private final LiveImageServiceConnection mConnection;
    private final Messenger mMessenger;

    private boolean mBound;
    private Messenger mService;


    public LiveImageManager(Context context, OnStatusChangedListener listener) {
        mContext = context;
        mListener = listener;
        mConnection = new LiveImageServiceConnection();
        mMessenger = new Messenger(new IncomingHandler(this));
    }

    /**
     * Bind to LiveImageService
     */
    public void bind() {
        Intent intent = new Intent();
        intent.setClassName("com.android.liveimage", "com.android.liveimage.LiveImageService");

        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        mBound = true;
    }

    /**
     * Unbind from LiveImageService
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
     * Start installing LiveImage from URI, with default userdata size
     */
    public void start(Uri uri, long systemSize) {
        start(uri, systemSize, DEFAULT_USERDATA_SIZE);
    }

    /**
     * Start installing LiveImage from URI
     */
    public void start(Uri uri, long systemSize, long userdataSize) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_LIVEIMAGE);

        intent.setData(uri);
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
