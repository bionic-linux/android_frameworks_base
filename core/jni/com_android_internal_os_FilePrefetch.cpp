/*
 * Copyright (C) 2008 The Android Open Source Project
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

#define LOG_TAG "FilePrefetch"

// sys/mount.h has to come before linux/fs.h due to redefinition of MS_RDONLY, MS_BIND, etc
#include <sys/mount.h>
#include <linux/fs.h>

#include <list>
#include <sstream>
#include <string>

#include <fcntl.h>
#include <grp.h>
#include <inttypes.h>
#include <malloc.h>
#include <mntent.h>
#include <paths.h>
#include <signal.h>
#include <stdlib.h>
#include <sys/capability.h>
#include <sys/cdefs.h>
#include <sys/personality.h>
#include <sys/prctl.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/utsname.h>
#include <sys/wait.h>
#include <unistd.h>

#include "android-base/logging.h"
#include <cutils/fs.h>
#include <cutils/multiuser.h>
#include <cutils/sched_policy.h>
#include <private/android_filesystem_config.h>
#include <utils/String8.h>
#include <selinux/android.h>
#include <processgroup/processgroup.h>

#include "core_jni_helpers.h"
#include <nativehelper/JNIHelp.h>
#include <nativehelper/ScopedLocalRef.h>
#include <nativehelper/ScopedPrimitiveArray.h>
#include <nativehelper/ScopedUtfChars.h>
#include "fd_utils.h"

#include "nativebridge/native_bridge.h"

namespace {

static constexpr off_t kPageSize = 0x1000;
static constexpr off_t kPagesPerFadvise = 32;
static constexpr off_t kBytesPerFadvise = kPagesPerFadvise * kPageSize;

static void RuntimeAbort(JNIEnv* env, int line, const char* msg) {
    std::ostringstream oss;
    oss << __FILE__ << ":" << line << ": " << msg;
    env->FatalError(oss.str().c_str());
}

class FdFile {
 public:
  explicit FdFile(int fd) : fd_(fd) { }
  int get() const { return fd_; }
  ~FdFile() { close(fd_); }
 private:
  int fd_;
  DISALLOW_COPY_AND_ASSIGN(FdFile);
};

static void prefetchFileChunk(const char* path_name, int offset, int length) {
  ALOGE("============== Prefetch: %s, %d, %d", path_name, offset, length);
  if (offset < 0 || length < 0) {
    ALOGE("============== Prefetch: offset or length is negative.");
    return;
  }
  FdFile fd(open(path_name, O_RDONLY));
  if (fd.get() < 0) {
    ALOGE("============== Prefetch: open failed: %d.", errno);
    return;
  }
  off_t aligned_offset = static_cast<off_t>(offset) & ~(kPageSize - 1);
  while (length > 0) {
    int retval = posix_fadvise(fd.get(),
                               aligned_offset,
                               std::min(static_cast<off_t>(length), kBytesPerFadvise),
                               POSIX_FADV_WILLNEED);
    if (retval != 0) {
      ALOGE("============== Prefetch: posix_fadvise failed: %d.", retval);
      return;
    }
    aligned_offset += kBytesPerFadvise;
    length -= kBytesPerFadvise;
  }
}

}  // anonymous namespace

namespace android {

static void com_android_internal_os_FilePrefetch_nativePrefetchFileChunk(
        JNIEnv* env, jclass, jstring path, jint start_offset, jint length) {
    ScopedUtfChars path_native(env, path);
    const char* path_cstr = path_native.c_str();
    if (!path_cstr) {
        RuntimeAbort(env, __LINE__, "path_cstr == NULL");
    }
    prefetchFileChunk(path_cstr, start_offset, length);
}

static const JNINativeMethod gMethods[] = {
    { "nativePrefetchFileChunk", "(Ljava/lang/String;II)V",
      (void *) com_android_internal_os_FilePrefetch_nativePrefetchFileChunk }
};

int register_com_android_internal_os_FilePrefetch(JNIEnv* env) {
  return RegisterMethodsOrDie(env,
                              "com/android/internal/os/FilePrefetch",
                              gMethods,
                              NELEM(gMethods));
}
}  // namespace android
