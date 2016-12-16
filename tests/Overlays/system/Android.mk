LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PACKAGE_NAME := OverlaysSystem

LOCAL_MODULE_TAGS := tests

LOCAL_CERTIFICATE := platform

LOCAL_SDK_VERSION := current

include $(BUILD_RRO_PACKAGE)
