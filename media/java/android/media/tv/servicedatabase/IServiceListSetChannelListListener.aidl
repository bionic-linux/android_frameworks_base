package android.media.tv.servicedatabase;


/**
 * @hide
 */
oneway interface IServiceListSetChannelListListener {
    void onCompleted(in int setChannelListResult);
}