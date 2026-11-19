package com.voyah.lib_base.mvp.base

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import com.voyah.lib_base.R
import com.voyah.lib_base.mvp.widget.LoadingDataDialog

abstract class BaseFragment : Fragment() {
    private var loadingDialog: LoadingDataDialog? = null
    private var loadingNoBackgroundDialog: LoadingDataDialog? = null

    protected var parentActivity: Activity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        parentActivity = activity
        val view = inflater.inflate(getLayoutId(), null, false)
        return view
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView(savedInstanceState)
    }

    /**
     * 初始化视图
     *
     * @param view
     */
    protected abstract fun initView(savedInstanceState: Bundle?)
    protected abstract fun getLayoutId(): Int
    /**
     * 悬浮在内容正中间上方，不隐藏内存区域
     */
    protected fun showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = LoadingDataDialog(activity)
        }

        if (!loadingDialog!!.isShowing) {
            loadingDialog!!.setContentText("")//R.string.pull_to_refresh_footer_refreshing_label
            loadingDialog!!.show()
        }
    }

    protected fun showLoadingNoBackgroundDialog() {
        if (loadingNoBackgroundDialog == null) {
            loadingNoBackgroundDialog = LoadingDataDialog(activity, R.style.dialog_style2)
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
            loadingDialog = LoadingDataDialog(activity)
        }

        if (!loadingDialog!!.isShowing) {
            loadingDialog!!.setContentText(msg)
            loadingDialog!!.show()
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
            loadingDialog = LoadingDataDialog(parentActivity)
        }

        if (!loadingDialog!!.isShowing) {
            if (null == msg) {
                loadingDialog!!.setContentText("正在加载中\\u2026")
            } else {
                loadingDialog!!.setContentText(msg)
            }
            if (null == enableOutsid) {
                loadingDialog!!.setCanceledOnTouchOutside(false)
            } else {
                loadingDialog!!.setCanceledOnTouchOutside(enableOutsid)
            }
            loadingDialog!!.setCancelable(false)
            loadingDialog!!.show()
        }
    }

    protected fun dismissLoaddingDialog() {
        if (loadingDialog != null && loadingDialog!!.isShowing) {
            loadingDialog!!.dismiss()
        }
    }
    protected fun dismissLoaddingNoBackgroundDialog() {
        if (loadingNoBackgroundDialog != null && loadingNoBackgroundDialog!!.isShowing) {
            loadingNoBackgroundDialog!!.dismiss()
        }
    }

}