package com.cj.mobile.myarapplication.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @ProjectName: MyARApplication
 * @Package: com.cj.mobile.myarapplication.util
 * @ClassName: CommUtil
 * @Description:
 * @Author: WLY
 * @CreateDate: 2025/4/1 17:32
 */
public class CommUtil {
    private static final String TAG = "CommUtil";

    // 获取Asset文件实际大小（精确方法）
    public static long getAssetFileSize(InputStream is) throws IOException {
        long size = 0;
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = is.read(buffer)) != -1) {
            size += bytesRead;
        }
        return size;
    }

    /**
     * 将assets文件复制到缓存目录
     */
    public static File copyAssetToCache(Context mContext, String assetPath) throws IOException {
        File cacheFile = new File(mContext.getCacheDir(), assetPath);
        if (!cacheFile.getParentFile().exists()) {
            cacheFile.getParentFile().mkdirs();
        }

        if (cacheFile.exists()) {
            return cacheFile; // 直接返回已有文件
        }

        try (InputStream is = mContext.getAssets().open(assetPath);
             FileOutputStream os = new FileOutputStream(cacheFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
        return cacheFile;
    }
}
