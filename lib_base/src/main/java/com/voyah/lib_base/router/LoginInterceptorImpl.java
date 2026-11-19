package com.voyah.lib_base.router;

import android.content.Context;
import android.text.TextUtils;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Interceptor;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.template.IInterceptor;
import com.voyah.lib_base.mvp.utils.DBUtil;

@Interceptor(priority = 1)
public class LoginInterceptorImpl implements IInterceptor {
    @Override
    public void process(Postcard postcard, InterceptorCallback callback) {
        String path = postcard.getPath();
        boolean isLogin = !(TextUtils.isEmpty(DBUtil.getUserToken()));
        System.out.println("拦截器：" + path);
        if (isLogin) {
            //登录了，不拦截
            callback.onContinue(postcard);
        } else {
            switch (path) {
                // 不需要登录的直接进入这个页面
                case RouterConstants.MainRouterPath.ACTIVITYMAIN:
                    callback.onContinue(postcard);
                    System.out.println("拦截器：没拦截" + path);
                    break;

                default:
                    System.out.println("拦截器：拦截了" + path);
                    // 需要登录的直接拦截下来
                    callback.onInterrupt(null);
                    break;
            }
        }

    }

    @Override
    public void init(Context context) {
        System.out.println("路由登录拦截器初始化成功");
    }
}
