package com.voyah.lib_base.utils

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.voyah.lib_base.R
import com.voyah.lib_base.bean.DialogContact
import com.voyah.lib_base.mvp.utils.DBUtil

object DialogUtil {
    // 自定义 dialog
    fun getDialogOnMiddle(
        context: Context,
        contentView: View,
        d: Dialog?,
        onTouchOutside: Boolean
    ): Dialog {
        // 防止dialog重复弹出

        if (d != null && d.isShowing) {
            return d
        }
        val dialog = Dialog(context, R.style.myDialogIsScale)
        val window = dialog.window
        val windowParams = WindowManager.LayoutParams()
        val wh = DisplayUtil.getDeviceWH(context)
        windowParams.x = wh[0]
        windowParams.y = 0
        // 控制dialog停放位置
        window!!.attributes = windowParams
        dialog.setContentView(contentView)
        dialog.setCanceledOnTouchOutside(onTouchOutside)
        // 最终决定dialog的大小,实际由contentView确定了
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        try {
            dialog.show()
        } catch (e: Exception) {
        }
        return dialog
    }


    //取消版本升级等待框
    fun dialogCancleLoading(context: Context, dialogContact: DialogContact): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_cancle_dowlond, null)
        val cancle_tv = view.findViewById<TextView>(R.id.cancle_tv)
        val confirm_tv = view.findViewById<TextView>(R.id.confirm_tv)
        cancle_tv.setOnClickListener { view1: View? ->
            if (dialogContact.listener != null) {
                dialogContact.listener!!.onRightClick()
            }
        }
        confirm_tv.setOnClickListener { view12: View? ->
            if (dialogContact.listener != null) {
                dialogContact.listener!!.onLeftClick()
            }
        }
        return getDialogOnMiddle(context, view, null, dialogContact.onTouchOutside)
    }


    fun dialogRenew(context: Context?, dialogContact: DialogContact, runnable: Runnable?): Dialog {
        val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_renew, null)
        val tv_content = view.findViewById<TextView>(R.id.tv_content)
        val tv_title = view.findViewById<TextView>(R.id.newversion_tv)
        val tv_update = view.findViewById<TextView>(R.id.tv_update)
        val tv_update_cancle = view.findViewById<TextView>(R.id.tv_update_cancle)
        val txt_publish_time = view.findViewById<TextView>(R.id.txt_publish_time)
        val tv_view = view.findViewById<View>(R.id.tv_view)
        val img_cancel = view.findViewById<ImageView>(R.id.img_cancel)

        //？
        if (dialogContact.isUpdateModule) {
            tv_update_cancle.text = "暂不更新"
        }
        //是否是强制更新，如果是则不显示不再提醒我的按钮
        if (dialogContact.isForce) {
            tv_update_cancle.visibility = View.GONE
            tv_view.visibility = View.GONE
        } else {
            tv_update_cancle.visibility = View.VISIBLE
            tv_view.visibility = View.VISIBLE
            //？
            if (!dialogContact.showCancelImg) {
                tv_update_cancle.visibility = View.GONE
                tv_view.visibility = View.GONE
            }
        }
        //新版本版本号
        tv_title.text = "版本更新V" + dialogContact.versionCode
        txt_publish_time.text =
            "发布时间：" + DateUtils.hisstampTopicTimeActDown(dialogContact.appUpdateTime)
        //更新内容
        tv_content.text = dialogContact.content
        //监听view的加载或变化。不然获取到的行数是0
        val observer = tv_content.viewTreeObserver // textAbstract为TextView控件
        observer.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val obs = tv_content.viewTreeObserver
                obs.removeGlobalOnLayoutListener(this)
                //获取输入框行数，如果不足三行则居中显示，超过则左对齐
//                int lineCount = tv_content.getLineCount();
//                if (lineCount <= 2) {
//                    tv_content.setGravity(Gravity.CENTER);
//                } else {
                tv_content.gravity = Gravity.LEFT
                //                }
            }
        })
        //不再提醒的点击事件
        tv_update_cancle.setOnClickListener { view1: View? ->
            if (dialogContact.listener != null) {
                dialogContact.listener!!.onRightClick()
                if (!dialogContact.isForce) {
                    if (!dialogContact.isUpdateModule) {
                        DBUtil.setUpdateVersionTag(false)
                        DBUtil.SHOW_MAIN_UPGRADE = false
                        DBUtil.setUpdateVersionName(dialogContact.versionCode)
                    } else {
                        runnable?.run()
                    }
                }
            }
        }
        //立即更新的点击事件
        tv_update.setOnClickListener { v: View? ->
            if (dialogContact.listener != null) {
                dialogContact.listener!!.onLeftClick()
            }
        }
        //关闭弹窗的点击事件
        img_cancel.setOnClickListener { v: View? ->
            if (dialogContact.listener != null) {
                dialogContact.listener!!.onRightClick()
                if (dialogContact.isUpdateModule && runnable != null) {
                    runnable.run()
                } else {
                    DBUtil.SHOW_MAIN_UPGRADE = false
                }
            }
        }
        return getDialogOnMiddle(context!!, view, null, dialogContact.onTouchOutside)
    }

    fun getDialogOnMiddleBottomDownToUp2(
        context: Context?,
        contentView: View?,
        d: Dialog?,
        onTouchOutside: Boolean,
        isTransparent: Boolean
    ): Dialog {
        // 防止dialog重复弹出

        if (d != null && d.isShowing) {
            return d
        }
        val dialog = Dialog(context!!, R.style.myDialogIsScale4)
        val window = dialog.window
        val windowParams = WindowManager.LayoutParams()
        val wh = DisplayUtil.getDeviceWH(context!!)
        windowParams.x = wh[0]
        windowParams.y = 0
        // 控制dialog停放位置
        window?.setGravity(Gravity.BOTTOM)
        //        window.setAttributes(windowParams);
        // 设置动画
        window?.setWindowAnimations(R.style.dialog_menu_animStyle)
        dialog.setContentView(contentView!!)
        dialog.setCanceledOnTouchOutside(onTouchOutside)
        // 最终决定dialog的大小,实际由contentView确定了
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        //        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (isTransparent) {
            val lp = dialog.window!!.attributes
            lp.dimAmount = 0f
            dialog.window!!.attributes = lp
        }
        try {
            dialog.show()
        } catch (e: java.lang.Exception) {
        }
        return dialog
    }

    fun getDialogOnMiddleCenter(
        context: Context?,
        contentView: View?,
        d: Dialog?,
        onTouchOutside: Boolean,
        isTransparent: Boolean
    ): Dialog {
        // 防止dialog重复弹出

        if (d != null && d.isShowing) {
            return d
        }
        val dialog = Dialog(context!!, R.style.myDialogIsScale4)
        val window = dialog.window
        val windowParams = WindowManager.LayoutParams()
        val wh = DisplayUtil.getDeviceWH(context!!)
        windowParams.x = wh[0]
        windowParams.y = 0
        // 控制dialog停放位置
        window?.setGravity(Gravity.CENTER)
        //        window.setAttributes(windowParams);
        // 设置动画
        window?.setWindowAnimations(R.style.dialog_menu_animStyle)
        dialog.setContentView(contentView!!)
        dialog.setCanceledOnTouchOutside(onTouchOutside)
        // 最终决定dialog的大小,实际由contentView确定了
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        //        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (isTransparent) {
            val lp = dialog.window!!.attributes
            lp.dimAmount = 0f
            dialog.window!!.attributes = lp
        }
        try {
            dialog.show()
        } catch (e: java.lang.Exception) {
        }
        return dialog
    }
}
