LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := nettool
LOCAL_SRC_FILES := nettool.c

include $(BUILD_SHARED_LIBRARY)
