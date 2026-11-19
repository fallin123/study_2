package com.voyah.lib_base.mvp.utils.ingeek;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;


import java.io.File;
import java.util.List;

/**
 * Created by lxx  on 2019/5/14
 */
public class LogUtils {

    private static final String SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();


    private static String getLogFileDir(Context context) {
        return SDCARD + "/" + getProcessName(context) + "/xlog/InGeek_" + TimeUtil.NowDateToString(TimeUtil.DATE_TO_STRING) + ".xlog";
    }


    private static String getProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (null == am) return "";
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

//    public static void shareTextToWechat(Context context, String content) {
//        Intent wechatIntent = new Intent(Intent.ACTION_SEND);
//        wechatIntent.setPackage("com.tencent.mm");
//        wechatIntent.setType("text/plain");
//        wechatIntent.putExtra(Intent.EXTRA_TEXT, content);
//        context.startActivity(wechatIntent);
//    }

    /**
     * 调用系统分享功能分享日志文件到微信
     *
     * @param context context
     */
    public static void shareLogFileToWechat(Context context) {
        File logFile = new File(LogUtils.getLogFileDir(context));
        if (!logFile.exists()) {
            Toast.makeText(context, "无对应日志", Toast.LENGTH_LONG).show();
            return;
        }
        Uri uri = FileUtils.getFileUri(context, logFile);
        Intent shareIntent = new Intent();
        shareIntent.setType("*/*");
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.addCategory("android.intent.category.DEFAULT");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (isWeixinAvailable(context)) {
            shareIntent.setPackage("com.tencent.mm");
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享日志文件"));
    }

    /**
     * 判断 用户是否安装微信客户端
     */
    private static boolean isWeixinAvailable(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(0);
        if (packageInfoList != null) {
            for (int i = 0; i < packageInfoList.size(); i++) {
                String pn = packageInfoList.get(i).packageName;
                if (pn.equals("com.tencent.mm")) {
                    return true;
                }
            }
        }
        return false;
    }

}
