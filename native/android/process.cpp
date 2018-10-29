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

#include <errno.h>
#include <sys/resource.h>
#include <sys/time.h>

#include <android/process.h>

#include <cutils/sched_policy.h>
#include <system/thread_defs.h>

//
// Validate priority levels
//
#define CHECK_PRIORITY(priority) \
  static_assert(THREAD_PRIORITY_ ## priority == ANDROID_PRIORITY_ ## priority, "Bad " #priority)

CHECK_PRIORITY(LOWEST);
CHECK_PRIORITY(BACKGROUND);
CHECK_PRIORITY(NORMAL);
CHECK_PRIORITY(FOREGROUND);
CHECK_PRIORITY(DISPLAY);
CHECK_PRIORITY(VIDEO);
CHECK_PRIORITY(AUDIO);
CHECK_PRIORITY(URGENT_AUDIO);
CHECK_PRIORITY(HIGHEST);

CHECK_PRIORITY(MORE_FAVORABLE);
CHECK_PRIORITY(LESS_FAVORABLE);

#undef CHECK_PRIORITY

int AProcess_setThreadPriority(int tid, int pri) {
  int rc = 0;

  if (pri >= THREAD_PRIORITY_BACKGROUND) {
    rc = set_sched_policy(tid, SP_BACKGROUND);
  } else if (getpriority(PRIO_PROCESS, tid) >= THREAD_PRIORITY_BACKGROUND) {
    rc = set_sched_policy(tid, SP_FOREGROUND);
  }

  if (setpriority(PRIO_PROCESS, tid, pri) < 0) {
    rc = -errno;
  }

  return rc;
}

int AProcess_getThreadPriority(int tid, int* priority) {
  errno = 0;
  int result = getpriority(PRIO_PROCESS, tid);
  if (result == -1 && errno != 0) {
    return -errno;
  }
  *priority = result;
  return 0;
}
