package com.voyah.lib_base.mvp.bean;

import java.io.Serializable;

public class BaseResponseHeadersBean<T> implements Serializable {
    public int code;
    public String msg;
    public T data;
    public OpenLoginHeadersBean headers;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public OpenLoginHeadersBean getHeaders() {
        return headers;
    }

    public void setHeaders(OpenLoginHeadersBean headers) {
        this.headers = headers;
    }
}
