package com.xh.hotme.me.setmeal.bean;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/15
 * @Description:
 */
public class SetMealListBean implements Serializable {

    public static final int TYPE_USER = 1;
    public static final int TYPE_TITLE = 2;
    public static final int TYPE_CONTENT_SERVICE = 3;
    public static final int TYPE_CONTENT_CARD = 4;

    public static final int TYPE_STORAGE = 5;

    public static final int TYPE_CONTENT_LIVE_CARD = 6;

    public static final int TYPE_CONTENT_PRO_SERVICE = 7;

    // 修改为列表类型
    public List<SetMealListPackageBean.DataBean> storageBeans;  // 将 storageBean 改为 storageBeans 列表


    public int type;
    public int packageType;
    public String title;
    public String btn;
    public boolean isBaseToPro;

    public PackageListBean.DataBean dataBean;
    public SetMealListPackageBean.DataBean dataBean2;
}
