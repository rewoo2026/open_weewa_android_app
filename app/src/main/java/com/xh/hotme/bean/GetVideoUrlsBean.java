package com.xh.hotme.bean;


import androidx.annotation.Keep;

import com.xh.hotme.live.bean.UserSetMealBean;

import java.io.Serializable;
import java.util.List;

@Keep
public class GetVideoUrlsBean implements Serializable {
    public Integer code;
    public List<String> data;
}
