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

package android.net;

import static android.net.NetworkCapabilities.TRANSPORT_TEST;
import static android.net.QosCallbackException.EX_TYPE_FILTER_SOCKET_LOCAL_ADDRESS_CHANGED;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import java.io.FileDescriptor;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Optional;

/**
 * Filters a {@link QosSession} according to the binding on the provided {@link Socket}.
 *
 * @hide
 */
public class QosSocketFilter extends QosFilter {

    private static final String TAG = QosSocketFilter.class.getSimpleName();

    @NonNull
    private final QosSocketInfo mQosSocketInfo;

    /**
     * Creates a {@link QosSocketFilter} based off of {@link QosSocketInfo}.
     *
     * @param qosSocketInfo the information required to filter and validate
     */
    public QosSocketFilter(@NonNull final QosSocketInfo qosSocketInfo) {
        mQosSocketInfo = qosSocketInfo;
    }

    /**
     * Performs two validations:
     * 1. If the socket is not bound, then return
     *    {@link QosCallbackException.EX_TYPE_FILTER_SOCKET_NOT_BOUND}. This is detected
     *    by checking the local address on the filter which becomes null when the socket is no
     *    longer bound.
     * 2. In the scenario that the socket is now bound to a different local address, which can
     *    happen in the case of UDP, then we return
     *    {@link QosCallbackException.EX_TYPE_FILTER_SOCKET_LOCAL_ADDRESS_CHANGED}
     * @return validation error code
     */
    @NonNull
    @Override
    public Optional<Integer> validate(NetworkCapabilities networkCapabilities) {
        if (networkCapabilities.getTransportTypes().length == 1
                && networkCapabilities.hasTransport(TRANSPORT_TEST)) {
            return Optional.empty();
        }

        InetSocketAddress sa = getAddressFromFileDescriptor();
        if (sa == null) {
            return Optional.of(QosCallbackException.EX_TYPE_FILTER_SOCKET_NOT_BOUND);
        }

        if (!sa.equals(mQosSocketInfo.getLocalSocketAddress())) {
            return Optional.of(EX_TYPE_FILTER_SOCKET_LOCAL_ADDRESS_CHANGED);
        }

        return Optional.empty();
    }

    /**
     * The local address of the socket's binding.
     *
     * Note: If the socket is no longer bound, null is returned.
     *
     * @return the local address
     */
    @Nullable
    private InetSocketAddress getAddressFromFileDescriptor() {
        try {
            if (mQosSocketInfo.getParcelFileDescriptor() == null) {
                return null;
            }

            FileDescriptor fd = mQosSocketInfo.getParcelFileDescriptor().getFileDescriptor();
            if (fd == null) {
                return null;
            }

            SocketAddress address = Os.getsockname(fd);
            if (address == null) {
                Log.w(TAG, "getAddressFromFileDescriptor: socket address should not be null!");
                return null;
            } else if (address instanceof InetSocketAddress) {
                return (InetSocketAddress) address;
            } else {
                Log.d(TAG, "getAddressFromFileDescriptor: address is of different type: "
                        + address.toString());
                return null;
            }
        } catch (ErrnoException e) {
            Log.e(TAG, "getAddressFromFileDescriptor: getLocalAddress exception", e);
            return null;
        }
    }

    /**
     * The network used with this filter.
     *
     * @return the registered {@link Network}
     */
    @NonNull
    @Override
    public Network getNetwork() {
        return mQosSocketInfo.getNetwork();
    }

    /**
     * The inner socket information
     *
     * @return the qos socket info
     */
    @NonNull
    public QosSocketInfo getQosSocketInfo() {
        return mQosSocketInfo;
    }
}
