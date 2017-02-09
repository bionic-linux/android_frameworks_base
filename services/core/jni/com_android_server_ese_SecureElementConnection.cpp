/*
 * Copyright (C) 2017 The Android Open Source Project
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

#define LOG_TAG "SecureElementConnection"
#include "utils/Log.h"

#include "jni.h"
#include "JNIHelp.h"

#include <ese/ese.h>

#include <ese/hw/nxp/pn80t/boards/hikey-spidev.h>
ESE_INCLUDE_HW(ESE_HW_NXP_PN80T_SPIDEV);

namespace android
{

static int android_server_ese_SecureElementConnection_nativeConnect() {
    ALOGE("connect!!");
    struct EseInterface ese = ESE_INITIALIZER(ESE_HW_NXP_PN80T_SPIDEV);
    if (ese_open(&ese, (void *)(&nxp_boards_hikey_spidev))) {
        ALOGE("Cannot open connection to SE");
        // TODO: check ese_error
        return -1;
    }
    return 0;
}

static int android_server_ese_SecureElementConnection_nativeDisconnect() {
    return 0;
}

static int android_server_ese_SecureElementConnection_nativeTransceive(
        jbyteArray command, jbyteArray response) {
    return 0;
}

static const JNINativeMethod method_table[] = {
    { "nativeConnect", "()I",
            (void*)android_server_ese_SecureElementConnection_nativeConnect },
    { "nativeDisconnect", "()I",
            (void*)android_server_ese_SecureElementConnection_nativeDisconnect },
    { "nativeTransceive", "([B[B)I",
            (void*)android_server_ese_SecureElementConnection_nativeTransceive },
};

int register_android_server_ese_SecureElementConnection(JNIEnv* env) {
    ALOGE("register");
    return jniRegisterNativeMethods(env, "com/android/server/ese/SecureElementConnection",
            method_table, NELEM(method_table));
}

};
