package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/3
 * @Description:
 */
@Keep
public class CityBean {


    public String msg;
    public Integer code;
    public List<DataBean> data;

    public static class DataBean {
        public String cityCode;
        public String name;
    }
}
