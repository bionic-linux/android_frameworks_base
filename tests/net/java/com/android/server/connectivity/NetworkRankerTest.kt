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

package com.android.server.connectivity

import android.net.NetworkRequest
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
@SmallTest
class NetworkRankerTest {
    private val ranker = NetworkRanker(mock(NetworkRequest::class.java))

    private fun makeNai(satisfy: Boolean, score: Int) = mock(NetworkAgentInfo::class.java).also {
        doReturn(satisfy).`when`(it).satisfies(any())
        doReturn(score).`when`(it).currentScore
    }

    @Test
    fun testGetBestNetwork() {
        val scores = listOf(20, 50, 90, 60, 23, 68)
        val nais = scores.map { makeNai(true, it) }
        val bestNetwork = nais[2] // The one with the top score
        val someRequest = mock(NetworkRequest::class.java)
        assertEquals(bestNetwork, ranker.getBestNetwork(someRequest, nais))
    }

    @Test
    fun testIgnoreNonSatisfying() {
        val nais = listOf(makeNai(true, 20), makeNai(true, 50), makeNai(false, 90),
                makeNai(false, 60), makeNai(true, 23), makeNai(false, 68))
        val bestNetwork = nais[1] // Top score that's satisfying
        val someRequest = mock(NetworkRequest::class.java)
        assertEquals(bestNetwork, ranker.getBestNetwork(someRequest, nais))
    }

    @Test
    fun testNoMatch() {
        val nais = listOf(makeNai(false, 20), makeNai(false, 50), makeNai(false, 90))
        val someRequest = mock(NetworkRequest::class.java)
        assertNull(ranker.getBestNetwork(someRequest, nais))
    }

    @Test
    fun testEmpty() {
        val someRequest = mock(NetworkRequest::class.java)
        assertNull(ranker.getBestNetwork(someRequest, emptyList()))
    }
}
