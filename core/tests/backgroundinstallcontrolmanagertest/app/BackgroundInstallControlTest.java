package android.app;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static junit.framework.Assert.assertNotNull;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;

import android.annotation.NonNull;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.platform.test.annotations.Presubmit;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.UserHandle;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;


/**
 * Unit tests for {@link android.app.BackgroundInstallControlManager}.
 * Since there is no device instrumentation for a standard test, refer to
 * {@link com.android.server.pm.test.BackgroundInstallControlHostTest} for client functional test.
 *
 * This is permission access test only.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
@Presubmit
public final class BackgroundInstallControlTest {

//    private static final String TEST_VALUE = "test_value";
    protected Context mContext;
    private BackgroundInstallControlManager mBicManager;

//    private HashSet<String> sharedResource;
    @Before
    public void setup() {
        //sharedResource = new HashSet<>();
        mContext = getInstrumentation().getContext();
        mBicManager = mContext.getSystemService(BackgroundInstallControlManager.class);
        assertNotNull(mBicManager);
    }

    @Test
    public void successfullyGetBackgroundInstalledPacakgesWithoutError() {
        var appList = mBicManager.getBackgroundInstalledPackages(PackageManager.MATCH_ALL,
                UserHandle.USER_ALL);

        assertNotNull(appList);
    }
//    @Test
//    public void successfullyRegistersCallbackWithoutError() throws Exception {
//        mBicManager.registerBackgroundInstallControlCallback(mContext.getMainExecutor(), new TestCallback(sharedResource));
//        mBicManager.getBackgroundInstalledPackages(PackageManager.MATCH_ALL | PackageManager.GET_SIGNING_CERTIFICATES,
//                UserHandle.USER_SYSTEM);
//
//        Thread.sleep(1000);
//        assertTrue(sharedResource.contains(TEST_VALUE));
//    }
//
//    @Test
//    public void successfullyUnregistersCallbackWithoutError() throws Exception {
//        TestCallback testCallback = new TestCallback(sharedResource);
//        mBicManager.registerBackgroundInstallControlCallback(mContext.getMainExecutor(), testCallback);
//
//        mBicManager.unregisterBackgroundInstallControlCallback(testCallback);
//        mBicManager.getBackgroundInstalledPackages(PackageManager.MATCH_ALL | PackageManager.GET_SIGNING_CERTIFICATES,
//                UserHandle.USER_SYSTEM);
//
//        Thread.sleep(1000);
//        assertFalse(sharedResource.contains(TEST_VALUE));
//    }

//    public class TestCallback implements BackgroundInstallControlManager.Callback {
//        private static final String TAG = "BackgroundInstallControlTest.TestCallback";
//        private final HashSet<String> testSet;
//        public TestCallback(HashSet<String> sharedResource) {
//            testSet = sharedResource;
//        }
//        @Override
//        public void onMbaDetected(@NonNull Bundle extras) {
//            Log.d(TAG, extras.getString(FLAGGED_PACKAGE_NAME_KEY));
//            Log.d(TAG, Arrays.toString(testSet.toArray()));
//            testSet.add(TEST_VALUE);
//        }
//    }
}
