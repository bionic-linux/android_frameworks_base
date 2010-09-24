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

#define LOG_TAG "MPEG4Extractor"
//#define LOG_NDEBUG 0
#include <utils/Log.h>

#include "include/MPEG4Extractor.h"
#include "include/SampleTable.h"
#include "include/TrackFragmentTable.h"

#include <arpa/inet.h>

#include <ctype.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include <media/stagefright/DataSource.h>
#include "include/ESDS.h"
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaBufferGroup.h>
#include <media/stagefright/MediaDebug.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/Utils.h>
#include <utils/String8.h>

namespace android {

class MPEG4Source : public MediaSource {
public:
    // Caller retains ownership of both "dataSource", "sampleTable" and "fragmentTable".
    MPEG4Source(const sp<MetaData> &format,
                const sp<DataSource> &dataSource,
                int32_t timeScale,
                const sp<SampleTable> &sampleTable,
                const sp<TrackFragmentTable> &fragmentTable,
                bool moofPresent,
                uint64_t *nextFragmentTimestamp,
                const sp<MPEG4Extractor> &parentExtractor);

    virtual status_t start(MetaData *params = NULL);
    virtual status_t stop();

    virtual sp<MetaData> getFormat();

    virtual status_t read(
            MediaBuffer **buffer, const ReadOptions *options = NULL);

protected:
    virtual ~MPEG4Source();

private:
    Mutex mLock;

    sp<MetaData> mFormat;
    sp<DataSource> mDataSource;
    int32_t mTimescale;
    sp<SampleTable> mSampleTable;
    sp<TrackFragmentTable> mFragmentTable;
    bool mHasFragments;
    sp<MPEG4Extractor> mParentExtractor;
    uint64_t mCurrentSampleIndex;
    uint64_t *mNextFragmentTimestamp;

    bool mIsAVC;
    size_t mNALLengthSize;

    bool mStarted;

    MediaBufferGroup *mGroup;

    MediaBuffer *mBuffer;

    bool mWantsNALFragments;

    uint8_t *mSrcBuffer;

    size_t parseNALSize(const uint8_t *data) const;

    MPEG4Source(const MPEG4Source &);
    MPEG4Source &operator=(const MPEG4Source &);
};

// This custom data source wraps an existing one and satisfies requests
// falling entirely within a cached range from the cache while forwarding
// all remaining requests to the wrapped datasource.
// This is used to cache the full sampletable metadata for a single track,
// possibly wrapping multiple times to cover all tracks, i.e.
// Each MPEG4DataSource caches the sampletable metadata for a single track.

struct MPEG4DataSource : public DataSource {
    MPEG4DataSource(const sp<DataSource> &source);

    virtual status_t initCheck() const;
    virtual ssize_t readAt(off_t offset, void *data, size_t size);
    virtual status_t getSize(off_t *size);
    virtual uint32_t flags();

    status_t setCachedRange(off_t offset, size_t size);

protected:
    virtual ~MPEG4DataSource();

private:
    Mutex mLock;

    sp<DataSource> mSource;
    off_t mCachedOffset;
    size_t mCachedSize;
    uint8_t *mCache;

    void clearCache();

