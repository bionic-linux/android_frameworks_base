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

//#define LOG_NDEBUG 0
#define LOG_TAG "StagefrightMediaScanner"
#include <utils/Log.h>

#include <media/stagefright/StagefrightMediaScanner.h>

#include <media/mediametadataretriever.h>
#include <private/media/VideoFrame.h>

// Sonivox includes
#include <libsonivox/eas.h>

// Ogg Vorbis includes
#include <Tremolo/ivorbiscodec.h>
#include <Tremolo/ivorbisfile.h>

// WavPack include
extern "C" {
#include "wavpack.h"
}

// Needed for WavPack functions 
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>


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


namespace android {

StagefrightMediaScanner::StagefrightMediaScanner()
    : mRetriever(new MediaMetadataRetriever) {
}

StagefrightMediaScanner::~StagefrightMediaScanner() {}

static bool FileHasAcceptableExtension(const char *extension) {
    static const char *kValidExtensions[] = {
        ".mp3", ".mp4", ".m4a", ".3gp", ".3gpp", ".3g2", ".3gpp2",
        ".mpeg", ".ogg", ".mid", ".smf", ".imy", ".wma", ".aac",
        ".wav", ".amr", ".midi", ".xmf", ".rtttl", ".rtx", ".ota",
        ".wv"
    };
    static const size_t kNumValidExtensions =
        sizeof(kValidExtensions) / sizeof(kValidExtensions[0]);

    for (size_t i = 0; i < kNumValidExtensions; ++i) {
        if (!strcasecmp(extension, kValidExtensions[i])) {
            return true;
        }
    }

    return false;
}

static status_t HandleMIDI(
        const char *filename, MediaScannerClient *client) {
    // get the library configuration and do sanity check
    const S_EAS_LIB_CONFIG* pLibConfig = EAS_Config();
    if ((pLibConfig == NULL) || (LIB_VERSION != pLibConfig->libVersion)) {
        LOGE("EAS library/header mismatch\n");
        return UNKNOWN_ERROR;
    }
    EAS_I32 temp;

    // spin up a new EAS engine
    EAS_DATA_HANDLE easData = NULL;
    EAS_HANDLE easHandle = NULL;
    EAS_RESULT result = EAS_Init(&easData);
    if (result == EAS_SUCCESS) {
        EAS_FILE file;
        file.path = filename;
        file.fd = 0;
        file.offset = 0;
        file.length = 0;
        result = EAS_OpenFile(easData, &file, &easHandle);
    }
    if (result == EAS_SUCCESS) {
        result = EAS_Prepare(easData, easHandle);
    }
    if (result == EAS_SUCCESS) {
        result = EAS_ParseMetaData(easData, easHandle, &temp);
    }
    if (easHandle) {
        EAS_CloseFile(easData, easHandle);
    }
    if (easData) {
        EAS_Shutdown(easData);
    }

    if (result != EAS_SUCCESS) {
        return UNKNOWN_ERROR;
    }

    char buffer[20];
    sprintf(buffer, "%ld", temp);
    if (!client->addStringTag("duration", buffer)) return UNKNOWN_ERROR;

    return OK;
}

static status_t HandleOGG(
        const char *filename, MediaScannerClient *client) {
    int duration;

    FILE *file = fopen(filename,"r");
    if (!file)
        return UNKNOWN_ERROR;

    OggVorbis_File vf;
    if (ov_open(file, &vf, NULL, 0) < 0) {
        return UNKNOWN_ERROR;
    }

    char **ptr=ov_comment(&vf,-1)->user_comments;
    while(*ptr){
        char *val = strstr(*ptr, "=");
        if (val) {
            int keylen = val++ - *ptr;
            char key[keylen + 1];
            strncpy(key, *ptr, keylen);
            key[keylen] = 0;
            if (!client->addStringTag(key, val)) goto failure;
        }
        ++ptr;
    }

    // Duration
    duration = ov_time_total(&vf, -1);
    if (duration > 0) {
        char buffer[20];
        sprintf(buffer, "%d", duration);
        if (!client->addStringTag("duration", buffer)) goto failure;
    }

    ov_clear(&vf); // this also closes the FILE
    return OK;

failure:
    ov_clear(&vf); // this also closes the FILE
    return UNKNOWN_ERROR;
}

static status_t HandleWavPack(const char *filename, MediaScannerClient *client)
{
    uint32_t sample_rate;
    char errorBuff[128];
    char buffer[60];
    char value [128];
    uint64_t num_samples = 0;
    uint32_t songduration = 0;

    WavpackContext *wpc;
    int open_flags;
    FILE *f = NULL;

    f = fopen(filename, "r");
    if (!f)
        return UNKNOWN_ERROR;

    open_flags = OPEN_TAGS | OPEN_2CH_MAX | OPEN_NORMALIZE;

    wpc = WavpackOpenFileInputEx (&freader, f, NULL, errorBuff, open_flags, 0);

    if (wpc == NULL) {
        goto tidyup;
    }

    num_samples = WavpackGetNumSamples(wpc);
    sample_rate = WavpackGetSampleRate(wpc);

    songduration = num_samples*1000 / sample_rate;
    sprintf(buffer, "%d", songduration);
    if (!client->addStringTag("duration", buffer)) goto tidyup;

    if (WavpackGetTagItem (wpc, "title", value, sizeof (value))) {
        if (!client->addStringTag("title", value)) goto tidyup;
    }

    if (WavpackGetTagItem (wpc, "album", value, sizeof (value))) {
        if (!client->addStringTag("album", value)) goto tidyup;
    }

    if (WavpackGetTagItem (wpc, "artist", value, sizeof (value))) {
        if (!client->addStringTag("artist", value)) goto tidyup;
    }

    if (WavpackGetTagItem (wpc, "year", value, sizeof (value))) {
        if (!client->addStringTag("year", value)) goto tidyup;
    }

    if (WavpackGetTagItem (wpc, "track", value, sizeof (value))) {
        if (!client->addStringTag("track", value)) goto tidyup;
    }

    if (WavpackGetTagItem (wpc, "genre", value, sizeof (value))) {
        if (!client->addStringTag("genre", value)) goto tidyup;
    }

    wpc = WavpackCloseFile(wpc);

    return OK;


tidyup:
    if(wpc != NULL) {
        wpc = WavpackCloseFile(wpc);
    }

    return UNKNOWN_ERROR;
}

static char* extractWavPackAlbumArt(int fd)
{
    WavpackContext *wpc;
    int open_flags;
    char errorBuff[128];
    char value[128];
    int art_size;

    FILE *f = fdopen(fd, "r");
    if (!f)
        return NULL;

    open_flags = OPEN_TAGS | OPEN_2CH_MAX | OPEN_NORMALIZE;

    wpc = WavpackOpenFileInputEx (&freader, f, NULL, errorBuff, open_flags, 0);
    if(wpc==NULL) {
        goto tidyup;
    }

    art_size = WavpackGetBinaryTagItem (wpc, "cover art (front)", NULL, 0);

    if(art_size > 0) {
        char *embeddedart = (char*)malloc(art_size);
        if(embeddedart) {
            WavpackGetBinaryTagItem (wpc, "cover art (front)", embeddedart, art_size);

            int fileNameLength = strlen((char*)embeddedart); // get the filename at the start of the data
            fileNameLength += 1; // skip an extra character to handle the null character at end of filename string

            int size_remaining = art_size - fileNameLength;

            char *data = (char*)malloc(size_remaining + 4);
            if(data) {
                long *len = (long*)data;
                *len = size_remaining;

                memcpy(data + 4, embeddedart+fileNameLength, size_remaining);

                free(embeddedart);
                wpc = WavpackCloseFile(wpc);

                return data;
            } else {
                free(embeddedart);
            }
        }
    }

    wpc = WavpackCloseFile(wpc);

    return NULL;


tidyup:

    if(wpc != NULL) {
        wpc = WavpackCloseFile(wpc);
    }
    return NULL;
}



status_t StagefrightMediaScanner::processFile(
        const char *path, const char *mimeType,
        MediaScannerClient &client) {
    LOGV("processFile '%s'.", path);

    client.setLocale(locale());
    client.beginFile();

    const char *extension = strrchr(path, '.');

    if (!extension) {
        return UNKNOWN_ERROR;
    }

    if (!FileHasAcceptableExtension(extension)) {
        client.endFile();

        return UNKNOWN_ERROR;
    }

    if (!strcasecmp(extension, ".mid")
            || !strcasecmp(extension, ".smf")
            || !strcasecmp(extension, ".imy")
            || !strcasecmp(extension, ".midi")
            || !strcasecmp(extension, ".xmf")
            || !strcasecmp(extension, ".rtttl")
            || !strcasecmp(extension, ".rtx")
            || !strcasecmp(extension, ".ota")) {
        return HandleMIDI(path, &client);
    }

    if (!strcasecmp(extension, ".ogg")) {
        return HandleOGG(path, &client);
    }

    if (!strcasecmp(extension, ".wv")) {
        return HandleWavPack(path, &client);
    }


    if (mRetriever->setDataSource(path) == OK
            && mRetriever->setMode(
                METADATA_MODE_METADATA_RETRIEVAL_ONLY) == OK) {
        const char *value;
        if ((value = mRetriever->extractMetadata(
                        METADATA_KEY_MIMETYPE)) != NULL) {
            client.setMimeType(value);
        }

        struct KeyMap {
            const char *tag;
            int key;
        };
        static const KeyMap kKeyMap[] = {
            { "tracknumber", METADATA_KEY_CD_TRACK_NUMBER },
            { "discnumber", METADATA_KEY_DISC_NUMBER },
            { "album", METADATA_KEY_ALBUM },
            { "artist", METADATA_KEY_ARTIST },
            { "albumartist", METADATA_KEY_ALBUMARTIST },
            { "composer", METADATA_KEY_COMPOSER },
            { "genre", METADATA_KEY_GENRE },
            { "title", METADATA_KEY_TITLE },
            { "year", METADATA_KEY_YEAR },
            { "duration", METADATA_KEY_DURATION },
            { "writer", METADATA_KEY_WRITER },
        };
        static const size_t kNumEntries = sizeof(kKeyMap) / sizeof(kKeyMap[0]);

        for (size_t i = 0; i < kNumEntries; ++i) {
            const char *value;
            if ((value = mRetriever->extractMetadata(kKeyMap[i].key)) != NULL) {
                client.addStringTag(kKeyMap[i].tag, value);
            }
        }
    }

    client.endFile();

    return OK;
}

char *StagefrightMediaScanner::extractAlbumArt(int fd) {
    LOGV("extractAlbumArt %d", fd);

    off_t size = lseek(fd, 0, SEEK_END);
    if (size < 0) {
        return NULL;
    }
    lseek(fd, 0, SEEK_SET);

    if (mRetriever->setDataSource(fd, 0, size) == OK
            && mRetriever->setMode(
                METADATA_MODE_FRAME_CAPTURE_ONLY) == OK) {
        sp<IMemory> mem = mRetriever->extractAlbumArt();

        if (mem != NULL) {
            MediaAlbumArt *art = static_cast<MediaAlbumArt *>(mem->pointer());

            char *data = (char *)malloc(art->mSize + 4);
            *(int32_t *)data = art->mSize;
            memcpy(&data[4], &art[1], art->mSize);

            return data;
        }
    }

    int32_t wvident;
    lseek(fd, 0, SEEK_SET);
    read(fd, &wvident, sizeof(wvident));
            
    if (wvident == 0x6b707677) {
        // a WavPack file
        // The following will either return NULL or the album art
        return(extractWavPackAlbumArt(fd));
    }

    return NULL;
}

}  // namespace android
