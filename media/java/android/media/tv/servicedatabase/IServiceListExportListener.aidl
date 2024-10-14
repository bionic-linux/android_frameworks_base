package android.media.tv.servicedatabase;


/**
 * @hide
 */
oneway interface IServiceListExportListener {
    void onExported(in int exportResult);
}