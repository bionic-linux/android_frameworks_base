package android.telephony;

import static android.Manifest.permission.READ_PHONE_STATE;
import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.UserHandle;
import android.net.Uri;
import android.provider.BaseColumns;
import android.telephony.Rlog;

import com.android.internal.telephony.TelephonyIntents;

import java.util.ArrayList;
import java.util.List;

/**
 *@hide
 */
public final class SimInfoManager implements BaseColumns {
    private static final String LOG_TAG = "PHONE";

    public static final Uri CONTENT_URI = 
            Uri.parse("content://telephony/siminfo");

    public static final int DEFAULT_INT_VALUE = -100;

    public static final String DEFAULT_STRING_VALUE = "N/A";

    /**
     * The ICC ID of a SIM.
     * <P>Type: TEXT (String)</P>
     */
    public static final String ICC_ID = "icc_id";

    /**
     * <P>Type: INTEGER (int)</P>
     */
    public static final String SIM_ID = "sim_id";

    public static final int SIM_NOT_INSERTED = -1;

    /**
     * The display name of a SIM.
     * <P>Type: TEXT (String)</P>
     */
    public static final String DISPLAY_NAME = "display_name";

    public static final int DEFAULT_NAME_RES = com.android.internal.R.string.unknownName;

    /**
     * The display name source of a SIM.
     * <P>Type: INT (int)</P>
     */
    public static final String NAME_SOURCE = "name_source";

    public static final int DEFAULT_SOURCE = 0;

    public static final int SIM_SOURCE = 1;

    public static final int USER_INPUT = 2;

    /**
     * The color of a SIM.
     * <P>Type: INTEGER (int)</P>
     */
    public static final String COLOR = "color";

    public static final int COLOR_1 = 0;

    public static final int COLOR_2 = 1;

    public static final int COLOR_3 = 2;

    public static final int COLOR_4 = 3;

    public static final int COLOR_DEFAULT = COLOR_1;

    /**
     * The phone number of a SIM.
     * <P>Type: TEXT (String)</P>
     */
    public static final String NUMBER = "number";
    
    /**
     * The number display format of a SIM.
     * <P>Type: INTEGER (int)</P>
     */
    public static final String DISPLAY_NUMBER_FORMAT = "display_number_format";

    public static final int DISPALY_NUMBER_NONE = 0;

    public static final int DISPLAY_NUMBER_FIRST = 1;

    public static final int DISPLAY_NUMBER_LAST = 2;

    public static final int DISLPAY_NUMBER_DEFAULT = DISPLAY_NUMBER_FIRST;

    /**
     * Permission for data roaming of a SIM.
     * <P>Type: INTEGER (int)</P>
     */
    public static final String DATA_ROAMING = "data_roaming";

    public static final int DATA_ROAMING_ENABLE = 1;

    public static final int DATA_ROAMING_DISABLE = 0;

    public static final int DATA_ROAMING_DEFAULT = DATA_ROAMING_DISABLE;

    private static final int RES_TYPE_BACKGROUND_DARK = 0;

    private static final int RES_TYPE_BACKGROUND_LIGHT = 1;

    private static final int[] sSimBackgroundDarkRes = setSimResource(RES_TYPE_BACKGROUND_DARK);

    private static final int[] sSimBackgroundLightRes = setSimResource(RES_TYPE_BACKGROUND_LIGHT);

    private SimInfoManager() {
    }

    /**
     * A SimInfoRecord instance represent one record in siminfo database
     * @param mSimInfoIdx SIM index in database
     * @param mIccId SIM IccId string
     * @param mSimId SIM in slot, 0: SIM1, 1: SIM2, 2: SIM3, 3: SIM4
     * @param mDisplayName SIM display name shown in SIM management
     * @param mNameSource Source of mDisplayName, 0: default source, 1: SIM source, 2: user source
     * @param mColor SIM color, 0: blue, 1: oprange, 2: green, 3: purple
     * @param mNumber Phone number string
     * @param mDispalyNumberFormat Display format of mNumber, 0: display none, 1: display number first, 2: display number last
     * @param mDataRoaming Data Roaming enable/disable status, 0: Don't allow data when roaming, 1:Allow data when roaming
     * @param mSimIconRes The SIM icon displayed by UI
     */
    public static class SimInfoRecord {
        public long mSimInfoIdx;
        public String mIccId;
        public int mSimId;
        public String mDisplayName;
        public int mNameSource;
        public int mColor;
        public String mNumber;
        public int mDispalyNumberFormat;
        public int mDataRoaming;
        public int[] mSimIconRes;
        private SimInfoRecord() {
            mSimInfoIdx = -1;
            mIccId = "";
            mSimId = SIM_NOT_INSERTED;
            mDisplayName = "";
            mNameSource = DEFAULT_SOURCE;
            mColor = COLOR_DEFAULT;
            mNumber = "";
            mDispalyNumberFormat = DISLPAY_NUMBER_DEFAULT;
            mDataRoaming = DATA_ROAMING_DEFAULT;
            mSimIconRes = new int[2];
        }
    }

