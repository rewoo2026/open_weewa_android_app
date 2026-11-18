package com.xh.hotme.camera;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import com.xh.hotme.me.camsetting.CameraSettingsActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.widget.TextView;
import com.alibaba.fastjson.JSONObject;
import com.xh.hotme.R;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.OTAVersionInfo;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.camera.ota.CameraOtaCheck;
import com.xh.hotme.camera.ota.OTAUpdateDialog;
import com.xh.hotme.utils.Constants;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import org.greenrobot.eventbus.EventBus;

/** Injects camera status replies. Queries use a fictitious device; never downloads or installs firmware. */
public class CameraOtaInstrumentation extends Instrumentation {
    private int passed;
    private final AtomicReference<OTAUpdateDialog> dialog = new AtomicReference<>();
    private final AtomicReference<CameraOtaCheck> automaticCheck = new AtomicReference<>();
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        int outcome = Activity.RESULT_OK;
        int originalMode = Constants.ACCESS_MODEL;
        Field cameraField = null;
        Object originalCamera = null;
        Activity activity = null;
        ActivityMonitor monitor = addMonitor(CameraUpgradeActivity.class.getName(), null, false);
        ActivityMonitor settingsMonitor = addMonitor(CameraSettingsActivity.class.getName(), null, false);
        try {
            cameraField = CameraManager.class.getDeclaredField("_connectCameraInfo");
            cameraField.setAccessible(true); originalCamera = cameraField.get(null);
            CameraInfo camera = new CameraInfo("OTA-INSTRUMENTATION");
            camera.deviceInfo = new DeviceInfo();
            camera.deviceInfo.device_id = "ota-instrumentation-no-device";
            camera.deviceInfo.device_name = "weewa_OTA_TEST";
            camera.deviceInfo.ota_version = "1.2.1";
            camera.deviceInfo.ota_version_code = 20261006;
            cameraField.set(null, camera);
            Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_REMOTE;
            CameraManager.clearOTAInfo();
            try (ParcelFileDescriptor command = getUiAutomation().executeShellCommand(
                    "am start -n com.xh.hotme/.me.camsetting.CameraSettingsActivity");
                 FileInputStream output = new FileInputStream(command.getFileDescriptor())) {
                byte[] buffer = new byte[1024]; while (output.read(buffer) != -1) {}
            }
            Activity owner = settingsMonitor.waitForActivityWithTimeout(10000);
            require(owner != null, "Settings activity did not open");
            onMain(() -> owner.startActivity(new Intent(owner, CameraUpgradeActivity.class)));
            activity = monitor.waitForActivityWithTimeout(10000);
            require(activity != null && !activity.isFinishing(), "Upgrade page rejected an empty cache"); passed++;
            CameraUpgradeActivity page = (CameraUpgradeActivity) activity;
            onMain(() -> {
                require(!page.findViewById(R.id.layout_downloadingLayout).isEnabled(), "Checking permits download");
                reply(1, 20261009, "2.0.0"); reply(2, 20261009, "2.0.0");
                require(!page.isFinishing(), "Transient failure closed page");
                require(!page.findViewById(R.id.layout_downloadingLayout).isEnabled(), "Failed check permits download");
                passed++;
                reply(4, 20261009, "2.0.0");
                require(((TextView) page.findViewById(R.id.tv_version_new)).getText().toString().contains("2.0.0"), "Wrong selected version");
                require(page.findViewById(R.id.layout_downloadingLayout).isEnabled(), "Valid offer is disabled");
                require(CameraManager.getOTAVersion().versionCode == 20261009, "Camera offer missing from cache"); passed++;
                reply(4, 20261010, "2.1.0");
                require(((TextView) page.findViewById(R.id.tv_version_new)).getText().toString().contains("2.1.0"), "Recheck kept stale version"); passed++;
                for (double progress : new double[]{34.869190401529089, 99.99, 100}) {
                    reply(5, 20261010, "2.1.0", progress);
                    int expected = (int) progress;
                    require(((android.widget.ProgressBar) page.findViewById(R.id.pbDownloading)).getProgress() == expected,
                            "Fractional camera progress was lost from the progress bar");
                    require(((TextView) page.findViewById(R.id.tvDownloadingProgress)).getText().toString()
                            .equals(page.getString(R.string.ota_download_progress, expected)),
                            "Fractional camera progress was lost from the download label"); passed++;
                }
                reply(7, 20261010, "2.1.0");
                require(page.installHintBindView.installHintLayout.getVisibility() == android.view.View.VISIBLE, "Downloaded package cannot install"); passed++;
                reply(3, 20261006, "1.2.1");
                require(page.isFinishing(), "Latest left stale upgrade actions");
                require(CameraManager.getOTAVersion() == null, "Latest kept stale offer"); passed++;
            });
            // Create native dialog views without attaching/sending a network refresh.
            // Direct EventBus replies still exercise the shared BLE/HTTP/MQ parser and cache.
            onMain(() -> {
                OTAUpdateDialog d = new OTAUpdateDialog(page, staleOffer());
                dialog.set(d); d.create(); EventBus.getDefault().register(d);
                reply(4, 20261011, "3.0.0");
                require(((TextView) d.findViewById(R.id.tvUpgradeTitle)).getText().toString().contains("3.0.0"), "Dialog kept single-server version");
                require(!((TextView) d.findViewById(R.id.tvUpgradeContent)).getText().toString().contains("STALE"), "Stale release notes retained"); passed++;
                reply(1, 20261011, "3.0.0");
                require(!d.findViewById(R.id.tvUpgradeConfirm).isEnabled(), "Dialog allows confirm while checking"); passed++;
                reply(4, 20261012, "3.1.0");
                require(((TextView) d.findViewById(R.id.tvUpgradeTitle)).getText().toString().contains("3.1.0"), "Dialog recheck kept old version");
                require(d.findViewById(R.id.tvUpgradeConfirm).isEnabled(), "Dialog offer remains disabled"); passed++;
                BluetoothHandle.postOTAStatus(JSONObject.parseObject("{\"st\":4,\"version_code\":20261006}"));
                require(CameraManager.getOTAVersion() == null, "Missing remote fields enabled old offer"); passed++;
            });
            onMain(() -> {
                CameraOtaCheck check = new CameraOtaCheck(); automaticCheck.set(check);
                int[] prompts = {0};
                check.start(() -> prompts[0]++);
                check.onStatus(status(1, 20261020, "4.0"));
                check.onStatus(status(2, 20261020, "4.0"));
                require(prompts[0] == 0, "Auto prompt ran before query completed");
                check.onStatus(status(4, 20261020, "4.0"));
                check.onStatus(status(4, 20261020, "4.0"));
                require(prompts[0] == 1, "Fresh auto prompt was lost or duplicated"); passed++;
                check.start(() -> prompts[0]++);
                check.onStatus(status(3, 20261006, "1.2.1"));
                require(prompts[0] == 1, "Latest triggered auto prompt"); passed++;
                check.start(() -> prompts[0]++); check.cancel();
                check.onStatus(status(4, 20261020, "4.0"));
                require(prompts[0] == 1, "Destroyed page triggered auto prompt"); passed++;
                check.start(() -> prompts[0]++);
                Handler timer = (Handler) field(check, "handler");
                Runnable timeout = (Runnable) field(check, "timeout");
                require(timer.hasCallbacks(timeout), "Missing bounded check timeout");
                timeout.run();
                check.onStatus(status(4, 20261020, "4.0"));
                require(prompts[0] == 1 && !timer.hasCallbacks(timeout), "Timeout kept a late prompt alive"); passed++;
                CameraManager.release(); require(CameraManager.getOTAVersion() == null, "Disconnect retained offer"); passed++;
            });
            result.putString("stream", "\nOK (" + passed + " checks): empty-cache entry, pending/failure guards, selected-version refresh, install-ready state, latest clearing, dialog notes, async auto prompt and cancellation\n");
            result.putInt("passed", passed);
        } catch (Throwable e) {
            outcome = Activity.RESULT_CANCELED;
            result.putString("stream", "\nFAIL after " + passed + ": " + e + "\n");
        } finally {
            onMain(() -> {
                if (automaticCheck.get() != null) automaticCheck.get().cancel();
                if (dialog.get() != null) {
                    if (EventBus.getDefault().isRegistered(dialog.get())) EventBus.getDefault().unregister(dialog.get());
                    ((Handler) field(dialog.get(), "_handler")).removeCallbacksAndMessages(null);
                    dialog.get().dismiss();
                }
                CameraManager.clearOTAInfo();
            });
            Constants.ACCESS_MODEL = originalMode;
            try { if (cameraField != null) cameraField.set(null, originalCamera); } catch (Exception ignored) {}
            removeMonitor(monitor);
            removeMonitor(settingsMonitor);
        }
        finish(outcome, result);
    }

    private static OTAVersionInfo staleOffer() {
        OTAVersionInfo info = new OTAVersionInfo(); info.version = "STALE";
        info.versionCode = 20261007; info.descr = "STALE server release notes"; return info;
    }
    private static com.xh.hotme.bluetooth.event.OTAStatusEvent status(int state, int remote, String version) {
        JSONObject json = new JSONObject(); json.put("st", state); json.put("version_code", 20261006);
        json.put("version", "1.2.1"); json.put("remote_version_code", remote);
        json.put("remote_version", version); json.put("force_upgrade", 1);
        return com.xh.hotme.bluetooth.event.OTAStatusEvent.fromJson(json);
    }
    private static void reply(int state, int remote, String version) {
        reply(state, remote, version, 0);
    }
    private static void reply(int state, int remote, String version, double progress) {
        com.xh.hotme.bluetooth.event.OTAStatusEvent e = status(state, remote, version);
        JSONObject json = new JSONObject(); json.put("st", e.status); json.put("version", e.versionName);
        json.put("version_code", e.versionCode); json.put("remote_version", e.remoteVersionName);
        json.put("remote_version_code", e.remoteVersionCode); json.put("force_upgrade", 1);
        json.put("progress", progress);
        BluetoothHandle.postOTAStatus(json);
    }
    private static Object field(Object object, String name) {
        try { Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private void onMain(Runnable action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        runOnMainSync(() -> { try { action.run(); } catch (Throwable e) { failure.set(e); } });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
