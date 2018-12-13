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
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ims.aidl.IRcs;
import android.util.Log;

/**
 * Rcs1To1Thread represents a single RCS conversation thread with a total of two
 * {@link RcsParticipant}s.
 *
 * @hide - TODO(109759350) make this public
 * TODO(109759350) add exceptions and roll back local changes if RPC fails
 */
public class Rcs1To1Thread extends RcsThread {
    private int mFallbackThreadId;
    private final RcsParticipant mRecipient;

    /**
     * Public constructor only for RcsMessageStoreController to initialize new threads.
     *
     * @hide
     */
    public Rcs1To1Thread(int threadId, RcsParticipant recipient, int fallbackThreadId) {
        super(threadId);
        mRecipient = recipient;
        mFallbackThreadId = fallbackThreadId;
    }

    /**
     * @return Returns false as this is always a 1 to 1 thread.
     */
    @Override
    public boolean isGroup() {
        return false;
    }

    /**
     * {@link Rcs1To1Thread}s can fall back to SMS as a back-up protocol. This function returns the
     * thread id to be used to query {@code content://mms-sms/conversation/#} to get the fallback
     * thread.
     *
     * @return The thread id to be used to query the mms-sms authority.
     */
    public int getFallbackThreadId() {
        return mFallbackThreadId;
    }

    /**
     * If the RCS client allows falling back to SMS, it needs to create an MMS-SMS thread in
     * {@code content://mms-sms/conversation/#}. Use this function to link the
     * {@link RcsGroupThread} to the MMS-SMS thread. This function also updates the storage.
     */
    @WorkerThread
    public void setFallbackThreadId(int fallbackThreadId) {
        if (mFallbackThreadId == fallbackThreadId) {
            return;
        }

        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.set1To1ThreadFallbackThreadId(mThreadId, fallbackThreadId);
                mFallbackThreadId = fallbackThreadId;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "Rcs1To1Thread: Exception happened during setFallbackThreadId", re);
        }
    }

    /**
     * @return Returns the {@link RcsParticipant} that receives the messages sent in this thread.
     */
    public RcsParticipant getRecipient() {
        return mRecipient;
    }

    public static final Creator<Rcs1To1Thread> CREATOR = new Creator<Rcs1To1Thread>() {
        @Override
        public Rcs1To1Thread createFromParcel(Parcel in) {
            // Do a dummy read to skip the type.
            in.readInt();
            return new Rcs1To1Thread(in);
        }

        @Override
        public Rcs1To1Thread[] newArray(int size) {
            return new Rcs1To1Thread[size];
        }
    };

    protected Rcs1To1Thread(Parcel in) {
        super(in);
        mRecipient = in.readParcelable(RcsParticipant.class.getClassLoader());
        mFallbackThreadId = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(RCS_1_TO_1_TYPE);
        super.writeToParcel(dest, flags);
        dest.writeParcelable(mRecipient, flags);
        dest.writeInt(mFallbackThreadId);
    }
}
