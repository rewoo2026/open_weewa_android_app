package com.xh.hotme.me.setmeal.bean;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/15
 * @Description:
 */

public class PackageListBean implements Serializable {


    public String msg;
    public Integer code;
    public List<DataBean> data;

    public static class DataBean implements Serializable {
        public String packageId;
        public String packageName;
        public String packageType;
        public String packagePrice;
        public String videoCount;
        public String liveCount;
        public String personCount;
        public String storeCount;
        public String bigDataCount;
        public String packageDeclare;
    }
}
