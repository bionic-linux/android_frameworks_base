/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.bluetooth;

import java.io.IOException;

/**
 * A new exception to report Bluetooth related error code when RFCOMM
 * connecting failed. Upper layer can catch this exception to check
 * the result when the BluetoothSocket (RFCOMM type) connect() was invoked.
 *
 * @hide
 */
public class BluetoothIOException extends IOException {
    /* Bluetooth RFCOMM failures.*/
    /*These error codes are maped to bt_rfc_conn_status_t in
    system/bt/include/hardware/bluetooth.h*/
    public static final int RFCOMM_ERROR_SDP_CONN_FAIL = 1;
    public static final int RFCOMM_ERROR_NO_SDP_RECORD = 2;
    public static final int RFCOMM_ERROR_SECURITY      = 3;
    public static final int RFCOMM_ERROR_RFC_FAIL      = 4;
    public static final int RFCOMM_ERROR_REJECTED      = 5;
    public static final int RFCOMM_ERROR_OTHER_FAIL    = 6;

    private int mErrorCode;

    public BluetoothIOException(int error) {
        super();
        mErrorCode = error;
    }

    public BluetoothIOException(String message, int error) {
        super(message);
        mErrorCode = error;
    }

    public int getErrorCode() {
        return mErrorCode;
    }
}
