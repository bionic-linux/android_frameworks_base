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
 * limitations under the License.
 */

package android.net;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import com.android.internal.util.MessageUtils;

import java.util.Objects;

/** @hide */
public final class VpnTransportInfo implements TransportInfo, Parcelable {
    private static final SparseArray<String> sTypeToString =
            MessageUtils.findMessageNames(new Class[]{VpnManager.class}, new String[]{"TYPE_VPN_"});

    /** Type of this VPN. */
    @VpnManager.VpnType public final int type;

    /** Session name of this VPN. Only visible to the system, not to unprivileged applications. */
    @Nullable
    public final String sessionName;

    public VpnTransportInfo(@VpnManager.VpnType int type, String sessionName) {
        this.type = type;
        this.sessionName = sessionName;
    }

    @Override
    public TransportInfo redact() {
        return new VpnTransportInfo(type, null);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VpnTransportInfo)) return false;

        VpnTransportInfo that = (VpnTransportInfo) o;
        return this.type == that.type && Objects.equals(this.sessionName, that.sessionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, sessionName);
    }

    @Override
    public String toString() {
        final String typeString = sTypeToString.get(type, "VPN_TYPE_???");
        return String.format("VpnTransportInfo{%s, '%s'}", typeString, sessionName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeString(sessionName);
    }

    public static final @NonNull Creator<VpnTransportInfo> CREATOR =
            new Creator<VpnTransportInfo>() {
        public VpnTransportInfo createFromParcel(Parcel in) {
            return new VpnTransportInfo(in.readInt(), in.readString());
        }
        public VpnTransportInfo[] newArray(int size) {
            return new VpnTransportInfo[size];
        }
    };
}
