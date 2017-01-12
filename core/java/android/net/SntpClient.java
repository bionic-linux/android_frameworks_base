/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.os.SystemClock;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.android.internal.util.BitUtils.getUint8;
import static com.android.internal.util.BitUtils.getUint32;
import static com.android.internal.util.BitUtils.uint8;

/**
 * {@hide}
 *
 * Simple SNTP client class for retrieving network time.
 *
 * Sample usage:
 * <pre>SntpClient client = new SntpClient();
 * if (client.requestTime("time.foo.com")) {
 *     long now = client.getNtpTime() + SystemClock.elapsedRealtime() - client.getNtpTimeReference();
 * }
 * </pre>
 */
public class SntpClient {
    private static final String TAG = "SntpClient";
    private static final boolean DBG = true;

    private static final int REFERENCE_TIME_OFFSET = 16;
    private static final int ORIGINATE_TIME_OFFSET = 24;
    private static final int RECEIVE_TIME_OFFSET = 32;
    private static final int TRANSMIT_TIME_OFFSET = 40;
    private static final int NTP_PACKET_SIZE = 48;

    private static final int NTP_PORT = 123;
    private static final int NTP_MODE_CLIENT = 3;
    private static final int NTP_MODE_SERVER = 4;
    private static final int NTP_MODE_BROADCAST = 5;
    private static final int NTP_VERSION = 3;

    private static final int NTP_LEAP_NOSYNC = 3;
    private static final int NTP_STRATUM_DEATH = 0;
    private static final int NTP_STRATUM_MAX = 15;

    // Number of seconds between Jan 1, 1900 and Jan 1, 1970
    // 70 years plus 17 leap days
    private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

    // system time computed from NTP server response
    private long mNtpTimeMs;

    // value of SystemClock.elapsedRealtime() corresponding to mNtpTimeMs
    private long mNtpTimeReferenceMs;

    // round trip time in milliseconds
    private long mRoundTripTimeMs;

    private static class InvalidServerReplyException extends Exception {
        public InvalidServerReplyException(String message) {
            super(message);
        }
    }

    /**
     * Sends an SNTP request to the given host and processes the response.
     *
     * @param host host name of the server.
     * @param timeout network timeout in milliseconds.
     * @return true if the transaction was successful.
     */
    public boolean requestTime(String host, int timeout) {
        InetAddress address = null;
        try {
            address = InetAddress.getByName(host);
        } catch (Exception e) {
            EventLogTags.writeNtpFailure(host, e.toString());
            if (DBG) Log.d(TAG, "request time failed: " + e);
            return false;
        }
        return requestTime(address, NTP_PORT, timeout);
    }

