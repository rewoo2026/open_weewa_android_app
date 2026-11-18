package com.xh.hotme.me.setmeal.utils;
import org.junit.Test;
import static org.junit.Assert.*;
public class DeviceQuotaPolicyTest {
    @Test public void fixedCamerasKeepSeparateRecordingAndLiveUsage() {
        assertTrue(DeviceQuotaPolicy.requiresUsageUpload("2", 1));
        assertTrue(DeviceQuotaPolicy.requiresUsageUpload("2", 3));
        assertTrue(DeviceQuotaPolicy.requiresUsageUpload("3", 1));
        assertFalse(DeviceQuotaPolicy.usesV2("2"));
    }
    @Test public void commercialMobileCameraStillPaysForLive() {
        for (String type : new String[]{"6", "7"}) {
            assertFalse(DeviceQuotaPolicy.requiresUsageUpload(type, 1));
            assertTrue(DeviceQuotaPolicy.requiresUsageUpload(type, 3));
            assertTrue(DeviceQuotaPolicy.usesV2(type));
        }
        assertFalse(DeviceQuotaPolicy.requiresUsageUpload("4", 3));
        assertFalse(DeviceQuotaPolicy.requiresUsageUpload("1", 1));
    }
    @Test public void wholeMinutesAreNotOvercharged() {
        assertEquals(0, DeviceQuotaPolicy.billableMinutes(0));
        assertEquals(1, DeviceQuotaPolicy.billableMinutes(1));
        assertEquals(1, DeviceQuotaPolicy.billableMinutes(60));
        assertEquals(2, DeviceQuotaPolicy.billableMinutes(61));
        assertEquals(120, DeviceQuotaPolicy.billableMinutes(7200));
    }
    @Test public void failedUsageReportsCannotCrossDevicesOrUsageTypes() {
        assertNotEquals(DeviceQuotaPolicy.cacheKey("A", 1), DeviceQuotaPolicy.cacheKey("B", 1));
        assertNotEquals(DeviceQuotaPolicy.cacheKey("A", 1), DeviceQuotaPolicy.cacheKey("A", 3));
    }
}
