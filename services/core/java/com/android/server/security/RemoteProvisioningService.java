/**
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.security;

import android.content.Context;
import android.os.RemoteException;
import com.android.security.rkpd.service.Registration;
import com.android.server.security.remoteprovisioning.IGetRegistrationCallback;
import com.android.server.security.remoteprovisioning.IRegistration;
import com.android.server.security.remoteprovisioning.IRemoteProvisioning;
import android.util.Log;

import com.android.server.SystemService;

/**
 * A {@link SystemService} that implements the remote provisioning system service.
 * This service is backed by a mainline module, allowing the underlying implementation
 * to be updated. The code here is mostly just a thin proxy for the code in
 * com.android.rkpd.
 * @hide
 */
public class RemoteProvisioningService extends IRemoteProvisioning.Stub {
    static final String TAG = "RemoteProvisioningSysSvc";

    Context mContext;

    public RemoteProvisioningService(Context context) {
       mContext = context;
    }

    @Override
    public void getRegistration(String irpcName, IGetRegistrationCallback callback) throws RemoteException {
        Log.i(TAG, "getRegistration(" + irpcName + ")");
        Registration.getRegistration(mContext, irpcName);
        callback.onError("getRegistration not yet implemented");
    }

    @Override
    public void cancelGetRegistration(IGetRegistrationCallback callback) throws RemoteException {
        Log.i(TAG, "cancelGetRegistration()");
        callback.onError("cancelGetRegistration not yet implemented");
    }
}
