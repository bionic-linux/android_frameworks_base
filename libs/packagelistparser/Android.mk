LOCAL_PATH:= $(call my-dir)

#########################
include $(CLEAR_VARS)

LOCAL_MODULE := libpackagelistparser
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := packagelistparser.c
LOCAL_SHARED_LIBRARIES := libcutils

include $(BUILD_SHARED_LIBRARY)

#########################
include $(CLEAR_VARS)

LOCAL_MODULE := libpackagelistparser
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := packagelistparser.c
LOCAL_STATIC_LIBRARIES := libcutils

include $(BUILD_STATIC_LIBRARY)
