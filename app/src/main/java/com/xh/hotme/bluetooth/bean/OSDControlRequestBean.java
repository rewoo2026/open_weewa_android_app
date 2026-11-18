package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class OSDControlRequestBean extends BaseBluetoothBean {
    int hide_score = -1;
    int hide_team = -1;

    public OSDControlRequestBean() {
        this.c = BleConstants.BLE_CMD_OSD_CONTROL;
    }

    public int getHide_score() {
        return hide_score;
    }

    public void setHide_score(int hide_score) {
        this.hide_score = hide_score;
    }

    public int getHide_team() {
        return hide_team;
    }

    public void setHide_team(int hide_team) {
        this.hide_team = hide_team;
    }
}
