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

import android.net.util.PacketReader;
import android.os.Handler;
import android.util.Log;

/**
 * A {@link PacketReader} to receive and parse {@link DhcpPacket}.
 * @hide
 */
public abstract class DhcpPacketListener extends PacketReader {
    public DhcpPacketListener(Handler handler) {
        super(handler, DhcpPacket.MAX_LENGTH);
    }

    @Override
    protected final void handlePacket(byte[] recvbuf, int length) {
        try {
            final DhcpPacket packet = DhcpPacket.decodeFullPacket(recvbuf, length,
                    DhcpPacket.ENCAP_L2);
            onReceive(packet);
        } catch (DhcpPacket.ParseException e) {
            logParseError(recvbuf, length, e);
        }
    }

    protected abstract void onReceive(DhcpPacket packet);
    protected abstract void logParseError(byte[] packet, int length, DhcpPacket.ParseException e);
}
