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
package android.telephony.ims;

import android.annotation.Nullable;
import android.annotation.WorkerThread;
import android.net.Uri;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ims.aidl.IRcs;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RcsGroupThread represents a single RCS conversation thread where {@link RcsParticipant}s can join
 * or leave.
 * @hide - TODO(sahinc) make this public
 */
public class RcsGroupThread extends RcsThread {
    // The user visible name of this RCS group thread
    private String mGroupName;

    // The URI to the user visible icon of this RCS group thread
    private Uri mGroupIcon;

    // The participant that "owns" the thread. This participant can remove other participants, but
    // storage APIs do not assert that it was the owner that removed a participant.
    private RcsParticipant mOwnerParticipant;

    // The conference URI for this RCS group. The client should use this to send & receive messages
    private String mConferenceUri;

    // The set of participants in this group.
    private final Set<RcsParticipant> mParticipants;

    /**
     * Public constructor only for RcsMessageStoreController to initialize new threads.
     *
     * @hide
     */
    public RcsGroupThread(int threadId, RcsParticipant ownerParticipant, String groupName,
            Uri groupIcon, String conferenceUri) {
        super(threadId);
        mParticipants = new HashSet<>();
        mGroupName = groupName;
        mGroupIcon = groupIcon;

        mOwnerParticipant = ownerParticipant;
        if (mOwnerParticipant != null) {
            mParticipants.add(ownerParticipant);
        }

        mConferenceUri = conferenceUri;
    }

    /**
     * @return Returns true as this is always a group thread
     */
    @Override
    public boolean isGroup() {
        return true;
    }

    /**
     * @return Returns the given name of this {@link RcsGroupThread}
     */
    public String getGroupName() {
        return mGroupName;
    }

    /**
     * Sets the name of this {@link RcsGroupThread} and saves it into storage.
     */
    @WorkerThread
    public void setGroupName(String groupName) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setGroupThreadName(mThreadId, groupName);
                mGroupName = groupName;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "Rcs1To1Thread: Exception happened during setFallbackThreadId", re);
        }
    }

    /**
     * @return Returns a URI that points to the group's icon {@link RcsGroupThread}
     */
    public Uri getGroupIcon() {
        return mGroupIcon;
    }

    /**
     * Sets the icon for this {@link RcsGroupThread} and saves it into storage.
     */
    @WorkerThread
    public void setGroupIcon(Uri groupIcon) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setGroupThreadIcon(mThreadId, groupIcon);
                mGroupIcon = groupIcon;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "Rcs1To1Thread: Exception happened during setFallbackThreadId", re);
        }
    }

    /**
     * @return Returns the owner of this thread.
     */
    @Nullable
    public RcsParticipant getOwner() {
        return mOwnerParticipant;
    }

    /**
     * Sets the owner of this {@link RcsGroupThread} and saves it into storage. This is intended to
     * be used for selecting a new owner for a group thread if the owner leaves the thread. The
     * owner needs to be in the list of existing participants.
     *
     * @param participant The new owner of the thread. Null values are allowed.
     */
    @WorkerThread
    public void setOwner(@Nullable RcsParticipant participant) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.setGroupThreadOwner(mThreadId, participant);
                mOwnerParticipant = participant;
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "Rcs1To1Thread: Exception happened during setFallbackThreadId", re);
        }
    }

    /**
     * Adds a new {@link RcsParticipant} to this group thread and persists into storage.
     *
     * @param participant The new participant to be added to the thread.
     */
    @WorkerThread
    public void addParticipant(RcsParticipant participant) {
        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.addParticipantToGroupThread(mThreadId, participant);
                mParticipants.add(participant);
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "Rcs1To1Thread: Exception happened during setFallbackThreadId", re);
        }
    }

    /**
     * Removes an {@link RcsParticipant} from this group thread and persists into storage. If the
     * removed participant was the owner of this group, the owner will become null.
     */
    @WorkerThread
    public void removeParticipant(RcsParticipant participant) {
        if (mOwnerParticipant.equals(participant)) {
            setOwner(null);
        }

        try {
            IRcs iRcs = IRcs.Stub.asInterface(ServiceManager.getService("ircs"));
            if (iRcs != null) {
                iRcs.removeParticipantFromGroupThread(mThreadId, participant);
                mParticipants.remove(participant);
            }
        } catch (RemoteException re) {
            Log.e(RcsMessageStore.TAG,
                    "Rcs1To1Thread: Exception happened during setFallbackThreadId", re);
        }
    }

    /**
     * Returns the set of {@link RcsParticipant}s that contribute to this group thread. The
     * returned set does not support modifications, please use
     * {@link RcsGroupThread#addParticipant(RcsParticipant)}
     * and {@link RcsGroupThread#removeParticipant(RcsParticipant)} instead.
     *
     * @return the immutable set of {@link RcsParticipant} in this group thread.
     */
    public Set<RcsParticipant> getParticipants() {
        return Collections.unmodifiableSet(mParticipants);
    }

    /**
     * Returns the conference URI for this {@link RcsGroupThread}. In order to be more lenient
     * against different RCS implementations, this returns a String rather than a
     * {@link android.net.Uri}
     */
    public String getConferenceUri() {
        return mConferenceUri;
    }

    /**
     * Sets the conference URI for this {@link RcsGroupThread} and persists into storage.
     * In order to be more lenient against different RCS implementations, this accepts a String
     * rather than a {@link android.net.Uri}
     *
     * @param conferenceUri The URI as String to be used as the conference URI.
     */
    public void setConferenceUri(String conferenceUri) {
        mConferenceUri = conferenceUri;

        // TODO (109759350) - implement saving into storage
    }

    public static final Creator<RcsGroupThread> CREATOR = new Creator<RcsGroupThread>() {
        @Override
        public RcsGroupThread createFromParcel(Parcel in) {
            // Do a dummy read to skip the type.
            in.readInt();
            return new RcsGroupThread(in);
        }

        @Override
        public RcsGroupThread[] newArray(int size) {
            return new RcsGroupThread[size];
        }
    };

    protected RcsGroupThread(Parcel in) {
        super(in);
        mGroupName = in.readString();
        String uriAsString = in.readString();

        if (!TextUtils.isEmpty(uriAsString)) {
            mGroupIcon = Uri.parse(uriAsString);
        }
        mConferenceUri = in.readString();
        List<RcsParticipant> participantList = new ArrayList<>();
        in.readTypedList(participantList, RcsParticipant.CREATOR);
        mParticipants = new HashSet<>(participantList);

        mOwnerParticipant = in.readParcelable(RcsParticipant.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(RCS_GROUP_TYPE);
        super.writeToParcel(dest, flags);
        dest.writeString(mGroupName);
        dest.writeString(mGroupIcon.toString());
        dest.writeString(mConferenceUri);

        List<RcsParticipant> participantList = new ArrayList<>();
        participantList.addAll(mParticipants);

        dest.writeTypedList(participantList);
        dest.writeParcelable(mOwnerParticipant, flags);
    }
}
