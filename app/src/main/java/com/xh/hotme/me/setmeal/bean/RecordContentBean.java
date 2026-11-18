package com.xh.hotme.me.setmeal.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/23
 * @Description:
 */
public class RecordContentBean implements Serializable {


    public String title;
    public boolean retract;

    public final List<UsageRecordBean.DataBean.RowsBean> list;

    public RecordContentBean() {
        list = new ArrayList<>();
    }
}
