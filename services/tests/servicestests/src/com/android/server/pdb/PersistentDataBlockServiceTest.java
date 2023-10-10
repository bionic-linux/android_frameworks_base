/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.pdb;

import static com.android.server.pdb.PersistentDataBlockService.DIGEST_SIZE_BYTES;
import static com.android.server.pdb.PersistentDataBlockService.HEADER_SIZE;
import static com.android.server.pdb.PersistentDataBlockService.MAX_FRP_CREDENTIAL_HANDLE_SIZE;
import static com.android.server.pdb.PersistentDataBlockService.MAX_TEST_MODE_DATA_SIZE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertThrows;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserManager;
import android.service.persistentdata.IPersistentDataBlockService;

import androidx.test.core.app.ApplicationProvider;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

@RunWith(JUnitParamsRunner.class)
public class PersistentDataBlockServiceTest {
    private static final String TAG = "PersistentDataBlockServiceTest";

    private static final byte[] SMALL_DATA = "data to write".getBytes();
    private static final byte[] ANOTHER_SMALL_DATA = "something else".getBytes();

    private Context mContext;
    private PersistentDataBlockService mPdbService;
    private IPersistentDataBlockService mInterface;
    private PersistentDataBlockManagerInternal mInternalInterface;
    private File mDataBlockFile;

    @Mock private UserManager mUserManager;

    private static class FakePersistentDataBlockService extends PersistentDataBlockService {
        FakePersistentDataBlockService(Context context, String dataBlockFile) {
            super(context, /* isFileBacked */ true, dataBlockFile);
        }

        @Override
        void setProperty(String key, String value) {
            // Do nothing. Setting a property is a result (which doesn't work in unit test) and it's
            // not currently verified in the test.
        }
    }

    @Rule public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mDataBlockFile = mTemporaryFolder.newFile();
        mContext = spy(ApplicationProvider.getApplicationContext());
        mPdbService = new FakePersistentDataBlockService(mContext, mDataBlockFile.getPath());
        mPdbService.setAllowedUid(Binder.getCallingUid());
        mPdbService.formatPartitionLocked(/* setOemUnlockEnabled */ false);
        mInterface = mPdbService.getInterfaceForTesting();
        mInternalInterface = mPdbService.getInternalInterfaceForTesting();

