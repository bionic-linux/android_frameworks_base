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
 * limitations under the License.
 */

package android.net.ipmemorystore;

import android.annotation.NonNull;
import android.os.RemoteException;

/**
 * A listener for the IpMemoryStore to return a L2 key.
 * @hide
 */
public abstract class OnL2KeyResponseListener {
    /**
     * The operation has completed with the specified status.
     */
    public abstract void onL2KeyResponse(Status status, String l2Key);

    /** Converts this OnL2KeyResponseListener to a parcelable object */
    @NonNull
    public IOnL2KeyResponseListener.Stub toParcelable() {
        return new IOnL2KeyResponseListener.Stub() {
            @Override
            public void onL2KeyResponse(final StatusParcelable statusParcelable, final String l2Key)
                    throws RemoteException {
                OnL2KeyResponseListener.this.onL2KeyResponse(new Status(statusParcelable), l2Key);
            }
        };
    }
}
