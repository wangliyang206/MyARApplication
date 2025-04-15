package com.cj.mobile.myarapplication;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

/**
 * @ProjectName: MyARApplication
 * @Package: com.cj.mobile.myarapplication
 * @ClassName: MultiDexApplication
 * @Description:
 * @Author: WLY
 * @CreateDate: 2025/3/20 12:56
 */
public class MultiDexApplication extends Application {

    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
