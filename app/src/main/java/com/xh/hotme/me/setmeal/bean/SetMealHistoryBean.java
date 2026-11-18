package com.xh.hotme.me.setmeal.bean;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/15
 * @Description:
 */

public class SetMealHistoryBean implements Serializable {

    public String msg;
    public Integer code;
    public DataBean data;

    public static class DataBean implements Serializable {
        public Integer total;
        public List<RowsBean> rows;

        public static class RowsBean implements Serializable {
            public String renewId;
            public String packageId;
            public String deviceId;
            public String phoneNumber;
            public String createTime;
            public String endTime;
            public String packageType;
            public String packageName;
            public String packageDeclare;
            public String payId;
            public String payChanneltype;
            public String packagePrice;
        }
    }

}
