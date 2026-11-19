package com.voyah.lib_base.mvp.base;

import android.app.ActivityManager;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.List;
import com.voyah.lib_base.mvp.utils.DeviceInfoModel;

import java.util.Timer;

public class BaseApplication extends Application {
    /**
     * 系统上下文
     */
    private static Context mAppContext;
    private static BaseApplication mInstance;
    public static String deviceSN = "undefined";
    public static boolean IsReception = true;
    public static String sign = "";


    /*
     加载腾讯的XLog库
    */
    static {


    }


    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = getApplicationContext();
        mInstance = this;
        deviceSN = DeviceInfoModel.getIMEI3(mAppContext);
    }

    public static BaseApplication getApplicationInstance() {
        return mInstance;
    }

    /**
     * 获取系统上下文：用于ToastUtil类
     */
    public static Context getAppContext() {
        return mAppContext;
    }

    /**
     * 获取版本号和版本次数
     */
    public String getVersionCodeTime(int type) {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (type == 1) {
                return String.valueOf(pi.versionCode);
            } else if (type == 2) {
                return String.valueOf(pi.versionName);
            } else {
                return pi.packageName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (null == am) {
            return "";
        }
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (null == runningApps || runningApps.isEmpty()) {
            return "";
        }
        for (ActivityManager.RunningAppProcessInfo processInfo : runningApps) {
            if (processInfo.pid == pid) {
                return processInfo.processName;
            }
        }
        return "";
    }
}
