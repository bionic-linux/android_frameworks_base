/*
 * Copyright (C) 2018 The Android Open Source Project
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

#define LOG_TAG "AudioProductStrategies-JNI"

#include <inttypes.h>
#include <jni.h>
#include <nativehelper/JNIHelp.h>
#include "core_jni_helpers.h"

#include <utils/Log.h>
#include <vector>

#include <media/AudioSystem.h>
#include <media/AudioPolicy.h>

#include <nativehelper/ScopedUtfChars.h>

#include "android_media_AudioAttributes.h"
#include "android_media_AudioErrors.h"

// ----------------------------------------------------------------------------

using namespace android;

// ----------------------------------------------------------------------------
static const char* const kClassPathName = "android/media/audiopolicy/AudioProductStrategy";
static const char* const kAudioProductStrategyClassPathName =
        "android/media/audiopolicy/AudioProductStrategy";

static const char* const kAudioAttributesGroupsClassPathName =
        "android/media/audiopolicy/AudioProductStrategy$AudioAttributesGroup";

static jclass gAudioProductStrategyClass;
static jmethodID gAudioProductStrategyCstor;
static struct {
    jfieldID    mAudioAttributesGroups;
    jfieldID    mName;
    jfieldID    mId;
} gAudioProductStrategyFields;

static jclass gAudioAttributesGroupClass;
static jmethodID gAudioAttributesGroupCstor;
static struct {
    jfieldID    mVolumeGroupId;
    jfieldID    mLegacyStreamType;
    jfieldID    mAudioAttributes;
} gAudioAttributesGroupsFields;

static jclass gArrayListClass;
static struct {
    jmethodID    add;
    jmethodID    toArray;
} gArrayListMethods;

static jclass gPairClass;
static jmethodID gPairCstor;

static jint convertAudioProductStrategiesFromNative(
        JNIEnv *env, jobject *jAudioStrategy, const AudioProductStrategy &strategy)
{
    jint jStatus = (jint)AUDIO_JAVA_SUCCESS;
    jobjectArray jAudioAttributesGroups = NULL;
    jobjectArray jAudioAttributes = NULL;
    jobject jAudioAttribute = NULL;
    jstring jName = NULL;
    jint jStrategyId = NULL;
    jint numAttributesGroups;
    size_t indexGroup = 0;

    jName = env->NewStringUTF(strategy.getName().c_str());
    jStrategyId = static_cast<jint>(strategy.getId());

    // Audio Attributes Group array
    int attrGroupIndex = 0;
    std::map<int /**attributesGroupIndex*/, std::vector<AudioAttributes> > groups;
    for (const auto &attr : strategy.getAudioAttributes()) {
        int groupId = attr.getGroupId();
        int streamType = attr.getStreamType();
        const auto &iter = std::find_if(begin(groups), end(groups),
                                        [groupId, streamType](const auto &iter) {
            const auto &frontAttr = iter.second.front();
            return frontAttr.getGroupId() == groupId && frontAttr.getStreamType() == streamType;
        });
        // Same Volume Group Id and same stream type
        if (iter != end(groups)) {
             groups[iter->first].push_back(attr);
        } else {
            // Add a new Group of AudioAttributes for this product strategy
            groups[attrGroupIndex++].push_back(attr);
        }
    }
    numAttributesGroups = groups.size();

    jAudioAttributesGroups = env->NewObjectArray(numAttributesGroups, gAudioAttributesGroupClass, NULL);

    for (const auto &iter : groups) {
        std::vector<AudioAttributes> audioAttributesGroups = iter.second;
        jint numAttributes = audioAttributesGroups.size();
        jint jGroupId = audioAttributesGroups.front().getGroupId();
        jint jLegacyStreamType = audioAttributesGroups.front().getStreamType();

        jStatus = JNIAudioAttributeHelper::getJavaArray(env, &jAudioAttributes, numAttributes);
        if (jStatus != (jint)AUDIO_JAVA_SUCCESS) {
            goto exit;
        }
        for (size_t j = 0; j < static_cast<size_t>(numAttributes); j++) {
            auto attributes = audioAttributesGroups[j].getAttributes();

            jStatus = JNIAudioAttributeHelper::nativeToJava(env, &jAudioAttribute, attributes);
            if (jStatus != AUDIO_JAVA_SUCCESS) {
                goto exit;
            }
            env->SetObjectArrayElement(jAudioAttributes, j, jAudioAttribute);
        }
        jobject jAudioAttributesGroup = env->NewObject(gAudioAttributesGroupClass,
                                                       gAudioAttributesGroupCstor,
                                                       jGroupId,
                                                       jLegacyStreamType,
                                                       jAudioAttributes);
        env->SetObjectArrayElement(jAudioAttributesGroups, indexGroup++, jAudioAttributesGroup);

        if (jAudioAttributes != NULL) {
            env->DeleteLocalRef(jAudioAttributes);
            jAudioAttributes = NULL;
        }
        if (jAudioAttribute != NULL) {
            env->DeleteLocalRef(jAudioAttribute);
            jAudioAttribute = NULL;
        }
        if (jAudioAttributesGroup != NULL) {
            env->DeleteLocalRef(jAudioAttributesGroup);
            jAudioAttributesGroup = NULL;
        }
    }
    *jAudioStrategy = env->NewObject(gAudioProductStrategyClass, gAudioProductStrategyCstor,
                                     jName,
                                     jStrategyId,
                                     jAudioAttributesGroups);