    /**
     * Broadcast when siminfo settings has chanded
     * @simInfoIdx The unique SimInfoRecord index in database
     * @param columnName The column that is updated
     * @param intContent The updated integer value
     * @param stringContent The updated string value
     */
     private static void broadcastSimInfoContentChanged(Context context, long simInfoIdx, 
            String columnName, int intContent, String stringContent) {
        Intent intent = new Intent(TelephonyIntents.ACTION_SIMINFO_CONTENT_CHANGE);
        intent.putExtra(BaseColumns._ID, simInfoIdx);
        intent.putExtra(TelephonyIntents.EXTRA_COLUMN_NAME, columnName);
        intent.putExtra(TelephonyIntents.EXTRA_INT_CONTENT, intContent);
        intent.putExtra(TelephonyIntents.EXTRA_STRING_CONTENT, stringContent);
        if (intContent != DEFAULT_INT_VALUE) {
            logd("SimInfoRecord" + simInfoIdx + " changed, " + columnName + " -> " +  intContent);
        } else {
            logd("SimInfoRecord" + simInfoIdx + " changed, " + columnName + " -> " +  stringContent);
        }
        context.sendBroadcast(intent);
    }

    /**
     * New SimInfoRecord instance and fill in detail info
     * @param cursor
     * @return the query result of desired SimInfoRecord
     */
    private static SimInfoRecord getSimInfoRecord(Cursor cursor) {
        SimInfoRecord info = new SimInfoRecord();
        info.mSimInfoIdx = cursor.getLong(cursor.getColumnIndexOrThrow(_ID));
        info.mIccId = cursor.getString(cursor.getColumnIndexOrThrow(ICC_ID));
        info.mSimId = cursor.getInt(cursor.getColumnIndexOrThrow(SIM_ID));
        info.mDisplayName = cursor.getString(cursor.getColumnIndexOrThrow(DISPLAY_NAME));
        info.mNameSource = cursor.getInt(cursor.getColumnIndexOrThrow(NAME_SOURCE));
        info.mColor = cursor.getInt(cursor.getColumnIndexOrThrow(COLOR));
        info.mNumber = cursor.getString(cursor.getColumnIndexOrThrow(NUMBER));
        info.mDispalyNumberFormat = cursor.getInt(cursor.getColumnIndexOrThrow(DISPLAY_NUMBER_FORMAT));
        info.mDataRoaming = cursor.getInt(cursor.getColumnIndexOrThrow(DATA_ROAMING));

        int size = sSimBackgroundDarkRes.length;
        if (info.mColor >= 0 && info.mColor < size) {
            info.mSimIconRes[RES_TYPE_BACKGROUND_DARK] = sSimBackgroundDarkRes[info.mColor];
            info.mSimIconRes[RES_TYPE_BACKGROUND_LIGHT] = sSimBackgroundLightRes[info.mColor];
        }
        logd("[getSimInfoRecord] simInfoIdx:" + info.mSimInfoIdx + " iccid:" + info.mIccId + " simId:" + info.mSimId
                + " displayName:" + info.mDisplayName + " color:" + info.mColor);

        return info;
    }

