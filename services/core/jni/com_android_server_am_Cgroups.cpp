/*
 * Copyright (C) 2019 The Android Open Source Project
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

#define LOG_TAG "AM-cgroups"

#include "jni.h"
#include <nativehelper/JNIHelp.h>
#include "android_runtime/AndroidRuntime.h"

#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <utils/Log.h>
#include <unistd.h>

#define CPUCTL_PATH "/dev/cpuctl"
#define ROOT_TASKS CPUCTL_PATH "/tasks"

#define APP_DIR_FORMAT CPUCTL_PATH "/app_uid_%d"
#define APP_PROCS_FORMAT APP_DIR_FORMAT "/cgroup.procs"
#define APP_SHARES_FORMAT APP_DIR_FORMAT "/cpu.shares"


extern "C" {

static void write_value(const char* path, int value) {
    int fd = open(path, O_WRONLY);
    if (fd == -1) {
        SLOGE("error opening %s: %s", path, strerror(errno));
        return;
    }

    char buf[21];
    sprintf(buf, "%d", value);
    if (write(fd, buf, strlen(buf)) == -1) {
        SLOGE("error writing %s to %s: %s", buf, path, strerror(errno));
    }

    if (close(fd) == -1) {
        SLOGE("error closing %s: %s", path, strerror(errno));
    }
}

void Java_com_android_server_am_Cgroups_putProc(JNIEnv* jni, jclass clazz, jint pid, jint uid) {
    char buf[sizeof(APP_PROCS_FORMAT) + 20]; // 20 is more than enough for the uid

    sprintf(buf, APP_DIR_FORMAT, uid);
    int ret = mkdir(buf, 0755) != 0;
    if (ret != 0 && errno != EEXIST) {
        SLOGE("error creating %s for pid %d: %s", buf, pid, strerror(errno));
        return;
    }

    sprintf(buf, APP_PROCS_FORMAT, uid);
    write_value(buf, pid);
}

void Java_com_android_server_am_Cgroups_putThreadInRoot(JNIEnv* jni, jclass clazz, jint tid) {
    write_value(ROOT_TASKS, tid);
}

void Java_com_android_server_am_Cgroups_uidPrio(JNIEnv* jni, jclass clazz, jint uid, jint shares) {
    char path[sizeof(APP_SHARES_FORMAT) + 20]; // 20 is more than enough for the uid

    sprintf(path, APP_SHARES_FORMAT, uid);
    write_value(path, shares);
}

};
