package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.GameStatusListBean;
import com.xh.hotme.bluetooth.bean.BaseBluetoothBean;

import java.util.List;

@Keep
public class GameStatListEvent {
    public final int status;

    public final BaseBluetoothBean requestBean;

    public final List<GameStatusListBean> goalList;

    public GameStatListEvent(int s, List<GameStatusListBean> goal, BaseBluetoothBean requestBean) {
        this.status = s;
        this.goalList = goal;
        this.requestBean = requestBean;
    }

}
