package com.xh.hotme.me.setmeal.bean;

import java.io.Serializable;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/23
 * @Description:
 */
public class RecordTopBean implements Serializable {


    public final String title;
    public boolean isSelect;

    public RecordTopBean(String title, boolean isSelect) {
        this.title = title;
        this.isSelect = isSelect;
    }
}
