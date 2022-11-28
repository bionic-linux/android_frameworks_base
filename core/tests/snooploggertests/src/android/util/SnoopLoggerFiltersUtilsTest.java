/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import android.content.Context;
import android.os.SystemProperties;
import android.test.suitebuilder.annotation.SmallTest;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SnoopLoggerFiltersUtilsTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        cleanup();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        SystemProperties.set(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY, "");
        SystemProperties.set(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPES_PROPERTY, "");
    }

    @Test
    public void testSetEnabled_profilesfiltered() {
        assertFalse(SnoopLoggerFiltersUtils.isEnabled(
                mContext,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES));

        SnoopLoggerFiltersUtils.setEnabled(
                null /* context */,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES,
                true);

        assertEquals(SystemProperties.get(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPES_PROPERTY, null),
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES);
    }

    @Test
    public void testSetEnabled_all() {
        assertFalse(SnoopLoggerFiltersUtils.isEnabled(
                mContext,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_RFCOMM_CHANNEL));
        assertFalse(SnoopLoggerFiltersUtils.isEnabled(
                mContext,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_HEADERS));
        assertFalse(SnoopLoggerFiltersUtils.isEnabled(
                mContext,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_A2DP_PKTS));
        assertFalse(SnoopLoggerFiltersUtils.isEnabled(
                mContext,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES));

        SnoopLoggerFiltersUtils.setEnabled(
                null /* context */,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_RFCOMM_CHANNEL,
                true);
        SnoopLoggerFiltersUtils.setEnabled(
                null /* context */,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_HEADERS,
                true);
        SnoopLoggerFiltersUtils.setEnabled(
                null /* context */,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_A2DP_PKTS,
                true);
        SnoopLoggerFiltersUtils.setEnabled(
                null /* context */,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES,
                true);

        assertEquals(SystemProperties.get(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPES_PROPERTY, null),
                String.join(
                        ",",
                        SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_RFCOMM_CHANNEL,
                        SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_HEADERS,
                        SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_A2DP_PKTS,
                        SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES));
    }

    @Test
    public void testSetDisabled_a2dppktsfiltered_fromAllEnabled() {
        assertFalse(SnoopLoggerFiltersUtils.isEnabled(
                mContext,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_RFCOMM_CHANNEL));
        assertFalse(SnoopLoggerFiltersUtils.isEnabled(
                mContext,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_HEADERS));
        assertFalse(SnoopLoggerFiltersUtils.isEnabled(
                mContext,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_A2DP_PKTS));
        assertFalse(SnoopLoggerFiltersUtils.isEnabled(
                mContext,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES));

        SnoopLoggerFiltersUtils.setEnabled(
                null /* context */,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_RFCOMM_CHANNEL,
                true);
        SnoopLoggerFiltersUtils.setEnabled(
                null /* context */,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_HEADERS,
                true);
        SnoopLoggerFiltersUtils.setEnabled(
                null /* context */,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_A2DP_PKTS,
                true);
        SnoopLoggerFiltersUtils.setEnabled(
                null /* context */,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES,
                true);

        assertEquals(SystemProperties.get(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPES_PROPERTY, null),
                String.join(
                        ",",
                        SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_RFCOMM_CHANNEL,
                        SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_HEADERS,
                        SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_A2DP_PKTS,
                        SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES));

        SnoopLoggerFiltersUtils.setEnabled(
                null /* context */,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_A2DP_PKTS,
                false);

        assertEquals(SystemProperties.get(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPES_PROPERTY, null),
                String.join(
                        ",",
                        SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_RFCOMM_CHANNEL,
                        SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_HEADERS,
                        SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES));
    }

    @Test
    public void testFilterTypeEnabled_notSet_shouldReturnFalse() {
        assertFalse(SnoopLoggerFiltersUtils.isEnabled(mContext, "does_not_exist"));
    }

    @Test
    public void test_isProfilesFilteringEnabled() {
        assertFalse(SnoopLoggerFiltersUtils.isProfilesFilteringEnabled());

        SystemProperties.set(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_MODE_ENABLED_FILTERED);

        SystemProperties.set(
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPES_PROPERTY,
                SnoopLoggerFiltersUtils.BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES);

        assertTrue(SnoopLoggerFiltersUtils.isProfilesFilteringEnabled());
    }

    @Test
    public void getAllSnoopLoggerFilterTypes_shouldNotBeNull() {
        assertNotNull(SnoopLoggerFiltersUtils.getSnoopLoggerFilterTypes());
    }
}
