package android.service.attention;

/**
 * Callback for registerProximityUpdates request.
 *
 * @hide
 */
oneway interface IProximityCallback {
    void onSuccess(int result, long timestamp);
    void onFailure(int error);
}
