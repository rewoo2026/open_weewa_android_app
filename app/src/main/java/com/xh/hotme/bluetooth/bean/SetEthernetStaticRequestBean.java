package com.xh.hotme.bluetooth.bean;

import androidx.annotation.Keep;

import com.xh.hotme.bluetooth.BleConstants;

@Keep
public class SetEthernetStaticRequestBean extends BaseBluetoothBean {

    public int dhcp;
    private String ip;
    private String netmask;
    private String gateway;
    private String dns;

    public SetEthernetStaticRequestBean(int dhcp, String ip, String subnet, String gateway, String dns) {
        this.c = BleConstants.BLE_CMD_SET_ETHERNET_DHCP;
        this.dhcp = dhcp;
        this.ip = ip;
        this.netmask = subnet;
        this.gateway = gateway;
        this.dns = dns;
    }


    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getSubnet() {
        return netmask;
    }

    public void setSubnet(String subnet) {
        this.netmask = subnet;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
    }
}