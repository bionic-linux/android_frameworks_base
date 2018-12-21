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
import java.util.List;

/**
 * This is a single instance of a message sent over RCS.
 * @hide - TODO(109759350) make this public
 */
public class RcsOutgoingMessage extends RcsMessage {
    private final List<RcsOutgoingMessageDelivery> mOutgoingDeliveries;

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
}
