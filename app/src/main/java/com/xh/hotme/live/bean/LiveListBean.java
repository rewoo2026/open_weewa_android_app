package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2023/12/27
 * @Description:
 */
@Keep
public class LiveListBean implements Serializable {
    public String code;
    public String msg;
    public List<DataBean> data;


    public static class DataBean implements Serializable {
        public String createUserId;
        public String createTime;
        public String updateUserId;
        public String updateTime;
        public String deviceId;
        public String domainId;
        public String platformType;
        public String platformAccount;
        public String platformKey;
        public String pullAddress;
        public String pushAddress;
        public String pushStream;
    }
}
