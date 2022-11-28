/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.annotation.TestApi;
import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Util class to get feature flag information.
 *
 * @hide
 */
@TestApi
public class SnoopLoggerFiltersUtils {
    private static final String PREFERENCE_KEY = "bt_hci_snoop_log_filters";
    private static final String TAG = "SnoopLoggerFiltersUtils";
    private static final Map<String, String> SNOOP_LOGGER_FILTER_TYPES;

    public static final String BLUETOOTH_BTSNOOP_LOG_FILTER_TYPES_PROPERTY =
            "persist.bluetooth.btsnooplogfilter.types";
    public static final String BLUETOOTH_BTSNOOP_LOG_PBAP_FILTER_MODE_PROPERTY =
            "persist.bluetooth.btsnooplogfilter.profiles.pbap";
    public static final String BLUETOOTH_BTSNOOP_LOG_MAP_FILTER_MODE_PROPERTY =
            "persist.bluetooth.btsnooplogfilter.profiles.map";
    public static final String BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY =
            "persist.bluetooth.btsnooplogmode";

    public static final String BLUETOOTH_BTSNOOP_LOG_MODE_FULL = "full";
    public static final String BLUETOOTH_BTSNOOP_LOG_MODE_ENABLED_FILTERED = "filtered";
    public static final String BLUETOOTH_BTSNOOP_LOG_MODE_DISABLED = "disabled";

    public static final String BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_RFCOMM_CHANNEL =
            "rfcommchannelfiltered";
    public static final String BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_HEADERS =
            "snoopheadersfiltered";
    public static final String BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_A2DP_PKTS =
            "a2dppktsfiltered";
    public static final String BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES =
            "profilesfiltered";

    static {
        SNOOP_LOGGER_FILTER_TYPES = new HashMap<>();
        SNOOP_LOGGER_FILTER_TYPES.put(BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_RFCOMM_CHANNEL,
                                      "RFCOMM Channel Filtered");
        SNOOP_LOGGER_FILTER_TYPES.put(BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_HEADERS,
                                      "Headers Filtered");
        SNOOP_LOGGER_FILTER_TYPES.put(BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_A2DP_PKTS,
                                      "A2DP Media Packets Filtered");
        SNOOP_LOGGER_FILTER_TYPES.put(BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES,
                                      "Profiles Filtered");
    }

    /**
     * Whether or not a filter type is enabled.
     *
     * @param feature the filter type name
     * @return true if the filter type is enabled (either by default in system, or override by user)
     */
    public static boolean isEnabled(Context context, String feature) {
        String value;

        if (SNOOP_LOGGER_FILTER_TYPES.containsKey(feature)) {
            value = SystemProperties.get(BLUETOOTH_BTSNOOP_LOG_FILTER_TYPES_PROPERTY);
            if (value.contains(feature)) {
                return Boolean.parseBoolean(SystemProperties.get(feature));
            }
        }
        return false;
    }

    /**
     * Whether or not profile filtering is enabled.
     *
     * @return true if the filtering is enabled
     */
    public static boolean isProfilesFilteringEnabled() {
        final String currentLogMode = SystemProperties.get(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY);
        final String currentFilterType = SystemProperties.get(
                BLUETOOTH_BTSNOOP_LOG_FILTER_TYPES_PROPERTY);
        final boolean enabled = TextUtils.equals(currentLogMode,
                                        BLUETOOTH_BTSNOOP_LOG_MODE_ENABLED_FILTERED)
                                && currentFilterType.contains(
                                        BLUETOOTH_BTSNOOP_LOG_FILTER_TYPE_PROFILES);
        return enabled;
    }

    /**
     * Add new type to filter types property.
     */
    public static void setEnabled(Context context, String feature, boolean enabled) {
        String new_sysprop = "";
        String current_sysprop = SystemProperties.get(BLUETOOTH_BTSNOOP_LOG_FILTER_TYPES_PROPERTY);
        List<String> current_sysprops_list =
                new ArrayList(Arrays.asList(current_sysprop.split(",", -1)));

        if (enabled) {
            if (current_sysprops_list.contains(feature)) {
                return;
            }
            current_sysprops_list.add(feature);
        } else {
            if (current_sysprops_list.contains(feature)) {
                current_sysprops_list.remove(feature);
            }
        }

        if (current_sysprops_list.contains("")) {
            current_sysprops_list.remove("");
        }

        for (int i = 0; i < current_sysprops_list.size(); i++) {
            if (i != current_sysprops_list.size() - 1) {
                new_sysprop += (current_sysprops_list.get(i) + ",");
            } else {
                new_sysprop += (current_sysprops_list.get(i));
            }
        }

        SystemProperties.set(BLUETOOTH_BTSNOOP_LOG_FILTER_TYPES_PROPERTY, new_sysprop);
    }

    /**
     * Returns all Snoop Logger Filter Types and titles.
     */
    public static Map<String, String> getSnoopLoggerFilterTypes() {
        return SNOOP_LOGGER_FILTER_TYPES;
    }
}
