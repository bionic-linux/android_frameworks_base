package android.media.tv.servicedatabase;


import android.media.tv.servicedatabase.IServiceListExportListener;
import android.media.tv.servicedatabase.IServiceListImportListener;
import android.media.tv.servicedatabase.IServiceListSetChannelListListener;
import android.os.IBinder;


/**
 * @hide
 */
interface IServiceListTransferInterface {
    IBinder createExportSession(in IServiceListExportListener listener);
    IBinder createImportSession(in IServiceListImportListener listener);
    IBinder createSetChannelListSession(in IServiceListSetChannelListListener listener);
}