    MPEG4DataSource(const MPEG4DataSource &);
    MPEG4DataSource &operator=(const MPEG4DataSource &);
};

MPEG4DataSource::MPEG4DataSource(const sp<DataSource> &source)
    : mSource(source),
      mCachedOffset(0),
      mCachedSize(0),
      mCache(NULL) {
}

MPEG4DataSource::~MPEG4DataSource() {
    clearCache();
}

void MPEG4DataSource::clearCache() {
    if (mCache) {
        free(mCache);
        mCache = NULL;
    }

    mCachedOffset = 0;
    mCachedSize = 0;
}

status_t MPEG4DataSource::initCheck() const {
    return mSource->initCheck();
}

ssize_t MPEG4DataSource::readAt(off_t offset, void *data, size_t size) {
    Mutex::Autolock autoLock(mLock);

    if (offset >= mCachedOffset
            && offset + size <= mCachedOffset + mCachedSize) {
        memcpy(data, &mCache[offset - mCachedOffset], size);
        return size;
    }

    return mSource->readAt(offset, data, size);
}

status_t MPEG4DataSource::getSize(off_t *size) {
    return mSource->getSize(size);
}

uint32_t MPEG4DataSource::flags() {
    return mSource->flags();
}

status_t MPEG4DataSource::setCachedRange(off_t offset, size_t size) {
    Mutex::Autolock autoLock(mLock);

    clearCache();

    mCache = (uint8_t *)malloc(size);

    if (mCache == NULL) {
        return -ENOMEM;
    }

    mCachedOffset = offset;
    mCachedSize = size;

    ssize_t err = mSource->readAt(mCachedOffset, mCache, mCachedSize);

    if (err < (ssize_t)size) {
        clearCache();
        return ERROR_IO;
    }

    return OK;
}

////////////////////////////////////////////////////////////////////////////////

static void hexdump(const void *_data, size_t size) {
    const uint8_t *data = (const uint8_t *)_data;
    size_t offset = 0;
    while (offset < size) {
        printf("0x%04x  ", offset);

        size_t n = size - offset;
        if (n > 16) {
            n = 16;
        }

        for (size_t i = 0; i < 16; ++i) {
            if (i == 8) {
                printf(" ");
            }

            if (offset + i < size) {
                printf("%02x ", data[offset + i]);
            } else {
                printf("   ");
            }
        }

        printf(" ");

        for (size_t i = 0; i < n; ++i) {
            if (isprint(data[offset + i])) {
                printf("%c", data[offset + i]);
            } else {
                printf(".");
            }
        }

        printf("\n");

        offset += 16;
    }
}

static const char *FourCC2MIME(uint32_t fourcc) {
    switch (fourcc) {
        case FOURCC('m', 'p', '4', 'a'):
            return MEDIA_MIMETYPE_AUDIO_AAC;

        case FOURCC('s', 'a', 'm', 'r'):
            return MEDIA_MIMETYPE_AUDIO_AMR_NB;

        case FOURCC('s', 'a', 'w', 'b'):
            return MEDIA_MIMETYPE_AUDIO_AMR_WB;

        case FOURCC('m', 'p', '4', 'v'):
            return MEDIA_MIMETYPE_VIDEO_MPEG4;

        case FOURCC('s', '2', '6', '3'):
            return MEDIA_MIMETYPE_VIDEO_H263;

        case FOURCC('a', 'v', 'c', '1'):
            return MEDIA_MIMETYPE_VIDEO_AVC;

        default:
            CHECK(!"should not be here.");
            return NULL;
    }
}

MPEG4Extractor::MPEG4Extractor(const sp<DataSource> &source)
    : mDataSource(source),
      mHaveMetadata(false),
      mHasVideo(false),
      mHasFragments(false),
      mMFRAParsed(false),
      mFirstTrack(NULL),
      mLastTrack(NULL),
      mFirstTFRA(NULL),
      mLastTFRA(NULL),
      mFirstPSSH(NULL),
      mLastPSSH(NULL),
      mFirstMovieFrag(NULL),
      mLastMovieFrag(NULL),
      mNextMoofParsingOffset(0),
      mPresentationTimescale(0),
      mFileMetaData(new MetaData) {
    LOGV("MPEG4Extractor::MPEG4Extractor() In/Out");
}

MPEG4Extractor::~MPEG4Extractor() {
    LOGV("MPEG4Extractor::~MPEG4Extractor() In");

    Track *track = mFirstTrack;
    while (track) {
        Track *next = track->next;

        delete track;
        track = next;
    }
    mFirstTrack = mLastTrack = NULL;

    TrackFragRandomAccess *tfra = mFirstTFRA;
    while (tfra) {

        TFRAEntry *entry = tfra->firstTFRAEntry;
        while (entry) {

            TFRAEntry *nextEntry = entry->next;
            delete entry;
            entry = nextEntry;
        }
        tfra->firstTFRAEntry = NULL;

        TrackFragRandomAccess *nextTfra = tfra->next;
        delete tfra;
        tfra = nextTfra;
    }
    mFirstTFRA = mLastTFRA = NULL;

    PSSHeader *pssh = mFirstPSSH;
    while (pssh) {

        if (NULL != pssh->data) {
            free(pssh->data);
            pssh->data = NULL;
        }
        PSSHeader *nextPssh = pssh->next;
        delete pssh;
        pssh = nextPssh;
    }
    mFirstPSSH = mLastPSSH = NULL;

    MovieFrag *moof = mFirstMovieFrag;
    while (moof) {

        TrackFrag *traf = moof->firstTRAF;
        while (traf) {

            TrackFragRun *trun = traf->firstTrun;
            while (trun) {

                Sample *sample = trun->firstSample;
                while (sample) {

                    Sample *nextSample = sample->next;
                    delete sample;
                    sample = nextSample;
                }
                trun->firstSample = trun->lastSample = NULL;

                TrackFragRun *nextTrun = trun->next;
                delete trun;
                trun = nextTrun;
            }
            traf->firstTrun = traf->lastTrun = NULL;

            if (traf->encryption) {

                EncryptionInfo *info = traf->encryption->firstInfo;
                while (info) {

                    if (info->initVec) {
                        free(info->initVec);
                        info->initVec = NULL;
                    }

                    SubSampleMappingData *entry = info->firstEntry;
                    while (entry) {

                        SubSampleMappingData *nextEntry = entry->next;
                        delete(entry);
                        entry = nextEntry;
                    }
                    info->firstEntry = info->lastEntry = NULL;

                    EncryptionInfo *nextInfo = info->next;
                    delete info;
                    info = nextInfo;
                }
                traf->encryption->firstInfo = traf->encryption->lastInfo = NULL;

                delete(traf->encryption);
            }
            traf->encryption = NULL;

            TrackFrag *nextTraf = traf->next;
            delete traf;
            traf = nextTraf;
        }
        moof->firstTRAF = moof->lastTRAF = NULL;

        MovieFrag *nextMoof = moof->next;
        delete moof;
        moof = nextMoof;
    }
    mFirstMovieFrag = mLastMovieFrag = NULL;
    LOGV("MPEG4Extractor::~MPEG4Extractor() Out");
}

sp<MetaData> MPEG4Extractor::getMetaData() {
    status_t err;
    if ((err = readMetaData()) != OK) {
        return new MetaData;
    }

    return mFileMetaData;
}

size_t MPEG4Extractor::countTracks() {
    status_t err;
    if ((err = readMetaData()) != OK) {
        return 0;
    }

    size_t n = 0;
    Track *track = mFirstTrack;
    while (track) {
        ++n;
        track = track->next;
    }

    return n;
}

sp<MetaData> MPEG4Extractor::getTrackMetaData(
        size_t index, uint32_t flags) {
    status_t err;
    if ((err = readMetaData()) != OK) {
        return NULL;
    }

    Track *track = mFirstTrack;
    while (index > 0) {
        if (track == NULL) {
            return NULL;
        }

        track = track->next;
        --index;
    }

    if (track == NULL) {
        return NULL;
    }

    if ((flags & kIncludeExtensiveMetaData)
            && !track->includes_expensive_metadata) {
        track->includes_expensive_metadata = true;

        const char *mime;
        CHECK(track->meta->findCString(kKeyMIMEType, &mime));
        if (!strncasecmp("video/", mime, 6)) {
            if (!mHasFragments) {
                // no movie fragments
                uint32_t sampleIndex;
                uint32_t sampleTime;
                if (track->sampleTable->findThumbnailSample(&sampleIndex) == OK
                        && track->sampleTable->getMetaDataForSample(
                            sampleIndex, NULL /* offset */, NULL /* size */,
                            &sampleTime) == OK) {
                                track->meta->setInt64(
                                kKeyThumbnailTime,
                                ((int64_t)sampleTime * 1000000) / track->timescale);
                }
            } else {
                uint32_t sampleTime = 0;
                if (track->hasFragmentTable) {
                    if (track->fragmentTable->findThumbnailSample(
                        NULL /* offset */, NULL /* size */,
                        &sampleTime) == OK) {
                                track->meta->setInt64(
                                        kKeyThumbnailTime,
                                        (sampleTime * 1000000) / track->timescale);
                    }
                }
            }
        }
    }

    return track->meta;
}

status_t MPEG4Extractor::readMetaData() {
    if (mHaveMetadata) {
        return OK;
    }

    off_t offset = 0;
    status_t err;
    while ((err = parseChunk(&offset, 0)) == OK) {
    }

    if (mHaveMetadata) {
        if (mHasVideo) {
            mFileMetaData->setCString(kKeyMIMEType, "video/mp4");
        } else {
            mFileMetaData->setCString(kKeyMIMEType, "audio/mp4");
        }

        if (mHasFragments) {
            off_t fileSize = 0;
            err = mDataSource->getSize(&fileSize);
            if (err != OK) {
                return err;
            }

            // parse the moof boxes to get first samples for each track
            bool done = false;
            while (!done) {

                if  (mNextMoofParsingOffset < fileSize) {
                    err = parseNextMOOF(mNextMoofParsingOffset, mDataSource);
                    if (err != OK) {
                        break;
                    }
                }

                done = true;
                Track *track = mFirstTrack;
                while (NULL != track) {
                    if (!track->hasFragmentTable || !track->fragmentTable->firstFragmentUpdated()) {
                        // this track needs samples
                        done = false;
                        break;
                    }
                    track = track->next;
                }
            }

            // for video tracks, parse more MOOF until the best thumbnail is extracted
            Track *track = mFirstTrack;
            while (NULL != track) {
                if (track->hasFragmentTable && track->fragmentTable->isVideo()) {
                    while (!track->fragmentTable->hasBestThumbnail()) {
                        // this track needs more fragments to determine the best thumbnail
                        if  (mNextMoofParsingOffset < fileSize) {
                            err = parseNextMOOF(mNextMoofParsingOffset, mDataSource);
                            if (err != OK) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
                track = track->next;
            }

            // set max buffer size for each track
            // default to 1K if not known
            track = mFirstTrack;
            while (NULL != track) {
                size_t maxSize = 0;
                track->fragmentTable->getMaxSampleSize(&maxSize);
                if (0 == maxSize) maxSize = 1024;
                track->meta->setInt32(kKeyMaxInputSize, maxSize + 10 * 2);
                track = track->next;
            }
        }
        return OK;
    }

    return err;
}

static void MakeFourCCString(uint32_t x, char *s) {
    s[0] = x >> 24;
    s[1] = (x >> 16) & 0xff;
    s[2] = (x >> 8) & 0xff;
    s[3] = x & 0xff;
    s[4] = '\0';
}

struct PathAdder {
    PathAdder(Vector<uint32_t> *path, uint32_t chunkType)
        : mPath(path) {
        mPath->push(chunkType);
    }

    ~PathAdder() {
        mPath->pop();
    }

private:
    Vector<uint32_t> *mPath;

    PathAdder(const PathAdder &);
    PathAdder &operator=(const PathAdder &);
};

static bool underMetaDataPath(const Vector<uint32_t> &path) {
    return path.size() >= 5
        && path[0] == FOURCC('m', 'o', 'o', 'v')
        && path[1] == FOURCC('u', 'd', 't', 'a')
        && path[2] == FOURCC('m', 'e', 't', 'a')
        && path[3] == FOURCC('i', 'l', 's', 't');
}

// Given a time in seconds since Jan 1 1904, produce a human-readable string.
static void convertTimeToDate(int64_t time_1904, String8 *s) {
    time_t time_1970 = time_1904 - (((66 * 365 + 17) * 24) * 3600);

    char tmp[32];
    strftime(tmp, sizeof(tmp), "%Y%m%dT%H%M%S.000Z", gmtime(&time_1970));

    s->setTo(tmp);
}

status_t MPEG4Extractor::parseChunk(off_t *offset, int depth) {

    //LOGV("parseChunk() offset 0x%x", (uint32_t)offset);
    uint32_t hdr[2];
    if (mDataSource->readAt(*offset, hdr, 8) < 8) {
        return ERROR_IO;
    }
    uint64_t chunk_size = ntohl(hdr[0]);
    uint32_t chunk_type = ntohl(hdr[1]);
    off_t data_offset = *offset + 8;

    if (chunk_size == 1) {
        if (mDataSource->readAt(*offset + 8, &chunk_size, 8) < 8) {
            return ERROR_IO;
        }
        chunk_size = ntoh64(chunk_size);
        data_offset += 8;
    }

    char chunk[5];
    MakeFourCCString(chunk_type, chunk);

#if 0
    static const char kWhitespace[] = "                                        ";
    const char *indent = &kWhitespace[sizeof(kWhitespace) - 1 - 2 * depth];
    printf("%sfound chunk '%s' of size %lld\n", indent, chunk, chunk_size);

    char buffer[256];
    size_t n = chunk_size;
    if (n > sizeof(buffer)) {
        n = sizeof(buffer);
    }
    if (mDataSource->readAt(*offset, buffer, n)
            < (ssize_t)n) {
        return ERROR_IO;
    }

    hexdump(buffer, n);
#endif

    PathAdder autoAdder(&mPath, chunk_type);

    off_t chunk_data_size = *offset + chunk_size - data_offset;

    if (chunk_type != FOURCC('c', 'p', 'r', 't')
            && mPath.size() == 5 && underMetaDataPath(mPath)) {
        off_t stop_offset = *offset + chunk_size;
        *offset = data_offset;
        while (*offset < stop_offset) {
            status_t err = parseChunk(offset, depth + 1);
            if (err != OK) {
                return err;
            }
        }

        if (*offset != stop_offset) {
            return ERROR_MALFORMED;
        }

        return OK;
    }

    switch(chunk_type) {
        case FOURCC('m', 'o', 'o', 'v'):
        case FOURCC('t', 'r', 'a', 'k'):
        case FOURCC('m', 'd', 'i', 'a'):
        case FOURCC('m', 'i', 'n', 'f'):
        case FOURCC('d', 'i', 'n', 'f'):
        case FOURCC('s', 't', 'b', 'l'):
        case FOURCC('m', 'v', 'e', 'x'):
        case FOURCC('m', 'o', 'o', 'f'):
        case FOURCC('t', 'r', 'a', 'f'):
        case FOURCC('m', 'f', 'r', 'a'):
        case FOURCC('s', 'k', 'i' ,'p'):
        case FOURCC('u', 'd', 't', 'a'):
        case FOURCC('i', 'l', 's', 't'):
        {
            bool isMovieExtends = false;
            if (chunk_type == FOURCC('m', 'v', 'e', 'x')) {
                mHasFragments = true;
                isMovieExtends = true;
            }

            if (chunk_type == FOURCC('m', 'o', 'o', 'f')) {
                mHasFragments = true;
                status_t err = parseMOOF(*offset, chunk_size, mDataSource);
                if (err != OK) {
                    return err;
                }
            }

            if (chunk_type == FOURCC('s', 't', 'b', 'l')) {
                LOGV("sampleTable chunk is %d bytes long.", (size_t)chunk_size);

                if (mDataSource->flags() & DataSource::kWantsPrefetching) {
                    sp<MPEG4DataSource> cachedSource =
                        new MPEG4DataSource(mDataSource);

                    if (cachedSource->setCachedRange(*offset, chunk_size) == OK) {
                        mDataSource = cachedSource;
                    }
                }

                mLastTrack->sampleTable = new SampleTable(mDataSource);
            }

            bool isTrack = false;
            if (chunk_type == FOURCC('t', 'r', 'a', 'k')) {
                isTrack = true;

                Track *track = new Track;
                track->next = NULL;
                if (mLastTrack) {
                    mLastTrack->next = track;
                } else {
                    mFirstTrack = track;
                }
                mLastTrack = track;

                track->meta = new MetaData;
                track->includes_expensive_metadata = false;
                track->skipTrack = false;
                track->timescale = 0;
                track->meta->setCString(kKeyMIMEType, "application/octet-stream");
                track->sampleTable = NULL;
                track->fragmentTable = NULL;
                track->defaultSampleDescIndex = 0;
                track->defaultSampleDuration = 0;
                track->defaultSampleSize = 0;
                track->defaultSampleFlags = 0;
                track->nextTimestamp = 0;
                track->hasFragmentTable = false;
            }

            off_t stop_offset = *offset + chunk_size;
            *offset = data_offset;
            while (*offset < stop_offset) {
                status_t err = parseChunk(offset, depth + 1);
                if (err != OK) {
                    return err;
                }
            }

            if (*offset != stop_offset) {
                return ERROR_MALFORMED;
            }

            if (isTrack) {
                if (mLastTrack->skipTrack) {
                    Track *cur = mFirstTrack;

                    if (cur == mLastTrack) {
                        delete cur;
                        mFirstTrack = mLastTrack = NULL;
                    } else {
                        while (cur && cur->next != mLastTrack) {
                            cur = cur->next;
                        }
                        cur->next = NULL;
                        delete mLastTrack;
                        mLastTrack = cur;
                    }

                    return OK;
                }

                status_t err = verifyTrack(mLastTrack);

                if (err != OK) {
                    return err;
                }
            } else if (isMovieExtends && !mMFRAParsed) {
                // parse mfra now
                // mfra box is optional
                parseMFRA(*offset);
                mMFRAParsed = true;

            } else if (chunk_type == FOURCC('m', 'o', 'o', 'v')) {
                mHaveMetadata = true;
                return UNKNOWN_ERROR;  // Return a dummy error.
            }
            break;
        }

        case FOURCC('t', 'k', 'h', 'd'):
        {
            if (chunk_data_size < 4) {
                return ERROR_MALFORMED;
            }

            uint8_t version;
            if (mDataSource->readAt(data_offset, &version, 1) < 1) {
                return ERROR_IO;
            }

            uint64_t ctime, mtime, duration;
            uint32_t id = 0;
            uint32_t width, height;

            if (version == 1) {
                if (chunk_data_size != 36 + 60) {
                    return ERROR_MALFORMED;
                }

                uint8_t buffer[36 + 60];
                if (mDataSource->readAt(
                            data_offset, buffer, sizeof(buffer)) < (ssize_t)sizeof(buffer)) {
                    return ERROR_IO;
                }

                ctime = U64_AT(&buffer[4]);
                mtime = U64_AT(&buffer[12]);
                id = U32_AT(&buffer[20]);
                duration = U64_AT(&buffer[28]);
                width = U32_AT(&buffer[88]);
                height = U32_AT(&buffer[92]);
            } else if (version == 0) {
                if (chunk_data_size != 24 + 60) {
                    return ERROR_MALFORMED;
                }

                uint8_t buffer[24 + 60];
                if (mDataSource->readAt(
                            data_offset, buffer, sizeof(buffer)) < (ssize_t)sizeof(buffer)) {
                    return ERROR_IO;
                }
                ctime = U32_AT(&buffer[4]);
                mtime = U32_AT(&buffer[8]);
                id = U32_AT(&buffer[12]);
                duration = U32_AT(&buffer[20]);
                width = U32_AT(&buffer[76]);
                height = U32_AT(&buffer[80]);
            }

            mLastTrack->id = id;
            *offset += chunk_size;
            break;
        }

        case FOURCC('m', 'd', 'h', 'd'):
        {
            if (chunk_data_size < 4) {
                return ERROR_MALFORMED;
            }

            uint8_t version;
            if (mDataSource->readAt(
                        data_offset, &version, sizeof(version))
                    < (ssize_t)sizeof(version)) {
                return ERROR_IO;
            }

            off_t timescale_offset;

            if (version == 1) {
                timescale_offset = data_offset + 4 + 16;
            } else if (version == 0) {
                timescale_offset = data_offset + 4 + 8;
            } else {
                return ERROR_IO;
            }

            uint32_t timescale;
            if (mDataSource->readAt(
                        timescale_offset, &timescale, sizeof(timescale))
                    < (ssize_t)sizeof(timescale)) {
                return ERROR_IO;
            }

            mLastTrack->timescale = ntohl(timescale);

            int64_t duration;
            if (version == 1) {
                if (mDataSource->readAt(
                            timescale_offset + 4, &duration, sizeof(duration))
                        < (ssize_t)sizeof(duration)) {
                    return ERROR_IO;
                }
                duration = ntoh64(duration);
            } else {
                int32_t duration32;
                if (mDataSource->readAt(
                            timescale_offset + 4, &duration32, sizeof(duration32))
                        < (ssize_t)sizeof(duration32)) {
                    return ERROR_IO;
                }
                duration = ntohl(duration32);
            }
            mLastTrack->meta->setInt64(
                    kKeyDuration, (duration * 1000000) / mLastTrack->timescale);

            *offset += chunk_size;
            break;
        }

        case FOURCC('s', 't', 's', 'd'):
        {
            if (chunk_data_size < 8) {
                return ERROR_MALFORMED;
            }

            uint8_t buffer[8];
            if (chunk_data_size < (off_t)sizeof(buffer)) {
                return ERROR_MALFORMED;
            }

            if (mDataSource->readAt(
                        data_offset, buffer, 8) < 8) {
                return ERROR_IO;
            }

            if (U32_AT(buffer) != 0) {
                // Should be version 0, flags 0.
                return ERROR_MALFORMED;
            }

            uint32_t entry_count = U32_AT(&buffer[4]);

            if (entry_count > 1) {
                // For now we only support a single type of media per track.

                mLastTrack->skipTrack = true;
                *offset += chunk_size;
                break;
            }

            off_t stop_offset = *offset + chunk_size;
            *offset = data_offset + 8;
            for (uint32_t i = 0; i < entry_count; ++i) {
                status_t err = parseChunk(offset, depth + 1);
                if (err != OK) {
                    return err;
                }
            }

            // there could be other boxes under stsd box
            // if there is stuff left in the stsd box, just skip them
            if (stop_offset < *offset) {
                return ERROR_MALFORMED;
            } else if (stop_offset > *offset) {
                *offset = stop_offset;
            }
            break;
        }

        case FOURCC('m', 'p', '4', 'a'):
        case FOURCC('s', 'a', 'm', 'r'):
        case FOURCC('s', 'a', 'w', 'b'):
        {
            uint8_t buffer[8 + 20];
            if (chunk_data_size < (ssize_t)sizeof(buffer)) {
                // Basic AudioSampleEntry size.
                return ERROR_MALFORMED;
            }

            if (mDataSource->readAt(
                        data_offset, buffer, sizeof(buffer)) < (ssize_t)sizeof(buffer)) {
                return ERROR_IO;
            }

            uint16_t data_ref_index = U16_AT(&buffer[6]);
            uint16_t num_channels = U16_AT(&buffer[16]);

            uint16_t sample_size = U16_AT(&buffer[18]);
            uint32_t sample_rate = U32_AT(&buffer[24]) >> 16;

            if (!strcasecmp(MEDIA_MIMETYPE_AUDIO_AMR_NB,
                            FourCC2MIME(chunk_type))) {
                // AMR NB audio is always mono, 8kHz
                num_channels = 1;
                sample_rate = 8000;
            } else if (!strcasecmp(MEDIA_MIMETYPE_AUDIO_AMR_WB,
                               FourCC2MIME(chunk_type))) {
                // AMR WB audio is always mono, 16kHz
                num_channels = 1;
                sample_rate = 16000;
            }

#if 0
            printf("*** coding='%s' %d channels, size %d, rate %d\n",
                   chunk, num_channels, sample_size, sample_rate);
#endif

            mLastTrack->meta->setCString(kKeyMIMEType, FourCC2MIME(chunk_type));
            mLastTrack->meta->setInt32(kKeyChannelCount, num_channels);
            mLastTrack->meta->setInt32(kKeySampleRate, sample_rate);

            off_t stop_offset = *offset + chunk_size;
            *offset = data_offset + sizeof(buffer);
            while (*offset < stop_offset) {
                status_t err = parseChunk(offset, depth + 1);
                if (err != OK) {
                    return err;
                }
            }

            if (*offset != stop_offset) {
                return ERROR_MALFORMED;
            }
            break;
        }

        case FOURCC('m', 'p', '4', 'v'):
        case FOURCC('s', '2', '6', '3'):
        case FOURCC('a', 'v', 'c', '1'):
        {
            mHasVideo = true;

            uint8_t buffer[78];
            if (chunk_data_size < (ssize_t)sizeof(buffer)) {
                // Basic VideoSampleEntry size.
                return ERROR_MALFORMED;
            }

            if (mDataSource->readAt(
                        data_offset, buffer, sizeof(buffer)) < (ssize_t)sizeof(buffer)) {
                return ERROR_IO;
            }

            uint16_t data_ref_index = U16_AT(&buffer[6]);
            uint16_t width = U16_AT(&buffer[6 + 18]);
            uint16_t height = U16_AT(&buffer[6 + 20]);

            // printf("*** coding='%s' width=%d height=%d\n",
            //        chunk, width, height);

            mLastTrack->meta->setCString(kKeyMIMEType, FourCC2MIME(chunk_type));
            mLastTrack->meta->setInt32(kKeyWidth, width);
            mLastTrack->meta->setInt32(kKeyHeight, height);

            off_t stop_offset = *offset + chunk_size;
            *offset = data_offset + sizeof(buffer);
            while (*offset < stop_offset) {
                status_t err = parseChunk(offset, depth + 1);
                if (err != OK) {
                    return err;
                }
            }

            if (*offset != stop_offset) {
                return ERROR_MALFORMED;
            }
            break;
        }

        case FOURCC('s', 't', 'c', 'o'):
        case FOURCC('c', 'o', '6', '4'):
        {
            status_t err =
                mLastTrack->sampleTable->setChunkOffsetParams(
                        chunk_type, data_offset, chunk_data_size);

            if (err != OK) {
                return err;
            }

            *offset += chunk_size;
            break;
        }

        case FOURCC('s', 't', 's', 'c'):
        {
            status_t err =
                mLastTrack->sampleTable->setSampleToChunkParams(
                        data_offset, chunk_data_size);

            if (err != OK) {
                return err;
            }

            *offset += chunk_size;
            break;
        }

        case FOURCC('s', 't', 's', 'z'):
        case FOURCC('s', 't', 'z', '2'):
        {
            status_t err =
                mLastTrack->sampleTable->setSampleSizeParams(
                        chunk_type, data_offset, chunk_data_size);

            if (err != OK) {
                return err;
            }

            size_t max_size;
            CHECK_EQ(mLastTrack->sampleTable->getMaxSampleSize(&max_size), OK);

            // Assume that a given buffer only contains at most 10 fragments,
            // each fragment originally prefixed with a 2 byte length will
            // have a 4 byte header (0x00 0x00 0x00 0x01) after conversion,
            // and thus will grow by 2 bytes per fragment.
            mLastTrack->meta->setInt32(kKeyMaxInputSize, max_size + 10 * 2);

            *offset += chunk_size;
            break;
        }

        case FOURCC('s', 't', 't', 's'):
        {
            status_t err =
                mLastTrack->sampleTable->setTimeToSampleParams(
                        data_offset, chunk_data_size);

            if (err != OK) {
                return err;
            }

            *offset += chunk_size;
            break;
        }

        case FOURCC('s', 't', 's', 's'):
        {
            status_t err =
                mLastTrack->sampleTable->setSyncSampleParams(
                        data_offset, chunk_data_size);

            if (err != OK) {
                return err;
            }

            *offset += chunk_size;
            break;
        }

        case FOURCC('e', 's', 'd', 's'):
        {
            if (chunk_data_size < 4) {
                return ERROR_MALFORMED;
            }

            uint8_t buffer[256];
            if (chunk_data_size > (off_t)sizeof(buffer)) {
                return ERROR_BUFFER_TOO_SMALL;
            }

            if (mDataSource->readAt(
                        data_offset, buffer, chunk_data_size) < chunk_data_size) {
                return ERROR_IO;
            }

            if (U32_AT(buffer) != 0) {
                // Should be version 0, flags 0.
                return ERROR_MALFORMED;
            }

            mLastTrack->meta->setData(
                    kKeyESDS, kTypeESDS, &buffer[4], chunk_data_size - 4);

            if (mPath.size() >= 2
                    && mPath[mPath.size() - 2] == FOURCC('m', 'p', '4', 'a')) {
                // Information from the ESDS must be relied on for proper
                // setup of sample rate and channel count for MPEG4 Audio.
                // The generic header appears to only contain generic
                // information...

                status_t err = updateAudioTrackInfoFromESDS_MPEG4Audio(
                        &buffer[4], chunk_data_size - 4);

                if (err != OK) {
                    return err;
                }
            }

            *offset += chunk_size;
            break;
        }

        case FOURCC('a', 'v', 'c', 'C'):
        {
            char buffer[256];
            if (chunk_data_size > (off_t)sizeof(buffer)) {
                return ERROR_BUFFER_TOO_SMALL;
            }

            if (mDataSource->readAt(
                        data_offset, buffer, chunk_data_size) < chunk_data_size) {
                return ERROR_IO;
            }

            mLastTrack->meta->setData(
                    kKeyAVCC, kTypeAVCC, buffer, chunk_data_size);

            *offset += chunk_size;
            break;
        }

        case FOURCC('m', 'e', 't', 'a'):
        {
            uint8_t buffer[4];
            if (chunk_data_size < (off_t)sizeof(buffer)) {
                return ERROR_MALFORMED;
            }

            if (mDataSource->readAt(
                        data_offset, buffer, 4) < 4) {
                return ERROR_IO;
            }

            if (U32_AT(buffer) != 0) {
                // Should be version 0, flags 0.

                // If it's not, let's assume this is one of those
                // apparently malformed chunks that don't have flags
                // and completely different semantics than what's
                // in the MPEG4 specs and skip it.
                *offset += chunk_size;
                return OK;
            }

            off_t stop_offset = *offset + chunk_size;
            *offset = data_offset + sizeof(buffer);
            while (*offset < stop_offset) {
                status_t err = parseChunk(offset, depth + 1);
                if (err != OK) {
                    return err;
                }
            }

            if (*offset != stop_offset) {
                return ERROR_MALFORMED;
            }
            break;
        }

        case FOURCC('d', 'a', 't', 'a'):
        {
            if (mPath.size() == 6 && underMetaDataPath(mPath)) {
                status_t err = parseMetaData(data_offset, chunk_data_size);

                if (err != OK) {
                    return err;
                }
            }

            *offset += chunk_size;
            break;
        }

        case FOURCC('m', 'v', 'h', 'd'):
        {
            if (chunk_data_size < 12) {
                return ERROR_MALFORMED;
            }

            uint8_t header[28];
            if (mDataSource->readAt(
                        data_offset, header, sizeof(header))
                    < (ssize_t)sizeof(header)) {
                return ERROR_IO;
            }

            int64_t creationTime;
            if (header[0] == 1) {
                creationTime = U64_AT(&header[4]);
                mPresentationTimescale = U64_AT(&header[20]);
            } else if (header[0] != 0) {
                return ERROR_MALFORMED;
            } else {
                creationTime = U32_AT(&header[4]);
                mPresentationTimescale = U32_AT(&header[12]);
            }

            String8 s;
            convertTimeToDate(creationTime, &s);

            mFileMetaData->setCString(kKeyDate, s.string());

            *offset += chunk_size;
            break;
        }

        case FOURCC('m', 'e', 'h', 'd'):
        {
            if (chunk_data_size < 4) {
                return ERROR_MALFORMED;
            }

            uint8_t header[12];
            if (mDataSource->readAt(
                        data_offset, header, sizeof(header)) < (ssize_t)sizeof(header)) {
                return ERROR_IO;
            }

            if (header[0] == 1) {
                mOverallDuration = U64_AT(&header[4]);
            } else {
                mOverallDuration = U32_AT(&header[4]);
            }
            // override individual track duration if applicable
            if ((0 != mOverallDuration) && (0 != mPresentationTimescale)) {
                Track * track = mFirstTrack;
                while (NULL != track) {
                    int64_t tmp_1 = 0;
                    int64_t tmp_2 = (mOverallDuration * 1000000) / mPresentationTimescale;
                    track->meta->findInt64(kKeyDuration, &tmp_1);
                    if (tmp_1 < tmp_2) {
                    track->meta->setInt64(
                            kKeyDuration,
                            tmp_2);
                    }
                    track = track->next;
                }
            }

            *offset += chunk_size;
            break;
        }

        case FOURCC('t', 'r', 'e', 'x'):
        {
            if (chunk_data_size < 24) {
                return ERROR_MALFORMED;
            }

            uint8_t buffer[24];
            if (mDataSource->readAt(
                        data_offset, buffer, sizeof(buffer)) < (ssize_t)sizeof(buffer)) {
                return ERROR_IO;
            }

            uint32_t id = U32_AT(&buffer[4]);

            // find the track with match id
            // assume all trak's come before mvex
            Track* tmpTrack = mFirstTrack;
            while (NULL != tmpTrack) {
                if (id == tmpTrack->id) {
                    tmpTrack->defaultSampleDescIndex = U32_AT(&buffer[8]);
                    tmpTrack->defaultSampleDuration = U32_AT(&buffer[12]);
                    tmpTrack->defaultSampleSize = U32_AT(&buffer[16]);
                    tmpTrack->defaultSampleFlags = U32_AT(&buffer[20]);
                    break;
                }
                tmpTrack = tmpTrack->next;
            }

            *offset += chunk_size;
            break;
        }

        case FOURCC('u', 'u', 'i', 'd'):
        {
            if (chunk_data_size < 20) {
                return ERROR_MALFORMED;
            }

            // read in extended type, 16 bytes
            // check for Protection System Specific Header box (PIFF extension)
            uint8_t buffer[16];
            if (mDataSource->readAt(
                    data_offset, buffer, 16) < 16) {
                return ERROR_IO;
            }
            data_offset += 16;

            bool found = true;
            for (uint32_t idx = 0; idx < 16; idx++) {
                if (buffer[idx] != PSSH_BOX_UUID[idx]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                PSSHeader *pssh = new PSSHeader;
                pssh->next = NULL;
                if (mLastPSSH) {
                    mLastPSSH->next = pssh;
                } else {
                    mFirstPSSH = pssh;
                }
                mLastPSSH = pssh;

                // read in system id
                data_offset += 4;
                if (mDataSource->readAt(
                        data_offset, &pssh->systemId[0], 16) < 16) {
                    return ERROR_IO;
                }
                data_offset += 16;

                // read in data size
                if (mDataSource->readAt(
                        data_offset, &buffer[0], 4) < 4) {
                    return ERROR_IO;
                }
                data_offset += 4;
                pssh->dataSize = U32_AT(&buffer[0]);

                // read in data
                pssh->data = (uint8_t *)malloc(pssh->dataSize);
                if (NULL == pssh->data) {
                    return -ENOMEM;
                }
                if (mDataSource->readAt(
                        data_offset, pssh->data, pssh->dataSize) < (ssize_t)pssh->dataSize) {
                    return ERROR_IO;
                }
            }

            *offset += chunk_size;
            break;
        }

        default:
        {
            *offset += chunk_size;
            break;
        }
    }

    return OK;
}

status_t MPEG4Extractor::parseMetaData(off_t offset, size_t size) {
    if (size < 4) {
        return ERROR_MALFORMED;
    }

    uint8_t *buffer = new uint8_t[size + 1];
    if (mDataSource->readAt(
                offset, buffer, size) != (ssize_t)size) {
        delete[] buffer;
        buffer = NULL;

        return ERROR_IO;
    }

    uint32_t flags = U32_AT(buffer);

    uint32_t metadataKey = 0;
    switch (mPath[4]) {
        case FOURCC(0xa9, 'a', 'l', 'b'):
        {
            metadataKey = kKeyAlbum;
            break;
        }
        case FOURCC(0xa9, 'A', 'R', 'T'):
        {
            metadataKey = kKeyArtist;
            break;
        }
        case FOURCC('a', 'A', 'R', 'T'):
        {
            metadataKey = kKeyAlbumArtist;
            break;
        }
        case FOURCC(0xa9, 'd', 'a', 'y'):
        {
            metadataKey = kKeyYear;
            break;
        }
        case FOURCC(0xa9, 'n', 'a', 'm'):
        {
            metadataKey = kKeyTitle;
            break;
        }
        case FOURCC(0xa9, 'w', 'r', 't'):
        {
            metadataKey = kKeyWriter;
            break;
        }
        case FOURCC('c', 'o', 'v', 'r'):
        {
            metadataKey = kKeyAlbumArt;
            break;
        }
        case FOURCC('g', 'n', 'r', 'e'):
        {
            metadataKey = kKeyGenre;
            break;
        }
        case FOURCC(0xa9, 'g', 'e', 'n'):
        {
            metadataKey = kKeyGenre;
            break;
        }
        case FOURCC('t', 'r', 'k', 'n'):
        {
            if (size == 16 && flags == 0) {
                char tmp[16];
                sprintf(tmp, "%d/%d",
                        (int)buffer[size - 5], (int)buffer[size - 3]);

                mFileMetaData->setCString(kKeyCDTrackNumber, tmp);
            }
            break;
        }
        case FOURCC('d', 'i', 's', 'k'):
        {
            if (size == 14 && flags == 0) {
                char tmp[16];
                sprintf(tmp, "%d/%d",
                        (int)buffer[size - 3], (int)buffer[size - 1]);

                mFileMetaData->setCString(kKeyDiscNumber, tmp);
            }
            break;
        }

        default:
            break;
    }

    if (size >= 8 && metadataKey) {
        if (metadataKey == kKeyAlbumArt) {
            mFileMetaData->setData(
                    kKeyAlbumArt, MetaData::TYPE_NONE,
                    buffer + 8, size - 8);
        } else if (metadataKey == kKeyGenre) {
            if (flags == 0) {
                // uint8_t genre code, iTunes genre codes are
                // the standard id3 codes, except they start
                // at 1 instead of 0 (e.g. Pop is 14, not 13)
                // We use standard id3 numbering, so subtract 1.
                int genrecode = (int)buffer[size - 1];
                genrecode--;
                if (genrecode < 0) {
                    genrecode = 255; // reserved for 'unknown genre'
                }
                char genre[10];
                sprintf(genre, "%d", genrecode);

                mFileMetaData->setCString(metadataKey, genre);
            } else if (flags == 1) {
                // custom genre string
                buffer[size] = '\0';

                mFileMetaData->setCString(
                        metadataKey, (const char *)buffer + 8);
            }
        } else {
            buffer[size] = '\0';

            mFileMetaData->setCString(
                    metadataKey, (const char *)buffer + 8);
        }
    }

    delete[] buffer;
    buffer = NULL;

    return OK;
}

status_t MPEG4Extractor::parseMFRA(off_t offset) {
    // seek to EOF minus 16 bytes
    // look for mfro (movie fragment random access offset) box
    off_t fileSize = 0;
    uint32_t mfraOffset = 0, tfraOffset = 0;
    uint32_t mfroOffset = 16;
    uint64_t mfraSize = 0, boxSize = 0;

    status_t err = mDataSource->getSize(&fileSize);
    if (err != OK) {
        return err;
    }

    uint32_t hdr[4];
    off_t readOffset = fileSize - mfroOffset;

    if (mDataSource->readAt(readOffset, hdr, 16) < 16) {
        return ERROR_IO;
    }

    uint32_t boxType = ntohl(hdr[1]);
    char box[5];
    MakeFourCCString(boxType, box);
    PathAdder autoAdder(&mPath, boxType);

    switch (boxType) {
        case FOURCC('m', 'f', 'r', 'o'):
            LOGV("Found mfro box");
            mfraOffset = ntohl(hdr[3]);
            LOGV("mfraOffset 0x%x", mfraOffset);
            break;
        default:
            return ERROR_IO;
            break;
    }

    // get to the beginning of the mfra box
    readOffset -= (mfraOffset - mfroOffset);
    if (mDataSource->readAt(readOffset, &hdr[0], 8) < 8) {
        return ERROR_IO;
    }
    readOffset += 8;

    boxType = ntohl(hdr[1]);
    MakeFourCCString(boxType, box);
    PathAdder autoAdder_1(&mPath, boxType);

    tfraOffset = 8;

    switch (boxType) {
        case FOURCC('m', 'f', 'r', 'a'): {
            LOGV("Found mfra box");
            mfraSize = ntohl(hdr[0]);

            if (mfraSize == 1) {
                if (mDataSource->readAt(readOffset, &mfraSize, 8) < 8) {
                    return ERROR_IO;
                }
                readOffset += 8;
                mfraSize = ntoh64(mfraSize);
                tfraOffset += 8;
            }
            break;
        }
        default:
            return ERROR_IO;
            break;
    }

    off_t count = mfraSize - tfraOffset;
    off_t savedReadOffset = readOffset;
    bool done = false;
    while (((readOffset - savedReadOffset) < count) && !done) {
        // read in the tfra boxes
        if (mDataSource->readAt(readOffset, hdr, 8) < 8) {
                return ERROR_IO;
        }
        readOffset += 8;

        uint32_t boxType = ntohl(hdr[1]);
        char box[5];
        MakeFourCCString(boxType, box);
        PathAdder autoAdder_2(&mPath, boxType);

        switch (boxType) {
            case FOURCC('m', 'f', 'r', 'o'):
                LOGV("Found mfro box again, done parsing");
                done = true;
                break;

            case FOURCC('t', 'f', 'r', 'a'): {
                LOGV("Found tfra box");
                boxSize = ntohl(hdr[0]);

                if (boxSize == 1) {
                    if (mDataSource->readAt(readOffset, &boxSize, 8) < 8) {
                        return ERROR_IO;
                    }
                    readOffset += 8;
                    boxSize = ntoh64(boxSize);
                }
                uint8_t version;
                if (mDataSource->readAt(readOffset, &version, 1) < 1) {
                    return ERROR_IO;
                }
                // discard 3 bytes of flags
                readOffset += 4;

                uint32_t id = 0;
                if (mDataSource->readAt(readOffset, &id, 4) < 4) {
                    return ERROR_IO;
                }
                readOffset += 4;
                id = ntohl(id);

                uint32_t reserved = 0;
                if (mDataSource->readAt(readOffset, &reserved, 4) < 4) {
                    return ERROR_IO;
                }
                readOffset += 4;
                reserved = ntohl(reserved);

                uint32_t entries = 0;
                if (mDataSource->readAt(readOffset, &entries, 4) < 4) {
                    return ERROR_IO;
                }
                readOffset += 4;
                entries = ntohl(entries);

                TrackFragRandomAccess *tfra = new TrackFragRandomAccess;
                tfra->next = NULL;
                if (mLastTFRA) {
                    mLastTFRA->next = tfra;
                } else {
                    mFirstTFRA = tfra;
                }
                mLastTFRA = tfra;

                tfra->id = id;
                tfra->entryCount = entries;
                tfra->firstTFRAEntry = NULL;
                tfra->lastTFRAEntry = NULL;

                uint8_t trafNumSizelen = reserved & 0x00000003;
                uint8_t trunNumSizelen = (reserved >> 2) & 0x00000003;
                uint8_t sampleNumSizelen = (reserved >> 4) & 0x00000003;

                for (uint32_t idx = 0; idx < entries; idx++) {

                    TFRAEntry *entry = new TFRAEntry;
                    entry->next = NULL;
                    if (tfra->lastTFRAEntry) {
                        tfra->lastTFRAEntry->next = entry;
                    } else {
                        tfra->firstTFRAEntry = entry;
                    }
                    tfra->lastTFRAEntry = entry;

                    entry->time64 = 0;
                    entry->moofOffset64 = 0;
                    entry->trafNumber = 0;
                    entry->trunNumber = 0;
                    entry->sampleNumber = 0;

                    uint8_t data[8];

                    if (1 == version) {
                        if (mDataSource->readAt(readOffset, &data[0], 8) < 8) {
                            return ERROR_IO;
                        }
                        readOffset += 8;
                        entry->time64 = U64_AT(&data[0]);

                        if (mDataSource->readAt(readOffset, &data[0], 8) < 8) {
                            return ERROR_IO;
                        }
                        readOffset += 8;
                        entry->moofOffset64 = U64_AT(&data[0]);
                    } else {
                        if (mDataSource->readAt(readOffset, &data[0], 4) < 4) {
                            return ERROR_IO;
                        }
                        readOffset += 4;
                        uint32_t temp =  U32_AT(&data[0]);
                        entry->time64 = temp;

                        if (mDataSource->readAt(readOffset, &data[0], 4) < 4) {
                            return ERROR_IO;
                        }
                        readOffset += 4;
                        temp = U32_AT(&data[0]);
                        entry->moofOffset64 = temp;
                    }

                    switch (trafNumSizelen) {
                        case 0: {
                            if (mDataSource->readAt(readOffset, &data[0], 1) < 1) {
                                return ERROR_IO;
                            }
                            readOffset += 1;
                            entry->trafNumber = data[0];
                            break;
                        }
                        case 1: {
                            if (mDataSource->readAt(readOffset, &data[0], 2) < 1) {
                                return ERROR_IO;
                            }
                            readOffset += 2;
                            entry->trafNumber = U16_AT(&data[0]);
                            break;
                        }
                        case 2: {
                            if (mDataSource->readAt(readOffset, &data[0], 3) < 1) {
                                return ERROR_IO;
                            }
                            readOffset += 2;
                            entry->trafNumber = U24_AT(&data[0]);
                            break;
                        }
                        case 3: {
                            if (mDataSource->readAt(readOffset, &data[0], 4) < 1) {
                                return ERROR_IO;
                            }
                            readOffset += 4;
                            entry->trafNumber = U32_AT(&data[0]);
                            break;
                        }
                    }

                    switch (trunNumSizelen) {
                        case 0: {
                            if (mDataSource->readAt(readOffset, &data[0], 1) < 1) {
                                return ERROR_IO;
                            }
                            readOffset += 1;
                            entry->trunNumber = data[0];
                            break;
                        }
                        case 1: {
                            if (mDataSource->readAt(readOffset, &data[0], 2) < 1) {
                                return ERROR_IO;
                            }
                            readOffset += 2;
                            entry->trunNumber = U16_AT(&data[0]);
                            break;
                        }
                        case 2: {
                            if (mDataSource->readAt(readOffset, &data[0], 3) < 1) {
                                return ERROR_IO;
                            }
                            readOffset += 3;
                            entry->trunNumber = U24_AT(&data[0]);
                            break;
                        }
                        case 3: {
                            if (mDataSource->readAt(readOffset, &data[0], 4) < 1) {
                                return ERROR_IO;
                            }
                            readOffset += 4;
                            entry->trunNumber = U32_AT(&data[0]);
                            break;
                        }
                    }

                    switch (sampleNumSizelen) {
                        case 0: {
                            if (mDataSource->readAt(readOffset, &data[0], 1) < 1) {
                                return ERROR_IO;
                            }
                            readOffset += 1;
                            entry->sampleNumber = data[0];
                            break;
                        }
                        case 1: {
                            if (mDataSource->readAt(readOffset, &data[0], 2) < 1) {
                                return ERROR_IO;
                            }
                            readOffset += 2;
                            entry->sampleNumber = U16_AT(&data[0]);
                            break;
                        }
                        case 2: {
                            if (mDataSource->readAt(readOffset, &data[0], 3) < 1) {
                                return ERROR_IO;
                            }
                            readOffset += 3;
                            entry->sampleNumber = U24_AT(&data[0]);
                            break;
                        }
                        case 3:	{
                            if (mDataSource->readAt(readOffset, &data[0], 4) < 1) {
                                return ERROR_IO;
                            }
                            readOffset += 4;
                            entry->sampleNumber = U32_AT(&data[0]);
                            break;
                        }
                    }
                } // end for
                break;
            } //end case
            default: {
                // skip this box
                boxSize = ntohl(hdr[0]);
                if (boxSize == 1) {
                    if (mDataSource->readAt(readOffset, &boxSize, 8) < 8) {
                        return ERROR_IO;
                    }
                    boxSize = ntoh64(boxSize);
                    readOffset += (boxSize - 16);
                } else {
                    readOffset += (boxSize - 8);
                }
                break;
            }
        } // end switch
    } // end while

    // create the track fragment tables for each track
    Track *track = mFirstTrack;
    while (NULL != track) {

        bool isVideoTrack = false;
        const char *mime;
        CHECK(track->meta->findCString(kKeyMIMEType, &mime));
        if (!strncasecmp("video/", mime, 6)) {
            isVideoTrack = true;
        }

        track->fragmentTable = new TrackFragmentTable(track->id, track->timescale, isVideoTrack);
        track->hasFragmentTable = true;
        // set the random access info
        TrackFragRandomAccess *randomAccess = mFirstTFRA;
        while (NULL != randomAccess) {
            // there is only one per track
            if (randomAccess->id == track->id) {
                track->fragmentTable->setRandomAccessInfo(randomAccess);
                // set first sample timestamp to first sync sample timestamp
                track->nextTimestamp = randomAccess->firstTFRAEntry->time64;
                break;
            } else {
                randomAccess = randomAccess->next;
            }
        }
        track = track->next;
    }

    return OK;
}

status_t MPEG4Extractor::parseNextMOOF(const sp<DataSource> &dataSource) {
    return parseNextMOOF(mNextMoofParsingOffset, dataSource);
}


status_t MPEG4Extractor::parseNextMOOF(off_t offset, const sp<DataSource> &dataSource) {
    //LOGV("MPEG4Extractor::parseNextMOOF() In offset 0x%x", (uint32_t)offset);

    Mutex::Autolock autoLock(mLock);

    off_t fileSize = 0;
    status_t err = dataSource->getSize(&fileSize);
    if (err != OK) {
        return err;
    }

    off_t nextMoofOffset = offset;
    bool found = false;
    while (fileSize > 0) {
        // look the first moof box from offset
        // parse it and return
        uint32_t hdr[2];
        if (dataSource->readAt(nextMoofOffset, hdr, 8) < 8) {
            return ERROR_IO;
        }
        uint64_t chunk_size = 0;
        chunk_size = ntohl(hdr[0]);
        uint32_t chunk_type = ntohl(hdr[1]);

        if (chunk_size == 1) {
            if (dataSource->readAt(nextMoofOffset + 8, &chunk_size, 8) < 8) {
                return ERROR_IO;
            }
            chunk_size = ntoh64(chunk_size);
        }

        char chunk[5];
        MakeFourCCString(chunk_type, chunk);
        PathAdder autoAdder(&mPath, chunk_type);

        if (chunk_type == FOURCC('m', 'o', 'o', 'f')) {
            found = true;
            // check if there are sync samples in this moof
            mSyncSamplesInMoof = new MoofSyncSamples;
            mSyncSamplesInMoof->entryCount = 0;
            mSyncSamplesInMoof->firstSyncSample = NULL;
            mSyncSamplesInMoof->lastSyncSample = NULL;

            TrackFragRandomAccess *randomAccess = mFirstTFRA;
               while (NULL != randomAccess) {
                   TFRAEntry *entry = randomAccess->firstTFRAEntry;
                   while (NULL != entry) {
                       if (nextMoofOffset == (off_t)entry->moofOffset64) {

                           SyncSample *syncSample = new SyncSample;
                           syncSample->next = NULL;
                           if (mSyncSamplesInMoof->lastSyncSample) {
                               mSyncSamplesInMoof->lastSyncSample->next = syncSample;
                           } else {
                               mSyncSamplesInMoof->firstSyncSample = syncSample;
                           }
                           mSyncSamplesInMoof->lastSyncSample = syncSample;
                           mSyncSamplesInMoof->entryCount++;
                           syncSample->entry = entry;
                       }
                       entry = entry->next;
                   }
                randomAccess = randomAccess->next;
            }
               if (0 == mSyncSamplesInMoof->entryCount) {
                   delete mSyncSamplesInMoof;
                   mSyncSamplesInMoof = NULL;
                   parseMOOF(nextMoofOffset, chunk_size, dataSource);
               } else {
                   parseMOOF(nextMoofOffset, chunk_size, dataSource, mSyncSamplesInMoof);

                   SyncSample *syncSample = mSyncSamplesInMoof->firstSyncSample;
                   while ((syncSample != NULL) && (mSyncSamplesInMoof->entryCount > 0)) {

                       SyncSample *next = syncSample->next;
                       syncSample->entry = NULL;

                       delete syncSample;
                       mSyncSamplesInMoof->entryCount--;
                       syncSample = next;
                   }
                   delete mSyncSamplesInMoof;
                   mSyncSamplesInMoof = NULL;
               }
            nextMoofOffset += chunk_size;
            break;
        } else {
            nextMoofOffset += chunk_size;
            fileSize -= chunk_size;
        }
    }
    mNextMoofParsingOffset = nextMoofOffset;

    if (found) {
        return OK;
    }

    return ERROR_IO;
}

status_t MPEG4Extractor::parseMOOF(off_t moofOffset, off_t moofSize,
        const sp<DataSource> &dataSource, MoofSyncSamples *syncSamples) {
    //LOGV("MPEG4Extractor::parseMOOF In moofOffset 0x%x moofSize 0x%x syncSamples 0x%x",
    //		(uint32_t)moofOffset, (uint32_t)moofSize, (uint32_t)syncSamples);

    MovieFrag *moof = new MovieFrag;
    moof->next = NULL;
    if (mLastMovieFrag) {
        mLastMovieFrag->next = moof;
    } else {
        mFirstMovieFrag = moof;
    }
    mLastMovieFrag = moof;

    moof->offset = moofOffset;
    moof->sequenceNum = 0;
    moof->firstTRAF = NULL;
    moof->lastTRAF = NULL;

    off_t readOffset = moofOffset;
    uint32_t hdr[4];

    // skip over moof box type and size
    if (dataSource->readAt(readOffset, hdr, 8) < 8) {
        return ERROR_IO;
    }
    readOffset += 8;

    uint64_t boxSize = ntohl(hdr[0]);
    if (boxSize == 1) {
        // skip over the extended size
        readOffset += 8;
    }

    off_t dataSize = moofSize - (readOffset - moofOffset);
    uint32_t trafCount = 0;
    uint32_t trunCount = 0;
    uint64_t sampleDataOffset = moofOffset;
    uint64_t sampleOffsetForNextRun = moofOffset;
    bool tfhdHasOffset = false;
    bool firstSyncSampleInTRAF = true;

    while ((readOffset - moofOffset) < dataSize) {
        // look for a mfhd, a traf, a trun or a uuid
        if (dataSource->readAt(readOffset, hdr, 8) < 8) {
            return ERROR_IO;
        }
        readOffset += 8;

        uint64_t boxSize = ntohl(hdr[0]);
        uint32_t boxType = ntohl(hdr[1]);

        char box[5];
        MakeFourCCString(boxType, box);
        PathAdder autoAdder(&mPath, boxType);

        switch (boxType) {
            case FOURCC('m', 'f', 'h', 'd'): {
                if (boxSize == 1) {
                    // skip over the extended size
                    readOffset += 8;
                }
                // skip over version + flags
                readOffset += 4;
                uint8_t data[4];
                if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                    return ERROR_IO;
                }
                readOffset += 4;
                moof->sequenceNum = U32_AT(&data[0]);

                break;
            }
            case FOURCC('t', 'r', 'a', 'f'): {
                TrackFrag *traf = new TrackFrag;
                traf->next = NULL;
                if (moof->lastTRAF) {
                    moof->lastTRAF->next = traf;
                } else {
                    moof->firstTRAF = traf;
                }
                moof->lastTRAF = traf;
                trafCount++;
                trunCount = 0;
                firstSyncSampleInTRAF = true;

                traf->id = 0;
                traf->trafNumInParentMoof = trafCount;
                traf->offsetOfParentMoof = moofOffset;
                traf->sizeOfParentMoof = moofSize;
                traf->hasSyncSamples = false;
                traf->firstSyncSTrunNum = 0;
                traf->firstSyncSampleNum = 0;
                traf->firstSyncTimestamp = 0;
                traf->firstSampleTimestamp = 0;
                traf->fixTimestamps = false;
                traf->maxSampleSize = 0;
                traf->baseDataOffset = 0;
                traf->sampleDescIndex = 0;
                traf->defaultSampleDuration = 0;
                traf->defaultSampleSize = 0;
                traf->defaultSampleFlags = 0;
                traf->firstTrun = NULL;
                traf->lastTrun = NULL;
                traf->encryption = NULL;

                if (boxSize == 1) {
                    // skip over the extended size
                    readOffset += 8;
                }
                break;
            }
            case FOURCC('t', 'f', 'h', 'd'): {
                if (boxSize == 1) {
                    // skip over the extended size
                    readOffset += 8;
                }

                // skip version, read in flags
                uint8_t data[8];
                if (dataSource->readAt(readOffset + 1, &data[0], 3) < 3) {
                    return ERROR_IO;
                }
                readOffset += 4;
                uint32_t flags = U24_AT(&data[0]);
                if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                    return ERROR_IO;
                }
                readOffset += 4;
                moof->lastTRAF->id = U32_AT(&data[0]);

                if (flags & 0x000001) {
                    if (dataSource->readAt(readOffset, &data[0], 8) < 8) {
                        return ERROR_IO;
                    }
                    readOffset += 8;
                    tfhdHasOffset = true;
                    moof->lastTRAF->baseDataOffset = U64_AT(&data[0]);
                } else {
                    tfhdHasOffset = false;

                    TrackFrag *tmpTRAF = moof->firstTRAF;
                    uint32_t id = moof->lastTRAF->id;
                    bool found = false;
                    while (tmpTRAF != moof->lastTRAF) {
                        if (id == tmpTRAF->id) {
                            found = true;
                            break;
                        }
                        tmpTRAF = tmpTRAF->next;
                    }
                    if (found) {
                        moof->lastTRAF->baseDataOffset = sampleOffsetForNextRun;
                    } else {
                        moof->lastTRAF->baseDataOffset = moofOffset;
                    }
                }

                bool found = false;
                Track *track = mFirstTrack;
                while (!found && (NULL != track)) {
                    if (track->id != moof->lastTRAF->id) {
                        track = track->next;
                    } else {
                        found = true;
                    }
                }

                moof->lastTRAF->firstSampleTimestamp = track->nextTimestamp;

                if (flags & 0x000002) {
                    if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                        return ERROR_IO;
                    }
                    readOffset += 4;
                    moof->lastTRAF->sampleDescIndex = U32_AT(&data[0]);
                } else {
                    moof->lastTRAF->sampleDescIndex = track->defaultSampleDescIndex;
                }

                if (flags & 0x000008) {
                    if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                        return ERROR_IO;
                    }
                    readOffset += 4;
                    moof->lastTRAF->defaultSampleDuration = U32_AT(&data[0]);
                } else {
                    moof->lastTRAF->defaultSampleDuration = track->defaultSampleDuration;
                }

                if (flags & 0x000010) {
                    if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                        return ERROR_IO;
                    }
                    readOffset += 4;
                    moof->lastTRAF->defaultSampleSize = U32_AT(&data[0]);
                } else {
                    moof->lastTRAF->defaultSampleSize = track->defaultSampleSize;
                }

                if (flags & 0x000020) {
                    if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                        return ERROR_IO;
                    }
                    readOffset += 4;
                    moof->lastTRAF->defaultSampleFlags = U32_AT(&data[0]);
                } else {
                    moof->lastTRAF->defaultSampleFlags = track->defaultSampleFlags;
                }

                moof->lastTRAF->maxSampleSize = (track->defaultSampleSize > moof->lastTRAF->defaultSampleSize) ?
                                                    track->defaultSampleSize : moof->lastTRAF->defaultSampleSize;
                break;
            }

            case FOURCC('t', 'r', 'u', 'n'): {
                TrackFragRun *trun = new TrackFragRun;
                trun->next = NULL;
                if (moof->lastTRAF->lastTrun) {
                    moof->lastTRAF->lastTrun->next = trun;
                } else {
                    moof->lastTRAF->firstTrun = trun;
                }
                moof->lastTRAF->lastTrun = trun;

                trunCount++;

                bool found = false;
                Track *track = mFirstTrack;
                while (!found && (NULL != track)) {
                    if (track->id != moof->lastTRAF->id) {
                        track = track->next;
                    } else {
                        found = true;
                    }
                }

                trun->trunNumInParentTraf = trunCount;
                trun->entryCount = 0;
                trun->dataOffset = 0;
                trun->firstSampleFlags = 0;
                trun->firstSample = NULL;
                trun->lastSample = NULL;

                // find out if there are any sync samples in this run
                uint32_t numSyncSamples = 0;
                uint32_t *sampleNums = NULL;
                uint64_t *timestamps = NULL;
                if (syncSamples != NULL) {
                    sampleNums = (uint32_t *)malloc(syncSamples->entryCount * sizeof(uint32_t));
                    timestamps = (uint64_t *)malloc(syncSamples->entryCount * sizeof(uint64_t));
                    SyncSample *syncSample = syncSamples->firstSyncSample;
                    for (uint32_t idx = 0; idx < syncSamples->entryCount; idx++) {
                        if ((syncSample->entry->trafNumber == trafCount) &&
                                (syncSample->entry->trunNumber == trunCount)) {
                            sampleNums[numSyncSamples] = syncSample->entry->sampleNumber;
                            timestamps[numSyncSamples] = syncSample->entry->time64;
                            numSyncSamples++;
                        }
                        syncSample = syncSample->next;
                    }
                }

                if (numSyncSamples) {
                    moof->lastTRAF->hasSyncSamples = true;
                }

                if (boxSize == 1) {
                    // skip over the extended size
                    readOffset += 8;
                }
                // skip version, read in flags
                uint8_t data[4];
                if (dataSource->readAt(readOffset + 1, &data[0], 3) < 3) {
                    return ERROR_IO;
                }
                readOffset += 4;
                uint32_t flags = U24_AT(&data[0]);

                if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                    return ERROR_IO;
                }
                readOffset += 4;
                trun->entryCount = U32_AT(&data[0]);

                if (flags & 0x000001) {
                    if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                        return ERROR_IO;
                    }
                    readOffset += 4;
                    trun->dataOffset = moof->lastTRAF->baseDataOffset;
                    trun->dataOffset += U32_AT(&data[0]);
                } else {
                    if (1 == trunCount) {
                        if (tfhdHasOffset) {
                            trun->dataOffset = moof->lastTRAF->baseDataOffset;
                        } else {
                            trun->dataOffset = moofOffset + moofSize + 8;
                        }
                    } else {
                        trun->dataOffset = sampleOffsetForNextRun;
                    }
                }

