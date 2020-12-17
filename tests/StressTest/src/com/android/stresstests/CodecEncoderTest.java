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

package com.android.stresstests;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class CodecEncoderTest extends CodecEncoderTestBase {
    private static final String LOG_TAG = CodecEncoderTest.class.getSimpleName();
    private final int[] mBitrates;
    private final int[] mEncParamList1;
    private final int[] mEncParamList2;
    public ArrayList<MediaFormat> mFormats;

    public CodecEncoderTest(String mime, int[] bitrates, int[] encoderInfo1, int[] encoderInfo2) {
        super(mime);
        mBitrates = bitrates;
        mEncParamList1 = encoderInfo1;
        mEncParamList2 = encoderInfo2;
        mFormats = new ArrayList<>();
    }

    void dequeueOutput(int bufferIndex, MediaCodec.BufferInfo info) {
        super.dequeueOutput(bufferIndex, info);
    }

    public static final List<Object[]> exhaustiveArgsList = Arrays.asList(new Object[][]{
            // Audio - CodecMime, arrays of bit-rates, sample rates, channel counts
            {MediaFormat.MIMETYPE_AUDIO_AAC, new int[]{64000}, new int[]{8000, 48000},
                    new int[]{1}},
            {MediaFormat.MIMETYPE_AUDIO_OPUS, new int[]{6600, 23850}, new int[]{16000},
                    new int[]{1}},
            {MediaFormat.MIMETYPE_AUDIO_AMR_NB, new int[]{4750, 12200}, new int[]{8000},
                    new int[]{1}},
            {MediaFormat.MIMETYPE_AUDIO_AMR_WB, new int[]{6600, 23850}, new int[]{16000},
                    new int[]{1}},
            /* TODO(169310292) */
            {MediaFormat.MIMETYPE_AUDIO_FLAC, new int[]{8}, new int[]{8000, 192000}, new int[]{1}},

            // Video - CodecMime, arrays of bit-rates, height, width
            {MediaFormat.MIMETYPE_VIDEO_H263, new int[]{32000, 64000}, new int[]{176},
                    new int[]{144}},
            {MediaFormat.MIMETYPE_VIDEO_MPEG4, new int[]{32000, 64000}, new int[]{176},
                    new int[]{144}},
            {MediaFormat.MIMETYPE_VIDEO_AVC, new int[]{256000}, new int[]{176, 352},
                    new int[]{144, 240}},
            {MediaFormat.MIMETYPE_VIDEO_HEVC, new int[]{256000}, new int[]{176, 352,},
                    new int[]{144, 240}},
            {MediaFormat.MIMETYPE_VIDEO_VP8, new int[]{256000}, new int[]{176, 352},
                    new int[]{144, 240}},
            {MediaFormat.MIMETYPE_VIDEO_VP9, new int[]{256000}, new int[]{176, 352},
                    new int[]{144, 240}},
            {MediaFormat.MIMETYPE_VIDEO_AV1, new int[]{256000}, new int[]{176, 352},
                    new int[]{144, 240}},
    });

    void setUpSource() throws IOException {
        setUpSource(mInputFile);
    }

    public void setUpParams(int limit) {
        int count = 0;
        for (int bitrate : mBitrates) {
            if (mIsAudio) {
                for (int rate : mEncParamList1) {
                    for (int channels : mEncParamList2) {
                        MediaFormat format = new MediaFormat();
                        format.setString(MediaFormat.KEY_MIME, mMime);
                        if (mMime.equals(MediaFormat.MIMETYPE_AUDIO_FLAC)) {
                            format.setInteger(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL, bitrate);
                        } else {
                            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
                        }
                        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, rate);
                        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channels);
                        mFormats.add(format);
                        count++;
                        if (count >= limit) return;
                    }
                }
            } else {
                assertTrue("Wrong number of height, width parameters",
                        mEncParamList1.length == mEncParamList2.length);
                for (int i = 0; i < mEncParamList1.length; i++) {
                    MediaFormat format = new MediaFormat();
                    format.setString(MediaFormat.KEY_MIME, mMime);
                    format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
                    format.setInteger(MediaFormat.KEY_WIDTH, mEncParamList1[i]);
                    format.setInteger(MediaFormat.KEY_HEIGHT, mEncParamList2[i]);
                    format.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
                    format.setInteger(MediaFormat.KEY_MAX_B_FRAMES, mMaxBFrames);
                    format.setFloat(MediaFormat.KEY_I_FRAME_INTERVAL, 1.0f);
                    format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                    mFormats.add(format);
                    count++;
                    if (count >= limit) return;
                }
            }
        }
    }

    private void checkErrorAndVerifyPTS(String log)  {
        assertTrue(log + " unexpected error", !mAsyncHandle.hasSeenError());
        assertTrue(log + "no input sent", 0 != mInputCount);
        assertTrue(log + "output received", 0 != mOutputCount);
        if (!mIsAudio) {
            assertTrue(log + "input count != output count, act/exp: " + mOutputCount +
                    " / " + mInputCount, mInputCount == mOutputCount);
            assertTrue(
                    log + " input pts list and output pts list are not identical",
                    mOutputBuff.isOutPtsListIdenticalToInpPtsList((mMaxBFrames != 0)));
        } else {
            assertTrue(log + " pts is not strictly increasing",
                    mOutputBuff.isPtsStrictlyIncreasing(mPrevOutputPts));
        }
    }

    private void initParams(MediaFormat format) {
        if (mIsAudio) {
            mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        } else {
            mWidth = format.getInteger(MediaFormat.KEY_WIDTH);
            mHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
        }
    }

    public void encode(String encoder, boolean isAsync, boolean eosType, int frameLimit)
            throws IOException, InterruptedException {
        setUpParams(Integer.MAX_VALUE);
        setUpSource(mInputFile);
        mSaveToMem = false;
        mOutputBuff = new OutputManager();
        assertTrue("codec name act/got: " + mCodec.getName() + '/' + encoder,
                mCodec.getName().equals(encoder));
        assertTrue("error! codec canonical name is null",
                mCodec.getCanonicalName() != null && !mCodec.getCanonicalName().isEmpty());
        for (MediaFormat format : mFormats) {
            initParams(format);
            String log =
                    String.format("format: %s \n codec: %s, file: %s, mode: %s, eos type: %s:: ",
                            format, encoder, mInputFile, (isAsync ? "async" : "sync"),
                            (eosType ? "eos with last frame" : "eos separate"));
            mOutputBuff.reset();
            configureCodec(format, isAsync, eosType, true);
            mCodec.start();
            doWork(frameLimit);
            queueEOS();
            waitForAllOutputs();
            mCodec.stop();
            checkErrorAndVerifyPTS(log);
        }
    }

    public void flushAndEncode(String encoder, boolean isAsync, int frameLimit)
            throws IOException, InterruptedException {
        setUpParams(1);
        setUpSource(mInputFile);
        mSaveToMem = false;
        mOutputBuff = new OutputManager();
        MediaFormat format = mFormats.get(0);
        initParams(format);
        String log = String.format("encoder: %s, input file: %s, mode: %s:: ", encoder,
                mInputFile, (isAsync ? "async" : "sync"));
        configureCodec(format, isAsync, true, true);
        mCodec.start();

        /* test flush in running state before queuing input */
        flushCodec();
        mOutputBuff.reset();
        if (mIsCodecInAsyncMode) mCodec.start();
        doWork(23);
        assertTrue(log + " pts is not strictly increasing",
                mOutputBuff.isPtsStrictlyIncreasing(mPrevOutputPts));

        /* test flush in running state */
        flushCodec();
        mOutputBuff.reset();
        if (mIsCodecInAsyncMode) mCodec.start();
        doWork(frameLimit);
        queueEOS();
        waitForAllOutputs();
        checkErrorAndVerifyPTS(log);

        /* test flush in eos state */
        flushCodec();
        mOutputBuff.reset();
        if (mIsCodecInAsyncMode) mCodec.start();
        doWork(frameLimit);
        queueEOS();
        waitForAllOutputs();
        mCodec.stop();
        checkErrorAndVerifyPTS(log);
    }

    public void testReconfigure(String encoder, boolean isAsync, int frameLimit)
            throws IOException, InterruptedException {
        setUpParams(2);
        setUpSource(mInputFile);
        MediaFormat format = mFormats.get(0);
        initParams(format);
        mSaveToMem = false;
        mOutputBuff = new OutputManager();
        String log = String.format("encoder: %s, input file: %s, mode: %s:: ", encoder,
                mInputFile, (isAsync ? "async" : "sync"));
        configureCodec(format, isAsync, true, true);

        /* test reconfigure in stopped state */
        reConfigureCodec(format, !isAsync, false, true);
        mCodec.start();

        /* test reconfigure in running state before queuing input */
        reConfigureCodec(format, !isAsync, false, true);
        mCodec.start();
        doWork(23);

        /* test reconfigure codec in running state */
        reConfigureCodec(format, isAsync, true, true);
        mCodec.start();
        mOutputBuff.reset();
        doWork(frameLimit);
        queueEOS();
        waitForAllOutputs();
        mCodec.stop();
        checkErrorAndVerifyPTS(log);

        /* test reconfigure codec at eos state */
        reConfigureCodec(format, !isAsync, false, true);
        mCodec.start();
        mOutputBuff.reset();
        doWork(frameLimit);
        queueEOS();
        waitForAllOutputs();
        mCodec.stop();
        checkErrorAndVerifyPTS(log);

        /* test reconfigure codec for new format */
        if (mFormats.size() > 1) {
            format = mFormats.get(1);
            reConfigureCodec(format, isAsync, false, true);
            initParams(format);
            mCodec.start();
            mOutputBuff.reset();
            doWork(frameLimit);
            queueEOS();
            waitForAllOutputs();
            mCodec.stop();
            checkErrorAndVerifyPTS(log);
        }
    }
}
