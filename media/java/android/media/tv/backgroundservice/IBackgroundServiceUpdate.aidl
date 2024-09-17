package android.media.tv.backgroundservice;


import android.media.tv.backgroundservice.IBackgroundServiceUpdateListener;


/**
 * @hide
 */
interface IBackgroundServiceUpdate {
    // Set listener for background service update, receives notifications for svl/tsl/nwl update during background service update.
    void addBackgroundServiceUpdateListener(in String clientToken, in IBackgroundServiceUpdateListener listener);
    // Remove listener for background service update to stop receiving notifications for svl/tsl/nwl update during background service update.
    void removeBackgroundServiceUpdateListener(in IBackgroundServiceUpdateListener listener);
}