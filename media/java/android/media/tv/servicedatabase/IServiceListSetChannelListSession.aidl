package android.media.tv.servicedatabase;


import android.os.Bundle;


/**
 * @hide
 */
interface IServiceListSetChannelListSession {
    // Set channelList with channelinfo bundles, serviceListInfo, and operation type.
    int setChannelList(in Bundle[] channelsInfo, in Bundle ServiceListInfoBundle, int optType);
    // Release set channellist resources.
    int release();
}