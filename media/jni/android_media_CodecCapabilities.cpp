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
#include <media/VideoCapabilities.h>
#include <media/stagefright/foundation/ADebug.h>
#include <nativehelper/JNIHelp.h>

using namespace android;

// ----------------------------------------------------------------------------

struct fields_t {
    jfieldID audioCapsContext;
    jfieldID performancePointContext;
    jfieldID videoCapsContext;
};
static fields_t fields;

// Getters

static AudioCapabilities* getAudioCapabilities(JNIEnv *env, jobject thiz) {
    AudioCapabilities* const p = (AudioCapabilities*)env->GetLongField(
            thiz, fields.audioCapsContext);
    return p;
}

static VideoCapabilities::PerformancePoint& getPerformancePoints(JNIEnv *env, jobject thiz) {
    VideoCapabilities::PerformancePoint* const p = (VideoCapabilities::PerformancePoint*)
            env->GetLongField(thiz, fields.performancePointContext);
    return *p;
}

static VideoCapabilities* getVideoCapabilities(JNIEnv *env, jobject thiz) {
    VideoCapabilities* const p = (VideoCapabilities*)env->GetLongField(
            thiz, fields.videoCapsContext);
    return p;
}

// Utils

static jobject getJavaIntRangeFromNative(JNIEnv *env, const Range<int>& range) {
    // Get Integer Objects
    jclass integerClazz = env->FindClass("java/lang/Integer");
    jmethodID integerConstructID = env->GetMethodID(integerClazz, "<init>", "(I)V");
    jobject jLower = env->NewObject(integerClazz, integerConstructID, range.lower());
    jobject jUpper = env->NewObject(integerClazz, integerConstructID, range.upper());

    // Get Integer Range
    jclass helperClazz = env->FindClass("android/media/MediaCodecInfo$GenericHelper");
    jmethodID getIntegerRangeID = env->GetStaticMethodID(helperClazz, "getIntegerRange",
            "(Ljava/lang/Integer;Ljava/lang/Integer;)Landroid/util/Range;");
    jobject jRange = env->CallStaticObjectMethod(helperClazz, getIntegerRangeID, jLower, jUpper);

    env->DeleteLocalRef(jLower);
    jLower = NULL;
    env->DeleteLocalRef(jUpper);
    jUpper = NULL;

    return jRange;
}

static jobject getJavaDoubleRangeFromNative(JNIEnv *env, const Range<double>& range) {
    // Get Double Objects
    jclass doubleClazz = env->FindClass("java/lang/Double");
    jmethodID doubleConstructID = env->GetMethodID(doubleClazz, "<init>", "(D)V");
    jobject jLower = env->NewObject(doubleClazz, doubleConstructID, range.lower());
    jobject jUpper = env->NewObject(doubleClazz, doubleConstructID, range.upper());

    // Get Double Range
    jclass helperClazz = env->FindClass("android/media/MediaCodecInfo$GenericHelper");
    jmethodID getDoubleRangeID = env->GetStaticMethodID(helperClazz, "getDoubleRange",
            "(Ljava/lang/Double;Ljava/lang/Double;)Landroid/util/Range;");
    jobject jRange = env->CallStaticObjectMethod(helperClazz, getDoubleRangeID, jLower, jUpper);

    env->DeleteLocalRef(jLower);
    jLower = NULL;
    env->DeleteLocalRef(jUpper);
    jUpper = NULL;

    return jRange;
}

