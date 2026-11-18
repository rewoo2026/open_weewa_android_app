package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.GoalStatusBean;

@Keep
public class GoalEditEvent {
    public final int status;

    public final GoalStatusBean goalStatusBean;

    public GoalEditEvent(int s, GoalStatusBean goal) {
        this.status = s;
        this.goalStatusBean = goal;
    }
}
