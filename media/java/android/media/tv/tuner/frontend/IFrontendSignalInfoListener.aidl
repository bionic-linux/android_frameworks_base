package android.media.tv.tuner.frontend;


/**
* @hide
*/
oneway interface IFrontendSignalInfoListener {
    void onFrontendStatusChanged(int frontendStatus);
}