/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wm;

import static android.app.WindowConfiguration.ACTIVITY_TYPE_STANDARD;
import static android.app.WindowConfiguration.WINDOWING_MODE_FULLSCREEN;
import static android.view.DisplayAdjustments.DEFAULT_DISPLAY_ADJUSTMENTS;
import static android.view.WindowManager.LayoutParams.TYPE_BASE_APPLICATION;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mock;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.spy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.anyInt;

import android.content.ClipData;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManagerGlobal;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.platform.test.annotations.Presubmit;
import android.view.Display;
import android.view.InputChannel;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.SurfaceControl.Transaction;
import android.view.View;

import androidx.test.filters.FlakyTest;
import androidx.test.filters.SmallTest;

import com.android.server.LocalServices;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;

/**
 * Tests for the {@link DragDropController} class.
 *
 * Build/Install/Run:
 *  atest FrameworksServicesTests:DragDropControllerTests
 */
@SmallTest
@Presubmit
public class DragDropControllerTests extends WindowTestsBase {
    private static final int TIMEOUT_MS = 3000;
    private TestDragDropController mTarget;
    private WindowState mWindow;
    private IBinder mToken;
    private TestableDisplayContent mTestableDisplayContent;

    static class TestDragDropController extends DragDropController {
        private Runnable mCloseCallback;

        TestDragDropController(WindowManagerService service, Looper looper) {
            super(service, looper);
        }

        void setOnClosedCallbackLocked(Runnable runnable) {
            assertTrue(dragDropActiveLocked());
            mCloseCallback = runnable;
        }

        @Override
        void onDragStateClosedLocked(DragState dragState) {
            super.onDragStateClosedLocked(dragState);
            if (mCloseCallback != null) {
                mCloseCallback.run();
                mCloseCallback = null;
            }
        }
    }

    /**
     * Creates a TestableSurfaceControlBuilder which can be used as a SurfaceControl.Builder Mock.
     */
    private class TestableSurfaceControlBuilder extends MockSurfaceControlBuilder {
        private SurfaceControl mParent;

        @Override
        public SurfaceControl.Builder setParent(SurfaceControl sc) {
            super.setParent(sc);
            mParent = sc;
            return this;
        }

        @Override
        public SurfaceControl build() {
            final SurfaceControl surfaceControl = mock(SurfaceControl.class);
            if (mTestableDisplayContent != null) {
                mTestableDisplayContent.addParentFor(surfaceControl, mParent);
            }
            return surfaceControl;
        }
    }

    /**
     * Creates a TestableStubTransaction which can be used as a Transaction Mock.
     */
    private class TestableStubTransaction extends StubTransaction {

        @Override
        public SurfaceControl.Transaction reparent(SurfaceControl sc, SurfaceControl newParent) {
            if (mTestableDisplayContent != null) {
                mTestableDisplayContent.addParentFor(sc, newParent);
            }
            return super.reparent(sc, newParent);
        }
    }

    /**
     * Creates a TestableDisplayContent which can be used as a DisplayContent Mock.
     */
    private class TestableDisplayContent extends DisplayContent {

        private final HashMap<SurfaceControl, SurfaceControl> mParentFor = new HashMap<>();

        TestableDisplayContent(Display display, WindowManagerService service,
                               ActivityDisplay activityDisplay) {
            super(display, service, activityDisplay);
        }

        @Override
        SurfaceControl.Builder makeOverlay() {
            final SurfaceControl overlay = getPrivateFieldInDisplayContent("mOverlayLayer");
            final TestableSurfaceControlBuilder builder = new TestableSurfaceControlBuilder();
            builder.setParent(overlay);
            return builder;
        }

        @Override
        void reparentToOverlay(Transaction transaction, SurfaceControl surface) {
            final SurfaceControl overlay = getPrivateFieldInDisplayContent("mOverlayLayer");
            final TestableStubTransaction testTransaction = new TestableStubTransaction();
            testTransaction.reparent(surface, overlay);
        }

        public void addParentFor(SurfaceControl child, SurfaceControl parent) {
            mParentFor.remove(child);
            mParentFor.put(child, parent);
        }

        public SurfaceControl getParentFor(SurfaceControl child) {
            return mParentFor.get(child);
        }
    }

    /**
     * Creates a window state which can be used as a drop target.
     */
    private WindowState createDropTargetWindow(String name, int ownerId) {
        final WindowTestUtils.TestAppWindowToken token = WindowTestUtils.createTestAppWindowToken(
                mDisplayContent);
        final TaskStack stack = createTaskStackOnDisplay(
                WINDOWING_MODE_FULLSCREEN, ACTIVITY_TYPE_STANDARD, mDisplayContent);
        final Task task = createTaskInStack(stack, ownerId);
        task.addChild(token, 0);

        final WindowState window = createWindow(
                null, TYPE_BASE_APPLICATION, token, name, ownerId, false);
        window.mInputChannel = new InputChannel();
        window.mHasSurface = true;
        return window;
    }

    @BeforeClass
    public static void setUpOnce() {
        final UserManagerInternal userManager = mock(UserManagerInternal.class);
        LocalServices.addService(UserManagerInternal.class, userManager);
    }

    @AfterClass
    public static void tearDownOnce() {
        LocalServices.removeServiceForTest(UserManagerInternal.class);
    }

    @Before
    public void setUp() throws Exception {
        mTarget = new TestDragDropController(mWm, mWm.mH.getLooper());
        mDisplayContent = spy(mDisplayContent);
        mWindow = createDropTargetWindow("Drag test window", 0);
        doReturn(mWindow).when(mDisplayContent).getTouchableWinAtPointLocked(0, 0);

        synchronized (mWm.mGlobalLock) {
            mWm.mWindowMap.put(mWindow.mClient.asBinder(), mWindow);
        }
    }

