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

package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityTrace;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.os.IBinder;

import com.android.server.accessibility.AbstractAccessibilityServiceConnection;
import com.android.server.accessibility.AccessibilitySecurityPolicy;
import com.android.server.accessibility.AccessibilityUserState;
import com.android.server.accessibility.AccessibilityWindowManager;
import com.android.server.accessibility.KeyEventDispatcher;
import com.android.server.accessibility.MotionEventInjector;
import com.android.server.accessibility.SystemActionPerformer;
import com.android.server.accessibility.magnification.MagnificationProcessor;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerInternal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ProxyAccessibilityServiceConnectionTest {
    @Mock
    AccessibilityUserState mMockUserState;
    @Mock
    Context mMockContext;
    @Mock
    AccessibilityServiceInfo mMockServiceInfo;
    @Mock
    ResolveInfo mMockResolveInfo;
    @Mock
    AccessibilitySecurityPolicy mMockSecurityPolicy;
    @Mock
    AccessibilityWindowManager mMockA11yWindowManager;
    @Mock
    ActivityTaskManagerInternal mMockActivityTaskManagerInternal;
    @Mock AbstractAccessibilityServiceConnection.SystemSupport mMockSystemSupport;
    @Mock
    AccessibilityTrace mMockA11yTrace;
    @Mock
    WindowManagerInternal mMockWindowManagerInternal;
    @Mock
    SystemActionPerformer mMockSystemActionPerformer;
    @Mock
    KeyEventDispatcher mMockKeyEventDispatcher;
    @Mock
    MagnificationProcessor mMockMagnificationProcessor;
    @Mock
    IBinder mMockIBinder;
    @Mock
    IAccessibilityServiceClient mMockServiceClient;
    @Mock
    MotionEventInjector mMockMotionEventInjector;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

//    @Test
//    public void
    //

    // binder died



}