    /**
     * Get the SimInfoRecord according to an index
     * @param context Context provided by caller
     * @param simInfoIdx The unique SimInfoRecord index in database
     * @return SimInfoRecord, maybe null
     */
    public static SimInfoRecord getSimInfoBySimInfoIdx(Context context, long simInfoIdx) {
        logd("[getSimInfoBySimInfoIdx]+ simInfoIdx:" + simInfoIdx);
        if (simInfoIdx <= 0) {
            logd("[getSimInfoBySimInfoIdx]- simInfoIdx <= 0");
            return null;
        }
        Cursor cursor = context.getContentResolver().query(CONTENT_URI, 
                null, _ID + "=?", new String[] {Long.toString(simInfoIdx)}, null);
        try {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    logd("[getSimInfoBySimInfoIdx]- Info detail:");
                    return getSimInfoRecord(cursor);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logd("[getSimInfoBySimInfoIdx]- null info return");

        return null;
    }

    /**
     * Get the SimInfoRecord according to an IccId
     * @param context Context provided by caller
     * @param iccId the IccId of SIM card
     * @return SimInfoRecord, maybe null
     */
    public static SimInfoRecord getSimInfoByIccId(Context context, String iccId) {
        logd("[getSimInfoByIccId]+ iccId:" + iccId);
        if (iccId == null) {
            logd("[getSimInfoByIccId]- null iccid");
            return null;
        }
        Cursor cursor = context.getContentResolver().query(CONTENT_URI, 
                null, ICC_ID + "=?", new String[] {iccId}, null);
        try {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    logd("[getSimInfoByIccId]- Info detail:");
                    return getSimInfoRecord(cursor);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logd("[getSimInfoByIccId]- null info return");

        return null;
    }

    /**
     * Get the SimInfoRecord according to simId
     * @param context Context provided by caller
     * @param simId the slot which the SIM is inserted
     * @return SimInfoRecord, maybe null
     */
    public static SimInfoRecord getSimInfoBySimId(Context context, int simId) {
        logd("[getSimInfoBySimId]+ simId:" + simId);
        if (simId < 0) {
            logd("[getSimInfoBySimId]- simId < 0");
            return null;
        }
        Cursor cursor = context.getContentResolver().query(CONTENT_URI, 
                null, SIM_ID + "=?", new String[] {String.valueOf(simId)}, null);
        try {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    logd("[getSimInfoBySimId]- Info detail:");
                    return getSimInfoRecord(cursor);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logd("[getSimInfoBySimId]- null info return");

        return null;
    }

    /**
     * Query SimInfoRecord(s) from siminfo database
     * @param context Context provided by caller
     * @param selection A filter declaring which rows to return
     * @param queryKey query key content
     * @return Array list of queried result from database
     */
     public static List<SimInfoRecord> getSimInfo(Context context, String selection, Object queryKey) {
        logd("selection:" + selection + queryKey);
        String[] selectionArgs = null;
        if (queryKey != null) {
            selectionArgs = new String[] {queryKey.toString()};
        }
        ArrayList<SimInfoRecord> simList = null;
        Cursor cursor = context.getContentResolver().query(CONTENT_URI, 
                null, selection, selectionArgs, null);
        try {
            if (cursor != null) {
                simList = new ArrayList<SimInfoRecord>();
                while (cursor.moveToNext()) {
                    simList.add(getSimInfoRecord(cursor));
                }
            } else {
                logd("Query fail");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return simList;
    }

    /**
     * Get all the SimInfoRecord(s) in siminfo database
     * @param context Context provided by caller
     * @return Array list of all SimInfoRecords in database, include thsoe that were inserted before
     */
    public static List<SimInfoRecord> getAllSimInfoList(Context context) {
        logd("[getAllSimInfoList]+");
        List<SimInfoRecord> simList = null;
        simList = getSimInfo(context, null, null);
        if (simList != null) {
            logd("[getAllSimInfoList]- " + simList.size() + " infos return");
        } else {
            logd("[getAllSimInfoList]- no info return");
        }

        return simList;
    }

    /**
     * Get the SimInfoRecord(s) of the currently inserted SIM(s)
     * @param context Context provided by caller
     * @return Array list of currently inserted SimInfoRecord(s)
     */
    public static List<SimInfoRecord> getInsertedSimInfoList(Context context) {
        logd("[getInsertedSimInfoList]+");
        List<SimInfoRecord> simList = null;
        simList = getSimInfo(context, SIM_ID + "!=" + SIM_NOT_INSERTED, null);
        if (simList != null) {
            logd("[getInsertedSimInfoList]- " + simList.size() + " infos return");
        } else {
            logd("[getInsertedSimInfoList]- no info return");
        }

        return simList;
    }

    /**
     * Get the SIM count of all SIM(s) in siminfo database
     * @param context Context provided by caller
     * @return all SIM count in database, include what was inserted before
     */
    public static int getAllSimInfoCount(Context context) {
        logd("[getAllSimCount]+");
        Cursor cursor = context.getContentResolver().query(CONTENT_URI, 
                null, null, null, null);
        try {
            if (cursor != null) {
                int count = cursor.getCount();
                logd("[getAllSimCount]- " + count + " SIM(s) in DB");
                return count;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logd("[getAllSimCount]- no SIM in DB");

        return 0;
    }

    /**
     * Get the SIM count of currently inserted SIM(s)
     * @param context Context provided by caller
     * @return currently inserted SIM count
     */
    public static int getInsertedSimCount(Context context) {
        logd("[getInsertedSimCount]+");
        Cursor cursor = context.getContentResolver().query(CONTENT_URI, 
                null, SIM_ID+ "!=" + SIM_NOT_INSERTED, null, null);
        try {
            if (cursor != null) {
                int count = cursor.getCount();
                logd("[getInsertedSimCount]- " + count + " SIM(s) inserted");
                return count;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logd("[getInsertedSimCount]- no SIM in device");

        return 0;
    }

    /**
     * Add a new SimInfoRecord to siminfo database if needed
     * @param context Context provided by caller
     * @param iccId the IccId of the SIM card
     * @param simId the slot which the SIM is inserted
     * @return the URL of the newly created row or the updated row
     */
    public static Uri addSimInfoRecord(Context context, String iccId, int simId) {
        logd("[addSimInfoRecord]+ iccId:" + iccId + " simId:" + simId);
        if (iccId == null) {
            logd("[addSimInfoRecord]- null iccId");
        }
        Uri uri = null;
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(CONTENT_URI, new String[] {_ID, SIM_ID}, 
                ICC_ID + "=?", new String[] {iccId}, null);
        try {
            if (cursor == null || !cursor.moveToFirst()) {
                ContentValues value = new ContentValues();
                value.put(ICC_ID, iccId);
                // default SIM color differs between slots
                value.put(COLOR, simId);
                value.put(SIM_ID, simId);
                uri = resolver.insert(CONTENT_URI, value);
                logd("[addSimInfoRecord]- New record created");
            } else {
                long simInfoIdx = cursor.getLong(0);
                int oldSimInfoId = cursor.getInt(1);
                if (simId != oldSimInfoId) {
                    ContentValues value = new ContentValues(1);
                    value.put(SIM_ID, simId);
                    resolver.update(CONTENT_URI, value,
                                        _ID + "=" + Long.toString(simInfoIdx), null);
                } 
                logd("[addSimInfoRecord]- Record already exist");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return uri;
    }

    /**
     * Set SIM color by simInfo index
     * @param context Context provided by caller
     * @param color the color of the SIM
     * @param simInfoIdx the unique SimInfoRecord index in database
     * @return the number of records updated
     */
    public static int setColor(Context context, int color, long simInfoIdx) {
        logd("[setColor]+ color:" + color + " simInfoIdx:" + simInfoIdx);
        int size = sSimBackgroundDarkRes.length;
        if (simInfoIdx <= 0 || color < 0 || color >= size) {
            logd("[setColor]- fail");
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put(COLOR, color);
        logd("[setColor]- color:" + color + " set");

        int result = context.getContentResolver().update(CONTENT_URI, value,
                                                            _ID + "=" + Long.toString(simInfoIdx), null);
        broadcastSimInfoContentChanged(context, simInfoIdx, COLOR, color, DEFAULT_STRING_VALUE);

        return result;
    }

    /**
     * Set display name by simInfo index
     * @param context Context provided by caller
     * @param displayName the display name of SIM card
     * @param simInfoIdx the unique SimInfoRecord index in database
     * @return the number of records updated
     */
    public static int setDisplayName(Context context, String displayName, long simInfoIdx) {
        return setDisplayName(context, displayName, simInfoIdx, -1);
    }

    /**
     * Set display name by simInfo index with name source
     * @param context Context provided by caller
     * @param displayName the display name of SIM card
     * @param simInfoIdx the unique SimInfoRecord index in database
     * @param nameSource, 0: DEFAULT_SOURCE, 1: SIM_SOURCE, 2: USER_INPUT
     * @return the number of records updated
     */
    public static int setDisplayName(Context context, String displayName, long simInfoIdx, long nameSource) {
        logd("[setDisplayName]+  displayName:" + displayName + " simInfoIdx:" + simInfoIdx + " nameSource:" + nameSource);
        if (simInfoIdx <= 0) {
            logd("[setDisplayName]- fail");
            return -1;
        }
        String nameToSet;
        if (displayName == null) {
            nameToSet = context.getString(DEFAULT_NAME_RES);
        } else {
            nameToSet = displayName;
        }
        ContentValues value = new ContentValues(1);
        value.put(DISPLAY_NAME, nameToSet);
        if (nameSource >= DEFAULT_SOURCE) {
            logd("Set nameSource=" + nameSource);
            value.put(NAME_SOURCE, nameSource);
        }
        logd("[setDisplayName]- mDisplayName:" + nameToSet + " set");

        int result = context.getContentResolver().update(CONTENT_URI, value,
                                                            _ID + "=" + Long.toString(simInfoIdx), null);
        broadcastSimInfoContentChanged(context, simInfoIdx, DISPLAY_NAME, DEFAULT_INT_VALUE, nameToSet);

        return result;
    }

    /**
     * Set phone number by simInfoIdx
     * @param context Context provided by caller
     * @param number the phone number of the SIM
     * @param simInfoIdx the unique SimInfoRecord index in database
     * @return the number of records updated
     */
    public static int setDispalyNumber(Context context, String number, long simInfoIdx) {
        logd("[setDispalyNumber]+ number:" + number + " simInfoIdx:" + simInfoIdx);
        if (number == null || simInfoIdx <= 0) {
            logd("[setDispalyNumber]- fail");
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put(NUMBER, number);
        logd("[setDispalyNumber]- number:" + number + " set");

        int result = context.getContentResolver().update(CONTENT_URI, value,
                                                            _ID + "=" + Long.toString(simInfoIdx), null);
        broadcastSimInfoContentChanged(context, simInfoIdx, NUMBER, DEFAULT_INT_VALUE, number);

        return result;
    }

    /**
     * Set number display format. 0: none, 1: the first four digits, 2: the last four digits
     * @param context Context provided by caller
     * @param format the display format of phone number
     * @param simInfoIdx the unique SimInfoRecord index in database
     * @return the number of records updated
     */
    public static int setDispalyNumberFormat(Context context, int format, long simInfoIdx) {
        logd("[setDispalyNumberFormat]+ format:" + format + " simInfoIdx:" + simInfoIdx);
        if (format < 0 || simInfoIdx <= 0) {
            logd("[setDispalyNumberFormat]- fail, return -1");
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put(DISPLAY_NUMBER_FORMAT, format);
        logd("[setDispalyNumberFormat]- format:" + format + " set");

        int result = context.getContentResolver().update(CONTENT_URI, value,
                                                            _ID + "=" + Long.toString(simInfoIdx), null);
        broadcastSimInfoContentChanged(context, simInfoIdx, DISPLAY_NUMBER_FORMAT, format, DEFAULT_STRING_VALUE);

        return result;
    }

    /**
     * Set data roaming by simInfo index
     * @param context Context provided by caller
     * @param roaming 0:Don't allow data when roaming, 1:Allow data when roaming
     * @param simInfoIdx the unique SimInfoRecord index in database
     * @return the number of records updated
     */
    public static int setDataRoaming(Context context, int roaming, long simInfoIdx) {
        logd("[setDataRoaming]+ roaming:" + roaming + " simInfoIdx:" + simInfoIdx);
        if (roaming < 0 || simInfoIdx <= 0) {
            logd("[setDataRoaming]- fail");
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put(DATA_ROAMING, roaming);
        logd("[setDataRoaming]- roaming:" + roaming + " set");

        int result = context.getContentResolver().update(CONTENT_URI, value,
                                                            _ID + "=" + Long.toString(simInfoIdx), null);
        broadcastSimInfoContentChanged(context, simInfoIdx, DATA_ROAMING, roaming, DEFAULT_STRING_VALUE);

        return result;
    }

    private static int[] setSimResource(int type) {
        int[] simResource = null;

        switch (type) {
            case RES_TYPE_BACKGROUND_DARK:
                simResource = new int[] {
                    com.android.internal.R.drawable.sim_dark_blue,
                    com.android.internal.R.drawable.sim_dark_orange,
                    com.android.internal.R.drawable.sim_dark_green,
                    com.android.internal.R.drawable.sim_dark_purple
                };
                break;
            case RES_TYPE_BACKGROUND_LIGHT:
                simResource = new int[] {
                    com.android.internal.R.drawable.sim_light_blue,
                    com.android.internal.R.drawable.sim_light_orange,
                    com.android.internal.R.drawable.sim_light_green,
                    com.android.internal.R.drawable.sim_light_purple
                };
                break;
        }

        return simResource;
    }

    private static void logd(String msg) {
        Rlog.d(LOG_TAG, "[SimInfoMgr]" + msg);
    }
}

