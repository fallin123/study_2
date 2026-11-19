package com.voyah.lib_base.bean

import android.graphics.Bitmap
import android.widget.EditText
import android.widget.TextView
import com.voyah.lib_base.listener.OnDialogListener
import java.io.Serializable


class DialogContact : Serializable {
    var title: String? = null
    var one: String? = null
    var two: String? = null
    var content: String = ""
    var leftText: String? = null
    var rightText: String? = null
    var list: List<Int>? = null
    var numlist: List<String>? = null
    var shareList: List<String>? = null
    var orderStauts: String? = null
    var onTouchOutside: Boolean = true
    var showCancelImg: Boolean = true

    @JvmField
    var listener: OnDialogListener? = null

    var ImgTitle: String? = null
    var tv_postTitle: String? = null
    var seriesName: String? = null
    var salesName: String? = null
    var storeName: String? = null
    var iv_usericon: String? = null
    var tv_username: String? = null
    var tv_storeName: String? = null
    var createTime: String? = null
    var img_code_share: String? = null
    var backendImg: String? = null
    var introduction: String? = null
    var versionCode: String? = null
    var isForce: Boolean = false
    var appUpdateTime: String? = null
    var isUpdateModule: Boolean = false
    var max: Int = 0
    var progress: Int = 0
    var id: String? = null
    var contentUrl: String? = null
    var feniangtu: String? = null
    var shareQQTag: String? = null
    var fenxiangpengyouquan: String? = null
    var fenxianglianjie: String? = null
    var cancleText: String? = null
    var qrcode: String? = null
    var medalText: String? = null

    var shareView: Bitmap? = null

    var type: String? = null

    var namePageActivity: String? = null
    var getCodeTv: TextView? = null
    val editCode: EditText? = null

    var hasVideo: Boolean = false
    var displayStaffIcon: Boolean = false
    var imgKoc: Boolean = false
    var imgfreeRight: Boolean = false
    var imgSkc: Boolean = false
    val userLevelName: String? = null
    var imgzhuiguangSmall: Boolean = false
    var imgzhuiguangBig: Boolean = false
    val userTextLabel: String? = null

    //是否显示上拉框的标题
    val isShowPullUpTitle = false

    //是否显示上拉框的取消
    val isShowPullUpCancle = false

    //设置上拉框的标题
    val pullUpTitle: String? = null
    val isTransparent = false
    val isSelectEndDate = false
    val totalRepay = 0.0
}
