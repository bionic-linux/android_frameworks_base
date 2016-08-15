# Copyright 2016 The Android Open Source Project

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := webview_zygote

LOCAL_SRC_FILES := webview_zygote.cpp

LOCAL_SHARED_LIBRARIES := \
	libandroid_runtime \
	libbinder \
	liblog \
	libcutils \
	libutils

LOCAL_LDFLAGS_32 := -Wl,--version-script,art/sigchainlib/version-script32.txt -Wl,--export-dynamic
LOCAL_LDFLAGS_64 := -Wl,--version-script,art/sigchainlib/version-script64.txt -Wl,--export-dynamic

LOCAL_WHOLE_STATIC_LIBRARIES := libsigchain

LOCAL_INIT_RC := webview_zygote32.rc

# Always include the 32-bit version of webview_zygote. If the target is 64-bit,
# also include the 64-bit webview_zygote.
ifeq ($(TARGET_SUPPORTS_64_BIT_APPS),true)
	LOCAL_INIT_RC += webview_zygote64.rc
endif

LOCAL_MULTILIB := both

LOCAL_MODULE_STEM_32 := webview_zygote32
LOCAL_MODULE_STEM_64 := webview_zygote64

include $(BUILD_EXECUTABLE)
