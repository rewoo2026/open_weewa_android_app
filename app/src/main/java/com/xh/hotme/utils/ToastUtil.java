package com.xh.hotme.utils;

import android.app.Activity;
import android.text.TextUtils;

import androidx.annotation.Keep;

import io.github.muddz.styleabletoast.StyleableToast;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.base.ActivityStackManager;

/**
 * Create by zhaozhihui on 2019/2/26
 **/
@Keep
public class ToastUtil {
    protected static final String TAG = ToastUtil.class.getSimpleName();

    public static void s(String msg) {
        if(TextUtils.isEmpty(msg)) return;

        // 如果错误信息是java的异常这种, 不直接显示, 而是显示一些用户能理解的文字
        if(msg.contains("Exception")) {
            AppTrace.d(TAG, "toast message is exception: " + msg);
            msg = "网络不给力, 请稍后再试";
        }

        Activity act = ActivityStackManager.impl.getCurrentActivity();
        if(act != null && !act.isDestroyed() && !act.isFinishing()) {
            StyleableToast toast = StyleableToast.makeText(act, msg, R.style.HotMeToast);
            AppTrace.d(TAG, msg);
            toast.show();
        }
    }

    public static void s(int resMsg) {
        s(HotmeApplication.getContext().getString(resMsg));
    }

    public static void l(String msg) {
        if(TextUtils.isEmpty(msg)) return;

        // 如果错误信息是java的异常这种, 不直接显示, 而是显示一些用户能理解的文字
        if(msg.indexOf("Exception") != -1) {
            AppTrace.d(TAG, "toast message is exception: " + msg);
            msg = "网络不给力, 请稍后再试";
        }

        StyleableToast toast = StyleableToast.makeText(HotmeApplication.getContext(), msg, R.style.HotMeToast_Long);
        AppTrace.d(TAG, msg);
        toast.show();
    }

    public static void l(int resMsg) {
        l(HotmeApplication.getContext().getString(resMsg));
    }
}
