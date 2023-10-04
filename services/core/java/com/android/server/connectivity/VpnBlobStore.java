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

package com.android.server.connectivity;

import android.annotation.NonNull;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Objects;

/**
 * Database for storing blobs with a key of alias strings.
 */
public class VpnBlobStore {
    private static final String TAG = VpnBlobStore.class.getSimpleName();
    private static final String TABLENAME = "blob_table";

    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLENAME + " ("
            + "owner INTEGER,"
            + "alias BLOB,"
            + "blob BLOB,"
            + "UNIQUE(owner, alias));";

    private static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLENAME;

    private static class DbHelper extends SQLiteOpenHelper {
        private static final int SCHEMA_VERSION = 1;
        private static final String DATABASE_FILENAME = "VpnBlobStore.db";

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
            // No upgrade supported yet.
            db.execSQL(DROP_TABLE);
            onCreate(db);
        }

        /** Called when the database is downgraded */
        @Override
        public void onDowngrade(@NonNull final SQLiteDatabase db, final int oldVersion,
                final int newVersion) {
            // Downgrades always nuke all data and recreate an empty table.
            db.execSQL(DROP_TABLE);
            onCreate(db);
        }
    }

    private final DbHelper mOpenHelper;

    public VpnBlobStore(@NonNull final Context context) {
        Objects.requireNonNull(context);
        mOpenHelper = new DbHelper(context);
    }

    /** */
    public boolean put(@NonNull String alias, @NonNull byte[] blob) {
        // TOOD: implement this
        return false;
    }

    /** */
    public byte[] get(@NonNull String alias) {
        // TODO: implement this
        return null;
    }

    /** */
    public boolean remove(@NonNull String alias) {
        // TODO: implement this
        return false;
    }

    /** */
    public String[] list(@NonNull String alias) {
        // TODO: implement this
        return null;
    }
}
