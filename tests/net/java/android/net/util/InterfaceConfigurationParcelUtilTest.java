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

import static android.net.util.InterfaceConfigurationParcelUtil.fromParcel;
import static android.net.util.InterfaceConfigurationParcelUtil.toParcel;

import static junit.framework.Assert.assertEquals;

import android.net.InetAddresses;
import android.net.InterfaceConfiguration;
import android.net.InterfaceConfigurationParcel;
import android.net.LinkAddress;
import android.net.shared.ParcelableTestUtil;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;

/**
 * Tests for {@link InterfaceConfigurationParcelUtil}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class InterfaceConfigurationParcelUtilTest {
    private InterfaceConfiguration mConfig;

    @Before
    public void setUp() {
        mConfig = new InterfaceConfiguration();
        mConfig.setFlag("test_flag_1");
        mConfig.setFlag("test_flag_2");
        mConfig.setLinkAddress(
                new LinkAddress(InetAddresses.parseNumericAddress("2001:db8::42"), 127));
        mConfig.setHardwareAddress("01:02:03:04:05:06");

        // Above code must be updated if fields are added
        ParcelableTestUtil.assertFieldCountEquals(3, InterfaceConfiguration.class);
    }

    @Test
    public void testParcelUnparcel() {
        doParcelUnparcelTest();
    }

    @Test
    public void testParcelUnparcel_NoFlags() {
        for (String flag : toSet(mConfig.getFlags())) {
            mConfig.clearFlag(flag);
        }
        doParcelUnparcelTest();
    }

    @Test
    public void testParcelUnparcel_NullHardwareAddress() {
        mConfig.setHardwareAddress(null);
        final InterfaceConfigurationParcel p = toParcel(mConfig, "test_iface");

        // null hardware addresses are not written to parcels
        assertEquals("", p.hwAddr);
    }

    @Test
    public void testParcelUnparcel_EmptyHardwareAddress() {
        mConfig.setHardwareAddress("");
        doParcelUnparcelTest();
    }

    private void doParcelUnparcelTest() {
        final String testIface = "test_iface";
        final InterfaceConfigurationParcel p = toParcel(mConfig, testIface);
        assertConfigEquals(mConfig, fromParcel(p));
        assertEquals(testIface, p.ifName);
    }

    private void assertConfigEquals(InterfaceConfiguration exp, InterfaceConfiguration actual) {
        assertEquals(toSet(exp.getFlags()), toSet(actual.getFlags()));
        assertEquals(exp.getLinkAddress(), actual.getLinkAddress());
        assertEquals(exp.getHardwareAddress(), actual.getHardwareAddress());

        // Above code must be updated if fields are added
        ParcelableTestUtil.assertFieldCountEquals(3, InterfaceConfiguration.class);
    }

    private static HashSet<String> toSet(Iterable<String> it) {
        final HashSet<String> set = new HashSet<>();
        it.forEach(set::add);
        return set;
    }
}
