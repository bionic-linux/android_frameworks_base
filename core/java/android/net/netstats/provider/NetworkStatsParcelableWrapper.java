/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.net.netstats.provider;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.NetworkStats;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Collection of active network statistics. Used by external modules to report statistics accounted
 * by their module. Can contain summary details across all interfaces, or details with per-UID
 * granularity.
 * @hide
 */
@SystemApi
public class NetworkStatsParcelableWrapper {
    // TODO: make this thread safe.
    private final ArrayList<Entry> mEntries;

    public NetworkStatsParcelableWrapper(int initCapacity) {
        mEntries = new ArrayList<>(initCapacity);
    }

    /** @hide */
    public NetworkStatsParcelableWrapper(@NonNull NetworkStatsParcelable parcel) {
        mEntries = new ArrayList<>(parcel.entries.length);
        for (int i = 0; i < parcel.entries.length; i++) {
            mEntries.add(new Entry(parcel.entries[i]));
        }
    }

    /**
     * A record entry of network statistics.
     */
    public static class Entry {
        /**
         * Virtual network interface for video telephony. This is for VT data usage counting
         * purpose.
         */
        public static final String IFACE_VT = "vt_data0";

        /** {@link #mUid} value when UID details unavailable. */
        public static final int UID_ALL = -1;
        /** Special UID value for data usage by tethering. */
        public static final int UID_TETHERING = -5;

        /** {@link #mSet} value where background data is accounted. */
        public static final int SET_DEFAULT = 0;
        /** {@link #mSet} value where foreground data is accounted. */
        public static final int SET_FOREGROUND = 1;

        /** {@link #mTag} value for total data across all tags. */
        public static final int TAG_NONE = 0;

        /** {@link #mMetered} value where native, unmetered data is accounted. */
        public static final int METERED_NO = 0;
        /** {@link #mMetered} value where metered data is accounted. */
        public static final int METERED_YES = 1;

        /** {@link #mRoaming} value where native, non-roaming data is accounted. */
        public static final int ROAMING_NO = 0;
        /** {@link #mRoaming} value where roaming data is accounted. */
        public static final int ROAMING_YES = 1;