    public boolean requestTime(InetAddress address, int port, int timeout) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout);

            final int length = NTP_PACKET_SIZE;
            ByteBuffer buffer = ByteBuffer.allocate(length);
            DatagramPacket request = new DatagramPacket(buffer.array(), length, address, port);

            // set mode = 3 (client) and version = 3
            // mode is in low 3 bits of first byte
            // version is in bits 3-5 of first byte
            buffer.put(0, (byte) (NTP_MODE_CLIENT | (NTP_VERSION << 3)));

            // get current time and write it to the request packet
            final long requestTimeMs = System.currentTimeMillis();
            final long requestTicksMs = SystemClock.elapsedRealtime();
            writeTimeStampMs(buffer, TRANSMIT_TIME_OFFSET, requestTimeMs);

            socket.send(request);

            // read the response
            DatagramPacket response = new DatagramPacket(buffer.array(), length);
            socket.receive(response);
            final long responseTicksMs = SystemClock.elapsedRealtime();
            final long tickRttMs = responseTicksMs - requestTicksMs;
            final long responseTimeMs = requestTimeMs + tickRttMs;

            // extract the results
            final byte leap = (byte) (getUint8(buffer, 0) >> 6);
            final byte mode = (byte) (getUint8(buffer, 0) & 0x7);
            final int stratum = getUint8(buffer, 1);
            final long originateTimeMs = readTimeStampMs(buffer, ORIGINATE_TIME_OFFSET);
            final long receiveTimeMs = readTimeStampMs(buffer, RECEIVE_TIME_OFFSET);
            final long transmitTimeMs = readTimeStampMs(buffer, TRANSMIT_TIME_OFFSET);

            /* do sanity check according to RFC */
            // TODO: validate originateTimeMs == requestTimeMs.
            checkValidServerReply(leap, mode, stratum, transmitTimeMs);

            final long roundTripTimeMs =
                    responseTicksMs - requestTicksMs - (transmitTimeMs - receiveTimeMs);
            // receiveTimeMs = originateTimeMs + transit + skew
            // responseTimeMs = transmitTimeMs + transit - skew
            // clockOffset = ((receiveTimeMs - originateTimeMs) + (transmitTimeMs - responseTimeMs))/2
            //             = ((originateTimeMs + transit + skew - originateTimeMs) +
            //                (transmitTimeMs - (transmitTimeMs + transit - skew)))/2
            //             = ((transit + skew) + (transmitTimeMs - transmitTimeMs - transit + skew))/2
            //             = (transit + skew - transit + skew)/2
            //             = (2 * skew)/2 = skew
            final long clockOffsetMs =
                    ((receiveTimeMs - originateTimeMs) + (transmitTimeMs - responseTimeMs))/2;
            EventLogTags.writeNtpSuccess(address.toString(), roundTripTimeMs, clockOffsetMs);
            if (DBG) {
                Log.d(TAG, "round trip: " + roundTripTimeMs + "ms, " +
                        "clock offset: " + clockOffsetMs + "ms");
            }

            // save our results - use the times on this side of the network latency
            // (response rather than request time)
            mNtpTimeMs = responseTimeMs + clockOffsetMs;
            mNtpTimeReferenceMs = responseTicksMs;
            mRoundTripTimeMs = roundTripTimeMs;
        } catch (Exception e) {
            EventLogTags.writeNtpFailure(address.toString(), e.toString());
            if (DBG) Log.d(TAG, "request time failed: " + e);
            return false;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }

        return true;
    }

    /**
     * Returns the time computed from the NTP transaction.
     *
     * @return time value computed from NTP server response.
     */
    public long getNtpTime() {
        return mNtpTimeMs;
    }

    /**
     * Returns the reference clock value (value of SystemClock.elapsedRealtime())
     * corresponding to the NTP time.
     *
     * @return reference clock corresponding to the NTP time.
     */
    public long getNtpTimeReference() {
        return mNtpTimeReferenceMs;
    }

    /**
     * Returns the round trip time of the NTP transaction
     *
     * @return round trip time in milliseconds.
     */
    public long getRoundTripTime() {
        return mRoundTripTimeMs;
    }

    private static void checkValidServerReply(
            byte leap, byte mode, int stratum, long transmitTime)
            throws InvalidServerReplyException {
        if (leap == NTP_LEAP_NOSYNC) {
            throw new InvalidServerReplyException("unsynchronized server");
        }
        if ((mode != NTP_MODE_SERVER) && (mode != NTP_MODE_BROADCAST)) {
            throw new InvalidServerReplyException("untrusted mode: " + mode);
        }
        if ((stratum == NTP_STRATUM_DEATH) || (stratum > NTP_STRATUM_MAX)) {
            throw new InvalidServerReplyException("untrusted stratum: " + stratum);
        }
        if (transmitTime == 0) {
            throw new InvalidServerReplyException("zero transmitTime");
        }
    }

    /**
     * Reads the NTP time stamp at the given offset in the buffer and returns
     * it as a system time (milliseconds since January 1, 1970).
     */
    private long readTimeStampMs(ByteBuffer buffer, int offset) {
        long seconds = getUint32(buffer, offset);
        long milliseconds = getUint32(buffer, offset + 4);
        // Special case: zero means zero.
        if (seconds == 0 && milliseconds == 0) {
            return 0;
        }
        return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((milliseconds * 1000L) / 0x100000000L);
    }

    /**
     * Writes system time (milliseconds since January 1, 1970) as an NTP time stamp
     * at the given offset in the buffer.
     */
    private void writeTimeStampMs(ByteBuffer buffer, int offset, long timestampMs) {
        // Special case: zero means zero.
        if (timestampMs == 0) {
            buffer.putLong(offset, 0);
            return;
        }

        long seconds = timestampMs / 1000L;
        long milliseconds = timestampMs - seconds * 1000L;
        seconds += OFFSET_1900_TO_1970;
        buffer.putInt(offset, (int) seconds);
        buffer.putInt(offset + 4, (int) milliseconds);
        // low order bits should be random data
        buffer.put(offset + 4 + 3, (byte) (Math.random() * 255.0));
    }
}
