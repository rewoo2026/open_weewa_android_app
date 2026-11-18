package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;


@Keep
public class GameScoreRemoveBean extends BaseBluetoothBean {

    public final int host_team;   //是否主队

    public GameScoreRemoveBean(int hostTeam) {
        this.c = BleConstants.BLE_CMD_COMPETITION_REMOVE_SCORE;
        this.host_team = hostTeam;
    }
}
