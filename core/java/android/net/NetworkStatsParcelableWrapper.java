/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.annotation.SystemApi;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * FIXME: revise comments
 * Collection of active network statistics. Can contain summary details across
 * all interfaces, or details with per-UID granularity. Internally stores data
 * as a large table, closely matching {@code /proc/} data format. This structure
 * optimizes for rapid in-memory comparison, but consider using
 * {@link NetworkStatsHistory} when persisting.
 *
 * @hide
 */
@SystemApi
public class NetworkStatsParcelableWrapper {
    // FIXME: 1. efficient add entries. 2. thread safe.
    final ArrayList<Entry> mEntries;

    NetworkStatsParcelableWrapper(int initCapacity) {
        mEntries = new ArrayList<>(initCapacity);
    }

    NetworkStatsParcelableWrapper(@NonNull NetworkStatsParcelable parcel) {
        mEntries = new ArrayList<>(parcel.entries.length);
        for (int i = 0; i < parcel.entries.length; i++) {
            mEntries.add(new Entry(parcel.entries[i]));
        }
    }

    /** @hide */
    @SystemApi
    public static class Entry {
        /** {@link #mUid} value when UID details unavailable. */
        public static final int UID_ALL = -1;
        /**
         * {@link #tag} value matching any tag.
         *
         * @hide
         */
        public static final int TAG_ANY = -1;

        /** {@link #set} value where background data is accounted. */
        public static final int SET_DEFAULT = 0;
        /** {@link #set} value where foreground data is accounted. */
        public static final int SET_FOREGROUND = 1;

        /** {@link #tag} value for total data across all tags. */
        public static final int TAG_NONE = 0;

        /** {@link #metered} value where native, unmetered data is accounted. */
        public static final int METERED_NO = 0;
        /** {@link #metered} value where metered data is accounted. */
        public static final int METERED_YES = 1;

        /** {@link #roaming} value where native, non-roaming data is accounted. */
        public static final int ROAMING_NO = 0;
        /** {@link #roaming} value where roaming data is accounted. */
        public static final int ROAMING_YES = 1;

        /** {@link #onDefaultNetwork} value to account for usage while not the default network. */
        public static final int DEFAULT_NETWORK_NO = 0;
        /** {@link #onDefaultNetwork} value to account for usage while the default network. */
        public static final int DEFAULT_NETWORK_YES = 1;

        /** @hide */
        @Nullable
        final String mIface;
        /** @hide */
        final int mUid;
        /** @hide */
        final int mSet;
        /** @hide */
        final int mTag;
        /** @hide */
        final int mMetered;
        /** @hide */
        final int mRoaming;
        /** @hide */
        final int mDefaultNetwork;
        /** @hide */
        final long mRxBytes;
        /** @hide */
        final long mRxPackets;
        /** @hide */
        final long mTxBytes;
        /** @hide */
        final long mTxPackets;
        /** @hide */
        final long mOperations;

        boolean matchesKey(@NonNull Entry other) {
            return Objects.equals(mIface, other.mIface) && mUid == other.mUid && mSet == other.mSet
                    && mTag == other.mTag && mMetered == other.mMetered
                    && mRoaming == other.mRoaming && mDefaultNetwork == other.mDefaultNetwork;
        }

        void assertMatchesKey(@NonNull Entry other) {
            if (!matchesKey(other)) throw new IllegalArgumentException("key does not match left="
                    + this.toString() + " right=" + other);
        }

        @NonNull Entry add(@NonNull Entry other) {
            assertMatchesKey(other);
            return new Entry(mIface, mUid, mSet, mTag, mMetered, mRoaming, mDefaultNetwork,
                    mRxBytes + other.mRxBytes, mRxPackets + other.mRxPackets,
                    mTxBytes + other.mTxBytes, mTxPackets + other.mTxPackets,
                    mOperations + other.mOperations);
        }

        @NonNull Entry subtract(@NonNull Entry other) {
            assertMatchesKey(other);
            return new Entry(mIface, mUid, mSet, mTag, mMetered, mRoaming, mDefaultNetwork,
                    mRxBytes - other.mRxBytes, mRxPackets - other.mRxPackets,
                    mTxBytes - other.mTxBytes, mTxPackets - other.mTxPackets,
                    mOperations - other.mOperations);
        }

        /** @hide */
        @SystemApi
        public Entry(@Nullable String iface, int uid, int set, int tag, int metered, int roaming,
                int defaultNetwork, long rxBytes, long rxPackets, long txBytes, long txPackets,
                long operations) {
            mIface = iface;
            mUid = uid;
            mSet = set;
            mTag = tag;
            mMetered = metered;
            mRoaming = roaming;
            mDefaultNetwork = defaultNetwork;
            mRxBytes = rxBytes;
            mRxPackets = rxPackets;
            mTxBytes = txBytes;
            mTxPackets = txPackets;
            mOperations = operations;
        }

