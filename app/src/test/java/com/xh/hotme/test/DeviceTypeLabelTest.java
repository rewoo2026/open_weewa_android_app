package com.xh.hotme.test;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DeviceTypeLabelTest {
    @Test
    public void rendersFixedDeviceFromOperationalType() {
        assertEquals("固定设备（DT_FIXED，4）", DeviceTypeLabel.format("4"));
        assertEquals("固定设备（DT_FIXED，4）", DeviceTypeLabel.format(" 4 "));
    }

    @Test
    public void handlesMissingAndUnknownTypes() {
        assertEquals("未知", DeviceTypeLabel.format(null));
        assertEquals("未知", DeviceTypeLabel.format(""));
        assertEquals("未知", DeviceTypeLabel.format("  "));
        assertEquals("未知类型（99）", DeviceTypeLabel.format("99"));
    }
}
