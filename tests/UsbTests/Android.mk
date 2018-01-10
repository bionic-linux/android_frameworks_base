LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_STATIC_JAVA_LIBRARIES := \
    frameworks-base-testutils \
    android-support-test \
    mockito-target-minus-junit4 \
    platform-test-annotations \
    services.core \
    services.net \
    services.usb \
    truth-prebuilt \

LOCAL_CERTIFICATE := platform

LOCAL_PACKAGE_NAME := UsbTests

LOCAL_COMPATIBILITY_SUITE := device-tests

include $(BUILD_PACKAGE)
