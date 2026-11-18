package com.xh.hotme.listener;


import androidx.annotation.Keep;

/**
 * 增加一个点击回调，帮助确定是从直播还是录制页面进入
 */
@Keep
public interface IDeviceConnectRequestListener {
    void onRequestStart();
}
