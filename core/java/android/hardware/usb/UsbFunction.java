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

package android.hardware.usb;

import android.hardware.usb.gadget.V1_0.GadgetFunction;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A parcelable enum that represents single functions that can combine to make a gadget
 * mode configuration.
 *
 * {@hide}
 */
public enum UsbFunction implements Parcelable {
    // The order of declaration controls the ordering of UsbFunctions#toString()
    // It must remain the same to maintain init script compatibility.
    MTP(GadgetFunction.MTP),
    PTP(GadgetFunction.PTP),
    RNDIS(GadgetFunction.RNDIS),
    MIDI(GadgetFunction.MIDI),
    ACCESSORY(GadgetFunction.ACCESSORY),
    AUDIO_SOURCE(GadgetFunction.AUDIO_SOURCE),
    ADB(GadgetFunction.ADB);

    private long mCode;

    UsbFunction(long code) {
        mCode = code;
    }

    static final Map<Long, UsbFunction> INT_MAP = Arrays.stream(UsbFunction.values())
            .collect(Collectors.toMap(UsbFunction::getCode, Function.identity()));

    static final Map<String, UsbFunction> STR_MAP = Arrays.stream(UsbFunction.values())
            .collect(Collectors.toMap(UsbFunction::toString, Function.identity()));

    /**
     * Gets the UsbFunction represented by the given string, or null if invalid.
     */
    public static UsbFunction get(String string) {
        return STR_MAP.get(string);
    }

    public static final Creator<UsbFunction> CREATOR =
            new Creator<UsbFunction>() {
                @Override
                public UsbFunction[] newArray(int size) {
                    return new UsbFunction[size];
                }

                @Override
                public UsbFunction createFromParcel(Parcel source) {
                    return INT_MAP.get(source.readLong());
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    long getCode() {
        return mCode;
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(getCode());
    }
}