                if (flags & 0x000004) {
                    if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                        return ERROR_IO;
                    }
                    readOffset += 4;
                    trun->firstSampleFlags = U32_AT(&data[0]);
                }

                uint32_t sampleNum = 0;
                uint64_t sampleOffset = trun->dataOffset;
                for (uint32_t idx = 0; idx < trun->entryCount; idx++) {
                    Sample *entry = new Sample;
                    entry->next = NULL;
                    if (trun->lastSample) {
                        trun->lastSample->next = entry;
                    } else {
                        trun->firstSample = entry;
                    }
                    trun->lastSample = entry;

                    sampleNum++;

                    entry->sampleNumInParentTrun = sampleNum;
                    entry->sampleDuration = moof->lastTRAF->defaultSampleDuration;
                    entry->sampleSize = moof->lastTRAF->defaultSampleSize;
                    entry->sampleFlags = moof->lastTRAF->defaultSampleFlags;
                    entry->sampleCompTimeOffset = 0;

                    if (flags & 0x000100) {
                        if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                            return ERROR_IO;
                        }
                        readOffset += 4;
                        entry->sampleDuration = U32_AT(&data[0]);
                    }

                    if (flags & 0x000200) {
                        if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                            return ERROR_IO;
                        }
                        readOffset += 4;
                        entry->sampleSize = U32_AT(&data[0]);
                    }

                    if (flags & 0x000400) {
                        if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                            return ERROR_IO;
                        }
                        readOffset += 4;
                        entry->sampleFlags = U32_AT(&data[0]);
                    }

                    if (flags & 0x000800) {
                        if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                            return ERROR_IO;
                        }
                        readOffset += 4;
                        entry->sampleCompTimeOffset = U32_AT(&data[0]);
                    }

                    if (entry->sampleSize > moof->lastTRAF->maxSampleSize) {
                        moof->lastTRAF->maxSampleSize = entry->sampleSize;
                    }

                    entry->sampleTimestamp = track->nextTimestamp;

                    if ((numSyncSamples > 0) && (NULL != sampleNums) && (NULL != timestamps)) {
                        // see if this is a sync sample
                        for (uint32_t idx = 0; idx < numSyncSamples; idx++) {
                            if (sampleNum == sampleNums[idx]) {
                                if (firstSyncSampleInTRAF) {
                                    // this is the first sync sample in the track fragment
                                    firstSyncSampleInTRAF = false;
                                    if ((trunCount > 1) || (sampleNum > 1)) {
                                        // the earlier samples in this traf may not have the correct timestamp
                                        // compare the sync sample timestamp with track time
                                        if (entry->sampleTimestamp != timestamps[idx]) {
                                            // need to fix the previous samples
                                            moof->lastTRAF->firstSyncSTrunNum = trunCount;
                                            moof->lastTRAF->firstSyncSampleNum = sampleNum;
                                            moof->lastTRAF->firstSyncTimestamp = entry->sampleTimestamp;
                                            moof->lastTRAF->fixTimestamps = true;

                                            // calculate the first sample timestamp in this traf
                                            uint64_t firstTS = moof->lastTRAF->firstTrun->firstSample->sampleTimestamp;
                                            uint64_t deltaTS = entry->sampleTimestamp - firstTS;

                                            moof->lastTRAF->firstSampleTimestamp =  timestamps[idx] - deltaTS;
                                        }
                                    }
                                }
                                entry->sampleTimestamp = timestamps[idx];
                                track->nextTimestamp = timestamps[idx];
                                break;
                            }
                        }
                    }

                    track->nextTimestamp += entry->sampleDuration;

                    entry->sampleDataOffset = sampleOffset;
                    sampleOffset += entry->sampleSize;
                } // end for

                sampleOffsetForNextRun = sampleOffset;
                break;
            }

            case FOURCC('u', 'u', 'i', 'd') : {
                // read in extended type, 16 bytes
                // check for Sample Encryption box (PIFF extension)
                off_t skipSize = boxSize - 16;
                if (boxSize == 1) {
                    if (dataSource->readAt(readOffset, &boxSize, 8) < 8) {
                        return ERROR_IO;
                    }
                    readOffset += 8;
                    boxSize = ntoh64(boxSize);
                    skipSize = boxSize - 24;
                }

                uint8_t data[16];
                if (dataSource->readAt(readOffset, &data[0], 16) < 16) {
                    return ERROR_IO;
                }
                readOffset += 16;

                bool found = true;
                for (uint32_t idx = 0; idx < 16; idx++) {
                    if (data[idx] != SAMPLE_ENCRYPTION_BOX_UUID[idx]) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    // skip version, read in flags
                    if (dataSource->readAt(readOffset + 1, &data[0], 3) < 3) {
                        return ERROR_IO;
                    }
                    readOffset += 4;
                    uint32_t flags = U24_AT(&data[0]);

                    SampleEncryption *encrypt = new SampleEncryption;
                    moof->lastTRAF->encryption = encrypt;

                    encrypt->algorithmID = 0;
                    encrypt->initVecSize = 0;
                    for (uint32_t ii = 0; ii < sizeof(encrypt->keyID); ii++) {
                        encrypt->keyID[ii] = 0;
                    }
                    encrypt->sampleCount = 0;
                    encrypt->firstInfo = NULL;
                    encrypt->lastInfo = NULL;

                    if (flags & 0x000001) {
                        if (dataSource->readAt(readOffset, &data[0], 3) < 3) {
                            return ERROR_IO;
                        }
                        readOffset += 3;
                        encrypt->algorithmID = U24_AT(&data[0]);

                        if (dataSource->readAt(readOffset, &data[0], 1) < 1) {
                            return ERROR_IO;
                        }

                        readOffset += 1;
                        encrypt->initVecSize = data[0];

                        if (dataSource->readAt(readOffset, &encrypt->keyID[0], 16) < 16) {
                            return ERROR_IO;
                        }
                        readOffset += 16;
                    }

                    if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                        return ERROR_IO;
                    }
                    readOffset += 4;
                    encrypt->sampleCount = U32_AT(&data[0]);

                    for (uint32_t ii = 0; ii < encrypt->sampleCount; ii++) {
                        EncryptionInfo *info = new EncryptionInfo;
                        info->next = NULL;
                        if (encrypt->lastInfo) {
                            encrypt->lastInfo->next = info;
                        } else {
                            encrypt->firstInfo = info;
                        }
                        encrypt->lastInfo = info;

                        info->initVec = NULL;
                        info->entryCount = 0;
                        info->firstEntry = NULL;
                        info->lastEntry = NULL;

                        info->initVec = (uint8_t *)malloc(encrypt->initVecSize);
                        if (NULL == info->initVec) {
                            return -ENOMEM;
                        }

                        if (dataSource->readAt(readOffset, info->initVec, encrypt->initVecSize) < encrypt->initVecSize) {
                            return ERROR_IO;
                        }
                        readOffset += encrypt->initVecSize;

                        if (flags & 0x000002) {
                            if (dataSource->readAt(readOffset, &data[0], 2) < 2) {
                                return ERROR_IO;
                            }
                            readOffset += 2;
                            info->entryCount = U16_AT(&data[0]);

                            for (uint32_t jj = 0; jj < info->entryCount; jj++) {
                                SubSampleMappingData *entry = new SubSampleMappingData;
                                entry->next = NULL;
                                if (info->lastEntry) {
                                    info->lastEntry->next = entry;
                                } else {
                                    info->firstEntry = entry;
                                }
                                info->lastEntry = entry;

                                entry->clearBytes = 0;
                                entry->encryptedbytes = 0;

                                if (dataSource->readAt(readOffset, &data[0], 2) < 2) {
                                    return ERROR_IO;
                                }
                                readOffset += 2;
                                entry->clearBytes = U16_AT(&data[0]);

                                if (dataSource->readAt(readOffset, &data[0], 4) < 4) {
                                    return ERROR_IO;
                                }
                                readOffset += 4;
                                entry->encryptedbytes = U32_AT(&data[0]);
                            } // end for
                        } // end if
                    } // end for
                } // end if
                break;
            }
            default: {
                // skip this box
                off_t skipSize = boxSize - 16;
                if (boxSize == 1) {
                    if (dataSource->readAt(readOffset, hdr, 8) < 8) {
                        return ERROR_IO;
                    }
                    boxSize = ntoh64(boxSize);
                    skipSize = boxSize - 24;
                }
                readOffset += skipSize;
                break;
            }
        } // end switch
    } // end while

    // update the track fragment tables
    TrackFrag* trackFrag = moof->firstTRAF;
    while (NULL != trackFrag) {
        Track * track = mFirstTrack;
        while (NULL != track) {
            if (track->id == trackFrag->id) {
                if (!track->hasFragmentTable) {
                    // mvex and mfra not available
                    // create table now
                    bool isVideoTrack = false;
                    const char *mime;
                    CHECK(track->meta->findCString(kKeyMIMEType, &mime));
                    if (!strncasecmp("video/", mime, 6)) {
                        isVideoTrack = true;
                    }
                    track->fragmentTable = new TrackFragmentTable(track->id, track->timescale, isVideoTrack);
                    track->hasFragmentTable = true;
                }

                if (trackFrag->fixTimestamps) {
                    // fix up the timestamps in the samples before the first sync sample in the traf
                    uint64_t timestamp = trackFrag->firstSampleTimestamp;

                    trunCount = 1;
                    bool done = false;
                    uint32_t sampleNum = 0;
                    TrackFragRun *trun = trackFrag->firstTrun;
                    while (NULL != trun && !done) {
                        if (trunCount < trackFrag->firstSyncSTrunNum) {
                            // all samples in this run needs fixing
                            Sample *sample = trun->firstSample;
                            while (NULL != sample) {
                                sample->sampleTimestamp = timestamp;
                                timestamp += sample->sampleDuration;
                                sample = sample->next;
                            }
                            trunCount++;
                            trun = trun->next;
                        } else if (trunCount == trackFrag->firstSyncSTrunNum) {
                            // some samples in this run needs fixing
                            sampleNum = 1;
                            Sample *sample = trun->firstSample;
                            while (NULL != sample) {
                                if (sampleNum < trackFrag->firstSyncSampleNum) {
                                    sample->sampleTimestamp = timestamp;
                                    timestamp += sample->sampleDuration;
                                    sample = sample->next;
                                    sampleNum++;
                                } else {
                                    done = true;
                                    break;
                                }
                            }
                        } else {
                            done = true;
                            break;
                        }
                    } // end while
                    trackFrag->fixTimestamps = false;
                } // end if

                track->fragmentTable->updateTable(trackFrag);
                size_t newMaxSize = 0;
                track->fragmentTable->getMaxSampleSize(&newMaxSize);
                int32_t maxSize = 0;
                track->meta->findInt32(kKeyMaxInputSize, &maxSize);
                if (newMaxSize > (size_t)maxSize) {
                    track->meta->setInt32(kKeyMaxInputSize, newMaxSize + 10 * 2);
                }
            } // end if
            track = track->next;
        } // end while
        trackFrag = trackFrag->next;
    } // end while

    return OK;
}

