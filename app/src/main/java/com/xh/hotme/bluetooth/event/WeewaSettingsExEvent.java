package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;

import java.util.HashMap;
import java.util.Map;

@Keep
public class WeewaSettingsExEvent {
    public Map<String, String> settings;

    public WeewaSettingsExEvent() {
        settings = new HashMap<>();
    }

    public void add(String key, String value) {
        settings.put(key, value);
    }

    public String get(String key) {
        return settings.containsKey(key) ? settings.get(key) : "";
    }

    public boolean has(String key) {
        return settings.containsKey(key);
    }
}
