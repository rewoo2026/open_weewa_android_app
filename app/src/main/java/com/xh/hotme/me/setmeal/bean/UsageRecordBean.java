package com.xh.hotme.me.setmeal.bean;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/25
 * @Description:
 */
public class UsageRecordBean implements Serializable {

    public String msg;
    public Integer code;
    public DataBean data;

    public static class DataBean implements Serializable {
        public List<RowsBean> rows;

        public static class RowsBean implements Serializable {
            public String usedId;
            public String competitionName;
            public Integer usedType;
            public String deviceId;
            public String phoneNumber;
            public String createTime;
            public String competitionContent;
        }
    }
}
