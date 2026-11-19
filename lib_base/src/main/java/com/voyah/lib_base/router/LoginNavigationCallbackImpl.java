package com.voyah.lib_base.router;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.callback.NavigationCallback;
import com.alibaba.android.arouter.launcher.ARouter;

public class LoginNavigationCallbackImpl implements NavigationCallback {
    @Override
    public void onFound(Postcard postcard) {
        //找到了
    }

    @Override
    public void onLost(Postcard postcard) {
        //找不到了

        System.out.println("拦截器：拦截了 找不到路由");
    }

    @Override
    public void onArrival(Postcard postcard) {
        //跳转成功了
    }

    @Override
    public void onInterrupt(Postcard postcard) {
//        String path = postcard.getPath();
//        Bundle bundle = postcard.getExtras();
        // 被登录拦截了下来了

        System.out.println("拦截器：拦截了 进入回调");
        // 需要调转到登录页面，把参数跟被登录拦截下来的路径传递给登录页面，登录成功后再进行跳转被拦截的页面
//        ARouter.getInstance().build(RouterConstants.MainRouterPath.MSG_CENTER)
//                .navigation();
    }
}
