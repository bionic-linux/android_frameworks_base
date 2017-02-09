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

// TODO: make singleton?
class SecureElementConnection {
    private native int nativeConnect();
    private native int nativeDisconnect();
    private native int nativeTransceive(byte[] command, byte[] response);

    public void connect() {
        if (nativeConnect() < 0) {
            // TODO: get the error message from libese
            throw new RuntimeException("Failed to connect to SE");
        }
    }

    public void disconnect() {
        if (nativeDisconnect() < 0) {
            // TODO: get the error message from libese
            // What are you meant to do about it?!
            throw new RuntimeException("Failed to disconnect from SE");
        }
    }

    /**
     * @throws ArrayIndexOutOfBoundsException if the response was larger than the provided buffer.
     */
    public synchronized void transceive(byte[] command, byte[] response) {
        final int received = nativeTransceive(command, response);
        if (received > response.length) {
            // TODO: is this a good way to do things?
            throw new ArrayIndexOutOfBoundsException("Response was " + response + " bytes but only " 
                    + response.length + " bytes available in buffer");
        }
    }
}
