package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

/*
{"c":"add_goal","guest_goal":0,"host_goal":2,"host_team":1,"r":"ok"}
 */
@Keep
public class GoalBean implements Serializable {
    int time;
    int host;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int isHost() {
        return host;
    }

    public void setHost(int host) {
        this.host = host;
    }
}
