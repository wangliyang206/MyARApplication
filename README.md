AR相机
===================================
根据人脸图片构建简单的3D人脸模型，然后在摄像头预览画面中展示。基于该人脸模型进行换脸或者添加装饰品。

基于
----------------------------------- 
* [dlib-android-app](https://github.com/tzutalin/dlib-android-app)
* [dlib-android](https://github.com/tzutalin/dlib-android) 提供Android平台可用的Dlib库。
* [Rajawali](https://github.com/Rajawali/Rajawali) OpenGL ES引擎。

构建
-----------------------------------  
一、本项目构建版本：
    * Android ndk 17，配置路径：MyARApplication/local.properties中ndk.dir=F\:\\AndroidSDK\\ndk\\17.2.4988734
    * OpenCV 3.4.16，配置路径：
        1、app/src/main/jni/Android.mk 中 include G:\OpenCV\opencv-3.4.16-android-sdk\OpenCV-android-sdk\sdk\native\jni\OpenCV.mk  改为你本地OpenCV路径
        2、dlib/src/main/jni/Android.mk 中 OPENCV_PATH = G:\OpenCV\opencv-3.4.16-android-sdk\OpenCV-android-sdk\sdk\native\jni  改为你本地OpenCV路径
    * dlib 19.1
二、app中支持变动包名，变动后需要注意：
    * 将项目中包含“com_cj_mobile_myarapplication”的包名，替换成你项目的包名。