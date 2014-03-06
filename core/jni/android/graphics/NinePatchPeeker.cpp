/*
 * Copyright (C) 2011 The Android Open Source Project
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

#include "NinePatchPeeker.h"

#include "SkBitmap.h"

using namespace android;

bool NinePatchPeeker::peek(const char tag[], const void* data, size_t length) {
    if (strcmp("npTc", tag) == 0 && length >= sizeof(Res_png_9patch)) {

#ifdef  __LP64__
        Res_png_9patch_compat* patch = (Res_png_9patch_compat*) data;
        // For new Res_png_9patch size
        size_t data_size = patch->dataSize();
        size_t patchSize = sizeof(Res_png_9patch) + data_size;
        size_t patchSize_compat = sizeof(Res_png_9patch_compat) + data_size;
        assert(length == patchSize_compat);

        // You have to copy the data because it is owned by the png reader
        // On 32bit, this is like just copy between patchNew and patch directly
        // On 64bit, fields need be assigned one by one and data could be
        // copied directly.
        Res_png_9patch* patchNew = (Res_png_9patch*) malloc(patchSize);
        patchNew->wasDeserialized = patch->wasDeserialized;
        patchNew->numXDivs = patch->numXDivs;
        patchNew->numYDivs = patch->numYDivs;
        patchNew->numColors = patch->numColors;
        patchNew->paddingLeft = patch->paddingLeft;
        patchNew->paddingRight = patch->paddingRight;
        patchNew->paddingTop = patch->paddingTop;
        patchNew->paddingBottom = patch->paddingBottom;

        void *src, *dst;
        src = (void *)((uintptr_t)patch+ sizeof(Res_png_9patch_compat));
        dst = (void *)((uintptr_t)patchNew + sizeof(Res_png_9patch));
        memcpy(dst, src, data_size);
#else
        Res_png_9patch* patch = (Res_png_9patch*) data;
        size_t patchSize = patch->serializedSize();
        assert(length == patchSize);
        // You have to copy the data because it is owned by the png reader
        Res_png_9patch* patchNew = (Res_png_9patch*) malloc(patchSize);
        memcpy(patchNew, patch, patchSize);
#endif

        // this relies on deserialization being done in place
        Res_png_9patch::deserialize(patchNew);
        patchNew->fileToDevice();
        free(fPatch);
        fPatch = patchNew;
        //printf("9patch: (%d,%d)-(%d,%d)\n",
        //       fPatch.sizeLeft, fPatch.sizeTop,
        //       fPatch.sizeRight, fPatch.sizeBottom);

        // now update our host to force index or 32bit config
        // 'cause we don't want 565 predithered, since as a 9patch, we know
        // we will be stretched, and therefore we want to dither afterwards.
        static const SkBitmap::Config gNo565Pref[] = {
            SkBitmap::kIndex8_Config,
            SkBitmap::kIndex8_Config,
            SkBitmap::kARGB_8888_Config,
            SkBitmap::kARGB_8888_Config,
            SkBitmap::kARGB_8888_Config,
            SkBitmap::kARGB_8888_Config,
        };
        fHost->setPrefConfigTable(gNo565Pref);
    } else if (strcmp("npLb", tag) == 0 && length == sizeof(int) * 4) {
        fLayoutBounds = new int[4];
        memcpy(fLayoutBounds, data, sizeof(int) * 4);
    }
    return true;    // keep on decoding
}
