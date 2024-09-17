package android.media.tv.servicedatabase;


import android.os.Bundle;
import android.os.ParcelFileDescriptor;


/**
 * @hide
 */
interface IServiceListExportSession {
    // Start export service list with reserved parameters.
    int exportServiceList(in ParcelFileDescriptor pfd, in Bundle exportParams);
    // Release export resources.
    int release();
}