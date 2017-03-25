package com.android.internal.policy;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * A class that manages SOS call.
 */
public class SOSCallManager {

    private final Context mContext;

    public SOSCallManager(Context context) {
        mContext = context;
    }

    public void performSOSCall() {
        sendCallIntent();
    }

    private void sendCallIntent() {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(getPhoneUri());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private Uri getPhoneUri() {
        return Uri.fromParts("tel", getNumber(), null);
    }

    public boolean isNumberSet() {
        return !TextUtils.isEmpty(getNumber());
    }

    private String getNumber() {
        return Settings.Global.getString(
                mContext.getContentResolver(), Settings.Global.SOS_CALL_NUMBER);
    }
}