        Entry(@NonNull NetworkStatsEntryParcelable parcel) {
            mIface = parcel.iface;
            mUid = parcel.uid;
            mSet = parcel.set;
            mTag = parcel.tag;
            mMetered = parcel.metered;
            mRoaming = parcel.roaming;
            mDefaultNetwork = parcel.defaultNetwork;
            mRxBytes = parcel.rxBytes;
            mRxPackets = parcel.rxPackets;
            mTxBytes = parcel.txBytes;
            mTxPackets = parcel.txPackets;
            mOperations = parcel.operations;
        }

        /** @hide */
        @SystemApi
        @NonNull
        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("iface=").append(mIface);
            builder.append(" uid=").append(mUid);
            builder.append(" set=").append(mSet);
            builder.append(" tag=").append(mTag);
            builder.append(" metered=").append(mMetered);
            builder.append(" roaming=").append(mRoaming);
            builder.append(" defaultNetwork=").append(mDefaultNetwork);
            builder.append(" rxBytes=").append(mRxBytes);
            builder.append(" rxPackets=").append(mRxPackets);
            builder.append(" txBytes=").append(mTxBytes);
            builder.append(" txPackets=").append(mTxPackets);
            builder.append(" operations=").append(mOperations);
            return builder.toString();
        }

        @NonNull
        NetworkStatsEntryParcelable toParcelable() {
            final NetworkStatsEntryParcelable parcel = new NetworkStatsEntryParcelable();
            parcel.iface = mIface;
            parcel.uid = mUid;
            parcel.set = mSet;
            parcel.tag = mTag;
            parcel.metered = mMetered;
            parcel.roaming = mRoaming;
            parcel.defaultNetwork = mDefaultNetwork;
            parcel.rxBytes = mRxBytes;
            parcel.rxPackets = mRxPackets;
            parcel.txBytes = mTxBytes;
            parcel.txPackets = mTxPackets;
            parcel.operations = mOperations;
            return parcel;
        }
    }

    /** @hide */
    @SystemApi
    @NonNull
    public NetworkStatsParcelableWrapper combineValues(@NonNull Entry entry) {
        final int index = findIndexThat((o -> o.matchesKey(entry)));
        if (index == -1) {
            // As long as the entry is final, there is no need to get defense copy of the added
            // entry.
            mEntries.add(entry);
        } else {
            mEntries.set(index, mEntries.get(index).add(entry));
        }
        return this;
    }

    /**
     * Find first stats index that fulfills the given predicate.
     */
    int findIndexThat(@NonNull Predicate<Entry> p) {
        for (int i = 0; i < mEntries.size(); i++) {
            if (p.test(mEntries.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find first stats index that fulfills the given predicate, starting
     * search around the hinted index as an optimization.
     */
    int findIndexHintedThat(@NonNull Predicate<Entry> p, int hintIndex) {
        for (int offset = 0; offset < mEntries.size(); offset++) {
            final int halfOffset = offset / 2;

            // search outwards from hint index, alternating forward and backward
            final int i;
            if (offset % 2 == 0) {
                i = (hintIndex + halfOffset) % mEntries.size();
            } else {
                i = (mEntries.size() + hintIndex - halfOffset - 1) % mEntries.size();
            }

            if (p.test(mEntries.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    NetworkStatsParcelable toParcelable() {
        final NetworkStatsParcelable parcel = new NetworkStatsParcelable();
        parcel.entries = new NetworkStatsEntryParcelable[mEntries.size()];
        for (int i = 0; i < mEntries.size(); i++) {
            parcel.entries[i] = mEntries.get(i).toParcelable();
        }
        return parcel;
    }

    // FIXME: javaddoc
    /**
     * Subtract the given {@link NetworkStatsParcelableWrapper}, effectively leaving the delta
     * between two snapshots in time. Assumes that statistics rows collect over
     * time, and that none of them have disappeared. This method does not mutate
     * the referencing object.
     *
     * @return the delta between two objects.
     */
    @NonNull
    public static NetworkStatsParcelableWrapper subtract(
            @NonNull NetworkStatsParcelableWrapper left,
            @NonNull NetworkStatsParcelableWrapper right) {
        final NetworkStatsParcelableWrapper ret =
                new NetworkStatsParcelableWrapper(left.mEntries.size());
        for (int i = 0; i < left.mEntries.size(); i++) {
            final Entry leftEntry = left.mEntries.get(i);
            final int rightIndex = right.findIndexHintedThat((o -> o.matchesKey(leftEntry)), i);
            if (rightIndex == -1) {
                // As long as the entry is final, there is no need to get defense copy of the added
                // entry.
                ret.mEntries.add(leftEntry);
            } else {
                ret.mEntries.add(leftEntry.subtract(right.mEntries.get(rightIndex)));
            }
        }
        return ret;
    }

    // FIXME: javadoc
    public void combineAllValues(@NonNull NetworkStatsParcelableWrapper another) {
        // TODO: Consider provide hint if the other stats is similar.
        for (final Entry entry : another.mEntries) {
            combineValues(entry);
        }
    }
}
