// public class SetMealHistoryPackageBean {
    
// }
package com.xh.hotme.me.setmeal.bean;

import androidx.annotation.Keep;

import java.io.Serializable;
@Keep
public class SetMealHistoryPackageBean implements Serializable {
    public DataBean[] data;

    public static class DataBean implements Serializable {
        public String createTime;          // 购买时间
        public Integer orderPoint;        // 购买来源
        public PackageSnapshotBean packageSnapshot;

        public static class PackageSnapshotBean implements Serializable {
            public String packageCategory;  // 套餐类型
            public String packageName;      // 套餐名称
            public double packagePrice;     // 套餐价格
        }
    }
}