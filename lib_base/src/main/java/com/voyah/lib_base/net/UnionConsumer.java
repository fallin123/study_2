package com.voyah.lib_base.net;

import android.util.Log;

import com.voyah.lib_base.mvp.utils.CrashLogFileUtils;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import retrofit2.HttpException;

public abstract class UnionConsumer<T> implements Observer<T> {


    @Override
    public void onSubscribe(@NonNull Disposable d) {
        //在订阅的时候
        onStart();
    }


    @Override
    public void onComplete() {
        //可以在成功的时候
        onHide();
    }

    @Override
    public void onError(Throwable e) {
        //处理异常,可根据不同的异常状态,显示不同的状态view,统一管理
        //        if (e instanceof ReOtherException) {
        //            //            onFailed(BaseApplication.getAppContext().getString(R.string.phone_logout));
        //        } else {
//        onFailed(BaseApplication.getAppContext().getString(R.string.net_message));
        Log.e("onError", "message:" + e.toString());
//        onFailed(e.getLocalizedMessage());




        onFailed(getErrorMapping(e));




        //        }
        try {
            CrashLogFileUtils.INSTANCE.writerNetworkCrashInfo(((HttpException) e).response().toString());
        } catch (Exception e1) {

        }

    }


    @Override
    public void onNext(@NonNull T t) {
        onSucceed(t);
    }

    public abstract void onStart();

    public abstract void onSucceed(T bean);

    public abstract void onFailed(String msg);

    public abstract void onHide();




    private String getErrorMapping(Throwable e){
        if (e instanceof UnknownHostException){
            return "网络加载中……请切换网络再试";
        }else if (e instanceof SocketTimeoutException){
            return "网络连接超时……请检查网络后再试";
        }


        return e.getLocalizedMessage();
    }
}