sp<MediaSource> MPEG4Extractor::getTrack(size_t index) {
    status_t err;
    if ((err = readMetaData()) != OK) {
        return NULL;
    }

    Track *track = mFirstTrack;
    while (index > 0) {
        if (track == NULL) {
            return NULL;
        }

        track = track->next;
        --index;
    }

    if (track == NULL) {
        return NULL;
    }

    return new MPEG4Source(
            track->meta, mDataSource, track->timescale, track->sampleTable,
            track->fragmentTable, mHasFragments, &track->nextTimestamp, this);
}

// static
status_t MPEG4Extractor::verifyTrack(Track *track) {
    const char *mime;
    CHECK(track->meta->findCString(kKeyMIMEType, &mime));

    uint32_t type;
    const void *data;
    size_t size;
    if (!strcasecmp(mime, MEDIA_MIMETYPE_VIDEO_AVC)) {
        if (!track->meta->findData(kKeyAVCC, &type, &data, &size)
                || type != kTypeAVCC) {
            return ERROR_MALFORMED;
        }
    } else if (!strcasecmp(mime, MEDIA_MIMETYPE_VIDEO_MPEG4)
            || !strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_AAC)) {
        if (!track->meta->findData(kKeyESDS, &type, &data, &size)
                || type != kTypeESDS) {
            return ERROR_MALFORMED;
        }
    }

    return OK;
}

