package com.xh.hotme.camera.ota;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xh.hotme.bean.OTAVersionInfo;
import com.xh.hotme.bluetooth.bean.OTAStatusRequestBean;
import com.xh.hotme.bluetooth.bean.OTADownloadRequestBean;
import com.xh.hotme.bluetooth.bean.OTAInstallRequestBean;
import com.xh.hotme.bluetooth.event.OTAStatusEvent;
import org.junit.Test;
import static org.junit.Assert.*;

public class CameraOtaOfferTest {
    private OTAStatusEvent reply(int state, int local, int remote, String version) {
        JSONObject json = new JSONObject();
        json.put("st", state); json.put("version_code", local);
        json.put("version", "local"); json.put("remote_version_code", remote);
        json.put("remote_version", version); json.put("force_upgrade", 1);
        return OTAStatusEvent.fromJson(json);
    }

    @Test public void usesSelectedCameraOfferRegardlessOfVersionName() {
        OTAVersionInfo offer = CameraOtaOffer.fromStatus(reply(4, 20261006, 20261009, "1.0.0"));
        assertEquals(20261009, offer.versionCode);
        assertEquals("1.0.0", offer.version); assertEquals("1", offer.forceUpdateFlag);
        assertNull(offer.descr); assertNull(offer.updateTitle); assertNull(offer.apkPath);
    }

    @Test public void acceptsEveryActiveUpgradeState() {
        for (int state = 4; state <= 10; state++) {
            assertNotNull("state " + state, CameraOtaOffer.fromStatus(reply(state, 100, 101, "v101")));
        }
    }

    @Test public void rejectsOlderAndEqualOffersExceptInstalledCompletion() {
        for (int state = 4; state <= 10; state++) {
            assertNull(CameraOtaOffer.fromStatus(reply(state, 100, 99, "99")));
            if (state != 9) assertNull(CameraOtaOffer.fromStatus(reply(state, 100, 100, "100")));
        }
        assertNotNull(CameraOtaOffer.fromStatus(reply(9, 100, 100, "100")));
    }

    @Test public void checkingFailureLatestAndNetworkStatesCannotRetainAnOffer() {
        for (int state : new int[]{-1, 0, 1, 2, 3, 11}) {
            assertNull(CameraOtaOffer.fromStatus(reply(state, 100, 200, "200")));
        }
        assertTrue(CameraOtaOffer.isPending(reply(1,100,200,"200")));
        assertTrue(CameraOtaOffer.isPending(reply(2,100,200,"200")));
        assertFalse(CameraOtaOffer.isPending(reply(3,100,200,"200")));
    }

    @Test public void missingAndMalformedCodesNeverEnableUpgrade() {
        for (String raw : new String[]{"{}", "{\"st\":null}",
                "{\"st\":4,\"version_code\":100}",
                "{\"st\":4,\"version_code\":100,\"remote_version_code\":999999999999,\"remote_version\":\"x\"}",
                "{\"st\":4,\"version_code\":100,\"remote_version_code\":\"invalid\",\"remote_version\":\"x\"}",
                "{\"st\":4,\"version_code\":100,\"remote_version_code\":101.5,\"remote_version\":\"x\"}"}) {
            assertNull(raw, CameraOtaOffer.fromStatus(OTAStatusEvent.fromJson(JSON.parseObject(raw))));
        }
        assertNull(CameraOtaOffer.fromStatus(reply(4,100,101," ")));
        assertNull(CameraOtaOffer.fromStatus(reply(4,0,101,"x")));
        assertEquals(-1, OTAStatusEvent.fromJson(null).status);
    }

    @Test public void acceptsStringProtocolFieldsAndMissingProgress() {
        OTAStatusEvent event = OTAStatusEvent.fromJson(JSON.parseObject(
                "{\"st\":\"4\",\"version_code\":\"100\",\"remote_version_code\":\"101\",\"remote_version\":\"new\",\"force_upgrade\":\"1\"}"));
        assertEquals(0, event.progress); assertTrue(event.forceUpgrade);
        assertEquals(101, CameraOtaOffer.fromStatus(event).versionCode);
        assertFalse(reply(3,100,100,"100").status == -1);
        assertFalse(OTAStatusEvent.fromJson(JSON.parseObject("{\"st\":3}")).forceUpgrade);
    }

    @Test public void acceptsFractionalDownloadProgressFromCamera() {
        for (String raw : new String[]{"28.899492498559976", "\"28.899492498559976\""}) {
            OTAStatusEvent event = OTAStatusEvent.fromJson(JSON.parseObject(
                    "{\"st\":5,\"progress\":" + raw + "}"));
            assertEquals(5, event.status);
            assertEquals(28, event.progress);
        }
        assertEquals(34, OTAStatusEvent.fromJson(JSON.parseObject(
                "{\"st\":5,\"progress\":34.869190401529089}")).progress);
        assertEquals(99, OTAStatusEvent.fromJson(JSON.parseObject(
                "{\"st\":5,\"progress\":99.99}")).progress);
    }

    @Test public void boundsProgressAndKeepsMalformedValuesSafe() {
        assertEquals(100, OTAStatusEvent.fromJson(JSON.parseObject("{\"progress\":100}")).progress);
        assertEquals(100, OTAStatusEvent.fromJson(JSON.parseObject("{\"progress\":100.5}")).progress);
        for (String raw : new String[]{"null", "-1", "0.9", "\"invalid\"", "true",
                "\"NaN\"", "\"Infinity\"", "\"-Infinity\"", "\"1e309\""}) {
            assertEquals(raw, 0, OTAStatusEvent.fromJson(JSON.parseObject(
                    "{\"progress\":" + raw + "}")).progress);
        }
    }

    @Test public void keepsExistingCameraCommandsAndRefreshPollingProtocol() {
        assertEquals(1, JSON.parseObject(JSON.toJSONString(new OTAStatusRequestBean(true))).getIntValue("refresh"));
        assertEquals(0, new OTAStatusRequestBean().refresh);
        assertEquals("get_ota_status", new OTAStatusRequestBean(true).c);
        assertEquals("start_ota_download", new OTADownloadRequestBean().c);
        assertEquals("start_ota", new OTAInstallRequestBean(0).c);
    }
}
