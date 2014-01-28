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
public class SubscriptionController implements BaseColumns {
    private static final String LOG_TAG = "PHONE";

    public static final Uri CONTENT_URI =
            Uri.parse("content://telephony/siminfo");

    public static final int DEFAULT_INT_VALUE = -100;

    public static final String DEFAULT_STRING_VALUE = "N/A";

    public static final int EXTRA_VALUE_NEW_SIM = 1;
    public static final int EXTRA_VALUE_REMOVE_SIM = 2;
    public static final int EXTRA_VALUE_REPOSITION_SIM = 3;
    public static final int EXTRA_VALUE_NOCHANGE = 4;

    public static final String INTENT_KEY_DETECT_STATUS = "simDetectStatus";
    public static final String INTENT_KEY_SIM_COUNT = "simCount";
    public static final String INTENT_KEY_NEW_SIM_SLOT = "newSIMSlot";
    public static final String INTENT_KEY_NEW_SIM_STATUS = "newSIMStatus";

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

    private static Context sContext = null;    

    public SubscriptionController(Context context) {
        sContext = context;
    }

    /**
     * A SubInfoRecord instance represent one record in siminfo database
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
    public static class SubInfoRecord {
        public long mSubId;
        public String mIccId;
        public int mSimId;
        public String mDisplayName;
        public int mNameSource;
        public int mColor;
        public String mNumber;
        public int mDispalyNumberFormat;
        public int mDataRoaming;
        public int[] mSimIconRes;
        private SubInfoRecord() {
            mSubId = -1;
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
     * Broadcast when subinfo settings has chanded
     * @SubId The unique SubInfoRecord index in database
     * @param columnName The column that is updated
     * @param intContent The updated integer value
     * @param stringContent The updated string value
     */
     private static void broadcastSimInfoContentChanged(Context context, long subId,
            String columnName, int intContent, String stringContent) {
        Intent intent = new Intent(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);
        intent.putExtra(BaseColumns._ID, subId);
        intent.putExtra(TelephonyIntents.EXTRA_COLUMN_NAME, columnName);
        intent.putExtra(TelephonyIntents.EXTRA_INT_CONTENT, intContent);
        intent.putExtra(TelephonyIntents.EXTRA_STRING_CONTENT, stringContent);
        if (intContent != DEFAULT_INT_VALUE) {
            logd("SubInfoRecord" + subId + " changed, " + columnName + " -> " +  intContent);
        } else {
            logd("SubInfoRecord" + subId + " changed, " + columnName + " -> " +  stringContent);
        }
        context.sendBroadcast(intent);
    }

    /**
     * New SubInfoRecord instance and fill in detail info
     * @param cursor
     * @return the query result of desired SubInfoRecord
     */
    private static SubInfoRecord getSubInfoRecord(Cursor cursor) {
            SubInfoRecord info = new SubInfoRecord();
            info.mSubId = cursor.getLong(cursor.getColumnIndexOrThrow(_ID));
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
            logd("[getSubInfoRecord] SubId:" + info.mSubId + " iccid:" + info.mIccId + " simId:" + info.mSimId
                    + " displayName:" + info.mDisplayName + " color:" + info.mColor);

            return info;
        }

    /**
     * Get the SubInfoRecord according to an index
     * @param context Context provided by caller
     * @param subId The unique SubInfoRecord index in database
     * @return SubInfoRecord, maybe null
     */
    public static SubInfoRecord getSubInfoUsingSubId(Context context, long subId) {
        logd("[getSubInfoUsingSubIdx]+ subId:" + subId);
        if (subId <= 0) {
            logd("[getSubInfoUsingSubIdx]- subId <= 0");
            return null;
        }
        Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                null, _ID + "=?", new String[] {Long.toString(subId)}, null);
        try {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    logd("[getSubInfoUsingSubIdx]- Info detail:");
                    return getSubInfoRecord(cursor);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logd("[getSubInfoUsingSubIdx]- null info return");

        return null;
    }

