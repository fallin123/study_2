package com.voyah.lib_base.net

interface EventConstant {
    companion object {
        const val LOGOUT_DIALOG: String = "logout_dialog" //弹出重新登录框
        const val ACCOUNT_ERROR: String = "account_error" //账号错误
        const val EXIT_ALL_ACTIVITY: String = "exit_all_activity" //退出所有activity
        const val SIGN_REQUEST: String = "sign_request" //重新请求sign签名
        const val REQUEST_SIGN: String = "Request_sign" //请求完了sign
    }
}
