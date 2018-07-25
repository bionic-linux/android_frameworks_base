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

// #define LOG_NDEBUG 0
#define LOG_TAG "android_os_NativeHandle"
#include <android-base/logging.h>

#include "android_os_NativeHandle.h"

#include <nativehelper/JNIHelp.h>

#include "core_jni_helpers.h"

#define PACKAGE_PATH    "android/os"
#define CLASS_NAME      "NativeHandle"
#define CLASS_PATH      PACKAGE_PATH "/" CLASS_NAME

namespace android {

static struct fields_t {
    jclass clazz;
    jmethodID constructID;  // NativeHandle(int[] fds, int[] ints)

    jmethodID getFdsID;  // int[] NativeHandle.getFds()
    jmethodID getIntsID;  // int[] NativeHandle.getInts()
} gFields;

jobject JNativeHandle::MakeJavaNativeHandleObj(
        JNIEnv *env, const native_handle_t *handle) {
    if (handle == nullptr) {
        jniThrowException(env, "java/lang/NullPointerException", nullptr);
        return nullptr;
    }

    const int numFds = handle->numFds;
    jintArray fds = env->NewIntArray(numFds);
    env->SetIntArrayRegion(fds, 0, numFds, &(handle->data[0]));

    const int numInts = handle->numInts;
    jintArray ints = env->NewIntArray(numInts);
    env->SetIntArrayRegion(ints, 0, numInts, &(handle->data[numFds]));

    return env->NewObject(gFields.clazz, gFields.constructID, fds, ints);
}

native_handle_t *JNativeHandle::MakeCppNativeHandle(
        JNIEnv *env, jobject jHandle, EphemeralStorage *storage) {
    if (jHandle == nullptr) {
        jniThrowException(env, "java/lang/NullPointerException", NULL);
        return nullptr;
    }
    if (!env->IsInstanceOf(jHandle, gFields.clazz)) {
        jniThrowException(env, "java/lang/UnsupportedOperationException",
                          nullptr);
        return nullptr;
    }

    jintArray fds = (jintArray) env->CallObjectMethod(
            jHandle, gFields.getFdsID);

    jintArray ints = (jintArray) env->CallObjectMethod(
            jHandle, gFields.getIntsID);

    const int numFds = (int) env->GetArrayLength(fds);
    const int numInts = (int) env->GetArrayLength(ints);

    native_handle_t *handle = (storage == nullptr)
            ? native_handle_create(numFds, numInts)
            : storage->allocTemporaryNativeHandle(numFds, numInts);

    if (handle != nullptr) {
        env->GetIntArrayRegion(fds, 0, numFds, &(handle->data[0]));
        env->GetIntArrayRegion(ints, 0, numInts, &(handle->data[numFds]));
    } else {
        jniThrowException(env, "java/lang/OutOfMemoryError", NULL);
    }

    return handle;
}

jobjectArray JNativeHandle::AllocJavaNativeHandleObjArray(
        JNIEnv *env, jsize length) {
    return env->NewObjectArray(length, gFields.clazz, NULL);
}

int register_android_os_NativeHandle(JNIEnv *env) {
    jclass clazz = FindClassOrDie(env, CLASS_PATH);
    gFields.clazz = MakeGlobalRefOrDie(env, clazz);
    gFields.constructID = GetMethodIDOrDie(env, clazz, "<init>", "([I[I)V");

    gFields.getFdsID = GetMethodIDOrDie(env, clazz, "getFds", "()[I");
    gFields.getIntsID = GetMethodIDOrDie(env, clazz, "getInts", "()[I");

    return 0;
}

}