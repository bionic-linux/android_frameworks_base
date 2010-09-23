/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.internal.telephony;

import android.content.ContentProvider;
import android.content.UriMatcher;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Message;
import android.util.Log;
import android.os.Handler;
import com.android.internal.telephony.gsm.GSMPhone;
import android.database.MatrixCursor;

/**
 * {@hide}
 */
public class IccCustProvider extends ContentProvider {
    private static final String TAG = "IccCustProvider";

    private static final boolean DBG = false;

    private static final String[] SIM_CARD_TYPE_COLUMN_NAMES = new String[] {
        "type"
    };

    private static final int SIM_CARD_TYPE_NOT_VALID = -1;

    private static final int EVENT_START_PROCESSING = 1;

    private static final int EVENT_GET_READ_SIM_FLAG_DONE = 2;

    private static final int SIM_FLAG = 1;

    private static final String STR_TAG = "tag";

    private static final UriMatcher URL_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URL_MATCHER.addURI("icc-cust", "sim_card_flag", SIM_FLAG);
    }

    private static final int WAIT_TIMEOUT = 10000;

    private boolean mSimulator;

    private int mRecordsToLoad = 0;

    private int mSimFlagValue = SIM_CARD_TYPE_NOT_VALID;

    private final Object mLock = new Object();

    private String mSimFolder;

    private int mSimFile;

    private int mSimByte;

    private int mSimBit;

    private boolean mSimReadReady;

    private boolean mProcessingStarted;

    private final Object mProcessReadLock = new Object();

    /**
     * Handler receiving messages during reading of the SIM flag
     */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            IccFileHandler iccFh;
            byte data[];
            try {
                switch (msg.what) {
                    case EVENT_START_PROCESSING:
                        synchronized (mProcessReadLock) {
                            if (!mProcessingStarted) {
                                mProcessingStarted = true;
                                mSimFlagValue = SIM_CARD_TYPE_NOT_VALID;
                                Phone p = PhoneFactory.getDefaultPhone();
                                if (p != null) {
                                    if (p.getIccRecordsLoaded()) {
                                        iccFh = p.getIccFileHandler();
                                        iccFh.loadEFTransparent(IccConstants.MF_SIM + mSimFolder,
                                                mSimFile, mHandler.obtainMessage(
                                                        EVENT_GET_READ_SIM_FLAG_DONE));
                                        mRecordsToLoad++;
                                    }
                                }
                            }
                        }
                        break;
                    case EVENT_GET_READ_SIM_FLAG_DONE:
                        mRecordsToLoad--;
                        ar = (AsyncResult)msg.obj;
                        data = (byte[])ar.result;
                        if (ar.exception == null) {
                            if (data.length >= mSimByte) {
                                mSimFlagValue = (int)(data[mSimByte - 1] >>
                                        (mSimBit - 1)) & 0x01;
                            }
                        }
                        break;
                }
            } catch (RuntimeException exc) {
                mRecordsToLoad = 0;
                mSimFlagValue = SIM_CARD_TYPE_NOT_VALID;
            } finally {
                if (mRecordsToLoad == 0) {
                    synchronized (mLock) {
                        mSimReadReady = true;
                        mLock.notifyAll();
                    }
                }
            }
        }
    };

    @Override
    /**
     * Called when the provider is being started.
     *
     * @return true, the provider was successfully loaded.
     */
    public boolean onCreate() {
        if (DBG) log("onCreate");
        return true;
    }

    @Override
    /**
     * Query this provider for information.
     *
     * @param url URI to query.
     * @param projection Not used by this provider, only one column is returned.
     * @param selection Not needed by this provider the selection arguments are always the same.
     * @param selectionArgs Arguments pointing out the actual SIM flag: path, fileid, byte number, bit numer.
     * @param sortOrder Not used, only one row is returned.
     *
     * @return A Cursor containing the SIM-flag (the flag value is -1 if the flag is not valid.
     */
    public Cursor query(Uri url, String[] projection, String selection, String[] selectionArgs,
            String sort) {

        Cursor cursor = null;
        if (selectionArgs == null || selectionArgs.length != 4) {
            throw new IllegalArgumentException("Incorrect selection argument");
        }

        String simFolder = null;
        int simFile = 0;
        int simByte = 0;
        int simBit = 0;
        try {
            simFolder = selectionArgs[0];
            simFile = Integer.parseInt(selectionArgs[1], 16);
            simByte = Integer.parseInt(selectionArgs[2], 16);
            simBit = Integer.parseInt(selectionArgs[3], 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Incorrect selectionArgs argument");
        }

        if (simByte <= 0 || simBit <= 0 || simBit > 8) {
            throw new IllegalArgumentException("Incorrect selectionArgs argument");
        }

        switch (URL_MATCHER.match(url)) {
            case SIM_FLAG:
                int simFlag = fetchSimFlag(simFolder, simFile, simByte, simBit);
                Integer[] record = new Integer[]{new Integer(simFlag)};
                cursor = new MatrixCursor(SIM_CARD_TYPE_COLUMN_NAMES);
                ((MatrixCursor)cursor).addRow(record);
                break;

            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }

        return cursor;
    }

    @Override
    /**
     * Return the MIME type of the data at the given URI.
     *
     * @param url URI to query.
     *
     * @return MIME type string.
     */
    public String getType(Uri url) {
        String type = "unknown";
        switch (URL_MATCHER.match(url)) {
            case SIM_FLAG:
                type = "vnd.android.cursor.dir/sim-flag";
                break;

            default:
                if (DBG) log("getType IllegalArgumentException");
                throw new IllegalArgumentException("Unknown URL " + url);
        }
        return type;
    }

    @Override
    /**
     * Insert is not supported for this content provider.
     *
     * @param url URI of the insertion request.
     * @param initialValues Not used by this provider.
     *
     * @return Not supported, the method will throw an exception
     */
    public Uri insert(Uri url, ContentValues initialValues) {
        throw new UnsupportedOperationException("Cannot insert into URL: " + url);
    }

    @Override
    /**
     * Delete is not supported for this content provider.
     *
     * @param url URI of the deletion request.
     * @param where Not used by this provider.
     * @param whereArgs Not used by this provider.
     *
     * @return Not supported, the method will throw an exception
     */
    public int delete(Uri url, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Cannot delete URL: " + url);
    }

    @Override
    /**
     * Update is not supported for this content provider.
     *
     * @param url URI of the update request.
     * @param values Not used by this provider.
     * @param where Not used by this provider.
     * @param whereArgs Not used by this provider.
     *
     * @return Not supported, the method will throw an exception
     */
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Cannot update URL: " + url);
    }

    /**
     * Fetch/read specific flag from the SIM card
     *
     * @param simFolder Folder/directory to read.
     * @param simFile to read.
     * @param simByte Byte number to read.
     * @param simBit Bit number containing the flag.
     *
     * @return SIM card flag.
     */
    private synchronized int fetchSimFlag(String simFolder, int simFile, int simByte, int simBit) {
        int simFlag;
        synchronized (mLock) {
            mSimFolder = simFolder;
            mSimFile = simFile;
            mSimByte = simByte;
            mSimBit = simBit;
            mSimReadReady = false;
            while (!mSimReadReady) {
                synchronized (mProcessReadLock) {
                    mProcessingStarted = false;
                }
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_START_PROCESSING));
                try {
                    mLock.wait(WAIT_TIMEOUT);
                    synchronized (mProcessReadLock) {
                        if (!mProcessingStarted) {
                            return SIM_CARD_TYPE_NOT_VALID;
                        }
                    }
                } catch (Exception e) {
                    if (DBG) log("fetchSimFlag exception " + e.getMessage());
                }
            }
            simFlag = mSimFlagValue;
        }

        return simFlag;
    }

    private void log(String msg) {
        Log.d(TAG, "[IccCustProvider] " + msg);
    }
}
