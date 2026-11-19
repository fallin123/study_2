
package com.voyah.lib_base.mvp.utils;

import android.app.Activity;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * 在首页想要退出APP时可以把已经加进来的activity逐一清除掉
 * 
 * @author wangdongsheng
 */
public class ActivityHelper {

    private static final String TAG = "ActivityHelper";

    private static final List<Activity> activitys = new LinkedList<Activity>();

    public static void addActivity(Activity activity) {
        Log.w(TAG, "add Activity in Helper === " + activity.getClass().getSimpleName());
        activitys.add(activity);
    }

    public static void removeActivity(Activity activity) {
        Log.w(TAG, "remove Activity in Helper === " + activity.getClass().getSimpleName());
        activitys.remove(activity);
    }

    public static void destoryActivitys() {
        Log.w(TAG, "Destory acticitys ");
        for (Activity activity : activitys) {
            if (activity != null) {
                activity.finish();
            }
        }
    }

    public static boolean isContainsHomeActicuty() {
        boolean contains = false;
        for (Activity activity : activitys) {
            if (activity.getClass().getSimpleName().equals("HomeActivity")) {
                contains = true;
            }
        }
        return contains;
    }

    public static boolean isContainsActicuty(String tag) {
        boolean contains = false;
        for (Activity activity : activitys) {
            if (activity.getClass().getSimpleName().equals(tag)) {
                contains = true;
            }
        }
        return contains;
    }

    
    /**
     * 得到Activity集合，用于判断手机是否退出App
     * @return  maxiaopeng
     */
    public static List<Activity> getActivityList() {
    	return activitys;
    }

    public static Activity getTopActivity() {
        // 确保activityStack不为null且不为空
        if (activitys != null && !activitys.isEmpty()) {
            // 返回栈顶的元素，注意这里直接取最后一个元素，需确保栈内元素类型正确
            return activitys.get(activitys.size() - 1);
        }
        // 如果栈为空或为null，则返回null（或抛出异常，根据需求决定）
        return null;
    }

}
