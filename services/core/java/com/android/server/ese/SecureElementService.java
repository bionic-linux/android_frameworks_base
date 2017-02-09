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

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.ese.ISecureElementService;

import com.android.server.SystemService;

/**
 * Manages communication with the embedded secure element.
 */
public class SecureElementService extends SystemService {
    private static final String TAG = "SecureElementService";

    private SecureElementConnection mSeConnection;
    private SecureElement mSe;

    public SecureElementService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        android.util.Slog.e("MYTAG", "starting the SE service!");
        mSeConnection = new SecureElementConnection();
        mSeConnection.connect(); // TODO: disconnect
        // Select the appropriate implementation for the hardware
        mSe = new JavaCardSecureElement(mSeConnection);

        publishBinderService(Context.SECURE_ELEMENT_SERVICE, mService);
    }

    // TODO: persmissions etc??
    private final IBinder mService = new ISecureElementService.Stub() {
        @Override
        public int gatekeeperGetNumSlots() throws RemoteException {
            return mSe.gatekeeperGetNumSlots();
        }

        @Override
        public void gatekeeperWrite(int slotId, byte[] key, byte[] value) throws RemoteException {
            mSe.gatekeeperWrite(slotId, key, value);
        }

        @Override
        public byte[] gatekeeperRead(int slotId, byte[] key) throws RemoteException {
            return mSe.gatekeeperRead(slotId, key);
        }

        @Override
        public void gatekeeperErase(int slotId) throws RemoteException {
            mSe.gatekeeperErase(slotId);
        }

        @Override
        public void gatekeeperEraseAll() throws RemoteException {
            mSe.gatekeeperEraseAll();
        }
    };
}
