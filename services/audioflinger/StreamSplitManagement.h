/*
** Copyright 2011, The Android Open Source Project
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

#ifndef STREAMSPLIT_MANAGEMENT_H
#define STREAMSPLIT_MANAGEMENT_H

#include <stdint.h>
#include <sys/types.h>
#include <media/AudioSystem.h>
#include <hardware_legacy/AudioHardwareInterface.h>

namespace android {

#define INPUT_DEVICE_MAX_SPLIT_STREAMS 3 // Total number of supported input devices active at the same time
#define READINPUT_BUFFER_SIZE (10*4800)  // Internal buffer is 10 times the defualt read size
#define READINPUT_MAX_CLIENTS 3          // Max number of clients for a specific stream

class OpenInputStreamManagement
{
public:
    OpenInputStreamManagement(uint32_t device);
    ~OpenInputStreamManagement();
    int getNbrOfClients() const;
    AudioStreamIn *getInput(void) const;
    void setInput(AudioStreamIn *input, bool recordClient);
    AudioSystem::audio_input_clients getLatestClientId() const;
    AudioSystem::audio_input_clients getInputClientType(AudioSystem::audio_input_clients inputClientId) const;
    int purgeOpenInputData(AudioSystem::audio_input_clients inputClientId);
    uint32_t getDevice() const;

private:
    struct OpenInputStreamContext_t{
        uint32_t device;
        AudioStreamIn *input;
        int pbClients;
        AudioSystem::audio_input_clients pbClientId[READINPUT_MAX_CLIENTS];
        int recClients;
        AudioSystem::audio_input_clients recClientId[READINPUT_MAX_CLIENTS];
        int totalClients;
    };

    OpenInputStreamContext_t *mOpenInputStreamContext;
};

class ReadInputStreamManagement
{
public:
    ReadInputStreamManagement();
    ~ReadInputStreamManagement();
    bool init(AudioStreamIn *input, AudioSystem::audio_input_clients inputClientId);
    bool setReadClientContext(AudioSystem::audio_input_clients inputClientId);
    uint32_t readFromInput(AudioSystem::audio_input_clients inputClientId, uint8_t *buf, size_t bytes);
    void purgeReadInputData(AudioSystem::audio_input_clients inputClientId, int noOfOpenClients);
    void activateClient(AudioSystem::audio_input_clients inputClientId);
    void deactivateClient(AudioSystem::audio_input_clients inputClientId);
    AudioStreamIn *getInput(void) const;

private:

    struct ClientContext_t{
        AudioSystem::audio_input_clients inputClientId;
        uint8_t *pClientHead;
        bool    active;
    };

    struct ReadInputStreamContext_t{
        AudioStreamIn *input;
        uint8_t *pBufferHead;
        uint8_t *pBufferTail;
        uint8_t *buffer;
        ClientContext_t *pClientContext;
        uint32_t nbrOfActiveClients;
        pthread_t readThread;
        bool threadIsExecuting;
    };

    ReadInputStreamContext_t* mReadInputStreamContext;
    mutable     Mutex                   mLock;

    ClientContext_t *getClientContext(AudioSystem::audio_input_clients inputClientId);
    size_t calculateBufferDistance(uint8_t *pBufferPtr1, uint8_t *pBufferPtr2) const;
    void copyFromInputBuffer(AudioSystem::audio_input_clients inputClientId, uint8_t *buf, size_t bytes);
    uint8_t *getNextClientHeadInSegment(uint8_t *pHead, uint8_t *pTail);
    bool withinBufferSegment(uint8_t *pTest, uint8_t *pHead, uint8_t *pTail) const;
    void writeToInputBuffer(uint8_t *buf, size_t bytes);
    static void *readFromHW(void *args_p);
    void increaseNbrOfActiveClients();
    void decreaseNbrOfActiveClients();
    bool isClientActive(AudioSystem::audio_input_clients inputClientId);
};

class StreamSplitManagement
{
public:
    StreamSplitManagement();
    ~StreamSplitManagement();
    OpenInputStreamManagement *getOrCreateOpenInputStreamManagement(uint32_t device, bool recordClient);
    OpenInputStreamManagement *getOpenInputStreamManagement(AudioStreamIn *input);
    ReadInputStreamManagement *getOrCreateReadInputStreamManagement(AudioStreamIn *input, AudioSystem::audio_input_clients inputClientId);
    ReadInputStreamManagement *getReadInputStreamManagement(AudioStreamIn* input);
    void closeInputContext(AudioStreamIn* input, AudioSystem::audio_input_clients inputClientId);
    void cleanup(AudioStreamIn* input);

private:

    struct SplitStreamHandle_t{
        OpenInputStreamManagement *openInputStreamManagementContext;
        ReadInputStreamManagement *readInputStreamManagementContext;
    };

    SplitStreamHandle_t *mSplitStreamHandle;
};

// ----------------------------------------------------------------------------
}; // namespace android

#endif // STREAMSPLIT_MANAGEMENT_H