#define ATRACE_TAG ATRACE_TAG_RESOURCES

#include <iostream>
#include <sstream>

#include <binder/BinderService.h>
#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>

#include "android-base/macros.h"

#include "Idmap2Service.h"

using android::BinderService;
using android::IPCThreadState;
using android::ProcessState;
using android::sp;
using android::status_t;
using android::os::Idmap2Service;

int main(int argc ATTRIBUTE_UNUSED, char** argv ATTRIBUTE_UNUSED) {
  IPCThreadState::self()->disableBackgroundScheduling(true);
  status_t ret = BinderService<Idmap2Service>::publish();
  if (ret != android::OK) {
    return 1;
  }
  sp<ProcessState> ps(ProcessState::self());
  ps->startThreadPool();
  ps->giveThreadPoolName();
  IPCThreadState::self()->joinThreadPool();
  return 0;
}
