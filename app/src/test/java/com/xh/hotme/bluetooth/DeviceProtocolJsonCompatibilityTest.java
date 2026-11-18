package com.xh.hotme.bluetooth;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Exercises the legacy Fastjson API used by camera responses after the SDK upgrade. */
public class DeviceProtocolJsonCompatibilityTest {
    @Test
    public void uploadFailureArrayRemainsAJavaList() {
        JSONObject response = JSON.parseObject("{\"c\":\"auto_upload_status\","
                + "\"r\":\"ok\",\"st\":1,\"progress\":42,\"has_upload\":1,"
                + "\"force_end\":0,\"failed\":[\"match/one.mp4\",\"match/two.mp4\"]}");

        assertEquals("ok", response.getString("r"));
        assertEquals(42, response.getIntValue("progress"));
        assertTrue(response.getIntValue("st") != 0);
        assertEquals(Arrays.asList("match/one.mp4", "match/two.mp4"),
                (List<?>) response.get("failed"));
    }

    @Test
    public void absentStatusFieldsKeepTheirPreviousDefaults() {
        JSONObject response = JSON.parseObject("{\"r\":\"error\"}");

        assertFalse(response.containsKey("progress"));
        assertEquals(0, response.getIntValue("progress"));
        assertNull(response.getInteger("progress"));
        assertNull(response.get("failed"));
    }

    @Test
    public void nestedCameraNetworkObjectsRemainAccessible() {
        JSONObject response = JSON.parseObject("{\"wifi\":{\"st\":1,\"s\":\"test-network\"}}");

        JSONObject wifi = response.getJSONObject("wifi");
        assertEquals(Integer.valueOf(1), wifi.getInteger("st"));
        assertEquals("test-network", wifi.getString("s"));
    }
}
