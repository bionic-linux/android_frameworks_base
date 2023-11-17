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
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.util.Log;

import java.util.Objects;

/**
 * Database for storing blobs with a key of alias strings.
 * @hide
 */
public class ConnectivityBlobStore {
    private static final String TAG = ConnectivityBlobStore.class.getSimpleName();
    private static final String TABLENAME = "blob_table";

    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLENAME + " ("
            + "owner INTEGER,"
            + "alias BLOB,"
            + "blob BLOB,"
            + "UNIQUE(owner, alias));";

    private static class DbHelper extends SQLiteOpenHelper {
        private static final int SCHEMA_VERSION = 1;
        private static final String DATABASE_FILENAME = "ConnectivityBlobStore.db";

        DbHelper(@NonNull final Context context) {
            super(context, DATABASE_FILENAME, null /* factory */, SCHEMA_VERSION);
        }

        /** Called when the database is created */
        @Override
        public void onCreate(@NonNull final SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE);
        }

        /** Called when the database is upgraded */
        @Override
        public void onUpgrade(@NonNull final SQLiteDatabase db, final int oldVersion,
                final int newVersion) {
            Log.wtf(TAG, "Unexpected onUpgrade called, upgrade not supported yet.");
        }

        /** Called when the database is downgraded */
        @Override
        public void onDowngrade(@NonNull final SQLiteDatabase db, final int oldVersion,
                final int newVersion) {
            Log.wtf(TAG, "Unexpected onDowngrade called, downgrade not supported yet.");
        }
    }

    private final DbHelper mDbHelper;

    /**
     * Construct a ConnectivityBlobStore object.
     * The database is not actually created or opened until one of the public methods are called.
     * @param context the context to access storage with.
     */
    public ConnectivityBlobStore(@NonNull final Context context) {
        Objects.requireNonNull(context);
        // This method always returns very quickly. The database is not actually created or opened
        // until getWritableDatabase() is called.
        mDbHelper = new DbHelper(context);
    }

    /**
     * Stores the blob under the alias in the database. Existing blobs by the same alias will be
     * replaced.
     * @param alias The name of the blob
     * @param blob The blob.
     * @return true if the blob was successfully added. False otherwise.
     * @hide
     */
    public boolean put(@NonNull String alias, @NonNull byte[] blob) {
        final int callerUid = Binder.getCallingUid();
        final ContentValues values = new ContentValues();
        values.put("owner", callerUid);
        values.put("alias", alias);
        values.put("blob", blob);

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        // No need for try-catch since it is done within db.replace
        // nullColumnHack is for the case where values may be empty since SQL does not allow
        // inserting a completely empty row. Since values is never empty, set this to null.
        final long res = db.replace(TABLENAME, null /* nullColumnHack */, values);
        return res > 0;
    }

    /**
     * Retrieves a blob by the name alias from the database.
     * @param alias Name of the blob to retrieve.
     * @return The unstructured blob, that is the blob that was stored using
     *         {@link com.android.internal.net.ConnectivityBlobStore#put}.
     *         Returns null if no blob was found.
     * @hide
     */
    public byte[] get(@NonNull String alias) {
        final int callerUid = Binder.getCallingUid();
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        try (Cursor cursor = db.query(TABLENAME,
                new String[] {"blob"} /* columns */,
                "owner=? AND alias=?" /* selection */,
                new String[] {Integer.toString(callerUid), alias} /* selectionArgs */,
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
     * Removes a blob by the name alias from the database.
     * @param alias Name of the blob to be removed.
     * @return True if a blob was removed. False if no such alias was found.
     * @hide
     */
    public boolean remove(@NonNull String alias) {
        final int callerUid = Binder.getCallingUid();
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int res = db.delete(TABLENAME,
                "owner=? AND alias=?" /* whereClause */,
                new String[] {Integer.toString(callerUid), alias} /* whereArgs */);
        return res > 0;
    }

    /** */
    public String[] list(@NonNull String alias) {
        // TODO: implement this
        return null;
    }
}
