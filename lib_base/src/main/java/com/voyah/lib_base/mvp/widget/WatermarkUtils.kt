package com.voyah.lib_base.mvp.widget

import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup
import com.voyah.lib_base.mvp.base.BaseActivity

// WatermarkUtils.kt
object  WatermarkUtils {

    fun addWatermarkToActivity(activity: BaseActivity, watermarkText: String) {
        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val watermarkView = WatermarkView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            updateWatermark(
                text = watermarkText,
                color = Color.parseColor("#15000000"),
                textSize = 38f,
                angle = -30f,
                hSpacing = 200f,
                vSpacing = 200f
            )
        }
        rootView.addView(watermarkView)
    }

    fun removeWatermarkFromActivity(activity: Activity) {
        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        for (i in 0 until rootView.childCount) {
            val view = rootView.getChildAt(i)
            if (view is WatermarkView) {
                rootView.removeView(view)
                return
            }
        }
    }

    // 使用示例（在 BaseActivity 中）：
//    open class BaseActivity : AppCompatActivity() {
//        override fun onCreate(savedInstanceState: Bundle?) {
//            super.onCreate(savedInstanceState)
//            WatermarkUtils.addWatermarkToActivity(this, "Confidential")
//        }
//
//        override fun onDestroy() {
//            WatermarkUtils.removeWatermarkFromActivity(this)
//            super.onDestroy()
//        }
//    }
}
