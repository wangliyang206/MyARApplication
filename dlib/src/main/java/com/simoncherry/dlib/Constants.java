package com.simoncherry.dlib;

import android.os.Environment;

import java.io.File;

/**
 * Created by darrenl on 2016/4/22.
 */
public final class Constants {
    public static final String MODEL_PATH = "shape_predictor_68_face_landmarks.dat";

    private Constants() {
        // Constants should be prive
    }

    /**
     * getFaceShapeModelPath
     * @return default face shape model path
     */
    public static String getFaceShapeModelPath() {
        File sdcard = Environment.getExternalStorageDirectory();
        String targetPath = sdcard.getAbsolutePath() + File.separator + MODEL_PATH;
        return targetPath;
    }

//    public static String getFaceShapeModelPath() {
//        return "file:///android_asset/shape_predictor_68_face_landmarks.dat";
//    }
}
