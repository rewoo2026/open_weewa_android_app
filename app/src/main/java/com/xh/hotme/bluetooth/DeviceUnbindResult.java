package com.xh.hotme.bluetooth;

/** Recognizes the camera protocol and the success text sent by older firmware. */
public final class DeviceUnbindResult {
    private DeviceUnbindResult() {}

    public static boolean isSuccess(String result) {
        if (result == null) return false;
        String value = result.trim();
        // Older firmware forwarded the server's success message as r with ec=1.
        return "ok".equalsIgnoreCase(value) || "解绑成功".equals(value);
    }
}
