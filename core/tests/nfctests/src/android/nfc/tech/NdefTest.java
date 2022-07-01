/*
 * Copyright 2022 The Android Open Source Project
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

package android.nfc.tech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.nfc.ErrorCodes;
import android.nfc.FormatException;
import android.nfc.INfcTag;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.RemoteException;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;



/**
 * Test of {@link Ndef}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class NdefTest {

    private final int mHandle = 1; // non-zero service handle.
    private NdefMessage mNdefMsg1 = new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                  new byte[] {1, 2, 3}, new byte[] {4, 5, 6}, new byte[] {7, 8, 9}));
    private INfcTag mMockTagService = mock(INfcTag.class);

    @Test
    public void testConnectAndClose() throws RemoteException {
        Ndef ndef = Ndef.get(getNdefTag(mMockTagService, mNdefMsg1));
        assertNotNull(ndef);
        doReturn(ErrorCodes.SUCCESS).when(mMockTagService).connect(anyInt(),
                eq(TagTechnology.NDEF));
        doNothing().when(mMockTagService).resetTimeouts();
        doReturn(ErrorCodes.SUCCESS).when(mMockTagService).reconnect(anyInt());
        doReturn(true).when(mMockTagService).isTagUpToDate(anyLong());
        doReturn(true).when(mMockTagService).isPresent(anyInt());
        try {
            ndef.connect();
            assertTrue(ndef.isConnected());
            ndef.close();
            assertTrue(!ndef.isConnected());
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException", e);
        }
        verify(mMockTagService, times(1)).connect(eq(mHandle), anyInt());
    }

    @Test
    public void testGet() {
        assertNull(Ndef.get(getNfcATag(mMockTagService)));
        assertNotNull(Ndef.get(getNdefTag(mMockTagService, mNdefMsg1)));
    }

    @Test
    public void testCanMakeReadOnly() throws RemoteException {
        Tag tag = getNdefTag(mMockTagService, mNdefMsg1);
        Ndef ndef = Ndef.get(tag);
        Bundle extras = tag.getTechExtras(TagTechnology.NDEF);
        assertNotNull(ndef);
        doReturn(true).when(mMockTagService).isTagUpToDate(anyLong());
        doReturn(true).when(mMockTagService).canMakeReadOnly(anyInt());
        assertTrue(ndef.canMakeReadOnly());
        verify(mMockTagService, times(1)).canMakeReadOnly(eq(extras.getInt(Ndef.EXTRA_NDEF_TYPE)));
    }

    @Test
    public void testGetCachedNdefMessage() {
        Ndef ndef = Ndef.get(getNdefTag(mMockTagService, mNdefMsg1));
        assertNotNull(ndef);
        assertEquals(ndef.getCachedNdefMessage(), mNdefMsg1);
    }

    @Test
    public void testGetMaxSize() {
        Tag tag = getNdefTag(mMockTagService, mNdefMsg1);
        Ndef ndef = Ndef.get(tag);
        Bundle extras = tag.getTechExtras(TagTechnology.NDEF);
        assertNotNull(ndef);
        assertEquals(ndef.getMaxSize(), extras.getInt(Ndef.EXTRA_NDEF_MAXLENGTH));
    }

    @Test
    public void testGetNdefMessageSuccess() throws RemoteException {
        NdefMessage msgRead = new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                  new byte[] {1, 2, 3}, new byte[] {1, 2, 3}, new byte[] { 1, 2, 3}));
        NdefMessage msgSend = new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                  new byte[] {1, 1, 1}, new byte[] {2, 2, 2}, new byte[] { 3, 3, 3}));
        doReturn(msgSend).when(mMockTagService).ndefRead(anyInt());
        doReturn(true).when(mMockTagService).isNdef(anyInt());
        doReturn(true).when(mMockTagService).isTagUpToDate(anyLong());
        doReturn(ErrorCodes.SUCCESS).when(mMockTagService).connect(anyInt(),
                eq(TagTechnology.NDEF));
        Ndef ndef = Ndef.get(getNdefTag(mMockTagService, mNdefMsg1));
        try {
            ndef.connect();
            msgRead = ndef.getNdefMessage();
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException", e);
        } catch (FormatException e) {
            throw new AssertionError("Unexpected FormatException", e);
        }
        verify(mMockTagService, times(1)).ndefRead(mHandle);
        assertEquals(msgSend, msgRead);
    }

    @Test
    public void testGetTag() {
        Tag tag = getNdefTag(mMockTagService, mNdefMsg1);
        Ndef ndef = Ndef.get(tag);
        assertNotNull(ndef);
        assertEquals(tag, ndef.getTag());
    }

    @Test
    public void testGetType() {
        Ndef ndef = Ndef.get(getNdefTag(mMockTagService, mNdefMsg1));
        assertNotNull(ndef);
        assertEquals(ndef.getType(), Ndef.NFC_FORUM_TYPE_2);
    }

    @Test
    public void testIsWritable() {
        Ndef ndef = Ndef.get(getNdefTag(mMockTagService, mNdefMsg1));
        assertNotNull(ndef);
        assertEquals(ndef.isWritable(), true);
        Ndef ndef2 = Ndef.get(getNdefTagReadOnly(mMockTagService, mNdefMsg1));
        assertNotNull(ndef2);
        assertEquals(ndef2.isWritable(), false);
    }

    @Test
    public void testMakeReadOnlySuccess() throws RemoteException {
        doReturn(ErrorCodes.SUCCESS).when(mMockTagService).ndefMakeReadOnly(anyInt());
        doReturn(true).when(mMockTagService).isNdef(anyInt());
        doReturn(true).when(mMockTagService).isTagUpToDate(anyLong());
        doReturn(ErrorCodes.SUCCESS).when(mMockTagService).connect(anyInt(),
                eq(TagTechnology.NDEF));
        Ndef ndef = Ndef.get(getNdefTag(mMockTagService, mNdefMsg1));
        // SUCCESS
        try {
            ndef.connect();
            assertTrue(ndef.makeReadOnly());
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException", e);
        }
        verify(mMockTagService, times(1)).ndefMakeReadOnly(mHandle);
    }

    @Test
    public void testWriteNdefMessageSuccess() throws RemoteException {
        doReturn(ErrorCodes.SUCCESS).when(mMockTagService).ndefWrite(anyInt(), any());
        doReturn(true).when(mMockTagService).isNdef(anyInt());
        doReturn(true).when(mMockTagService).isTagUpToDate(anyLong());
        doReturn(ErrorCodes.SUCCESS).when(mMockTagService).connect(anyInt(),
                eq(TagTechnology.NDEF));
        Ndef ndef = Ndef.get(getNdefTag(mMockTagService, mNdefMsg1));
        NdefMessage ndefMsg = new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                  new byte[] {1, 1, 1}, new byte[] {2, 2, 2}, new byte[] { 3, 3, 3}));
        // SUCCESS
        try {
            ndef.connect();
            ndef.writeNdefMessage(ndefMsg);
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException", e);
        } catch (FormatException e) {
            throw new AssertionError("Unexpected FormatException", e);
        }
        verify(mMockTagService, times(1)).ndefWrite(anyInt(), any());
    }

    @Test
    public void testWriteNdefMessageErrorIo() throws RemoteException {
        doReturn(ErrorCodes.ERROR_IO).when(mMockTagService).ndefWrite(anyInt(), any());
        doReturn(true).when(mMockTagService).isNdef(anyInt());
        doReturn(true).when(mMockTagService).isTagUpToDate(anyLong());
        doReturn(ErrorCodes.SUCCESS).when(mMockTagService).connect(anyInt(),
                eq(TagTechnology.NDEF));
        Ndef ndef = Ndef.get(getNdefTag(mMockTagService, mNdefMsg1));
        NdefMessage ndefMsg = new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                  new byte[] {1, 1, 1}, new byte[] {2, 2, 2}, new byte[] { 3, 3, 3}));
        // IOException
        try {
            ndef.connect();
            ndef.writeNdefMessage(ndefMsg);
            fail("Expect a IOException");
        } catch (IOException e) {
            // Expected
        } catch (FormatException e) {
            throw new AssertionError("Unexpected FormatException", e);
        }
        verify(mMockTagService, times(1)).ndefWrite(eq(mHandle), any());
    }

    @Test
    public void testWriteNdefMessageInvalidParam() throws RemoteException {
        doReturn(ErrorCodes.ERROR_INVALID_PARAM).when(mMockTagService).ndefWrite(anyInt(), any());
        doReturn(true).when(mMockTagService).isNdef(anyInt());
        doReturn(true).when(mMockTagService).isTagUpToDate(anyLong());
        doReturn(ErrorCodes.SUCCESS).when(mMockTagService).connect(anyInt(),
                eq(TagTechnology.NDEF));
        Ndef ndef = Ndef.get(getNdefTag(mMockTagService, mNdefMsg1));
        NdefMessage ndefMsg = new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                  new byte[] {1, 1, 1}, new byte[] {2, 2, 2}, new byte[] { 3, 3, 3}));
        // FormatException
        try {
            ndef.connect();
            ndef.writeNdefMessage(ndefMsg);
            fail("Expect a FormatException");
        } catch (IOException e) {
            fail("Unexpected IOException");
        } catch (FormatException e) {
            // Expected
        }
        verify(mMockTagService, times(1)).ndefWrite(eq(mHandle), any());
    }


    private Tag getNfcATag(INfcTag tagService) {
        byte[] id = {0x01, 0x02, 0x03, 0x04};
        int cookie = 0;
        int[] techList = {TagTechnology.NFC_A};
        byte[] atqa = {0x04, 0x00};
        Bundle extraBundle = new Bundle();
        Bundle[] techListExtras = new Bundle[techList.length];
        extraBundle.putShort(NfcA.EXTRA_SAK, (short) 0x20);
        extraBundle.putByteArray(NfcA.EXTRA_ATQA, atqa);
        techListExtras[0] = extraBundle;
        return new Tag(id, techList, techListExtras, mHandle, cookie, tagService);
    }

    private Tag getNdefTag(INfcTag tagService, NdefMessage ndefMsg) {
        byte[] id = {0x01, 0x02, 0x03, 0x04};
        byte[] atqa = {0x04, 0x00};
        short sak = (short) 0x20;
        int cookie = 0;
        int[] techList = {TagTechnology.NFC_A, TagTechnology.NDEF};
        Bundle extraBundleA = new Bundle();
        Bundle extraBundleNdef = new Bundle();
        Bundle[] techListExtras = new Bundle[techList.length];
        extraBundleA.putShort(NfcA.EXTRA_SAK, sak);
        extraBundleA.putByteArray(NfcA.EXTRA_ATQA, atqa);
        extraBundleNdef.putParcelable(Ndef.EXTRA_NDEF_MSG, ndefMsg);
        extraBundleNdef.putInt(Ndef.EXTRA_NDEF_MAXLENGTH, 9);
        extraBundleNdef.putInt(Ndef.EXTRA_NDEF_CARDSTATE, Ndef.NDEF_MODE_READ_WRITE);
        extraBundleNdef.putInt(Ndef.EXTRA_NDEF_TYPE, Ndef.TYPE_2);
        techListExtras[0] = extraBundleA;
        techListExtras[1] = extraBundleNdef;
        return new Tag(id, techList, techListExtras, mHandle, cookie, tagService);
    }

    private Tag getNdefTagReadOnly(INfcTag tagService, NdefMessage ndefMsg) {
        byte[] id = {0x01, 0x02, 0x03, 0x04};
        byte[] atqa = {0x04, 0x00};
        short sak = (short) 0x20;
        int cookie = 0;
        int[] techList = {TagTechnology.NFC_A, TagTechnology.NDEF};
        Bundle extraBundleA = new Bundle();
        Bundle extraBundleNdef = new Bundle();
        Bundle[] techListExtras = new Bundle[techList.length];
        extraBundleA.putShort(NfcA.EXTRA_SAK, sak);
        extraBundleA.putByteArray(NfcA.EXTRA_ATQA, atqa);
        extraBundleNdef.putParcelable(Ndef.EXTRA_NDEF_MSG, ndefMsg);
        extraBundleNdef.putInt(Ndef.EXTRA_NDEF_MAXLENGTH, 9);
        extraBundleNdef.putInt(Ndef.EXTRA_NDEF_CARDSTATE, Ndef.NDEF_MODE_READ_ONLY);
        extraBundleNdef.putInt(Ndef.EXTRA_NDEF_TYPE, Ndef.TYPE_2);
        techListExtras[0] = extraBundleA;
        techListExtras[1] = extraBundleNdef;
        return new Tag(id, techList, techListExtras, mHandle, cookie, tagService);
    }
}
