package android.media.tv.teletext;


import android.view.SurfaceControlViewHost.SurfacePackage;


/**
 * @hide
 */
oneway interface ITeletextViewSessionListener {
    void onSetSurface(in SurfacePackage surfacePackage);
}