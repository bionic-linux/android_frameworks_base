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
package android.platform.test.ravenwood;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;

import com.android.hoststubgen.hosthelper.HostTestUtils;

public class RavenwoodInstrumentation extends Instrumentation {

    private final UiAutomation mUiAutomationMock;

    public RavenwoodInstrumentation(Context instrContext, Context appContext) {
        basicInit(instrContext, appContext);
        mUiAutomationMock = mock(UiAutomation.class, invocation -> {
            HostTestUtils.onThrowMethodCalled();
            return null;
        });
        doNothing().when(mUiAutomationMock).adoptShellPermissionIdentity();
        doNothing().when(mUiAutomationMock).adoptShellPermissionIdentity(any());
        doNothing().when(mUiAutomationMock).dropShellPermissionIdentity();
    }

    @Override
    public UiAutomation getUiAutomation(int flags) {
        return mUiAutomationMock;
    }
}
