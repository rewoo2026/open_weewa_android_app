package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class DeviceUsageInfo implements Serializable {
    public int temperature;
    public int energy;
    public int charging;

    public StorageBean storage;

    public String toString() {
        return "energy=" + this.energy + "\ntemperature=" + temperature + "\n" +
            "storage= {total=" + storage.total + ", use=" + storage.used + ", free=" + storage.free + " }"
            ;
    }
    public String toFormatString() {
        String totalStr = String.format("%dG", (int) (storage.total / 1024 / 1024 / 1024));
        String usedStr = "";
        if(storage.used / 1024 / 1024 > 1024) {
            if(storage.used * 1.0 / 1024 / 1024 / 1024 > 10) {
                usedStr = String.format("%dG", (int) (storage.used * 1.0 / 1024 / 1024 / 1024));
            } else {
                usedStr = String.format("%.1fG", storage.used * 1.0 / 1024 / 1024 / 1024);
            }
        } else {
            usedStr = String.format("%.2fM", storage.used * 1.0 / 1024 / 1024);
        }
        String charging = this.charging ==1 ? "充电中" : "";

        return "电量=" + this.energy + "%, " + charging + "\n温度=" + temperature + "°C\n" +
                "存储空间= {总共=" + totalStr + ", 已使用=" + usedStr + " }"
                ;
    }
}
