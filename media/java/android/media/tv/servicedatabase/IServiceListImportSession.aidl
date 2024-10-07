package android.media.tv.servicedatabase;


import android.os.Bundle;
import android.os.ParcelFileDescriptor;


/**
 * @hide
 */
interface IServiceListImportSession {
    // Start import service list. Should call after preload and before release.
    int importServiceList(in ParcelFileDescriptor pfd, in Bundle importParams);
    // Preparing for import.
    int preload(in ParcelFileDescriptor pfd);
    // Release import resources.
    int release();
}