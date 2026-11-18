package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

/*
{"c":"add_goal","guest_goal":0,"host_goal":2,"host_team":1,"r":"ok"}
 */
@Keep
public class GoalStatusBean implements Serializable {
    int guest_goal;
    int host_goal;
    int host_team;

    int type;

    String pointId;
    int time;
    int remove;

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

    public int getHost_team() {
        return host_team;
    }

    public void setHost_team(int host_team) {
        this.host_team = host_team;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getPointId() {
        return pointId;
    }

    public void setPointId(String pointId) {
        this.pointId = pointId;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getRemove() {
        return remove;
    }

    public void setRemove(int remove) {
        this.remove = remove;
    }
}
