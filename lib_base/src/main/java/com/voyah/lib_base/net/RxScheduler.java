package com.voyah.lib_base.net;

import com.voyah.lib_base.mvp.bean.BaseResponseBean;
import com.voyah.lib_base.mvp.utils.DBUtil;

import org.greenrobot.eventbus.EventBus;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RxScheduler {


    /**
     * 统一线程处理
     *
     * @param <T> 指定的泛型类型
     * @return ObservableTransformer
     */
    public static <T> ObservableTransformer<BaseResponseBean<T>, BaseResponseBean<T>> Obs_io_main() {

        return upstream -> upstream.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).
                flatMap((Function<BaseResponseBean<T>, ObservableSource<BaseResponseBean<T>>>) response -> {
                    //根据自己的业务来定,
                    //如果是成功,就返回数据,当然,如果你自己的业务的字段不一样,可以根据自己的来修改
                    if (response.getCode() == 300040011) {
                        //单点登录
                        DBUtil.clearData();
                        EventBus.getDefault().post(EventConstant.LOGOUT_DIALOG);
                        return Observable.error(new ReOtherException());
                    }else {
                        return Observable.just(response);
                    }
                    //可以增加其他异常
                });
    }

}
