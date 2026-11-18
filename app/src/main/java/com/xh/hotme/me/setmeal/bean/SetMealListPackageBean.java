package com.xh.hotme.me.setmeal.bean;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.List;

@Keep
public class SetMealListPackageBean implements Serializable {
    public List<DataBean> data;

    public static class DataBean implements Serializable {
        public String createTime;
        public String updateTime;
        public String createUserId;
        public String updateUserId;
        public String packageId;
        public String deviceType;
        public String packageName;
        public String packageCategory;
        public String packageType;
        public Integer packagePeriod;
        public double packagePrice;
        public Integer recordCount;
        public Integer liveCount;
        public Integer storeCount;
        public Integer bigDataCount;
        public String packageDesc;
        public Integer usageSort;
        public Integer enabled;
        public Integer upgradable;
    }
}
