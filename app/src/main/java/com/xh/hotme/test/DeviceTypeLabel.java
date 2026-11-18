package com.xh.hotme.test;

/** Operational camera types, matching the firmware WeewaDeviceType enum. */
final class DeviceTypeLabel {
    private DeviceTypeLabel() {}

    static String format(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "未知";
        }
        switch (type.trim()) {
            case "0": return "未知（DT_UNDEFINED1，0）";
            case "1": return "测试设备（DT_TEST，1）";
            case "2": return "个人设备（DT_PERSONAL，2）";
            case "3": return "兼职设备（DT_PARTTIME，3）";
            case "4": return "固定设备（DT_FIXED，4）";
            case "5": return "IBS设备（DT_IBS，5）";
            case "6": return "专业设备（DT_PROFESSIONAL，6）";
            case "7": return "商用设备（DT_COMMERCIAL，7）";
            default: return "未知类型（" + type.trim() + "）";
        }
    }
}
