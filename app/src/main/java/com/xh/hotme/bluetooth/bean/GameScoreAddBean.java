package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;


@Keep
public class GameScoreAddBean extends BaseBluetoothBean {

    public final int host_team;   //是否主队

    public GameScoreAddBean(int hostTeam) {
        this.c = BleConstants.BLE_CMD_COMPETITION_ADD_SCORE;
        this.host_team = hostTeam;
    }
}
