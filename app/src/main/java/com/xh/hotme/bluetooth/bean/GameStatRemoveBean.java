package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;


@Keep
public class GameStatRemoveBean extends BaseBluetoothBean {
    public final int host_team;   //是否主队
    public final int type;  // 打点类型

    public GameStatRemoveBean(int hostTeam, int type) {
        this.c = BleConstants.BLE_CMD_COMPETITION_REMOVE_STAT;
        this.type = type;
        this.host_team = hostTeam;
    }
}
