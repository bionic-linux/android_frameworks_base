/*
 * Copyright 2008, The Android Open Source Project
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

#define LOG_TAG "wifi"

#include "jni.h"
#include <ScopedUtfChars.h>
#include <utils/misc.h>
#include <android_runtime/AndroidRuntime.h>
#include <utils/Log.h>
#include <utils/String16.h>

#include "wifi.h"

#define WIFI_PKG_NAME "android/net/wifi/WifiNative"
#define REPLY_BUF_SIZE 4096 // wpa_supplicant's maximum size.
#define EVENT_BUF_SIZE 2048

namespace android {

static jint DBG = false;

static bool doCommand(const ScopedUtfChars& if_name, const ScopedUtfChars& cmd, char* reply, size_t reply_len) {
    if (DBG) {
        ALOGD("doCommand(if=%s): %s", if_name.c_str(), cmd.c_str());
    }

    if (if_name.c_str() == NULL || cmd.c_str() == NULL) {
        return false; // We already threw.
    }

    --reply_len; // Ensure we have room to add NUL termination.
    if (::wifi_command(if_name.c_str(), cmd.c_str(), reply, &reply_len) != 0) {
        return false;
    }

    // Strip off trailing newline.
    if (reply_len > 0 && reply[reply_len-1] == '\n') {
        reply[reply_len-1] = '\0';
    } else {
        reply[reply_len] = '\0';
    }
    return true;
}

static jint doIntCommand(const ScopedUtfChars& if_name, const ScopedUtfChars& cmd) {
    char reply[REPLY_BUF_SIZE];
    if (!doCommand(if_name, cmd, reply, sizeof(reply))) {
        return -1;
    }
    return static_cast<jint>(atoi(reply));
}

static jboolean doBooleanCommand(const ScopedUtfChars& if_name, const ScopedUtfChars& cmd) {
    char reply[REPLY_BUF_SIZE];
    if (!doCommand(if_name, cmd, reply, sizeof(reply))) {
        return JNI_FALSE;
    }
    return (strcmp(reply, "OK") == 0);
}

// Send a command to the supplicant, and return the reply as a String
static jstring doStringCommand(JNIEnv* env, const ScopedUtfChars& if_name, const ScopedUtfChars& cmd) {
    char reply[REPLY_BUF_SIZE];
    if (!doCommand(if_name, cmd, reply, sizeof(reply))) {
        return NULL;
    }
    // TODO: why not just NewStringUTF?
    String16 str(reply);
    return env->NewString((const jchar *)str.string(), str.size());
}

static jboolean android_net_wifi_isDriverLoaded(JNIEnv* env, jobject)
{
    return (jboolean)(::is_wifi_driver_loaded() == 1);
}

static jboolean android_net_wifi_loadDriver(JNIEnv* env, jobject)
{
    return (jboolean)(::wifi_load_driver() == 0);
}

static jboolean android_net_wifi_unloadDriver(JNIEnv* env, jobject)
{
    return (jboolean)(::wifi_unload_driver() == 0);
}

static jboolean android_net_wifi_startSupplicant(JNIEnv* env, jobject, jboolean p2pSupported)
{
    return (jboolean)(::wifi_start_supplicant(p2pSupported) == 0);
}

static jboolean android_net_wifi_killSupplicant(JNIEnv* env, jobject, jboolean p2pSupported)
{
    return (jboolean)(::wifi_stop_supplicant(p2pSupported) == 0);
}

static jboolean android_net_wifi_connectToSupplicant(JNIEnv* env, jobject, jstring jIface)
{
    ScopedUtfChars if_name(env, jIface);
    return (jboolean)(::wifi_connect_to_supplicant(if_name.c_str()) == 0);
}

static void android_net_wifi_closeSupplicantConnection(JNIEnv* env, jobject, jstring jIface)
{
    ScopedUtfChars if_name(env, jIface);
    ::wifi_close_supplicant_connection(if_name.c_str());
}

static jstring android_net_wifi_waitForEvent(JNIEnv* env, jobject, jstring jIface)
{
    char buf[EVENT_BUF_SIZE];
    ScopedUtfChars if_name(env, jIface);
    int nread = ::wifi_wait_for_event(if_name.c_str(), buf, sizeof buf);
    if (nread > 0) {
        return env->NewStringUTF(buf);
    } else {
        return NULL;
    }
}

static jboolean android_net_wifi_doBooleanCommand(JNIEnv* env, jobject, jstring jIface,
        jstring jCommand)
{
    ScopedUtfChars if_name(env, jIface);
    ScopedUtfChars command(env, jCommand);

    if (command.c_str() == NULL) {
        return JNI_FALSE;
    }
    return doBooleanCommand(if_name, command);
}

static jint android_net_wifi_doIntCommand(JNIEnv* env, jobject, jstring jIface,
        jstring jCommand)
{
    ScopedUtfChars if_name(env, jIface);
    ScopedUtfChars command(env, jCommand);

    if (command.c_str() == NULL) {
        return -1;
    }
    return doIntCommand(if_name, command);
}

static jstring android_net_wifi_doStringCommand(JNIEnv* env, jobject, jstring jIface,
        jstring jCommand)
{
    ScopedUtfChars if_name(env, jIface);

    ScopedUtfChars command(env, jCommand);
    if (command.c_str() == NULL) {
        return NULL;
    }
    return doStringCommand(env, if_name, command);
}



// ----------------------------------------------------------------------------

/*
 * JNI registration.
 */
static JNINativeMethod gWifiMethods[] = {
    /* name, signature, funcPtr */

    { "loadDriver", "()Z",  (void *)android_net_wifi_loadDriver },
    { "isDriverLoaded", "()Z",  (void *)android_net_wifi_isDriverLoaded },
    { "unloadDriver", "()Z",  (void *)android_net_wifi_unloadDriver },
    { "startSupplicant", "(Z)Z",  (void *)android_net_wifi_startSupplicant },
    { "killSupplicant", "(Z)Z",  (void *)android_net_wifi_killSupplicant },
    { "connectToSupplicant", "(Ljava/lang/String;)Z",
            (void *)android_net_wifi_connectToSupplicant },
    { "closeSupplicantConnection", "(Ljava/lang/String;)V",
            (void *)android_net_wifi_closeSupplicantConnection },
    { "waitForEvent", "(Ljava/lang/String;)Ljava/lang/String;",
            (void*) android_net_wifi_waitForEvent },
    { "doBooleanCommand", "(Ljava/lang/String;Ljava/lang/String;)Z",
            (void*) android_net_wifi_doBooleanCommand },
    { "doIntCommand", "(Ljava/lang/String;Ljava/lang/String;)I",
            (void*) android_net_wifi_doIntCommand },
    { "doStringCommand", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
            (void*) android_net_wifi_doStringCommand },
};

int register_android_net_wifi_WifiManager(JNIEnv* env)
{
    return AndroidRuntime::registerNativeMethods(env,
            WIFI_PKG_NAME, gWifiMethods, NELEM(gWifiMethods));
}

}; // namespace android