static jobjectArray getJavaIntRangeArrayFromNative(JNIEnv *env,
        const std::vector<Range<int>>& ranges) {
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

// Constructors from native instances to Java objects

// The Java AudioCapabilities object keep bitrateRange, sampleRates, sampleRateRanges
// and inputChannelRanges in it to prevent reconstruction when called the getters functions.
static jobject getJavaAudioCapabilitiesFromNative(
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

    // construct Java AudioCapsNativeImpl
    jclass nativeImplClazz
            = env->FindClass("android/media/MediaCodecInfo$AudioCapabilities$AudioCapsNativeImpl");
    CHECK(nativeImplClazz != NULL);
    jmethodID nativeImplConstructID = env->GetMethodID(nativeImplClazz, "<init>",
            "(Landroid/util/Range;"
            "[I"
            "[Landroid/util/Range;"
            "[Landroid/util/Range;)V");
    jobject jAudioCapsNativeImpl = env->NewObject(nativeImplClazz, nativeImplConstructID,
            jBitrateRange, jSampleRates, jSampleRateRanges, jInputChannelRanges);

    env->SetLongField(jAudioCapsNativeImpl, fields.audioCapsContext, (jlong)audioCaps.get());

    // construct Java AudioCapabilities object with native impl object
    jclass audioCapsClazz
            = env->FindClass("android/media/MediaCodecInfo$AudioCapabilities");
    CHECK(audioCapsClazz != NULL);
    jmethodID audioCapsConstructID = env->GetMethodID(audioCapsClazz, "<init>",
            "(Landroid/media/MediaCodecInfo$AudioCapabilities$AudioCapsNativeImpl;)V");
    jobject jAudioCaps = env->NewObject(audioCapsClazz, audioCapsConstructID, jAudioCapsNativeImpl);

    env->DeleteLocalRef(jBitrateRange);
    jBitrateRange = NULL;

    env->DeleteLocalRef(jSampleRates);
    jSampleRates = NULL;

    env->DeleteLocalRef(jSampleRateRanges);
    jSampleRateRanges = NULL;

    env->DeleteLocalRef(jInputChannelRanges);
    jInputChannelRanges = NULL;

    env->DeleteLocalRef(jAudioCapsNativeImpl);
    jAudioCapsNativeImpl = NULL;

    return jAudioCaps;
}

// convert native PerformancePoints to Java objects
static jobject convertPerformancePointVectorToList(JNIEnv *env,
        const std::vector<VideoCapabilities::PerformancePoint>& performancePoints) {
    jclass performancePointClazz = env->FindClass(
            "android/media/MediaCodecInfo$VideoCapabilities$PerformancePoint");
    CHECK(performancePointClazz != NULL);
    jmethodID performancePointConstructID = env->GetMethodID(performancePointClazz, "<init>",
            "(IIIJII)V");

    jobjectArray jPerformancePoints = env->NewObjectArray(performancePoints.size(),
            performancePointClazz, NULL);
    for (int i = 0; i < performancePoints.size(); i++) {
        VideoCapabilities::PerformancePoint performancePoint = performancePoints.at(i);
        jobject jPerformancePoint = env->NewObject(performancePointClazz,
                performancePointConstructID, performancePoint.getWidth(),
                performancePoint.getHeight(), performancePoint.getMaxFrameRate(),
                performancePoint.getMaxMacroBlockRate(), performancePoint.getBlockSize().getWidth(),
                performancePoint.getBlockSize().getHeight());

        env->SetLongField(jPerformancePoint, fields.performancePointContext,
                (jlong)(&performancePoint));

        env->SetObjectArrayElement(jPerformancePoints, i, jPerformancePoint);

        env->DeleteLocalRef(jPerformancePoint);
        jPerformancePoint = NULL;
    }

    jclass helperClazz = env->FindClass("android/media/MediaCodecInfo$GenericHelper");
    CHECK(helperClazz != NULL);
    jmethodID asListID = env->GetStaticMethodID(helperClazz, "convertPerformancePointArrayToList",
            "([Landroid/media/MediaCodecInfo$VideoCapabilities$PerformancePoint;)Ljava/util/List;");
    CHECK(asListID != NULL);
    jobject jList = env->CallStaticObjectMethod(helperClazz, asListID, jPerformancePoints);

    return jList;
}

// construct native PerformancePoint instance from Java objects
static VideoCapabilities::PerformancePoint GetNativePerformancePointFromJava(JNIEnv *env,
        jobject pp) {
    jclass clazz = env->FindClass(
            "android/media/MediaCodecInfo$VideoCapabilities$PerformancePoint");
    CHECK(clazz != NULL);
    CHECK(env->IsInstanceOf(pp, clazz));

    jmethodID getWidthID = env->GetMethodID(clazz, "getWidth", "()I");
    CHECK(getWidthID != NULL);
    jint width = env->CallIntMethod(pp, getWidthID);

    jmethodID getHeightID = env->GetMethodID(clazz, "getHeight", "()I");
    CHECK(getHeightID != NULL);
    jint height = env->CallIntMethod(pp, getHeightID);

    jmethodID getMaxFrameRateID = env->GetMethodID(clazz, "getMaxFrameRate", "()I");
    CHECK(getMaxFrameRateID != NULL);
    jint maxFrameRate = env->CallIntMethod(pp, getMaxFrameRateID);

    jmethodID getMaxMacroBlockRateID = env->GetMethodID(clazz, "getMaxMacroBlockRate", "()J");
    CHECK(getMaxMacroBlockRateID != NULL);
    jlong maxMacroBlockRate = env->CallLongMethod(pp, getMaxMacroBlockRateID);

    jmethodID getBlockWidthID = env->GetMethodID(clazz, "getBlockWidth", "()I");
    CHECK(getBlockWidthID != NULL);
    jint blockWidth = env->CallIntMethod(pp, getBlockWidthID);

    jmethodID getBlockHeightID = env->GetMethodID(clazz, "getBlockHeight", "()I");
    CHECK(getBlockHeightID != NULL);
    jint blockHeight = env->CallIntMethod(pp, getBlockHeightID);

    VideoCapabilities::PerformancePoint res = VideoCapabilities::PerformancePoint(
            VideoSize(blockWidth, blockHeight), width, height, maxFrameRate, maxMacroBlockRate);

    return res;
}

jobject getJavaVideoCapabilitiesFromNative(JNIEnv *env,
        std::shared_ptr<VideoCapabilities> videoCaps) {
    if (videoCaps == nullptr) {
        return NULL;
    }

    // get Java bitrateRange
    const Range<int>& bitrateRange = videoCaps->getBitrateRange();
    jobject jBitrateRange = getJavaIntRangeFromNative(env, bitrateRange);

    // get Java widthRange
    const Range<int>& widthRange = videoCaps->getSupportedWidths();
    jobject jWidthRange = getJavaIntRangeFromNative(env, widthRange);

    // get Java heightRange
    const Range<int>& heightRange = videoCaps->getSupportedHeights();
    jobject jHeightRange = getJavaIntRangeFromNative(env, heightRange);

    // get Java frameRateRange
    const Range<int>& frameRateRange = videoCaps->getSupportedFrameRates();
    jobject jFrameRateRange = getJavaIntRangeFromNative(env, frameRateRange);

    // get Java performancePoints
    const std::vector<VideoCapabilities::PerformancePoint>& performancePoints
            = videoCaps->getSupportedPerformancePoints();
    // jobjectArray jPerformancePoints = getJavaPerformancePointArrayFromNative(
    //         env, performancePoints);
    jobject jPerformancePoints = convertPerformancePointVectorToList(env, performancePoints);

    // get width alignment
    int widthAlignment = videoCaps->getWidthAlignment();

    // get height alignment
    int heightAlignment = videoCaps->getHeightAlignment();

    // get Java VideoCapabilities
    jclass videoCapsClazz =
        env->FindClass("android/media/MediaCodecInfo$VideoCapabilities");
    CHECK(videoCapsClazz != NULL);
    jmethodID videoCapsConstructID = env->GetMethodID(videoCapsClazz, "<init>",
            "(Landroid/util/Range;"
            "Landroid/util/Range;"
            "Landroid/util/Range;"
            "Landroid/util/Range;"
            "Ljava/util/List;II)V");
    jobject jVideoCaps = env->NewObject(videoCapsClazz, videoCapsConstructID, jBitrateRange,
            jWidthRange, jHeightRange, jFrameRateRange, jPerformancePoints, widthAlignment,
            heightAlignment);

    env->DeleteLocalRef(jBitrateRange);
    jBitrateRange = NULL;

    env->DeleteLocalRef(jWidthRange);
    jWidthRange = NULL;

    env->DeleteLocalRef(jHeightRange);
    jHeightRange = NULL;

    env->DeleteLocalRef(jFrameRateRange);
    jFrameRateRange = NULL;

    env->DeleteLocalRef(jPerformancePoints);
    jPerformancePoints = NULL;

    env->SetLongField(jVideoCaps, fields.videoCapsContext, (jlong)videoCaps.get());

    return jVideoCaps;
}

// ----------------------------------------------------------------------------

// AudioCapabilities

static void android_media_AudioCapabilities_native_init(JNIEnv *env) {
    jclass audioCapsClazz
            = env->FindClass("android/media/MediaCodecInfo$AudioCapabilities$AudioCapsNativeImpl");
    if (audioCapsClazz == NULL) {
        return;
    }

    fields.audioCapsContext = env->GetFieldID(audioCapsClazz, "mNativeContext", "J");
    if (fields.audioCapsContext == NULL) {
        return;
    }

    env->DeleteLocalRef(audioCapsClazz);
}

static jint android_media_AudioCapabilities_getMaxInputChannelCount(JNIEnv *env, jobject thiz) {
    AudioCapabilities* const audioCaps = getAudioCapabilities(env, thiz);
    if (audioCaps == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    int maxInputChannelCount = audioCaps->getMaxInputChannelCount();
    return maxInputChannelCount;
}

static jint android_media_AudioCapabilities_getMinInputChannelCount(JNIEnv *env, jobject thiz) {
    AudioCapabilities* const audioCaps = getAudioCapabilities(env, thiz);
    if (audioCaps == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    int minInputChannelCount = audioCaps->getMinInputChannelCount();
    return minInputChannelCount;
}

static jboolean android_media_AudioCapabilities_isSampleRateSupported(JNIEnv *env, jobject thiz,
        int sampleRate) {
    AudioCapabilities* const audioCaps = getAudioCapabilities(env, thiz);
    if (audioCaps == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    bool res = audioCaps->isSampleRateSupported(sampleRate);
    return res;
}

// PerformancePoint

static void android_media_VideoCapabilities_PerformancePoint_native_init(JNIEnv *env) {
    jclass clazz = env->FindClass(
            "android/media/MediaCodecInfo$VideoCapabilities$PerformancePoint");
    if (clazz == NULL) {
        return;
    }

    fields.performancePointContext = env->GetFieldID(clazz, "mNativeContext", "J");
    if (fields.performancePointContext == NULL) {
        return;
    }

    env->DeleteLocalRef(clazz);
}

static jboolean android_media_VideoCapabilities_PerformancePoint_covers(JNIEnv *env, jobject thiz,
        jobject other) {
    VideoCapabilities::PerformancePoint& pp0 = getPerformancePoints(env, thiz);
    VideoCapabilities::PerformancePoint pp1 = GetNativePerformancePointFromJava(env, other);

    bool res = pp0.covers(pp1);

    return res;
}

static jboolean android_media_VideoCapabilities_PerformancePoint_equals(JNIEnv *env, jobject thiz,
        jobject other) {
    VideoCapabilities::PerformancePoint& pp0 = getPerformancePoints(env, thiz);
    VideoCapabilities::PerformancePoint pp1 = GetNativePerformancePointFromJava(env, other);

    bool res = pp0.equals(pp1);

    return res;
}

static jstring android_media_VideoCapabilities_PerformancePoint_toString(JNIEnv *env,
        jobject thiz) {
    VideoCapabilities::PerformancePoint& pp = getPerformancePoints(env, thiz);
    std::string str = pp.toString();
    return env->NewStringUTF(str.c_str());
}

// VideoCapabilities

static void android_media_VideoCapabilities_native_init(JNIEnv *env) {
    jclass clazz
            = env->FindClass("android/media/MediaCodecInfo$VideoCapabilities");
    if (clazz == NULL) {
        return;
    }

    fields.videoCapsContext = env->GetFieldID(clazz, "mNativeContext", "J");
    if (fields.videoCapsContext == NULL) {
        return;
    }

    env->DeleteLocalRef(clazz);
}

static jboolean android_media_VideoCapabilities_areSizeAndRateSupported(JNIEnv *env, jobject thiz,
        int width, int height, double frameRate) {
    VideoCapabilities* const videoCaps = getVideoCapabilities(env, thiz);
    if (videoCaps == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    bool res = videoCaps->areSizeAndRateSupported(width, height, frameRate);
    return res;
}

static jboolean android_media_VideoCapabilities_isSizeSupported(JNIEnv *env, jobject thiz,
        int width, int height) {
    VideoCapabilities* const videoCaps = getVideoCapabilities(env, thiz);
    if (videoCaps == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    bool res = videoCaps->isSizeSupported(width, height);
    return res;
}

static jobject android_media_VideoCapabilities_getAchievableFrameRatesFor(JNIEnv *env, jobject thiz,
        int width, int height) {
    VideoCapabilities* const videoCaps = getVideoCapabilities(env, thiz);
    if (videoCaps == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return NULL;
    }

    std::optional<Range<double>> frameRates = videoCaps->getAchievableFrameRatesFor(width, height);
    if (!frameRates) {
        return NULL;
    }
    jobject jFrameRates = getJavaDoubleRangeFromNative(env, frameRates.value());
    return jFrameRates;
}

static jobject android_media_VideoCapabilities_getSupportedFrameRatesFor(JNIEnv *env, jobject thiz,
        int width, int height) {
    VideoCapabilities* const videoCaps = getVideoCapabilities(env, thiz);
    if (videoCaps == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return NULL;
    }

    std::optional<Range<double>> frameRates = videoCaps->getSupportedFrameRatesFor(width, height);
    if (!frameRates) {
        return NULL;
    }
    jobject jFrameRates = getJavaDoubleRangeFromNative(env, frameRates.value());
    return jFrameRates;
}

static jobject android_media_VideoCapabilities_getSupportedWidthsFor(JNIEnv *env, jobject thiz,
        int height) {
    VideoCapabilities* const videoCaps = getVideoCapabilities(env, thiz);
    if (videoCaps == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return NULL;
    }

    std::optional<Range<int>> supportedWidths = videoCaps->getSupportedWidthsFor(height);
    if (!supportedWidths) {
        return NULL;
    }
    jobject jSupportedWidths = getJavaIntRangeFromNative(env, supportedWidths.value());

    return jSupportedWidths;
}

static jobject android_media_VideoCapabilities_getSupportedHeightsFor(JNIEnv *env, jobject thiz,
        int width) {
    VideoCapabilities* const videoCaps = getVideoCapabilities(env, thiz);
    if (videoCaps == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return NULL;
    }

    std::optional<Range<int>> supportedHeights = videoCaps->getSupportedHeightsFor(width);
    if (!supportedHeights) {
        return NULL;
    }
    jobject jSupportedHeights = getJavaIntRangeFromNative(env, supportedHeights.value());

    return jSupportedHeights;
}

// ----------------------------------------------------------------------------

static const JNINativeMethod gAudioCapsMethods[] = {
    {"native_init", "()V", (void *)android_media_AudioCapabilities_native_init},
    {"native_getMaxInputChannelCount", "()I", (void *)android_media_AudioCapabilities_getMaxInputChannelCount},
    {"native_getMinInputChannelCount", "()I", (void *)android_media_AudioCapabilities_getMinInputChannelCount},
    {"native_isSampleRateSupported", "(I)Z", (void *)android_media_AudioCapabilities_isSampleRateSupported}
};

static const JNINativeMethod gPerformancePointMethods[] = {
    {"native_init", "()V", (void *)android_media_VideoCapabilities_PerformancePoint_native_init},
    {"native_covers", "(Landroid/media/MediaCodecInfo$VideoCapabilities$PerformancePoint;)Z", (void *)android_media_VideoCapabilities_PerformancePoint_covers},
    {"native_equals", "(Landroid/media/MediaCodecInfo$VideoCapabilities$PerformancePoint;)Z", (void *)android_media_VideoCapabilities_PerformancePoint_equals},
    {"native_toString", "()Ljava/lang/String;", (void *)android_media_VideoCapabilities_PerformancePoint_toString}
};

static const JNINativeMethod gVideoCapsMethods[] = {
    {"native_init", "()V", (void *)android_media_VideoCapabilities_native_init},
    {"native_areSizeAndRateSupported", "(IID)Z", (void *)android_media_VideoCapabilities_areSizeAndRateSupported},
    {"native_isSizeSupported", "(II)Z", (void *)android_media_VideoCapabilities_isSizeSupported},
    {"native_getAchievableFrameRatesFor", "(II)Landroid/util/Range;", (void *)android_media_VideoCapabilities_getAchievableFrameRatesFor},
    {"native_getSupportedFrameRatesFor", "(II)Landroid/util/Range;", (void *)android_media_VideoCapabilities_getSupportedFrameRatesFor},
    {"native_getSupportedWidthsFor", "(I)Landroid/util/Range;", (void *)android_media_VideoCapabilities_getSupportedWidthsFor},
    {"native_getSupportedHeightsFor", "(I)Landroid/util/Range;", (void *)android_media_VideoCapabilities_getSupportedHeightsFor}
};

int register_android_media_CodecCapabilities(JNIEnv *env) {
    int result = AndroidRuntime::registerNativeMethods(env,
            "android/media/MediaCodecInfo$AudioCapabilities$AudioCapsNativeImpl",
            gAudioCapsMethods, NELEM(gAudioCapsMethods));
    if (result != JNI_OK) {
        return result;
    }

    result = AndroidRuntime::registerNativeMethods(env,
            "android/media/MediaCodecInfo$VideoCapabilities$PerformancePoint",
            gPerformancePointMethods, NELEM(gPerformancePointMethods));
    if (result != JNI_OK) {
        return result;
    }

    result = AndroidRuntime::registerNativeMethods(env,
            "android/media/MediaCodecInfo$VideoCapabilities",
            gVideoCapsMethods, NELEM(gVideoCapsMethods));
    if (result != JNI_OK) {
        return result;
    }

    return result;
}