/*
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

import android.util.Log;

import com.android.security.rkpd.service.Registration;
import com.android.server.security.remoteprovisioning.IGetKeyCallback;
import com.android.server.security.remoteprovisioning.IRegistration;

/**
 * Implementation of the {@link IRegistration} binder that wraps up the module api for {@link
 * Registration}.
 */
final class RegistrationBinder extends IRegistration.Stub {
    static final String TAG = RemoteProvisioningService.TAG;

    Registration mRegistration;

    RegistrationBinder(Registration registration) {
        mRegistration = registration;
    }

    @Override
    public void getKey(int keyId, IGetKeyCallback callback) {
        Log.e(TAG, "RegistrationBinder.getKey NOT YET IMPLEMENTED");
    }

    @Override
    public void cancelGetKey(IGetKeyCallback callback) {
        Log.e(TAG, "RegistrationBinder.cancelGetKey NOT YET IMPLEMENTED");
    }

    @Override
    public void storeUpgradedKey(int keyId, byte[] newKeyBlob) {
        Log.e(TAG, "RegistrationBinder.storeUpgradedKey NOT YET IMPLEMENTED");
    }
}
