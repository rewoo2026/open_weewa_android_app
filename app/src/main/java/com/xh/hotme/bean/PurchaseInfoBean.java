package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class PurchaseInfoBean implements Serializable {
    public String name;
    public String start_time;
    public String end_time;
    public String description;
    public int service_type;
    public String purchase_type;

    public String toString() {
        return "name=" + name
            + ", description=" + description
            + ", start_time=" + start_time
            + ", end_time=" + end_time
            + ", service_type=" + service_type
            + ", purchase_type=" + purchase_type

            ;
    }
}
