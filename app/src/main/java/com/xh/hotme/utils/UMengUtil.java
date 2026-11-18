package com.xh.hotme.utils;

import android.content.Context;

import com.umeng.commonsdk.UMConfigure;
import com.umeng.commonsdk.listener.OnGetOaidListener;
import com.umeng.socialize.PlatformConfig;
import com.xh.hotme.HotmeApplication;

public class UMengUtil {
    public static void init(Context context){
        //初始化组件化基础库, 所有友盟业务SDK都必须调用此初始化接口。
        UMConfigure.init(context, Constants.UMENG_APP_ID, "Umeng", UMConfigure.DEVICE_TYPE_PHONE, "");
        // 微信设置
        PlatformConfig.setWeixin(Constants.WEIXIN_APP_ID, Constants.WEIXIN_SECRECT_KEY);
        PlatformConfig.setWXFileProvider("com.xh.hotme.fileprovider");
        UMConfigure.getOaid(context, new OnGetOaidListener() {
            @Override
            public void onGetOaid(String oaid) {
                AppTrace.d("UMConfigure", "oaid = " + oaid);
                HotmeApplication.getInstance().mOAID = oaid;
            }
        });
    }
}
