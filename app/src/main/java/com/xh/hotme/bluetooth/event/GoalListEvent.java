package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import com.xh.hotme.bean.GoalBean;

import java.util.List;

@Keep
public class GoalListEvent {
    public final int status;

    public final List<GoalBean> goalList;

    public GoalListEvent(int s, List<GoalBean> goal) {
        this.status = s;
        this.goalList = goal;
    }
}
