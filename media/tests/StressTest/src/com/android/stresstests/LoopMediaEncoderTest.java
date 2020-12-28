/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.mediastresstest;

import android.media.MediaCodec;
import android.media.MediaFormat;

import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import static com.android.mediastresstest.CodecTestBase.selectCodecs;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class LoopMediaEncoderTest {
    private final int mMaxSamples = 90;
    private final int mMaxIterations = 10;
    private final long mSeed = 0x12b9b0a1;  // random seed
    private final String mMime;
    private final int[] mBitrates;
    private final int[] mParamList1;
    private final int[] mParamList2;

    public LoopMediaEncoderTest(String mime, int[] bitrates, int[] encoderInfo1,
            int[] encoderInfo2) {
        mMime = mime;
        mBitrates = bitrates;
        mParamList1 = encoderInfo1;
        mParamList2 = encoderInfo2;
    }

    @Parameterized.Parameters(name = "{index}({0})")
    public static Collection<Object[]> input() {
        final boolean isEncoder = true;
        final boolean needAudio = true;
        final boolean needVideo = true;
        return CodecEncoderTestBase
                .prepareParamList(CodecEncoderTest.exhaustiveArgsList, isEncoder, needAudio,
                        needVideo, true);
    }

    @SmallTest
    @Test
    public void testConfigureAndReset() throws IOException {
        CodecEncoderTest cet = new CodecEncoderTest(mMime, mBitrates, mParamList1, mParamList2);
        cet.setUpParams(0);
        cet.setUpSource();
        ArrayList<String> listOfEncoders = selectCodecs(mMime, cet.mFormats, null, true);
        Assume.assumeTrue("no suitable codecs found for mime: " + mMime, !listOfEncoders.isEmpty());
        Random rand = new Random(mSeed);
        for (String encoder : listOfEncoders) {
            cet.mCodec = MediaCodec.createByCodecName(encoder);
            MediaFormat inpFormat = cet.mFormats.get(0);
            for (int i = 0; i < mMaxIterations; i++) {
                boolean isAsync = ((rand.nextInt() & 1) == 0);
                cet.configureCodec(inpFormat, isAsync, false, true);
                cet.mCodec.reset();
            }
            cet.mCodec.release();
        }
    }

    @SmallTest
    @Test
    public void testConfigureAndStop() throws IOException {
        CodecEncoderTest cet = new CodecEncoderTest(mMime, mBitrates, mParamList1, mParamList2);
        cet.setUpParams(0);
        cet.setUpSource();
        ArrayList<String> listOfEncoders = selectCodecs(mMime, cet.mFormats, null, true);
        Assume.assumeTrue("no suitable codecs found for mime: " + mMime, !listOfEncoders.isEmpty());
        Random rand = new Random(mSeed);
        for (String encoder : listOfEncoders) {
            cet.mCodec = MediaCodec.createByCodecName(encoder);
            MediaFormat inpFormat = cet.mFormats.get(0);
            for (int i = 0; i < mMaxIterations; i++) {
                boolean isAsync = ((rand.nextInt() & 1) == 0);
                cet.configureCodec(inpFormat, isAsync, false, true);
                cet.mCodec.start();
                cet.mCodec.stop();
            }
            cet.mCodec.release();
        }
    }

    @LargeTest
    @Test(timeout = mMaxIterations * CodecTestBase.PER_TEST_TIMEOUT_LARGE_TEST_MS)
    public void testEncoder() throws IOException, InterruptedException {
        CodecEncoderTest cet = new CodecEncoderTest(mMime, mBitrates, mParamList1, mParamList2);
        cet.setUpParams(Integer.MAX_VALUE);
        ArrayList<String> listOfEncoders = selectCodecs(mMime, cet.mFormats, null, true);
        Assume.assumeTrue("no suitable codecs found for mime: " + mMime, !listOfEncoders.isEmpty());
        Random rand = new Random(mSeed);
        for (String encoder : listOfEncoders) {
            cet.mCodec = MediaCodec.createByCodecName(encoder);
            for (int i = 1; i <= mMaxIterations; i++) {
                boolean isAsync = ((rand.nextInt() & 1) == 0);
                boolean eosType = ((rand.nextInt() & 1) == 0);
                int numFrames = rand.nextInt(mMaxSamples);
                if ((i & (i - 1)) == 0) numFrames = Integer.MAX_VALUE;
                assertTrue(CodecEncoderTest.isEncoderRunPass(cet, encoder, isAsync, eosType,
                        numFrames, CodecEncoderTest.randomChoice(rand)));
                cet.encode(encoder, isAsync, eosType, numFrames);
            }
            cet.mCodec.release();
        }
    }
}
