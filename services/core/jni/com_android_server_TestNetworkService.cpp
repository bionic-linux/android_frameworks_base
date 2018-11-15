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

#define LOG_NDEBUG 0

#define LOG_TAG "TestNetworkServiceJni"

#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <linux/if.h>
#include <linux/if_tun.h>
#include <linux/ipv6_route.h>
#include <linux/route.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>

#include <log/log.h>

#include "netutils/ifc.h"

#include "jni.h"
#include <android-base/unique_fd.h>
#include <nativehelper/JNIHelp.h>

namespace android {

//------------------------------------------------------------------------------

constexpr int SYSTEM_ERROR = -1;

static int create_tun_interface(const char* iface) {
    base::unique_fd tun(open("/dev/tun", O_RDWR | O_NONBLOCK));

    ifreq ifr{};

    // Allocate interface.
    ifr.ifr_flags = IFF_TUN | IFF_NO_PI;
    strncpy(ifr.ifr_name, iface, IFNAMSIZ - 1);
    if (ioctl(tun.get(), TUNSETIFF, &ifr)) {
        ALOGE("Cannot allocate TUN: %s", strerror(errno));
        return SYSTEM_ERROR;
    }

    // Activate interface using an unconnected datagram socket.
    base::unique_fd inet4CtrlSock(socket(AF_INET, SOCK_DGRAM, 0));
    ifr.ifr_flags = IFF_UP;

    if (ioctl(inet4CtrlSock.get(), SIOCSIFFLAGS, &ifr)) {
        ALOGE("Cannot activate %s: %s", ifr.ifr_name, strerror(errno));
        return SYSTEM_ERROR;
    }

    return tun.release();
}

static int reset_tun_interface(const char* iface) {
    ifreq ifr{};
    strncpy(ifr.ifr_name, iface, IFNAMSIZ - 1);

    // Send reset signal (with no IFF_TUN or IFF_UP) to bring the interface down.
    base::unique_fd inet4CtrlSock(socket(AF_INET, SOCK_DGRAM, 0));
    if (ioctl(inet4CtrlSock.get(), SIOCSIFFLAGS, &ifr) && errno != ENODEV) {
        ALOGE("Cannot reset %s: %s", iface, strerror(errno));
        return SYSTEM_ERROR;
    }
    return 0;
}

//------------------------------------------------------------------------------

static void throwException(JNIEnv* env, int error, const char* message) {
    if (error == SYSTEM_ERROR) {
        jniThrowException(env, "java/lang/IllegalStateException", message);
    } else {
        jniThrowException(env, "java/lang/IllegalArgumentException", message);
    }
}

static jint create(JNIEnv* env, jobject /* thiz */, jstring jIface) {
    const char* iface = jIface ? env->GetStringUTFChars(jIface, NULL) : NULL;
    if (!iface) {
        // If we are here, jIface was null, and GetStringUTFChars was never called. Do not call
        // ReleaseStringUTFChars.
        jniThrowNullPointerException(env, "iface");
        return -1;
    }

    int tun = create_tun_interface(iface);

    // Always make sure to cleanup UTF string buffer after last use
    env->ReleaseStringUTFChars(jIface, iface);

    if (tun < 0) {
        throwException(env, tun, "Cannot create interface");
        return -1;
    }
    return tun;
}

static void reset(JNIEnv* env, jobject /* thiz */, jstring jIface) {
    const char* iface = jIface ? env->GetStringUTFChars(jIface, NULL) : NULL;
    if (!iface) {
        // If we are here, jIface was null, and GetStringUTFChars was never called. Do not call
        // ReleaseStringUTFChars.
        jniThrowNullPointerException(env, "iface");
        return;
    }

    int ret = reset_tun_interface(iface);

    // Always make sure to cleanup UTF string buffer after last use
    env->ReleaseStringUTFChars(jIface, iface);

    if (ret < 0) {
        throwException(env, SYSTEM_ERROR, "Cannot reset interface");
    }
}

//------------------------------------------------------------------------------

static const JNINativeMethod gMethods[] = {
    {"jniCreateTun", "(Ljava/lang/String;)I", (void*)create},
    {"jniTeardownTun", "(Ljava/lang/String;)V", (void*)reset},
};

int register_android_server_TestNetworkService(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "com/android/server/TestNetworkService", gMethods,
                                    NELEM(gMethods));
}

}; // namespace android
