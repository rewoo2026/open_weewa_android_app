package com.xh.hotme.me.setmeal.dialog;

import android.content.Context;

import com.xh.hotme.R;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/15
 * @Description:
 */
class SetMealDialogUtils {

    //套餐使用次数耗尽
    public static void showExpiredDialog(Context context) {

        ExpiredDialog expiredDialog = new ExpiredDialog(context);
        expiredDialog.showOnCenter(0);
        expiredDialog.setText("套餐内使用次数已耗尽", "请购买套餐", "立即购买", 1);
    }

    // 年费套餐过期
    public static void showExpiredYearDialog(Context context, String time) {

        ExpiredDialog expiredDialog = new ExpiredDialog(context);
        expiredDialog.showOnCenter(0);
        expiredDialog.setText("年费套餐已过期", time, "立即购买", 1);
    }


    // 基础服务
    public static void showBaseDialog(Context context, String content1, String content2, String content3) {

        ExpiredServiceDialog expiredDialog = new ExpiredServiceDialog(context);
        expiredDialog.showOnCenter(0);
        expiredDialog.setText("基础服务", content1, content2, content3, "升级");
    }

    // PRO服务
    public static void showProDialog(Context context, String content1, String content2, String content3) {

        ExpiredServiceDialog expiredDialog = new ExpiredServiceDialog(context);
        expiredDialog.showOnCenter(0);
        expiredDialog.setText(context.getString(R.string.pro_service), content1, content2, content3, "续费");
    }

    // 热我大礼包
    public static void showRwPackDialog(Context context, String content1, String content2, String content3) {

        ExpiredServiceDialog expiredDialog = new ExpiredServiceDialog(context);
        expiredDialog.showOnCenter(0);
        expiredDialog.setText(context.getString(R.string.rewoo_gift), content1, content2, content3, "立即购买");
    }
}
