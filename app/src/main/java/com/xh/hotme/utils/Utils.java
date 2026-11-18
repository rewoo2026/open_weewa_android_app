package com.xh.hotme.utils;

import android.content.Context;
import android.content.Intent;

import com.xh.hotme.HotmeApplication;

import java.util.Random;

public class Utils {
    public static String generateMixedString(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            // 生成随机数，当生成的数字为0时，表示生成字母,生成数字为1时，表示生成数字；
            int type = random.nextInt(2);
            if (type == 0) {
                // 生成随机字母（小写）
                char letter = (char) (random.nextInt(26) + 'a');
                sb.append(letter);
            } else {
                // 生成随机数字
                char digit = (char) (random.nextInt(10) + '0');
                sb.append(digit);
            }
        }

        return sb.toString().toUpperCase();
    }

    public static void lunchBaiDuNetDisk(Context context){
        Intent intent = HotmeApplication.getContext().getPackageManager().getLaunchIntentForPackage("com.baidu.netdisk");
        if(intent!=null){
            context.startActivity(intent);
        }else{
            ToastUtil.s("无法启动百度网盘");
        }
    }
}
