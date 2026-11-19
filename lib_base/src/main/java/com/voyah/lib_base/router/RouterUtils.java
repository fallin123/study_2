package com.voyah.lib_base.router;

import android.app.Activity;
import android.os.Bundle;

import com.alibaba.android.arouter.launcher.ARouter;
import com.voyah.lib_base.mvp.base.BaseApplication;

import java.io.Serializable;
import java.util.ArrayList;

public class RouterUtils {
    public static void RouterStart(String path) {
        ARouter.getInstance().build(path).navigation(BaseApplication.getAppContext(), new LoginNavigationCallbackImpl());
    }
    public static void RouterStart(String path,int start,int end,Activity activity) {
        ARouter.getInstance().build(path).withTransition(start,end).navigation(activity, new LoginNavigationCallbackImpl());
    }
    public static void RouterStart(String path, Bundle bundle) {
        ARouter.getInstance().build(path).with(bundle).navigation(BaseApplication.getAppContext(), new LoginNavigationCallbackImpl());
    }

    public static void RouterStart(String path, IntentMap bean) {
            ARouter.getInstance().build(path).withSerializable("bean", bean).navigation(BaseApplication.getAppContext(), new LoginNavigationCallbackImpl());

    }

    public static void RouterStart(Activity activity, String path, int requestCode, IntentMap bean) {
        ARouter.getInstance().build(path).withSerializable("bean", bean).navigation(activity, requestCode, new LoginNavigationCallbackImpl());

    }

    public static void RouterStart(String path, boolean isLogin) {
            ARouter.getInstance().build(path).navigation(BaseApplication.getAppContext(), isLogin ? new LoginNavigationCallbackImpl() : null);

    }

    public static void RouterStart(String path, Serializable bean) {
            ARouter.getInstance().build(path).withSerializable("bean", bean).navigation(BaseApplication.getAppContext(), new LoginNavigationCallbackImpl());

    }
    public static void RouterStart(String path, ArrayList<Serializable> bean) {
        ARouter.getInstance().build(path).withSerializable("bean", bean).navigation(BaseApplication.getAppContext(), new LoginNavigationCallbackImpl());
    }

    public static void RouterStart(String path, int RequestCode, Activity activity, Serializable bean) {
            ARouter.getInstance().build(path).withSerializable("bean", bean).navigation(activity, RequestCode);
    }


}
