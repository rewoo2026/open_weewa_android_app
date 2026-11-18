package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.List;

@Keep
public class UserSetMealBean implements Serializable {
    public Integer code;
    public List<DataBean> data;

    public static class DataBean implements Serializable {
        public String createUserId;
        public String createTime;
        public String updateUserId;
        public String updateTime;
        public String deviceId;
        public String devicePhone;
        public Integer packageType;
        public Integer countType;
        public int totalCount = -1;
        public Integer useCount;
        public Integer remainCount;
        public String expireTime;
        public boolean expired;
    }
}
