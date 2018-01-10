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

import android.annotation.CheckResult;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An immutable parcelable class that represents a USB function gadget mode
 * configuration.
 *
 * {@hide}
 */
public final class UsbFunctions implements Parcelable, Iterable<UsbFunction> {

    private static final UsbFunctions SETTABLE_FUNCTIONS = new UsbFunctions(
            UsbFunction.MTP, UsbFunction.PTP, UsbFunction.RNDIS, UsbFunction.MIDI);

    /**
     * Returns whether the given functions are valid inputs to UsbManager.
     * Currently the empty functions or any of MTP, PTP, RNDIS, MIDI are accepted.
     */
    public static boolean isSettableFunctions(UsbFunctions functions) {
        return functions.size() == 0 || (functions.size() == 1
                && SETTABLE_FUNCTIONS.mFunctions.containsAll(functions.mFunctions));
    }

    public static final Parcelable.Creator<UsbFunctions> CREATOR =
            new Parcelable.Creator<UsbFunctions>() {
        @Override
        public UsbFunctions[] newArray(int size) {
            return new UsbFunctions[size];
        }

        @Override
        public UsbFunctions createFromParcel(Parcel source) {
            UsbFunction[] functions = source.createTypedArray(UsbFunction.CREATOR);
            return new UsbFunctions(functions);
        }
    };

    private final EnumSet<UsbFunction> mFunctions;

    /**
     * Creates a UsbFunctions object containing all the given functions.
     */
    public UsbFunctions(UsbFunction... functions) {
        if (functions.length == 0) {
            mFunctions = EnumSet.noneOf(UsbFunction.class);
        } else if (functions.length == 1) {
            mFunctions = EnumSet.of(functions[0]);
        } else {
            mFunctions = EnumSet.copyOf(Lists.newArrayList(functions));
        }
    }

    /**
     * Creates a UsbFunctions object from a string serialization.
     */
    public UsbFunctions(String functions) {
        mFunctions = EnumSet.noneOf(UsbFunction.class);
        if (functions == null) {
            return;
        }
        for (String function : functions.split(",")) {
            if (UsbFunction.STR_MAP.containsKey(function)) {
                mFunctions.add(UsbFunction.get(function));
            }
        }
    }

    /**
     * Creates a UsbFunctions object from a bitwise AND of individual functions.
     */
    public UsbFunctions(Long functions) {
        mFunctions = EnumSet.noneOf(UsbFunction.class);
        for (UsbFunction f : UsbFunction.values()) {
            if ((functions & f.getCode()) != 0) {
                mFunctions.add(f);
            }
        }

    }

    @Override
    public String toString() {
        List<UsbFunction> sortedFuncs = new ArrayList<>(mFunctions);
        sortedFuncs.sort(Comparator.comparing(UsbFunction::ordinal));
        return sortedFuncs.stream().map(UsbFunction::toString).collect(Collectors.joining(","));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeTypedArray(mFunctions.toArray(new UsbFunction[0]), flags);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof UsbFunctions) {
            return mFunctions.equals(((UsbFunctions) other).mFunctions);
        }
        return false;
    }

    private UsbFunctions copy() {
        UsbFunctions ret = new UsbFunctions();
        ret.mFunctions.addAll(mFunctions);
        return ret;
    }

    /**
     * Returns whether the given function is enabled.
     */
    public boolean contains(UsbFunction f) {
        return mFunctions.contains(f);
    }

    /**
     * Returns the number of functions in this set.
     */
    public int size() {
        return mFunctions.size();
    }

    /**
     * Return a UsbFunctions with the given function added.
     */
    @CheckResult
    public UsbFunctions withFunction(UsbFunction function) {
        UsbFunctions ret = copy();
        ret.mFunctions.add(function);
        return ret;
    }

    /**
     * Return a UsbFunctions with the given function removed.
     */
    @CheckResult
    public UsbFunctions withoutFunction(UsbFunction function) {
        UsbFunctions ret = copy();
        ret.mFunctions.remove(function);
        return ret;
    }

    /**
     * Returns whether these functions are empty.
     */
    public boolean empty() {
        return size() == 0;
    }

    /**
     * Returns a long representation of the functions bitwise OR'd together.
     */
    public long toBitwise() {
        long ret = 0;
        for (UsbFunction f : this) {
            ret |= f.getCode();
        }
        return ret;
    }

    @Override
    public Iterator<UsbFunction> iterator() {
        return mFunctions.iterator();
    }
}
