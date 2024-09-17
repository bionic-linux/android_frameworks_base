package android.media.tv.broadcastclock;


import android.os.Bundle;


/**
 * @hide
 */
interface IBroadcastClockService {
    long getUtcTime();
    long getLocalTime();
    Bundle getTimeZoneInfo();
    long getUtcTimePerStream(String SessionToken);
    long getLocalTimePerStream(String SessionToken);
}