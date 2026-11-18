package com.xh.hotme.bluetooth.bean;

import android.text.TextUtils;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;


@Keep
public class GameStatAddBean extends BaseBluetoothBean {

    public final int host_team;   //是否主队
    public final int type;  // 打点类型
    public final String n;  // 打点类型
    public final String id;  // 服务器返回的打点id
    public String mark_name;  // 比赛阶段名称
    public String mark_id;  // 比赛阶段id，服务器生成

    public GameStatAddBean(int hostTeam, int type, String name) {
        this.c = BleConstants.BLE_CMD_COMPETITION_ADD_STAT;
        this.type = type;
        this.host_team = hostTeam;
        this.n = name;
        this.id = "";
        this.mark_name = "";
        this.mark_id = "";
    }
    public GameStatAddBean(int hostTeam, int type, String name, String id) {
        this.c = BleConstants.BLE_CMD_COMPETITION_ADD_STAT;
        this.type = type;
        this.host_team = hostTeam;
        this.n = name;
        this.id = id;
        this.mark_name = "";
        this.mark_id = "";
    }
    public GameStatAddBean(int hostTeam, int type, String name, String id, String markName, String markId) {
        this.c = BleConstants.BLE_CMD_COMPETITION_ADD_STAT;
        this.type = type;
        this.host_team = hostTeam;
        this.n = name;
        this.id = id;
        this.mark_name = TextUtils.isEmpty(markName) ? "": markName;
        this.mark_id = TextUtils.isEmpty(markId) ? "" : markId;;


    }
}
