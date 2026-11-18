package com.xh.hotme.bluetooth.listener;

import com.xh.hotme.bean.CameraVideoDateListBean;

import java.util.List;

public interface IBleVideoFilterNotifyListener {
    void onVideoFilterList(int type, List<CameraVideoDateListBean> data);

    void onVideoFilterListFail(String msg);

}
