package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.GoalStatusBean;

@Keep
public class RemoveGoalEvent {
    public final int status;

    public final GoalStatusBean goalStatusBean;

    public RemoveGoalEvent(int s, GoalStatusBean goal) {
        this.status = s;
        this.goalStatusBean = goal;
    }
}
