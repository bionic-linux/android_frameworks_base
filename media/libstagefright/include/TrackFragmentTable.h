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

#ifndef TRACK_FRAGMENT_TABLE_H_

#define TRACK_FRAGMENT_TABLE_H_

#include <sys/types.h>
#include <stdint.h>

#include <media/stagefright/MediaErrors.h>
#include <utils/RefBase.h>
#include <utils/threads.h>
#ifndef MPEG4_EXTRACTOR_H_
#include <include/MPEG4Extractor.h>
#endif

namespace android {

class DataSource;
struct TrackFragRandomAccess;
struct TrackFrag;
struct TFRAEntry;
struct TrackFragRun;
struct Sample;

class TrackFragmentTable : public RefBase {

public:
    TrackFragmentTable(uint32_t aTrackId, uint32_t aTimescale, bool aIsVideoTrack);

    // called by extractor if mfra/tfra is found by this track
    status_t setRandomAccessInfo(TrackFragRandomAccess *aInfo);

    // called by extractor to fill the table
    // it is updated on the fly when more traf boxes are encountered for this track
    status_t updateTable(TrackFrag* aTrackFragment);

    status_t getMaxSampleSize(size_t *aMaxSize);

    // returns data offset, size and decoding time for a sample
    status_t getMetaDataForNextSample(off_t *aSampleOffset,
                    size_t *aSampleSize, uint32_t *aSampleTimestamp,
                    off_t *aMoofOffsetToParse, uint64_t *aNextFragTimestamp);

    // returns the sample number closest to the media time
    enum {
        kSyncSample_Flag = 1
    };
    status_t findClosestSample(uint64_t aMediaTime, uint32_t aFlags,
            off_t *aSampleOffset, size_t *aSampleSize, uint32_t *aSampleTimestamp,
            off_t *aMoofOffsetToParse, uint64_t *aNextFragTimestamp);

    status_t findThumbnailSample(off_t *aSampleOffset,
            size_t *aSampleSize, uint32_t *aSampleTimestamp);

    bool firstFragmentUpdated() { return mGotFirstFragment; }

    bool isVideo() { return mIsVideoTrack; }

    bool hasBestThumbnail() { return mGotBestThumbnail; }

protected:
    ~TrackFragmentTable();

private:
    struct FragmentEntry {
        FragmentEntry *next;
        TrackFrag *traf;
        uint64_t offsetOfParentMoof;
        uint32_t trafNumInParentMoof;
    };

    uint32_t mTrackId;
    uint32_t mTimescale;
    bool mIsVideoTrack;
    bool mGotFirstFragment;

    Mutex mLock;

    // random access
    uint32_t mTFRAEntryCount;
    TFRAEntry *mFirstTFRAEntry;
    FragmentEntry *mUpdateTableHintEntry;

    uint64_t mFragmentCount;
    FragmentEntry *mFirstFragmentEntry, *mLastFragmentEntry;

    uint32_t mMaxSampleSize;

    uint32_t mNumSyncSamplesLeftToScan;
    TFRAEntry* mLastScannedTFRAEntry;
    off_t mThumbnailSampleOffset;
    size_t mThumbnailSampleSize;
    uint32_t mThumbnailSampleTimestamp;
    bool mGotBestThumbnail;

    FragmentEntry *mCurrentFragmentEntry;
    TrackFragRun *mCurrentFragmentRun;
    Sample *mCurrentSample;
    bool mSetNewCurrentFragment;
    uint64_t mNextFragmentTimestamp;

    TrackFragmentTable(const TrackFragmentTable &);
    TrackFragmentTable &operator=(const TrackFragmentTable &);
};

}  // namespace android

#endif  // TRACK_FRAGMENT_TABLE_H_

