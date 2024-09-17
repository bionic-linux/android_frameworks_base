package android.media.tv.teletext;


import android.media.tv.teletext.ITeletextViewSessionListener;
import android.os.IBinder;


/**
 * @hide
 */
interface ITeletextViewSession {
    void setSurfaceInfo(in String displayId, in String sessionToken, in IBinder hostToken,
        in double width, in double high, in ITeletextViewSessionListener teletextViewSessionListener);
}