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

#define LOG_TAG "StreamSplitManagement"

#include <utils/Log.h>

#include "StreamSplitManagement.h"

namespace android {

//---------- class OpenInputStreamManagement ------------------------

OpenInputStreamManagement::OpenInputStreamManagement(uint32_t device)
{
    mOpenInputStreamContext = new OpenInputStreamContext_t;
    if (mOpenInputStreamContext != NULL) {
        memset(mOpenInputStreamContext, 0, sizeof(OpenInputStreamContext_t));
        mOpenInputStreamContext->device = device;
    }
}

OpenInputStreamManagement::~OpenInputStreamManagement()
{
    if (mOpenInputStreamContext != NULL) {
        delete mOpenInputStreamContext;
    }
}

int OpenInputStreamManagement::getNbrOfClients() const
{
    if (mOpenInputStreamContext == NULL) {
        return 0;
    }
    return mOpenInputStreamContext->totalClients;
}

AudioStreamIn* OpenInputStreamManagement::getInput(void) const
{
    if (mOpenInputStreamContext == NULL) {
        return NULL;
    }
    return mOpenInputStreamContext->input;
}

void OpenInputStreamManagement::setInput(AudioStreamIn *input, bool recordClient)
{
    if (mOpenInputStreamContext == NULL) {
        return;
    }
    if (mOpenInputStreamContext->input == 0) {
        mOpenInputStreamContext->input = input;
    }

    if (mOpenInputStreamContext->input == input) {
        mOpenInputStreamContext->totalClients++;
        if (recordClient) {
            mOpenInputStreamContext->recClientId[mOpenInputStreamContext->recClients] =
                (AudioSystem::audio_input_clients) mOpenInputStreamContext->totalClients;
            mOpenInputStreamContext->recClients++;
        } else {
            mOpenInputStreamContext->pbClientId[mOpenInputStreamContext->pbClients] =
                (AudioSystem::audio_input_clients) mOpenInputStreamContext->totalClients;
            mOpenInputStreamContext->pbClients++;
        }
    }
}

AudioSystem::audio_input_clients OpenInputStreamManagement::getLatestClientId() const
{
    if (mOpenInputStreamContext == NULL) {
        return (AudioSystem::audio_input_clients) 0;
    }
    return (AudioSystem::audio_input_clients) mOpenInputStreamContext->totalClients;
}

uint32_t OpenInputStreamManagement::getDevice() const
{
    if (mOpenInputStreamContext == NULL) {
        return 0;
    }
    return mOpenInputStreamContext->device;
}

AudioSystem::audio_input_clients OpenInputStreamManagement::getInputClientType(
    AudioSystem::audio_input_clients inputClientId) const
{
    if (mOpenInputStreamContext == NULL) {
        return (AudioSystem::audio_input_clients) 0;
    }

    for (int i = 0; i < READINPUT_MAX_CLIENTS; i++) {
        if (mOpenInputStreamContext->pbClientId[i] == inputClientId) {
            return AudioSystem::AUDIO_INPUT_CLIENT_PLAYBACK;
        } else if (mOpenInputStreamContext->recClientId[i] == inputClientId) {
            return AudioSystem::AUDIO_INPUT_CLIENT_RECORD;
        }
    }
    return (AudioSystem::audio_input_clients) 0;
}

int OpenInputStreamManagement::purgeOpenInputData(AudioSystem::audio_input_clients inputClientId)
{
    if (mOpenInputStreamContext == NULL) {
        return 0;
    }

    mOpenInputStreamContext->totalClients--;
    if (mOpenInputStreamContext->totalClients == 0) {
        return 0;
    }

    for (int i= 0; i < READINPUT_MAX_CLIENTS; i++) {
        if (mOpenInputStreamContext->pbClientId[i] == inputClientId) {
            mOpenInputStreamContext->pbClientId[i] = (AudioSystem::audio_input_clients) 0;
            mOpenInputStreamContext->pbClients--;
            break;
        } else if (mOpenInputStreamContext->recClientId[i] == inputClientId) {
            mOpenInputStreamContext->recClientId[i] = (AudioSystem::audio_input_clients) 0;
            mOpenInputStreamContext->recClients--;
            break;
        }
    }
    return mOpenInputStreamContext->totalClients;
}

//---------- class ReadInputStreamManagement ------------------------

ReadInputStreamManagement::ReadInputStreamManagement() {}

ReadInputStreamManagement::~ReadInputStreamManagement()
{
    if (mReadInputStreamContext != NULL) {
        delete mReadInputStreamContext;
    }
}

bool ReadInputStreamManagement::init(AudioStreamIn *input,
                                     AudioSystem::audio_input_clients inputClientId)
{
    mReadInputStreamContext = new ReadInputStreamContext_t;
    if (mReadInputStreamContext == NULL) {
        return false;
    }
    memset(mReadInputStreamContext, 0, sizeof(ReadInputStreamContext_t));

    mReadInputStreamContext->buffer = new uint8_t[READINPUT_BUFFER_SIZE];
    if (mReadInputStreamContext->buffer == NULL) {
        return false;
    }
    memset(mReadInputStreamContext->buffer,0,READINPUT_BUFFER_SIZE);

    mReadInputStreamContext->pClientContext = new ClientContext_t[READINPUT_MAX_CLIENTS];
    if (mReadInputStreamContext->pClientContext == NULL) {
        return false;
    }
    memset(mReadInputStreamContext->pClientContext,0,sizeof(ClientContext_t)*READINPUT_MAX_CLIENTS);

    mReadInputStreamContext->input = input;
    mReadInputStreamContext->pBufferHead = mReadInputStreamContext->buffer;
    mReadInputStreamContext->pBufferTail = mReadInputStreamContext->buffer;
    mReadInputStreamContext->pClientContext[0].inputClientId = inputClientId;
    mReadInputStreamContext->pClientContext[0].pClientHead = mReadInputStreamContext->pBufferHead;
    mReadInputStreamContext->threadIsExecuting = false;
    return true;
}

bool ReadInputStreamManagement::setReadClientContext(AudioSystem::audio_input_clients inputClientId)
{
    if (mReadInputStreamContext == NULL) {
        return false;
    }

    int i;
    for (i = 0; i < READINPUT_MAX_CLIENTS; i++) {
        if (mReadInputStreamContext->pClientContext[i].inputClientId == inputClientId) {
            return true;
        }
    }

    for (i = 0; i < READINPUT_MAX_CLIENTS; i++) {
        if (mReadInputStreamContext->pClientContext[i].inputClientId == 0) {
            mReadInputStreamContext->pClientContext[i].inputClientId = inputClientId;
            mReadInputStreamContext->pClientContext[i].pClientHead =
                mReadInputStreamContext->pBufferTail;
            return true;
        }
    }
    return false;
}

uint32_t ReadInputStreamManagement::readFromInput(AudioSystem::audio_input_clients inputClientId,
                                                  uint8_t *buf,
                                                  size_t bytes)
{
    if (mReadInputStreamContext == NULL) {
        return 0;
    }

    mLock.lock();
    if (!isClientActive((AudioSystem::audio_input_clients) inputClientId)) {
        activateClient((AudioSystem::audio_input_clients) inputClientId);
    }

    if (bytes > READINPUT_BUFFER_SIZE) {
        bytes = READINPUT_BUFFER_SIZE;
    }

    uint32_t copyBytes = 0;

    ClientContext_t *pThisClientContext = getClientContext(inputClientId);
    if (mReadInputStreamContext->nbrOfActiveClients < 2 &&
            !mReadInputStreamContext->threadIsExecuting) {
        copyBytes = mReadInputStreamContext->input->read(buf, bytes);
        mLock.unlock();
        return copyBytes;
    }

    if (!mReadInputStreamContext->threadIsExecuting) {
        if (pthread_create(&mReadInputStreamContext->readThread,
                           NULL,
                           readFromHW,
                           (void*) this) != 0) {
            LOGE("pthread_create failed\n");
            return 0;
        }
        mReadInputStreamContext->threadIsExecuting = true;
    }
    uint32_t clientDataInBuffer = 0;
    uint32_t nrOfTries = 0;

    if (pThisClientContext == NULL) {
        LOGW("ClientContext is NULL");
        mLock.unlock();
        return 0;
    }

    clientDataInBuffer = calculateBufferDistance(pThisClientContext->pClientHead,
                                                 mReadInputStreamContext->pBufferTail);

    while (clientDataInBuffer == 0 && nrOfTries < 100) {
        mLock.unlock();
        usleep(1000);
        mLock.lock();
        clientDataInBuffer = calculateBufferDistance(pThisClientContext->pClientHead,
                                                     mReadInputStreamContext->pBufferTail);
        nrOfTries++;
    }

    if (clientDataInBuffer == 0) {
        LOGW("No data available in buffer");
        mLock.unlock();
        return 0;
    }

    copyBytes = clientDataInBuffer;

    if (bytes < clientDataInBuffer) {
        copyBytes = bytes;
    }

    copyFromInputBuffer(inputClientId, buf, copyBytes);
    mLock.unlock();
    return copyBytes;
}

void ReadInputStreamManagement::purgeReadInputData(AudioSystem::audio_input_clients inputClientId,
                                                   int noOfOpenClients)
{
    if (mReadInputStreamContext == NULL) {
        return;
    }

    if (noOfOpenClients == 0) {
        delete [] mReadInputStreamContext->pClientContext;
        mReadInputStreamContext->pClientContext = NULL;
        delete [] mReadInputStreamContext->buffer;
        mReadInputStreamContext->buffer = NULL;
    } else {
        for (int i = 0 ; i < READINPUT_MAX_CLIENTS ; i++) {
            if (mReadInputStreamContext->pClientContext[i].inputClientId == inputClientId) {
                mReadInputStreamContext->pClientContext[i].inputClientId =
                    (AudioSystem::audio_input_clients) 0;
                mReadInputStreamContext->pClientContext[i].pClientHead = NULL;
            }
        }
    }
}

ReadInputStreamManagement::ClientContext_t *ReadInputStreamManagement::getClientContext(
    AudioSystem::audio_input_clients inputClientId)
{
    if (mReadInputStreamContext == NULL) {
        return NULL;
    }

    for (int i = 0; i < READINPUT_MAX_CLIENTS; i++) {
        if (mReadInputStreamContext->pClientContext[i].inputClientId == inputClientId) {
            return &(mReadInputStreamContext->pClientContext[i]);
        }
    }
    return NULL;
}

size_t ReadInputStreamManagement::calculateBufferDistance(uint8_t *pBufferPtr1,
                                                          uint8_t *pBufferPtr2) const
{
    if (pBufferPtr1 <= pBufferPtr2) {
        return pBufferPtr2 - pBufferPtr1;
    }
    return READINPUT_BUFFER_SIZE - (pBufferPtr1 - pBufferPtr2);
}

//The calling function needs to check that enough data in the buffer for the specific client exist
void ReadInputStreamManagement::copyFromInputBuffer(AudioSystem::audio_input_clients inputClientId,
                                                    uint8_t *buf, size_t bytes)
{
    uint32_t clientHeadOffset;
    bool updateBufferHead = false;

    ClientContext_t *pThisClientContext = getClientContext(inputClientId);
    if (pThisClientContext == NULL || mReadInputStreamContext == NULL) {
        return;
    }
    clientHeadOffset = pThisClientContext->pClientHead - mReadInputStreamContext->buffer;

    uint32_t nbrOfBytesToBufferBoundary = READINPUT_BUFFER_SIZE - clientHeadOffset;

    if (nbrOfBytesToBufferBoundary >= bytes) {
        memcpy(buf, mReadInputStreamContext->buffer + clientHeadOffset, bytes);
    } else {
        memcpy(buf,
               mReadInputStreamContext->buffer + clientHeadOffset,
               nbrOfBytesToBufferBoundary);
        memcpy(buf + nbrOfBytesToBufferBoundary,
               mReadInputStreamContext->buffer,
               bytes - nbrOfBytesToBufferBoundary);
    }

    if (pThisClientContext->pClientHead == mReadInputStreamContext->pBufferHead) {
        updateBufferHead = true;
    }

    pThisClientContext->pClientHead =
        mReadInputStreamContext->buffer + ((clientHeadOffset + bytes) % READINPUT_BUFFER_SIZE);

    if (updateBufferHead) {
        mReadInputStreamContext->pBufferHead =
            getNextClientHeadInSegment(mReadInputStreamContext->pBufferHead,
                                       pThisClientContext->pClientHead);
    }

}

uint8_t *ReadInputStreamManagement::getNextClientHeadInSegment(uint8_t *pHead, uint8_t *pTail)
{
    uint32_t minDistance = READINPUT_BUFFER_SIZE;
    uint32_t bufferDistance;
    uint8_t* newHead = pTail;
    ClientContext_t pClientContext;

    if (mReadInputStreamContext == NULL) {
        return NULL;
    }

    for (int i = 0; i < READINPUT_MAX_CLIENTS; i++) {
        if (withinBufferSegment(mReadInputStreamContext->
                pClientContext[i].pClientHead,pHead,pTail)) {
            bufferDistance =
                calculateBufferDistance(mReadInputStreamContext->pBufferHead,
                                        mReadInputStreamContext->pClientContext[i].pClientHead);
            if (bufferDistance < minDistance) {
                minDistance = bufferDistance;
                newHead = mReadInputStreamContext->pClientContext[i].pClientHead;
            }
        }
    }
    return newHead;
}

bool ReadInputStreamManagement::withinBufferSegment(uint8_t *pTest, uint8_t *pHead, uint8_t *pTail) const
{
    if (pHead <= pTail) {
          return pTest >= pHead && pTest <= pTail;
    } else {
        return !(pTest >= pTail && pTest <= pHead);
    }
}

void ReadInputStreamManagement::writeToInputBuffer(uint8_t *buf, size_t bytes)
{
    mLock.lock();

    if (mReadInputStreamContext == NULL) {
        return;
    }
    uint32_t tailOffset = mReadInputStreamContext->pBufferTail - mReadInputStreamContext->buffer;
    uint32_t nbrOfBytesToBufferBoundary = READINPUT_BUFFER_SIZE - tailOffset;

    if (nbrOfBytesToBufferBoundary >= bytes) {
        memcpy (mReadInputStreamContext->buffer + tailOffset, buf, bytes);
    } else {
        memcpy(mReadInputStreamContext->buffer + tailOffset, buf, nbrOfBytesToBufferBoundary);
        memcpy(mReadInputStreamContext->buffer,
               buf + nbrOfBytesToBufferBoundary,
               bytes - nbrOfBytesToBufferBoundary);
    }

    mReadInputStreamContext->pBufferTail =
        mReadInputStreamContext->buffer + ((tailOffset + bytes) % READINPUT_BUFFER_SIZE);
    mLock.unlock();
}

void *ReadInputStreamManagement::readFromHW(void *args_p)
{
    uint32_t bytesRead;
    ReadInputStreamManagement *ReadInputStreamManagement_p =
        reinterpret_cast<ReadInputStreamManagement *>(args_p);
    uint32_t bufferSize = ReadInputStreamManagement_p->mReadInputStreamContext->input->bufferSize();
    uint8_t buffer[bufferSize];
    uint32_t sampleRate = ReadInputStreamManagement_p->mReadInputStreamContext->input->sampleRate();
    uint32_t frameSize = ReadInputStreamManagement_p->mReadInputStreamContext->input->frameSize();
    uint32_t timeToSleep;

    while (ReadInputStreamManagement_p->mReadInputStreamContext->nbrOfActiveClients > 0) {
        bytesRead =
            ReadInputStreamManagement_p->mReadInputStreamContext->input->read((uint8_t*) buffer,
                                                                              bufferSize);
        if (bytesRead > 0) {
            ReadInputStreamManagement_p->writeToInputBuffer(buffer, bytesRead);
            //Wait 75% of the read data size in time before reading once again.
            timeToSleep = ((bytesRead * 1000000.0 ) / (sampleRate * frameSize)) * 0.75;
            usleep(timeToSleep);
        } else {
            //No data available in input buffer. Waiting 50% instead.
            timeToSleep = ((bufferSize * 1000000.0 ) / (sampleRate * frameSize)) * 0.5;
            usleep(timeToSleep);
        }
    }
    return NULL;
}

void ReadInputStreamManagement::increaseNbrOfActiveClients()
{
    if (mReadInputStreamContext == NULL) {
        return;
    }
    mReadInputStreamContext->nbrOfActiveClients++;
}

void ReadInputStreamManagement::decreaseNbrOfActiveClients()
{
    if (mReadInputStreamContext == NULL) {
        return;
    }
    mReadInputStreamContext->nbrOfActiveClients--;
}

bool ReadInputStreamManagement::isClientActive(AudioSystem::audio_input_clients inputClientId)
{
    ClientContext_t* clientContext = getClientContext(inputClientId);
    if (clientContext != NULL) {
        return clientContext->active;
    } else {
        return false;
    }
}

void ReadInputStreamManagement::activateClient(AudioSystem::audio_input_clients inputClientId)
{
    ClientContext_t* clientContext = getClientContext(inputClientId);
    if (clientContext != NULL) {
        clientContext->active = true;
        increaseNbrOfActiveClients();
    }
}

void ReadInputStreamManagement::deactivateClient(AudioSystem::audio_input_clients inputClientId)
{
    ClientContext_t* clientContext = getClientContext(inputClientId);
    if (clientContext != NULL) {
        clientContext->active = false;
        decreaseNbrOfActiveClients();
        if (mReadInputStreamContext != NULL && mReadInputStreamContext->nbrOfActiveClients == 0) {
            pthread_join(mReadInputStreamContext->readThread, NULL);
        }
    }
}

AudioStreamIn *ReadInputStreamManagement::getInput(void) const
{
    if (mReadInputStreamContext == NULL) {
        return NULL;
    }
    return mReadInputStreamContext->input;
}

//---------- class StreamSplitManagement ----------------------------

StreamSplitManagement::StreamSplitManagement()
{
    mSplitStreamHandle = new SplitStreamHandle_t[INPUT_DEVICE_MAX_SPLIT_STREAMS];
    if (mSplitStreamHandle != NULL) {
        memset(mSplitStreamHandle, 0, sizeof(SplitStreamHandle_t)*INPUT_DEVICE_MAX_SPLIT_STREAMS);
    }
}

StreamSplitManagement::~StreamSplitManagement()
{
    if (mSplitStreamHandle != NULL) {
        delete [] mSplitStreamHandle;
    }
}

OpenInputStreamManagement *StreamSplitManagement::getOrCreateOpenInputStreamManagement(
    uint32_t device,
    bool recordClient)
{
    if (mSplitStreamHandle == NULL) {
        return NULL;
    }
    int i;
    //Get the OpenInputStreamManagementContext for the device
    for (i = 0; i < INPUT_DEVICE_MAX_SPLIT_STREAMS; i++) {
        if (mSplitStreamHandle[i].openInputStreamManagementContext != NULL) {
            if (mSplitStreamHandle[i].openInputStreamManagementContext->getDevice() == device) {
                return mSplitStreamHandle[i].openInputStreamManagementContext;
            }
        } else {
            break;
        }
    }
    //Create OpenInputStreamManagementContext for the device
    if (i < INPUT_DEVICE_MAX_SPLIT_STREAMS) {
        mSplitStreamHandle[i].openInputStreamManagementContext =
            new OpenInputStreamManagement(device);
        if (mSplitStreamHandle[i].openInputStreamManagementContext != NULL) {
            return mSplitStreamHandle[i].openInputStreamManagementContext;
        }
    }
    return NULL;
}

OpenInputStreamManagement *StreamSplitManagement::getOpenInputStreamManagement(AudioStreamIn *input)
{
    if (mSplitStreamHandle == NULL) {
        return NULL;
    }
    for (int i = 0; i < INPUT_DEVICE_MAX_SPLIT_STREAMS; i++) {
        if (mSplitStreamHandle[i].openInputStreamManagementContext != NULL &&
                mSplitStreamHandle[i].openInputStreamManagementContext->getInput() == input) {
            return mSplitStreamHandle[i].openInputStreamManagementContext;
        }
    }
    return NULL;
}

ReadInputStreamManagement *StreamSplitManagement::getOrCreateReadInputStreamManagement(
    AudioStreamIn *input,
    AudioSystem::audio_input_clients inputClientId)
{
    if (mSplitStreamHandle == NULL) {
        return NULL;
    }
    for (int i = 0; i < INPUT_DEVICE_MAX_SPLIT_STREAMS; i++) {
        if (mSplitStreamHandle[i].openInputStreamManagementContext != NULL &&
                mSplitStreamHandle[i].openInputStreamManagementContext->getInput() == input) {
            if (mSplitStreamHandle[i].readInputStreamManagementContext != NULL) {
                if (!mSplitStreamHandle[i].
                        readInputStreamManagementContext->setReadClientContext(inputClientId)) {
                    return NULL;
                }
                return mSplitStreamHandle[i].readInputStreamManagementContext;
            } else {
                mSplitStreamHandle[i].readInputStreamManagementContext = new ReadInputStreamManagement();
                ReadInputStreamManagement* readInputStreamManagementContext =
                    mSplitStreamHandle[i].readInputStreamManagementContext;
                if (mSplitStreamHandle[i].readInputStreamManagementContext != NULL) {
                    if (!readInputStreamManagementContext->init(input, inputClientId)) {
                        delete mSplitStreamHandle[i].readInputStreamManagementContext;
                        mSplitStreamHandle[i].readInputStreamManagementContext = NULL;
                    }
                    return mSplitStreamHandle[i].readInputStreamManagementContext;
                }
            }
        }
    }
    return NULL;
}

ReadInputStreamManagement *StreamSplitManagement::getReadInputStreamManagement(AudioStreamIn* input)
{
    if (mSplitStreamHandle == NULL) {
        return NULL;
    }
    for (int i = 0; i < INPUT_DEVICE_MAX_SPLIT_STREAMS; i++) {
        if (mSplitStreamHandle[i].openInputStreamManagementContext != NULL &&
                mSplitStreamHandle[i].openInputStreamManagementContext->getInput() == input &&
                mSplitStreamHandle[i].readInputStreamManagementContext != NULL) {
            return mSplitStreamHandle[i].readInputStreamManagementContext;
        }
    }
    return NULL;
}

void StreamSplitManagement::closeInputContext(AudioStreamIn* input,
                                              AudioSystem::audio_input_clients inputClientId)
{
    if (mSplitStreamHandle == NULL) {
        return;
    }
    OpenInputStreamManagement *pOpenInputStreamManagementContext =
        getOpenInputStreamManagement(input);
    if (pOpenInputStreamManagementContext == NULL) {
        return;
    }
    int noOfOpenClients = pOpenInputStreamManagementContext->purgeOpenInputData(inputClientId);
    ReadInputStreamManagement *pReadInputStreamManagementContext =
        getReadInputStreamManagement(input);
    if (pReadInputStreamManagementContext != NULL) {
        pReadInputStreamManagementContext->purgeReadInputData(inputClientId, noOfOpenClients);
    }
    if (noOfOpenClients == 0) {
        cleanup((AudioStreamIn*) input);
    }
}

void StreamSplitManagement::cleanup(AudioStreamIn* input)
{
    if (mSplitStreamHandle == NULL) {
        return;
    }
    for (int i = 0; i < INPUT_DEVICE_MAX_SPLIT_STREAMS; i++) {
        if (mSplitStreamHandle[i].openInputStreamManagementContext != NULL &&
                mSplitStreamHandle[i].openInputStreamManagementContext->getInput() == input) {
            delete mSplitStreamHandle[i].openInputStreamManagementContext;
            mSplitStreamHandle[i].openInputStreamManagementContext = NULL;
        }
    }

    for (int i = 0; i < INPUT_DEVICE_MAX_SPLIT_STREAMS; i++) {
        if (mSplitStreamHandle[i].readInputStreamManagementContext != NULL &&
                mSplitStreamHandle[i].readInputStreamManagementContext->getInput() == input) {
            delete mSplitStreamHandle[i].readInputStreamManagementContext;
            mSplitStreamHandle[i].readInputStreamManagementContext = NULL;
        }
    }
}

// ----------------------------------------------------------------------------
}; // namespace android