/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.net;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.android.internal.util.Preconditions;

import java.util.Objects;

/**
 * A {@link NetworkSpecifier} used to identify test interfaces.
 *
 * @see TestNetworkManager
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class TestNetworkSpecifier extends NetworkSpecifier implements Parcelable {

    /**
     * Name of the network interface.
     */
    @NonNull
    public final String interfaceName;

    public TestNetworkSpecifier(@NonNull String specifier) {
        Preconditions.checkStringNotEmpty(specifier);
        this.interfaceName = specifier;
    }

    /** @hide */
    @Override
    public boolean canBeSatisfiedBy(NetworkSpecifier other) {
        return equals(other);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TestNetworkSpecifier)) return false;
        return TextUtils.equals(interfaceName, ((TestNetworkSpecifier) o).interfaceName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(interfaceName);
    }

    @Override
    public String toString() {
        return interfaceName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(interfaceName);
    }

    public static final @NonNull Creator<TestNetworkSpecifier> CREATOR =
            new Creator<TestNetworkSpecifier>() {
        public TestNetworkSpecifier createFromParcel(Parcel in) {
            return new TestNetworkSpecifier(in.readString());
        }
        public TestNetworkSpecifier[] newArray(int size) {
            return new TestNetworkSpecifier[size];
        }
    };
}
