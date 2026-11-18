package com.xh.hotme.bluetooth;

import org.junit.Test;
import static org.junit.Assert.*;

public class DeviceUnbindResultTest {
    @Test public void acceptsProtocolSuccess() {
        assertTrue(DeviceUnbindResult.isSuccess("ok"));
        assertTrue(DeviceUnbindResult.isSuccess("OK"));
    }

    @Test public void acceptsLegacyLocalizedSuccess() {
        assertTrue(DeviceUnbindResult.isSuccess("解绑成功"));
        assertTrue(DeviceUnbindResult.isSuccess(" 解绑成功 "));
    }

    @Test public void preservesFailuresAndRejectsMissingResult() {
        for (String value : new String[]{null, "", "验证码错误", "验证码已过期",
                "设备不属于该账号，无法解绑", "解绑失败", "unknown error", "解绑成功失败"}) {
            assertFalse(DeviceUnbindResult.isSuccess(value));
        }
    }
}
