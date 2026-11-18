package com.xh.hotme.utils;

import android.content.Context;

import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.record.RecordActivity;

public class PlayUtil {
    public static void startPlay(Context context, DeviceInfo deviceInfo, Device device) {
        RecordActivity.start(context, deviceInfo, device);

    }

    public static void startPlay(Context context, DeviceInfo deviceInfo, Device device, NetworkBean network) {
        RecordActivity.start(context, deviceInfo, device, network);
    }
}
