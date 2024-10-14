package android.media.tv.tuner;


import android.os.Bundle;


/*
* @hide
*/
oneway interface IChannelTunedListener {
    void onChannelTuned(in String sessionToken, in Bundle channelTunedInfo);
}