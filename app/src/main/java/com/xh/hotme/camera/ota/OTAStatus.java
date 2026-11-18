package com.xh.hotme.camera.ota;

public enum OTAStatus {
    // 初始状态, 等待网络可用以便可以检查ota版本
    OTA_WAIT_NETWORK(0),

    // 正在检查ota版本
    OTA_CHECKING(1),

    // 检查版本失败, 会自动开始重试, 所以这个状态只可能存在一会儿
    OTA_CHECK_FAILED(2),

    // 版本已是最新无需升级
    OTA_LATEST(3),

    // 可升级，但需要先下载升级包
    OTA_DOWNLOADABLE(4),

    // 正在下载升级包
    OTA_DOWNLOADING(5),

    // 下载失败, 稍后会重试, 所以这个状态不会存在很久
    OTA_DOWNLOAD_FAILED(6),

    // 下载已完成可以开始升级
    OTA_UPGRADABLE(7),

    // 升级中
    OTA_UPGRADING(8),

    // 升级完成
    OTA_UPGRADED(9),

    // 升级失败
    OTA_UPGRADE_FALED(10);

    private final int value;

    //构造器默认也只能是private, 从而保证构造函数只能在内部使用
    OTAStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static OTAStatus fromOrdinal(int ordinal) {
        try {
            return values()[ordinal];
        } catch(Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}