status_t MPEG4Extractor::updateAudioTrackInfoFromESDS_MPEG4Audio(
        const void *esds_data, size_t esds_size) {
    ESDS esds(esds_data, esds_size);

    uint8_t objectTypeIndication;
    if (esds.getObjectTypeIndication(&objectTypeIndication) != OK) {
        return ERROR_MALFORMED;
    }

    if (objectTypeIndication == 0xe1) {
        // This isn't MPEG4 audio at all, it's QCELP 14k...
        mLastTrack->meta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_QCELP);
        return OK;
    }

    const uint8_t *csd;
    size_t csd_size;
    if (esds.getCodecSpecificInfo(
                (const void **)&csd, &csd_size) != OK) {
        return ERROR_MALFORMED;
    }

#if 0
    printf("ESD of size %d\n", csd_size);
    hexdump(csd, csd_size);
#endif

    if (csd_size < 2) {
        return ERROR_MALFORMED;
    }

    uint32_t objectType = csd[0] >> 3;

    if (objectType == 31) {
        return ERROR_UNSUPPORTED;
    }

    uint32_t freqIndex = (csd[0] & 7) << 1 | (csd[1] >> 7);
    int32_t sampleRate = 0;
    int32_t numChannels = 0;
    if (freqIndex == 15) {
        if (csd_size < 5) {
            return ERROR_MALFORMED;
        }

        sampleRate = (csd[1] & 0x7f) << 17
                        | csd[2] << 9
                        | csd[3] << 1
                        | (csd[4] >> 7);

        numChannels = (csd[4] >> 3) & 15;
    } else {
        static uint32_t kSamplingRate[] = {
            96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050,
            16000, 12000, 11025, 8000, 7350
        };

        if (freqIndex == 13 || freqIndex == 14) {
            return ERROR_MALFORMED;
        }

        sampleRate = kSamplingRate[freqIndex];
        numChannels = (csd[1] >> 3) & 15;
    }

    if (numChannels == 0) {
        return ERROR_UNSUPPORTED;
    }

    int32_t prevSampleRate;
    CHECK(mLastTrack->meta->findInt32(kKeySampleRate, &prevSampleRate));

    if (prevSampleRate != sampleRate) {
        LOGV("mpeg4 audio sample rate different from previous setting. "
             "was: %d, now: %d", prevSampleRate, sampleRate);
    }

    mLastTrack->meta->setInt32(kKeySampleRate, sampleRate);

    int32_t prevChannelCount;
    CHECK(mLastTrack->meta->findInt32(kKeyChannelCount, &prevChannelCount));

    if (prevChannelCount != numChannels) {
        LOGV("mpeg4 audio channel count different from previous setting. "
             "was: %d, now: %d", prevChannelCount, numChannels);
    }

    mLastTrack->meta->setInt32(kKeyChannelCount, numChannels);

    return OK;
}

