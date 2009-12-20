/*
** Copyright 2009, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

//#define LOG_NDEBUG 0
#define LOG_TAG "WavPackPlayer"
#include "utils/Log.h"

#include <stdio.h>
#include <assert.h>
#include <limits.h>
#include <unistd.h>
#include <fcntl.h>
#include <sched.h>
#include <sys/types.h>
#include <sys/stat.h>

#include "WavPackPlayer.h"

#ifdef HAVE_GETTID
static pid_t myTid() { return gettid(); }
#else
static pid_t myTid() { return getpid(); }
#endif

static int32_t read_bytes (void *id, void *data, int32_t bcount) {
    return (int32_t) fread (data, 1, bcount, (FILE*) id);
}

static uint32_t get_pos (void *id) {
    return ftell ((FILE*) id);
}

static int set_pos_abs (void *id, uint32_t pos) {
    return fseek ((FILE *)id, pos, SEEK_SET);
}

static int set_pos_rel (void *id, int32_t delta, int mode) {
    return fseek ((FILE *)id, delta, mode);
}

static int push_back_byte (void *id, int c) {
    return ungetc (c, (FILE *)id);
}

static uint32_t get_length (void *id) {
    FILE *file = (FILE *)id;
    struct stat statbuf;

    if (!file || fstat (fileno (file), &statbuf) || !(statbuf.st_mode & S_IFREG))
        return 0;

    return statbuf.st_size;
}

static int can_seek (void *id) {
    FILE *file = (FILE *)id;
    struct stat statbuf;

    return file && !fstat (fileno (file), &statbuf) && (statbuf.st_mode & S_IFREG);
}

static int32_t write_bytes (void *id, void *data, int32_t bcount) {
    return (int32_t) fwrite (data, 1, bcount, (FILE*) id);
}

static WavpackStreamReader freader = {
    read_bytes, get_pos, set_pos_abs, set_pos_rel, push_back_byte, get_length, can_seek,
    write_bytes
};

// ----------------------------------------------------------------------------

namespace android {

// ----------------------------------------------------------------------------

// TODO: Determine appropriate return codes
static status_t ERROR_NOT_OPEN = -1;
static status_t ERROR_OPEN_FAILED = -2;
static status_t ERROR_ALLOCATE_FAILED = -4;
static status_t ERROR_NOT_SUPPORTED = -8;
static status_t ERROR_NOT_READY = -16;
static status_t STATE_INIT = 0;
static status_t STATE_ERROR = 1;
static status_t STATE_OPEN = 2;


WavPackPlayer::WavPackPlayer() :
    mTotalSamples(-1), mCurrentSample(0), mBytesPerSample(-1), mOutputBytesPerSample(-1),
    mChannels(-1), mSampleRate(-1), mAudioBuffer(NULL), 
    mAudioBufferSize(0), mState(STATE_ERROR), mStreamType(AudioSystem::MUSIC),
    mLoop(false), mAndroidLoop(false), mExit(false), mPaused(false),
    mRender(false), mRenderTid(-1)
{
    LOGV("constructor");
}

void WavPackPlayer::onFirstRef()
{
    LOGV("onFirstRef");
    // create playback thread
    Mutex::Autolock l(mMutex);
    createThreadEtc(renderThread, this, "WavPack decoder", ANDROID_PRIORITY_AUDIO);
    mCondition.wait(mMutex);
    if (mRenderTid > 0) {
        LOGV("render thread(%d) started", mRenderTid);
        mState = STATE_INIT;
    }
}

status_t WavPackPlayer::initCheck()
{
    if (mState != STATE_ERROR) return NO_ERROR;
    return ERROR_NOT_READY;
}

WavPackPlayer::~WavPackPlayer() {
    LOGV("WavPackPlayer destructor");
    release();
}

status_t WavPackPlayer::setDataSource(const char* path)
{
    return setdatasource(path, -1, 0, 0x7ffffffffffffffLL); // intentionally less than LONG_MAX
}

status_t WavPackPlayer::setDataSource(int fd, int64_t offset, int64_t length)
{
    return setdatasource(NULL, fd, offset, length);
}

status_t WavPackPlayer::setdatasource(const char *path, int fd, int64_t offset, int64_t length)
{
    LOGV("setDataSource url=%s, fd=%d", path, fd);

    // file still open?
    Mutex::Autolock l(mMutex);
    if (mState == STATE_OPEN) {
        reset_nosync();
    }

    // open file and set paused state
    if (path) {
        mFile = fopen(path, "r");
    } else {
        mFile = fdopen(dup(fd), "r");
    }

    if (mFile == NULL) {
        return ERROR_OPEN_FAILED;
    }

    struct stat sb;
    int ret;
    if (path) {
        ret = stat(path, &sb);
    } else {
        ret = fstat(fd, &sb);
    }

    if (ret != 0) {
        mState = STATE_ERROR;
        fclose(mFile);
        return ERROR_OPEN_FAILED;
    }

    fseek(mFile, offset, SEEK_SET);

    // We attempt to open a correction file if it is in the same directory as the main WavPack file
    // Correction files have the same filename, but have the file extension wvc

    mCorrectionFile = NULL;

    if(path) {    

        char *correctionfilename = (char *)malloc (strlen (path) + 10);
        
        if(correctionfilename) {
            strcpy (correctionfilename, path);
            strcat (correctionfilename, "c");
            mCorrectionFile = fopen (correctionfilename, "rb");
            free (correctionfilename);
        }
    }

    int open_flags;

    open_flags = OPEN_TAGS | OPEN_2CH_MAX | OPEN_NORMALIZE;

    mWpc = WavpackOpenFileInputEx (&freader, mFile, mCorrectionFile, errorBuff, open_flags, 0);

    if (mWpc == NULL) {
        LOGE("failed to allocate decoder: %s\n", errorBuff);
        mState = STATE_ERROR;
        fclose(mFile);
        if(mCorrectionFile != NULL) {
            fclose(mCorrectionFile);
        }
        return ERROR_OPEN_FAILED;
    }

    mTotalSamples = WavpackGetNumSamples(mWpc);
    mBytesPerSample = WavpackGetBytesPerSample(mWpc);
    mChannels = WavpackGetReducedChannels(mWpc);
    mSampleRate = WavpackGetSampleRate(mWpc);
    endWavPackFile = 0;

    if (mBytesPerSample == 3) {
        // we dither 24-bit to 16-bit
        mOutputBytesPerSample = 2;
    } else {
        mOutputBytesPerSample = mBytesPerSample;
    }

    if (mOutputBytesPerSample != 1 && mOutputBytesPerSample != 2) {
        LOGE("Can only support 8 or 16 bits per sample; input is %d\n", mBytesPerSample * 8);
        mState = STATE_ERROR;
        return ERROR_NOT_SUPPORTED;
    }

    mLengthInMsec = mTotalSamples*1000 / mSampleRate;

    // look for the android loop tag  (for ringtones)
    char value[64];

    if (WavpackGetTagItem (mWpc, "android loop", value, sizeof (value))) {
        mAndroidLoop = (strncmp(value, "true", 4) == 0); 
    }
 
    LOGV_IF(mAndroidLoop, "looped sound");
    
    mState = STATE_OPEN;
    return NO_ERROR;
}

status_t WavPackPlayer::prepare()
{
    LOGV("prepare");
    if (mState != STATE_OPEN ) {
        return ERROR_NOT_OPEN;
    }
    return NO_ERROR;
}

status_t WavPackPlayer::prepareAsync() {
    LOGV("prepareAsync");
    // can't hold the lock here because of the callback
    // it's safe because we don't change state
    if (mState != STATE_OPEN) {
        sendEvent(MEDIA_ERROR);
        return NO_ERROR;
    }
    sendEvent(MEDIA_PREPARED);
    return NO_ERROR;
}


status_t WavPackPlayer::start()
{
    LOGV("start\n");
    Mutex::Autolock l(mMutex);
    if (mState != STATE_OPEN) 
    {
        return ERROR_NOT_OPEN;
    }

    mPaused = false;
    mRender = true;

    // wake up render thread
    LOGV("  wakeup render thread\n");
    mCondition.signal();
    return NO_ERROR;
}

status_t WavPackPlayer::stop()
{
    LOGV("stop\n");
    Mutex::Autolock l(mMutex);
    if (mState != STATE_OPEN) 
    {
        return ERROR_NOT_OPEN;
    }
    mPaused = true;
    mRender = false;
    return NO_ERROR;
}

status_t WavPackPlayer::seekTo(int msec)
{
    LOGV("seekTo %d\n", msec);
    Mutex::Autolock l(mMutex);
    if (mState != STATE_OPEN) {
        return ERROR_NOT_OPEN;
    }

    int sr_1000 = mSampleRate / 1000;

    WavpackSeekSample(mWpc, (int)(sr_1000 * msec));
    mCurrentSample = WavpackGetSampleIndex(mWpc);

    sendEvent(MEDIA_SEEK_COMPLETE);
    return NO_ERROR;

}

status_t WavPackPlayer::pause()
{
    LOGV("pause\n");
    Mutex::Autolock l(mMutex);
    if (mState != STATE_OPEN) {
        return ERROR_NOT_OPEN;
    }
    mPaused = true;
    return NO_ERROR;
}

bool WavPackPlayer::isPlaying()
{
    LOGV("isPlaying\n");
    if (mState == STATE_OPEN) {
        return mRender;
    }
    return false;
}

status_t WavPackPlayer::getCurrentPosition(int* msec)
{
    LOGV("getCurrentPosition\n");
    Mutex::Autolock l(mMutex);
    if (mState != STATE_OPEN) {
        LOGE("getCurrentPosition(): file not open");
        return ERROR_NOT_OPEN;
    }

    *msec = (int)(mCurrentSample * 1000 / mSampleRate);
    return NO_ERROR;
}

status_t WavPackPlayer::getDuration(int* duration)
{
    LOGV("getDuration\n");
    if (mState != STATE_OPEN) {
        return ERROR_NOT_OPEN;
    }

    *duration = mLengthInMsec;
    return NO_ERROR;
}

status_t WavPackPlayer::release()
{
    LOGV("release\n");
    Mutex::Autolock l(mMutex);
    reset_nosync();

    // TODO: timeout when thread won't exit
    // wait for render thread to exit
    if (mRenderTid > 0) {
        mExit = true;
        mCondition.signal();
        mCondition.wait(mMutex);
    }
    return NO_ERROR;
}

status_t WavPackPlayer::reset()
{
    LOGV("reset\n");
    Mutex::Autolock l(mMutex);
    return reset_nosync();
}

// always call with lock held
status_t WavPackPlayer::reset_nosync()
{
    if(mWpc != NULL) {
        mWpc = WavpackCloseFile(mWpc);
    }

    // close file
    if (mFile != NULL) {
        fclose(mFile);
        mFile = NULL;
    }
    if(mCorrectionFile != NULL) {
        fclose(mCorrectionFile);
        mCorrectionFile = NULL;
    }

    mState = STATE_ERROR;

    mTotalSamples = -1;
    mBytesPerSample = -1;
    mOutputBytesPerSample = -1;
    mChannels = -1;
    mSampleRate = -1;
    mLoop = false;
    mAndroidLoop = false;
    mPaused = false;
    mRender = false;
    mWpc = NULL;
    return NO_ERROR;
}

status_t WavPackPlayer::setLooping(int loop)
{
    LOGV("setLooping\n");
    Mutex::Autolock l(mMutex);
    mLoop = (loop != 0);
    return NO_ERROR;
}

status_t WavPackPlayer::createOutputTrack() {
    LOGV("Create AudioTrack object: rate=%ld, channels=%d\n",
            mSampleRate, mChannels);
LOGE("********************************************* WavPack - Create AudioTrack object: rate=%ld, channels=%d\n", mSampleRate, mChannels);

    if(mOutputBytesPerSample==1) {
        if (mAudioSink->open(mSampleRate, mChannels, AudioSystem::PCM_8_BIT, DEFAULT_AUDIOSINK_BUFFERCOUNT) != NO_ERROR) {
            LOGE("mAudioSink open failed\n");
            return ERROR_OPEN_FAILED;
        }
    } else {
        if (mAudioSink->open(mSampleRate, mChannels, AudioSystem::PCM_16_BIT, DEFAULT_AUDIOSINK_BUFFERCOUNT) != NO_ERROR) {
            LOGE("mAudioSink open failed\n");
            return ERROR_OPEN_FAILED;
        }
    }

    return NO_ERROR;
}

static uint32_t lcg_rand (uint32_t a)
{
    return (a * 16807) % 2147483647;
}

typedef struct {
    int error[3];
    int random;
} dither_state;

static int convert24to16(int sample, dither_state *dither)
{
    unsigned scale = 8;
    int output, mask, random;
    const int MIN = -8388608; 
    const int MAX = 8388607;	// this is 2 to the power of 23 minus 1 (24 bit audio)

    mask = 0xff;

    /* noise shaping */
    sample += dither->error[0] - dither->error[1] + dither->error[2];

    dither->error[2] = dither->error[1];
    dither->error[1] = dither->error[0] / 2;

    /* dither using Park–Miller random number generator */
    output = sample+128;
    random = (int)lcg_rand(dither->random);
    output += (random & mask) - (dither->random & mask);

    dither->random = random;

    /* clip */
    if(output > MAX) {
        output = MAX;

        if(sample > MAX)
            sample = MAX;
    } else if(output < MIN) {
        output = MIN;

        if(sample < MIN)
            sample = MIN;
    }

    output &= ~mask;

    /* error feedback */
    dither->error[0] = sample - output;

    /* scale */
    return output >> scale;
}

