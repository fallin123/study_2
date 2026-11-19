package com.voyah.lib_base.mvp.utils;

import android.util.Log;

import com.google.gson.Gson;

public class LogUtil {
    private static Gson gson;
    /**
     * 截断输出日志
     * @param msg
     */
    public static void e(String tag, String msg) {
        try {
            if (tag == null || tag.length() == 0
                    || msg == null || msg.length() == 0)
                return;

            int segmentSize = 3 * 1024;
            long length = msg.length();
            if (length <= segmentSize ) {// 长度小于等于限制直接打印
                Log.e(tag, msg);
            }else {
                while (msg.length() > segmentSize ) {// 循环分段打印日志
                    String logContent = msg.substring(0, segmentSize );
                    msg = msg.replace(logContent, "");
                    Log.e(tag, logContent);
                }
                Log.e(tag, msg);// 打印剩余日志
            }
        } catch (Exception e) {
        }

    }


    /**
     * 截断输出日志
     * @param msg
     */
    public static void d(String tag, String msg) {
        if (tag == null || tag.length() == 0
                || msg == null || msg.length() == 0)
            return;

        int segmentSize = 2 * 1024;
        long length = msg.length();
        if (length <= segmentSize ) {// 长度小于等于限制直接打印
            Log.d(tag, msg);
        }else {
            while (msg.length() > segmentSize ) {// 循环分段打印日志
                String logContent = msg.substring(0, segmentSize );
                msg = msg.replace(logContent, "");
                Log.d(tag, logContent);
            }
            Log.d(tag, msg);// 打印剩余日志
        }
    }
    /**
     * 截断输出json
     * @param obj
     */
    public static void printObj(String tag, Object obj) {
        String msg="";

        if(obj!=null){
            try {
                if(obj instanceof String){
                    msg=(String) obj;
                }else {
                    if(gson==null){
                        gson=new Gson();
                    }

                    msg=gson.toJson(obj);
                }


            }catch (Exception e){

            }
        }


        if (tag == null || tag.length() == 0
                || msg == null || msg.length() == 0)
            return;

        int segmentSize = 2 * 1024;
        long length = msg.length();
        if (length <= segmentSize ) {// 长度小于等于限制直接打印
            Log.d(tag, msg);
        }else {
            while (msg.length() > segmentSize ) {// 循环分段打印日志
                String logContent = msg.substring(0, segmentSize );
                msg = msg.replace(logContent, "");
                Log.d(tag, logContent);
            }
            Log.d(tag, msg);// 打印剩余日志
        }
    }
}
