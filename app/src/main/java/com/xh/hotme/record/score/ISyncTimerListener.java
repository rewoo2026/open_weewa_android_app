package com.xh.hotme.record.score;

import com.xh.hotme.bean.GameStatusListBean;

import java.util.ArrayList;

public interface ISyncTimerListener {
    public int getTime();

    public boolean isDotEnable();
    public ArrayList<GameStatusListBean> getStateList();
}
