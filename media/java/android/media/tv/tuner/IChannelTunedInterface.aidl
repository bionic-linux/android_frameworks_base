package android.media.tv.tuner;


import android.media.tv.tuner.IChannelTunedListener;


/*
* @hide
*/
interface IChannelTunedInterface {
    void addChannelTunedListener(in IChannelTunedListener listener);
    void removeChannelTunedListener(in IChannelTunedListener listener);
}