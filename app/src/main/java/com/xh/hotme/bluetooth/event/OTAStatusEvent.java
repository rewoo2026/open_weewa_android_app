package com.xh.hotme.bluetooth.event;

import androidx.annotation.Keep;
import com.alibaba.fastjson.JSONObject;

@Keep
public class OTAStatusEvent {
    public int status = -1;
    public int progress;
    public int versionCode;
    public String versionName;
    public int remoteVersionCode;
    public String remoteVersionName;
    public boolean forceUpgrade;

    public static OTAStatusEvent fromJson(JSONObject json) {
        OTAStatusEvent event = new OTAStatusEvent();
        if (json == null) return event;
        event.status = integer(json, "st", -1);
        event.progress = progress(json);
        event.versionCode = integer(json, "version_code", 0);
        event.versionName = json.getString("version");
        event.remoteVersionCode = integer(json, "remote_version_code", 0);
        event.remoteVersionName = json.getString("remote_version");
        event.forceUpgrade = "1".equals(json.getString("force_upgrade"));
        return event;
    }

    private static int progress(JSONObject json) {
        Object value = json.get("progress");
        if (value == null) return 0;
        try {
            // Camera progress is a percentage with fractional digits, unlike version codes.
            double number = Double.parseDouble(value.toString());
            if (Double.isNaN(number) || Double.isInfinite(number)) return 0;
            return (int) Math.max(0, Math.min(100, number));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int integer(JSONObject json, String key, int fallback) {
        Object value = json.get(key);
        if (value == null) return fallback;
        try {
            long number = Long.parseLong(value.toString());
            return number >= 0 && number <= Integer.MAX_VALUE ? (int) number : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
