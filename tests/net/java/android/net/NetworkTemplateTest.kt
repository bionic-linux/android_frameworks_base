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

package android.net

import android.content.Context
import android.net.ConnectivityManager.TYPE_MOBILE
import android.net.ConnectivityManager.TYPE_WIFI
import android.net.NetworkIdentity.SUBTYPE_COMBINED
import android.net.NetworkIdentity.buildNetworkIdentity
import android.net.NetworkStats.DEFAULT_NETWORK_ALL
import android.net.NetworkStats.METERED_ALL
import android.net.NetworkStats.ROAMING_ALL
import android.net.NetworkTemplate.MATCH_MOBILE
import android.net.NetworkTemplate.MATCH_WIFI
import android.net.NetworkTemplate.NETWORK_TYPE_5G_NSA
import android.net.NetworkTemplate.NETWORK_TYPE_ALL
import android.net.NetworkTemplate.SSID_YES
import android.net.NetworkTemplate.SUBSCRIBER_ID_YES
import android.net.NetworkTemplate.buildTemplateWifi
import android.net.NetworkTemplate.buildTemplateCarrierWildcard
import android.net.NetworkTemplate.buildTemplateMobileWithRatType
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import com.android.testutils.assertParcelSane
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val TEST_IMSI1 = "imsi1"
private const val TEST_IMSI2 = "imsi2"
private const val TEST_SSID1 = "ssid1"
private const val TEST_SSID2 = "ssid2"

@RunWith(JUnit4::class)
class NetworkTemplateTest {
    private val mockContext = mock(Context::class.java)
    private val mockWifiManager = mock(WifiManager::class.java)

    private fun buildMobileNetworkState(subscriberId: String): NetworkState =
            buildNetworkState(TYPE_MOBILE, subscriberId = subscriberId)
    private fun buildWifiNetworkState(subscriberId: String?, ssid: String?): NetworkState =
            buildNetworkState(TYPE_WIFI, subscriberId = subscriberId, ssid = ssid)

