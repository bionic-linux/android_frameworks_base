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

package android.net.netstats.provider

import android.net.NetworkStats
import android.net.netstats.provider.NetworkStatsParcelableWrapper.Entry
import android.net.netstats.provider.NetworkStatsParcelableWrapper.Entry.DEFAULT_NETWORK_NO
import android.net.netstats.provider.NetworkStatsParcelableWrapper.Entry.DEFAULT_NETWORK_YES
import android.net.netstats.provider.NetworkStatsParcelableWrapper.Entry.IFACE_VT
import android.net.netstats.provider.NetworkStatsParcelableWrapper.Entry.METERED_NO
import android.net.netstats.provider.NetworkStatsParcelableWrapper.Entry.METERED_YES
import android.net.netstats.provider.NetworkStatsParcelableWrapper.Entry.ROAMING_NO
import android.net.netstats.provider.NetworkStatsParcelableWrapper.Entry.ROAMING_YES
import android.net.netstats.provider.NetworkStatsParcelableWrapper.Entry.SET_DEFAULT
import android.net.netstats.provider.NetworkStatsParcelableWrapper.Entry.SET_FOREGROUND
import android.net.netstats.provider.NetworkStatsParcelableWrapper.Entry.TAG_NONE
import android.net.netstats.provider.NetworkStatsParcelableWrapper.subtract
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals

/**
 * Tests for {@link android.net.netstats.provider.NetworkStatsParcelableWrapper}.
 */
@RunWith(JUnit4::class)
@SmallTest
class NetworkStatsParcelableWrapperTest {
    companion object {
        const val TEST_IFACE = "test0"
        const val TEST_UID1 = 1001
        const val TEST_UID2 = 1002
        val TEST_STATS_SET1 = arrayOf(Entry(TEST_IFACE, TEST_UID1, SET_DEFAULT, TAG_NONE,
                METERED_NO, ROAMING_NO, DEFAULT_NETWORK_NO, 101, 2, 103, 4, 5),
                Entry(TEST_IFACE, TEST_UID1, Entry.SET_DEFAULT, Entry.TAG_NONE,
                        METERED_NO, ROAMING_NO, DEFAULT_NETWORK_YES, 20, 3, 57, 40, 3),
                Entry(TEST_IFACE, TEST_UID1, SET_DEFAULT, TAG_NONE,
                        METERED_NO, ROAMING_YES, DEFAULT_NETWORK_NO, 31, 7, 24, 5, 8),
                Entry(TEST_IFACE, TEST_UID1, SET_DEFAULT, TAG_NONE, METERED_YES,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 25, 3, 47, 8, 2),
                Entry(TEST_IFACE, TEST_UID1, SET_DEFAULT, 0x80, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 17, 2, 11, 1, 0),
                Entry(TEST_IFACE, TEST_UID1, SET_FOREGROUND, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 37, 52, 1, 10, 4),
                Entry(TEST_IFACE, TEST_UID2, SET_DEFAULT, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 40, 1, 0, 0, 8),
                Entry(IFACE_VT, TEST_UID1, SET_DEFAULT, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 3, 1, 6, 2, 0))
        val TEST_STATS_SET2 = arrayOf(Entry(TEST_IFACE, TEST_UID1, SET_DEFAULT, 0x80, METERED_NO,
                ROAMING_NO, DEFAULT_NETWORK_NO, 3, 15, 2, 31, 1),
                Entry(TEST_IFACE, TEST_UID1, SET_FOREGROUND, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 13, 61, 10, 1, 45),
                Entry(TEST_IFACE, TEST_UID2, SET_DEFAULT, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 11, 2, 3, 4, 7),
                Entry(IFACE_VT, TEST_UID1, SET_DEFAULT, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 4, 3, 2, 1, 0),
                Entry(IFACE_VT, TEST_UID2, SET_DEFAULT, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 2, 3, 7, 8, 0))

        val TEST_STATS_EMPTY = NetworkStatsParcelableWrapper(0)
        val TEST_STATS1 = NetworkStatsParcelableWrapper(TEST_STATS_SET1.size)

        // STATS2 has some rows with common key with STATS1
        val TEST_STATS2 = NetworkStatsParcelableWrapper(TEST_STATS_SET2.size)

        // STATS3 = STATS1 + STATS2
        val TEST_STATS3 = NetworkStatsParcelableWrapper(9)
    }

