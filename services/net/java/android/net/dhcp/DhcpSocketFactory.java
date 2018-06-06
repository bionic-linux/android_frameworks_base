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

package android.net.dhcp;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_PACKET;
import static android.system.OsConstants.ETH_P_IP;
import static android.system.OsConstants.IPPROTO_UDP;
import static android.system.OsConstants.SOCK_DGRAM;
import static android.system.OsConstants.SOCK_RAW;
import static android.system.OsConstants.SOL_SOCKET;
import static android.system.OsConstants.SO_BINDTODEVICE;
import static android.system.OsConstants.SO_BROADCAST;
import static android.system.OsConstants.SO_RCVBUF;
import static android.system.OsConstants.SO_REUSEADDR;

import android.net.NetworkUtils;
import android.net.util.InterfaceParams;
import android.system.ErrnoException;
import android.system.Os;
import android.system.PacketSocketAddress;

import libcore.io.IoBridge;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.SocketException;

/**
 * A creator of sockets used for send/receive operations in DHCP clients/servers.
 * @hide
 */
public class DhcpSocketFactory {
    /**
     * Build a socket for a DHCP client to receive packets.
     */
    public FileDescriptor makePacketSocket(InterfaceParams iface)
            throws SocketException, ErrnoException {
        final FileDescriptor socket = Os.socket(AF_PACKET, SOCK_RAW, ETH_P_IP);
        PacketSocketAddress addr = new PacketSocketAddress((short) ETH_P_IP, iface.index);
        Os.bind(socket, addr);
        NetworkUtils.attachDhcpFilter(socket);
        return socket;
    }

    /**
     * Build a UDP socket for a DHCP client or server to send/receive packets.
     */
    public FileDescriptor makeUdpSocket(InterfaceParams iface, int port)
            throws ErrnoException, SocketException {
        final FileDescriptor socket = Os.socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
        Os.setsockoptInt(socket, SOL_SOCKET, SO_REUSEADDR, 1);
        Os.setsockoptIfreq(socket, SOL_SOCKET, SO_BINDTODEVICE, iface.name);
        Os.setsockoptInt(socket, SOL_SOCKET, SO_BROADCAST, 1);
        Os.setsockoptInt(socket, SOL_SOCKET, SO_RCVBUF, 0);
        Os.bind(socket, Inet4Address.ANY, port);
        NetworkUtils.protectFromVpn(socket);

        return socket;
    }
}
