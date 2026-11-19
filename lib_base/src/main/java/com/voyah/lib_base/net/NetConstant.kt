package com.voyah.lib_base.net
import com.voyah.lib_base.mvp.utils.DBUtil.AES_PRIVATE
import com.voyah.lib_base.mvp.utils.DBUtil.RES_PUBLIC

class NetConstant {
    var type = "生产"//测试，预生产，生产

    fun surroundings() {
        when (type) {
            "测试" -> {
                isDebug=true
                VERSIONTYPE = "测试"
                ASSISTANT_URL = "https://fassistant.idsmp.voyah.com.cn"
                BUGLY_APPID = "4f270cf62e"  //该测试id，由项目组提供
                AES_PRIVATE = "" //接口入参加密所需
                RES_PUBLIC = ""
                LOGIN_CODE_SUBMIT = "https://m.idsmp.voyah.com.cn/app/tcaptcha.html"   //发送验证码时的防水墙

            }

            "预生产" -> {
                isDebug=true
                VERSIONTYPE = "测试"
                ASSISTANT_URL = "https://fassistant.predsmp.voyah.com.cn"
                BUGLY_APPID = "c6ef9a6718"  //该测试id，由项目组提供
                AES_PRIVATE = "" //接口入参加密所需
                RES_PUBLIC = ""
                LOGIN_CODE_SUBMIT = "https://m.idsmp.voyah.com.cn/app/tcaptcha.html"   //发送验证码时的防水墙
            }

            "生产" -> {
                isDebug=false
                VERSIONTYPE = "生产"
                ASSISTANT_URL = "https://fassistant.dsmp.voyah.com.cn"
                BUGLY_APPID = ""  //该生产id，由项目组提供
                AES_PRIVATE = "" //接口入参加密所需
                RES_PUBLIC = ""
                LOGIN_CODE_SUBMIT = "https://m.voyah.com.cn/app/tcaptcha.html" //发送验证码时的防水墙
            }
        }
    }

    companion object URL {

        //超级助手开发环境
        @JvmStatic
        var ASSISTANT_URL = ""

        //开发环境
        @JvmStatic
        var VERSIONTYPE = ""

        //防水墙验证
        @JvmStatic
        var LOGIN_CODE_SUBMIT = ""


        //异常上报
        @JvmStatic
        var BUGLY_APPID = ""

        @JvmStatic
        var isDebug=false

    }
}