exit:
    if (jAudioAttributes != NULL) {
        env->DeleteLocalRef(jAudioAttributes);
    }
    if (jAudioAttribute != NULL) {
        env->DeleteLocalRef(jAudioAttribute);
        jAudioAttribute = NULL;
    }
    if (jAudioAttributesGroups != NULL) {
        env->DeleteLocalRef(jAudioAttributesGroups);
    }
    if (jName != NULL) {
        env->DeleteLocalRef(jName);
    }
    return jStatus;
}

static jint
android_media_AudioSystem_listAudioProductStrategies(JNIEnv *env, jobject clazz,
                                                     jobject jStrategies)
{
    if (env == NULL) {
        return AUDIO_JAVA_DEAD_OBJECT;
    }
    if (jStrategies == NULL) {
        ALOGE("listAudioProductStrategies NULL AudioProductStrategies");
        return (jint)AUDIO_JAVA_BAD_VALUE;
    }
    if (!env->IsInstanceOf(jStrategies, gArrayListClass)) {
        ALOGE("listAudioProductStrategies not an arraylist");
        return (jint)AUDIO_JAVA_BAD_VALUE;
    }

    status_t status;
    AudioProductStrategyVector strategies;
    jint jStatus;
    jobject jStrategy = NULL;

    status = AudioSystem::listAudioProductStrategies(strategies);
    if (status != NO_ERROR) {
        ALOGE("AudioSystem::listAudioProductStrategies error %d", status);
        return nativeToJavaStatus(status);
    }
    for (const auto &strategy : strategies) {
        jStatus = convertAudioProductStrategiesFromNative(env, &jStrategy, strategy);
        if (jStatus != AUDIO_JAVA_SUCCESS) {
            goto exit;
        }
        env->CallBooleanMethod(jStrategies, gArrayListMethods.add, jStrategy);
    }
exit:
    if (jStrategy != NULL) {
        env->DeleteLocalRef(jStrategy);
    }
    return jStatus;
}

static jobject
android_media_AudioSystem_getPreferredDeviceForStrategy(JNIEnv *env, jobject clazz, jint strategyId)
{
    AudioDeviceTypeAddr device;
    status_t status = AudioSystem::getPreferredDeviceForStrategy(
                static_cast<product_strategy_t>(strategyId), device);
    if (status != NO_ERROR) {
        return nullptr;
    }
    jstring jAddress = env->NewStringUTF(device.mAddress.c_str());
    return env->NewObject(gPairClass, gPairCstor, device.mType, jAddress);
}

static int
android_media_AudioSystem_setPreferredDeviceForStrategy(
        JNIEnv *env, jobject clazz, jint strategyId, jint deviceType, jstring deviceAddress)
{
    const char *c_deviceAddress = env->GetStringUTFChars(deviceAddress, NULL);
    AudioDeviceTypeAddr device{static_cast<audio_devices_t>(deviceType), c_deviceAddress};
    status_t status = AudioSystem::setPreferredDeviceForStrategy(
                static_cast<product_strategy_t>(strategyId), device);
    env->ReleaseStringUTFChars(deviceAddress, c_deviceAddress);
    return nativeToJavaStatus(status);
}