////////////////////////////////////////////////////////////////////////////////

MPEG4Source::MPEG4Source(
        const sp<MetaData> &format,
        const sp<DataSource> &dataSource,
        int32_t timeScale,
        const sp<SampleTable> &sampleTable,
        const sp<TrackFragmentTable> &fragmentTable,
        bool moofPresent,
        uint64_t *nextFragmentTimestamp,
        const sp<MPEG4Extractor> &parentExtractor)
    : mFormat(format),
      mDataSource(dataSource),
      mTimescale(timeScale),
      mSampleTable(sampleTable),
      mFragmentTable(fragmentTable),
      mHasFragments(moofPresent),
      mParentExtractor(parentExtractor),
      mCurrentSampleIndex(0),
      mNextFragmentTimestamp(nextFragmentTimestamp),
      mIsAVC(false),
      mNALLengthSize(0),
      mStarted(false),
      mGroup(NULL),
      mBuffer(NULL),
      mWantsNALFragments(false),
      mSrcBuffer(NULL) {

    const char *mime;
    bool success = mFormat->findCString(kKeyMIMEType, &mime);
    CHECK(success);

    mIsAVC = !strcasecmp(mime, MEDIA_MIMETYPE_VIDEO_AVC);

    if (mIsAVC) {
        uint32_t type;
        const void *data;
        size_t size;
        CHECK(format->findData(kKeyAVCC, &type, &data, &size));

        const uint8_t *ptr = (const uint8_t *)data;

        CHECK(size >= 7);
        CHECK_EQ(ptr[0], 1);  // configurationVersion == 1

        // The number of bytes used to encode the length of a NAL unit.
        mNALLengthSize = 1 + (ptr[4] & 3);
    }
}

