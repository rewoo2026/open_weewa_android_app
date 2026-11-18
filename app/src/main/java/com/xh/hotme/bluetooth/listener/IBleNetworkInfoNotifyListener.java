package com.xh.hotme.bluetooth.listener;

import com.xh.hotme.bean.NetworkBean;

public interface IBleNetworkInfoNotifyListener {
    void onNetworkStatus(NetworkBean networkBean);
}
