package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class StorageBean implements Serializable {
    public final long total;
    public final long used;
    public final long free;

    public StorageBean(long total, long used, long free) {
        this.total = total;
        this.used = used;
        this.free = free;
    }

    public String getString() {
        return "total: " + this.total + "\nused: " + this.used + "\nfree: " + this.free;
    }
}
