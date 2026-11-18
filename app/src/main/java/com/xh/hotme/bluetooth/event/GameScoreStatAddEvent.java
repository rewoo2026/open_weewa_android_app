package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.GoalStatusBean;

@Keep
public class GameScoreStatAddEvent {
    public final int status;

    public final GoalStatusBean goalStatusBean;

    public GameScoreStatAddEvent(int s, GoalStatusBean goal) {
        this.status = s;
        this.goalStatusBean = goal;
    }
}
