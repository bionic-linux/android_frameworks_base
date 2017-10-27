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

package com.android.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.INetd;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import com.android.server.IpSecService.IResource;
import com.android.server.IpSecService.RefcountedResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit tests for {@link IpSecService}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class IpSecServiceRefcountedResourceTest {

    Context mMockContext;
    INetd mMockNetd;
    IpSecService.IpSecServiceConfiguration mMockIpSecSrvConfig;
    IpSecService mIpSecService;

    @Before
    public void setUp() throws Exception {
        mMockContext = mock(Context.class);
        mMockNetd = mock(INetd.class);
        mMockIpSecSrvConfig = mock(IpSecService.IpSecServiceConfiguration.class);
        mIpSecService = new IpSecService(mMockContext, mMockIpSecSrvConfig);

        // Injecting mock netd
        when(mMockIpSecSrvConfig.getNetdInstance()).thenReturn(mMockNetd);
    }

    private void assertResourceState(
            RefcountedResource<IResource> resource,
            int refCount,
            int cleanupAndReleaseCallCount,
            int releaseIfUnreferencedRecursivelyCallCount,
            int cleanupCallCount,
            int releaseResourcesAndQuotaCallCount)
            throws RemoteException {
        // Check refcount on RefcountedResource
        assertEquals(refCount, resource.getReferenceCount());

        // Check call count of RefcountedResource
        verify(resource, times(cleanupAndReleaseCallCount)).cleanupAndRelease();
        verify(resource, times(releaseIfUnreferencedRecursivelyCallCount))
                .releaseIfUnreferencedRecursively();

        // Check call count of IResource
        verify(resource.getResource(), times(cleanupCallCount)).cleanup();
        verify(resource.getResource(), times(releaseResourcesAndQuotaCallCount))
                .releaseResourcesAndQuota();
    }

    /** Adds mockito instrumentation */
    private RefcountedResource<IResource> getTestRefcountedResource(
            RefcountedResource... dependencies) {
        return getTestRefcountedResource(new Binder(), dependencies);
    }

    /** Adds mockito instrumentation with provided binder */
    private RefcountedResource<IResource> getTestRefcountedResource(
            IBinder binder, RefcountedResource... dependencies) {
        return spy(
                mIpSecService
                .new RefcountedResource<IResource>(mock(IResource.class), binder, dependencies));
    }

    @Test
    public void testConstructor() throws RemoteException {
        IBinder binderMock = mock(IBinder.class);
        RefcountedResource<IResource> childResource = getTestRefcountedResource();
        RefcountedResource<IResource> parentResource =
                getTestRefcountedResource(binderMock, childResource);

        // Verify parent's refcount starts at 1 (for user-reference)
        assertResourceState(parentResource, 1, 0, 0, 0, 0);

        // Verify child's refcounts were incremented
        assertResourceState(childResource, 2, 0, 0, 0, 0);

        // Verify linking to binder death
        verify(binderMock).linkToDeath(anyObject(), anyInt());
    }

    @Test
    public void testFailLinkToDeath() throws RemoteException {
        IBinder binderMock = mock(IBinder.class);
        doThrow(new RemoteException()).when(binderMock).linkToDeath(anyObject(), anyInt());

        RefcountedResource<IResource> refcountedResource = getTestRefcountedResource(binderMock);

        // Verify that cleanup is performed (Spy limitations prevent verification of method calls
        // for binder death scenario; check refcount to determine if cleanup was performed.)
        assertEquals(-1, refcountedResource.getReferenceCount());
    }

    @Test
    public void testCleanupAndRelease() throws RemoteException {
        IBinder binderMock = mock(IBinder.class);
        RefcountedResource<IResource> refcountedResource = getTestRefcountedResource(binderMock);

        // Verify user-initiated cleanup path decrements refcount and calls full cleanup flow
        refcountedResource.cleanupAndRelease();
        assertResourceState(refcountedResource, -1, 1, 1, 1, 1);

        // Verify user-initated cleanup path unlinks from binder
        verify(binderMock).unlinkToDeath(eq(refcountedResource), eq(0));
    }

    @Test
    public void testMultipleCallsToCleanupAndRelease() throws RemoteException {
        RefcountedResource<IResource> refcountedResource = getTestRefcountedResource();

        // Verify calling cleanupAndRelease multiple times does not change refcount or execute full
        // cleanup flow
        try {
            refcountedResource.cleanupAndRelease();
            refcountedResource.cleanupAndRelease();
            fail("Expected second call to cleanupAndRelease to throw an IllegalStateException");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testBinderDeath() throws RemoteException {
        RefcountedResource<IResource> refcountedResource = getTestRefcountedResource();

        // Verify binder death caused cleanup
        refcountedResource.binderDied();
        verify(refcountedResource, times(1)).binderDied();
        assertResourceState(refcountedResource, -1, 1, 1, 1, 1);
    }

    @Test
    public void testCleanupParentDecrementsChildRefcount() throws RemoteException {
        RefcountedResource<IResource> childResource = getTestRefcountedResource();
        RefcountedResource<IResource> parentResource = getTestRefcountedResource(childResource);

        parentResource.cleanupAndRelease();

        // Verify parent gets cleaned up properly, and triggers releaseIfUnreferencedRecursively on
        // child
        assertResourceState(childResource, 1, 0, 1, 0, 0);
        assertResourceState(parentResource, -1, 1, 1, 1, 1);
    }

    @Test
    public void testCleanupChildDoesNotTriggerRelease() throws RemoteException {
        RefcountedResource<IResource> childResource = getTestRefcountedResource();
        RefcountedResource<IResource> parentResource = getTestRefcountedResource(childResource);

        childResource.cleanupAndRelease();

        // Verify that child cleans up everything except kernel resources and quota.
        assertResourceState(childResource, 1, 1, 1, 1, 0);
        assertResourceState(parentResource, 1, 0, 0, 0, 0);
    }

    @Test
    public void testTwoParents() throws RemoteException {
        RefcountedResource<IResource> childResource = getTestRefcountedResource();
        RefcountedResource<IResource> parentResource1 = getTestRefcountedResource(childResource);
        RefcountedResource<IResource> parentResource2 = getTestRefcountedResource(childResource);

        childResource.cleanupAndRelease();
        assertResourceState(childResource, 2, 1, 1, 1, 0);

        parentResource1.cleanupAndRelease();
        assertResourceState(childResource, 1, 1, 2, 1, 0);

        parentResource2.cleanupAndRelease();
        assertResourceState(childResource, -1, 1, 3, 1, 1);
    }

    @Test
    public void testTwoChildren() throws RemoteException {
        RefcountedResource<IResource> childResource1 = getTestRefcountedResource();
        RefcountedResource<IResource> childResource2 = getTestRefcountedResource();
        RefcountedResource<IResource> parentResource =
                getTestRefcountedResource(childResource1, childResource2);

        childResource1.cleanupAndRelease();
        assertResourceState(childResource1, 1, 1, 1, 1, 0);
        assertResourceState(childResource2, 2, 0, 0, 0, 0);

        parentResource.cleanupAndRelease();
        assertResourceState(childResource1, -1, 1, 2, 1, 1);
        assertResourceState(childResource2, 1, 0, 1, 0, 0);

        childResource2.cleanupAndRelease();
        assertResourceState(childResource1, -1, 1, 2, 1, 1);
        assertResourceState(childResource2, -1, 1, 2, 1, 1);
    }

    @Test
    public void testSampleUdpEncapTranform() throws RemoteException {
        RefcountedResource<IResource> spi1 = getTestRefcountedResource();
        RefcountedResource<IResource> spi2 = getTestRefcountedResource();
        RefcountedResource<IResource> udpEncapSocket = getTestRefcountedResource();
        RefcountedResource<IResource> transform =
                getTestRefcountedResource(spi1, spi2, udpEncapSocket);

        // Pretend one SPI goes out of reference (releaseManagedResource -> cleanupAndRelease)
        spi1.cleanupAndRelease();

        // User called releaseManagedResource on udpEncap socket
        udpEncapSocket.cleanupAndRelease();

        // User dies, and binder kills the rest
        spi2.binderDied();
        transform.binderDied();

        // Check resource states
        assertResourceState(spi1, -1, 1, 2, 1, 1);
        assertResourceState(spi2, -1, 1, 2, 1, 1);
        assertResourceState(udpEncapSocket, -1, 1, 2, 1, 1);
        assertResourceState(transform, -1, 1, 1, 1, 1);
    }

    @Test
    public void testSampleDualTransformEncapSocket() throws RemoteException {
        RefcountedResource<IResource> spi1 = getTestRefcountedResource();
        RefcountedResource<IResource> spi2 = getTestRefcountedResource();
        RefcountedResource<IResource> spi3 = getTestRefcountedResource();
        RefcountedResource<IResource> spi4 = getTestRefcountedResource();
        RefcountedResource<IResource> udpEncapSocket = getTestRefcountedResource();
        RefcountedResource<IResource> transform1 =
                getTestRefcountedResource(spi1, spi2, udpEncapSocket);
        RefcountedResource<IResource> transform2 =
                getTestRefcountedResource(spi3, spi4, udpEncapSocket);

        // Pretend one SPIs goes out of reference (releaseManagedResource -> cleanupAndRelease)
        spi1.cleanupAndRelease();

        // User called releaseManagedResource on udpEncap socket and spi4
        udpEncapSocket.cleanupAndRelease();
        spi4.cleanupAndRelease();

        // User dies, and binder kills the rest
        spi2.binderDied();
        spi3.binderDied();
        transform2.binderDied();
        transform1.binderDied();

        // Check resource states
        assertResourceState(spi1, -1, 1, 2, 1, 1);
        assertResourceState(spi2, -1, 1, 2, 1, 1);
        assertResourceState(spi3, -1, 1, 2, 1, 1);
        assertResourceState(spi4, -1, 1, 2, 1, 1);
        assertResourceState(udpEncapSocket, -1, 1, 3, 1, 1);
        assertResourceState(transform1, -1, 1, 1, 1, 1);
        assertResourceState(transform2, -1, 1, 1, 1, 1);
    }
}
