package android.media.tv.servicedatabase;


/**
 * @hide
 */
oneway interface IServiceDatabaseListEditListener {
    void onCompleted(int requestId, in int result);
}