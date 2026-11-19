package com.voyah.lib_base.mvp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.voyah.lib_base.mvp.base.BaseApplication;


/**
 * SharedPreferences的一个工具类，调用setParam就能保存String, Integer, Boolean, Float, Long类型的参数
 * 同样调用getParam就能获取到保存在手机里面的数据
 */
public class DBUtil {
    private static final String FILE_NAME = "DongFeng";
    private static final String SHARE_NAME = "shareName";
    public static final String SGIN_NAME = "SGIN_NAME";
    public static final String UPDATE_VERSION = "UPDATE_VERSION";
    public static final String UPDATE_VERSION_NAME = "UPDATE_VERSION_NAME";
    private static final String APP_NAME = "AppName";
    public static String RES_PUBLIC = "";
    public static String AES_PRIVATE = "";
    public static final String SHOW_UPDATE_VERSION = "show_updateversion";
    public static boolean SHOW_MAIN_UPGRADE = true;

    public static SharedPreferences mSharePres = BaseApplication.getAppContext().getSharedPreferences(FILE_NAME,
            Context.MODE_PRIVATE);
    public static SharedPreferences mAppPresShare = BaseApplication.getAppContext().getSharedPreferences(APP_NAME,
            Context.MODE_PRIVATE); //退出帐号不会清理
    private static final String COOKIE = "cookie";


    public static void setUpdateVersionTag(boolean isShow) {
        setParam(SHOW_UPDATE_VERSION, isShow, UPDATE_VERSION);
    }

    public static Boolean getUpdateVersionTag() {
        return (Boolean) getParam(SHOW_UPDATE_VERSION, true, UPDATE_VERSION);
    }

    public static void setUpdateVersionName(String versionName) {
        setParam(SHOW_UPDATE_VERSION, versionName, UPDATE_VERSION_NAME);
    }

    public static String getUpdateVersionName() {
        String name = (String) getParam(SHOW_UPDATE_VERSION, "", UPDATE_VERSION_NAME);
        return name;
    }


    public static void setCookie(String cookie) {
        setParam(COOKIE, cookie);
    }

    public static String getCookie() {
        return (String) getParam(COOKIE, "");
    }



    public static final String USER_TOKEN = "user_token";

    public static void setUserToken(String userToken) {
        setParam(USER_TOKEN, userToken);
    }

    public static String getUserToken() {
        String userToken = (String) getParam(USER_TOKEN, "");
        return userToken;
    }

    public static final String USER_STORE = "user_store";

    public static void setUserStore(String userToken) {
        setParam(USER_STORE, userToken);
    }

    public static String getUserStore() {
        String userToken = (String) getParam(USER_STORE, "");
        return userToken;
    }

    /**
     * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
     *
     * @param key
     * @param object
     */
    public static void setParam(String key, Object object) {
        if (object != null) {
            SharedPreferences.Editor editor = mSharePres.edit();
            if (object instanceof String) {
                editor.putString(key, (String) object);
            } else if (object instanceof Integer) {
                editor.putInt(key, (Integer) object);
            } else if (object instanceof Boolean) {
                editor.putBoolean(key, (Boolean) object);
            } else if (object instanceof Float) {
                editor.putFloat(key, (Float) object);
            } else if (object instanceof Long) {
                editor.putLong(key, (Long) object);
            } else {
                editor.putString(key, object.toString());
            }

            editor.apply();
        }

    }

    /**
     * 得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
     *
     * @param key
     * @param defaultObject
     * @return
     */
    public static Object getParam(String key, Object defaultObject) {
        String type = defaultObject.getClass().getSimpleName();
        if ("String".equals(type)) {
            return mSharePres.getString(key, (String) defaultObject);
        } else if ("Integer".equals(type)) {
            return mSharePres.getInt(key, (Integer) defaultObject);
        } else if ("Boolean".equals(type)) {
            return mSharePres.getBoolean(key, (Boolean) defaultObject);
        } else if ("Float".equals(type)) {
            return mSharePres.getFloat(key, (Float) defaultObject);
        } else if ("Long".equals(type)) {
            return mSharePres.getLong(key, (Long) defaultObject);
        }

        return null;
    }


    /**
     * 得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
     *
     * @param key
     * @param defaultObject
     * @return
     */
    public static Object getParam(String key, Object defaultObject, String sharedPreferences) {
        SharedPreferences mSharePresShare = BaseApplication.getAppContext().getSharedPreferences(sharedPreferences,
                Context.MODE_PRIVATE);
        String type = defaultObject.getClass().getSimpleName();
        if ("String".equals(type)) {
            return mSharePresShare.getString(key, (String) defaultObject);
        } else if ("Integer".equals(type)) {
            return mSharePresShare.getInt(key, (Integer) defaultObject);
        } else if ("Boolean".equals(type)) {
            return mSharePresShare.getBoolean(key, (Boolean) defaultObject);
        } else if ("Float".equals(type)) {
            return mSharePresShare.getFloat(key, (Float) defaultObject);
        } else if ("Long".equals(type)) {
            return mSharePresShare.getLong(key, (Long) defaultObject);
        }

        return null;
    }


    /**
     * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
     *
     * @param key
     * @param object
     */
    public static void setParam(String key, Object object, String sharedPreferences) {
        SharedPreferences mSharePresShare = BaseApplication.getAppContext().getSharedPreferences(sharedPreferences,
                Context.MODE_PRIVATE);
        if (object != null) {
            SharedPreferences.Editor editor = mSharePresShare.edit();
            if (object instanceof String) {
                editor.putString(key, (String) object);
            } else if (object instanceof Integer) {
                editor.putInt(key, (Integer) object);
            } else if (object instanceof Boolean) {
                editor.putBoolean(key, (Boolean) object);
            } else if (object instanceof Float) {
                editor.putFloat(key, (Float) object);
            } else if (object instanceof Long) {
                editor.putLong(key, (Long) object);
            } else {
                editor.putString(key, object.toString());
            }

            editor.apply();
        }

    }


    //清除所有
    public static void clearData() {
        mSharePres.edit().clear().apply();
    }
}
