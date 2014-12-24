LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := nettool
LOCAL_SRC_FILES := nettool.c
LOCAL_CFLAGS += -std=c99 -Wall -O2

include $(BUILD_SHARED_LIBRARY)