static unsigned char *format_samples (int bps, unsigned char *dst, int32_t *src, uint32_t samcnt, uint32_t channels)
{
    int channel = 0;
    static dither_state dither[2];	/* we only support mono or stereo */

    int32_t temp;

    switch (bps) {

        case 1:
            while (samcnt--)
                *dst++ = *src++ + 128;
            break;

        case 2:
            while (samcnt--) {
                *dst++ = (unsigned char)(temp = *src++);
                *dst++ = (unsigned char)(temp >> 8);
            }
            break;

        case 3:
	    while (samcnt--) {
		// We dither 24bit to 16bit
		
		temp = convert24to16(*src++, &dither[channel]);

		*dst++ = (unsigned char)(temp);
		*dst++ = (unsigned char)(temp >> 8);

		if(channels>1) {
			if(channel==0) {	
				channel=1;
			} else {
				channel=0;
			}
		}
	    }

	    break;

        case 4:
            while (samcnt--) {
                *dst++ = (unsigned char)(temp = *src++);
                *dst++ = (unsigned char)(temp >> 8);
                *dst++ = (unsigned char)(temp >> 16);
                *dst++ = (unsigned char)(temp >> 24);
            }
            break;
    }

    return dst;
}

int WavPackPlayer::decodeWavPack()
{
    int32_t internal_temp_buffer[256];
    unsigned char pDecodedBuffer[1024];	// 4 times space of temp_buffer
    mCurrentSample = WavpackGetSampleIndex(mWpc);
    long samples_unpacked = WavpackUnpackSamples(mWpc,internal_temp_buffer,256/mChannels);
    if(samples_unpacked) {

        format_samples(mBytesPerSample,(unsigned char*) pDecodedBuffer, internal_temp_buffer, samples_unpacked*mChannels, mChannels);
        if (!mAudioSink->ready()) {
            LOGE("wavpack - audio sink not ready");
            if (createOutputTrack() != NO_ERROR)
                return(1);
        }

        if (!mAudioSink->write(pDecodedBuffer, samples_unpacked*mChannels*mOutputBytesPerSample)) {
            LOGE("Error in WavPack decoder: writing decoded data to stream\n");
        }
    } else {
        endWavPackFile = 1;  // end of file
    }
    return(0);
}

