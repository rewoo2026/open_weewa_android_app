package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.List;

@Keep
public class LiveTaskListBean implements Serializable {
    public Integer code;
    public String msg;
    public List<HmCompetitionLive> data;
}
