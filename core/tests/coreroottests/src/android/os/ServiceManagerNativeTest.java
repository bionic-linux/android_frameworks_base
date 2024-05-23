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

package android.os;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.testng.Assert.assertThrows;

import android.os.ServiceManagerNative;
import com.android.internal.os.BinderInternal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;

import androidx.test.filters.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;

@SmallTest
public class ServiceManagerNativeTest {

    @SmallTest
    @Test
    public void testServiceManagerNativeSecurityException() throws RemoteException {
        // Find the service manager
         IServiceManager sServiceManager = ServiceManagerNative
                .asInterface(Binder.allowBlocking(BinderInternal.getContextObject()));

        Binder binder = new Binder();
        sServiceManager.addService("InalidName!!!",  binder,
            anyBoolean(), anyInt());

    }
}
