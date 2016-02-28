LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := hellojni
LOCAL_SRC_FILES := hellojni.cpp
LOCAL_SHARED_LIBRARIES := libhello

include $(BUILD_SHARED_LIBRARY)
