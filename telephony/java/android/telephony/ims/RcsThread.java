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

package android.telephony.ims;

import android.annotation.WorkerThread;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ims.aidl.IRcs;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

/**
 * RcsThread represents a single RCS conversation thread. It holds messages that were sent and
 * received and events that occurred on that thread.
 *
 * @hide - TODO(109759350) make this public
 * TODO(109759350) add exceptions and roll back local changes if RPC fails
 */
public abstract class RcsThread implements Parcelable {
    // Since this is an abstract class that gets parcelled, the sub-classes need to write these
    // magic values into the parcel so that we know which type to unparcel into. These are defined
    // as integer instead of boolean for extensibility.
    protected static final int RCS_1_TO_1_TYPE = 998;
    protected static final int RCS_GROUP_TYPE = 999;

    protected int mThreadId;

    protected RcsThread(int threadId) {
        mThreadId = threadId;
    }

    protected RcsThread(Parcel in) {
        mThreadId = in.readInt();
    }

    /**
     * @return Returns the number of {@link RcsMessage}s that exist in this RcsThread.
     */
    public int getMessageCount() {
        // TODO (109759350) - implement
        return 0;
    }

    /**
     * Adds a new {@link RcsMessage} to this RcsThread and persists it in storage.
     *
     * @param rcsMessage The message to add to the thread
     */
    @WorkerThread
    public void addMessage(RcsMessage rcsMessage) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.addMessage(rcsMessage, mThreadId);
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsThread: Exception happened during addMessage: ", re);
        }
    }

    /**
     * Deletes an {@link RcsMessage} from this RcsThread and updates the storage.
     *
     * @param rcsMessage The message to delete from the thread
     */
    @WorkerThread
    public void deleteMessage(RcsMessage rcsMessage) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.deleteMessage(rcsMessage.getId(), rcsMessage.isIncoming(), mThreadId,
                        isGroup());
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsThread: Exception happened during deleteMessage: ", re);
        }
    }

    /**
     * @return Returns whether this is a group thread or not
     */
    public abstract boolean isGroup();

    /**
     * @hide
     */
    @VisibleForTesting
    public int getThreadId() {
        return mThreadId;
    }

    public static final Creator<RcsThread> CREATOR = new Creator<RcsThread>() {
        @Override
        public RcsThread createFromParcel(Parcel in) {
            int type = in.readInt();

            switch (type) {
                case RCS_1_TO_1_TYPE:
                    return new Rcs1To1Thread(in);
                case RCS_GROUP_TYPE:
                    return new RcsGroupThread(in);
                default:
                    Log.e(RcsMessageStore.TAG, "Cannot unparcel RcsThread, wrong type: " + type);
            }
            return null;
        }

        @Override
        public RcsThread[] newArray(int size) {
            return new RcsThread[0];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mThreadId);
    }
}
