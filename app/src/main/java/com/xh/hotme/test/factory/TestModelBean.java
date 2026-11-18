package com.xh.hotme.test.factory;

public class TestModelBean {

    public  static  final int  TYPE_BLUETOOTH = 0;  //蓝牙
    public  static  final int  TYPE_BEEP = 1;  //蜂鸣器
    public  static  final int  TYPE_FAN = 2;  //风扇
    public  static  final int  TYPE_LIGHT = 3;  //指示灯
    public  static  final int  TYPE_RTC_TIME = 4;  //RTC系统时间
    public  static  final int  TYPE_NETWORK_4G = 5;  //4G网络
    public  static  final int  TYPE_NETWORK_WIFI = 6;  //wifi网络
    public  static  final int  TYPE_NETWORK_ETH = 7;  //以太网

    public  static  final int  TYPE_CAMERA = 8;  //摄像头
    public  static  final int  TYPE_AUDIO = 9;  //录音
    public  static  final int  TYPE_FAST_CHARGE = 10;  //快充

    public  static  final int  TYPE_ARECORD = 11;  //arecord录音

    public  static  final int  STATUS_UNTEST = 0;  //未测试
    public  static  final int  STATUS_PASS = 1;  //测试通过
    public  static  final int  STATUS_FAIL = 2;  ///测试未通过

    private String name;
    private String description;
    private int status;

    private int type;

    private int position;

    private String errMessage;
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }

    public TestModelBean(int type, int status, int position) {
        this.status = status;
        this.type = type;
        this.position = position;

        switch (type){
            case TYPE_BLUETOOTH:
                this.name = "蓝牙";
                this.description = "产品能正常的连接主板,能检测到蓝牙的信号强度（强度大于-52db能自动PASS)";
                break;
            case TYPE_BEEP:
                this.name = "蜂鸣器";
                this.description = "是否听到相机发出三声“滴、滴、滴”响声";
                break;
            case TYPE_FAN:
                this.name = "风扇";
                this.description = "检查相机底部的风扇能正常的转动";
                break;
            case TYPE_LIGHT:
                this.name = "指示灯";
                this.description = "用切换开关控制所有LED灯的亮灭状态（点击开LED灯同时亮，点击关LED等同时灭）";
                break;
            case TYPE_RTC_TIME:
                this.name = "RTC系统时间";
                this.description = "连接外网时间能更新到北京时间，然后断电、断网重启系统时间还能与北京时间保持同步";
                break;
            case TYPE_NETWORK_4G:
                this.name = "4G网络";
                this.description = "插入SIM卡，点击4G测试按钮能正常ping通外网";
                break;
            case TYPE_NETWORK_WIFI:
                this.name = "wifi网络";
                this.description = "连接附近的路由器，点击WIFI测试按钮能正常的ping通外网,能检测到WIFI的信号强度（强度大于-52db自动PASS)";
                break;
            case TYPE_NETWORK_ETH:
                this.name = "以太网";
                this.description = "连接好网线，点击以太网测试按钮能正常的ping通外网";
                break;
            case TYPE_CAMERA:
                this.name = "摄像头";
                this.description = "点击测试界面的摄像头测试按钮可以正常的预览2个摄像头的画面";
                break;
            case TYPE_AUDIO:
                this.name = "录音";
                this.description = "点击测试界面的录音按钮进行录制声音约5S左右，然后通过手机进行播放录制的音源";
                break;
            case TYPE_FAST_CHARGE:
                this.name = "快充";
                this.description = "连接电源适配器，获取电流大小，大于标准值既是处于快充";
                break;
            case TYPE_ARECORD:
                this.name = "arecord录音";
                this.description = "点击测试界面的录音按钮进行录制声音约5S左右，然后通过手机进行播放录制的音源";
                break;
        }
    }


}
