package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class RoiOpRequestBean extends BaseBluetoothBean {
    public static final int ROI_OP_LEFT = 0;
    public static final int ROI_OP_RIGHT = 1;
    public static final int ROI_OP_UP = 2;
    public static final int ROI_OP_BOTTOM = 3;
    public static final int ROI_OP_SCALE_UP = 4;
    public static final int ROI_OP_SCALE_DOWN = 5;
    public static final int ROI_OP_RESET = 6;
    public static final int ROI_OP_FREE_MOVE = 7; // p1是x偏移,p2是y偏移
    public static final int ROI_OP_FREE_SCALE = 8; // p1是缩放比例

    final int op;

    // 额外参数,有没有意义具体要看op的类型
    final double p1;
    final double p2;

    public RoiOpRequestBean(int op, double p1, double p2) {
        this.c = BleConstants.BLE_CMD_ROI_OP;
        this.op = op;
        this.p1 = p1;
        this.p2 = p2;
    }
}
