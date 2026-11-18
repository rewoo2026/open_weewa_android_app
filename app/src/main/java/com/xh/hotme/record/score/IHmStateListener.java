package com.xh.hotme.record.score;

import com.xh.hotme.record.bean.DotStates;

public interface IHmStateListener {

    public void onAddState(int hotTeam, DotStates dotState);
    public void onRemoveState(int hotTeam, DotStates dotStat);
    public void onEditState(int hotTeam, DotStates dotStat);
}
