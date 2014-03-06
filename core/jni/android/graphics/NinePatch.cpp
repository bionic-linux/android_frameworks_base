/*
**
** Copyright 2006, The Android Open Source Project
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

#define LOG_TAG "9patch"
#define LOG_NDEBUG 1

#include <androidfw/ResourceTypes.h>
#include <utils/Log.h>

#include <Caches.h>

#include "SkCanvas.h"
#include "SkRegion.h"
#include "GraphicsJNI.h"

#include "JNIHelp.h"

extern void NinePatch_Draw(SkCanvas* canvas, const SkRect& bounds, const SkBitmap& bitmap,
        const android::Res_png_9patch& chunk, const SkPaint* paint, SkRegion** outRegion);

using namespace android;

/**
 * IMPORTANT NOTE: 9patch chunks can be manipuated either as an array of bytes
 * or as a Res_png_9patch instance. It is important to note that the size of the
 * array required to hold a 9patch chunk is greater than sizeof(Res_png_9patch).
 * The code below manipulates chunks as Res_png_9patch* types to draw and as
 * int8_t* to allocate and free the backing storage.
 */

class SkNinePatchGlue {
public:
    static jboolean isNinePatchChunk(JNIEnv* env, jobject, jbyteArray obj) {
        if (NULL == obj) {
            return JNI_FALSE;
        }
        if (env->GetArrayLength(obj) < (int)sizeof(Res_png_9patch_compat)) {
            return JNI_FALSE;
        }
        const jbyte* array = env->GetByteArrayElements(obj, 0);
        if (array != NULL) {
            const Res_png_9patch_compat* chunk = reinterpret_cast<const Res_png_9patch_compat*>(array);
            int8_t wasDeserialized = chunk->wasDeserialized;
            env->ReleaseByteArrayElements(obj, const_cast<jbyte*>(array), JNI_ABORT);
            return (wasDeserialized != -1) ? JNI_TRUE : JNI_FALSE;
        }
        return JNI_FALSE;
    }

    static jlong validateNinePatchChunk(JNIEnv* env, jobject, jlong, jbyteArray obj) {
        size_t chunkSize = env->GetArrayLength(obj);
        if (chunkSize < (int) (sizeof(Res_png_9patch_compat))) {
            jniThrowRuntimeException(env, "Array too small for chunk.");
            return NULL;
        }

        int8_t* storage = new int8_t[chunkSize];
        if (storage == NULL) return NULL;

        // This call copies the content of the jbyteArray
        env->GetByteArrayRegion(obj, 0, chunkSize, reinterpret_cast<jbyte*>(storage));
#ifdef  __LP64__
        // When aapt is 32bit on build host and the target is 64bit. There is 32/64
        // compatiblility issue for NinePatch.
        Res_png_9patch_compat* patch = (Res_png_9patch_compat*) storage;
        // For new Res_png_9patch size
        size_t data_size = patch->dataSize();
        size_t patchSize = sizeof(Res_png_9patch) + data_size;
        size_t patchSize_compat = sizeof(Res_png_9patch_compat) + data_size;
        assert(chunkSize == patchSize_compat);

        // You have to copy the data because it is owned by the png reader
        // On 32bit, this is like just copy between patchNew and patch directl
        // On 64bit, fields need be assigned one by one and data could be
        // copied directly.
        int8_t* patchMem = new int8_t[patchSize];
        if (patchMem == NULL) {
            delete[] storage;
            return NULL;
        }

        Res_png_9patch* patchNew =
            reinterpret_cast<Res_png_9patch*>(patchMem);
        patchNew->wasDeserialized = patch->wasDeserialized;
        patchNew->numXDivs = patch->numXDivs;
        patchNew->numYDivs = patch->numYDivs;
        patchNew->numColors = patch->numColors;
        patchNew->paddingLeft = patch->paddingLeft;
        patchNew->paddingRight = patch->paddingRight;
        patchNew->paddingTop = patch->paddingTop;
        patchNew->paddingBottom = patch->paddingBottom;
        void *src, *dst;
        src = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(patch) +
            sizeof(Res_png_9patch_compat));
        dst = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(patchNew) +
            sizeof(Res_png_9patch));
        memcpy(dst, src, data_size);
        delete[] storage;
        storage = reinterpret_cast<int8_t*>(patchNew);
