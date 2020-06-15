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

import static android.net.QosCallbackException.EX_TYPE_FILTER_SOCKET_LOCAL_ADDRESS_CHANGED;
import static android.net.QosCallbackException.EX_TYPE_FILTER_SOCKET_NOT_BOUND;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.QosSocketFilter;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Optional;

/**
 * The validator for {@link QosSocketFilter}
 *
 * @hide
 */
public class QosSocketFilterValidator extends QosCallbackValidator {
    @NonNull
    private final QosSocketFilter mFilter;

    /**
     * The original socket address of the filter.  Used to validate against that the address did not
     * change down the line.
     */
    @Nullable
    private final SocketAddress mFirstTimeSocketAddress;

    /**
     * ..ctor for validator
     * @param filter the qos socket filter to validate
     */
    public QosSocketFilterValidator(@NonNull final QosSocketFilter filter) {
        Objects.requireNonNull(filter, "filter must be non-null");
        mFilter = filter;
        mFirstTimeSocketAddress = mFilter.getLocalAddress();
    }

    /**
     * Performs two validations:
     * 1. If the socket is not bound, then return
     *    {@link EX_TYPE_FILTER_SOCKET_NOT_BOUND}. This is detected
     *    by checking the local address on the filter which becomes null when the socket is no
     *    longer bound.
     * 2. In the scenario that the socket is now bound to a different local address, which can
     *    happen in the case of UDP, then we return
     *    {@link EX_TYPE_FILTER_SOCKET_LOCAL_ADDRESS_CHANGED}
     * @return validation error code
     */
    @NonNull
    @Override
    public Optional<Integer> validate() {
        InetSocketAddress sa = mFilter.getLocalAddress();
        if (sa == null || mFirstTimeSocketAddress == null) {
            return Optional.of(EX_TYPE_FILTER_SOCKET_NOT_BOUND);
        } else if (!sa.equals(mFirstTimeSocketAddress)) {
            return Optional.of(EX_TYPE_FILTER_SOCKET_LOCAL_ADDRESS_CHANGED);
        } else {
            return Optional.empty();
        }
    }
}
