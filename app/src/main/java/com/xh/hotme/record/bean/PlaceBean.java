package com.xh.hotme.record.bean;

public class PlaceBean {
    public final int type;
    public final String name;

    public PlaceBean(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
