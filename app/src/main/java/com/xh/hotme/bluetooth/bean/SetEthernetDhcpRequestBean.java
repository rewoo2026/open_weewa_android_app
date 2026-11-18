package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;
@Keep
public class SetEthernetDhcpRequestBean extends BaseBluetoothBean {


    public int dhcp;
    public SetEthernetDhcpRequestBean() {
        this.c = BleConstants.BLE_CMD_SET_ETHERNET_DHCP;
        this.dhcp = 1;
    }
}