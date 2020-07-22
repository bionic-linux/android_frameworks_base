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
 * limitations under the License
 */

package com.android.server.net.integrationtests

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.DnsResolver
import android.net.DnsResolver.TYPE_A
import android.net.DnsResolver.TYPE_AAAA
import android.net.INetworkMonitorCallbacks
import android.net.InetAddresses
import android.net.Network
import android.net.metrics.IpConnectivityLog
import android.net.util.SharedLog
import android.os.IBinder
import com.android.networkstack.netlink.TcpSocketTracker
import com.android.networkstack.util.DnsUtils.PRIVATE_DNS_PROBE_HOST_SUFFIX
import com.android.server.NetworkStackService
import com.android.server.NetworkStackService.NetworkMonitorConnector
import com.android.server.NetworkStackService.NetworkStackConnector
import com.android.server.connectivity.NetworkMonitor
import com.android.server.net.integrationtests.NetworkStackInstrumentationService.InstrumentationConnector
import com.android.testutils.FakeDns
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.URLConnection
import java.nio.charset.StandardCharsets

private const val TEST_NETID = 42

/**
 * Android service that can return an [android.net.INetworkStackConnector] which can be instrumented
 * through [NetworkStackInstrumentationService].
 * Useful in tests to create test instrumented NetworkStack components that can receive
 * instrumentation commands through [NetworkStackInstrumentationService].
 */
class TestNetworkStackService : Service() {
    override fun onBind(intent: Intent): IBinder = TestNetworkStackConnector(makeTestContext())

    private fun makeTestContext() = spy(applicationContext).also {
        doReturn(mock(IBinder::class.java)).`when`(it).getSystemService(Context.NETD_SERVICE)
    }

    private class TestPermissionChecker : NetworkStackService.PermissionChecker() {
        override fun enforceNetworkStackCallingPermission() = Unit
    }

    private class NetworkMonitorDeps(
        private val privateDnsBypassNetwork: Network,
        private val resolver: DnsResolver
    ) : NetworkMonitor.Dependencies() {
        override fun getPrivateDnsBypassNetwork(network: Network?) = privateDnsBypassNetwork
        override fun sendNetworkConditionsBroadcast(context: Context, broadcast: Intent) = Unit
        override fun getDnsResolver() = resolver
    }

    private inner class TestNetworkStackConnector(context: Context) : NetworkStackConnector(
            context, TestPermissionChecker(), NetworkStackService.Dependencies()) {

        private val network = TestNetwork(TEST_NETID)
        private val privateDnsBypassNetwork = TestPrivateDnsBypassNetwork(TEST_NETID)
        private val mockDnsResolver = mock(DnsResolver::class.java)
        private val fakeDns = FakeDns(mockDnsResolver).apply { startMocking() }

        private inner class TestNetwork(netId: Int) : Network(netId) {
            override fun getAllByName(host: String): Array<InetAddress> {
                return arrayOf(InetAddresses.parseNumericAddress("1.2.3.4"))
            }
        }

        private inner class TestPrivateDnsBypassNetwork(netId: Int) : Network(netId) {
            override fun openConnection(url: URL): URLConnection {
                val response = InstrumentationConnector.processRequest(url)
                val responseBytes = response.content.toByteArray(StandardCharsets.UTF_8)

                val connection = mock(HttpURLConnection::class.java)
                doReturn(response.responseCode).`when`(connection).responseCode
                doReturn(responseBytes.size.toLong()).`when`(connection).contentLengthLong
                doReturn(response.redirectUrl).`when`(connection).getHeaderField("location")
                doReturn(ByteArrayInputStream(responseBytes)).`when`(connection).inputStream
                return connection
            }
        }

        override fun makeNetworkMonitor(
            network: Network,
            name: String?,
            cb: INetworkMonitorCallbacks
        ) {
            addTestMockDnsResponse()
            val nm = NetworkMonitor(this@TestNetworkStackService, cb,
                    this.network,
                    mock(IpConnectivityLog::class.java), mock(SharedLog::class.java),
                    mock(NetworkStackService.NetworkStackServiceManager::class.java),
                    NetworkMonitorDeps(privateDnsBypassNetwork, mockDnsResolver),
                    mock(TcpSocketTracker::class.java))
            cb.onNetworkMonitorCreated(NetworkMonitorConnector(nm, TestPermissionChecker()))
        }

        private fun addTestMockDnsResponse() {
            fakeDns.setAnswer("secure.test.android.com", arrayOf("192.168.0.1"), TYPE_A)
            fakeDns.setAnswer("test.android.com", arrayOf("192.168.0.2"), TYPE_A)
            fakeDns.setAnswer(TEST_PRIVATE_DNS_HOST_NAME, arrayOf("192.168.0.3"), TYPE_A)
            fakeDns.setAnswer(TEST_PRIVATE_DNS_HOST_NAME, arrayOf("2001:db8::1"), TYPE_AAAA)
            fakeDns.setAnswer(PRIVATE_DNS_PROBE_HOST_SUFFIX, arrayOf("192.168.0.4"), TYPE_A)
        }
    }
}