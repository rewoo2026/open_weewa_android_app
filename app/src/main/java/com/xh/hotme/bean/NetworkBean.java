package com.xh.hotme.bean;

import java.io.Serializable;

public class NetworkBean implements Serializable {
    NetworkStatus wifi;
    NetworkStatus eth;
    NetworkStatus sim;
    ApInfo ap;

    int http;  //http 监听1端口

    // 相机那边的网络是否联通的,也就是相机是否能连上外网
    // 其他的st字段只是表示是否开启,不一定能表示相机是否能联网,这个字段是确定的,相机会15一次探测一下是否能连上
    // 外网,所以这个标志不算很实时,但他是一个确定的指标
    int reachable;

    public long _t;

    public static class NetworkStatus implements Serializable {
        public static final int RSSI_STRONG = 0;
        public static final int RSSI_MEDIUM = 1;
        public static final int RSSI_WEAK = 2;

        public int st;    //对于4g这个st并不表示4g的连接状态,只表示4g是否硬件上是可用的. 对于其他网络则是表示是否连接
        public String ip;   //ip地址
        public String s;  //wifi有用 wifi ssid
        public int rssi_lv;
        public int has_card;  //4G有用 0未插卡1:已插卡
        public int connected;  //4G有用 网络是否连通 0未连通1:已连通

        public String imei;  //4G有用 imei号
        public String apn;  //4G有用 apn名称

        public String iccid;  //4G有用 sim卡 iccid号

        public String rssi2str() {
            switch(rssi_lv) {
                case RSSI_STRONG:
                    return "STRONG";
                case RSSI_MEDIUM:
                    return "MEDIUM";
                default:
                    return "WEAK";
            }
        }
    }

    public NetworkStatus getWifi() {
        return wifi;
    }

    public void setWifi(NetworkStatus wifi) {
        this.wifi = wifi;
    }

    public NetworkStatus getEth() {
        return eth;
    }

    public void setEth(NetworkStatus eth) {
        this.eth = eth;
    }

    public NetworkStatus getSim() {
        return sim;
    }

    public void setSim(NetworkStatus sim) {
        this.sim = sim;
    }

    public int getHttp() {
        return http;
    }

    public void setHttp(int http) {
        this.http = http;
    }

    public boolean isReachable() {
        return reachable != 0;
    }

    public void setReachable(boolean v) {
        reachable = v ? 1 : 0;
    }


    public static class ApInfo implements Serializable {
        public String ssid;
        public String password;
        public int st;
    }

    public ApInfo getAp() {
        return ap;
    }

    public void setAp(ApInfo ap) {
        this.ap = ap;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");

        if(wifi != null) {
            builder.append("wifi: st=" + wifi.st + ", ip=" + wifi.ip + ", s=" + wifi.s + "\n");
        }

        if(eth != null) {
            builder.append("eth: st=" + eth.st + ", ip=" + eth.ip + "\n");
        }

        if(sim != null) {
            builder.append("sim: st=" + sim.st + ", has_card=" + sim.has_card + ", connected=" + sim.connected + ", rssi=" + sim.rssi2str()+ ", imei=" + sim.imei+ ", iccid=" + sim.iccid + "\n");
        }

        if(ap != null) {
            builder.append("ap: ssid=" + ap.ssid + ", password=" + ap.password + ", st=" + ap.st + "\n");
        }

        builder.append("http port= " + http + "\n");

        builder.append("}");

        return builder.toString();
    }
}
