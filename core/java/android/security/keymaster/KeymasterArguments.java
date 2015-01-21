/**
 * Copyright (c) 2015, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.security.keymaster;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class for the java side of user specified Keymaster arguments.
 * <p>
 * Serialization code for this and subclasses must be kept in sync with system/security/keystore
 * @hide
 */
public class KeymasterArguments implements Parcelable {
    List<KeymasterArgument> mArguments;

    public static final Parcelable.Creator<KeymasterArguments> CREATOR = new
            Parcelable.Creator<KeymasterArguments>() {
                public KeymasterArguments createFromParcel(Parcel in) {
                    return new KeymasterArguments(in);
                }
                public KeymasterArguments[] newArray(int size) {
                    return new KeymasterArguments[size];
                }
            };

    public KeymasterArguments() {
        mArguments = new ArrayList<KeymasterArgument>();
    }

    private KeymasterArguments(Parcel in) {
        mArguments = in.createTypedArrayList(KeymasterArgument.CREATOR);
    }

    public void addInt(int tag, int value) {
        mArguments.add(new KeymasterIntArgument(tag, value));
    }

    public void addBoolean(int tag) {
        mArguments.add(new KeymasterBooleanArgument(tag));
    }

    public void addLong(int tag, long value) {
        mArguments.add(new KeymasterLongArgument(tag, value));
    }

    public void addBlob(int tag, byte[] value) {
        mArguments.add(new KeymasterBlobArgument(tag, value));
    }

    public void addDate(int tag, Date value) {
        mArguments.add(new KeymasterDateArgument(tag, value));
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeTypedList(mArguments);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
