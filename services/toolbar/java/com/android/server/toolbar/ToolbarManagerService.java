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
 * limitations under the License
 */

package com.android.server.toolbar;

import android.app.IToolbarManager;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import com.android.internal.infra.AndroidFuture;
import com.android.server.infra.AbstractMasterSystemService;

public final class ToolbarManagerService extends
        AbstractMasterSystemService<ToolbarManagerService, ToolbarManagerPerUserService> {

    /**
     * Initializes the system service.
     * <p>
     * Subclasses must define a single argument constructor that accepts the context
     * and passes it to super.
     * </p>
     *
     * @param context The system server context.
     */
    public ToolbarManagerService(Context context) {
        super(context, /* serviceNameResolver= */ null, /* disallowProperty= */ null);
    }

    @Override
    public void onStart() {
        publishBinderService(Context.TOOLBAR_SERVICE, new ToolbarManagerServiceStub());
    }

    @Override
    protected ToolbarManagerPerUserService newServiceLocked(int resolvedUserId, boolean disabled) {
        return new ToolbarManagerPerUserService(this, mLock, resolvedUserId);
    }

    private final class ToolbarManagerServiceStub extends IToolbarManager.Stub {

        @Override
        public void getToolbarSlice(Intent intent, int w, int h, AndroidFuture future) {
            synchronized (mLock) {
                int userId = UserHandle.getCallingUserId();
                getServiceForUserLocked(userId).getToolbarSlice(intent, w, h, future);
            }
        }
    }
}
