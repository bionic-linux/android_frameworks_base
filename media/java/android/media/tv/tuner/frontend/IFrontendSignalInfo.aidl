package android.media.tv.tuner.frontend;


import android.os.Bundle;
import android.media.tv.tuner.frontend.IFrontendSignalInfoListener;


/**
* @hide
*/
interface IFrontendSignalInfo {
    Bundle getFrontendSignalInfo(in String sessionToken);
    void setFrontendSignalInfoListener(in IFrontendSignalInfoListener listener);
}