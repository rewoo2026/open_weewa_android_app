package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.GoalStatusBean;

@Keep
public class GameScoreAddEvent {
    public final int status;

    public final GoalStatusBean goalStatusBean;

    public GameScoreAddEvent(int s, GoalStatusBean goal) {
        this.status = s;
        this.goalStatusBean = goal;
    }
}
