/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package android.telephony.ims;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *  @hide
 */

public class MMTelCapabilityConfig implements Parcelable {

    public static final int FEATURE_TYPE_VOICE_OVER_LTE = 0;
    public static final int FEATURE_TYPE_VIDEO_OVER_LTE = 1;
    public static final int FEATURE_TYPE_VOICE_OVER_WIFI = 2;
    public static final int FEATURE_TYPE_VIDEO_OVER_WIFI = 3;
    public static final int FEATURE_TYPE_UT_OVER_LTE = 4;
    public static final int FEATURE_TYPE_UT_OVER_WIFI = 5;

    protected MMTelCapabilityConfig(Parcel in) {
    }

    public static final Creator<MMTelCapabilityConfig> CREATOR = new Creator<MMTelCapabilityConfig>() {
        @Override
        public MMTelCapabilityConfig createFromParcel(Parcel in) {
            return new MMTelCapabilityConfig(in);
        }

        @Override
        public MMTelCapabilityConfig[] newArray(int size) {
            return new MMTelCapabilityConfig[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
