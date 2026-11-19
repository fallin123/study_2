package com.voyah.lib_base.mvp.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.voyah.lib_base.mvp.base.BaseApplication;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static android.content.Context.TELEPHONY_SERVICE;

public class DeviceInfoModel {
    private static final String TAG = "DeviceInfoModel";
    private static DeviceInfoModel instance;

    public DeviceInfoModel() {
    }


    public static DeviceInfoModel getInstance() {

        if (instance == null) {
            instance = new DeviceInfoModel();
        }
        return instance;
    }

    /**
     * 获取机型
     */
    public String getPhoneModel() {
        //        TelephonyManager manager= (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        //        String mtype = android.os.Build.MODEL;
        String brand = Build.BRAND;//手机品牌
        String model = Build.MODEL;//手机型号
        Log.w(TAG, "手机型号：" + brand + " " + model);
        return brand + " " + model;
    }

    /**
     * 获取操作系统
     *
     * @return
     */
    public String getOS() {
       /* if (HarmonyOsUtil.isHarmonyOs()) {
            Log.w(TAG, "HarmonyOs操作系统:" + "HarmonyOs" + Build.VERSION.RELEASE);
            return "HarmonyOs" + HarmonyOsUtil.getHarmonyVersion();
        } else {
            Log.w(TAG, "Android操作系统:" + "Android" + Build.VERSION.RELEASE);
            return "Android" + Build.VERSION.RELEASE;
        }*/

        Log.w(TAG, "Android操作系统:" + "Android" + Build.VERSION.RELEASE);
        return "Android" + Build.VERSION.RELEASE;

    }


    /**
     * 获取手机分辨率
     *
     * @param context
     * @return
     */
    public String getResolution(Context context) {
        // 方法1 Android获得屏幕的宽和高
        WindowManager windowManager = ((Activity) context).getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();
        Log.w(TAG, "分辨率：" + screenWidth + "*" + screenHeight);
        return screenWidth + "*" + screenHeight;

    }

    /**
     * 获取手机高度
     */
    public int getHeight(Context context) {
        WindowManager windowManager = ((Activity) context).getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        return display.getHeight();
    }

    /**
     * 获取手机宽度
     */
    public int getWidth(Context context) {
        WindowManager windowManager = ((Activity) context).getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        return display.getWidth();
    }


    /**
     * 获取唯一设备号
     */
    public static String getUniqueID(Context context) {
        String id = null;
        final String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (!TextUtils.isEmpty(androidId) && !"9774d56d682e549c".equals(androidId)) {
            UUID uuid = UUID.nameUUIDFromBytes(androidId.getBytes(StandardCharsets.UTF_8));
            id = uuid.toString();
        }

        if (TextUtils.isEmpty(id)) {
            id = getUUID();
        }

        return TextUtils.isEmpty(id) ? UUID.randomUUID().toString() : id;
    }

