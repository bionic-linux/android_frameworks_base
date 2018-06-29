/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.telephony.test;

import android.content.Context;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.telephony.test.V1_0.ITestService;

public class TestService {

    private Context mContext;

    private final ITestService.Stub sTestService = new ITestService.Stub() {
        @Override
        public String getString() {
            return TestService.this.getString();
        }
    };

    public TestService(Context context) {
        mContext = context;
    }

    public String getString() {
        return "test";
    }

    public final void setTestService() {
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        tm.setTestRadio(sTestService);
    }
}
