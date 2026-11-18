package com.xh.hotme.bean;


import androidx.annotation.Keep;

@Keep
public class CompetitionName {
    private long time;
    private String name;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CompetitionName(String name, long time) {
        this.name = name;
        this.time = time;
    }
}
