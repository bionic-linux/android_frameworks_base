/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.net.Inet4Address;
import java.nio.ByteBuffer;

/**
 * This class implements the DHCP-ACK packet.
 */
class DhcpAckPacket extends DhcpPacket {

    DhcpAckPacket(int transId, short secs, boolean broadcast, Inet4Address nextIp,
            Inet4Address clientIp, Inet4Address yourIp, byte[] clientMac) {
      super(transId, secs, clientIp, yourIp, nextIp, INADDR_ANY, clientMac, broadcast);
    }

    public String toString() {
        String s = super.toString();
        String dnsServers = " DNS servers: ";

        for (Inet4Address dnsServer: mDnsServers) {
            dnsServers += dnsServer.toString() + " ";
        }

        return s + " ACK: your new IP " + mYourIp +
                ", netmask " + mSubnetMask +
                ", gateways " + mGateways + dnsServers +
                ", lease time " + mLeaseTime;
    }

    @Override
    public byte getRequestCode() {
        return DHCP_BOOTREPLY;
    }

    /**
     * Adds the optional parameters to the client-generated ACK packet.
     */
    void finishPacket(ByteBuffer buffer) {
        addTlv(buffer, DHCP_MESSAGE_TYPE, DHCP_MESSAGE_TYPE_ACK);
        addTlv(buffer, DHCP_SERVER_IDENTIFIER, mServerIdentifier);

        addCommonServerTlvs(buffer);
        addTlvEnd(buffer);
    }
}
