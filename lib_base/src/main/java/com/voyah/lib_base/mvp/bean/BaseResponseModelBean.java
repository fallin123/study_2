package com.voyah.lib_base.mvp.bean;

import java.io.Serializable;

public class BaseResponseModelBean<T> implements Serializable {
    public int code;
    public String message;
    public T model;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getModel() {
        return model;
    }

    public void setModel(T model) {
        this.model = model;
    }
}
