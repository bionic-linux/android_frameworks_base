package android.platform.test.ravenwood;

import android.annotation.Nullable;

import java.util.function.BiConsumer;

/**
 * Contains Ravenwood private APIs.
 */
public class RavenwoodConfig$Private {
    private RavenwoodConfig$Private() {
    }

    static volatile BiConsumer<String, Throwable> sCriticalErrorHandler = null;

    /**
     * Set a listener for onCriticalError(), for testing. If a listener is set, we won't call
     * System.exit().
     */
    public static void setCriticalErrorHandler(@Nullable BiConsumer<String, Throwable> handler) {
        sCriticalErrorHandler = handler;
    }
}
