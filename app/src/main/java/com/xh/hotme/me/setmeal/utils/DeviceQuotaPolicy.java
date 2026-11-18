package com.xh.hotme.me.setmeal.utils;

public final class DeviceQuotaPolicy {
    private DeviceQuotaPolicy() { }
    public static boolean usesV2(String type) { return "6".equals(type) || "7".equals(type); }
    public static boolean isFullyExempt(String type) { return "1".equals(type) || "4".equals(type); }
    public static boolean requiresUsageUpload(String type, int countType) {
        return !isFullyExempt(type) && !(usesV2(type) && countType == 1);
    }
    public static int billableMinutes(int seconds) { return (int) ((Math.max(0L, seconds) + 59L) / 60L); }
    public static String cacheKey(String device, int countType) { return "PlanUsage:" + device + ":" + countType; }
}
