package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class CreateOrderBean implements Serializable {
    public String msg;
    public Integer code;
    public DataBean data;

    public static class DataBean implements Serializable {
        public String tnValue;
        public HmPackageBean hmPackage;
        public PkgPayBean pkgPay;
        public HmOrderBean hmOrder;

        public static class HmPackageBean implements Serializable {
            public String packageId;
            public String packageName;
            public String packageType;
            public Double packagePrice;
            public String videoCount;
            public String liveCount;
            public String personCount;
            public String storeCount;
            public String packageDeclare;
        }


        public static class PkgPayBean implements Serializable {
            public String payId;
            public String deviceId;
            public String packageId;
            public String phoneNumber;
            public String payStatus;
            public String payOrdertype;
            public String payAmount;
            public String nowpayOrderNo;
            public String payChanneltype;
            public String createTime;
        }

        public static class HmOrderBean implements Serializable {
            public String orderNo;
            public String orderName;
            public String amt;
            public String orderDetail;
            public String orderTime;
        }
    }


}
