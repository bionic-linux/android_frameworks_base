package android.media.tv.audio;


import android.os.Bundle;


/**
 * @hide
 */
interface IAnalogAudioInfo {
    Bundle getAnalogAudioInfo(in String sessionToken);
}