    @SuppressLint("MissingPermission")
    private static String getUUID() {
        String serial = null;

        String m_szDevIDShort = "35" +
                Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +

                Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +

                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +

                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +

                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +

                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +

                Build.USER.length() % 10; //13 位

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                serial = Build.getSerial(); // TODO crash in Q
            } else {
                serial = Build.SERIAL;
            }
            //API>=9 使用serial号
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        } catch (Exception exception) {
            serial = "serial"; // 随便一个初始化
        }

        //使用硬件信息拼凑出来的15位号码
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }

    /**
     * 获取运营商
     *
     * @param context
     * @return
     */
    public String getNetOperator(Context context) {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        String iNumeric = manager.getSimOperator();
        String netOperator = "";
        if (iNumeric.length() > 0) {
            if (iNumeric.equals("46000") || iNumeric.equals("46002")) {
                // 中国移动
                netOperator = "中国移动";
            } else if (iNumeric.equals("46003")) {
                // 中国电信
                netOperator = "中国电信";
            } else if (iNumeric.equals("46001")) {
                // 中国联通
                netOperator = "中国联通";
            } else {
                //未知
                netOperator = "未知";
            }
        }
        Log.w(TAG, "运营商：" + netOperator);
        return netOperator;
    }


    /**
     * 获取联网方式
     */
    public String getNetMode(Context context) {
        String strNetworkType = "未知";
        //        TelephonyManager manager= (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        //        manager.getNetworkType();
        ConnectivityManager manager = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            int netMode = networkInfo.getType();
            if (netMode == ConnectivityManager.TYPE_WIFI) {
                strNetworkType = "WIFI";
                //wifi
            } else if (netMode == ConnectivityManager.TYPE_MOBILE) {
                int networkType = networkInfo.getSubtype();
                switch (networkType) {

                    //2g
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                    case TelephonyManager.NETWORK_TYPE_IDEN: //api<8 : replace by 11
                        strNetworkType = "2G";
                        break;

                    //3g
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B: //api<9 : replace by 14
                    case TelephonyManager.NETWORK_TYPE_EHRPD:  //api<11 : replace by 12
                    case TelephonyManager.NETWORK_TYPE_HSPAP:  //api<13 : replace by 15
                        strNetworkType = "3G";
                        break;

                    case TelephonyManager.NETWORK_TYPE_LTE:    //api<11 : replace by 13
                        strNetworkType = "4G";
                        break;

                    default:
                        String _strSubTypeName = networkInfo.getSubtypeName();
                        // http://baike.baidu.com/item/TD-SCDMA 中国移动 联通 电信 三种3G制式
                        if (_strSubTypeName.equalsIgnoreCase("TD-SCDMA") || _strSubTypeName.equalsIgnoreCase("WCDMA") || _strSubTypeName.equalsIgnoreCase("CDMA2000")) {
                            strNetworkType = "3G";
                        } else {
                            strNetworkType = _strSubTypeName;
                        }
                        break;
                }
            }
        }
        Log.w(TAG, "联网方式:" + strNetworkType);
        return strNetworkType;
    }

    private static boolean checkReadPhoneStatePermission(Context context) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                //ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_PHONE_STATE},
                  //      10);
                return false;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public static String getIMEI3(Context context) {
        //避免重复读取DeviceID
        if(!BaseApplication.deviceSN.equals("undefined") && !BaseApplication.deviceSN.isEmpty()) {
            return BaseApplication.deviceSN;
        }
        //        if (!checkReadPhoneStatePermission(context)) {
        //            Log.w(TAG, "获取唯一设备号-getIMEI: 无权限");
        //            return "";
        //        }
        //        String deviceId;
        //        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        //            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        //            deviceId = telephonyManager.getDeviceId();
        //        }else {
        //            //android 10以上已经获取不了imei了 用 android id代替
        //            deviceId = Settings.System.getString(
        //                    context.getContentResolver(), Settings.Secure.ANDROID_ID);
        //        }
        //
        //
        //
        //        return  deviceId;
        String uuid = (String) DBUtil.getParam("uuid", "");
        if (!TextUtils.isEmpty(uuid)) {
            Log.e("ffz", "getIMEI3:uuid==== " + uuid);
            return uuid;
        }
        String imei = getPhoneIMEI(context);
        if (!TextUtils.isEmpty(imei)) {
            Log.e("ffz", "getIMEI3:imei==== " + imei);
            return imei;
        }
        String androidID = getAndroidID(context);
        if (!TextUtils.isEmpty(androidID)) {
            Log.e("ffz", "getIMEI3:androidID==== " + androidID);
            return androidID;
        }
        uuid = UUID.randomUUID().toString();
        if (!TextUtils.isEmpty(uuid)) {
            uuid = getVersionName(context) + uuid;
            Log.e("ffz", "getIMEI3:UUID.randomUUID()==== " + uuid);
        } else {
            uuid = BaseApplication.deviceSN;
            Log.e("ffz", "getIMEI3:BaseApplication.deviceSN==== " + uuid);
        }
        DBUtil.setParam("uuid", uuid);
        return uuid;
    }

    /**
     * 获取设备的ANDROID_ID
     *
     * @param context
     * @return
     */
    public static String getAndroidID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * 获取设备号
     *
     * @param context
     * @return
     */
    public static String getPhoneIMEI(Context context) {
        if (!checkReadPhoneStatePermission(context)) {
            Log.e("ffz", "获取唯一设备号-getIMEI: 无权限");
            return "";
        }
        try {
            return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static int getVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }


    @SuppressLint("MissingPermission")
    public String getIMEI(Context context) {
        if (!checkReadPhoneStatePermission(context)) {
            Log.w(TAG, "获取唯一设备号-getIMEI: 无权限");
            return "";
        }
        String imei1 = "";
        TelephonyManager mTelephonyMgr = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        if (null != mTelephonyMgr) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                imei1 = mTelephonyMgr.getImei(0);
                Log.i(TAG, "Android版本大于o-26-优化后的获取---imei-1:" + imei1);
            } else {
                try {
                    imei1 = getDoubleImei(mTelephonyMgr, "getDeviceIdGemini", 0);
                } catch (Exception e) {
                    try {
                        imei1 = getDoubleImei(mTelephonyMgr, "getDeviceId", 0);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    Log.e(TAG, "get device id fail: " + e);
                }
            }
        }

        Log.i(TAG, "优化后的获取---imei1：" + imei1);
        return imei1;
    }

    @SuppressLint("MissingPermission")
    public String getIMEI2(Context context) {
        if (!checkReadPhoneStatePermission(context)) {
            Log.w(TAG, "获取唯一设备号-getIMEI2: 无权限");
            return "";
        }
        String imei2 = "";
        TelephonyManager mTelephonyMgr = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        if (null != mTelephonyMgr) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                imei2 = mTelephonyMgr.getImei(1);
                mTelephonyMgr.getMeid();
                Log.i(TAG, "Android版本大于o-26-优化后的获取---imei-2:" + imei2);
            } else {
                try {
                    imei2 = getDoubleImei(mTelephonyMgr, "getDeviceIdGemini", 1);
                } catch (Exception e) {
                    try {
                        imei2 = getDoubleImei(mTelephonyMgr, "getDeviceId", 1);
                    } catch (Exception ex) {
                        Log.e(TAG, "get device id fail: " + e);
                    }
                }
            }
        }

        Log.i(TAG, "优化后的获取--- imei2:" + imei2);
        return imei2;
    }

    /**
     * 获取双卡手机的imei
     */
    private String getDoubleImei(TelephonyManager telephony, String predictedMethodName, int slotID) throws Exception {
        String inumeric = null;

        Class<?> telephonyClass = Class.forName(telephony.getClass().getName());
        Class<?>[] parameter = new Class[1];
        parameter[0] = int.class;
        Method getSimID = telephonyClass.getMethod(predictedMethodName, parameter);
        Object[] obParameter = new Object[1];
        obParameter[0] = slotID;
        Object ob_phone = getSimID.invoke(telephony, obParameter);
        if (ob_phone != null) {
            inumeric = ob_phone.toString();
        }
        return inumeric;
    }
}