static int
android_media_AudioSystem_removePreferredDeviceForStrategy(
        JNIEnv *env, jobject clazz, jint strategyId)
{
    status_t status = AudioSystem::removePreferredDeviceForStrategy(
                static_cast<product_strategy_t>(strategyId));
    return nativeToJavaStatus(status);
}

/*
 * JNI registration.
 */
static const JNINativeMethod gMethods[] = {
    {"native_list_audio_product_strategies", "(Ljava/util/ArrayList;)I",
                        (void *)android_media_AudioSystem_listAudioProductStrategies},
    {"native_get_preferred_device_for_strategy", "(I)Landroid/util/Pair;",
                        (void *)android_media_AudioSystem_getPreferredDeviceForStrategy},
    {"native_set_preferred_device_for_strategy", "(IILjava/lang/String;)I",
                        (void *)android_media_AudioSystem_setPreferredDeviceForStrategy},
    {"native_remove_preferred_device_for_strategy", "(I)I",
                        (void *)android_media_AudioSystem_removePreferredDeviceForStrategy},
};

int register_android_media_AudioProductStrategies(JNIEnv *env)
{
    jclass arrayListClass = FindClassOrDie(env, "java/util/ArrayList");
    gArrayListClass = MakeGlobalRefOrDie(env, arrayListClass);
    gArrayListMethods.add = GetMethodIDOrDie(env, arrayListClass, "add", "(Ljava/lang/Object;)Z");
    gArrayListMethods.toArray = GetMethodIDOrDie(env, arrayListClass,
                                                 "toArray", "()[Ljava/lang/Object;");

    jclass audioProductStrategyClass = FindClassOrDie(env, kAudioProductStrategyClassPathName);
    gAudioProductStrategyClass = MakeGlobalRefOrDie(env, audioProductStrategyClass);
    gAudioProductStrategyCstor = GetMethodIDOrDie(
                env, audioProductStrategyClass, "<init>",
                "(Ljava/lang/String;I[Landroid/media/audiopolicy/AudioProductStrategy$AudioAttributesGroup;)V");
    gAudioProductStrategyFields.mAudioAttributesGroups = GetFieldIDOrDie(
                env, audioProductStrategyClass, "mAudioAttributesGroups",
                "[Landroid/media/audiopolicy/AudioProductStrategy$AudioAttributesGroup;");
    gAudioProductStrategyFields.mName = GetFieldIDOrDie(
                env, audioProductStrategyClass, "mName", "Ljava/lang/String;");
    gAudioProductStrategyFields.mId = GetFieldIDOrDie(
                env, audioProductStrategyClass, "mId", "I");

    jclass audioAttributesGroupClass = FindClassOrDie(env, kAudioAttributesGroupsClassPathName);
    gAudioAttributesGroupClass = MakeGlobalRefOrDie(env, audioAttributesGroupClass);
    gAudioAttributesGroupCstor = GetMethodIDOrDie(env, audioAttributesGroupClass, "<init>",
                                                  "(II[Landroid/media/AudioAttributes;)V");
    gAudioAttributesGroupsFields.mVolumeGroupId = GetFieldIDOrDie(
                env, audioAttributesGroupClass, "mVolumeGroupId", "I");
    gAudioAttributesGroupsFields.mLegacyStreamType = GetFieldIDOrDie(
                env, audioAttributesGroupClass, "mLegacyStreamType", "I");
    gAudioAttributesGroupsFields.mAudioAttributes = GetFieldIDOrDie(
                env, audioAttributesGroupClass, "mAudioAttributes",
                "[Landroid/media/AudioAttributes;");

    env->DeleteLocalRef(audioProductStrategyClass);
    env->DeleteLocalRef(audioAttributesGroupClass);

    jclass pairClass = FindClassOrDie(env, "android/util/Pair");
    gPairClass = MakeGlobalRefOrDie(env, pairClass);
    gPairCstor = GetMethodIDOrDie(env, pairClass, "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");

    return RegisterMethodsOrDie(env, kClassPathName, gMethods, NELEM(gMethods));
}
