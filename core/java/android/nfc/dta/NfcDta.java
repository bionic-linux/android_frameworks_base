 /*
  * Copyright (C) 2017 NXP Semiconductors
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

package android.nfc.dta;

import android.content.Context;
import android.nfc.INfcDta;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;

/**
 * This class provides the primary API for DTA operations.
*/
public final class NfcDta {
    private static final String TAG = "NfcDta";

    private static INfcDta sService;
    private static HashMap<Context, NfcDta> sNfcDtas = new HashMap<Context, NfcDta>();

    private final Context mContext;

    private NfcDta(Context context, INfcDta service) {
        mContext = context.getApplicationContext();
        sService = service;
    }

    /**
     * Helper to get an instance of this class.
     *
     * @param adapter A reference to an NfcAdapter object.
     * @return
     */
    public static synchronized NfcDta getInstance(NfcAdapter adapter) {
        if (adapter == null) throw new NullPointerException("NfcAdapter is null");
        Context context = adapter.getContext();
        if (context == null) {
            Log.e(TAG, "NfcAdapter context is null.");
            throw new UnsupportedOperationException();
        }

        NfcDta manager = sNfcDtas.get(context);
        if (manager == null) {
            INfcDta service = adapter.getNfcDtaInterface();
            if (service == null) {
                Log.e(TAG, "This device does not implement the INfcDta interface.");
                throw new UnsupportedOperationException();
            }
            manager = new NfcDta(context, service);
            sNfcDtas.put(context, manager);
        }
        return manager;
    }

    /**
     * Configure DTA
     * This call allows to send the DTA configuration for nfc forum test cases.
     * @return the status of the current operation.
     * @throws IOException If a failure occurred during snepDtaCmd
     */
    public boolean configure(Bundle bundle) {

        try {
            return sService.configure(bundle);
        } catch (RemoteException e) {
            return false;
        }
    }

}
