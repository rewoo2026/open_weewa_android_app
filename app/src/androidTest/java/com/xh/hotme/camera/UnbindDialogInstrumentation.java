package com.xh.hotme.camera;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Handler;
import android.widget.TextView;
import com.alibaba.fastjson.JSONObject;
import com.xh.hotme.R;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.me.camsetting.CameraSettingsActivity;
import java.lang.reflect.Field;
import java.io.FileInputStream;
import java.util.concurrent.atomic.AtomicReference;

/** Injects camera replies locally; never sends SMS or unbind requests. */
public class UnbindDialogInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        AtomicReference<MobileUnbindDialog> dialog = new AtomicReference<>();
        Activity activity = null;
        ActivityMonitor monitor = addMonitor(CameraSettingsActivity.class.getName(), null, false);
        int status = Activity.RESULT_OK;
        try {
            // MIUI blocks starts attributed to the test APK; the shell can open
            // the exported settings activity without changing app permissions.
            try (ParcelFileDescriptor command = getUiAutomation().executeShellCommand(
                    "am start -n com.xh.hotme/.me.camsetting.CameraSettingsActivity");
                 FileInputStream output = new FileInputStream(command.getFileDescriptor())) {
                byte[] buffer = new byte[1024];
                while (output.read(buffer) != -1) { /* drain the shell command */ }
            }
            activity = monitor.waitForActivityWithTimeout(10000);
            require(activity != null, "Camera settings activity did not open");
            final Activity owner = activity;
            onMain(() -> {
                MobileUnbindDialog d = new MobileUnbindDialog(owner, "02:00:00:00:00:FE");
                dialog.set(d); d.show(); d.onSendUnbindSms();
            });
            Thread.sleep(200);
            onMain(() -> BluetoothHandle.postDeviceUnBind(
                    JSONObject.parseObject("{\"c\":\"device_unbind\",\"r\":\"验证码错误\",\"ec\":1}")));
            Thread.sleep(200);
            onMain(() -> {
                MobileUnbindDialog d = dialog.get();
                require(d.isShowing(), "A failure must preserve the retry form");
                TextView message = d.findViewById(R.id.login_status);
                require("验证码错误".contentEquals(message.getText()), "Wrong failure message");
            });
            Field handlerField = MobileUnbindDialog.class.getDeclaredField("_handler");
            Field timeoutField = MobileUnbindDialog.class.getDeclaredField("_unbindTimeoutRunnable");
            handlerField.setAccessible(true); timeoutField.setAccessible(true);
            Handler handler = (Handler) handlerField.get(dialog.get());
            Runnable timeout = (Runnable) timeoutField.get(dialog.get());
            onMain(() -> handler.postDelayed(timeout, 60000));
            onMain(() -> BluetoothHandle.postDeviceUnBind(
                    JSONObject.parseObject("{\"c\":\"device_unbind\",\"r\":\"解绑成功\",\"ec\":1}")));
            Thread.sleep(300);
            onMain(() -> require(!dialog.get().isShowing(), "Legacy success must dismiss the dialog"));
            require(!handler.hasCallbacks(timeout), "Success left a timeout queued");
            onMain(() -> dialog.get().onUnbind()); // Duplicate success must be harmless.
            Thread.sleep(200);
            onMain(() -> {
                require(!dialog.get().isShowing(), "Duplicate callback reopened the dialog");
                MobileUnbindDialog d = new MobileUnbindDialog(owner, "02:00:00:00:00:FE");
                dialog.set(d); d.show();
            });
            Thread.sleep(200);
            onMain(() -> BluetoothHandle.postDeviceUnBind(JSONObject.parseObject(
                        "{\"c\":\"device_unbind\",\"r\":\"ok\",\"ec\":0}")));
            Thread.sleep(300);
            onMain(() -> {
                require(!dialog.get().isShowing(), "Protocol success must dismiss the dialog");
                require(owner.findViewById(R.id.photo_name).isShown(), "Underlying settings page is unavailable");
            });
            result.putString("stream", "\nOK: failure keeps retry form; legacy and protocol success dismiss; duplicate callback safe; timeout cleared\n");
            result.putInt("passed", 5);
        } catch (Throwable e) {
            status = Activity.RESULT_CANCELED;
            result.putString("stream", "\nFAIL: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n");
        } finally {
            onMain(() -> { if (dialog.get() != null) dialog.get().dismiss(); });
            removeMonitor(monitor);
        }
        finish(status, result);
    }

    private void onMain(Runnable action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        runOnMainSync(() -> {
            try { action.run(); } catch (Throwable e) { failure.set(e); }
        });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
