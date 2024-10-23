package android.media.tv.tuner;


import android.os.Bundle;


/**
* @hide
*/
interface IMuxTuneSession {
    // Start mux tune with tune params.
    void start(int broadcastType, int frequency, int brandwith, in Bundle muxTuneParams);
    // Stop mux tune.
    void stop();
    // Release muxtune resources.
    void release();
    // Get the session token created by TIS to identify different sessions.
    String getSessionToken();
}