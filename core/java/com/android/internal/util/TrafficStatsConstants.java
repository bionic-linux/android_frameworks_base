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

package com.android.internal.util;

/**
 * Constants for traffic stats.
 * @hide
 */
public class TrafficStatsConstants {
    // These values must stay inside the range defined by
    // {@link android.net.TrafficStats#TAG_SYSTEM_NETWORK_STACK_RANGE_START} and
    // {@link android.net.TrafficStats#TAG_SYSTEM_NETWORK_STACK_RANGE_END}.
    public static final int TAG_SYSTEM_DHCP = 0xFFFFFF40;
    public static final int TAG_SYSTEM_NTP = 0xFFFFFF41;
    public static final int TAG_SYSTEM_PROBE = 0xFFFFFF42;
    public static final int TAG_SYSTEM_NEIGHBOR = 0xFFFFFF43;
    public static final int TAG_SYSTEM_GPS = 0xFFFFFF44;
    public static final int TAG_SYSTEM_PAC = 0xFFFFFF45;
    public static final int TAG_SYSTEM_DHCP_SERVER = 0xFFFFFF46;
}
