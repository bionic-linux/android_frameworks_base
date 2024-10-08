package android.media.tv.recordcontents;


/**
 * @hide
 */
oneway interface IDeleteRecordedContentsCallback {
    void onRecordedContentsDeleted(in String[] contentUri, in int[] result);
}