int WavPackPlayer::renderThread(void* p) {
    return ((WavPackPlayer*)p)->render();
}

int WavPackPlayer::render() {
    int result = -1;
    int temp;
    int current_section = 0;
    bool audioStarted = false;

    LOGV("render\n");

    // let main thread know we're ready
    {
        Mutex::Autolock l(mMutex);
        mRenderTid = myTid();
        mCondition.signal();
    }

    while (1) {
        {
            Mutex::Autolock l(mMutex);

            // pausing?
            if (mPaused) {
                if (mAudioSink->ready()) mAudioSink->pause();
                mRender = false;
                audioStarted = false;
            }

            // nothing to render, wait for client thread to wake us up
            if (!mExit && !mRender) {
                LOGV("render - signal wait\n");
                mCondition.wait(mMutex);
                LOGV("render - signal rx'd\n");
            }
            if (mExit) break;

            // We could end up here if start() is called, and before we get a
            // chance to run, the app calls stop() or reset(). Re-check render
            // flag so we don't try to render in stop or reset state.
            if (!mRender) continue;

            // create audio output track if necessary
            if (!mAudioSink->ready()) {
                LOGV("render - create output track\n");

                if (createOutputTrack() != NO_ERROR)
                    break;
            }


            // start audio output if necessary
            if (!audioStarted && !mPaused && !mExit) {
                LOGV("render - starting audio\n");
                mAudioSink->start();
                audioStarted = true;
            }

            if (endWavPackFile != 1) {
                decodeWavPack();
            } else {
                // end of file, do we need to loop?
                // ...
                if (mLoop || mAndroidLoop) {

                    // handle loop here
                    WavpackSeekSample(mWpc,0); // seek to start of file

                    mCurrentSample = WavpackGetSampleIndex(mWpc);
                    endWavPackFile = 0; // no longer at end of WavPack file
                    decodeWavPack();
                } else {
                    mAudioSink->stop();
                    audioStarted = false;
                    mRender = false;
                    mPaused = true;

                    LOGV("send MEDIA_PLAYBACK_COMPLETE\n");
                    sendEvent(MEDIA_PLAYBACK_COMPLETE);

                    // wait until we're started again
                    LOGV("playback complete - wait for signal\n");
                    mCondition.wait(mMutex);
                    LOGV("playback complete - signal rx'd\n");
                    
                    if (mExit) break;

                    // if we're still at the end, restart from the beginning
                    if (mState == STATE_OPEN) {
                                        
                        WavpackSeekSample(mWpc,0); // seek to start of file
                        
                        mCurrentSample = WavpackGetSampleIndex(mWpc);

                        endWavPackFile = 0;	// no longer at end of WavPack file 
                        decodeWavPack();
                    }
                }
            }
        }

    }

threadExit:
    mAudioSink.clear();
    if (mAudioBuffer != NULL) {
        delete [] mAudioBuffer;
        mAudioBuffer = NULL;
        mAudioBufferSize = 0;
    }

    // tell main thread goodbye
    Mutex::Autolock l(mMutex);
    mRenderTid = -1;
    mCondition.signal();
    return result;
}

} // end namespace android
