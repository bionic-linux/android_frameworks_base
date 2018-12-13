/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.tests.ims;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;
import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;
import android.telephony.ims.RcsGroupThread;
import android.telephony.ims.RcsParticipant;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RcsGroupThreadTest {
    @Test
    public void testCanUnparcel() {
        RcsParticipant ownerParticipant = new RcsParticipant(5, "+5557654321");
        Uri groupIcon = Uri.parse("content://group_icon");

        RcsGroupThread rcsGroupThread = new RcsGroupThread(10, ownerParticipant, "Group name",
                groupIcon, "conferenceUri");

        Parcel parcel = Parcel.obtain();
        rcsGroupThread.writeToParcel(parcel, rcsGroupThread.describeContents());

        parcel.setDataPosition(0);
        rcsGroupThread = RcsGroupThread.CREATOR.createFromParcel(parcel);

        assertThat(rcsGroupThread.getThreadId()).isEqualTo(10);
        assertThat(rcsGroupThread.getConferenceUri()).isEqualTo("conferenceUri");
        assertThat(rcsGroupThread.getGroupName()).isEqualTo("Group name");
        assertThat(rcsGroupThread.getGroupIcon()).isEqualTo(groupIcon);
        assertThat(rcsGroupThread.getOwner()).isEqualTo(ownerParticipant);
    }
}
