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

import android.annotation.IntDef;
import android.annotation.WorkerThread;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ims.aidl.IRcs;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is a single instance of a message sent or received over RCS.
 *
 * @hide - TODO(109759350) make this public
 */
public abstract class RcsMessage implements Parcelable {
    public static final @RcsMessageStatus int DRAFT = 0;
    public static final @RcsMessageStatus int SUCCEEDED = 1;
    public static final @RcsMessageStatus int FAILED = 2;
    public static final @RcsMessageStatus int SENDING = 3;
    public static final @RcsMessageStatus int SENT = 4;
    public static final @RcsMessageStatus int DOWNLOADING = 5;
    public static final @RcsMessageStatus int PAUSED = 6;
    public static final @RcsMessageStatus int RETRYING = 7;

    // Since this is an abstract class that gets parcelled, the sub-classes need to write these
    // magic values into the parcel so that we know which type to unparcel into. These are defined
    // as integer instead of boolean for extensibility.
    protected static final int INCOMING_MESSAGE_TYPE = 1998;
    protected static final int OUTGOING_MESSAGE_TYPE = 1999;

    int mId;
    private String mRcsMessageGlobalId;
    private int mSubId;
    private @RcsMessageStatus int mMessageStatus;
    private long mOriginationTimestamp;
    private final Set<RcsPart> mParts;
    private RcsThread mOwnerThread;

    @IntDef({
            DRAFT, SUCCEEDED, FAILED, SENDING, SENDING, DOWNLOADING, PAUSED, RETRYING
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RcsMessageStatus {
    }

    RcsMessage(String rcsMessageGlobalId, int subId, @RcsMessageStatus int messageStatus,
            long originationTimestamp) {
        mRcsMessageGlobalId = rcsMessageGlobalId;
        mSubId = subId;
        mMessageStatus = messageStatus;
        mOriginationTimestamp = originationTimestamp;

        mParts = new HashSet<>();
    }

    /**
     * Sets the row Id of the common message. This is needed to be set externally as the message
     * does not write itself to the storage.
     * @hide
     */
    public void setId(int id) {
        mId = id;
    }

    /**
     * Returns the row Id from the common message.
     * @hide
     */
    public int getId() {
        return mId;
    }

    /**
     * @return Returns the subscription ID that this message was sent from, or delivered to
     */
    public int getSubId() {
        return mSubId;
    }

    /**
     * Sets the subscription ID that this message was sent from, or delivered to and persists it
     * into storage.
     *
     * @param subId The subscription ID to persists into storage.
     */
    public void setSubId(int subId) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setMessageSubId(mId, isIncoming(), subId);
                mSubId = subId;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsMessage: Exception happened during setSubId", re);
        }
    }

    /**
     * Sets the status of this message and persists it into storage.
     */
    @WorkerThread
    public void setStatus(@RcsMessageStatus int rcsMessageStatus) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setMessageStatus(mId, isIncoming(), rcsMessageStatus);
                mMessageStatus = rcsMessageStatus;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsMessage: Exception happened during setStatus", re);
        }
    }

    /**
     * @return Returns the status of this message.
     */
    @RcsMessageStatus
    public int getStatus() {
        return mMessageStatus;
    }

    /**
     * Sets the origination timestamp of this message and persists it into storage. Origination is
     * defined as when either sender or receiver tapped the send button.
     *
     * @param timestamp The origination timestamp value in milliseconds passed after midnight,
     *                  January 1, 1970 UTC
     */
    @WorkerThread
    public void setOriginationTimestamp(long timestamp) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setMessageOriginationTimestamp(mId, isIncoming(), timestamp);
                mOriginationTimestamp = timestamp;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsMessage: Exception happened during setOriginationTimestamp", re);
        }
    }

    /**
     * @return Returns the origination timestamp of this message in milliseconds passed after
     * midnight, January 1, 1970 UTC. Origination is defined as when either sender or receiver
     * tapped the send button.
     */
    public long getOriginationTimestamp() {
        return mOriginationTimestamp;
    }

    /**
     * Sets the globally unique RCS message identifier for this message and persists it into
     * storage. This function does not confirm that this message id is unique.
     *
     * @param rcsMessageGlobalId The globally unique RCS message identifier
     */
    @WorkerThread
    public void setRcsMessageId(String rcsMessageGlobalId) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setGlobalMessageIdForMessage(mId, isIncoming(), mRcsMessageGlobalId);
                mRcsMessageGlobalId = rcsMessageGlobalId;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsMessage: Exception happened during setRcsMessageId", re);
        }
    }

    /**
     * @return Returns the globally unique RCS message identifier for this message.
     */
    public String getRcsMessageId() {
        return mRcsMessageGlobalId;
    }

    /**
     * @return Returns an immutable set of {@link RcsPart}s that this RCS message is composed of.
     */
    public Set<RcsPart> getParts() {
        return Collections.unmodifiableSet(mParts);
    }

    /**
     * Adds an {@link RcsPart} to the set of existing parts this message has and persists into the
     * storage.
     */
    @WorkerThread
    public void addPart(RcsPart rcsPart) {
        addPart(Collections.singletonList(rcsPart));
    }

    /**
     * Adds a collection of {@link RcsPart}s to the set of existing parts this message has and
     * persists into the storage.
     */
    @WorkerThread
    public void addPart(Iterable<RcsPart> rcsParts) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                for (RcsPart rcsPart : rcsParts) {
                    iRcs.addPartToMessage(mId, isIncoming(), rcsPart);
                    mParts.add(rcsPart);
                }
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsMessage: Exception happened during addPart", re);
        }
    }

    /**
     * Removes a part from the set of existing {@link RcsPart}s this message already has and
     * persists into the storage.
     */
    @WorkerThread
    public void removePart(RcsPart rcsPart) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.removePartFromMessage(mId, isIncoming(), rcsPart);
                mParts.remove(rcsPart);
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "RcsMessage: Exception happened during removePart", re);
        }
    }

    /**
     * @return Returns true if this message was received on this device, false if it was sent.
     */
    public abstract boolean isIncoming();

    RcsMessage(Parcel in) {
        mId = in.readInt();
        mSubId = in.readInt();
        mMessageStatus = in.readInt();
        mOriginationTimestamp = in.readLong();
        mRcsMessageGlobalId = in.readString();

        List<RcsPart> partList = new ArrayList<>();
        in.readTypedList(partList, RcsPart.CREATOR);
        mParts = new HashSet<>(partList);

        mOwnerThread = in.readParcelable(RcsThread.class.getClassLoader());
    }

    public static final Creator<RcsMessage> CREATOR = new Creator<RcsMessage>() {
        @Override
        public RcsMessage createFromParcel(Parcel in) {
            int type = in.readInt();

            switch (type) {
                case INCOMING_MESSAGE_TYPE:
                    return new RcsIncomingMessage(in);
                case OUTGOING_MESSAGE_TYPE:
                    return new RcsOutgoingMessage(in);
                default:
                    Log.e(RcsMessageStore.TAG, "Cannot unparcel RcsMessage, wrong type: " + type);
            }
            return null;
        }

        @Override
        public RcsMessage[] newArray(int size) {
            return new RcsMessage[0];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeInt(mSubId);
        dest.writeInt(mMessageStatus);
        dest.writeLong(mOriginationTimestamp);
        dest.writeString(mRcsMessageGlobalId);

        List<RcsPart> partList = new ArrayList<>(mParts);
        dest.writeTypedList(partList, flags);

        dest.writeParcelable(mOwnerThread, flags);
    }
}
