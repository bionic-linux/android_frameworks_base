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

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;
import android.telephony.ims.Rcs1To1Thread;
import android.telephony.ims.RcsParticipant;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class Rcs1To1ThreadTest {

    @Test
    public void testCanUnparcel() {
        RcsParticipant recipient = new RcsParticipant(55, "+5551234567");
        Rcs1To1Thread rcs1To1Thread = new Rcs1To1Thread(123, recipient, 456);

        Parcel parcel = Parcel.obtain();
        rcs1To1Thread.writeToParcel(parcel, rcs1To1Thread.describeContents());

        parcel.setDataPosition(0);
        rcs1To1Thread = Rcs1To1Thread.CREATOR.createFromParcel(parcel);

        assertThat(rcs1To1Thread.getThreadId()).isEqualTo(123);
        assertThat(rcs1To1Thread.getRecipient()).isEqualTo(recipient);
        assertThat(rcs1To1Thread.getFallbackThreadId()).isEqualTo(456);
    }
}