    @After
    public void tearDown() throws Exception {
        final CountDownLatch latch;
        synchronized (mWm.mGlobalLock) {
            if (!mTarget.dragDropActiveLocked()) {
                return;
            }
            if (mToken != null) {
                mTarget.cancelDragAndDrop(mToken, false);
            }
            latch = new CountDownLatch(1);
            mTarget.setOnClosedCallbackLocked(latch::countDown);
        }
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @FlakyTest(bugId = 131005232)
    public void testDragFlow() {
        dragFlow(0, ClipData.newPlainText("label", "Test"), 0, 0);
    }

    @Test
    @FlakyTest(bugId = 131005232)
    public void testPerformDrag_NullDataWithGrantUri() {
        dragFlow(View.DRAG_FLAG_GLOBAL | View.DRAG_FLAG_GLOBAL_URI_READ, null, 0, 0);
    }

    @Test
    @FlakyTest(bugId = 131005232)
    public void testPerformDrag_NullDataToOtherUser() {
        final WindowState otherUsersWindow =
                createDropTargetWindow("Other user's window", 1 * UserHandle.PER_USER_RANGE);
        doReturn(otherUsersWindow).when(mDisplayContent).getTouchableWinAtPointLocked(10, 10);

        dragFlow(0, null, 10, 10);
    }

    @Test
    public void testPerformDrag_OverlayLayerTopOfDragStateInputSurface() {
        mTestableDisplayContent = createTestableDisplayContent();
        mTestableDisplayContent = spy(mTestableDisplayContent);
        final RootWindowContainer root = mock(RootWindowContainer.class);
        final RootWindowContainer saveRoot = mWm.mRoot;
        mWm.mRoot = root;
        doReturn(mTestableDisplayContent).when(mWm.mRoot).getDisplayContent(anyInt());
        dragFlow(0, ClipData.newPlainText("OverlayLayer", "Test"), 0, 0);
        mWm.mRoot = saveRoot;

        final DragState dragState = getPrivateFieldInDragDropController("mDragState");
        assertNotNull(dragState);
        SurfaceControl inputSurface = dragState.mInputSurface;
        final SurfaceControl overlay = getPrivateFieldInDisplayContent("mOverlayLayer");

        // Check makeOverlay method is called and inputSurface has the mOverlayLayer parent.
        // If not, verify reparentToOverlay method is called.
        if (mTestableDisplayContent.getParentFor(inputSurface) != overlay) {
            dragState.mDisplayContent = mTestableDisplayContent;
            dragState.mInputSurface = mock(SurfaceControl.class);
            invokePrivateMethodInDragState(dragState, "showInputSurface");
            inputSurface = dragState.mInputSurface;
        }
        assertEquals("DragState.mInputSurface does not have the parent mOverlayLayer.",
                overlay, mTestableDisplayContent.getParentFor(inputSurface));
    }

    private void dragFlow(int flag, ClipData data, float dropX, float dropY) {
        final SurfaceSession appSession = new SurfaceSession();
        try {
            final SurfaceControl surface = new SurfaceControl.Builder(appSession)
                    .setName("drag surface")
                    .setBufferSize(100, 100)
                    .setFormat(PixelFormat.TRANSLUCENT)
                    .build();

            mToken = mTarget.performDrag(
                    new SurfaceSession(), 0, 0, mWindow.mClient, flag, surface, 0, 0, 0, 0, 0,
                    data);
            assertNotNull(mToken);

            mTarget.handleMotionEvent(false, dropX, dropY);
            mToken = mWindow.mClient.asBinder();
        } finally {
            appSession.kill();
        }
    }

    private TestableDisplayContent createTestableDisplayContent() {
        final int displayId = getValueAndPostIncrementPrivateFieldInWindowTestsBase("sNextDisplayId");
        final Display display = new Display(DisplayManagerGlobal.getInstance(), displayId,
                mDisplayInfo, DEFAULT_DISPLAY_ADJUSTMENTS);

        synchronized (mWm.mGlobalLock) {
            TestableDisplayContent testableDisplayContent = new TestableDisplayContent(display, mWm, mock(ActivityDisplay.class));
            return testableDisplayContent;
        }
    }

    private SurfaceControl getPrivateFieldInDisplayContent(String fieldName) {
        try {
            Field fd = DisplayContent.class.getDeclaredField(fieldName);
            fd.setAccessible(true);
            return (SurfaceControl)fd.get(mTestableDisplayContent);
        } catch (Exception e) {
            return null;
        }
    }

    private int getValueAndPostIncrementPrivateFieldInWindowTestsBase(String fieldName) {
        try {
            Field fd = WindowTestsBase.class.getDeclaredField(fieldName);
            fd.setAccessible(true);
            int value = (int)fd.get(this);
            fd.set(this, value + 1);
            return value;
        } catch (Exception e) {
            return 0;
        }
    }

    private DragState getPrivateFieldInDragDropController(String fieldName) {
        try {
            Field fd = DragDropController.class.getDeclaredField(fieldName);
            fd.setAccessible(true);
            DragDropController dragDropController = (DragDropController)mTarget;
            return (DragState)fd.get(dragDropController);
        } catch (Exception e) {
            return null;
        }
    }

    private void invokePrivateMethodInDragState(DragState dragState, String methodName) {
        try {
            Method method = DragState.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(dragState);
        } catch (Exception e) {
        }
    }
}
