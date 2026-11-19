package com.voyah.lib_base.listener

interface OnDialogListener {
    fun onLeftClick() {}
    fun onLeftClick(type:Int) {}
    fun onRightClick() {}
    fun onOneClick() {}
    fun onTwoClick() {}
    fun onInterest(list: List<Int>) {}
}