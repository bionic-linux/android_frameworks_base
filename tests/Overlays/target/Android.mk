LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := OverlaysTarget

LOCAL_MODULE_TAGS := tests

LOCAL_USE_AAPT2 := true

LOCAL_CERTIFICATE := platform

# use LOCAL_PRIVATE_PLATFORM_APIS until the overlay manager is
# part of the public API
#LOCAL_SDK_VERSION := current
LOCAL_PRIVATE_PLATFORM_APIS := true

include $(BUILD_PACKAGE)
