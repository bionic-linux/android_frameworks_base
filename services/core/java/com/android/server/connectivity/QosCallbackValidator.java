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

package com.android.server.connectivity;

import static android.net.NetworkCapabilities.TRANSPORT_TEST;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.QosFilter;
import android.net.QosSocketFilter;

import java.util.Optional;

/**
 * Validates that a {@link NetworkAgentInfo} is valid.
 *
 * @hide
 */
public abstract class QosCallbackValidator {

    /**
     * Validates the network agent info
     * @return error code
     */
    public abstract Optional<Integer> validate();


    /**
     * Factory method for validators.  Can be moved out and extended if more types of validators
     * come along later.
     * @param networkAgentInfo the network agent inf oto validate against
     * @param filter registered filter
     * @return designated validator for the filter
     */
    @Nullable
    public static QosCallbackValidator create(
            @NonNull final NetworkAgentInfo networkAgentInfo,
            @NonNull final QosFilter filter) {
        if (networkAgentInfo.networkCapabilities.getTransportTypes().length == 1
                && networkAgentInfo.networkCapabilities.hasTransport(TRANSPORT_TEST)) {
            return new QosTestFilterValidator();
        } else if (filter instanceof QosSocketFilter) {
            return new QosSocketFilterValidator((QosSocketFilter) filter);
        } else {
            return null;
        }
    }

    /**
     * Used with cts tests.  Only used in the case of a test network.
     */
    private static class QosTestFilterValidator extends QosCallbackValidator {
        @Override
        public Optional<Integer> validate() {
            return Optional.empty();
        }
    }
}
