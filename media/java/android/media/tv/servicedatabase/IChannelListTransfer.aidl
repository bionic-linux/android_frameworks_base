package android.media.tv.servicedatabase;


import android.os.ParcelFileDescriptor;


/**
 * @hide
 */
interface IChannelListTransfer {
    // Parse XML file and import Channels information.
    void importChannelList(in ParcelFileDescriptor pfd);
    // Get Channels information for export and create XML file.
    void exportChannelList(in ParcelFileDescriptor pfd);
}