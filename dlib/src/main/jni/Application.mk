NDK_TOOLCHAIN_VERSION := clang
APP_ABI := armeabi-v7a
APP_CPPFLAGS := -std=c++11 -frtti -fexceptions
APP_PLATFORM := android-8
APP_STL := gnustl_static
#APP_CFLAGS+=-DDLIB_NO_GUI_SUPPORT=on
#APP_CFLAGS+=-DDLIB_PNG_SUPPORT=off
APP_CFLAGS+=-DDLIB_JPEG_SUPPORT=on
APP_CFLAGS+=-DDLIB_JPEG_STATIC=on
