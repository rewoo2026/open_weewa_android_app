package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;


@Keep
public class GameStatEditBean extends BaseBluetoothBean {
    public int time;  //打点时间
    public int seq;   //第几个
    public int host_team;   //是否主队
    public int type;  // 打点类型
    public int remove;  // 是否删除
    public String mark_name;  // 标记（比赛阶段）名称
    public String mark_id;  // 标记（比赛阶段）id

    public GameStatEditBean() {
        this.c = BleConstants.BLE_CMD_COMPETITION_EDIT_STAT;
        this.mark_name = "";
        this.mark_id = "";
    }
}
