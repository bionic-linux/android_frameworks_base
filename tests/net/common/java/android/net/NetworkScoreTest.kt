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

package android.net

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import com.android.testutils.assertParcelSane
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_SCORE = 80
private const val KEY_DEFAULT_CAPABILITIES = "DEFAULT_CAPABILITIES"

@RunWith(AndroidJUnit4::class)
@SmallTest
class NetworkScoreTest {
    @Test
    fun testParcelNetworkScore() {
        val networkScore = NetworkScore()
        val defaultCap = NetworkCapabilities()
        networkScore.putExtension(KEY_DEFAULT_CAPABILITIES, defaultCap)
        assertEquals(defaultCap, networkScore.getExtension(KEY_DEFAULT_CAPABILITIES))
        networkScore.putExtension(NetworkScore.LEGACY_SCORE, TEST_SCORE)
        assertEquals(TEST_SCORE, networkScore.getIntExtension(NetworkScore.LEGACY_SCORE))
        assertParcelSane(networkScore, 1)
    }

    @Test
    fun testEqualsNetworkScore() {
        val ns1 = NetworkScore()
        val ns2 = NetworkScore()
        assertTrue(ns1.equals(ns2))
        assertEquals(ns1.hashCode(), ns2.hashCode())

        ns1.putExtension(NetworkScore.LEGACY_SCORE, TEST_SCORE)
        assertFalse(ns1.equals(ns2))
        assertNotEquals(ns1.hashCode(), ns2.hashCode())
        ns2.putExtension(NetworkScore.LEGACY_SCORE, TEST_SCORE)
        assertTrue(ns1.equals(ns2))
        assertEquals(ns1.hashCode(), ns2.hashCode())

        val defaultCap = NetworkCapabilities()
        ns1.putExtension(KEY_DEFAULT_CAPABILITIES, defaultCap)
        assertFalse(ns1.equals(ns2))
        assertNotEquals(ns1.hashCode(), ns2.hashCode())
        ns2.putExtension(KEY_DEFAULT_CAPABILITIES, defaultCap)
        assertTrue(ns1.equals(ns2))
        assertEquals(ns1.hashCode(), ns2.hashCode())

        ns1.putExtension(null, 10)
        assertFalse(ns1.equals(ns2))
        assertNotEquals(ns1.hashCode(), ns2.hashCode())
        ns2.putExtension(null, 10)
        assertTrue(ns1.equals(ns2))
        assertEquals(ns1.hashCode(), ns2.hashCode())
    }
}
