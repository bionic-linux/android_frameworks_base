package android.media.tv.tuner;


import android.media.tv.tuner.IMuxTuneSession;


/**
* @hide
*/
interface IMuxTuneInterface {
    IMuxTuneSession createSession(in int broadcastType, in @nullable String clientToken);
}