    /**
     * Get the SubInfoRecord according to an IccId
     * @param context Context provided by caller
     * @param iccId the IccId of SIM card
     * @return SubInfoRecord, maybe null
     */
    public static List<SubInfoRecord> getSubInfoUsingIccId(Context context, String iccId) {
        logd("[getSubInfoUsingIccId]+ iccId:" + iccId);
        if (iccId == null) {
            logd("[getSubInfoUsingIccId]- null iccid");
            return null;
        }
        Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                null, ICC_ID + "=?", new String[] {iccId}, null);
        ArrayList<SubInfoRecord> subList = null;
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    SubInfoRecord subInfo = getSubInfoRecord(cursor);
                    if (subInfo != null)
                    {
                        if (subList == null)
                        {
                            subList = new ArrayList<SubInfoRecord>();
                        }
                        subList.add(subInfo);
                }
                }
            } else {
                logd("Query fail");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return subList;
    }

    /**
     * Get the SubInfoRecord according to simId
     * @param context Context provided by caller
     * @param simId the slot which the SIM is inserted
     * @return SubInfoRecord, maybe null
     */
    public static List<SubInfoRecord> getSubInfoUsingSimId(Context context, int simId) {
        logd("[getSubInfoUsingSimId]+ simId:" + simId);
        if (simId < 0) {
            logd("[getSubInfoUsingSimId]- simId < 0");
            return null;
        }
        Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                null, SIM_ID + "=?", new String[] {String.valueOf(simId)}, null);
        ArrayList<SubInfoRecord> subList = null;
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    SubInfoRecord subInfo = getSubInfoRecord(cursor);
                    if (subInfo != null)
                    {
                        if (subList == null)
                        {
                            subList = new ArrayList<SubInfoRecord>();
                        }
                        subList.add(subInfo);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logd("[getSubInfoUsingSimId]- null info return");

        return subList;
    }

    /**
     * Query SubInfoRecord(s) from subinfo database
     * @param context Context provided by caller
     * @param selection A filter declaring which rows to return
     * @param queryKey query key content
     * @return Array list of queried result from database
     */
     public static List<SubInfoRecord> getSubInfo(Context context, String selection, Object queryKey) {
        logd("selection:" + selection + " " + queryKey);
        String[] selectionArgs = null;
        if (queryKey != null) {
            selectionArgs = new String[] {queryKey.toString()};
        }
        ArrayList<SubInfoRecord> subList = null;
        Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                null, selection, selectionArgs, null);
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    SubInfoRecord subInfo = getSubInfoRecord(cursor);
                    if (subInfo != null)
                    {
                        if (subList == null)
                        {
                            subList = new ArrayList<SubInfoRecord>();
                        }
                        subList.add(subInfo);
                }
                }
            } else {
                logd("Query fail");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return subList;
    }

    /**
     * Get all the SubInfoRecord(s) in subinfo database
     * @param context Context provided by caller
     * @return Array list of all SubInfoRecords in database, include thsoe that were inserted before
     */
    public static List<SubInfoRecord> getAllSubInfoList(Context context) {
        logd("[getAllSubInfoList]+");
        List<SubInfoRecord> subList = null;
        subList = getSubInfo(context, null, null);
        if (subList != null) {
            logd("[getAllSubInfoList]- " + subList.size() + " infos return");
        } else {
            logd("[getAllSubInfoList]- no info return");
        }

        return subList;
    }

    /**
     * Get the SubInfoRecord(s) of the currently inserted SIM(s)
     * @param context Context provided by caller
     * @return Array list of currently inserted SubInfoRecord(s)
     */
    public static List<SubInfoRecord> getActivatedSubInfoList(Context context) {
        logd("[getActivatedSubInfoList]+");
        List<SubInfoRecord> subList = null;
        subList = getSubInfo(context, SIM_ID + "!=" + SIM_NOT_INSERTED, null);
        if (subList != null) {
            logd("[getActivatedSubInfoList]- " + subList.size() + " infos return");
        } else {
            logd("[getActivatedSubInfoList]- no info return");
        }

        return subList;
    }

    /**
     * Get the SUB count of all SUB(s) in subinfo database
     * @param context Context provided by caller
     * @return all SIM count in database, include what was inserted before
     */
    public static int getAllSubInfoCount(Context context) {
        logd("[getAllSubInfoCount]+");
        Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                null, null, null, null);
        try {
            if (cursor != null) {
                int count = cursor.getCount();
                logd("[getAllSubInfoCount]- " + count + " SUB(s) in DB");
                return count;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        logd("[getAllSubInfoCount]- no SUB in DB");

        return 0;
    }

    /**
     * Add a new SubInfoRecord to subinfo database if needed
     * @param context Context provided by caller
     * @param iccId the IccId of the SIM card
     * @param simId the slot which the SIM is inserted
     * @return the URL of the newly created row or the updated row
     */
    public static Uri addSubInfoRecord(Context context, String iccId, int simId) {
        if (sContext == null)
        {
            sContext = context;
        }
        logd("[addSubInfoRecord]+ iccId:" + iccId + " simId:" + simId);
        if (iccId == null) {
            logd("[addSubInfoRecord]- null iccId");
        }
        Uri uri = null;
        String nameToSet;
        nameToSet = "SUB 0"+Integer.toString(simId+1);
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(CONTENT_URI, new String[] {_ID, SIM_ID, NAME_SOURCE},
                ICC_ID + "=?", new String[] {iccId}, null);
        try {
            if (cursor == null || !cursor.moveToFirst()) {
                ContentValues value = new ContentValues();
                value.put(ICC_ID, iccId);
                // default SIM color differs between slots
                value.put(COLOR, simId);
                value.put(SIM_ID, simId);
                value.put(DISPLAY_NAME, nameToSet);
                uri = resolver.insert(CONTENT_URI, value);
                logd("[addSubInfoRecord]- New record created");
            } else {
                long subId = cursor.getLong(0);
                int oldSimInfoId = cursor.getInt(1);
                int nameSource = cursor.getInt(2);
                ContentValues value = new ContentValues();

                if (simId != oldSimInfoId) {
                    value.put(SIM_ID, simId);
                }

                if (nameSource != USER_INPUT) {
                    value.put(DISPLAY_NAME, nameToSet);
                }

                resolver.update(CONTENT_URI, value,
                                    _ID + "=" + Long.toString(subId), null);

                logd("[addSubInfoRecord]- Record already exist");
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
     * @param subId the unique SubInfoRecord index in database
     * @return the number of records updated
     */
    public static int setColor(Context context, int color, long subId) {
        logd("[setColor]+ color:" + color + " subId:" + subId);
        int size = sSimBackgroundDarkRes.length;
        if (subId <= 0 || color < 0 || color >= size) {
            logd("[setColor]- fail");
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put(COLOR, color);
        logd("[setColor]- color:" + color + " set");

        int result = context.getContentResolver().update(CONTENT_URI, value,
                                                            _ID + "=" + Long.toString(subId), null);
        broadcastSimInfoContentChanged(context, subId, COLOR, color, DEFAULT_STRING_VALUE);

        return result;
    }

    /**
     * Set display name by simInfo index
     * @param context Context provided by caller
     * @param displayName the display name of SIM card
     * @param subId the unique SubInfoRecord index in database
     * @return the number of records updated
     */
    public static int setDisplayName(Context context, String displayName, long subId) {
        return setDisplayName(context, displayName, subId, -1);
    }

    /**
     * Set display name by simInfo index with name source
     * @param context Context provided by caller
     * @param displayName the display name of SIM card
     * @param subId the unique SubInfoRecord index in database
     * @param nameSource, 0: DEFAULT_SOURCE, 1: SIM_SOURCE, 2: USER_INPUT
     * @return the number of records updated
     */
    public static int setDisplayName(Context context, String displayName, long subId, long nameSource) {
        logd("[setDisplayName]+  displayName:" + displayName + " subId:" + subId + " nameSource:" + nameSource);
        if (subId <= 0) {
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
                                                            _ID + "=" + Long.toString(subId), null);
        broadcastSimInfoContentChanged(context, subId, DISPLAY_NAME, DEFAULT_INT_VALUE, nameToSet);

        return result;
    }

    /**
     * Set phone number by subId
     * @param context Context provided by caller
     * @param number the phone number of the SIM
     * @param subId the unique SubInfoRecord index in database
     * @return the number of records updated
     */
    public static int setDispalyNumber(Context context, String number, long subId) {
        logd("[setDispalyNumber]+ number:" + number + " subId:" + subId);
        if (number == null || subId <= 0) {
            logd("[setDispalyNumber]- fail");
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put(NUMBER, number);
        logd("[setDispalyNumber]- number:" + number + " set");

        int result = context.getContentResolver().update(CONTENT_URI, value,
                                                            _ID + "=" + Long.toString(subId), null);
        broadcastSimInfoContentChanged(context, subId, NUMBER, DEFAULT_INT_VALUE, number);

        return result;
    }

    /**
     * Set number display format. 0: none, 1: the first four digits, 2: the last four digits
     * @param context Context provided by caller
     * @param format the display format of phone number
     * @param subId the unique SubInfoRecord index in database
     * @return the number of records updated
     */
    public static int setDispalyNumberFormat(Context context, int format, long subId) {
        logd("[setDispalyNumberFormat]+ format:" + format + " subId:" + subId);
        if (format < 0 || subId <= 0) {
            logd("[setDispalyNumberFormat]- fail, return -1");
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put(DISPLAY_NUMBER_FORMAT, format);
        logd("[setDispalyNumberFormat]- format:" + format + " set");

        int result = context.getContentResolver().update(CONTENT_URI, value,
                                                            _ID + "=" + Long.toString(subId), null);
        broadcastSimInfoContentChanged(context, subId, DISPLAY_NUMBER_FORMAT, format, DEFAULT_STRING_VALUE);

        return result;
    }

    /**
     * Set data roaming by simInfo index
     * @param context Context provided by caller
     * @param roaming 0:Don't allow data when roaming, 1:Allow data when roaming
     * @param subId the unique SubInfoRecord index in database
     * @return the number of records updated
     */
    public static int setDataRoaming(Context context, int roaming, long subId) {
        logd("[setDataRoaming]+ roaming:" + roaming + " subId:" + subId);
        if (roaming < 0 || subId <= 0) {
            logd("[setDataRoaming]- fail");
            return -1;
        }
        ContentValues value = new ContentValues(1);
        value.put(DATA_ROAMING, roaming);
        logd("[setDataRoaming]- roaming:" + roaming + " set");

        int result = context.getContentResolver().update(CONTENT_URI, value,
                                                            _ID + "=" + Long.toString(subId), null);
        broadcastSimInfoContentChanged(context, subId, DATA_ROAMING, roaming, DEFAULT_STRING_VALUE);

        return result;
    }

    public static int getSimId(long subId) {
        logd("[getSimId]+ subId:" + subId);
        if (subId <= 0) {
            logd("[getSimId]- subId <= 0");
            return SIM_NOT_INSERTED;
        }

        if (sContext == null) {
            logd("[getSimId]- context null");
            return SIM_NOT_INSERTED;
        }
        
        //return (int)subId;
        if (getSubInfoUsingSubId(sContext, subId) != null)
            return getSubInfoUsingSubId(sContext, subId).mSimId;
        else
            return SIM_NOT_INSERTED;

    }

    public static long[] getSubId(int simId) {
        logd("[getSubId]+ simId:" + simId);
        
        if (simId < 0) {
            logd("[getSubId]- simId < 0");
            return null;
        }

        if (sContext == null) {
            logd("[getSubId]- context null");
            return null;
        }
        
        
        long[] subId = new long[] {(long)simId,(long)simId,(long)simId,(long)simId};

        List<SubInfoRecord> SubInfo = SubscriptionController.getSubInfoUsingSimId(sContext, simId);
        if (SubInfo != null)
        {
            for (int i=0; i < SubInfo.size(); i++)
            {
                subId[i]=SubInfo.get(i).mSubId;
            }
        }

        logd("[getSubId]-, subId = "+subId[0]);
        return subId;
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
        Rlog.d(LOG_TAG, "[SubscriptionController]" + msg);
    }

    public static long getDefaultSubId()
    {
        logd("[getDefaultSubId] return 1");
        return 1;
        
    }

}

