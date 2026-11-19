
package com.voyah.lib_base.mvp.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.voyah.lib_base.R;


/**
 * 加载数据的对话框
 *
 * @author peekaboo
 */
public class LoadingDataDialog extends Dialog {

    private boolean mCanceledOnTouchOutside = false;
    private boolean mCanceledOnTouchBackButton = true;
    private TextView mContentTextView = null;
    private View mRootView = null;
    private LottieAnimationView lottie_h_main_animation = null;

    private String msg;

    public LoadingDataDialog(Context context, int theme) {
        super(context, theme);
        init();
    }

    public LoadingDataDialog(Context context) {
        super(context, R.style.dialog_style);
        init();
    }

    public LoadingDataDialog(Context context, String msg) {
        super(context, R.style.dialog_style);
        this.msg = msg;
        init();
    }

    private void init() {
        //完全移除遮罩效果，解决弹出对话框和消失时，标题栏文字闪烁的bug
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        setContentView(R.layout.loading_layout_default);
        mContentTextView = (TextView) findViewById(R.id.textview_content);
        mRootView = findViewById(R.id.rootview);
        lottie_h_main_animation = findViewById(R.id.lottie_h_main_animation);
        lottie_h_main_animation.playAnimation();
        setCanceledOnTouchOutside(mCanceledOnTouchOutside);
        //mRootView.setBackgroundResource(R.color.loading_bg);

        Window window = getWindow();
        window.setGravity(Gravity.CENTER);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, dip2px(getContext(),300));

        enabledDimBehind(false);
        enabledBlackBorder(false);
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * 是否启用背景蒙层
     *
     * @param enable
     */
    public void enabledDimBehind(boolean enable) {
        if (!enable) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.dimAmount = 0;
            getWindow().setAttributes(lp);
        }
    }

    /**
     * 是否启用黑色背景边框样式
     *
     * @param enable
     */
    public void enabledBlackBorder(boolean enable) {
        if (enable) {
            //mRootView.setBackgroundResource(R.drawable.toast_background);
        }
    }

    public void setCanceledOnTouchOutside(boolean canceledOnTouchOutside) {
        this.mCanceledOnTouchOutside = canceledOnTouchOutside;
        super.setCanceledOnTouchOutside(canceledOnTouchOutside);
    }

    public void setCanceledOnTouchBackButton(boolean canceledOnTouchBackButton) {
        this.mCanceledOnTouchBackButton = canceledOnTouchBackButton;
        super.setCanceledOnTouchOutside(canceledOnTouchBackButton);
    }

    public void setContentText(String content) {
        if (TextUtils.isEmpty(content)) {
            //mContentTextView.setVisibility(View.GONE);
        } else {
             mContentTextView.setVisibility(View.VISIBLE);
             mContentTextView.setText(content);
        }

    }

    public void setContentTextColor(int color) {
        mContentTextView.setTextColor(color);
    }

    public void setContentTextColor(ColorStateList color) {
        mContentTextView.setTextColor(color);
    }

    public void setContentText(int contentResourceId) {
        setContentText(getContext().getString(contentResourceId));
    }

    @Override
    public void show() {
        try {
            super.show();
        } catch (Exception e) {
        }
    }

    @Override
    public void dismiss() {
        try {
            super.dismiss();
        } catch (Exception e) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mCanceledOnTouchOutside && mCanceledOnTouchBackButton) {
                    this.dismiss();
                    return true;
                }
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

}
