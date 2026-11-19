package com.voyah.lib_base.mvp.utils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.voyah.lib_base.R;
import com.voyah.lib_base.mvp.base.BaseApplication;


/**
 * Author: "ChenboCui"
 * Date: 2020/8/19
 * Toast工具类
 */

public class TU {
    private static Toast toast;
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    private final static String THREAD_MAIN = "main";
    private static Dialog mDialog;

    public static void show(CharSequence text) {
        try {
            if (Looper.myLooper() == null) {
                Looper.prepare();
            }
            if (Looper.myLooper() != Looper.getMainLooper()) {
                return;
            }
         /*   if (toast == null)
                toast = Toast.makeText(BaseApplication.getAppContext(), BaseApplication.getAppContext().getString(R.string.net_message), Toast.LENGTH_SHORT);
            toast.setText(text);
            toast.show();*/
            showCentr(text.toString());
        } catch (Exception e) {

            //Toast.makeText(BaseApplication.getAppContext(), text, Toast.LENGTH_SHORT).show();
        }

    }

    public static void showAndCancel(CharSequence text) {
        try {
            if (Looper.myLooper() == null) {
                Looper.prepare();
            }
            if (Looper.myLooper() != Looper.getMainLooper()) {
                return;
            }
            if (toast!=null){
                toast.cancel();
            }
            toast = new Toast(BaseApplication.getAppContext());
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.setDuration(Toast.LENGTH_SHORT);
            View toastView = LayoutInflater.from(BaseApplication.getAppContext()).inflate(R.layout.login_toast_layout, null);
            TextView textView = toastView.findViewById(R.id.text);
            textView.setText(text);
            toast.setView(toastView);
            toast.show();
        } catch (Exception e) {

            //Toast.makeText(BaseApplication.getAppContext(), text, Toast.LENGTH_SHORT).show();
        }

    }

    public static void showR(CharSequence text) {
        if (toast != null)
            toast = null;

        toast = ToastUtils.makeText(BaseApplication.getAppContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }


    @SuppressLint("ShowToast")
    public static void showShortToast(String msg) {
        try {
            // 判断是在子线程，还是主线程
            if (THREAD_MAIN.equals(Thread.currentThread().getName())) {
                if (toast == null) {
                    toast = Toast.makeText(BaseApplication.getAppContext(), msg, Toast.LENGTH_SHORT);
                } else {
                    toast.cancel();
                    toast = Toast.makeText(BaseApplication.getAppContext(), msg, Toast.LENGTH_SHORT);
                    toast.setText(msg);
                }
                toast.show();
            } else {
                // 子线程
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BaseApplication.getAppContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
            //android API25系统BUG,TOAST异常
        }

    }

    public static void showCentr(String s) {
        Toast toast = new Toast(BaseApplication.getAppContext());
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        View toastView = LayoutInflater.from(BaseApplication.getAppContext()).inflate(R.layout.login_toast_layout, null);
        TextView textView = toastView.findViewById(R.id.text);
        textView.setText(s);
        toast.setView(toastView);
        toast.show();
    }

    /**
     * 带图片的吐司
     *
     * @param s          吐司内容
     * @param imageTipId 吐司内容顶部图片
     *                   ✓号：R.drawable.light_tip_succes
     *                   感叹号：R.drawable.icon_upload_fail.png
     *                   x号：R.drawable.light_tip_error
     */
    public static void showTip(String s, int imageTipId) {
        Toast toast = new Toast(BaseApplication.getAppContext());
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        View toastView = LayoutInflater.from(BaseApplication.getAppContext()).inflate(R.layout.dialog_light_tip_layout, null);
        TextView textView = toastView.findViewById(R.id.tip_title);
        ImageView tip_iv = toastView.findViewById(R.id.tip_iv);
        tip_iv.setImageResource(imageTipId);
        textView.setText(s);
        toast.setView(toastView);
        toast.show();
    }



    public interface ToastCallback {
        void onToastHidden();
    }


}
