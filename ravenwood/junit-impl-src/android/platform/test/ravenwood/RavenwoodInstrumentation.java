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

import static org.mockito.Mockito.mock;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;

import com.android.hoststubgen.hosthelper.HostTestUtils;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

public class RavenwoodInstrumentation extends Instrumentation {

    private final UiAutomation mUiAutomationMock;

    private static class UiAutomationMockAnswer implements Answer {

        private Set<String> mAdoptedPermissions = Collections.emptySet();

        @Override
        public Object answer(InvocationOnMock invocation) {
            Method m = invocation.getMethod();
            return switch (m.getName()) {
                case "adoptShellPermissionIdentity" -> {
                    if (m.getParameterCount() == 0 || invocation.getArgument(0) == null) {
                        mAdoptedPermissions = UiAutomation.ALL_PERMISSIONS;
                    } else {
                        mAdoptedPermissions = (Set) Set.of(invocation.getArguments());
                    }
                    yield null;
                }
                case "dropShellPermissionIdentity" -> {
                    mAdoptedPermissions = Collections.emptySet();
                    yield null;
                }
                case "getAdoptedShellPermissions" -> mAdoptedPermissions;
                default -> {
                    HostTestUtils.onThrowMethodCalled();
                    yield null;
                }
            };
        }
    }

    public RavenwoodInstrumentation(Context instrContext, Context appContext) {
        basicInit(instrContext, appContext);
        mUiAutomationMock = mock(UiAutomation.class, new UiAutomationMockAnswer());
    }

    @Override
    public UiAutomation getUiAutomation(int flags) {
        return mUiAutomationMock;
    }
}
