package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class ExecRequestBean extends BaseBluetoothBean {
    public String cmdline;
    public int bg;

    public ExecRequestBean(String cmdline, boolean background) {
        this.c = BleConstants.BLE_CMD_EXEC;
        this.cmdline = cmdline;
        this.bg = background ? 1 : 0;
    }
}
