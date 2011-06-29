/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.os.ParcelFileDescriptor;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.AndroidTestCase;
import dalvik.system.VMRuntime;

public class ParcelFileDescriptorTest extends AndroidTestCase {

    private final Object waiter = new Object();

    private boolean finalizerHasRun;

    /*
     * Tests that there are no crashes when running the finalizer if the
     * ParcelFileDescriptor was created using a null reference.
     */
    @MediumTest
    public void testNullFinalization() throws Exception {
        finalizerHasRun = false;
        try {
            ParcelFileDescriptor nullReference = null;
            ExtendedParcelFileDescriptor pfd = new ExtendedParcelFileDescriptor(nullReference);
            fail("Expected NullPointerException was not thrown.");
        } catch (NullPointerException e) {
            // Expected exception received.
        }
        System.gc();
        final VMRuntime runtime = VMRuntime.getRuntime();
        runtime.runFinalizationSync();

        synchronized (waiter) {
            // Wait for heap worker thread to run.
            waiter.wait(1000);
            assertTrue(finalizerHasRun);
        }
    }

    class ExtendedParcelFileDescriptor extends ParcelFileDescriptor {
        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            finalizerHasRun = true;
            synchronized (waiter) {
                waiter.notify();
            }
        }

        public ExtendedParcelFileDescriptor(ParcelFileDescriptor descriptor) {
            super(descriptor);
        }
    }
}
