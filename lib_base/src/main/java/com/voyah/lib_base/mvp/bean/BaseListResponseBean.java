package com.voyah.lib_base.mvp.bean;



import java.io.Serializable;

public class BaseListResponseBean<T> implements Serializable {
    public int code;
    public String message;
    public T dataList;
    public int totalCount;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

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

    public T getDataList() {
        return dataList;
    }

    public void setDataList(T dataList) {
        this.dataList = dataList;
    }
}
