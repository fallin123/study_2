package com.voyah.lib_base.mvp.base

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.voyah.lib_base.R
import com.voyah.lib_base.mvp.utils.ActivityHelper
import com.voyah.lib_base.mvp.utils.LogUtil
import com.voyah.lib_base.mvp.utils.StatusBarUtil
import com.voyah.lib_base.mvp.widget.LoadingDataDialog
import com.voyah.lib_base.mvp.widget.WatermarkUtils

abstract class BaseActivity : AppCompatActivity() {
    private var loadingDialog: LoadingDataDialog? = null
    private var loadingNoBackgroundDialog: LoadingDataDialog? = null
    private var startTime: Long = 0
    protected var totalTime: Long = 0
    private val TAG = "岚图汽车"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtil.d(TAG, "------------------${javaClass.simpleName}----------------onCreate")
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(getLayoutId())
        initView(savedInstanceState)

        if (Transparent()) {
            StatusBarUtil.transparencyBar(this)
        }
        if (statusBarFontColor()) {
            StatusBarUtil.setLightStatusBar(this, true, true)
        } else {
            StatusBarUtil.setLightStatusBar(this, false, true)
        }

        ActivityHelper.addActivity(this)

    }

    /**
     * 设置布局
     *
     * @return
     */
    abstract fun getLayoutId(): Int

    /**
     * 初始化视图
     */
    abstract fun initView(savedInstanceState: Bundle?)

    /**
     * 设置是否透明状态栏
     */
    protected open fun Transparent(): Boolean {
        return false
    }

    protected open fun isShowAiReBot(): Boolean {
        return true
    }

    /**
     * 设置状态栏字体颜色
     */
    protected open fun statusBarFontColor(): Boolean {
        return true
    }

    override fun onStart() {
        super.onStart()
        LogUtil.d(TAG, "------------------${javaClass.simpleName}----------------onStart")
    }

    override fun onResume() {
        super.onResume()
        LogUtil.d(TAG, "------------------${javaClass.simpleName}----------------onResume")
        startTime = System.currentTimeMillis() //获取页面浏览开始时间
    }

    override fun onPause() {
        super.onPause()
        LogUtil.d(TAG, "------------------${javaClass.simpleName}----------------onPause")
        //暂停计时
        val stayTime = System.currentTimeMillis() - startTime
        totalTime += stayTime
    }

    override fun onStop() {
        super.onStop()
        LogUtil.d(TAG, "------------------${javaClass.simpleName}----------------onStop")
    }

    override fun onDestroy() {
        ActivityHelper.removeActivity(this)
        super.onDestroy()
        LogUtil.d(TAG, "------------------${javaClass.simpleName}----------------onDestroy")
        WatermarkUtils.removeWatermarkFromActivity(this)
    }

    /**
     * 悬浮在内容正中间上方，不隐藏内存区域
     */
    fun showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = LoadingDataDialog(this)
        }
        if (!loadingDialog!!.isShowing) {
            loadingDialog!!.setContentText("")//R.string.pull_to_refresh_footer_refreshing_label
            loadingDialog!!.show()
        }
    }

    fun showLoadingNoBackgroundDialog() {
        if (loadingNoBackgroundDialog == null) {
            loadingNoBackgroundDialog = LoadingDataDialog(this, R.style.dialog_style2)
        }
        if (!loadingNoBackgroundDialog!!.isShowing) {
            loadingNoBackgroundDialog!!.setContentText("")//R.string.pull_to_refresh_footer_refreshing_label
            loadingNoBackgroundDialog!!.show()
        }
    }

    /**
     * 悬浮在内容正中间上方，不隐藏内存区域，设置msg
     *
     * @param msg
     */
    protected fun showLoadingDialog(msg: String) {
        if (loadingDialog == null) {
            loadingDialog = LoadingDataDialog(this)
        }

        if (!loadingDialog!!.isShowing) {
            loadingDialog?.setContentText(msg)
            loadingDialog?.show()
        }
    }

    /**
     * 悬浮在内容正中间上方，不隐藏内存区域，设置msg
     * 设置点击dialog外部可否取消dialog加载
     * 两个参数都可以传null
     *
     * @param msg
     */
    protected fun showLoadingDialog(@Nullable msg: String?, @Nullable enableOutsid: Boolean?) {
        if (loadingDialog == null) {
            loadingDialog = LoadingDataDialog(this)
        }

        if (loadingDialog?.isShowing == false) {
            if (null == msg) {
                loadingDialog?.setContentText("正在加载中\\u2026")
            } else {
                loadingDialog?.setContentText(msg)
            }
            if (null == enableOutsid) {
                loadingDialog?.setCanceledOnTouchOutside(false)
            } else {
                loadingDialog?.setCanceledOnTouchOutside(enableOutsid)
            }
            loadingDialog?.setCancelable(false)
            loadingDialog?.show()
        }
    }

    fun dismissLoaddingDialog() {
        if (loadingDialog != null && loadingDialog!!.isShowing) {
            loadingDialog?.dismiss()
        }
    }

    fun dismissLoaddingNoBackgroundDialog() {
        if (loadingNoBackgroundDialog != null && loadingNoBackgroundDialog!!.isShowing) {
            loadingNoBackgroundDialog?.dismiss()
        }
    }
}