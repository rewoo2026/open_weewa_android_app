package com.xh.hotme.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Keep;

/**
 * Create by zhaozhihui on 2018/10/13
 **/
@Keep
public class MainHandler extends Handler {
    private static final class HolderClass {
        public static final MainHandler INSTANCE = new MainHandler();
    }

    public static MainHandler getImpl() {
        return HolderClass.INSTANCE;
    }

    private MainHandler() {
        super(Looper.getMainLooper());
    }

    public static void runOnUiThread(Runnable r) {
        runOnUiThread(r, 0);
    }

    public static void runOnUiThread(Runnable r, int delayMillis) {
        MainHandler.getImpl().postDelayed(r, delayMillis);
    }

    public static void removeUITask(Runnable r) {
        MainHandler.getImpl().removeCallbacks(r);
    }
}