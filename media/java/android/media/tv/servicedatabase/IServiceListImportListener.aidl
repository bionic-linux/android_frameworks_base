package android.media.tv.servicedatabase;


/**
 * @hide
 */
interface IServiceListImportListener {
    void onImported(in int importResult);
    void onPreloaded(in int preloadResult);
}