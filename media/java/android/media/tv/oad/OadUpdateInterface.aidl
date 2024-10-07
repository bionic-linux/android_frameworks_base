package android.media.tv.oad;


interface OadUpdateInterface {
    // Enable or disable the OAD function.
    void setOadStatus(in boolean enable);
    // Get status of OAD function.
    boolean getOadStatus();
    // Start OAD scan of all frequency in the program list.
    void startScan();
    // Stop OAD scan of all frequency in the program list.
    void stopScan();
    // Start OAD detect for the current channel.
    void startDetect();
    // Stop OAD detect for the current channel.
    void stopDetect();
    // Start OAD download after it has been detected or scanned.
    void startDownload();
    // Stop OAD download.
    void stopDownload();
    // Retrives current OAD software version.
    int getSoftwareVersion();
}