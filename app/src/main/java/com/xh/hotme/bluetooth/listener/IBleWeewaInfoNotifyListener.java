package com.xh.hotme.bluetooth.listener;

import com.xh.hotme.bean.WeewaInfo;

public interface IBleWeewaInfoNotifyListener {
    void onWeewaInfo(WeewaInfo data);

    void onWeewaInfoFail();

}
