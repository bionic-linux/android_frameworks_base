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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Used in conjunction with
 * {@link ConnectivityManager#registerQosCallback(QosSocketInfo, QosCallback, Executor)}
 * in order to receive Qos Sessions related to the local address and port of a bound {@link Socket}.
 *
 * @hide
 */
@SystemApi
public final class QosSocketInfo implements Parcelable {

    private static final String TAG = QosSocketInfo.class.getSimpleName();

    @NonNull
    private final Network mNetwork;

    @Nullable
    private final ParcelFileDescriptor mParcelFileDescriptor;

    @Nullable
    private final InetSocketAddress mLocalSocketAddress;

    /**
     * The {@link Network} the socket is on.
     *
     * @return the registered {@link Network}
     */
    @NonNull
    public Network getNetwork() {
        return mNetwork;
    }

    /**
     * The parcel file descriptor wrapped around the socket's file descriptor.
     *
     * @return the parcel file descriptor of the socket
     */
    @Nullable
    ParcelFileDescriptor getParcelFileDescriptor() {
        return mParcelFileDescriptor;
    }

    /**
     * The local address of the socket passed into {@link QosSocketInfo(Network, Socket)}.
     * The value does not reflect any changes that occur to the socket after it is first set
     * in the constructor.
     *
     * @return the local address of the socket
     */
    @Nullable
    public InetSocketAddress getLocalSocketAddress() {
        return mLocalSocketAddress;
    }

    /**
     * Creates a {@link QosSocketInfo} given a {@link Network} and bound {@link Socket}.  The
     * {@link Socket} must remain bound in order to receive {@link QosSession}s.
     *
     * @param network the network
     * @param socket the bound {@link Socket}
     */
    public QosSocketInfo(@NonNull final Network network, @NonNull final Socket socket) {
        mNetwork = Objects.requireNonNull(network, "network cannot be null");
        Objects.requireNonNull(socket, "socket cannot be null");
        ParcelFileDescriptor parcelFileDescriptor;
        InetAddress socketAddress;
        int port;

        try {
            parcelFileDescriptor = ParcelFileDescriptor.dup(socket.getFileDescriptor$());
            socketAddress = socket.getLocalAddress();
            port = socket.getLocalPort();
        } catch (final IOException e) {
            /* This occurs when the socket is not bound.  For testing purposes, we allow this
               state when TRANSPORT_TEST is used.  If the transport on the network is not set to
               TRANSPORT_TEST though, ConnectivityService will error. */
            parcelFileDescriptor = null;
            socketAddress = null;
            port = 0;
            Log.w(TAG, "ParcelFileDescriptor is invalid.");
        }

        mParcelFileDescriptor = parcelFileDescriptor;
        mLocalSocketAddress = new InetSocketAddress(socketAddress, port);
    }

    /* Parcelable methods */
    private QosSocketInfo(final Parcel in) {
        mNetwork = Objects.requireNonNull(Network.CREATOR.createFromParcel(in));
        if (in.readBoolean()) {
            mParcelFileDescriptor = ParcelFileDescriptor.CREATOR.createFromParcel(in);
        } else {
            mParcelFileDescriptor = null;
        }

        final int addressLength = in.readInt();
        if (addressLength > 0) {
            mLocalSocketAddress = readSocketAddress(in, addressLength);
        } else {
            mLocalSocketAddress = null;
        }
    }

    private InetSocketAddress readSocketAddress(final Parcel in, final int addressLength) {
        final byte[] address = new byte[addressLength];
        in.readByteArray(address);
        final int port = in.readInt();

        try {
            return new InetSocketAddress(InetAddress.getByAddress(address), port);
        } catch (final UnknownHostException e) {
            /* Nothing we can do here since we already checked that address
               was not null ahead of time. */
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        mNetwork.writeToParcel(dest, 0);
        if (mParcelFileDescriptor != null && mParcelFileDescriptor.getFileDescriptor() != null) {
            dest.writeBoolean(true);
            mParcelFileDescriptor.writeToParcel(dest, 0);
        } else {
            dest.writeBoolean(false);
        }

        if (mLocalSocketAddress != null) {
            final byte[] address = mLocalSocketAddress.getAddress().getAddress();
            dest.writeInt(address.length);
            dest.writeByteArray(address);
            dest.writeInt(mLocalSocketAddress.getPort());
        } else {
            dest.writeInt(0);
        }
    }

    @NonNull
    public static final Parcelable.Creator<QosSocketInfo> CREATOR =
            new Parcelable.Creator<QosSocketInfo>() {
            @NonNull
            @Override
            public QosSocketInfo createFromParcel(final Parcel in) {
                return new QosSocketInfo(in);
            }

            @NonNull
            @Override
            public QosSocketInfo[] newArray(final int size) {
                return new QosSocketInfo[size];
            }
        };

    /**
     * Thrown when an event is being delivered to {@link QosCallback} and the socket is closed.
     *
     * Sent as the cause of the exception passed into
     * {@link QosCallback#onError(QosCallbackException)}
     */
    public static class SocketNotBoundException extends Exception {

        public SocketNotBoundException() {
            super("The socket is unbound");
        }
    }

    /**
     * Raised when an event is being delivered to {@link QosCallback} and the local address
     * of the socket changed from when the filter was originally created.
     *
     * Sent as the cause of the exception passed into
     * {@link QosCallback#onError(QosCallbackException)}
     */
    public static class SocketLocalAddressChangedException extends Exception {
        public SocketLocalAddressChangedException() {
            super("The local address of the socket changed");
        }
    }
}
