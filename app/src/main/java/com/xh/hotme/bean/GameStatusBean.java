package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

/*
{"c":"add_goal","guest_goal":0,"host_goal":2,"host_team":1,"r":"ok"}
 */
@Keep
public class GameStatusBean implements Serializable {
    int type;
    int guest_goal;
    int host_goal;

    public GameStatusBean(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getGuest_goal() {
        return guest_goal;
    }

    public void setGuest_goal(int guest_goal) {
        this.guest_goal = guest_goal;
    }

    public int getHost_goal() {
        return host_goal;
    }

    public void setHost_goal(int host_goal) {
        this.host_goal = host_goal;
    }

    public void reset() {
        this.guest_goal = 0;
        this.host_goal = 0;
    }

}
