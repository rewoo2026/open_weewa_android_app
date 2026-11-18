package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class UpdateTeamNameRequestBean extends BaseBluetoothBean {
    final String guest_team;
    final String host_team;

    public UpdateTeamNameRequestBean(String hostName, String guestName) {
        this.c = BleConstants.BLE_CMD_SET_TEAM_NAME;
        this.host_team = hostName;
        this.guest_team = guestName;
    }
}
