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

package android.telephony.ims;

import android.os.RemoteException;

import com.android.ims.internal.IImsConfigCallback;

/**
 *
 */

public class ImsConfigCallback {

    private final IImsConfigCallback mImsConfigCallbackBinder = new IImsConfigCallback.Stub() {

        @Override
        public void onProvisionedValueChanged(int item, int value) throws RemoteException {
            ImsConfigCallback.this.onProvisionedValueChanged(item, value);
        }

        @Override
        public void onProvisionedValueChangedString(int item, String value) throws RemoteException {
            ImsConfigCallback.this.onProvisionedValueChanged(item, value);
        }

        @Override
        public void onSetProvisionedValueResult(int item, int status) throws RemoteException {
            ImsConfigCallback.this.onSetProvisionedValueResult(item, status);
        }

        @Override
        public void onGetProvisionedValue(int item, int value) throws RemoteException {
            ImsConfigCallback.this.onGetProvisionedValue(item, value);
        }

        @Override
        public void onGetProvisionedStringValue(int item, String value) throws RemoteException {
            ImsConfigCallback.this.onGetProvisionedValue(item, value);
        }
    };

    public IImsConfigCallback getBinder() {
        return mImsConfigCallbackBinder;
    }

    public void onProvisionedValueChanged(int item, int value) {

    }

    public void onProvisionedValueChanged(int item, String value) {

    }

    public void onSetProvisionedValueResult(int item, int status) {

    }

    public void onGetProvisionedValue(int item, int value) {

    }

    public void onGetProvisionedValue(int item, String value) {

    }
}
