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

package com.android.server.connectivity.tethering

import android.net.LinkAddress
import android.net.MacAddress
import android.net.TetheredClient
import android.net.TetheredClient.AddressInfo
import android.net.TetheringManager.TETHERING_USB
import android.net.TetheringManager.TETHERING_WIFI
import android.net.ip.IpServer
import android.net.wifi.WifiClient
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@SmallTest
class ConnectedClientsTrackerTest {

    private val server1 = mock(IpServer::class.java)
    private val server2 = mock(IpServer::class.java)
    private val servers = listOf(server1, server2)

    private val clock = TestClock(1324L)

    private val client1Addr = MacAddress.fromString("01:23:45:67:89:0A")
    private val client1 = TetheredClient(client1Addr, listOf(
            AddressInfo(LinkAddress("192.168.43.44/32"), null /* hostname */, clock.time + 20)),
            TETHERING_WIFI)
    private val client2Addr = MacAddress.fromString("02:34:56:78:90:AB")
    private val client2 = TetheredClient(client2Addr, listOf(
            AddressInfo(LinkAddress("192.168.43.45/32"), "my_hostname", clock.time + 30),
            AddressInfo(LinkAddress("2001:db8:12::34/72"), "other_hostname", clock.time + 10)),
            TETHERING_WIFI)
    private val client3Addr = MacAddress.fromString("03:45:67:89:0A:BC")
    private val client3 = TetheredClient(client3Addr,
            listOf(AddressInfo(LinkAddress("2001:db8:34::34/72"), "other_other_hostname",
                    clock.time + 10)),
            TETHERING_USB)

    @Test
    fun testUpdateConnectedClients() {
        doReturn(emptyList<TetheredClient>()).`when`(server1).allLeases
        doReturn(emptyList<TetheredClient>()).`when`(server2).allLeases

        val tracker = ConnectedClientsTracker(clock)
        assertEquals(emptyList(), tracker.updateConnectedClients(servers, null))

        // Obtain a lease for client 1
        doReturn(listOf(client1)).`when`(server1).allLeases
        assertSameClients(listOf(client1), tracker.updateConnectedClients(servers, null))

        // Client 2 L2-connected, no lease yet
        val client2WithoutAddr = TetheredClient(client2Addr, emptyList(), TETHERING_WIFI)
        assertSameClients(listOf(client1, client2WithoutAddr),
                tracker.updateConnectedClients(servers, listOf(WifiClient(client2Addr))))

        // Client 2 lease obtained
        doReturn(listOf(client1, client2)).`when`(server1).allLeases
        assertSameClients(listOf(client1, client2),
                tracker.updateConnectedClients(servers, listOf(WifiClient(client2Addr))))

        // Client 3 lease obtained
        doReturn(listOf(client3)).`when`(server2).allLeases
        assertSameClients(listOf(client1, client2, client3),
                tracker.updateConnectedClients(servers, listOf(WifiClient(client2Addr))))

        // Client 2 L2-disconnected
        assertSameClients(listOf(client1, client3),
                tracker.updateConnectedClients(servers, emptyList()))

        // Client 1 L2-connected: no change
        assertSameClients(listOf(client1, client3),
                tracker.updateConnectedClients(servers, listOf(WifiClient(client1Addr))))

        // Client 1 L2-disconnected: lost
        assertSameClients(listOf(client3),
                tracker.updateConnectedClients(servers, emptyList()))

        // Client 1 comes back
        assertSameClients(listOf(client1, client3),
                tracker.updateConnectedClients(servers, listOf(WifiClient(client1Addr))))

        // Leases lost, client 1 still L2-connected
        doReturn(emptyList<TetheredClient>()).`when`(server1).allLeases
        doReturn(emptyList<TetheredClient>()).`when`(server2).allLeases
        assertSameClients(listOf(TetheredClient(client1Addr, emptyList(), TETHERING_WIFI)),
                tracker.updateConnectedClients(servers, listOf(WifiClient(client1Addr))))
    }

    @Test
    fun testUpdateConnectedClients_LeaseExpiration() {
        val tracker = ConnectedClientsTracker(clock)
        doReturn(listOf(client1, client2)).`when`(server1).allLeases
        doReturn(listOf(client3)).`when`(server2).allLeases
        assertSameClients(listOf(client1, client2, client3),
                tracker.updateConnectedClients(servers, emptyList()))

        clock.time += 20
        // Client 3 has no remaining lease: removed
        val expectedClients = listOf(
                // Client 1 has no remaining lease but is L2-connected
                TetheredClient(client1Addr, emptyList(), TETHERING_WIFI),
                // Client 2 has some expired leases
                TetheredClient(
                        client2Addr,
                        client2.addresses.filter { it.expirationTime > clock.time }.toList(),
                        TETHERING_WIFI))
        assertSameClients(expectedClients,
                tracker.updateConnectedClients(servers, listOf(WifiClient(client1Addr))))
    }

    private fun assertSameClients(expected: List<TetheredClient>, actual: List<TetheredClient>) {
        val expectedSet = HashSet(expected)
        assertEquals(expected.size, expectedSet.size)
        assertEquals(expectedSet, HashSet(actual))
    }

    private class TestClock(var time: Long) : ConnectedClientsTracker.Clock() {
        override fun elapsedRealtime(): Long {
            return time
        }
    }
}