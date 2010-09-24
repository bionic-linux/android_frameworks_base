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

#define LOG_TAG "TrackFragmentTable"
//#define LOG_NDEBUG 0
#include <utils/Log.h>

#include "include/TrackFragmentTable.h"

#include <arpa/inet.h>

#include <media/stagefright/DataSource.h>
#include <media/stagefright/MediaDebug.h>
#include <media/stagefright/Utils.h>

#define kMaxNumSyncSamplesToScan	20
namespace android {

////////////////////////////////////////////////////////////////////////////////


TrackFragmentTable::TrackFragmentTable(uint32_t aTrackId, uint32_t aTimescale, bool aIsVideoTrack)
      : mTrackId(aTrackId),
        mTimescale(aTimescale),
        mIsVideoTrack(aIsVideoTrack),
        mGotFirstFragment(false),
        mTFRAEntryCount(0),
        mFirstTFRAEntry(NULL),
        mUpdateTableHintEntry(NULL),
        mFragmentCount(0),
        mFirstFragmentEntry(NULL),
        mLastFragmentEntry(NULL),
        mMaxSampleSize(0),
        mNumSyncSamplesLeftToScan(0),
        mLastScannedTFRAEntry(NULL),
        mThumbnailSampleOffset(0),
        mThumbnailSampleSize(0),
        mThumbnailSampleTimestamp(0),
        mGotBestThumbnail(false),
        mCurrentFragmentEntry(NULL),
        mCurrentFragmentRun(NULL),
        mCurrentSample(NULL),
        mSetNewCurrentFragment(true),
        mNextFragmentTimestamp(0)
{
    LOGV("TrackFragmentTable() mTrackId %d  mTimescale %d",
            mTrackId, mTimescale);
}

TrackFragmentTable::~TrackFragmentTable() {
    LOGV("~TrackFragmentTable() mTrackId %d", mTrackId);

    FragmentEntry *entry = mFirstFragmentEntry;
    while (entry) {
        FragmentEntry *next = entry->next;
        entry->traf = NULL;
        delete entry;
        entry = next;
    }
    mFirstFragmentEntry = mLastFragmentEntry = NULL;
}

status_t TrackFragmentTable::setRandomAccessInfo(TrackFragRandomAccess *aInfo) {
    LOGV("setRandomAccessInfo() mTrackId %d", mTrackId);

    if ((NULL == aInfo) || (aInfo->id != mTrackId)) {
        return ERROR_MALFORMED;
    }

    mTFRAEntryCount = aInfo->entryCount;
    mFirstTFRAEntry = aInfo->firstTFRAEntry;

#if 0
    LOGV("mTFRAEntryCount %d", mTFRAEntryCount);
    TFRAEntry *temp = mFirstTFRAEntry;
    for (uint32_t idx = 0; idx < mTFRAEntryCount; idx++) {
        LOGV("Entry %d moofOffset64 0x%x time64 %lld trafNumber %d trunNumber %d sampleNumber %d",
            idx, (uint32_t)temp->moofOffset64, temp->time64, temp->trafNumber, temp->trunNumber, temp->sampleNumber);

        temp = temp->next;
    }
#endif

    TFRAEntry *tfra = mFirstTFRAEntry;
    uint64_t curMoofOffset = 0;
    uint32_t curTrafNumber = 0;

    // allocate the fragment table
    for (uint32_t idx = 0; idx < mTFRAEntryCount; idx++) {

        if ((curMoofOffset != tfra->moofOffset64) || (curTrafNumber != tfra->trafNumber)) {
            // new fragment encountered
            curMoofOffset = tfra->moofOffset64;
            curTrafNumber = tfra->trafNumber;

            FragmentEntry *entry = new FragmentEntry;
            entry->next = NULL;
            if (mLastFragmentEntry) {
                mLastFragmentEntry->next = entry;
            } else {
                mFirstFragmentEntry = entry;
            }
            mLastFragmentEntry = entry;

            entry->trafNumInParentMoof = tfra->trafNumber;
            entry->offsetOfParentMoof = tfra->moofOffset64;
            entry->traf = NULL;
        }
        tfra = tfra->next;
    }

    if (mIsVideoTrack) {
        mNumSyncSamplesLeftToScan = (mTFRAEntryCount > kMaxNumSyncSamplesToScan) ? kMaxNumSyncSamplesToScan : mTFRAEntryCount;
        LOGV("setRandomAccessInfo() mNumSyncSamplesLeftToScan %d", mNumSyncSamplesLeftToScan);
    }

    return OK;
}

status_t TrackFragmentTable::updateTable(TrackFrag* aTrackFragment) {

#if 0
    LOGV("updateTable() mTrackId %d", mTrackId);
    LOGV("aTrackFragment->offsetOfParentMoof 0x%x", (uint32_t)aTrackFragment->offsetOfParentMoof);
    LOGV("aTrackFragment->trafNumInParentMoof %d", aTrackFragment->trafNumInParentMoof);
    LOGV("updateTable() aTrackFragment first sample timestamp %lld last sample timestamp %lld",
            aTrackFragment->firstTrun->firstSample->sampleTimestamp,
            aTrackFragment->lastTrun->lastSample->sampleTimestamp);
#endif

    if ((NULL == aTrackFragment) || (aTrackFragment->id != mTrackId)) {
        return ERROR_MALFORMED;
    }

    FragmentEntry *entry = NULL;

    // if TFRA list is present (clip is seekable),
    // the fragment entries should have been created,
    // find it in the table,
    // if not found, create a new entry and add to the table in data offset order
    bool hasTFRA = (0 != mTFRAEntryCount) && (NULL != mFirstTFRAEntry);
    bool isSyncFrag = false;

    if (hasTFRA) {
        if (NULL != mUpdateTableHintEntry) {
            entry = mUpdateTableHintEntry;
        } else {
            entry = mFirstFragmentEntry;
        }
        while (NULL != entry) {
            if ((entry->trafNumInParentMoof == aTrackFragment->trafNumInParentMoof) &&
                    (entry->offsetOfParentMoof == aTrackFragment->offsetOfParentMoof)) {
                isSyncFrag = true;
                break;
            }
            entry = entry->next;
        }

        if (isSyncFrag) {
            entry->traf = aTrackFragment;
        } else {
            LOGV("updateTable() not sync fragment");
            entry = new FragmentEntry;
            entry->next = NULL;

            entry->traf = aTrackFragment;
            entry->trafNumInParentMoof = aTrackFragment->trafNumInParentMoof;
            entry->offsetOfParentMoof= aTrackFragment->offsetOfParentMoof;

            FragmentEntry *left = NULL;
            FragmentEntry *right = NULL;

            if (NULL != mUpdateTableHintEntry) {
                left = mUpdateTableHintEntry;
                right = mUpdateTableHintEntry->next;
            } else {
                left = mFirstFragmentEntry;
                right = mFirstFragmentEntry->next;
            }
            bool found = false;
            while ((NULL != left) && (NULL != right)) {
                if ((entry->offsetOfParentMoof > left->offsetOfParentMoof) &&
                    (entry->offsetOfParentMoof < right->offsetOfParentMoof)) {
                    // insert in the middle
#if 0
                    LOGV("updateTable() found, entry 0x%x left 0x%x right 0x%x",
                            (uint32_t)entry->offsetOfParentMoof, (uint32_t)left->offsetOfParentMoof,
                            (uint32_t)right->offsetOfParentMoof);
#endif
                    left->next = entry;
                    entry->next = right;

                    found = true;
                    break;
                }
                if (NULL != right) {
                    left = right;
                    right = right->next;
                } else {
                    break;
                }
            }

            if (!found) {
                // add to the end of table
                LOGV("updateTable() not found, add as last fragment");
                left = mLastFragmentEntry;
                mLastFragmentEntry->next = entry;
                mLastFragmentEntry = entry;
            }

            // see if we need to fix up the timestamps
            if (NULL != left->traf) {
                uint64_t nextTimestamp = left->traf->lastTrun->lastSample->sampleTimestamp +
                        left->traf->lastTrun->lastSample->sampleDuration;
                if (entry->traf->firstTrun->firstSample->sampleTimestamp != nextTimestamp) {
                    LOGV("updateTable() fixing timestamps");
                    // need to fix timestamps
                    TrackFragRun *trun = entry->traf->firstTrun;
                    while (NULL != trun) {
                        Sample *sample = trun->firstSample;
                        while (NULL != sample) {
                            sample->sampleTimestamp = nextTimestamp;
                            nextTimestamp += sample->sampleDuration;
                            sample = sample->next;
                        }
                        trun = trun->next;
                    }
                }
            }
        }
    } else {
        // not seekable, just add to end
        entry = new FragmentEntry;
        entry->next = NULL;
        if (mLastFragmentEntry) {
            mLastFragmentEntry->next = entry;
        } else {
            mFirstFragmentEntry = entry;
        }
        mLastFragmentEntry = entry;

        entry->traf = aTrackFragment;
        entry->trafNumInParentMoof = aTrackFragment->trafNumInParentMoof;
        entry->offsetOfParentMoof= aTrackFragment->offsetOfParentMoof;
    }

    mGotFirstFragment = true;
    mFragmentCount++;

    if (entry->traf->maxSampleSize > mMaxSampleSize) {
        mMaxSampleSize = entry->traf->maxSampleSize;
    }

    if (mSetNewCurrentFragment) {
        mSetNewCurrentFragment = false;
        mCurrentFragmentEntry = entry;
        mCurrentFragmentRun = mCurrentFragmentEntry->traf->firstTrun;
        mCurrentSample = mCurrentFragmentRun->firstSample;
    }

    // scan for thumbnails as the fragments come in
    if (mIsVideoTrack && !mGotBestThumbnail) {
        if (0 == mTFRAEntryCount) {
            // no random access info
            // use the first sample as thumbnail
            mNumSyncSamplesLeftToScan = 0;
            mGotBestThumbnail = true;
            mThumbnailSampleOffset = mFirstFragmentEntry->traf->firstTrun->firstSample->sampleDataOffset;
            mThumbnailSampleSize = mFirstFragmentEntry->traf->firstTrun->firstSample->sampleSize;
            mThumbnailSampleTimestamp = mFirstFragmentEntry->traf->firstTrun->firstSample->sampleTimestamp;
        } else if (isSyncFrag){
            // find the sync sample entries in this track fragment
            while (mNumSyncSamplesLeftToScan > 0) {
                // examine more sync samples to find the best(largest in sample size)
                TFRAEntry *thisEntry = NULL;
                if (NULL == mLastScannedTFRAEntry) {
                    thisEntry = mFirstTFRAEntry;
                } else {
                    thisEntry = mLastScannedTFRAEntry->next;
                }
                if ((NULL == thisEntry) || (thisEntry->moofOffset64 > aTrackFragment->offsetOfParentMoof)) {
                    // done with this fragment
                    break;
                }

                bool found = false;
                if ((aTrackFragment->offsetOfParentMoof == thisEntry->moofOffset64) &&
                        (aTrackFragment->trafNumInParentMoof == thisEntry->trafNumber)) {
                    // sync sample in this traf
                    TrackFragRun* thisTrun = aTrackFragment->firstTrun;
                    while ((NULL != thisTrun) && !found) {
                        if (thisTrun->trunNumInParentTraf == thisEntry->trunNumber) {
                            Sample *thisSample = thisTrun->firstSample;
                            while ((NULL != thisSample) && !found) {
                                if (thisSample->sampleNumInParentTrun == thisEntry->sampleNumber) {
                                    // check this sync sample's size
                                    LOGV("updateTable() thisSample->sampleSize %d mThumbnailSampleSize %d",
                                            thisSample->sampleSize, mThumbnailSampleSize);
                                    if (thisSample->sampleSize > mThumbnailSampleSize) {
                                        LOGV("updateTable() found bigger thumbnail");
                                        mThumbnailSampleOffset = thisSample->sampleDataOffset;
                                        mThumbnailSampleSize = thisSample->sampleSize;
                                        mThumbnailSampleTimestamp = thisSample->sampleTimestamp;
                                    }
                                    found = true;
                                } else {
                                    thisSample = thisSample->next;
                                }
                            } // end while
                        } else {
                            thisTrun = thisTrun->next;
                        }
                    } // end while
                    LOGV("updateTable() mNumSyncSamplesLeftToScan %d", mNumSyncSamplesLeftToScan);

                    mNumSyncSamplesLeftToScan--;
                    // done with this sync sample
                    mLastScannedTFRAEntry = thisEntry;
                } //end if
            } // end while

            if (0 == mNumSyncSamplesLeftToScan) {
                LOGV("updateTable() done with thumbnails");
                mGotBestThumbnail = true;
            }
        } // end else
    } // end if

    mUpdateTableHintEntry = NULL;
    return OK;
}

status_t TrackFragmentTable::getMaxSampleSize(size_t *aMaxSize) {
    Mutex::Autolock autoLock(mLock);

    *aMaxSize = mMaxSampleSize;

    return OK;
}

// given a media time, return the matching sample index, sample data offset, sample size and decding time offset
// if kSyncSample_Flag is set, return the sync sample closest to (before) the media time
// if kSyncSample_Flag is not set, returns the actual sample
status_t TrackFragmentTable::findClosestSample(uint64_t aMediaTime, uint32_t aFlags,
        off_t *aSampleOffset, size_t *aSampleSize, uint32_t *aSampleTimestamp,
        off_t *aMoofOffsetToParse, uint64_t *aNextFragTimestamp) {
    Mutex::Autolock autoLock(mLock);

    LOGV("findClosestSample() mTrackId %d aMediaTime %lld",
            mTrackId, aMediaTime);

    if ((0 == mTFRAEntryCount) || (NULL == mFirstTFRAEntry) ||
        (0 == mFragmentCount) || (NULL == mFirstFragmentEntry)) {
        return ERROR_OUT_OF_RANGE;
    }

    // find the closest sync sample in the random access list
    TFRAEntry *thisEntry = mFirstTFRAEntry;

    if (aMediaTime > thisEntry->time64) {
        while (1) {
            TFRAEntry *nextEntry = thisEntry->next;
            if (NULL == nextEntry) {
                // no more sync samples
                // this one is the closest
                break;
            } else if (aMediaTime < nextEntry->time64) {
                // this entry is the closest
                break;
            } else {
                thisEntry = nextEntry;
            }
        }
    }

    LOGV("thisEntry->time64 %lld", thisEntry->time64);
    LOGV("thisEntry->moofOffset64 0x%x", (uint32_t)thisEntry->moofOffset64);
    LOGV("thisEntry->trafNumber %d", thisEntry->trafNumber);
    LOGV("thisEntry->trunNumber %d", thisEntry->trunNumber);
    LOGV("thisEntry->sampleNumber %d", thisEntry->sampleNumber);

    bool found = false;
    FragmentEntry *thisFrag = mFirstFragmentEntry;
    Sample *thisSample = NULL;
    TrackFragRun *thisTrun = NULL;

    // find the sync sample in the fragment table
    while ((NULL != thisFrag) && !found) {
        // find the track fragment
        if ((thisFrag->offsetOfParentMoof == thisEntry->moofOffset64) &&
            (thisFrag->trafNumInParentMoof == thisEntry->trafNumber)) {

            if (NULL == thisFrag->traf) {
                *aMoofOffsetToParse = thisEntry->moofOffset64;
                mUpdateTableHintEntry = thisFrag;
                *aNextFragTimestamp = 0;
                return ERROR_NOT_YET_PARSED;
            }
            // find the run
            thisTrun = thisFrag->traf->firstTrun;
            while ((NULL != thisTrun) && !found){
                if (thisTrun->trunNumInParentTraf == thisEntry->trunNumber) {
                    // find the sample
                    thisSample = thisTrun->firstSample;
                    while ((NULL != thisSample) && !found) {
                        if (thisSample->sampleNumInParentTrun == thisEntry->sampleNumber) {
                            // found the sample
                            LOGV("Found sample");
                            found = true;
                        } else {
                            thisSample = thisSample->next;
                        }
                    } // end while
                } else {
                    thisTrun = thisTrun->next;
                }
            } // end while
        } else {
            thisFrag = thisFrag->next;
        }
    } // end while

    if (found && thisFrag != NULL) {
        if ((aFlags & kSyncSample_Flag) || (aMediaTime == thisSample->sampleTimestamp)) {
            // return the sync sample
            // this is the most likely usecase
            LOGV("return sync sample");
        } else {
            // don't want the sync sample
            // find the sample that closely matches the media time in this fragment
            // don't bother looking at the other fragments
            found = false;
            // look for the fragment
            if (aMediaTime <
                (thisFrag->traf->lastTrun->lastSample->sampleTimestamp +
                    thisFrag->traf->lastTrun->lastSample->sampleDuration - 1)) {
                // look for the run
                thisTrun = thisFrag->traf->firstTrun;
                while (NULL != thisTrun) {
                    if (aMediaTime <=
                        (thisTrun->lastSample->sampleTimestamp +
                            thisTrun->lastSample->sampleDuration - 1)) {
                        // found the run
                        found = true;
                        break;
                    } else {
                        thisTrun = thisTrun->next;
                    }
                } // end while

                if (found) {
                    // look for the sample
                    found = false;
                    thisSample = thisTrun->firstSample;
                    while (NULL != thisSample) {
                        Sample *nextSample = thisSample->next;
                        if ((NULL == nextSample) || (aMediaTime < nextSample->sampleTimestamp)) {
                            // found the sample
                            found = true;
                            break;
                        } else {
                            thisSample = nextSample;
                        }
                    } // end while
                } // end else
            } // end else

            if (!found) {
                // use the last run
                found = true;
                thisSample = thisFrag->traf->lastTrun->lastSample;
            }
        } // end else

        if (found) {
            // return this sample
            if (NULL != aSampleOffset) {
                *aSampleOffset = thisSample->sampleDataOffset;
            }
            if (NULL != aSampleSize) {
                *aSampleSize = thisSample->sampleSize;
            }
            if (NULL != aSampleTimestamp) {
                *aSampleTimestamp = thisSample->sampleTimestamp;
            }

            // update current values
            mCurrentFragmentEntry = thisFrag;
            mCurrentFragmentRun = thisTrun;
            mCurrentSample = thisSample;

            LOGV("thisSample->sampleDataOffset 0x%x", (uint32_t)thisSample->sampleDataOffset);
            LOGV("thisSample->sampleSize %d", thisSample->sampleSize);
            LOGV("thisSample->sampleTimestamp %lld", thisSample->sampleTimestamp);

            // move on to the next sample
            mSetNewCurrentFragment = false;

            if (NULL != mCurrentSample->next) {
                // next sample in the same run
                mCurrentSample = mCurrentSample->next;
            } else if (NULL != mCurrentFragmentRun->next) {
                // first sample in next run
                mCurrentFragmentRun = mCurrentFragmentRun->next;
                mCurrentSample = mCurrentFragmentRun->firstSample;
            } else {
                if (NULL != mCurrentFragmentEntry->next) {
                    // first sample in the first run of the next fragment
                    mCurrentFragmentEntry = mCurrentFragmentEntry->next;
                    mCurrentFragmentRun = mCurrentFragmentEntry->traf->firstTrun;
                    mCurrentSample = mCurrentFragmentRun->firstSample;
                } else {
                    mSetNewCurrentFragment = true;
                }
            }

            return OK;
        } // end if
    } // end if

    return ERROR_OUT_OF_RANGE;
}


status_t TrackFragmentTable::findThumbnailSample(off_t *aSampleOffset,
        size_t *aSampleSize, uint32_t *aSampleTimestamp) {
    Mutex::Autolock autoLock(mLock);

    if (NULL != aSampleOffset) {
        *aSampleOffset = mThumbnailSampleOffset;
    }
    if (NULL != aSampleSize) {
        *aSampleSize = mThumbnailSampleSize;
    }
    if (NULL != aSampleTimestamp) {
        *aSampleTimestamp = mThumbnailSampleTimestamp;
    }

    LOGV("mThumbnailSampleOffset 0x%x", (uint32_t)mThumbnailSampleOffset);
    LOGV("mThumbnailSampleSize %d", mThumbnailSampleSize);
    LOGV("mThumbnailSampleTimestamp 0x%x", (uint32_t)mThumbnailSampleTimestamp);

    return OK;
}



status_t TrackFragmentTable::getMetaDataForNextSample(off_t *aSampleOffset,
        size_t *aSampleSize, uint32_t *aSampleTimestamp, off_t *aMoofOffsetToParse,
        uint64_t *aNextFragTimestamp) {
    Mutex::Autolock autoLock(mLock);

    //LOGV("getMetaDataForNextSample() mTrackId %d", mTrackId);

    if (mSetNewCurrentFragment) {
        *aMoofOffsetToParse = mCurrentFragmentEntry->offsetOfParentMoof + mCurrentFragmentEntry->traf->sizeOfParentMoof;
        mUpdateTableHintEntry = mCurrentFragmentEntry;
        *aNextFragTimestamp = mNextFragmentTimestamp;

        LOGV("getMetaDataForNextSample() ERROR_NOT_YET_PARSED aMoofOffsetToParse 0x%x aNextFragTimestamp %lld",
                (uint32_t)*aMoofOffsetToParse, *aNextFragTimestamp);

        return ERROR_NOT_YET_PARSED;
    }

    if (NULL != aSampleOffset) {
        *aSampleOffset = mCurrentSample->sampleDataOffset;
    }
    if (NULL != aSampleSize) {
        *aSampleSize = mCurrentSample->sampleSize;
    }
    if (NULL != aSampleTimestamp) {
        *aSampleTimestamp = mCurrentSample->sampleTimestamp;
    }

    // move on to next sample
    if (NULL != mCurrentSample->next) {
        // next sample in the same run
        mCurrentSample = mCurrentSample->next;
    } else if (NULL != mCurrentFragmentRun->next) {
        // first sample in next run
        mCurrentFragmentRun = mCurrentFragmentRun->next;
        mCurrentSample = mCurrentFragmentRun->firstSample;
    } else if ((NULL != mCurrentFragmentEntry->next) &&
            (NULL != mCurrentFragmentEntry->next->traf)) {
        // first sample in the first run of the next fragment
        mCurrentFragmentEntry = mCurrentFragmentEntry->next;
        mCurrentFragmentRun = mCurrentFragmentEntry->traf->firstTrun;
        mCurrentSample = mCurrentFragmentRun->firstSample;
    } else {
        // need to parse next fragment
        mNextFragmentTimestamp = mCurrentSample->sampleTimestamp + mCurrentSample->sampleDuration;
        mSetNewCurrentFragment = true;
    }

    return OK;
}

}  // namespace android

