package android.media.tv;


/**
 * @hide
 */
interface IAnalogAttributeInterface {
    int getVersion();
    void setColorSystemCapability(in String[] list);
    String[] getColorSystemCapability();
}