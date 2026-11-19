package com.voyah.lib_base.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.ScrollView;

import com.voyah.lib_base.R;
import com.voyah.lib_base.utils.ScreenUtil;


public class MaxHeightScrollView extends ScrollView {

    private Context mContext;
    private int mMaxHeight;

    public MaxHeightScrollView(Context context) {
        super(context);
    }

    public MaxHeightScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public MaxHeightScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        mContext = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightScrollView);
        mMaxHeight = typedArray.getLayoutDimension(R.styleable.MaxHeightScrollView_maxHeightScrollView, mMaxHeight);
        typedArray.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //如果有设置最大高度则使用最大高度，如果没有则最大高度设置为屏幕高度的一半
        if (mMaxHeight > 0) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
        } else {
            try {
                //最大高度显示为屏幕内容高度的一半
                Display display = ((Activity) mContext).getWindowManager().getDefaultDisplay();
                DisplayMetrics d = new DisplayMetrics();
                display.getMetrics(d);
                int maxHeight = ScreenUtil.dip2px(mContext, 160);
                //此处是关键，设置控件高度不能超过屏幕高度一半（d.heightPixels / 2）（在此替换成自己需要的高度）
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //重新计算控件高、宽
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}


