/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
    
public class PreciseCallState implements Parcelable {
    public int state;
    public String[] addresses;
    
    public PreciseCallState(int state, String[] addresses) {
        this.state = state;
        this.addresses = addresses;
    }

    public PreciseCallState(Parcel in) {
        state = in.readInt();
        addresses = in.readStringArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(state);
        dest.writeStringArray(addresses);
    }
    
    public static final Parcelable.Creator<PreciseCallState> CREATOR =
        new Parcelable.Creator<PreciseCallState>() {
        public PreciseCallState createFromParcel(Parcel in) {
            return new PreciseCallState(in);
        }
    
        public PreciseCallState[] newArray(int size) {
            return new PreciseCallState[size];
        }
    };
}
