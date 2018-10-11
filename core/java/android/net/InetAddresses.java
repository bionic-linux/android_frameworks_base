/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.net;

import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * Utility methods for {@link InetAddress} implementations.
 */
public class InetAddresses {

    private InetAddresses() {}

    /**
     * Returns an InetAddress corresponding to the given numeric address (such
     * as {@code "192.168.0.1"} or {@code "2001:4860:800d::68"}).
     *
     * <p>This method will never do a DNS lookup. A null or empty string will return an instance
     * of {@link Inet6Address} representing the loopback address. Other non-numeric addresses will
     * be treated as errors.
     *
     * @param numericAddress the address to parse, must be numeric, null or an empty string.
     * @throws IllegalArgumentException if {@code numericAddress} is not null, empty or a numeric
     * address
     */
    public static InetAddress parseNumericAddress(String numericAddress) {
        return InetAddress.parseNumericAddress(numericAddress);
    }
}
