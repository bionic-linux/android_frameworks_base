package android.media.tv.recordcontents;


import android.media.tv.recordcontents.IDeleteRecordedContentsCallback;
import android.media.tv.recordcontents.IGetInfoRecordedContentsCallback;


/**
 * @hide
 */
interface IRecordedContents {
    // Delete recorded contents by URIs, using callback to notify the result or any errors during the deletion process.
    void deleteRecordedContents(in String[] contentUri, in IDeleteRecordedContentsCallback callback);
    // Get the channel lock status for recorded content identified by the URI provided in sync way.
    int getRecordedContentsLockInfoSync(in String contentUri);
    // Get the channel lock status for recorded content identified by the URI provided in async way.
    void getRecordedContentsLockInfoAsync(in String contentUri, in IGetInfoRecordedContentsCallback callback);
}