LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	EGLUtils.cpp \
	EventHub.cpp \
	EventRecurrence.cpp \
	FramebufferNativeWindow.cpp \
	GraphicBuffer.cpp \
	GraphicBufferAllocator.cpp \
	GraphicBufferMapper.cpp \
	GraphicLog.cpp \
	KeyLayoutMap.cpp \
	KeyCharacterMap.cpp \
	Input.cpp \
	InputDispatcher.cpp \
	InputManager.cpp \
	InputReader.cpp \
	InputTransport.cpp \
	IOverlay.cpp \
	Overlay.cpp \
	PixelFormat.cpp \
	Rect.cpp \
	Region.cpp

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libutils \
	libEGL \
	libbinder \
	libpixelflinger \
	libhardware \
	libhardware_legacy

LOCAL_MODULE:= libui

ifeq ($(TARGET_SIMULATOR),true)
    LOCAL_LDLIBS += -lpthread
endif

ifeq ($(TARGET_BUILD_VARIANT), user)
    LOCAL_CFLAGS += -DBUILD_VARIANT_TYPE_USER
else ifeq ($(TARGET_BUILD_VARIANT), userdebug)
    LOCAL_CFLAGS += -DBUILD_VARIANT_TYPE_USERDEBUG
else ifeq ($(TARGET_BUILD_VARIANT), eng)
    LOCAL_CFLAGS += -DBUILD_VARIANT_TYPE_ENG
else
    $(error "TARGET_BUILD_VARIANT not set correctly: $(TARGET_BUILD_VARIANT)")
endif

include $(BUILD_SHARED_LIBRARY)


# Include subdirectory makefiles
# ============================================================

# If we're building with ONE_SHOT_MAKEFILE (mm, mmm), then what the framework
# team really wants is to build the stuff defined by this makefile.
ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call first-makefiles-under,$(LOCAL_PATH))
endif
