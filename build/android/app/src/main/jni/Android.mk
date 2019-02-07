# File: Android.mk
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := kuzzlesdk
LOCAL_SRC_FILES := ../../../../../../sdk-cpp/sdk-c/build/libkuzzlesdk.so
include $(PREBUILT_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE    := kuzzle
LOCAL_CFLAGS    := -frtti
LOCAL_CPP_FEATURES := rtti exceptions
LOCAL_CPPFLAGS += -std=gnu++11 -I../../../../../../sdk-cpp/include -I../../../../../../sdk-cpp/src -I../../../../../../sdk-cpp/build -I../../../../../../sdk-cpp/sdk-c/include -I../../../../../../sdk-cpp/sdk-c/build -I../../../../../../sdk-cpp/build/kuzzle-cpp-sdk/include
LOCAL_SHARED_LIBRARIES := kuzzlesdk
LOCAL_SRC_FILES := ../../../../../../kcore_wrap.cxx
include $(BUILD_SHARED_LIBRARY)