MPEG4Source::~MPEG4Source() {
    if (mStarted) {
        stop();
    }
}

status_t MPEG4Source::start(MetaData *params) {
    Mutex::Autolock autoLock(mLock);

    CHECK(!mStarted);

    int32_t val;
    if (params && params->findInt32(kKeyWantsNALFragments, &val)
        && val != 0) {
        mWantsNALFragments = true;
    } else {
        mWantsNALFragments = false;
    }

    mGroup = new MediaBufferGroup;

    int32_t max_size;
    CHECK(mFormat->findInt32(kKeyMaxInputSize, &max_size));

    mGroup->add_buffer(new MediaBuffer(max_size));

    mSrcBuffer = new uint8_t[max_size];

    mStarted = true;

    return OK;
}

status_t MPEG4Source::stop() {
    Mutex::Autolock autoLock(mLock);

    CHECK(mStarted);

    if (mBuffer != NULL) {
        mBuffer->release();
        mBuffer = NULL;
    }

    delete[] mSrcBuffer;
    mSrcBuffer = NULL;

    delete mGroup;
    mGroup = NULL;

    mStarted = false;
    mCurrentSampleIndex = 0;

    return OK;
}

sp<MetaData> MPEG4Source::getFormat() {
    Mutex::Autolock autoLock(mLock);

    return mFormat;
}