        /** {@link #mDefaultNetwork} value to account for usage while not the default network. */
        public static final int DEFAULT_NETWORK_NO = 0;
        /** {@link #mDefaultNetwork} value to account for usage while the default network. */
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
                    + this + " right=" + other);
        }

        boolean isEmpty() {
            return mRxBytes == 0 && mRxPackets == 0 && mTxBytes == 0 && mTxPackets == 0
                    && mOperations == 0;
        }

        @NonNull
        Entry add(@NonNull Entry other) {
            assertMatchesKey(other);
            return new Entry(mIface, mUid, mSet, mTag, mMetered, mRoaming, mDefaultNetwork,
                    mRxBytes + other.mRxBytes, mRxPackets + other.mRxPackets,
                    mTxBytes + other.mTxBytes, mTxPackets + other.mTxPackets,
                    mOperations + other.mOperations);
        }

        @NonNull
        Entry subtract(@NonNull Entry other) {
            assertMatchesKey(other);
            return new Entry(mIface, mUid, mSet, mTag, mMetered, mRoaming, mDefaultNetwork,
                    mRxBytes - other.mRxBytes, mRxPackets - other.mRxPackets,
                    mTxBytes - other.mTxBytes, mTxPackets - other.mTxPackets,
                    mOperations - other.mOperations);
        }

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

        /** @hide */
        public Entry(@NonNull NetworkStatsEntryParcelable parcel) {
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

        /** @hide */
        @NonNull
        public NetworkStatsEntryParcelable toParcelable() {
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

        /** @hide */
        @NonNull
        public NetworkStats.Entry toNetworkStatsEntry() {
            return new NetworkStats.Entry(mIface, mUid, mSet, mTag,
                    mMetered, mRoaming, mDefaultNetwork, mRxBytes, mRxPackets, mTxBytes, mTxPackets,
                    mOperations);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return mUid == entry.mUid
                    && mSet == entry.mSet
                    && mTag == entry.mTag
                    && mMetered == entry.mMetered
                    && mRoaming == entry.mRoaming
                    && mDefaultNetwork == entry.mDefaultNetwork
                    && mRxBytes == entry.mRxBytes
                    && mRxPackets == entry.mRxPackets
                    && mTxBytes == entry.mTxBytes
                    && mTxPackets == entry.mTxPackets
                    && mOperations == entry.mOperations
                    && Objects.equals(mIface, entry.mIface);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mIface, mUid, mSet, mTag, mMetered, mRoaming, mDefaultNetwork,
                    mRxBytes, mRxPackets, mTxBytes, mTxPackets, mOperations);
        }
    }

    /** @hide */
    @VisibleForTesting
    @NonNull
    public NetworkStatsParcelableWrapper addValues(@NonNull Entry entry) {
        mEntries.add(entry);
        return this;
    }

    /**
     * Combine given values with an existing row, or create a new row if
     * {@link #findIndexThat(Predicate)} is unable to find match. Can also be used to subtract
     * values from existing rows. This method mutates the referencing
     * {@link NetworkStatsParcelableWrapper} object.
     *
     * @param entry the {@link Entry} to combine.
     * @return a reference to this mutated {@link NetworkStatsParcelableWrapper} object.
     */
    @NonNull
    public NetworkStatsParcelableWrapper combineValues(@NonNull Entry entry) {
        // Skip if the entry only contains zeros.
        if (entry.isEmpty()) return this;

        final int index = findIndexThat((o -> o.matchesKey(entry)));
        if (index == -1) {
            // As long as the entry is final, there is no need to get defense copy of the added
            // entry.
            addValues(entry);
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

    /** @hide */
    @NonNull
    public NetworkStatsParcelable toParcelable() {
        final NetworkStatsParcelable parcel = new NetworkStatsParcelable();
        parcel.entries = new NetworkStatsEntryParcelable[mEntries.size()];
        for (int i = 0; i < mEntries.size(); i++) {
            parcel.entries[i] = mEntries.get(i).toParcelable();
        }
        return parcel;
    }

    /** @hide */
    @NonNull
    public NetworkStats toNetworkStats() {
        final NetworkStats stats = new NetworkStats(0L, mEntries.size());
        for (final Entry entry : mEntries) {
            stats.combineValues(entry.toNetworkStatsEntry());
        }
        return stats;
    }

    @NonNull
    static NetworkStatsParcelableWrapper subtract(
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
                ret.addValues(leftEntry);
            } else {
                final Entry subtracted = leftEntry.subtract(right.mEntries.get(rightIndex));
                // Skip empty entries if any.
                if (!subtracted.isEmpty()) ret.addValues(subtracted);
            }
        }
        return ret;
    }

    /**
     * Subtract the given {@link NetworkStatsParcelableWrapper}, effectively leaving the delta
     * between two snapshots in time. Assumes that statistics rows collect over time, and that none
     * of them have disappeared. This method does not mutate the referencing object.
     *
     * @return the delta between two objects.
     */
    @NonNull
    public NetworkStatsParcelableWrapper subtract(@NonNull NetworkStatsParcelableWrapper right) {
        return subtract(this, right);
    }

    /**
     * Combine all values from another {@link NetworkStatsParcelableWrapper} into this object.
     */
    public void combineAllValues(@NonNull NetworkStatsParcelableWrapper another) {
        for (final Entry entry : another.mEntries) {
            combineValues(entry);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkStatsParcelableWrapper that = (NetworkStatsParcelableWrapper) o;
        if (mEntries.size() != that.mEntries.size()) return false;

        // Compare entries which might be in different position.
        for (int i = 0; i < mEntries.size(); i++) {
            final Entry leftEntry = mEntries.get(i);
            final int j = that.findIndexHintedThat(right -> right.matchesKey(leftEntry), i);
            if (j == -1) return false;
            if (!leftEntry.equals(that.mEntries.get(j))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mEntries);
    }

    /** @hide */
    @VisibleForTesting
    public int size() {
        return mEntries.size();
    }

    /** @hide */
    @VisibleForTesting
    public void clear() {
        mEntries.clear();
    }

    @NonNull
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<NetworkStats:\n");
        for (int i = 0; i < mEntries.size(); i++) {
            sb.append("  [" + i + "]" + mEntries.get(i) + "\n");
        }
        sb.append(">");
        return sb.toString();
    }
}
