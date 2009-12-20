/*
**
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

#ifndef ANDROID_WAVPACKPLAYER_H
#define ANDROID_WAVPACKPLAYER_H

#include <utils/threads.h>

#include <media/MediaPlayerInterface.h>
#include <media/AudioTrack.h>

extern "C" {
#include "wavpack.h"
}

#define ANDROID_WAVPACK_LOOP_TAG "Android Loop"

namespace android {

class WavPackPlayer : public MediaPlayerInterface {
public:
                         WavPackPlayer();
                         ~WavPackPlayer();

    virtual void         onFirstRef();
    virtual status_t     initCheck();
    virtual status_t     setDataSource(const char* path);
    virtual status_t     setDataSource(int fd, int64_t offset, int64_t length);
    virtual status_t     setVideoSurface(const sp<ISurface>& surface) { return UNKNOWN_ERROR; }
    virtual status_t     prepare();
    virtual status_t     prepareAsync();
    virtual status_t     start();
    virtual status_t     stop();
    virtual status_t     seekTo(int msec);
    virtual status_t     pause();
    virtual bool         isPlaying();
    virtual status_t     getCurrentPosition(int* msec);
    virtual status_t     getDuration(int* msec);
    virtual status_t     release();
    virtual status_t     reset();
    virtual status_t     setLooping(int loop);
    virtual player_type  playerType() { return WAVPACK_PLAYER; }
    virtual status_t    invoke(const Parcel& request, Parcel *reply) {return INVALID_OPERATION;}

private:
            status_t     setdatasource(const char *path, int fd, int64_t offset, int64_t length);
            status_t     reset_nosync();
            status_t     createOutputTrack();
    static  int          renderThread(void*);
            int          render();
            int          decodeWavPack();
     
    int64_t              mTotalSamples;
    int64_t              mCurrentSample;
    uint32_t             mBytesPerSample;
    uint32_t             mOutputBytesPerSample;
    uint32_t             mChannels;
    uint32_t             mSampleRate;
    uint32_t             mLengthInMsec;

    int                  endWavPackFile;

    int8_t *             mAudioBuffer;
    uint32_t             mAudioBufferSize;

    Mutex                mMutex;
    Condition            mCondition;
    FILE*                mFile;
    FILE*                mCorrectionFile;
    char                 errorBuff[128];
    WavpackContext      *mWpc;
    status_t             mState;
    int                  mStreamType;
    bool                 mLoop;
    bool                 mAndroidLoop;
    volatile bool        mExit;
    bool                 mPaused;
    volatile bool        mRender;
    pid_t                mRenderTid;
};

}; // namespace android

#endif // ANDROID_WAVPACKPLAYER_H