size_t MPEG4Source::parseNALSize(const uint8_t *data) const {
    switch (mNALLengthSize) {
        case 1:
            return *data;
        case 2:
            return U16_AT(data);
        case 3:
            return ((size_t)data[0] << 16) | U16_AT(&data[1]);
        case 4:
            return U32_AT(data);
    }

    // This cannot happen, mNALLengthSize springs to life by adding 1 to
    // a 2-bit integer.
    CHECK(!"Should not be here.");

    return 0;
}

status_t MPEG4Source::read(
        MediaBuffer **out, const ReadOptions *options) {
    Mutex::Autolock autoLock(mLock);

    CHECK(mStarted);

    *out = NULL;

    int64_t seekTimeUs;
    off_t offset = 0;
    size_t size = 0;
    uint32_t dts = 0;
    status_t err = OK;

    if (!mHasFragments) {
        // no movie fragments, use sample table
        if (options && options->getSeekTo(&seekTimeUs)) {
            uint32_t sampleIndex;
            err = mSampleTable->findClosestSample(
                    seekTimeUs * mTimescale / 1000000,
                    &sampleIndex, SampleTable::kSyncSample_Flag);

            if (err != OK) {
                if (err == ERROR_OUT_OF_RANGE) {
                    // An attempt to seek past the end of the stream would
                    // normally cause this ERROR_OUT_OF_RANGE error. Propagating
                    // this all the way to the MediaPlayer would cause abnormal
                    // termination. Legacy behaviour appears to be to behave as if
                    // we had seeked to the end of stream, ending normally.
                    err = ERROR_END_OF_STREAM;
                }
                return err;
            }

            mCurrentSampleIndex = sampleIndex;

            if (mBuffer != NULL) {
                mBuffer->release();
                mBuffer = NULL;
            }
            // fall through
        }

        if (mBuffer == NULL) {
            err = mSampleTable->getMetaDataForSample(
                    mCurrentSampleIndex, &offset, &size, &dts);

            if (err != OK) {
                return err;
            }
        }
    } else {
        // has movie fragments, use fragment table
        off_t moofOffset = 0;
        uint64_t nextTimestamp = 0;

        if (options && options->getSeekTo(&seekTimeUs)) {
            uint64_t sampleIndex;
            err = mFragmentTable->findClosestSample(
                    seekTimeUs * mTimescale / 1000000,
                    TrackFragmentTable::kSyncSample_Flag,
                    &offset, &size, &dts, &moofOffset, &nextTimestamp);
            // if the moof has not been parsed,
            // we need to parse it and try again
            if (err == ERROR_NOT_YET_PARSED) {
                *mNextFragmentTimestamp = nextTimestamp;
                err = mParentExtractor->parseNextMOOF(moofOffset, mDataSource);
                if (err == OK) {
                    err = mFragmentTable->findClosestSample(
                            seekTimeUs * mTimescale / 1000000,
                            TrackFragmentTable::kSyncSample_Flag,
                            &offset, &size, &dts, &moofOffset, &nextTimestamp);
                }
            }

            if (err != OK) {
                if (err == ERROR_OUT_OF_RANGE) {
                    // An attempt to seek past the end of the stream would
                    // normally cause this ERROR_OUT_OF_RANGE error. Propagating
                    // this all the way to the MediaPlayer would cause abnormal
                    // termination. Legacy behaviour appears to be to behave as if
                    // we had seeked to the end of stream, ending normally.
                    err = ERROR_END_OF_STREAM;
                }
                return err;
            }

            if (mBuffer != NULL) {
                mBuffer->release();
                mBuffer = NULL;
            }
        } else {
            if (mBuffer == NULL) {
                err = mFragmentTable->getMetaDataForNextSample(
                        &offset, &size, &dts, &moofOffset, &nextTimestamp);

                if (err == ERROR_NOT_YET_PARSED) {
                    // no sample returned
                    // parse next moof box
                    // try getting metadata for sample again
                    *mNextFragmentTimestamp = nextTimestamp;
                    err = mParentExtractor->parseNextMOOF(moofOffset, mDataSource);
                    while (1) {
                        if (err != OK) {
                            // end of data reached
                            return ERROR_END_OF_STREAM;
                        }
                        err = mFragmentTable->getMetaDataForNextSample(
                                   &offset, &size, &dts, &moofOffset, &nextTimestamp);
                        if (err == OK) {
                            // found
                            break;
                        }
                        // look at the next moof
                        err = mParentExtractor->parseNextMOOF(mDataSource);
                    }
                }
            }
        }
    } // end else

    bool newBuffer = false;
    if (mBuffer == NULL) {
        newBuffer = true;
        err = mGroup->acquire_buffer(&mBuffer);

        if (err != OK) {
            CHECK_EQ(mBuffer, NULL);
            return err;
        }
        if (mHasFragments) {
            // make sure the buffer is big enough
            // if not, allocate a bigger buffer
            size_t bufSize = mBuffer->size();
            int32_t maxSize = 0;

            mFormat->findInt32(kKeyMaxInputSize, &maxSize);
            if (bufSize < (size_t)maxSize) {
                // delete old buffer
                mBuffer->release();
                mBuffer = NULL;

                delete mGroup;
                mGroup = NULL;

                delete[] mSrcBuffer;
                mSrcBuffer = NULL;

                // allocate new buffer
                mGroup = new MediaBufferGroup;

                mGroup->add_buffer(new MediaBuffer(maxSize));

                mSrcBuffer = new uint8_t[maxSize];

                err = mGroup->acquire_buffer(&mBuffer);
                if (err != OK) {
                    CHECK_EQ(mBuffer, NULL);
                    return err;
                }
            }
        }
    }

    if (!mIsAVC || mWantsNALFragments) {
        if (newBuffer) {
            ssize_t num_bytes_read =
                mDataSource->readAt(offset, (uint8_t *)mBuffer->data(), size);

            if (num_bytes_read < (ssize_t)size) {
                mBuffer->release();
                mBuffer = NULL;
                return ERROR_IO;
            }

            CHECK(mBuffer != NULL);
            mBuffer->set_range(0, size);
            mBuffer->meta_data()->clear();
            mBuffer->meta_data()->setInt64(
                    kKeyTime, ((int64_t)dts * 1000000) / mTimescale);
            ++mCurrentSampleIndex;
        }

        if (!mIsAVC) {
            *out = mBuffer;
            mBuffer = NULL;
            return OK;
        }

        // Each NAL unit is split up into its constituent fragments and
        // each one of them returned in its own buffer.

        CHECK(mBuffer->range_length() >= mNALLengthSize);

        const uint8_t *src =
            (const uint8_t *)mBuffer->data() + mBuffer->range_offset();

        size_t nal_size = parseNALSize(src);
        if (mBuffer->range_length() < mNALLengthSize + nal_size) {
            LOGE("incomplete NAL unit.");

            mBuffer->release();
            mBuffer = NULL;
            return ERROR_MALFORMED;
        }

        MediaBuffer *clone = mBuffer->clone();
        CHECK(clone != NULL);
        clone->set_range(mBuffer->range_offset() + mNALLengthSize, nal_size);

        CHECK(mBuffer != NULL);
        mBuffer->set_range(
                mBuffer->range_offset() + mNALLengthSize + nal_size,
                mBuffer->range_length() - mNALLengthSize - nal_size);

        if (mBuffer->range_length() == 0) {
            mBuffer->release();
            mBuffer = NULL;
        }

        *out = clone;
        return OK;
    } else {
        // Whole NAL units are returned but each fragment is prefixed by
        // the start code (0x00 00 00 01).

        ssize_t num_bytes_read =
            mDataSource->readAt(offset, mSrcBuffer, size);

        if (num_bytes_read < (ssize_t)size) {
            mBuffer->release();
            mBuffer = NULL;
            return ERROR_IO;
        }

        uint8_t *dstData = (uint8_t *)mBuffer->data();
        size_t srcOffset = 0;
        size_t dstOffset = 0;

        while (srcOffset < size) {
            CHECK(srcOffset + mNALLengthSize <= size);
            size_t nalLength = parseNALSize(&mSrcBuffer[srcOffset]);
            srcOffset += mNALLengthSize;

            if (srcOffset + nalLength > size) {
                mBuffer->release();
                mBuffer = NULL;
                return ERROR_MALFORMED;
            }

            if (nalLength == 0) {
                continue;
            }

            CHECK(dstOffset + 4 <= mBuffer->size());

            dstData[dstOffset++] = 0;
            dstData[dstOffset++] = 0;
            dstData[dstOffset++] = 0;
            dstData[dstOffset++] = 1;
            memcpy(&dstData[dstOffset], &mSrcBuffer[srcOffset], nalLength);
            srcOffset += nalLength;
            dstOffset += nalLength;
        }
        CHECK_EQ(srcOffset, size);

        CHECK(mBuffer != NULL);
        mBuffer->set_range(0, dstOffset);
        mBuffer->meta_data()->clear();
        mBuffer->meta_data()->setInt64(
                kKeyTime, ((int64_t)dts * 1000000) / mTimescale);
        ++mCurrentSampleIndex;

        *out = mBuffer;
        mBuffer = NULL;
        return OK;
    }
}

bool SniffMPEG4(
        const sp<DataSource> &source, String8 *mimeType, float *confidence) {
    uint8_t header[8];

    ssize_t n = source->readAt(4, header, sizeof(header));
    if (n < (ssize_t)sizeof(header)) {
        return false;
    }

    if (!memcmp(header, "ftyp3gp", 7) || !memcmp(header, "ftypmp42", 8)
        || !memcmp(header, "ftyp3gr6", 8) || !memcmp(header, "ftyp3gs6", 8)
        || !memcmp(header, "ftyp3ge6", 8) || !memcmp(header, "ftyp3gg6", 8)
        || !memcmp(header, "ftypisom", 8) || !memcmp(header, "ftypM4V ", 8)
        || !memcmp(header, "ftypM4A ", 8) || !memcmp(header, "ftypf4v ", 8)
        || !memcmp(header, "ftypkddi", 8) || !memcmp(header, "ftypM4VP", 8)
        || !memcmp(header, "ftypmmp4", 8) || !memcmp(header, "ftypisml", 8)) {
        *mimeType = MEDIA_MIMETYPE_CONTAINER_MPEG4;
        *confidence = 0.1;

        return true;
    }

    return false;
}

}  // namespace android

