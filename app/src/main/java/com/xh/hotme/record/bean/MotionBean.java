package com.xh.hotme.record.bean;

import java.util.List;

public class MotionBean {
    public final int type;
    public final String name;

    List<PlaceBean> placeList;

    public MotionBean(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public List<PlaceBean> getPlaceList() {
        return this.placeList;
    }
}
