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

/**
 * This class holds the delivery information of an {@link RcsOutgoingMessage} for each
 * {@link RcsParticipant} that the message was intended for.
 *
 * @hide - TODO(109759350) make this public
 */
public class RcsOutgoingMessageDelivery implements Parcelable {
    private long mDeliveredTimestamp;
    private long mSeenTimestamp;
    private final RcsParticipant mRecipient;
    private final RcsMessage mRcsMessage;

    /**
     * Sets the delivery time of this outgoing delivery and persists into storage.
     *
     * @param deliveredTimestamp The timestamp to set to delivery. It is defined as milliseconds
     *                           passed after midnight, January 1, 1970 UTC
     */
    @WorkerThread
    public void setDeliveredTimestamp(long deliveredTimestamp) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setOutgoingDeliveryDeliveredTimestamp(mRcsMessage, mRecipient,
                        deliveredTimestamp);
                mDeliveredTimestamp = deliveredTimestamp;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsOutgoingMessageDelivery: Exception happened during setDeliveredTimestamp: ",
                    re);
        }
    }

    /**
     * @return Returns the delivered timestamp of the associated message to the associated
     * participant. Timestamp is defined as milliseconds passed after midnight, January 1, 1970 UTC
     */
    public long getDeliveredTimestamp() {
        return mDeliveredTimestamp;
    }

    /**
     * Sets the seen time of this outgoing delivery and persists into storage.
     *
     * @param seenTimestamp The timestamp to set to delivery. It is defined as milliseconds
     *                      passed after midnight, January 1, 1970 UTC
     */
    @WorkerThread
    public void setSeenTimestamp(long seenTimestamp) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setOutgoingDeliverySeenTimestamp(mRcsMessage, mRecipient, seenTimestamp);
                mSeenTimestamp = seenTimestamp;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsOutgoingMessageDelivery: Exception happened during setSeenTimestamp: ", re);
        }
    }

    /**
     * @return Returns the seen timestamp of the associated message by the associated
     * participant. Timestamp is defined as milliseconds passed after midnight, January 1, 1970 UTC
     */
    public long getSeenTimestamp() {
        return mSeenTimestamp;
    }

    /**
     * @return Returns the recipient associated with this delivery.
     */
    public RcsParticipant getRecipient() {
        return mRecipient;
    }

    /**
     * @return Returns the {@link RcsMessage} associated with this delivery.
     */
    public RcsMessage getMessage() {
        return mRcsMessage;
    }

    protected RcsOutgoingMessageDelivery(Parcel in) {
        mDeliveredTimestamp = in.readLong();
        mSeenTimestamp = in.readLong();
        mRecipient = in.readParcelable(RcsParticipant.class.getClassLoader());
        mRcsMessage = in.readParcelable(RcsMessage.class.getClassLoader());
    }

    public static final Creator<RcsOutgoingMessageDelivery> CREATOR =
            new Creator<RcsOutgoingMessageDelivery>() {
                @Override
                public RcsOutgoingMessageDelivery createFromParcel(Parcel in) {
                    return new RcsOutgoingMessageDelivery(in);
                }

                @Override
                public RcsOutgoingMessageDelivery[] newArray(int size) {
                    return new RcsOutgoingMessageDelivery[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mDeliveredTimestamp);
        dest.writeLong(mSeenTimestamp);
        dest.writeParcelable(mRecipient, flags);
        dest.writeParcelable(mRcsMessage, flags);
    }
}
