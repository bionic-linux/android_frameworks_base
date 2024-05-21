/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.app.supervision;

import static android.app.KeyguardManager.ACTION_CONFIRM_PARENT_CREDENTIAL;
import static android.app.KeyguardManager.EXTRA_DESCRIPTION;
import static android.app.KeyguardManager.EXTRA_TITLE;

import android.annotation.FlaggedApi;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.app.supervision.flags.Flags;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.UserHandle;

/**
 * Service for handling parental supervision.
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_API)
@SystemService(Context.SUPERVISION_SERVICE)
public class SupervisionManager {
    private final Context mContext;
    private final ISupervisionManager mService;

    /**
     * @hide
     */
    @UnsupportedAppUsage
    public SupervisionManager(Context context, ISupervisionManager service) {
        mContext = context;
        mService = service;
    }

    /**
     * Returns whether the device is supervised.
     *
     * @hide
     */
    public boolean isSupervisionEnabled() {
        try {
            return mService.isSupervisionEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a parent for a user if set.
     *
     * <p>If parent is not set for this user, returns {@code null}.
     * @hide
     */
    @SystemApi
    @Nullable
    public UserHandle getParentUser() {
        try {
            int parentUser = mService.getParentUser();
            return parentUser != UserHandle.USER_NULL ? new UserHandle(parentUser) : null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Create an intent to confirm parent credentials for the current user. */
    @Nullable
    public Intent createConfirmParentCredentialsIntent() {
        return new Intent(ACTION_CONFIRM_PARENT_CREDENTIAL)
            .putExtra(EXTRA_TITLE, "Confirm you are a parent")
            .putExtra(EXTRA_DESCRIPTION, "Do you approve?")
            .setPackage("com.android.settings");
    }


}
