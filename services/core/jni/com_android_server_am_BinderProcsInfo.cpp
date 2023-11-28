/*
 * Copyright (C) 2023 The Android Open Source Project
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

#include <binderdebug/BinderDebug.h>
#include <core_jni_helpers.h>
#include <jni.h>
#include <nativehelper/JNIHelp.h>
#include <nativehelper/ScopedLocalRef.h>
#include <nativehelper/ScopedPrimitiveArray.h>

namespace android {

static struct {
    jclass clazz;
    jfieldID scannedPids;
    jfieldID trLines;
    jfieldID status;
} gBinderProcsInfoClass;

static struct {
    jclass clazz;
} gStringClass;

static void nGetBinderTransactionInfo(JNIEnv *env, jclass clazz, jint pid, jobject bpi) {
    BinderTransactionInfo trInfo;
    // Search all processes regardless context when looking for binder transaction chains
    status_t status =
            getBinderTransactions(BinderDebugContext::ALLBINDERS, static_cast<pid_t>(pid), trInfo);
    env->SetIntField(bpi, gBinderProcsInfoClass.status, status);

    jintArray scannedPids = env->NewIntArray((jint)trInfo.scannedPids.size());
    if (scannedPids != nullptr) {
        jint *arrayValues = env->GetIntArrayElements(scannedPids, nullptr);
        for (int ix = 0; ix < trInfo.scannedPids.size(); ix++) {
            arrayValues[ix] = trInfo.scannedPids[ix];
        }
        env->SetObjectField(bpi, gBinderProcsInfoClass.scannedPids, scannedPids);
        env->ReleaseIntArrayElements(scannedPids, arrayValues, 0);
        env->DeleteLocalRef(scannedPids);
    }

    jobjectArray trLines = env->NewObjectArray(trInfo.trLines.size(), gStringClass.clazz, nullptr);
    if (trLines != nullptr) {
        for (int ix = 0; ix < trInfo.trLines.size(); ix++) {
            jstring trLine = env->NewStringUTF(trInfo.trLines[ix].c_str());
            if (trLine != nullptr) {
                env->SetObjectArrayElement(trLines, ix, trLine);
                env->DeleteLocalRef(trLine);
            }
        }
        env->SetObjectField(bpi, gBinderProcsInfoClass.trLines, trLines);
        env->DeleteLocalRef(trLines);
    }
}

static const JNINativeMethod sMethods[] = {
        {"nGetBinderTransactionInfo", "(ILcom/android/server/am/BinderProcsInfo;)V",
         (void *)nGetBinderTransactionInfo},
};

int register_android_server_am_BinderProcsInfo(JNIEnv *env) {
    int res = RegisterMethodsOrDie(env, "com/android/server/am/BinderProcsInfo", sMethods,
                                   NELEM(sMethods));

    jclass clazz = FindClassOrDie(env, "com/android/server/am/BinderProcsInfo");
    gBinderProcsInfoClass.clazz = MakeGlobalRefOrDie(env, clazz);
    gBinderProcsInfoClass.scannedPids =
            GetFieldIDOrDie(env, gBinderProcsInfoClass.clazz, "mScannedPids", "[I");
    gBinderProcsInfoClass.trLines =
            GetFieldIDOrDie(env, gBinderProcsInfoClass.clazz, "mTrLines", "[Ljava/lang/String;");
    gBinderProcsInfoClass.status =
            GetFieldIDOrDie(env, gBinderProcsInfoClass.clazz, "mStatus", "I");

    clazz = FindClassOrDie(env, "java/lang/String");
    gStringClass.clazz = MakeGlobalRefOrDie(env, clazz);
    return res;
}

} /* namespace android */
