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

package android.net.util;

import android.platform.test.annotations.AppModeFull
import android.system.NetlinkSocketAddress
import android.system.Os
import android.system.OsConstants
import android.system.OsConstants.*
import android.system.PacketSocketAddress
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SocketUtilsTest {
    @Test
    fun testMakeNetlinkSocketAddress() {
        val nlAddress = SocketUtils.makeNetlinkSocketAddress(123, OsConstants.RTMGRP_NEIGH)
        if (nlAddress is NetlinkSocketAddress) {
            assertEquals(123, nlAddress.getPortId())
            assertEquals(OsConstants.RTMGRP_NEIGH, nlAddress.getGroupsMask())
        } else {
            fail("Not NetlinkSocketAddress object")
        }
    }

    @Test
    fun testMakePacketSocketAddress() {
        val pkAddress = SocketUtils.makePacketSocketAddress(OsConstants.ETH_P_IPV6, 123)
        assertTrue("Not PacketSocketAddress object", pkAddress is PacketSocketAddress)

        val pkAddress2 = SocketUtils.makePacketSocketAddress(123, byteArrayOf(0xff.toByte(),
                0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte()))
        assertTrue("Not PacketSocketAddress object", pkAddress2 is PacketSocketAddress)
    }

    @Test
    @AppModeFull(reason = "Os.socket() cannot create in instant app mode")
    fun testCloseSocket() {
        val fd = Os.socket(AF_INET, SOCK_NONBLOCK, IPPROTO_UDP)
        assertTrue(fd.valid())
        SocketUtils.closeSocket(fd)
        assertFalse(fd.valid())
    }
}