        when(mContext.getSystemService(eq(Context.USER_SERVICE))).thenReturn(mUserManager);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
    }

    abstract static class Block {
        public PersistentDataBlockService service;

        abstract int maxBlockSize();
        abstract int write(byte[] data) throws RemoteException;
        abstract byte[] read() throws RemoteException;
    }

    /**
     * Configuration for parameterizing tests, including the block name, maximum block size, and
     * an block implementation for the read/write operations.
     */
    public Object[][] getTestParametersForBlocks() {
        return new Object[][] {
            {
                new Block() {
                    @Override public int maxBlockSize() {
                        return service.getMaximumFrpDataSize();
                    }

                    @Override public int write(byte[] data) throws RemoteException {
                        return service.getInterfaceForTesting().write(data);
                    }

                    @Override public byte[] read() throws RemoteException {
                        return service.getInterfaceForTesting().read();
                    }
                },
            },
            {
                new Block() {
                    @Override public int maxBlockSize() {
                        return MAX_FRP_CREDENTIAL_HANDLE_SIZE;
                    }

                    @Override public int write(byte[] data) {
                        try {
                            service.getInternalInterfaceForTesting().setFrpCredentialHandle(data);
                            // The written size isn't returned. Pretend it's fully written in the
                            // test for now.
                            return data.length;
                        } catch (IllegalArgumentException e) {
                            // Handle the exception in a similar way as frp block, just to make it
                            // easier to parameterize the test.
                            return -1;
                        }
                    }

                    @Override public byte[] read() {
                        try {
                            return service.getInternalInterfaceForTesting()
                                    .getFrpCredentialHandle();
                        } catch (IllegalStateException e) {
                            // Handle the exception in a similar way as frp block, just to make it
                            // easier to parameterize the test.
                            return new byte[0];
                        }
                    }
                },
            },
            {
                new Block() {
                    @Override public int maxBlockSize() {
                        return MAX_TEST_MODE_DATA_SIZE;
                    }

                    @Override public int write(byte[] data) {
                        try {
                            service.getInternalInterfaceForTesting().setTestHarnessModeData(data);
                            // The written size isn't returned. Pretend it's fully written in the
                            // test for now.
                            return data.length;
                        } catch (IllegalArgumentException e) {
                            // Handle the exception in a similar way as frp block, just to make it
                            // easier to parameterize the test.
                            return -1;
                        }
                    }

                    @Override public byte[] read() {
                        try {
                            return service.getInternalInterfaceForTesting()
                                    .getTestHarnessModeData();
                        } catch (IllegalStateException e) {
                            // Handle the exception in a similar way as frp block, just to make it
                            // easier to parameterize the test.
                            return new byte[0];
                        }
                    }
                },
            },
        };
    }

    @Test
    @Parameters(method = "getTestParametersForBlocks")
    public void writeThenRead(Block block) throws Exception {
        block.service = mPdbService;
        assertThat(block.write(SMALL_DATA)).isEqualTo(SMALL_DATA.length);
        assertThat(block.read()).isEqualTo(SMALL_DATA);
    }

    @Test
    @Parameters(method = "getTestParametersForBlocks")
    public void writeOutOfBound(Block block) throws Exception {
        block.service = mPdbService;
        byte[] maxData = new byte[block.maxBlockSize()];
        assertThat(block.write(maxData)).isEqualTo(maxData.length);

        byte[] overflowData = new byte[block.maxBlockSize() + 1];
        assertThat(block.write(overflowData)).isLessThan(0);
    }

    @Test
    @Parameters(method = "getTestParametersForBlocks")
    public void readCorruptedData(Block block) throws Exception {
        block.service = mPdbService;
        assertThat(block.write(SMALL_DATA)).isEqualTo(SMALL_DATA.length);
        assertThat(block.read()).isEqualTo(SMALL_DATA);

        tamperWithDigest();

        // Expect the read to trigger formatting, resulting in reading empty data.
        assertThat(block.read()).hasLength(0);
    }

    @Test
    @Parameters(method = "getTestParametersForBlocks")
    public void writeWhileAlreadyCorrupted(Block block) throws Exception {
        block.service = mPdbService;
        assertThat(block.write(SMALL_DATA)).isEqualTo(SMALL_DATA.length);
        assertThat(block.read()).isEqualTo(SMALL_DATA);

        tamperWithDigest();

        // In the currently implementation, expect the write to not trigger formatting.
        assertThat(block.write(ANOTHER_SMALL_DATA)).isEqualTo(ANOTHER_SMALL_DATA.length);
    }

    @Test
    public void frpBlockReadWriteWithoutPermission() throws Exception {
        mPdbService.setAllowedUid(Binder.getCallingUid() + 1);  // unexpected uid
        assertThrows(SecurityException.class, () -> mInterface.write(SMALL_DATA));
        assertThrows(SecurityException.class, () -> mInterface.read());
    }

    @Test
    public void getMaximumDataBlockSizeDenied() throws Exception {
        mPdbService.setAllowedUid(Binder.getCallingUid() + 1);  // unexpected uid
        assertThrows(SecurityException.class, () -> mInterface.getMaximumDataBlockSize());
    }

    @Test
    public void getMaximumDataBlockSize() throws Exception {
        mPdbService.setAllowedUid(Binder.getCallingUid());
        assertThat(mInterface.getMaximumDataBlockSize())
                .isEqualTo(mPdbService.getMaximumFrpDataSize());
    }

    @Test
    public void getFrpDataBlockSizeGrantedByUid() throws Exception {
        assertThat(mInterface.write(SMALL_DATA)).isEqualTo(SMALL_DATA.length);

        mPdbService.setAllowedUid(Binder.getCallingUid());
        assertThat(mInterface.getDataBlockSize()).isEqualTo(SMALL_DATA.length);

        // Modify the magic / type marker. In the current implementation, getting the FRP data block
        // size does not check digest.
        tamperWithMagic();
        assertThat(mInterface.getDataBlockSize()).isEqualTo(0);
    }

    @Test
    public void getFrpDataBlockSizeGrantedByPermission() throws Exception {
        assertThat(mInterface.write(SMALL_DATA)).isEqualTo(SMALL_DATA.length);

        mPdbService.setAllowedUid(Binder.getCallingUid() + 1);  // unexpected uid
        grantAccessPdbStatePermission();

        assertThat(mInterface.getDataBlockSize()).isEqualTo(SMALL_DATA.length);

        // Modify the magic / type marker. In the current implementation, getting the FRP data block
        // size does not check digest.
        tamperWithMagic();
        assertThat(mInterface.getDataBlockSize()).isEqualTo(0);
    }

    @Test
    public void wipePermissionCheck() throws Exception {
        denyOemUnlockPermission();
        assertThrows(SecurityException.class, () -> mInterface.wipe());
    }

    @Test
    public void wipeMakesItNotWritable() throws Exception {
        grantOemUnlockPermission();
        mInterface.wipe();

        // Verify that nothing is written.
        assertThat(mInterface.write(SMALL_DATA)).isLessThan(0);
        assertThat(readBackingFile(DIGEST_SIZE_BYTES + HEADER_SIZE, SMALL_DATA.length).array())
                .isEqualTo(new byte[SMALL_DATA.length]);

        mInternalInterface.setFrpCredentialHandle(SMALL_DATA);
        assertThat(readBackingFile(mPdbService.getFrpCredentialDataOffset(), SMALL_DATA.length)
                .array())
                .isEqualTo(new byte[SMALL_DATA.length]);

        mInternalInterface.setTestHarnessModeData(SMALL_DATA);
        assertThat(readBackingFile(mPdbService.getTestHarnessModeDataOffset(), SMALL_DATA.length)
                .array())
                .isEqualTo(new byte[SMALL_DATA.length]);
    }

    @Test
    public void hasFrpCredentialHandleGrantedByUid() throws Exception {
        mPdbService.setAllowedUid(Binder.getCallingUid());

        assertThat(mInterface.hasFrpCredentialHandle()).isFalse();
        mInternalInterface.setFrpCredentialHandle(SMALL_DATA);
        assertThat(mInterface.hasFrpCredentialHandle()).isTrue();
    }

    @Test
    public void hasFrpCredentialHandleGrantedByPermission() throws Exception {
        mPdbService.setAllowedUid(Binder.getCallingUid() + 1);  // unexpected uid
        grantAccessPdbStatePermission();

        assertThat(mInterface.hasFrpCredentialHandle()).isFalse();
        mInternalInterface.setFrpCredentialHandle(SMALL_DATA);
        assertThat(mInterface.hasFrpCredentialHandle()).isTrue();
    }

    @Test
    public void oemUnlockWithoutPermission() throws Exception {
        denyOemUnlockPermission();

        assertThrows(SecurityException.class, () -> mInterface.setOemUnlockEnabled(true));
    }

    @Test
    public void oemUnlockNotAdmin() throws Exception {
        grantOemUnlockPermission();
        makeUserAdmin(false);

        assertThrows(SecurityException.class, () -> mInterface.setOemUnlockEnabled(true));
    }

    @Test
    public void oemUnlock() throws Exception {
        grantOemUnlockPermission();
        makeUserAdmin(true);

        mInterface.setOemUnlockEnabled(true);
        assertThat(mInterface.getOemUnlockEnabled()).isTrue();
    }

    @Test
    public void oemUnlockUserRestriction_OemUnlock() throws Exception {
        grantOemUnlockPermission();
        makeUserAdmin(true);
        when(mUserManager.hasUserRestriction(eq(UserManager.DISALLOW_OEM_UNLOCK)))
                .thenReturn(true);

        assertThrows(SecurityException.class, () -> mInterface.setOemUnlockEnabled(true));
    }

    @Test
    public void oemUnlockUserRestriction_FactoryReset() throws Exception {
        grantOemUnlockPermission();
        makeUserAdmin(true);
        when(mUserManager.hasUserRestriction(eq(UserManager.DISALLOW_FACTORY_RESET)))
                .thenReturn(true);

        assertThrows(SecurityException.class, () -> mInterface.setOemUnlockEnabled(true));
    }

    @Test
    public void oemUnlockIgnoreTampering() throws Exception {
        grantOemUnlockPermission();
        makeUserAdmin(true);

        // The current implementation does not check digest before set or get the oem unlock bit.
        tamperWithDigest();
        mInterface.setOemUnlockEnabled(true);
        tamperWithDigest();
        assertThat(mInterface.getOemUnlockEnabled()).isTrue();
    }

    @Test
    public void getOemUnlockEnabledPermissionCheck_NoPermission() throws Exception {
        assertThrows(SecurityException.class, () -> mInterface.getOemUnlockEnabled());
    }

    @Test
    public void getOemUnlockEnabledPermissionCheck_OemUnlcokState() throws Exception {
        doReturn(PackageManager.PERMISSION_GRANTED).when(mContext)
                .checkCallingOrSelfPermission(eq(Manifest.permission.OEM_UNLOCK_STATE));
        assertThat(mInterface.getOemUnlockEnabled()).isFalse();
    }

    @Test
    public void getOemUnlockEnabledPermissionCheck_ReadOemUnlcokState() throws Exception {
        doReturn(PackageManager.PERMISSION_GRANTED).when(mContext)
                .checkCallingOrSelfPermission(eq(Manifest.permission.READ_OEM_UNLOCK_STATE));
        assertThat(mInterface.getOemUnlockEnabled()).isFalse();
    }

    @Test
    public void getFlashLockStatePermissionCheck_NoPermission() throws Exception {
        assertThrows(SecurityException.class, () -> mInterface.getFlashLockState());
    }

    @Test
    public void getFlashLockStatePermissionCheck_OemUnlcokState() throws Exception {
        doReturn(PackageManager.PERMISSION_GRANTED).when(mContext)
                .checkCallingOrSelfPermission(eq(Manifest.permission.OEM_UNLOCK_STATE));
        mInterface.getFlashLockState();  // Do not throw
    }

    @Test
    public void getFlashLockStatePermissionCheck_ReadOemUnlcokState() throws Exception {
        doReturn(PackageManager.PERMISSION_GRANTED).when(mContext)
                .checkCallingOrSelfPermission(eq(Manifest.permission.READ_OEM_UNLOCK_STATE));
        mInterface.getFlashLockState();  // Do not throw
    }

    private void tamperWithDigest() throws Exception {
        try (var ch = FileChannel.open(mDataBlockFile.toPath(), StandardOpenOption.WRITE)) {
            ch.write(ByteBuffer.wrap("tampered-digest".getBytes()));
        }
    }

    private void tamperWithMagic() throws Exception {
        try (var ch = FileChannel.open(mDataBlockFile.toPath(), StandardOpenOption.WRITE)) {
            ch.write(ByteBuffer.wrap("mark".getBytes()), DIGEST_SIZE_BYTES);
        }
    }

    private void makeUserAdmin(boolean isAdmin) {
        when(mUserManager.isUserAdmin(anyInt())).thenReturn(isAdmin);
    }

    private void grantOemUnlockPermission() {
        doReturn(PackageManager.PERMISSION_GRANTED).when(mContext)
                .checkCallingOrSelfPermission(eq(Manifest.permission.OEM_UNLOCK_STATE));
        doNothing().when(mContext)
                .enforceCallingOrSelfPermission(eq(Manifest.permission.OEM_UNLOCK_STATE),
                        anyString());
    }

    private void denyOemUnlockPermission() {
        doReturn(PackageManager.PERMISSION_DENIED).when(mContext)
                .checkCallingOrSelfPermission(eq(Manifest.permission.OEM_UNLOCK_STATE));
    }

    private void grantAccessPdbStatePermission() {
        doReturn(PackageManager.PERMISSION_GRANTED).when(mContext)
                .checkCallingPermission(eq(Manifest.permission.ACCESS_PDB_STATE));
    }

    private ByteBuffer readBackingFile(long position, int size) throws Exception {
        try (var ch = FileChannel.open(mDataBlockFile.toPath(), StandardOpenOption.READ)) {
            var buffer = ByteBuffer.allocate(size);
            assertThat(ch.read(buffer, position)).isGreaterThan(0);
            return buffer;
        }
    }
}