    @Before
    fun setUp() {
        TEST_STATS_SET1.forEach {
            TEST_STATS1.addValues(it)
        }

        assertEquals(8, TEST_STATS1.size())

        TEST_STATS_SET2.forEach {
            TEST_STATS2.addValues(it)
        }

        assertEquals(5, TEST_STATS2.size())

        TEST_STATS3.addValues(Entry(TEST_IFACE, TEST_UID1, SET_DEFAULT, TAG_NONE, METERED_NO,
                ROAMING_NO, DEFAULT_NETWORK_NO, 101, 2, 103, 4, 5))
                .addValues(Entry(TEST_IFACE, TEST_UID1, Entry.SET_DEFAULT, Entry.TAG_NONE,
                        METERED_NO, ROAMING_NO, DEFAULT_NETWORK_YES, 20, 3, 57, 40, 3))
                .addValues(Entry(TEST_IFACE, TEST_UID1, SET_DEFAULT, TAG_NONE,
                        METERED_NO, ROAMING_YES, DEFAULT_NETWORK_NO, 31, 7, 24, 5, 8))
                .addValues(Entry(TEST_IFACE, TEST_UID1, SET_DEFAULT, TAG_NONE, METERED_YES,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 25, 3, 47, 8, 2))
                .addValues(Entry(TEST_IFACE, TEST_UID1, SET_DEFAULT, 0x80, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 20, 17, 13, 32, 1))
                .addValues(Entry(TEST_IFACE, TEST_UID1, SET_FOREGROUND, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 50, 113, 11, 11, 49))
                .addValues(Entry(TEST_IFACE, TEST_UID2, SET_DEFAULT, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 51, 3, 3, 4, 15))
                .addValues(Entry(IFACE_VT, TEST_UID1, SET_DEFAULT, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 7, 4, 8, 3, 0))
                .addValues(Entry(IFACE_VT, TEST_UID2, SET_DEFAULT, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 2, 3, 7, 8, 0))
        assertEquals(9, TEST_STATS3.size())
    }

    @After
    fun tearDown() {
        TEST_STATS1.clear()
        TEST_STATS2.clear()
        TEST_STATS3.clear()
    }

    @Test
    fun testCombineValues() {
        val stats = NetworkStatsParcelableWrapper(0)
        assertEquals(TEST_STATS_EMPTY, stats)

        TEST_STATS_SET1.forEach {
            stats.combineValues(it)
        }
        assertEquals(TEST_STATS1, stats)
        TEST_STATS_SET2.forEach {
            stats.combineValues(it)
        }
        // EMPTY + STATS1 + STATS2 = STATS3
        assertEquals(TEST_STATS3, stats)
    }

    @Test
    fun testCombineAllValues() {
        val stats = NetworkStatsParcelableWrapper(0)
        assertEquals(TEST_STATS_EMPTY, stats)
        stats.combineAllValues(TEST_STATS2)
        assertEquals(TEST_STATS2, stats)
        stats.combineAllValues(TEST_STATS1)
        // EMPTY + STATS2 + STATS1 = STATS3
        assertEquals(TEST_STATS3, stats)
    }

    @Test
    fun testParcelUnparcel() {
        assertEquals(TEST_STATS_EMPTY,
                NetworkStatsParcelableWrapper(TEST_STATS_EMPTY.toParcelable()))
        assertEquals(TEST_STATS1, NetworkStatsParcelableWrapper(TEST_STATS1.toParcelable()))
        assertEquals(TEST_STATS2, NetworkStatsParcelableWrapper(TEST_STATS2.toParcelable()))
        assertEquals(TEST_STATS3, NetworkStatsParcelableWrapper(TEST_STATS3.toParcelable()))
    }

    /**
     * Verifies that the entries can be converted to {@link NetworkStats.Entry}.
     */
    @Test
    fun testToNetworkStatsEntry() {
        val set2StatsEntry = arrayOf(NetworkStats.Entry(TEST_IFACE, TEST_UID1, SET_DEFAULT, 0x80,
                METERED_NO, ROAMING_NO, DEFAULT_NETWORK_NO, 3, 15, 2, 31, 1),
                NetworkStats.Entry(TEST_IFACE, TEST_UID1, SET_FOREGROUND, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 13, 61, 10, 1, 45),
                NetworkStats.Entry(TEST_IFACE, TEST_UID2, SET_DEFAULT, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 11, 2, 3, 4, 7),
                NetworkStats.Entry(IFACE_VT, TEST_UID1, SET_DEFAULT, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 4, 3, 2, 1, 0),
                NetworkStats.Entry(IFACE_VT, TEST_UID2, SET_DEFAULT, TAG_NONE, METERED_NO,
                        ROAMING_NO, DEFAULT_NETWORK_NO, 2, 3, 7, 8, 0))

        for (i in 0..TEST_STATS_SET2.size - 1) {
            assertEquals(set2StatsEntry[i], TEST_STATS_SET2[i].toNetworkStatsEntry())
        }
    }

    /**
     * Verifies that the statistics can be converted to {@link NetworkStats}.
     */
    @Test
    fun testToNetworkStats() {
        val expected = NetworkStats(0L, 0)
        TEST_STATS_SET1.forEach {
            // Assumes toNetworkStatsEntry is correct through testToNetworkStatsEntry.
            expected.combineValues(it.toNetworkStatsEntry())
        }
        assertEquals(TEST_STATS_SET1.size, expected.size())
        val actual = TEST_STATS1.toNetworkStats()
        assertEquals(TEST_STATS_SET1.size, actual.size())
        for (i in 0..TEST_STATS_SET1.size - 1) {
            assertEquals(expected.getValues(i, null), actual.getValues(i, null))
        }
    }

    @Test
    fun testSubtract() {
        // STATS3 - STATS2 = STATS1
        assertEquals(TEST_STATS1, subtract(TEST_STATS3, TEST_STATS2))
        // STATS3 - STATS1 = STATS2
        assertEquals(TEST_STATS2, subtract(TEST_STATS3, TEST_STATS1))
    }
}
