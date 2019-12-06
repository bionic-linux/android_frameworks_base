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

    boolean isRevoked(String keyId) {
        for (RevocationEntry entry : mEntries) {
            if (TextUtils.equals(entry.mStatus, RevocationEntry.STATUS_REVOKED)
                    && TextUtils.equals(entry.mKeyId, keyId)) {
                return true;
            }
        }
        return false;
    }

    void fetch(URL url) throws IOException, JSONException {
        Log.d(TAG, "Fetch from URL: " + url.toString());
        InputStream stream = null;
        String content = "";
        try {
            stream = url.openStream();
            content = toString(stream);
        } catch (IOException e) {
            throw e;
        } finally {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
        Log.d(TAG, "Fetched json content: ");
        // TODO(yochiang): Mock data for testing purpose
        content = "{\"entries\": ["
            + "{\"key_id\": \"key1\", \"status\": \"REVOKED\", \"reason\": \"reason1\"},"
            + "{\"key_id\": \"key2\", \"status\": \"REVOKED\"}"
            + "]}";
        JSONObject jsonObject = new JSONObject(content);
        if (jsonObject.has(ENTRIES)) {
            JSONArray entries = jsonObject.getJSONArray(ENTRIES);
            for (int i = 0; i < entries.length(); ++i) {
                JSONObject entry = entries.getJSONObject(i);
                Log.d(TAG, "RevocationEntry: " + entry.toString());
                mEntries.add(new RevocationEntry(entry));
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

    private static class RevocationEntry {
        private static final String KEY_ID = "key_id";
        private static final String STATUS = "status";
        private static final String REASON = "reason";

        private static final String STATUS_REVOKED = "REVOKED";

        String mKeyId = null;
        String mStatus = null;
        String mReason = "";

        RevocationEntry(JSONObject jsonObject) throws JSONException {
            mKeyId = jsonObject.getString(KEY_ID);
            mStatus = jsonObject.getString(STATUS);
            if (jsonObject.has(REASON)) {
                mReason = jsonObject.getString(REASON);
            }
        }
    }
}
