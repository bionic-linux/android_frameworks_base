package android.media.tv.servicedatabase;


import android.media.tv.servicedatabase.IServiceDatabaseListEditListener;
import android.os.Bundle;


/**
 * @hide
 */
interface IServiceDatabaseListEdit {
    // Open in edit mode. Must call close() after edit is done.
    int open(IServiceDatabaseListEditListener listener);
    // Method to close in edit mode.
    int close();
    // Method to commit changes made to service database.
    int commit();
    // Method to commit and close the changes.
    int userEditCommit();

    // Get a service/transportStream/Network/Satellite record information specified by serviceInfoId and keys from tv db.
    Bundle getServiceInfoFromDatabase(String serviceInfoId, in String[] keys);
    // Get a list of all service records' information specified by serviceListId and keys from tv db.
    Bundle getServiceInfoListFromDatabase(String serviceListId, in String[] keys);
    // Get a list of all service info IDs in the service list of serviceListId from tv db.
    String[] getServiceInfoIdsFromDatabase(String inServiceListId);
    // Update a service information by the contents of serviceInfo;
    int updateServiceInfoFromDatabase(in Bundle updateServiceInfo);
    // Update all service information by the contents of serviceInfoList.
    int updateServiceInfoByListFromDatabase(in Bundle[] updateServiceInfoList);
    // Remove a service information of the serviceInfoId from the service list.
    int removeServiceInfoFromDatabase(String serviceInfoId);
    // Remove all service information of the serviceInfoId from the service list.
    int removeServiceInfoByListFromDatabase(in String[] serviceInfoIdList);
    // Get a list of the Service list IDs which is equivalent to COLUMN_CHANNEL_LIST_ID in Channels table from tv db.
    String[] getServiceListChannelIds();
    // Get the information associated with the Service list Channel id.
    Bundle getServiceListInfoByChannelId(String serviceListChannelId, in String[] keys);

    // Get a list of transportStream records' information specified by serviceListId and keys.
    Bundle getTransportStreamInfoList(String serviceListId, in String[] keys);
    // Get a list of transportStream records' information specified by serviceListId and keys from work db.
    Bundle getTransportStreamInfoListForce(String serviceListId, in String[] keys);

    // Get a list of network records' information specified by serviceListId and keys.
    Bundle getNetworkInfoList(String serviceListId, in String[] keys);
    // Get a list of satellite records' information specified by serviceListId and keys.
    Bundle getSatelliteInfoList(String serviceListId, in String[] keys);

    // Decompress whole bundle value of single service/transportStream/Network/Satellite record.
    // RecordInfoBundle:a single record got from database by getServiceInfoFromDatabase()
    String toRecordInfoByType(in Bundle recordInfoBundle, in String recordType);
    // Set channels(tv.db) modified result to middleware database(SVL/TSL/NWL/SATL).
    int putRecordIdList(String serviceListId, in Bundle recordIdListBundle, int optType);

    // Add predefined ServiceListInfo of Hotbird 13E in scan two satellite scene EU region, following by commit().
    String addPredefinedServiceListInfo(int broadcastType, String serviceListType,
        String serviceListPrefix, String contryCode, int operatorId);
    // Add predefined channels of Hotbird 13E in scan two satellite scene EU region.
    int addPredefinedChannelList(String serviceListId, in Bundle[] predefinedListBundle);
    // Add predefined satellite info of Hotbird 13E in scan two satellite scene EU region.
    int addPredefinedSatInfo(String serviceListId, in Bundle predefinedSatInfoBundle);

}