#endif

        return reinterpret_cast<jlong>(Res_png_9patch::deserialize(storage));
    }

    static void finalize(JNIEnv* env, jobject, jlong patchHandle) {
        int8_t* patch = reinterpret_cast<int8_t*>(patchHandle);
#ifdef USE_OPENGL_RENDERER
        if (android::uirenderer::Caches::hasInstance()) {
            Res_png_9patch* p = (Res_png_9patch*) patch;
            android::uirenderer::Caches::getInstance().resourceCache.destructor(p);
            return;
        }
#endif // USE_OPENGL_RENDERER
        delete[] patch;
    }

    static void draw(JNIEnv* env, SkCanvas* canvas, SkRect& bounds, const SkBitmap* bitmap,
            Res_png_9patch* chunk, const SkPaint* paint, jint destDensity, jint srcDensity) {
        if (destDensity == srcDensity || destDensity == 0 || srcDensity == 0) {
            ALOGV("Drawing unscaled 9-patch: (%g,%g)-(%g,%g)",
                    SkScalarToFloat(bounds.fLeft), SkScalarToFloat(bounds.fTop),
                    SkScalarToFloat(bounds.fRight), SkScalarToFloat(bounds.fBottom));
            NinePatch_Draw(canvas, bounds, *bitmap, *chunk, paint, NULL);
        } else {
            canvas->save();

            SkScalar scale = SkFloatToScalar(destDensity / (float)srcDensity);
            canvas->translate(bounds.fLeft, bounds.fTop);
            canvas->scale(scale, scale);

            bounds.fRight = SkScalarDiv(bounds.fRight-bounds.fLeft, scale);
            bounds.fBottom = SkScalarDiv(bounds.fBottom-bounds.fTop, scale);
            bounds.fLeft = bounds.fTop = 0;

            ALOGV("Drawing scaled 9-patch: (%g,%g)-(%g,%g) srcDensity=%d destDensity=%d",
                    SkScalarToFloat(bounds.fLeft), SkScalarToFloat(bounds.fTop),
                    SkScalarToFloat(bounds.fRight), SkScalarToFloat(bounds.fBottom),
                    srcDensity, destDensity);

            NinePatch_Draw(canvas, bounds, *bitmap, *chunk, paint, NULL);

            canvas->restore();
        }
    }

    static void drawF(JNIEnv* env, jobject, jlong canvasHandle, jobject boundsRectF,
            jlong bitmapHandle, jlong chunkHandle, jlong paintHandle,
            jint destDensity, jint srcDensity) {
        SkCanvas* canvas       = reinterpret_cast<SkCanvas*>(canvasHandle);
        const SkBitmap* bitmap = reinterpret_cast<SkBitmap*>(bitmapHandle);
        Res_png_9patch* chunk  = reinterpret_cast<Res_png_9patch*>(chunkHandle);
        const SkPaint* paint   = reinterpret_cast<SkPaint*>(paintHandle);
        SkASSERT(canvas);
        SkASSERT(boundsRectF);
        SkASSERT(bitmap);
        SkASSERT(chunk);
        // paint is optional

        SkRect bounds;
        GraphicsJNI::jrectf_to_rect(env, boundsRectF, &bounds);

        draw(env, canvas, bounds, bitmap, chunk, paint, destDensity, srcDensity);
    }

    static void drawI(JNIEnv* env, jobject, jlong canvasHandle, jobject boundsRect,
            jlong bitmapHandle, jlong chunkHandle, jlong paintHandle,
            jint destDensity, jint srcDensity) {
        SkCanvas* canvas       = reinterpret_cast<SkCanvas*>(canvasHandle);
        const SkBitmap* bitmap = reinterpret_cast<SkBitmap*>(bitmapHandle);
        Res_png_9patch* chunk  = reinterpret_cast<Res_png_9patch*>(chunkHandle);
        const SkPaint* paint   = reinterpret_cast<SkPaint*>(paintHandle);
        SkASSERT(canvas);
        SkASSERT(boundsRect);
        SkASSERT(bitmap);
        SkASSERT(chunk);
        // paint is optional

        SkRect bounds;
        GraphicsJNI::jrect_to_rect(env, boundsRect, &bounds);
        draw(env, canvas, bounds, bitmap, chunk, paint, destDensity, srcDensity);
    }

    static jlong getTransparentRegion(JNIEnv* env, jobject, jlong bitmapHandle,
            jlong chunkHandle, jobject boundsRect) {
        const SkBitmap* bitmap = reinterpret_cast<SkBitmap*>(bitmapHandle);
        Res_png_9patch* chunk = reinterpret_cast<Res_png_9patch*>(chunkHandle);
        SkASSERT(bitmap);
        SkASSERT(chunk);
        SkASSERT(boundsRect);

        SkRect bounds;
        GraphicsJNI::jrect_to_rect(env, boundsRect, &bounds);

        SkRegion* region = NULL;
        NinePatch_Draw(NULL, bounds, *bitmap, *chunk, NULL, &region);

        return reinterpret_cast<jlong>(region);
    }

};

/////////////////////////////////////////////////////////////////////////////////////////

#include <android_runtime/AndroidRuntime.h>

static JNINativeMethod gNinePatchMethods[] = {
    { "isNinePatchChunk", "([B)Z",                        (void*) SkNinePatchGlue::isNinePatchChunk },
    { "validateNinePatchChunk", "(J[B)J",                 (void*) SkNinePatchGlue::validateNinePatchChunk },
    { "nativeFinalize", "(J)V",                           (void*) SkNinePatchGlue::finalize },
    { "nativeDraw", "(JLandroid/graphics/RectF;JJJII)V",  (void*) SkNinePatchGlue::drawF },
    { "nativeDraw", "(JLandroid/graphics/Rect;JJJII)V",   (void*) SkNinePatchGlue::drawI },
    { "nativeGetTransparentRegion", "(JJLandroid/graphics/Rect;)J",
                                                          (void*) SkNinePatchGlue::getTransparentRegion }
};

int register_android_graphics_NinePatch(JNIEnv* env) {
    return android::AndroidRuntime::registerNativeMethods(env,
            "android/graphics/NinePatch", gNinePatchMethods, SK_ARRAY_COUNT(gNinePatchMethods));
}
