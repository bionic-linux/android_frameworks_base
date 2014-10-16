#
# Copyright (C) 2014 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

LOCAL_MODULE := frameworks-input-tests
LOCAL_MODULE_TAGS := eng tests
LOCAL_C_INCLUDES := external/skia/include/core

LOCAL_SRC_FILES := \
    InputReader_test.cpp \
    InputDispatcher_test.cpp \

LOCAL_SHARED_LIBRARIES := \
    libcutils \
    liblog \
    libandroidfw \
    libutils \
    libhardware \
    libhardware_legacy \
    libui \
    libskia \
    libinput \
    libinputservice \

include $(BUILD_NATIVE_TEST)
