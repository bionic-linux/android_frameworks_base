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
 * limitations under the License.
 */

package com.android.dynsystem;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

class DynamicSystemKeyRevocationList {

    private static final String TAG = "DynamicSystemKeyRevocationList";

    private static final String ENTRIES = "entries";

    ArrayList<RevocationEntry> mEntries = new ArrayList<RevocationEntry>();

    /**
     * Returns the revocation reason of a public key.
     *
     * @return A String describing the revocation reason, null if publicKey is not revoked.
     */
    String getRevokeReasonForKey(String publicKey) {
        for (RevocationEntry entry : mEntries) {
            if (TextUtils.equals(entry.mStatus, RevocationEntry.STATUS_REVOKED)
                    && TextUtils.equals(entry.mPublicKey, publicKey)) {
                return entry.mReason;
            }
        }
        return null;
    }

    /** Test if a public key is revoked. */
    boolean isRevoked(String publicKey) {
        for (RevocationEntry entry : mEntries) {
            if (TextUtils.equals(entry.mStatus, RevocationEntry.STATUS_REVOKED)
                    && TextUtils.equals(entry.mPublicKey, publicKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a DynamicSystemKeyRevocationList from a JSON String.
     *
     * @throws JSONException if jsonString is malformed.
     */
    static DynamicSystemKeyRevocationList fromJsonString(String jsonString) throws JSONException {
        Log.d(TAG, "Begin of revocation list");
        JSONObject jsonObject = new JSONObject(jsonString);
        DynamicSystemKeyRevocationList list = new DynamicSystemKeyRevocationList();
        if (jsonObject.has(ENTRIES)) {
            JSONArray entries = jsonObject.getJSONArray(ENTRIES);
            for (int i = 0; i < entries.length(); ++i) {
                JSONObject entry = entries.getJSONObject(i);
                Log.d(TAG, "RevocationEntry: " + entry.toString());
                list.mEntries.add(new RevocationEntry(entry));
            }
        }
        Log.d(TAG, "End of revocation list");
        return list;
    }

    /**
     * Creates a DynamicSystemKeyRevocationList from a URL.
     *
     * @throws IOException if url is inaccessible.
     * @throws JSONException if fetched content is malformed.
     */
    static DynamicSystemKeyRevocationList fromUrl(URL url) throws IOException, JSONException {
        Log.d(TAG, "Fetch from URL: " + url.toString());
        InputStream stream = null;
        try {
            stream = url.openStream();
            return fromJsonString(toString(stream));
        } finally {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }

    private static String toString(InputStream in) throws IOException {
        int n;
        byte[] buffer = new byte[4096];
        StringBuilder builder = new StringBuilder();
        while ((n = in.read(buffer, 0, 4096)) > -1) {
            builder.append(new String(Arrays.copyOf(buffer, n)));
        }
        return builder.toString();
    }

    static class RevocationEntry {
        private static final String PUBLIC_KEY = "public_key";
        private static final String STATUS = "status";
        private static final String REASON = "reason";

        private static final String STATUS_REVOKED = "REVOKED";

        final String mPublicKey;
        final String mStatus;
        final String mReason;

        RevocationEntry(String publicKey, String status, String reason) {
            mPublicKey = publicKey;
            mStatus = status;
            mReason = reason;
        }

        RevocationEntry(JSONObject jsonObject) throws JSONException {
            mPublicKey = jsonObject.getString(PUBLIC_KEY);
            mStatus = jsonObject.getString(STATUS);
            mReason = jsonObject.has(REASON) ? jsonObject.getString(REASON) : "";
        }
    }
}
