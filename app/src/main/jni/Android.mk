LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
OpenCV_INSTALL_MODULES := on
OpenCV_CAMERA_MODULES := off
OPENCV_LIB_TYPE :=STATIC
ifeq ("$(wildcard $(OPENCV_MK_PATH))","")
include G:\OpenCV\opencv-3.4.16-android-sdk\OpenCV-android-sdk\sdk\native\jni\OpenCV.mk
else
include $(OPENCV_MK_PATH)
endif

LOCAL_MODULE := JNI_APP
LOCAL_SRC_FILES =: jni_app.cpp \
                   md5.cpp
LOCAL_LDLIBS +=  -lm -llog

include $(BUILD_SHARED_LIBRARY)