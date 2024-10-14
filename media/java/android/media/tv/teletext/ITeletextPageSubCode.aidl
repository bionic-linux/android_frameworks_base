package android.media.tv.teletext;


import android.os.Bundle;


/**
 * @hide
 */
interface ITeletextPageSubCode {
    // Get Teletext page number
    Bundle getTeletextPageNumber(String sessionToken);
    // Set Teletext page number.
    void setTeleltextPageNumber(String sessionToken, in int pageNumber);
    // Get Teletext sub page number.
    Bundle getTeletextPageSubCode(String sessionToken);
    // Set Teletext sub page number.
    void setTeletextPageSubCode(String sessionToken, in int pageSubCode);
    // Get Teletext TopInfo.
    Bundle getTeletextHasTopInfo(String sessionToken);
    // Get Teletext TopBlockList.
    Bundle getTeletextTopBlockList(String sessionToken);
    // Get Teletext TopGroupList.
    Bundle getTeletextTopGroupList(String sessionToken, in int indexGroup);
    // Get Teletext TopPageList.
    Bundle getTeletextTopPageList(String sessionToken, in int indexPage);
}