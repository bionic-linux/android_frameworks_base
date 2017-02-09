/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.server.ese;

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Implements command translation from a Java Card based secure element.
 *
 * Communication with the SE is done with APDUs and this class constrcts the APDU commands and
 * detructs the APDU responses.
 */
class JavaCardSecureElement implements SecureElement {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CHANNEL_SECURE_STORAGE, CHANNEL_GATEKEEPER, CHANNEL_KEYMASTER})
    private @interface LogicalChannel {}
    private static final int CHANNEL_SECURE_STORAGE = 1;
    private static final int CHANNEL_GATEKEEPER = 2;
    private static final int CHANNEL_KEYMASTER = 3;

    private static final int CLA_HIGH_NIBBLE = 0xa0; // ??
    private static final int CLA_SECURE_MESSAGING_BITS = 0; // None??
    private static final int CLA_LOGICAL_CHANNEL_MASK = 0x03;

    private SecureElementConnection mSeConnection;

    public JavaCardSecureElement(SecureElementConnection seConnection) {
        mSeConnection = seConnection;
    }

    private static byte makeCla(@LogicalChannel int channel) {
        return (byte) (CLA_HIGH_NIBBLE | CLA_SECURE_MESSAGING_BITS |
                (channel & CLA_LOGICAL_CHANNEL_MASK));
    }

    private static byte makeGatekeeperCla() {
        // Need to modify the top nibble too?
        return makeCla(CHANNEL_GATEKEEPER);
    }

    /**
     * Sends the command to the SE and checks the response code it ok.
     *
     * @param command The command to send to the SE.
     * @param reponse The buffer to receive the response.
     */
    private void transceive(byte[] command, byte[] response) {
        mSeConnection.transceive(command, response);
        final int end = response.length;
        final int sw = (response[end - 2] << 8) | response[end - 1];
        if (sw != 0x9000) {
            throw new RuntimeException("SE request not OK: " + Integer.toHexString(sw));
        }
    }

    @Override
    public int gatekeeperGetNumSlots() {
        // Overkill?
        final ByteBuffer command = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN);
        command.put(makeGatekeeperCla());
        command.put((byte) 0x08); // TODO: share constatnts with applet
        command.put((byte) 0);
        command.put((byte) 0);
        command.put((byte) 4);

        final byte[] response = new byte[4 + 2];
        transceive(command.array(), response);

        final ByteBuffer buffer = ByteBuffer.wrap(response).order(ByteOrder.BIG_ENDIAN);
        final int numSlots = buffer.getInt();
        return numSlots;
    }

    @Override
    public void gatekeeperWrite(int slotId, byte[] key, byte[] value) {
        //if (key.length != 16 || value.length != 16) {
        //    throw new InvalidArgumentException("Gotta be 16!");
        //}

        //final byte[] command = new byte[5 + 32];
        //command[0] = makeGatekeeperCla();
        //command[1] = 0x10;
        //command[2] = 0;
        //command[3] = 0;
        //command[4] = 32;
        //System.arraycopy(key, 0, command, 5, 16);
        //System.arraycopy(value, 0, command, 5 + 16, 16);

        //final byte[] response = transceive(command);
        //final ByteBuffer buffer = ByteBuffer.wrap(response).order(ByteOrder.BIG_ENDIAN);
        //final int status = buffer.getShort();
        //if (status != 0x9000) {
        //    throw new RuntimeException("SE request not OK");
        //}
    }

    @Override
    public byte[] gatekeeperRead(int slotId, byte[] key) {
        //if (key.length != 16) {
        //    throw new InvalidArgumentException("Gotta be 16!");
        //}

        //final byte[] command = new byte[5 + key.length];
        //command[0] = makeGatekeeperCla();
        //command[1] = 0x12;
        //command[2] = 0;
        //command[3] = 0;
        //command[4] = 16;
        //System.arraycopy(key, 0, command, 4, 16);

        //final byte[] response = transceive(command);
        //final ByteBuffer buffer = ByteBuffer.wrap(response).order(ByteOrder.BIG_ENDIAN);
        //final byte[] value = new byte[16];
        //buffer.get(value);
        //final int status = buffer.getShort();
        //if (status != 0x9000) {
        //    throw new RuntimeException("SE request not OK");
        //}
        //return value;
        return null;
    }

    @Override
    public void gatekeeperErase(int slotId) {
        //final byte[] command = new byte[5];
        //command[0] = makeGatekeeperCla();
        //command[1] = 0x14; // TODO: share constants with applet
        //command[2] = 0;
        //command[3] = 0;
        //command[4] = 4;

        //// Overkill?
        //final byte[] response = transceive(command);
        //final ByteBuffer buffer = ByteBuffer.wrap(response).order(ByteOrder.BIG_ENDIAN);
        //final int numSlots = buffer.getInt();
        //final int status = buffer.getShort();
        //if (status != 0x9000) {
        //    throw new RuntimeException("SE request not OK");
        //}
        //return numSlots;
    }

    @Override
    public void gatekeeperEraseAll() {
    }

}
