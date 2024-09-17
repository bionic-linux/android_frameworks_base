package android.media.tv.video;


/**
 * @hide
 */
interface IScreenModeSettings {
    // Set screen mode information using a JSON string.
    void setScreenModeSettings(in String sessionToken, in String setting);
    // Get the overscan index which TIS session is applied.
    int getOverScanIndex(in String sessionToken);
    // Get status that TIS session is support overscan or not.
    boolean getSupportApplyOverScan(in String sessionToken);

}