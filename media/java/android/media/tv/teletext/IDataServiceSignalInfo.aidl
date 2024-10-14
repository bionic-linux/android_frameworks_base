package android.media.tv.teletext;


import android.media.tv.teletext.IDataServiceSignalInfoListener;
import android.os.Bundle;


/**
 * @hide
 */
interface IDataServiceSignalInfo {
     // Get Teletext data service signal information.
     Bundle getDataServiceSignalInfo(String sessionToken);
     // Add data service signal info listener that receives notifications of teletext running information.
     void addDataServiceSignalInfoListener(String clientToken, IDataServiceSignalInfoListener listener);
     // Remove DataServiceSignalInfo listener that receives notifications of Teletext running information.
     void removeDataServiceSignalInfoListener(String clientToken, IDataServiceSignalInfoListener listener);
}