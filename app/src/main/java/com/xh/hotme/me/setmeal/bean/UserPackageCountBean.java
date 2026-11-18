package com.xh.hotme.me.setmeal.bean;

import androidx.annotation.Keep;

import java.util.List;
import java.io.Serializable;
//public class UserPackageCountBean {
//    public String number;          // 对应 tv_bottom_number
//    public String itemName;        // 对应 special_tv_name1
//    public String expiredTime;     // 对应 special_expired_time1
//    public String count;           // 对应 special_tv_count1
//    public boolean showBuyButton;  // 控制 special_view_item1_buy 的显示状态
//
//    public UserPackageCountBean(String number, String itemName, String expiredTime, String count, boolean showBuyButton) {
//        this.number = number;
//        this.itemName = itemName;
//        this.expiredTime = expiredTime;
//        this.count = count;
//        this.showBuyButton = showBuyButton;
//    }
//}
@Keep
public class UserPackageCountBean implements Serializable {
    public Integer code;
    public List<UserPackageData> data;

    public static class UserPackageData implements Serializable {
        public String createTime;
        public String updateTime;
        public String createUserId;
        public String updateUserId;
        public String domainId;
        public String deviceId;
        public String packageType;
        public String countName;
        public int countType;
        public String countUnit;
        public String usageSort;
        public double totalCount;
        public double useCount;
        public double remainCount;
        public String expireTime;
        public boolean expired;
        public String lastUseTime;
        public String lastOrderId;
        public Integer upgradable;
        public String buttonLabel;
        public List<UserPackageData> children;
    }
}