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

import android.os.Parcel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a single instance of a message sent over RCS.
 *
 * @hide - TODO(109759350) make this public
 */
public class RcsOutgoingMessage extends RcsMessage {
    private final List<RcsOutgoingMessageDelivery> mOutgoingDeliveries;

    private void addDelivery(RcsOutgoingMessageDelivery delivery) {
        mOutgoingDeliveries.add(delivery);
    }

    /**
     * @return Returns the {@link RcsOutgoingMessageDelivery}s associated with this message.
     */
    public List<RcsOutgoingMessageDelivery> getOutgoingDeliveries() {
        return mOutgoingDeliveries;
    }

    /**
     * @return Returns false as this is not an incoming message.
     */
    @Override
    public boolean isIncoming() {
        return false;
    }

    RcsOutgoingMessage(String rcsMessageGlobalId, int subId, int messageStatus,
            long originationTimestamp, String text, double latitude, double longitude) {
        super(rcsMessageGlobalId, subId, messageStatus, originationTimestamp, text, latitude,
                longitude);
        mOutgoingDeliveries = new ArrayList<>();
    }

    protected RcsOutgoingMessage(Parcel in) {
        super(in);

        mOutgoingDeliveries = new ArrayList<>();
        in.readTypedList(mOutgoingDeliveries, RcsOutgoingMessageDelivery.CREATOR);
    }

    public static final Creator<RcsOutgoingMessage> CREATOR = new Creator<RcsOutgoingMessage>() {
        @Override
        public RcsOutgoingMessage createFromParcel(Parcel in) {
            // Do a dummy read to skip the type.
            in.readInt();
            return new RcsOutgoingMessage(in);
        }

        @Override
        public RcsOutgoingMessage[] newArray(int size) {
            return new RcsOutgoingMessage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mOutgoingDeliveries);
    }

    /**
     * Use this builder to get an instance of {@link RcsOutgoingMessage}. The message will not be
     * persisted into storage until it is added to an {@link RcsThread}.
     */
    public static class Builder extends RcsMessage.Builder {
        // keep deliveries in a map to disallow multiple deliveries addressing the same participant
        private Map<RcsParticipant, RcsOutgoingMessageDelivery> mDeliveries;

        /**
         * Creates a new Builder for {@link RcsOutgoingMessage}
         */
        public Builder() {
            mDeliveries = new HashMap<>();
        }

        public Builder addMessageDelivery(RcsParticipant recipient) {
            mDeliveries.put(recipient, new RcsOutgoingMessageDelivery(recipient));
            return this;
        }

        /**
         * Convenience function to retroactively add a delivery information for a participant.
         * @param recipient The {@link RcsParticipant} that the delivery should be added for
         * @param deliveredTimestamp The timestamp value of when this message was delivered to
         *                           recipient. The value is in milliseconds passed after midnight,
         *                           January 1, 1970 UTC
         * @param seenTimestamp The timestamp value of when this message was seen by recipient. The
         *                      value is in milliseconds passed after midnight, January 1, 1970 UTC
         * @return The same instance of {@link Builder} to chain setter methods.
         */
        public Builder addMessageDelivery(RcsParticipant recipient, long deliveredTimestamp,
                long seenTimestamp) {
            mDeliveries.put(recipient,
                    new RcsOutgoingMessageDelivery(recipient, deliveredTimestamp, seenTimestamp));
            return this;
        }

        /**
         * Creates a new {@link RcsIncomingMessage}. This object is not persisted into storage until
         * it is added to a thread using {@link RcsThread#addMessage(RcsMessage)} and modifying its
         * values after it is created but before put in a thread will fail.
         *
         * @return A new instance of {@link RcsIncomingMessage}
         */
        public RcsOutgoingMessage build() {
            RcsOutgoingMessage outgoingMessage = new RcsOutgoingMessage(mRcsMessageGlobalId, mSubId,
                    mMessageStatus, mOriginationTimestamp, mText, mLatitude, mLongitude);

            for (RcsOutgoingMessageDelivery delivery : mDeliveries.values()) {
                delivery.setRcsOutgoingMessage(outgoingMessage);
                delivery.initialSaveToStorage();
                outgoingMessage.addDelivery(delivery);
            }

            return outgoingMessage;
        }
    }
}
