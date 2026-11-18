package com.xh.hotme.bluetooth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.xh.hotme.bean.BindResultBean;
import com.xh.hotme.bluetooth.bean.DeviceActiveBindRequestBean;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeviceActiveBindRequestBeanTest {
    private final Gson gson = new Gson();

    @Test public void fixedCodeEmptyDataStillSendsStringMac() {
        BindResultBean verification = gson.fromJson("{}", BindResultBean.class);
        JsonObject command = gson.toJsonTree(new DeviceActiveBindRequestBean(
                "13800000000", verification.getMac())).getAsJsonObject();
        assertEquals("active_bind", command.get("c").getAsString());
        assertEquals("13800000000", command.get("mb").getAsString());
        assertTrue(command.has("mac"));
        assertFalse(command.get("mac").isJsonNull());
        assertEquals("", command.get("mac").getAsString());
    }

    @Test public void legacyVerificationRetainsCameraMac() {
        BindResultBean verification = gson.fromJson(
                "{\"mac\":\"00:11:22:33:44:55\"}", BindResultBean.class);
        JsonObject command = gson.toJsonTree(new DeviceActiveBindRequestBean(
                "13800000000", verification.getMac())).getAsJsonObject();
        assertEquals("00:11:22:33:44:55", command.get("mac").getAsString());
    }
}
