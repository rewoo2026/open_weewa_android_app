package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.List;

@Keep
public class BindDeviceResultBean implements Serializable {
    public int total;
    public List<BindDeviceBean> list;
}
