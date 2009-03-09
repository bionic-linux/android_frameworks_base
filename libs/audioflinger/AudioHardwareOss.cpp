/*
 * Copyright (C) 2008 The Android Open Source Project
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

#include <stdint.h>
#include <sys/types.h>

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sched.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/soundcard.h>

#define LOG_TAG "AudioHardware"
//#define LOG_NDEBUG 0

#include <utils/Log.h>
#include <utils/String8.h>

#include "AudioHardwareOss.h"

namespace android {

// ----------------------------------------------------------------------------

static char const * const kAudioDeviceName = "/dev/snd/dsp";

// ----------------------------------------------------------------------------

AudioHardwareOss::AudioHardwareOss()
    : mOutput(0), mInput(0),  mFd(-1), mMicMute(false)
{
    mFd = ::open(kAudioDeviceName, O_RDWR);

    LOGD("Open audio ...\n");
    if (mFd < 0) {
        LOGD("Open audio device %s is failed! \n", kAudioDeviceName);
        return;
    }

    LOGD("Audio device is ready...\n");
    return;
}

AudioHardwareOss::~AudioHardwareOss()
{
    if (mFd >= 0) ::close(mFd);
    delete mOutput;
    delete mInput;
}

status_t AudioHardwareOss::initCheck()
{
    LOGD("initcheck\n");

    if (mFd >= 0) {
        if (::access(kAudioDeviceName, O_RDWR) == NO_ERROR)
            return NO_ERROR;
    }

    LOGD("initcheck failed\n");
    return NO_INIT;
}

AudioStreamOut* AudioHardwareOss::openOutputStream(
        int format, int channelCount, uint32_t sampleRate, status_t *status)
{
    AutoMutex lock(mLock);

    // only one output stream allowed
    if (mOutput) {
        if (status) {
            *status = INVALID_OPERATION;
        }
        return 0;
    }

    // create new output stream
    AudioStreamOutOss* out = new AudioStreamOutOss();
    status_t lstatus = out->set(this, mFd, format, channelCount, sampleRate);
    if (status) {
        *status = INVALID_OPERATION;
    }

    if (lstatus == NO_ERROR) {
        mOutput = out;
    } else {
        delete out;
    }
    return mOutput;
}

void AudioHardwareOss::closeOutputStream(AudioStreamOutOss* out) {
    if (out == mOutput) mOutput = 0;
}

AudioStreamIn* AudioHardwareOss::openInputStream(
        int format, int channelCount, uint32_t sampleRate, status_t *status)
{
    AutoMutex lock(mLock);

    // only one input stream allowed
    if (mInput) {
        if (status) {
            *status = INVALID_OPERATION;
        }
        return 0;
    }


    // create new output stream
    AudioStreamInOss* in = new AudioStreamInOss();
    status_t lstatus = in->set(this, mFd, format, channelCount, sampleRate);
    if (status) {
        *status = INVALID_OPERATION;
    }

    if (lstatus == NO_ERROR) {
        mInput = in;
    } else {
        delete in;
    }
    return mInput;
}

void AudioHardwareOss::closeInputStream(AudioStreamInOss* in) {
    if (in == mInput) mInput = 0;
}

status_t AudioHardwareOss::setVoiceVolume(float v)
{
    // Implement: set voice volume
    // For OSS API, the volume is from 0 to 100. 
    if (mFd) {
        int volume = int(v * 100);
        if (volume > 100) {
            LOGD("Bad value for voice volume setting. v= %f, should < 100\n", v);
            return BAD_VALUE;
        }

        if (::ioctl(mFd, SOUND_MIXER_WRITE_VOLUME, &volume) == -1) {
            return INVALID_OPERATION;
        }

        LOGD("setVoiceVolume to %f\n", v);
        return NO_ERROR;
    }

    LOGD("Audio device mFd is not opened. Func:%s, Line:%d\n", __FUNCTION__, __LINE__);

    return NO_ERROR;
}

status_t AudioHardwareOss::setMasterVolume(float v)
{
    // Implement: set master volume
    if (mFd) {
        int volume = int(v * 100);
        if (volume > 100) {
            LOGD("Bad value for master volume setting. v= %f, should < 100\n", v);
            return BAD_VALUE;
        }

        if (::ioctl(mFd, SOUND_MIXER_WRITE_VOLUME, &volume) == -1) {
            LOGD("File:%s, Func:%s, Line:%d\n", __FILE__, __FUNCTION__, __LINE__);
            return INVALID_OPERATION;
        }

        LOGD("setMasterVolume to %f\n", v);
        return NO_ERROR;
    }

    LOGD("Audio device mFd is not opened. Func:%s, Line:%d\n", __FUNCTION__, __LINE__);

    // return error - software mixer will handle it
    return INVALID_OPERATION;
}

status_t AudioHardwareOss::setMicMute(bool state)
{
    mMicMute = state;
    return NO_ERROR;
}

status_t AudioHardwareOss::getMicMute(bool* state)
{
    *state = mMicMute;
    return NO_ERROR;
}

status_t AudioHardwareOss::dumpInternals(int fd, const Vector<String16>& args)
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;
    result.append("AudioHardwareOss::dumpInternals\n");
    snprintf(buffer, SIZE, "\tmFd: %d mMicMute: %s\n",  mFd, mMicMute? "true": "false");
    result.append(buffer);
    ::write(fd, result.string(), result.size());
    return NO_ERROR;
}

status_t AudioHardwareOss::dump(int fd, const Vector<String16>& args)
{
    dumpInternals(fd, args);
    if (mInput) {
        mInput->dump(fd, args);
    }
    if (mOutput) {
        mOutput->dump(fd, args);
    }
    return NO_ERROR;
}

// ----------------------------------------------------------------------------

status_t AudioStreamOutOss::set(
        AudioHardwareOss *hw,
        int fd,
        int format,
        int channels,
        uint32_t rate)
{
    // fix up defaults
    if (format == 0) format = AudioSystem::PCM_16_BIT;
    if (channels == 0) channels = channelCount();
    if (rate == 0) rate = sampleRate();

    // check values
    if ((format != AudioSystem::PCM_16_BIT) ||
            (channels != channelCount()) ||
            (rate != sampleRate()))
        return BAD_VALUE;

    mAudioHardware = hw;
    mFd = fd;

    if (mFd < 0) {
        return BAD_VALUE;
    }

    if (::ioctl(mFd, SNDCTL_DSP_RESET, NULL) == -1) {
        LOGD("Reset audio out failed! File:%s, Func:%s, Line:%d\n", __FILE__, __FUNCTION__, __LINE__);
        return BAD_VALUE;
    }

    int value = AFMT_S16_LE;
    switch (format)
    {
        case AudioSystem::PCM_8_BIT:
            value = AFMT_U8; // /dev/dsp
            break;
        case AudioSystem::PCM_16_BIT:
	    default:
            value = AFMT_S16_LE; // /dev/dspW, default setting of Android
            break;
    }

    if (::ioctl(mFd, SNDCTL_DSP_SETFMT, &value) == -1) {
        LOGD("Set audio out format failed! File:%s, Func:%s, Line:%d\n", __FILE__, __FUNCTION__, __LINE__);
        return BAD_VALUE;
    }

    value = channels;
    if (::ioctl(mFd, SNDCTL_DSP_CHANNELS, &value) == -1) {
        LOGD("Set audio out channel count failed! File:%s, Func:%s, Line:%d\n", __FILE__, __FUNCTION__, __LINE__);
        return BAD_VALUE;
    }

    value = rate;
    if (::ioctl(mFd, SNDCTL_DSP_SPEED, &value) == -1) {
        LOGD("Set audio out sample rate failed! File:%s, Func:%s, Line:%d\n", __FILE__, __FUNCTION__, __LINE__);
        return BAD_VALUE;
    }

    LOGD("Audio out setting success! mFd = %d\n", mFd);

    return NO_ERROR;
}

AudioStreamOutOss::~AudioStreamOutOss()
{
    if (mAudioHardware)
        mAudioHardware->closeOutputStream(this);
}

ssize_t AudioStreamOutOss::write(const void* buffer, size_t bytes)
{
    Mutex::Autolock _l(mLock);
    return ssize_t(::write(mFd, buffer, bytes));
}

status_t AudioStreamOutOss::standby()
{
    // Implement: audio hardware to standby mode
    return NO_ERROR;
}

status_t AudioStreamOutOss::dump(int fd, const Vector<String16>& args)
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;
    snprintf(buffer, SIZE, "AudioStreamOutOss::dump\n");
    result.append(buffer);
    snprintf(buffer, SIZE, "\tsample rate: %d\n", sampleRate());
    result.append(buffer);
    snprintf(buffer, SIZE, "\tbuffer size: %d\n", bufferSize());
    result.append(buffer);
    snprintf(buffer, SIZE, "\tchannel count: %d\n", channelCount());
    result.append(buffer);
    snprintf(buffer, SIZE, "\tformat: %d\n", format());
    result.append(buffer);
    snprintf(buffer, SIZE, "\tmAudioHardware: %p\n", mAudioHardware);
    result.append(buffer);
    snprintf(buffer, SIZE, "\tmFd: %d\n", mFd);
    result.append(buffer);
    ::write(fd, result.string(), result.size());
    return NO_ERROR;
}

// ----------------------------------------------------------------------------

// record functions
status_t AudioStreamInOss::set(
        AudioHardwareOss *hw,
        int fd,
        int format,
        int channels,
        uint32_t rate)
{
    // FIXME: remove logging
    LOGD("AudioStreamInOss::set(%p, %d, %d, %d, %u)", hw, fd, format, channels, rate);
    // check values
    if ((format != AudioSystem::PCM_16_BIT) ||
            (channels != channelCount()) ||
            (rate != sampleRate())) {
        LOGE("Error opening input channel");
        return BAD_VALUE;
    }

    mAudioHardware = hw;
    mFd = fd;

    if (mFd < 0) {
        return BAD_VALUE;
    }

    if (::ioctl(mFd, SNDCTL_DSP_RESET, NULL) == -1) {
        LOGD("Reset audio in failed! File:%s, Func:%s, Line:%d\n", __FILE__, __FUNCTION__, __LINE__);
        return BAD_VALUE;
    }

    int value = AFMT_S16_LE;
    switch (format)
    {
        case AudioSystem::PCM_8_BIT:
            value = AFMT_U8; // /dev/dsp
            break;
        case AudioSystem::PCM_16_BIT:
	    default:
            value = AFMT_S16_LE; // /dev/dspW, default setting of Android
            break;
    }

    if (::ioctl(mFd, SNDCTL_DSP_SETFMT, &value) == -1) {
        LOGD("Set audio in format failed! File:%s, Func:%s, Line:%d\n", __FILE__, __FUNCTION__, __LINE__);
        return BAD_VALUE;
    }

    value = channels;
    if (::ioctl(mFd, SNDCTL_DSP_CHANNELS, &value) == -1) {
        LOGD("Set audio in channel count failed! File:%s, Func:%s, Line:%d\n", __FILE__, __FUNCTION__, __LINE__);
        return BAD_VALUE;
    }

    value = rate;
    if (::ioctl(mFd, SNDCTL_DSP_SPEED, &value) == -1) {
        LOGD("Set audio in sample rate failed! File:%s, Func:%s, Line:%d\n", __FILE__, __FUNCTION__, __LINE__);
        return BAD_VALUE;
    }

    LOGD("Audio in setting success! \n");

    return NO_ERROR;
}

AudioStreamInOss::~AudioStreamInOss()
{
    // FIXME: remove logging
    LOGD("AudioStreamInOss destructor");
    if (mAudioHardware)
        mAudioHardware->closeInputStream(this);
}

ssize_t AudioStreamInOss::read(void* buffer, ssize_t bytes)
{
    // FIXME: remove logging
    LOGD("AudioStreamInOss::read(%p, %d) from fd %d", buffer, (int)bytes, mFd);
    AutoMutex lock(mLock);
    if (mFd < 0) {
        LOGE("Attempt to read from unopened device");
        return NO_INIT;
    }
    return ::read(mFd, buffer, bytes);
}

status_t AudioStreamInOss::dump(int fd, const Vector<String16>& args)
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;
    snprintf(buffer, SIZE, "AudioStreamInOss::dump\n");
    result.append(buffer);
    snprintf(buffer, SIZE, "\tsample rate: %d\n", sampleRate());
    result.append(buffer);
    snprintf(buffer, SIZE, "\tbuffer size: %d\n", bufferSize());
    result.append(buffer);
    snprintf(buffer, SIZE, "\tchannel count: %d\n", channelCount());
    result.append(buffer);
    snprintf(buffer, SIZE, "\tformat: %d\n", format());
    result.append(buffer);
    snprintf(buffer, SIZE, "\tmAudioHardware: %p\n", mAudioHardware);
    result.append(buffer);
    snprintf(buffer, SIZE, "\tmFd: %d\n", mFd);
    result.append(buffer);
    ::write(fd, result.string(), result.size());
    return NO_ERROR;
}

// ----------------------------------------------------------------------------

extern "C" AudioHardwareInterface* createAudioHardware(void)
{
    return new AudioHardwareOss();
}

}; // namespace android
