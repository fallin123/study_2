package com.study.test;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.android.arouter.launcher.ARouter;
import com.voyah.lib_base.mvp.base.BaseApplication;
import com.voyah.lib_base.mvp.utils.LogUtil;
import com.voyah.lib_base.net.NetConstant;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public class MyApplication extends BaseApplication {
    /**
     * 维护Activity 的list
     */
    public static List<Activity> mActivitys = Collections
            .synchronizedList(new LinkedList<Activity>());
    /**
     * 维护配对码Dialog 的list
     */
    public static List<Dialog> mDialogs = Collections
            .synchronizedList(new LinkedList<Dialog>());
    public static final String TAG = "MyApplication";
    public static MyApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "----------------Application----------------onCreate");
        mInstance = this;
        NetConstant constant = new NetConstant();

        constant.setType("测试");
        constant.surroundings();

        ARouter.openLog();     // 打印日志
        ARouter.openDebug();   // 开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！线上版本需要关闭,否则有安全风险)
        ARouter.printStackTrace();
        ARouter.init(this); // 尽可能早，推荐在Application中初始化
        registerActivityLifecycleCallbacks(new StatisticActivityLifecycleCallback());
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                if (e instanceof UndeliverableException) {
                    Log.i(TAG, "setErrorHandler " + e.getMessage());
                } else if (e instanceof ClassCastException) {
                    Log.i(TAG, "setErrorHandler " + e.getMessage());
                } else {
                    Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }
        });

    }


    //回收所有Dialog
    public static void dismissPairCodeView() {
        if (mDialogs != null) {
            Iterator<Dialog> it = mDialogs.iterator();
            while (it.hasNext()) {
                Dialog dialog = it.next();
                it.remove();
                dialog.dismiss();
            }
        }
    }


    @Override
    public void onTerminate() {
        ARouter.getInstance().destroy();
        LogUtil.d(TAG, "------------------程序终止----------------onTerminate");
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
        LogUtil.d(TAG, "------------------低内存----------------onLowMemory");
        super.onLowMemory();
        LogUtil.d(TAG, "----------------wk 低内存onLowMemory 时 清除glide的内存缓存");
    }

    @Override
    public void onTrimMemory(int level) {
        LogUtil.d(TAG, "------------------回收内存----------------onTrimMemory");
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_MODERATE) {
            LogUtil.d(TAG, "-------------wk  低内存,进程可能被杀死的情况时 清除glide的内存缓存");
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        LogUtil.d(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

    /**
     * @param activity 作用说明 ：添加一个activity到管理里
     */
    public void pushActivity(Activity activity) {
        mActivitys.add(activity);
    }

    /**
     * @param activity 作用说明 ：删除一个activity在管理里
     */
    public void popActivity(Activity activity) {
        mActivitys.remove(activity);
    }

    public void popDialog(Activity activity) {
        if (mDialogs != null) {
            Iterator<Dialog> it = mDialogs.iterator();
            while (it.hasNext()) {
                Dialog dialog = it.next();
                if (dialog.getOwnerActivity() == activity) {
                    dialog.dismiss();
                    it.remove();
                }
            }
        }
    }


    class StatisticActivityLifecycleCallback implements Application.ActivityLifecycleCallbacks {
        private int foregroundActivities = 0;
        private boolean isChangingConfiguration;

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
            Log.i(TAG, "onActivityCreated bundle: " + bundle);
            /**
             *  监听到 Activity创建事件 将该 Activity 加入list
             */
            pushActivity(activity);
        }

        @Override
        public void onActivityStarted(Activity activity) {

            foregroundActivities++;
            if (foregroundActivities == 1 && !isChangingConfiguration) {
                // 应用切到前台
                Log.i(TAG, "application enter foreground");
//                EventBus.getDefault().post(EVENT_MAIN_START);
            }
            isChangingConfiguration = false;
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (null == mActivitys && mActivitys.isEmpty()) {
                return;
            }
            if (mActivitys.contains(activity)) {
                /**
                 *  监听到 Activity Resumed事件 将该Activity 从list中移除
                 *  再将该 Activity 加入list  用于Dialog获取当前Activity
                 */
                popActivity(activity);
                pushActivity(activity);
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
            foregroundActivities--;
            if (foregroundActivities == 0) {

                // 应用切到后台
                Log.i(TAG, "application enter background");
                //应用退到后台 断开长连接
//                EventBus.getDefault().post(EVENT_MAIN_STOP);
            }
            isChangingConfiguration = activity.isChangingConfigurations();
        }


        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (null == mActivitys && mActivitys.isEmpty()) {
                return;
            }

            /**
             *  监听到 Activity销毁事件 将该Dialog隐藏并从list中移除
             */
            popDialog(activity);

            if (mActivitys.contains(activity)) {
                /**
                 *  监听到 Activity销毁事件 将该Activity 从list中移除
                 */
                popActivity(activity);

            }

        }

    }

}