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

package android.hardware;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @hide
 */
public final class BatteryState implements Parcelable {
    public String scope;
    public String type;
    public int capacity;
    public String status;

    public BatteryState() {
        scope = "";
        type = "";
        capacity = 100;
        status = "";
    }

    public BatteryState(BatteryState state) {
        scope = state.scope;
        type = state.type;
        capacity = state.capacity;
        status = state.status;
    }

    /**
     * @hide
     */
    public static final Parcelable.Creator<BatteryState> CREATOR = new Parcelable.Creator() {
        public BatteryState[] newArray(int size) {
            return new BatteryState[size];
        }
        public BatteryState createFromParcel(Parcel in) {
            BatteryState state = new BatteryState();
            state.scope = in.readString();
            state.type = in.readString();
            state.capacity = in.readInt();
            state.status = in.readString();
            return state;
        }
    };

    /**
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    /**
     * @hide
     */
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(scope);
        out.writeString(type);
        out.writeInt(capacity);
        out.writeString(status);
    }

    @Override
    public String toString() {
        return "[scope: " + scope + ", type: " + type
                + ", capacity: " + capacity + ", status: " + status + "]";
    }
}