    private fun buildNetworkState(
        type: Int,
        subscriberId: String? = null,
        ssid: String? = null
    ): NetworkState {
        val info = mock(NetworkInfo::class.java)
        doReturn(type).`when`(info).type
        doReturn(NetworkInfo.State.CONNECTED).`when`(info).state
        val lp = LinkProperties()
        val caps = NetworkCapabilities().apply {
            setCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED, false)
            setCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING, true)
            setSSID(ssid)
        }
        return NetworkState(info, lp, caps, mock(Network::class.java), subscriberId, ssid)
    }

    private fun NetworkTemplate.assertMatches(ident: NetworkIdentity) =
            assertTrue(matches(ident), "$this does not match $ident")

    private fun NetworkTemplate.assertDoesNotMatch(ident: NetworkIdentity) =
            assertFalse(matches(ident), "$this should match $ident")

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        doReturn(mockWifiManager).`when`(mockContext).getSystemService(Context.WIFI_SERVICE)
        doReturn(null).`when`(mockWifiManager).getConnectionInfo()
    }

    @Test
    fun testWifiMatches() {
        val templateWifiSsid1 = buildTemplateWifi(TEST_SSID1)
        val templateWifiSsidNullImsiNull = buildTemplateWifi(null, null)
        val templateWifiSsid1ImsiNull = buildTemplateWifi(null, TEST_SSID1)
        val templateWifiSsidNullImsi1 = buildTemplateWifi(TEST_IMSI1, null)
        val templateWifiSsidNonNullImsiNonNull = buildTemplateWifi(SUBSCRIBER_ID_YES, SSID_YES)
        val templateWifiSsidNonNullImsiInvalid = buildTemplateWifi(String(), SSID_YES)
        val templateWifiSsidInvalidImsiNonNull = buildTemplateWifi(SUBSCRIBER_ID_YES, String())
        val templateWifiSsidNonNullImsi1 = buildTemplateWifi(TEST_IMSI1, SSID_YES)
        val templateWifiSsid1ImsiNonNull = buildTemplateWifi(SUBSCRIBER_ID_YES, TEST_SSID1)
        val templateWifiSsid1Imsi1 = buildTemplateWifi(TEST_IMSI1, TEST_SSID1)

        val identMobile1 = buildNetworkIdentity(mockContext, buildMobileNetworkState(TEST_IMSI1),
                false, TelephonyManager.NETWORK_TYPE_UMTS)
        val identWifiSsidNullImsiNull = buildNetworkIdentity(
                mockContext, buildWifiNetworkState(null, null), true, 0)
        val identWifiSsid1ImsiNull = buildNetworkIdentity(
                mockContext, buildWifiNetworkState(null, TEST_SSID1), true, 0)
        val identWifiSsid2ImsiNull = buildNetworkIdentity(
                mockContext, buildWifiNetworkState(null, TEST_SSID2), true, 0)
        val identWifiSsidNullImsi1 = buildNetworkIdentity(
                mockContext, buildWifiNetworkState(TEST_IMSI1, null), true, 0)
        val identWifiSsidNullImsi2 = buildNetworkIdentity(
                mockContext, buildWifiNetworkState(TEST_IMSI2, null), true, 0)
        val identWifiSsid1Imsi1 = buildNetworkIdentity(
                mockContext, buildWifiNetworkState(TEST_IMSI1, TEST_SSID1), true, 0)
        val identWifiSsid1Imsi2 = buildNetworkIdentity(
                mockContext, buildWifiNetworkState(TEST_IMSI2, TEST_SSID1), true, 0)
        val identWifiSsid2Imsi1 = buildNetworkIdentity(
                mockContext, buildWifiNetworkState(TEST_IMSI1, TEST_SSID2), true, 0)
        val identWifiSsid2Imsi2 = buildNetworkIdentity(
                mockContext, buildWifiNetworkState(TEST_IMSI2, TEST_SSID2), true, 0)

        // Verify that template with SSID only matches any subscriberId and specific SSID
        templateWifiSsid1.assertDoesNotMatch(identMobile1)
        templateWifiSsid1.assertDoesNotMatch(identWifiSsidNullImsiNull)
        templateWifiSsid1.assertMatches(identWifiSsid1ImsiNull)
        templateWifiSsid1.assertDoesNotMatch(identWifiSsid2ImsiNull)
        templateWifiSsid1.assertDoesNotMatch(identWifiSsidNullImsi1)
        templateWifiSsid1.assertDoesNotMatch(identWifiSsidNullImsi2)
        templateWifiSsid1.assertMatches(identWifiSsid1Imsi1)
        templateWifiSsid1.assertMatches(identWifiSsid1Imsi2)
        templateWifiSsid1.assertDoesNotMatch(identWifiSsid2Imsi1)
        templateWifiSsid1.assertDoesNotMatch(identWifiSsid2Imsi2)

        // Verify that template with null SSID matches any network with null SSID.
        templateWifiSsidNullImsiNull.assertDoesNotMatch(identMobile1)
        templateWifiSsidNullImsiNull.assertMatches(identWifiSsidNullImsiNull)
        templateWifiSsidNullImsiNull.assertDoesNotMatch(identWifiSsid1ImsiNull)
        templateWifiSsidNullImsiNull.assertDoesNotMatch(identWifiSsid2ImsiNull)
        templateWifiSsidNullImsiNull.assertDoesNotMatch(identWifiSsidNullImsi1)
        templateWifiSsidNullImsiNull.assertDoesNotMatch(identWifiSsidNullImsi2)
        templateWifiSsidNullImsiNull.assertDoesNotMatch(identWifiSsid1Imsi1)
        templateWifiSsidNullImsiNull.assertDoesNotMatch(identWifiSsid1Imsi2)
        templateWifiSsidNullImsiNull.assertDoesNotMatch(identWifiSsid2Imsi1)
        templateWifiSsidNullImsiNull.assertDoesNotMatch(identWifiSsid2Imsi2)

        // Verify that template with SSID1 and null imsi matches any network with
        // SSID1 and null imsi.
        templateWifiSsid1ImsiNull.assertDoesNotMatch(identMobile1)
        templateWifiSsid1ImsiNull.assertDoesNotMatch(identWifiSsidNullImsiNull)
        templateWifiSsid1ImsiNull.assertMatches(identWifiSsid1ImsiNull)
        templateWifiSsid1ImsiNull.assertDoesNotMatch(identWifiSsid2ImsiNull)
        templateWifiSsid1ImsiNull.assertDoesNotMatch(identWifiSsidNullImsi1)
        templateWifiSsid1ImsiNull.assertDoesNotMatch(identWifiSsidNullImsi2)
        templateWifiSsid1ImsiNull.assertDoesNotMatch(identWifiSsid1Imsi1)
        templateWifiSsid1ImsiNull.assertDoesNotMatch(identWifiSsid1Imsi2)
        templateWifiSsid1ImsiNull.assertDoesNotMatch(identWifiSsid2Imsi1)
        templateWifiSsid1ImsiNull.assertDoesNotMatch(identWifiSsid2Imsi2)

        // Verify that template with null SSID and imsi1 matches any network with
        // null SSID and imsi1.
        templateWifiSsidNullImsi1.assertDoesNotMatch(identMobile1)
        templateWifiSsidNullImsi1.assertDoesNotMatch(identWifiSsidNullImsiNull)
        templateWifiSsidNullImsi1.assertDoesNotMatch(identWifiSsid1ImsiNull)
        templateWifiSsidNullImsi1.assertDoesNotMatch(identWifiSsid2ImsiNull)
        templateWifiSsidNullImsi1.assertMatches(identWifiSsidNullImsi1)
        templateWifiSsidNullImsi1.assertDoesNotMatch(identWifiSsidNullImsi2)
        templateWifiSsidNullImsi1.assertDoesNotMatch(identWifiSsid1Imsi1)
        templateWifiSsidNullImsi1.assertDoesNotMatch(identWifiSsid1Imsi2)
        templateWifiSsidNullImsi1.assertDoesNotMatch(identWifiSsid2Imsi1)
        templateWifiSsidNullImsi1.assertDoesNotMatch(identWifiSsid2Imsi2)

        // Verify that template with SUBSCRIBER_ID_YES and SSID_YES matches
        // any non-null subscriberId and non-null SSID.
        templateWifiSsidNonNullImsiNonNull.assertDoesNotMatch(identMobile1)
        templateWifiSsidNonNullImsiNonNull.assertDoesNotMatch(identWifiSsidNullImsiNull)
        templateWifiSsidNonNullImsiNonNull.assertDoesNotMatch(identWifiSsid1ImsiNull)
        templateWifiSsidNonNullImsiNonNull.assertDoesNotMatch(identWifiSsid2ImsiNull)
        templateWifiSsidNonNullImsiNonNull.assertDoesNotMatch(identWifiSsidNullImsi1)
        templateWifiSsidNonNullImsiNonNull.assertDoesNotMatch(identWifiSsidNullImsi2)
        templateWifiSsidNonNullImsiNonNull.assertMatches(identWifiSsid1Imsi1)
        templateWifiSsidNonNullImsiNonNull.assertMatches(identWifiSsid1Imsi2)
        templateWifiSsidNonNullImsiNonNull.assertMatches(identWifiSsid2Imsi1)
        templateWifiSsidNonNullImsiNonNull.assertMatches(identWifiSsid2Imsi2)

        // Verify that template with invalid subscriberId does not matches non-null subscriberId.
        templateWifiSsidNonNullImsiInvalid.assertDoesNotMatch(identMobile1)
        templateWifiSsidNonNullImsiInvalid.assertDoesNotMatch(identWifiSsidNullImsiNull)
        templateWifiSsidNonNullImsiInvalid.assertDoesNotMatch(identWifiSsid1ImsiNull)
        templateWifiSsidNonNullImsiInvalid.assertDoesNotMatch(identWifiSsid2ImsiNull)
        templateWifiSsidNonNullImsiInvalid.assertDoesNotMatch(identWifiSsidNullImsi1)
        templateWifiSsidNonNullImsiInvalid.assertDoesNotMatch(identWifiSsidNullImsi2)
        templateWifiSsidNonNullImsiInvalid.assertDoesNotMatch(identWifiSsid1Imsi1)
        templateWifiSsidNonNullImsiInvalid.assertDoesNotMatch(identWifiSsid1Imsi2)
        templateWifiSsidNonNullImsiInvalid.assertDoesNotMatch(identWifiSsid2Imsi1)
        templateWifiSsidNonNullImsiInvalid.assertDoesNotMatch(identWifiSsid2Imsi2)

        // Verify that template with invalid SSID does not matches non-null SSID.
        templateWifiSsidInvalidImsiNonNull.assertDoesNotMatch(identMobile1)
        templateWifiSsidInvalidImsiNonNull.assertDoesNotMatch(identWifiSsidNullImsiNull)
        templateWifiSsidInvalidImsiNonNull.assertDoesNotMatch(identWifiSsid1ImsiNull)
        templateWifiSsidInvalidImsiNonNull.assertDoesNotMatch(identWifiSsid2ImsiNull)
        templateWifiSsidInvalidImsiNonNull.assertDoesNotMatch(identWifiSsidNullImsi1)
        templateWifiSsidInvalidImsiNonNull.assertDoesNotMatch(identWifiSsidNullImsi2)
        templateWifiSsidInvalidImsiNonNull.assertDoesNotMatch(identWifiSsid1Imsi1)
        templateWifiSsidInvalidImsiNonNull.assertDoesNotMatch(identWifiSsid1Imsi2)
        templateWifiSsidInvalidImsiNonNull.assertDoesNotMatch(identWifiSsid2Imsi1)
        templateWifiSsidInvalidImsiNonNull.assertDoesNotMatch(identWifiSsid2Imsi2)

        // Verify that template with specific subscriberId and NON_NULL_SSID matches
        // the specific subscriberId only and non-null SSID.
        templateWifiSsidNonNullImsi1.assertDoesNotMatch(identMobile1)
        templateWifiSsidNonNullImsi1.assertDoesNotMatch(identWifiSsidNullImsiNull)
        templateWifiSsidNonNullImsi1.assertDoesNotMatch(identWifiSsid1ImsiNull)
        templateWifiSsidNonNullImsi1.assertDoesNotMatch(identWifiSsid2ImsiNull)
        templateWifiSsidNonNullImsi1.assertDoesNotMatch(identWifiSsidNullImsi1)
        templateWifiSsidNonNullImsi1.assertDoesNotMatch(identWifiSsidNullImsi2)
        templateWifiSsidNonNullImsi1.assertMatches(identWifiSsid1Imsi1)
        templateWifiSsidNonNullImsi1.assertDoesNotMatch(identWifiSsid1Imsi2)
        templateWifiSsidNonNullImsi1.assertMatches(identWifiSsid2Imsi1)
        templateWifiSsidNonNullImsi1.assertDoesNotMatch(identWifiSsid2Imsi2)

        // Verify that template with SUBSCRIBER_ID_YES and specific SSID matches the
        // any non-null subscriberId and specific SSID
        templateWifiSsid1ImsiNonNull.assertDoesNotMatch(identMobile1)
        templateWifiSsid1ImsiNonNull.assertDoesNotMatch(identWifiSsidNullImsiNull)
        templateWifiSsid1ImsiNonNull.assertDoesNotMatch(identWifiSsid1ImsiNull)
        templateWifiSsid1ImsiNonNull.assertDoesNotMatch(identWifiSsid2ImsiNull)
        templateWifiSsid1ImsiNonNull.assertDoesNotMatch(identWifiSsidNullImsi1)
        templateWifiSsid1ImsiNonNull.assertDoesNotMatch(identWifiSsidNullImsi2)
        templateWifiSsid1ImsiNonNull.assertMatches(identWifiSsid1Imsi1)
        templateWifiSsid1ImsiNonNull.assertMatches(identWifiSsid1Imsi2)
        templateWifiSsid1ImsiNonNull.assertDoesNotMatch(identWifiSsid2Imsi1)
        templateWifiSsid1ImsiNonNull.assertDoesNotMatch(identWifiSsid2Imsi2)

        // Verify that template with specific subscriberId and specific SSID matches
        // the specific subscriberId and specific SSID
        templateWifiSsid1Imsi1.assertDoesNotMatch(identMobile1)
        templateWifiSsid1Imsi1.assertDoesNotMatch(identWifiSsidNullImsiNull)
        templateWifiSsid1Imsi1.assertDoesNotMatch(identWifiSsid1ImsiNull)
        templateWifiSsid1Imsi1.assertDoesNotMatch(identWifiSsid2ImsiNull)
        templateWifiSsid1Imsi1.assertDoesNotMatch(identWifiSsidNullImsi1)
        templateWifiSsid1Imsi1.assertDoesNotMatch(identWifiSsidNullImsi2)
        templateWifiSsid1Imsi1.assertMatches(identWifiSsid1Imsi1)
        templateWifiSsid1Imsi1.assertDoesNotMatch(identWifiSsid1Imsi2)
        templateWifiSsid1Imsi1.assertDoesNotMatch(identWifiSsid2Imsi1)
        templateWifiSsid1Imsi1.assertDoesNotMatch(identWifiSsid2Imsi2)
    }

    @Test
    fun testCarrierWildcardMatches() {
        val templateCarrierWildcardImsi1 = buildTemplateCarrierWildcard(TEST_IMSI1)

        val identMobile1 = buildNetworkIdentity(mockContext, buildMobileNetworkState(TEST_IMSI1),
                false, TelephonyManager.NETWORK_TYPE_UMTS)
        val identMobile2 = buildNetworkIdentity(mockContext, buildMobileNetworkState(TEST_IMSI2),
                false, TelephonyManager.NETWORK_TYPE_UMTS)
        val identWifiSsid1 = buildNetworkIdentity(
                mockContext, buildWifiNetworkState(null, TEST_SSID1), true, 0)
        val identCarrierWifiImsi1 = buildNetworkIdentity(
                mockContext, buildWifiNetworkState(TEST_IMSI1, null), true, 0)
        val identCarrierWifiImsi2 = buildNetworkIdentity(
                mockContext, buildWifiNetworkState(TEST_IMSI2, null), true, 0)

        templateCarrierWildcardImsi1.assertMatches(identCarrierWifiImsi1)
        templateCarrierWildcardImsi1.assertDoesNotMatch(identCarrierWifiImsi2)
        templateCarrierWildcardImsi1.assertDoesNotMatch(identWifiSsid1)
        templateCarrierWildcardImsi1.assertMatches(identMobile1)
        templateCarrierWildcardImsi1.assertDoesNotMatch(identMobile2)
    }

    @Test
    fun testRatTypeGroupMatches() {
        val stateMobile = buildMobileNetworkState(TEST_IMSI1)
        // Build UMTS template that matches mobile identities with RAT in the same
        // group with any IMSI. See {@link NetworkTemplate#getCollapsedRatType}.
        val templateUmts = buildTemplateMobileWithRatType(null, TelephonyManager.NETWORK_TYPE_UMTS)
        // Build normal template that matches mobile identities with any RAT and IMSI.
        val templateAll = buildTemplateMobileWithRatType(null, NETWORK_TYPE_ALL)
        // Build template with UNKNOWN RAT that matches mobile identities with RAT that
        // cannot be determined.
        val templateUnknown =
                buildTemplateMobileWithRatType(null, TelephonyManager.NETWORK_TYPE_UNKNOWN)

        val identUmts = buildNetworkIdentity(
                mockContext, stateMobile, false, TelephonyManager.NETWORK_TYPE_UMTS)
        val identHsdpa = buildNetworkIdentity(
                mockContext, stateMobile, false, TelephonyManager.NETWORK_TYPE_HSDPA)
        val identLte = buildNetworkIdentity(
                mockContext, stateMobile, false, TelephonyManager.NETWORK_TYPE_LTE)
        val identCombined = buildNetworkIdentity(
                mockContext, stateMobile, false, SUBTYPE_COMBINED)
        val identImsi2 = buildNetworkIdentity(mockContext, buildMobileNetworkState(TEST_IMSI2),
                false, TelephonyManager.NETWORK_TYPE_UMTS)
        val identWifi = buildNetworkIdentity(
                mockContext, buildWifiNetworkState(null, TEST_SSID1), true, 0)

        // Assert that identity with the same RAT matches.
        templateUmts.assertMatches(identUmts)
        templateAll.assertMatches(identUmts)
        templateUnknown.assertDoesNotMatch(identUmts)
        // Assert that identity with the RAT within the same group matches.
        templateUmts.assertMatches(identHsdpa)
        templateAll.assertMatches(identHsdpa)
        templateUnknown.assertDoesNotMatch(identHsdpa)
        // Assert that identity with the RAT out of the same group only matches template with
        // NETWORK_TYPE_ALL.
        templateUmts.assertDoesNotMatch(identLte)
        templateAll.assertMatches(identLte)
        templateUnknown.assertDoesNotMatch(identLte)
        // Assert that identity with combined RAT only matches with template with NETWORK_TYPE_ALL
        // and NETWORK_TYPE_UNKNOWN.
        templateUmts.assertDoesNotMatch(identCombined)
        templateAll.assertMatches(identCombined)
        templateUnknown.assertMatches(identCombined)
        // Assert that identity with different IMSI matches.
        templateUmts.assertMatches(identImsi2)
        templateAll.assertMatches(identImsi2)
        templateUnknown.assertDoesNotMatch(identImsi2)
        // Assert that wifi identity does not match.
        templateUmts.assertDoesNotMatch(identWifi)
        templateAll.assertDoesNotMatch(identWifi)
        templateUnknown.assertDoesNotMatch(identWifi)
    }

    @Test
    fun testParcelUnparcel() {
        val templateMobile = NetworkTemplate(MATCH_MOBILE, TEST_IMSI1, null, null, METERED_ALL,
                ROAMING_ALL, DEFAULT_NETWORK_ALL, TelephonyManager.NETWORK_TYPE_LTE)
        val templateWifi = NetworkTemplate(MATCH_WIFI, null, null, TEST_SSID1, METERED_ALL,
                ROAMING_ALL, DEFAULT_NETWORK_ALL, 0)
        assertParcelSane(templateMobile, 8)
        assertParcelSane(templateWifi, 8)
    }

    // Verify NETWORK_TYPE_* constants in NetworkTemplate do not conflict with
    // TelephonyManager#NETWORK_TYPE_* constants.
    @Test
    fun testNetworkTypeConstants() {
        for (ratType in TelephonyManager.getAllNetworkTypes()) {
            assertNotEquals(NETWORK_TYPE_ALL, ratType)
            assertNotEquals(NETWORK_TYPE_5G_NSA, ratType)
        }
    }
}
