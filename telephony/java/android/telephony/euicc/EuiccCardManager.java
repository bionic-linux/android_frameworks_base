/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.telephony.euicc;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.euicc.EuiccProfileInfo;
import android.util.Log;

import com.android.internal.telephony.euicc.IEuiccCardController;
import com.android.internal.telephony.euicc.IGetAllProfilesCallback;

/**
 * EuiccCardManager is the application interface to an eSIM card.
 *
 * @hide
 *
 * TODO(b/35851809): Make this a SystemApi.
 */
public class EuiccCardManager {

    private static final String TAG = "EuiccCardManager";

    private final Context mContext;

    public interface ResultCallback<T> {
        void onComplete(int resultCode, T result);
    }

    /** @hide */
    public EuiccCardManager(Context context) {
        mContext = context;
    }

    private IEuiccCardController getIEuiccCardController() {
        return IEuiccCardController.Stub.asInterface(
                ServiceManager.getService("euicc_card_controller"));
    }

    public void getAllProfiles(ResultCallback<EuiccProfileInfo[]> callback) {
        try {
            getIEuiccCardController().getAllProfiles(mContext.getOpPackageName(),
                    new IGetAllProfilesCallback.Stub() {
                        @Override
                        public void onComplete(int resultCode, EuiccProfileInfo[] profiles) {
                            callback.onComplete(resultCode, profiles);
                        }
                    });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getAllProfiles", e);
            throw e.rethrowFromSystemServer();
        }
    }
}
