/*
 * Copyright 2024, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#define LOG_NDEBUG 0
#define LOG_TAG "MediaCodec-JNI"

#include "android_runtime/AndroidRuntime.h"
#include "jni.h"

#include <media/AudioCapabilities.h>
#include <media/stagefright/foundation/ADebug.h>
#include <nativehelper/JNIHelp.h>

using namespace android;

// ----------------------------------------------------------------------------

struct fields_t {
    jfieldID context;
};
static fields_t fields;

// Utils

jobject getJavaIntRangeFromNative(JNIEnv *env, const Range<int>& range) {
    jclass rangeClazz = env->FindClass("android/util/Range");
    CHECK(rangeClazz != NULL);
    jmethodID rangeConstructID = env->GetMethodID(rangeClazz, "<init>", "(I;I)V");
    jobject jRange = env->NewObject(rangeClazz, rangeConstructID,
            range.lower(), range.upper());
    return jRange;
}

jobjectArray getJavaIntRangeArrayFromNative(JNIEnv *env, const std::vector<Range<int>>& ranges) {
    jclass rangeClazz = env->FindClass("android/util/Range");
    CHECK(rangeClazz != NULL);
    jobjectArray jRanges = env->NewObjectArray(ranges.size(), rangeClazz, NULL);
    for (int i = 0; i < ranges.size(); i++) {
        Range<int> range = ranges.at(i);
        jobject jRange = getJavaIntRangeFromNative(env, range);
        env->SetObjectArrayElement(jRanges, i, jRange);
        env->DeleteLocalRef(jRange);
        jRange = NULL;
    }
    return jRanges;
}

static std::shared_ptr<AudioCapabilities> getAudioCapabilities(JNIEnv *env, jobject thiz) {
    AudioCapabilities* const p = (AudioCapabilities*)env->GetLongField(thiz, fields.context);
    return std::shared_ptr<AudioCapabilities>(p);
}

// The Java AudioCapabilities object keep bitrateRange, sampleRates, sampleRateRanges
// and inputChannelRanges in it to prevent reconstruction when called the getters functions.
jobject getJavaAudioCapabilitiesFromNative(
        JNIEnv *env, std::shared_ptr<AudioCapabilities> audioCaps) {
    if (audioCaps == nullptr) {
        return NULL;
    }

    // construct Java bitrateRange
    const Range<int>& bitrateRange = audioCaps->getBitrateRange();
    jobject jBitrateRange = getJavaIntRangeFromNative(env, bitrateRange);

    // construct Java sampleRates array
    const std::vector<int>& sampleRates = audioCaps->getSupportedSampleRates();
    jintArray jSampleRates = env->NewIntArray(sampleRates.size());
    for (size_t i = 0; i < sampleRates.size(); ++i) {
        jint val = sampleRates.at(i);
        env->SetIntArrayRegion(jSampleRates, i, 1, &val);
    }

    // construct Java sampleRateRanges
    const std::vector<Range<int>>& sampleRateRanges = audioCaps->getSupportedSampleRateRanges();
    jobjectArray jSampleRateRanges = getJavaIntRangeArrayFromNative(env, sampleRateRanges);

    // construct Java inputChannelRanges
    const std::vector<Range<int>>& inputChannelRanges = audioCaps->getInputChannelCountRanges();
    jobjectArray jInputChannelRanges = getJavaIntRangeArrayFromNative(env, inputChannelRanges);

    // construct Java AudioCapabilities
    jclass audioCapsClazz =
        env->FindClass("android/media/MediaCodecInfo$AudioCapabilities");
    CHECK(audioCapsClazz != NULL);
    jmethodID audioCapsConstructID = env->GetMethodID(audioCapsClazz, "<init>",
            "(Landroid/util/Range;"
            "[I;"
            "[Landroid/util/Range;"
            "[Landroid/util/Range;)V");
    jobject jAudioCaps = env->NewObject(audioCapsClazz, audioCapsConstructID, jBitrateRange, jSampleRates,
            jSampleRateRanges, jInputChannelRanges);

    env->DeleteLocalRef(jBitrateRange);
    jBitrateRange = NULL;

    env->DeleteLocalRef(jSampleRates);
    jSampleRates = NULL;

    env->DeleteLocalRef(jSampleRateRanges);
    jSampleRateRanges = NULL;

    env->DeleteLocalRef(jInputChannelRanges);
    jInputChannelRanges = NULL;

    env->SetLongField(jAudioCaps, fields.context, (jlong)audioCaps.get());

    return jAudioCaps;
}

// ----------------------------------------------------------------------------

static jint android_media_AudioCapabilities_getMaxInputChannelCount(JNIEnv *env, jobject thiz) {
    std::shared_ptr<AudioCapabilities> audioCaps = getAudioCapabilities(env, thiz);
    if (audioCaps == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    int maxInputChannelCount = audioCaps->getMaxInputChannelCount();
    return maxInputChannelCount;
}

static jint android_media_AudioCapabilities_getMinInputChannelCount(JNIEnv *env, jobject thiz) {
    std::shared_ptr<AudioCapabilities> audioCaps = getAudioCapabilities(env, thiz);
    if (audioCaps == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    int minInputChannelCount = audioCaps->getMinInputChannelCount();
    return minInputChannelCount;
}

static jboolean android_media_AudioCapabilities_isSampleRateSupported(JNIEnv *env, jobject thiz, int sampleRate) {
    std::shared_ptr<AudioCapabilities> audioCaps = getAudioCapabilities(env, thiz);
    if (audioCaps == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    bool res = audioCaps->isSampleRateSupported(sampleRate);
    return res;
}

// ----------------------------------------------------------------------------

static const JNINativeMethod gAudioCapsMethods[] = {
    {"native_getMaxInputChannelCount", "()I", (void *)android_media_AudioCapabilities_getMaxInputChannelCount},
    {"native_getMinInputChannelCount", "()I", (void *)android_media_AudioCapabilities_getMinInputChannelCount},
    {"native_isSampleRateSupported", "(I)Z", (void *)android_media_AudioCapabilities_isSampleRateSupported}
};

int register_android_media_CodecCapabilities(JNIEnv *env) {
    int result = AndroidRuntime::registerNativeMethods(env, "android/media/MediaCodecInfo$AudioCapabilities",
            gAudioCapsMethods, NELEM(gAudioCapsMethods));
    if (result != JNI_OK) {
        return result;
    }

    return result;
}