package com.xh.hotme.live.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/3/21
 * @Description:
 */
@Keep
public class CheckSetMealBean implements Serializable {
    public Integer code;

    public DataDTO data;

    public String msg;

    public static class DataDTO implements Serializable {

        public Integer bigDataCount;

        public String code;
        public String expireTime;
        public boolean expired;

        public Integer liveCount;
        public Integer storeCount;
        public Integer videoCount;

        public Integer videoTotalCount;
    }
}
