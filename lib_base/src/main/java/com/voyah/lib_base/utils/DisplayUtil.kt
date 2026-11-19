package com.voyah.lib_base.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.voyah.lib_base.mvp.base.BaseApplication

/**
 * @author chenjiaming1 dp(dip): device independent pixels(设备独立像素).
 * 不同设备有不同的显示效果,这个和设备硬件有关，一般我们为了支持WVGA、HVGA和QVGA 推荐使用这个，不依赖像素。
 * dp也就是dip，这个和sp基本类似。如果设置表示长度、高度等属性时可以使用dp
 * 或sp。但如果设置字体，需要使用sp。dp是与密度无关，sp除了与密度无关外
 * ，还与scale无关。如果屏幕密度为160，这时dp和sp和px是一
 * 样的。1dp=1sp=1px，但如果使用px作单位，如果屏幕大小不变（
 * 假设还是3.2寸），而屏幕密度变成了320。那么原来TextView的宽度
 * 设成160px，在密度为320的3.2寸屏幕里看要比在密度为160的3
 * .2寸屏幕上看短了一半。但如果设置成160dp或160sp的话。系统会自动 将width属性值设置成320px的。也就是160 * 320
 * / 160。其中320 / 160可称为密度比例因子。也就是说，如果使用dp和sp，系统会根据屏幕密度的变化自动进行转换。 px:
 * pixels(像素). 不同设备显示效果相同，一般我们HVGA代表320x480像素，这个用的比较多。 pt:
 * point，是一个标准的长度单位，1pt＝1/72英寸，用于印刷业，非常简单易用； sp: scaled pixels(放大像素).
 * 主要用于字体显示best for textsize。
 */
@SuppressLint("StaticFieldLeak")
object DisplayUtil {
    private val mContext: Context = BaseApplication.getAppContext()

    /**
     * 将px值转换为dip或dp值，保证尺寸大小不变
     *
     * @param pxValue
     * @param  （DisplayMetrics类中属性density）
     * @return
     */
    @SuppressLint("StaticFieldLeak")
    fun px2dip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun px2dip(pxValue: Float): Int {
        return px2dip(mContext, pxValue)
    }

    /**
     * 将dip或dp值转换为px值，保证尺寸大小不变
     *
     * @param dipValue
     * @param  （DisplayMetrics类中属性density）
     * @return
     */
    fun dip2px(context: Context, dipValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }

    fun dip2px(dipValue: Float): Int {
        return dip2px(mContext, dipValue)
    }

    /**
     * 将px值转换为sp值，保证文字大小不变
     *
     * @param pxValue
     * @param  （DisplayMetrics类中属性scaledDensity）
     * @return
     */
    fun px2sp(context: Context, pxValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (pxValue / fontScale + 0.5f).toInt()
    }

    fun px2sp(pxValue: Float): Int {
        return px2sp(mContext, pxValue)
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     *
     * @param spValue
     * @param  （DisplayMetrics类中属性scaledDensity）
     * @return
     */
    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    fun sp2px(spValue: Float): Int {
        return sp2px(mContext, spValue)
    }

    val displayWidth: Int
        get() = getDisplayWidth(mContext)

    // 获取 设备的Width
    fun getDisplayWidth(context: Context): Int {
        val dm = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(dm)
        return dm.widthPixels
    }

    // 获取 设备的Width &Hight
    fun getDeviceWH(context: Context): IntArray {
        val wh = IntArray(2)
        var w = 0
        var h = 0
        val dm = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(dm)
        w = dm.widthPixels
        h = dm.heightPixels
        wh[0] = w
        wh[1] = h
        return wh
    }

    val deviceHeight: Int
        // 获取 设备的Hight
        get() = getDeviceHeight(mContext)

    // 获取 设备的Hight
    fun getDeviceHeight(context: Context): Int {
        val dm = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(dm)
        return dm.heightPixels
    }

    fun getDeviceRealHeight(context: Activity): Int {
        val outMetrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getRealMetrics(outMetrics)
        val deviceRealHeightPixels = outMetrics.heightPixels
        return deviceRealHeightPixels
    }

    /**
     * 获取状态栏的高度
     * @param context
     * @return状态栏的高度
     */
    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}
