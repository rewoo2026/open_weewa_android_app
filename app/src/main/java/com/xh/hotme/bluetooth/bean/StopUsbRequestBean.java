package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.camera.CameraManager;

import java.util.HashMap;
import java.util.Map;

@Keep
public class StopUsbRequestBean extends BaseBluetoothBean {
    public final long _t;
    public final String _c;

    public StopUsbRequestBean() {
        _t = System.currentTimeMillis();
        this.c = BleConstants.BLE_CMD_STOP_USB;
        Map<String, String> params = new HashMap<>();
        params.put(BleConstants.BLE_KEY_COMMAND, BleConstants.BLE_CMD_STOP_USB);
        params.put(BleConstants.BLE_KEY_TIMESTAMP, String.valueOf(_t));
        _c = CameraManager.calculateDigest(params);
    }
}
