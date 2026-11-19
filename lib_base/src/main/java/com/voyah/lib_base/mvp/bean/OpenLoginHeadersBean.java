package com.voyah.lib_base.mvp.bean;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class OpenLoginHeadersBean {
    private List<String> IOV_ACCOUNT_SESSIONID;
    @SerializedName("Set-Cookie")
    private List<String> SetCookie;
    private List<String> usersig;
    private List<String> userid;

    public List<String> getSetCookie() {
        return SetCookie;
    }

    public void setSetCookie(List<String> setCookie) {
        SetCookie = setCookie;
    }

    public List<String> getUsersig() {
        return usersig;
    }

    public void setUsersig(List<String> usersig) {
        this.usersig = usersig;
    }

    public List<String> getUserid() {
        return userid;
    }

    public void setUserid(List<String> userid) {
        this.userid = userid;
    }

    public List<String> getIOV_ACCOUNT_SESSIONID() {
        return IOV_ACCOUNT_SESSIONID;
    }

    public void setIOV_ACCOUNT_SESSIONID(List<String> IOV_ACCOUNT_SESSIONID) {
        this.IOV_ACCOUNT_SESSIONID = IOV_ACCOUNT_SESSIONID;
    }
}
