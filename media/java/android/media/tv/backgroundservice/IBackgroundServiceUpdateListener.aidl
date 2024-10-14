package android.media.tv.backgroundservice;


import android.os.Bundle;


/**
 * @hide
 */
interface IBackgroundServiceUpdateListener {
    // On background service update add/delete/update svl records.
    void onChannelListUpdate(String sessionToken, out Bundle[] updateInfos);
    // On background service update add/delete/update nwl records.
    void onNetworkListUpdate(String sessionToken, out Bundle[] updateInfos);
    // On background service update add/delete/update tsl records.
    void onTransportStreamingListUpdate(String sessionToken, out Bundle[] updateInfos);
}