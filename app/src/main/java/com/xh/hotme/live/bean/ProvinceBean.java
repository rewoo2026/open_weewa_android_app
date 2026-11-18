package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import java.util.List;

@Keep
public class ProvinceBean {

    public String msg;
    public Integer code;
    public List<DataBean> data;

    public static class DataBean {
        public String provinceCode;
        public String name;
    }
}
