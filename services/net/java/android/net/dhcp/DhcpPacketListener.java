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
 * A {@link Thread} to receive and parse {@link DhcpPacket} through a socket.
 * @hide
 */
public abstract class DhcpPacketListener extends PacketReader {
    private final String mTag;
    private final boolean mDbg;

    public DhcpPacketListener(Handler handler, String tag, boolean dbg) {
        super(handler, DhcpPacket.MAX_LENGTH);
        mTag = tag;
        mDbg = dbg;
    }

    @Override
    protected void handlePacket(byte[] recvbuf, int length) {
        try {
            final DhcpPacket packet = DhcpPacket.decodeFullPacket(recvbuf, length,
                    DhcpPacket.ENCAP_L2);
            if (mDbg) Log.d(mTag, "Received packet: " + packet);
            onReceive(packet);
        } catch (DhcpPacket.ParseException e) {
            logParseError(recvbuf, length, e);
        }
    }

    protected abstract void onReceive(DhcpPacket packet);
    protected abstract void logParseError(byte[] packet, int length, DhcpPacket.ParseException e);
}
