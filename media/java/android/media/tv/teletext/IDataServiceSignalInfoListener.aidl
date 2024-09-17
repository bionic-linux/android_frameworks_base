package android.media.tv.teletext;


import android.os.Bundle;


/**
 * @hide
 */
oneway interface IDataServiceSignalInfoListener {
    void onDataServiceSignalInfoChanged (in String sessionToken, in Bundle changedSignalInfo);
}