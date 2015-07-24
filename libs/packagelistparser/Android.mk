LOCAL_PATH:= $(call my-dir)

#########################
include $(CLEAR_VARS)

LOCAL_MODULE := libpackagelistparser
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := packagelistparser.c

include $(BUILD_SHARED_LIBRARY)

#########################
include $(CLEAR_VARS)

LOCAL_MODULE := libpackagelistparser
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := packagelistparser.c

include $(BUILD_STATIC_LIBRARY)
