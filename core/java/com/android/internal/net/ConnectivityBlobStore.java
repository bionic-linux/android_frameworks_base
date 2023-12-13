/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.internal.net;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.io.File;

/**
 * Database for storing blobs with a key of name strings.
 * @hide
 */
public class ConnectivityBlobStore {
    private static final String TAG = ConnectivityBlobStore.class.getSimpleName();
    private static final String TABLENAME = "blob_table";
    private static final String ROOT_DIR = "/data/misc/connectivityblobdb/";

    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLENAME + " ("
            + "name BLOB PRIMARY KEY,"
            + "blob BLOB)";

    private SQLiteDatabase mDb;

    /**
     * Construct a ConnectivityBlobStore object.
     *
     * @param dbName the filename of the database to create/access.
     */
    public ConnectivityBlobStore(String dbName) {
        this(new File(ROOT_DIR + dbName));
    }

    @VisibleForTesting
    public ConnectivityBlobStore(File file) {
        final SQLiteDatabase.OpenParams params = new SQLiteDatabase.OpenParams.Builder()
                .addOpenFlags(SQLiteDatabase.CREATE_IF_NECESSARY)
                .addOpenFlags(SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING)
                .build();
        mDb = SQLiteDatabase.openDatabase(file, params);
        mDb.execSQL(CREATE_TABLE);
    }

    /**
     * Stores the blob under the name in the database. Existing blobs by the same name will be
     * replaced.
     *
     * @param name The name of the blob
     * @param blob The blob.
     * @return true if the blob was successfully added. False otherwise.
     * @hide
     */
    public boolean put(@NonNull String name, @NonNull byte[] blob) {
        final ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("blob", blob);

        // No need for try-catch since it is done within db.replace
        // nullColumnHack is for the case where values may be empty since SQL does not allow
        // inserting a completely empty row. Since values is never empty, set this to null.
        final long res = mDb.replace(TABLENAME, null /* nullColumnHack */, values);
        return res > 0;
    }

    /**
     * Retrieves a blob by the name from the database.
     *
     * @param name Name of the blob to retrieve.
     * @return The unstructured blob, that is the blob that was stored using
     *         {@link com.android.internal.net.ConnectivityBlobStore#put}.
     *         Returns null if no blob was found.
     * @hide
     */
    public byte[] get(@NonNull String name) {
        try (Cursor cursor = mDb.query(TABLENAME,
                new String[] {"blob"} /* columns */,
                "name=?" /* selection */,
                new String[] {name} /* selectionArgs */,
                null /* groupBy */,
                null /* having */,
                null /* orderBy */)) {
            if (cursor.moveToFirst()) {
                return cursor.getBlob(0);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Error in get " + e);
        }

        return null;
    }

    /**
     * Removes a blob by the name from the database.
     *
     * @param name Name of the blob to be removed.
     * @return True if a blob was removed. False if no such name was found.
     * @hide
     */
    public boolean remove(@NonNull String name) {
        try {
            final int res = mDb.delete(TABLENAME,
                    "name=?" /* whereClause */,
                    new String[] {name} /* whereArgs */);
            return res > 0;
        } catch (SQLException e) {
            Log.e(TAG, "Error in remove " + e);
            return false;
        }
    }
}
