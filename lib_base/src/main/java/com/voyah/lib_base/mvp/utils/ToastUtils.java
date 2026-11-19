package com.voyah.lib_base.mvp.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;

import com.voyah.lib_base.R;


public class ToastUtils extends Toast {
    private static Toast mToast;
    /**
     * Construct an empty Toast object.  You must call {@link #setView} before you
     * can call {@link #show}.
     *
     * @param context The context to use.  Usually your {@link Application}
     *                or {@link Activity} object.
     */
    public ToastUtils(Context context) {
        super(context);
    }
    /**
     * 自定义Toast样式
     *
     * @description
     * @param context
     * @param text
     * @param duration
     *            hrq 2014-7-10下午2:15:36
     */
    public static Toast makeText(Context context, CharSequence text,
                                 int duration) {
        Toast result = new Toast(context);

        // 获取LayoutInflater对象
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // 由layout文件创建一个View对象
        View layout = inflater.inflate(R.layout.dialog_toast, null);

        // 实例化ImageView和TextView对象
        TextView textView = layout.findViewById(R.id.message);
        textView.setText(text);
        result.setView(layout);
        result.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        result.setDuration(duration);

        return result;
    }
    public static Toast makeTest(Context context,CharSequence text,int icon ){
        Toast result = new Toast(context);

        // 获取LayoutInflater对象
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // 由layout文件创建一个View对象
        View layout = inflater.inflate(R.layout.dialog_sucess_or_failed, null);

        // 实例化ImageView和TextView对象
        TextView textView = layout.findViewById(R.id.aso_tv_result);
        textView.setText(text);
        AppCompatImageView  imageView=layout.findViewById(R.id.aso_iv_result);
        imageView.setBackground(context.getResources().getDrawable(icon));
        result.setView(layout);
        result.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        result.setDuration(Toast.LENGTH_SHORT);
        return result;
    }
    public static void showToast(Context context,CharSequence text,int icon){
        mToast=makeTest(context,text,icon);
        mToast.show();
    }


    public static void showToast(Context context, String content) {

        mToast = makeText(context, content, Toast.LENGTH_LONG);
        mToast.show();
    }
}
