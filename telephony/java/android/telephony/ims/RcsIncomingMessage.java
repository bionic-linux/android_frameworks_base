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
 *
 * @hide - TODO(109759350) make this public
 */
public class RcsIncomingMessage extends RcsMessage {
    private RcsParticipant mSenderParticipant;
    private long mArrivalTimestamp;
    private long mNotifiedTimestamp;

    /**
     * @return Returns a new builder object for {@link RcsIncomingMessage}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Sets the timestamp of arrival for this message and persists into storage. The timestamp is
     * defined as milliseconds passed after midnight, January 1, 1970 UTC
     *
     * @param arrivalTimestamp The timestamp to set to.
     */
    @WorkerThread
    public void setArrivalTimestamp(long arrivalTimestamp) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setMessageArrivalTimestamp(mId, true, arrivalTimestamp);
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
     *
     * @param notifiedTimestamp The timestamp to set to.
     */
    @WorkerThread
    public void setNotifiedTimestamp(long notifiedTimestamp) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setMessageNotifiedTimestamp(mId, true, notifiedTimestamp);
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

    /**
     * @hide
     */
    RcsIncomingMessage(RcsParticipant senderParticipant, long arrivalTimestamp,
            long notifiedTimestamp, String rcsMessageGlobalId, int subId,
            @RcsMessageStatus int messageStatus, long originationTimestamp) {
        super(rcsMessageGlobalId, subId, messageStatus, originationTimestamp);
        mSenderParticipant = senderParticipant;
        mArrivalTimestamp = arrivalTimestamp;
        mNotifiedTimestamp = notifiedTimestamp;
    }

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

    /**
     * Use this builder to get an instance of {@link RcsIncomingMessage}. The message will not be
     * persisted into storage until it is added to an {@link RcsThread}.
     */
    public static class Builder {
        private RcsParticipant mSenderParticipant;
        private long mArrivalTimestamp;
        private long mNotifiedTimestamp;
        private String mRcsMessageGlobalId;
        private int mSubId;
        private @RcsMessageStatus int mMessageStatus;
        private long mOriginationTimestamp;

        /**
         * Sets the sender of the message to be built.
         * @param senderParticipant The {@link RcsParticipant} that sent this message
         * @return The same instance of {@link Builder} to chain setter methods
         */
        public Builder setSenderParticipant(RcsParticipant senderParticipant) {
            mSenderParticipant = senderParticipant;
            return this;
        }

        /**
         * Sets the arrival timestamp of the message to be built.
         * @param arrivalTimestamp The timestamp of the message arrival. The value is in
         *                         milliseconds passed after midnight, January 1, 1970 UTC
         * @return The same instance of {@link Builder} to chain setter methods
         */
        public Builder setArrivalTimestamp(long arrivalTimestamp) {
            mArrivalTimestamp = arrivalTimestamp;
            return this;
        }

        /**
         * Sets the notification timestamp of the message to be built.
         * @param notifiedTimestamp The timestamp of the user notified of this message. The value is
         *                          in milliseconds passed after midnight, January 1, 1970 UTC
         * @return The same instance of {@link Builder} to chain setter methods
         */
        public Builder setNotifiedTimestamp(long notifiedTimestamp) {
            mNotifiedTimestamp = notifiedTimestamp;
            return this;
        }

        /**
         * Sets the globally unique RCS message ID for this message.
         * @param rcsMessageId The unique ID for the message to be built
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setRcsMessageId(String rcsMessageId) {
            mRcsMessageGlobalId = rcsMessageId;
            return this;
        }

        /**
         * Sets the subscription identifier that this outgoing message was sent from.
         * @param subId The subscription identifier.
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setSubId(int subId) {
            mSubId = subId;
            return this;
        }

        /**
         * Sets the message status for this outgoing message. For a message that is yet to be sent,
         * it should be {@link android.telephony.ims.RcsMessage.RcsMessageStatus#DRAFT}.
         * @param rcsMessageStatus The current message status.
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setStatus(@RcsMessageStatus int rcsMessageStatus) {
            mMessageStatus = rcsMessageStatus;
            return this;
        }

        /**
         * Sets the origination timestamp for this outgoing message. This should be the time that
         * the message was completed and tried to be sent.
         * @param originationTimestamp The origination timestamp value in milliseconds passed
         *                             after midnight, January 1, 1970 UTC
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder setOriginationTimestamp(long originationTimestamp) {
            mOriginationTimestamp = originationTimestamp;
            return this;
        }

        /**
         * Creates a new {@link RcsIncomingMessage}. This object is not persisted into storage until
         * it is added to a thread using {@link RcsThread#addMessage(RcsMessage)} and modifying its
         * values after it is created but before put in a thread will fail.
         *
         * @return A new instance of {@link RcsIncomingMessage}
         */
        public RcsIncomingMessage build() {
            return new RcsIncomingMessage(mSenderParticipant,
                    mArrivalTimestamp, mNotifiedTimestamp, mRcsMessageGlobalId, mSubId,
                    mMessageStatus, mOriginationTimestamp);
        }
    }
}
