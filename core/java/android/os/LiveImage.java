/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.util.Slog;

/** The wrapper class for LiveImageManager */
@SystemApi
public class LiveImage {
    private static final String TAG = "LIVEIMAGE";

    public static enum Status {
        /** The live image is not started yet. */
        UNINIT,
        /** Setup in progress. */
        INPROGRESS,
        /** The setup is finished but user has not launched it. */
        READY,
        /** Device is running a live image. */
        INUSE
    };

    public static enum MessageType {
        STATUS,
        ERROR
    };

    public interface Callback {
        public void onStatusChanged(Status status);

        public void onError(Exception exception);
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (MessageType.values()[msg.what]) {
                case STATUS:
                    Status status = Status.values()[msg.arg1];
                    LiveImage.this.mCallback.onStatusChanged(status);
                    break;
                case ERROR:
                    String errmsg = new String(msg.getData().getByteArray("error"));
                    LiveImage.this.mCallback.onError(new Exception(errmsg));
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class LiveImageServiceConnection implements ServiceConnection {

        public void onServiceConnected(ComponentName className, IBinder service) {

            Slog.e(TAG, "LiveImageService connected");
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, 0);
                msg.replyTo = new Messenger(new IncomingHandler());
                mService.send(msg);
            } catch (RemoteException e) {
                mCallback.onError(e);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            Slog.e(TAG, "LiveImageService disconnected");
        }
    }

    private Callback mCallback;
    private Messenger mService;

    public LiveImage(Callback callback) {
        mCallback = callback;
        mService = null;
    }

    public void start(Activity activity, Uri uri, long size) throws Exception {
        activity.bindService(
                new Intent(Intent.ACTION_LIVE_IMAGE_STARTED, uri).putExtra("size", size),
                new LiveImageServiceConnection(),
                Context.BIND_AUTO_CREATE);
    }
}
