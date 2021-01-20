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

package android.net

import android.net.NetworkScore.POLICY_ACCEPT_UNVALIDATED
import android.net.NetworkScore.POLICY_IS_VALIDATED
import android.net.NetworkScore.POLICY_IS_VPN
import android.net.NetworkScore.POLICY_ONCE_CHOSEN_BY_USER
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@SmallTest
class NetworkScoreTest {
    // Convenience methods
    fun NetworkScore.withPolicy(
        validated: Boolean = false,
        vpn: Boolean = false,
        onceChosen: Boolean = false,
        acceptUnvalidated: Boolean = false
    ) =
            withPolicyBits(validated, vpn, onceChosen, acceptUnvalidated)
    fun makeScore(legacyInt: Int) = NetworkScore.Builder().setLegacyInt(legacyInt).build()

    @Test
    fun testGetLegacyInt() {
        val ns = makeScore(50)
        assertEquals(10, ns.legacyInt) // -40 penalty for not being validated
        assertEquals(50, ns.legacyIntAsValidated)

        val vpnNs = makeScore(101).withPolicy(vpn = true)
        assertEquals(101, vpnNs.legacyInt) // VPNs are not subject to unvalidation penalty
        assertEquals(101, vpnNs.legacyIntAsValidated)
        assertEquals(101, vpnNs.withPolicy(validated = true).legacyInt)
        assertEquals(101, vpnNs.withPolicy(validated = true).legacyIntAsValidated)

        val validatedNs = ns.withPolicy(validated = true)
        assertEquals(50, validatedNs.legacyInt) // No penalty, this is validated
        assertEquals(50, validatedNs.legacyIntAsValidated)

        val chosenNs = ns.withPolicy(onceChosen = true)
        assertEquals(10, chosenNs.legacyInt)
        assertEquals(100, chosenNs.legacyIntAsValidated)
        assertEquals(10, chosenNs.withPolicy(acceptUnvalidated = true).legacyInt)
        assertEquals(50, chosenNs.withPolicy(acceptUnvalidated = true).legacyIntAsValidated)
    }

    @Test
    fun testToString() {
        val string = makeScore(10).withPolicy(vpn = true, acceptUnvalidated = true).toString()
        assertTrue(string.contains("Score(10"), string)
        assertTrue(string.contains("ACCEPT_UNVALIDATED"), string)
        assertTrue(string.contains("IS_VPN"), string)
        assertFalse(string.contains("IS_VALIDATED"), string)
    }

    @Test
    fun testHasPolicy() {
        val ns = makeScore(50)
        assertFalse(ns.hasPolicy(POLICY_IS_VALIDATED))
        assertFalse(ns.hasPolicy(POLICY_IS_VPN))
        assertFalse(ns.hasPolicy(POLICY_ONCE_CHOSEN_BY_USER))
        assertFalse(ns.hasPolicy(POLICY_ACCEPT_UNVALIDATED))
        assertTrue(ns.withPolicy(validated = true).hasPolicy(POLICY_IS_VALIDATED))
        assertTrue(ns.withPolicy(vpn = true).hasPolicy(POLICY_IS_VPN))
        assertTrue(ns.withPolicy(onceChosen = true).hasPolicy(POLICY_ONCE_CHOSEN_BY_USER))
        assertTrue(ns.withPolicy(acceptUnvalidated = true).hasPolicy(POLICY_ACCEPT_UNVALIDATED))
    }
}
