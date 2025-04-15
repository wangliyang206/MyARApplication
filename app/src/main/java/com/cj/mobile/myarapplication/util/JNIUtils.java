package com.cj.mobile.myarapplication.util;

/**
 * 注意：如果此类要变动路径，请同步修改 app/src/main/jni 文件夹下的包含“com_cj_mobile_myarapplication”路径内容。
 */
public class JNIUtils {

    static {
        System.loadLibrary("JNI_APP");
    }

    public static native int[] doGrayScale(int[] buf, int w, int h);

    public static native String doFaceSwap(String[] paths);
}
