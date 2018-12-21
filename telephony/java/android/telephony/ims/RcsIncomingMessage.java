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
 * This is a single instance of a message received over RCS.
 * @hide - TODO(109759350) make this public
 */
public class RcsIncomingMessage extends RcsMessage {
    private RcsParticipant mSenderParticipant;
    private long mArrivalTimestamp;
    private long mNotifiedTimestamp;

    /**
     * Sets the timestamp of arrival for this message and persists into storage. The timestamp is
     * defined as milliseconds passed after midnight, January 1, 1970 UTC
     * @param arrivalTimestamp The timestamp to set to.
     */
    @WorkerThread
    public void setArrivalTimestamp(long arrivalTimestamp) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setMessageArrivalTimestamp(mId, arrivalTimestamp);
                mArrivalTimestamp = arrivalTimestamp;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsIncomingMessage: Exception happened during setArrivalTimestamp: ", re);
        }
    }

    /**
     * @return Returns the timestamp of arrival for this message. The timestamp is defined as
     * milliseconds passed after midnight, January 1, 1970 UTC
     */
    public long getArrivalTimestamp() {
        return mArrivalTimestamp;
    }

    /**
     * Sets the timestamp of notification for this message and persists into storage. The timestamp
     * is defined as milliseconds passed after midnight, January 1, 1970 UTC
     * @param notifiedTimestamp The timestamp to set to.
     */
    @WorkerThread
    public void setNotifiedTimestamp(long notifiedTimestamp) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setMessageNotifiedTimestamp(mId, notifiedTimestamp);
                mNotifiedTimestamp = notifiedTimestamp;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsIncomingMessage: Exception happened during setNotifiedTimestamp: ", re);
        }
    }

    /**
     * @return Returns the timestamp of notification for this message. The timestamp is defined as
     * milliseconds passed after midnight, January 1, 1970 UTC
     */
    public long getNotifiedTimestamp() {
        return mNotifiedTimestamp;
    }

    /**
     * @return Returns the sender of this incoming message.
     */
    public RcsParticipant getSenderParticipant() {
        return mSenderParticipant;
    }

    /**
     * @return Returns true as this is an incoming message
     */
    @Override
    public boolean isIncoming() {
        return true;
    }

    public static final Creator<RcsIncomingMessage> CREATOR = new Creator<RcsIncomingMessage>() {
        @Override
        public RcsIncomingMessage createFromParcel(Parcel in) {
            // Do a dummy read to skip the type.
            in.readInt();
            return new RcsIncomingMessage(in);
        }

        @Override
        public RcsIncomingMessage[] newArray(int size) {
            return new RcsIncomingMessage[size];
        }
    };

    protected RcsIncomingMessage(Parcel in) {
        super(in);
        mArrivalTimestamp = in.readLong();
        mNotifiedTimestamp = in.readLong();
        mSenderParticipant = in.readParcelable(RcsParticipant.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(INCOMING_MESSAGE_TYPE);
        dest.writeLong(mArrivalTimestamp);
        dest.writeLong(mNotifiedTimestamp);
        dest.writeParcelable(mSenderParticipant, flags);
    }
}
