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
import com.android.testutils.assertParcelingIsLossless
import org.junit.Test
import org.junit.runner.RunWith

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

const val TEST_URL = "https://test.example.com/api"
const val TEST_OTHER_URL = "https://other.example.com/api2"

@SmallTest
@RunWith(AndroidJUnit4::class)
class NetworkMetadataTest {
    private val metadata = NetworkMetadata.Builder()
            .setCaptivePortalApiUrl(Uri.parse(TEST_URL))
            .setCaptivePortalData(CaptivePortalData.Builder().setCaptive(true).build())
            .build()

    private fun makeBuilder(): NetworkMetadata.Builder = NetworkMetadata.Builder(metadata)

    @Test
    fun testParcelUnparcel() {
        assertParcelSane(metadata, fieldCount = 2)

        assertParcelingIsLossless(makeBuilder().setCaptivePortalApiUrl(null).build())
        assertParcelingIsLossless(makeBuilder().setCaptivePortalData(null).build())
    }

    @Test
    fun testEquals() {
        assertEquals(metadata, makeBuilder().build())

        assertNotEqualsAfterChange { it.setCaptivePortalApiUrl(Uri.parse(TEST_OTHER_URL)) }
        assertNotEqualsAfterChange { it.setCaptivePortalData(
                CaptivePortalData.Builder().setCaptive(false).build()) }
        assertNotEqualsAfterChange { it.setCaptivePortalApiUrl(null) }
        assertNotEqualsAfterChange { it.setCaptivePortalData(null) }
    }

    private fun assertNotEqualsAfterChange(mutator: (NetworkMetadata.Builder) -> Unit) {
        val newBuilder = makeBuilder().also { mutator(it) }
        assertNotEquals(metadata, newBuilder.build())
    }
}