/*
 * Copyright (C) 2009 The Android Open Source Project
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

#ifndef MPEG4_EXTRACTOR_H_

#define MPEG4_EXTRACTOR_H_

#include <media/stagefright/MediaExtractor.h>
#include <utils/Vector.h>
#include <utils/threads.h>

namespace android {

static const uint32_t UUID_SIZE = 16;
static const uint8_t PSSH_BOX_UUID[UUID_SIZE] =   {0xd0, 0x8a, 0x4f, 0x18, 0x10, 0xf3, 0x4a, 0x82, 0xb6, 0xc8, 0x32, 0xd8, 0xab, 0xa1, 0x83, 0xd3};
static const uint8_t TRACK_ENCRYPTION_BOX_UUID[UUID_SIZE] =   {0x89, 0x74, 0xdb, 0xce, 0x7b, 0xe7, 0x4c, 0x51, 0x84, 0xf9, 0x71, 0x48, 0xf9, 0x88, 0x25, 0x54};
static const uint8_t SAMPLE_ENCRYPTION_BOX_UUID[UUID_SIZE] = {0xA2, 0x39, 0x4F, 0x52, 0x5A, 0x9B, 0x4f, 0x14, 0xA2, 0x44, 0x6C, 0x42, 0x7C, 0x64, 0x8D, 0xF4};

class DataSource;
class SampleTable;
class String8;
class TrackFragmentTable;

struct TFRAEntry {
    TFRAEntry *next;
    uint64_t time64;
    uint64_t moofOffset64;
    uint32_t trafNumber;
    uint32_t trunNumber;
    uint32_t sampleNumber;
};

struct TrackFragRandomAccess {
    TrackFragRandomAccess *next;
    uint32_t id;
    uint32_t entryCount;
    TFRAEntry *firstTFRAEntry;
    TFRAEntry *lastTFRAEntry;
};

struct Sample {
    Sample *next;
    uint32_t sampleDuration;
    uint32_t sampleSize;
    uint32_t sampleFlags;
    uint32_t sampleCompTimeOffset;
    uint32_t sampleNumInParentTrun;
    uint64_t sampleTimestamp;
    off_t sampleDataOffset;
};

struct TrackFragRun {
    TrackFragRun *next;
    uint32_t trunNumInParentTraf;
    uint32_t entryCount;
    uint64_t dataOffset;
    uint32_t firstSampleFlags;
    Sample *firstSample, *lastSample;
};

struct SubSampleMappingData {
    SubSampleMappingData *next;
    uint16_t clearBytes;
    uint32_t encryptedbytes;
};

struct EncryptionInfo {
    EncryptionInfo *next;
    uint8_t *initVec;
    uint32_t entryCount;
    SubSampleMappingData *firstEntry, *lastEntry;
};

struct SampleEncryption {
    uint32_t algorithmID;
    uint8_t initVecSize;
    uint8_t keyID[16];
    uint32_t sampleCount;
    EncryptionInfo *firstInfo, *lastInfo;
};

struct TrackFrag {
    TrackFrag *next;
    uint32_t id;
    uint32_t trafNumInParentMoof;
    uint64_t offsetOfParentMoof;
    uint32_t sizeOfParentMoof;
    bool hasSyncSamples;
    uint32_t firstSyncSTrunNum;
    uint32_t firstSyncSampleNum;
    uint64_t firstSyncTimestamp;
    uint64_t firstSampleTimestamp;
    bool fixTimestamps;
    uint32_t maxSampleSize;
    uint64_t baseDataOffset;
    uint32_t sampleDescIndex;
    uint32_t defaultSampleDuration;
    uint32_t defaultSampleSize;
    uint32_t defaultSampleFlags;
    TrackFragRun *firstTrun, *lastTrun;
    SampleEncryption *encryption;
};

class MPEG4Extractor : public MediaExtractor {
public:
    // Extractor assumes ownership of "source".
    MPEG4Extractor(const sp<DataSource> &source);

    virtual size_t countTracks();
    virtual sp<MediaSource> getTrack(size_t index);
    virtual sp<MetaData> getTrackMetaData(size_t index, uint32_t flags);

    virtual sp<MetaData> getMetaData();

    status_t parseNextMOOF(const sp<DataSource> &dataSource);
    status_t parseNextMOOF(off_t offset, const sp<DataSource> &dataSource);

    // MPEG4Source needs to access parseNextMOOF() methods
    friend class MPEG4Source;

protected:
    virtual ~MPEG4Extractor();

private:
    struct Track {
        Track *next;
        uint32_t id;
        sp<MetaData> meta;
        uint32_t timescale;
        uint32_t defaultSampleDescIndex;
        uint32_t defaultSampleDuration;
        uint32_t defaultSampleSize;
        uint32_t defaultSampleFlags;
        uint64_t nextTimestamp;
        sp<SampleTable> sampleTable;
        sp<TrackFragmentTable> fragmentTable;
        bool hasFragmentTable;
        bool includes_expensive_metadata;
        bool skipTrack;
    };

    // content protection system info
    struct PSSHeader {
        PSSHeader *next;
        uint8_t systemId[16];
        uint32_t dataSize;
        uint8_t *data;
    };

    struct MovieFrag {
        MovieFrag *next;
        uint32_t sequenceNum;
        uint64_t offset;
        TrackFrag *firstTRAF, *lastTRAF;
    };

    struct SyncSample {
        SyncSample *next;
        TFRAEntry *entry;
    };

    struct MoofSyncSamples {
        uint32_t entryCount;
        SyncSample *firstSyncSample;
        SyncSample *lastSyncSample;
    };

    sp<DataSource> mDataSource;
    bool mHaveMetadata;
    bool mHasVideo;

    // moof support
    Mutex mLock;
    bool mHasFragments;
    bool mMFRAParsed;
    Track *mFirstTrack, *mLastTrack;
    TrackFragRandomAccess *mFirstTFRA, *mLastTFRA;
    PSSHeader *mFirstPSSH, *mLastPSSH;
    MovieFrag *mFirstMovieFrag, *mLastMovieFrag;
    uint64_t mOverallDuration;
    off_t mNextMoofParsingOffset;
    uint64_t mPresentationTimescale;

    MoofSyncSamples *mSyncSamplesInMoof;

    sp<MetaData> mFileMetaData;

    Vector<uint32_t> mPath;

    status_t readMetaData();
    status_t parseChunk(off_t *offset, int depth);
    status_t parseMetaData(off_t offset, size_t size);
    status_t parseMFRA(off_t offset);
    status_t parseMOOF(off_t moofOffset, off_t moofSize, const sp<DataSource> &dataSource,
            MoofSyncSamples *syncSamples = NULL);

    status_t updateAudioTrackInfoFromESDS_MPEG4Audio(
            const void *esds_data, size_t esds_size);

    static status_t verifyTrack(Track *track);

    MPEG4Extractor(const MPEG4Extractor &);
    MPEG4Extractor &operator=(const MPEG4Extractor &);
};

bool SniffMPEG4(
        const sp<DataSource> &source, String8 *mimeType, float *confidence);

} // namespace android

#endif  // MPEG4_EXTRACTOR_H_

