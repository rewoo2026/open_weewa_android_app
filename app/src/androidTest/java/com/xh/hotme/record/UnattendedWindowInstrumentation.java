package com.xh.hotme.record;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.TextView;
import android.widget.ScrollView;
import android.view.inputmethod.InputMethodManager;
import com.contrarywind.view.WheelView;
import com.xh.hotme.R;
import com.xh.hotme.account.DailyAccessManager;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.bean.LocalRecordStopRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStatusRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStopRequestBean;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.widget.SwitchButtonO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;

/** Explicit real-camera test. Defaults to read-only inspection; -e verify true records short labelled clips. */
public class UnattendedWindowInstrumentation extends Instrumentation implements IRecordControlListener {
    private Bundle args;
    private final AtomicReference<RecordStatusResultBean> reply = new AtomicReference<>();
    private final JSONArray evidence = new JSONArray();
    private File directory;
    private AddRecordTaskActivity form;
    private RecordActivity recorder;
    private String ownedTask, name;

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); args = arguments; start(); }
    @Override public void onStart() {
        Bundle result = new Bundle(); int outcome = Activity.RESULT_OK;
        int originalMode = Constants.ACCESS_MODEL;
        try {
            directory = new File(getTargetContext().getExternalFilesDir(null), "unattended-window-evidence");
            check(directory.isDirectory() || directory.mkdirs(), "Cannot create evidence directory");
            check(args.getString("address") != null && args.getString("deviceId") != null, "Explicit camera identity required");
            await(() -> com.xh.hotme.HotmeApplication.getInstance() != null, 10_000, "Application initialization");
            check(LoginManager.isLogon(), "Sign in before testing");
            if ("true".equals(args.getString("verify"))) {
                try (ParcelFileDescriptor command = getUiAutomation().executeShellCommand("am start -n com.xh.hotme/.main.MainActivity");
                     FileInputStream input = new FileInputStream(command.getFileDescriptor())) {
                    byte[] buffer = new byte[1024]; while (input.read(buffer) != -1) { }
                }
                Thread.sleep(1_500);
            }
            onMain(() -> {
                Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_LOCAL;
                CameraManager.newCamera(args.getString("address"));
                BluetoothHandle.addPlayNotifyListener(this);
                BluetoothManager.getImpl().connectToBle(new Device(args.getString("address"), 0, args.getString("name", "weewa175")));
            });
            await(() -> BluetoothManager.getImpl().isConnected(), 30_000, "Bluetooth connection");
            RecordStatusResultBean before = query();
            check(args.getString("deviceId").equals(CameraManager.getDeviceId()), "Unexpected camera identity");
            note("camera", snapshot(before));
            if ("true".equals(args.getString("cancelExisting"))) {
                UnattendedRecordTask existing = UnattendedRecordService.current(getTargetContext());
                check(existing == null || args.getString("deviceId").equals(existing.deviceId), "Existing task belongs to another camera");
                if (existing != null) {
                    onMain(() -> UnattendedRecordService.finishForDevice(getTargetContext()));
                    String existingName = existing.info == null ? existing.request.getVn() : existing.info.name;
                    if (before.getStatus() > 0 && existingName.equals(before.getVideoName())) {
                        stop(before); await(() -> query().getStatus() == 0, 45_000, "Cancelled task stop");
                    }
                    check(UnattendedRecordService.current(getTargetContext()) == null, "Task cancellation failed");
                    note("cancelled-existing", existingName);
                }
            }
            if (!"true".equals(args.getString("verify"))) {
                result.putString("stream", "\nPASS inspection: " + snapshot(before) + "; dailyReady=" + DailyAccessManager.ready() + "\n");
            } else {
                check(before.getStatus() == 0 && !"starting".equals(before.deviceState), "Camera is busy; do not interrupt existing recording");
                check(UnattendedRecordService.current(getTargetContext()) == null, "An unattended task already exists");
                if (before.localControlSupported == 1) synchronizeDaily();
                if (!"true".equals(args.getString("timerOnly"))) defaults();
                scheduledWindow();
                result.putString("stream", "true".equals(args.getString("timerOnly"))
                        ? "\nPASS: waiting button contains status only; existing timer counts down; recording restores elapsed timer and original stop button; test recording cleaned up.\n"
                        : "\nPASS: real form optional fields/picker/clear; default now + 2h; recording confirmed; future window does not start early; interrupted recording restarts; deadline stop and no late restart. Evidence=" + directory + "\n");
            }
        } catch (Throwable error) {
            outcome = Activity.RESULT_CANCELED;
            try { note("FAIL", error.toString()); capture("failure.png"); } catch (Exception ignored) { }
            result.putString("stream", "\nFAIL: " + error + "\n");
        } finally {
            try {
                UnattendedRecordTask saved = UnattendedRecordService.current(getTargetContext());
                if (saved != null && saved.id.equals(ownedTask)) onMain(() -> UnattendedRecordService.finishForDevice(getTargetContext()));
                if (name != null) {
                    RecordStatusResultBean current = query();
                    if (name.equals(current.getVideoName()) && (current.getStatus() > 0 || "starting".equals(current.deviceState))) {
                        stop(current);
                        await(() -> query().getStatus() == 0, 45_000, "Cleanup stop");
                    }
                }
                note("final", snapshot(query()));
            } catch (Throwable error) {
                outcome = Activity.RESULT_CANCELED;
                result.putString("stream", result.getString("stream", "") + "Cleanup unconfirmed: " + error + "\n");
            }
            onMain(() -> {
                if (form != null && !form.isFinishing()) form.finish();
                if (recorder != null && !recorder.isFinishing()) recorder.finish();
                BluetoothHandle.removePlayNotifyListener(this);
            });
            Constants.ACCESS_MODEL = originalMode;
        }
        finish(outcome, result);
    }

    private void synchronizeDaily() throws Exception {
        AtomicReference<String> synced = new AtomicReference<>();
        onMain(() -> DailyAccessManager.synchronize(false, new IGenericApiCallback<DailyAccessManager.Profile>() {
            public void onSuccess(DailyAccessManager.Profile value) { synced.set("ok"); }
            public void onFail(String code, String message) { synced.set(message); }
            public void onFinish() { }
        }));
        await(() -> synced.get() != null, 60_000, "Daily authorization synchronization");
        check("ok".equals(synced.get()), "Daily authorization: " + synced.get());
        DailyAccessManager.DeviceAccess access = DailyAccessManager.device(args.getString("deviceId"));
        check(access != null && access.recordingAllowed && access.permit != null, "Camera recording permission unavailable");
        note("daily", "Valid authorization for selected camera");
    }

    private void defaults() throws Exception {
        open("D");
        onMain(() -> {
            check(((TextView) form.findViewById(R.id.record_start_time)).length() == 0, "Start should be optional");
            check(((TextView) form.findViewById(R.id.record_end_time)).length() == 0, "End should be optional");
        });
        capture("01-optional-inputs.png");
        long temporary = (System.currentTimeMillis() / 60_000 + 2) * 60_000;
        selectTime(true, temporary);
        onMain(() -> form.findViewById(R.id.clear_record_start_time).performClick());
        selectTime(false, temporary + 120_000);
        onMain(() -> form.findViewById(R.id.clear_record_end_time).performClick());
        long clicked = System.currentTimeMillis();
        onMain(() -> form.findViewById(R.id.btn_record).performClick());
        UnattendedRecordTask task = awaitTask();
        check(task.startAt >= clicked && task.startAt <= System.currentTimeMillis(), "Blank start did not resolve at click");
        check(task.endAt - task.startAt == LocalRecordingWindow.DEFAULT_DURATION_MS, "Blank end did not resolve to +2h");
        note("defaults", new JSONObject().put("startAt", task.startAt).put("endAt", task.endAt));
        RecordStatusResultBean started = awaitRecording(90_000);
        note("default-recording", snapshot(started));
        onMain(() -> UnattendedRecordService.finishForDevice(getTargetContext()));
        stop(started);
        await(() -> query().getStatus() == 0, 45_000, "Default test stop");
        ownedTask = null;
        Thread.sleep(1_500);
    }

    private void scheduledWindow() throws Exception {
        open("W");
        long now = System.currentTimeMillis();
        long startAt = (now / 60_000 + 2) * 60_000;
        if ("true".equals(args.getString("timerOnly"))) startAt = (now / 60_000 + (now % 60_000 < 40_000 ? 1 : 2)) * 60_000;
        final long windowStart = startAt;
        long endAt = startAt + 120_000;
        selectTime(true, startAt);
        selectTime(false, endAt);
        capture("03-selected-window.png");
        onMain(() -> form.findViewById(R.id.btn_record).performClick());
        UnattendedRecordTask task = awaitTask();
        check(task.startAt == windowStart && task.endAt == endAt, "Picker values were not saved accurately");
        note("scheduled", new JSONObject().put("startAt", startAt).put("endAt", endAt));
        Intent recorderIntent = new Intent(getTargetContext(), RecordActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        recorderIntent.putExtra(Constants.REQUEST_DEVICE_INFO, GsonUtils.GsonString(CameraManager.getDeviceInfo()));
        recorderIntent.putExtra(Constants.REQUEST_DEVICE, GsonUtils.GsonString(BluetoothManager.getConnectDevice()));
        recorder = (RecordActivity) startActivitySync(recorderIntent);
        Thread.sleep(1_500);
        AtomicReference<String> firstLabel = new AtomicReference<>(), secondLabel = new AtomicReference<>();
        onMain(() -> {
            check(recorder.getString(R.string.unattended_waiting_button).contentEquals(((TextView) recorder.findViewById(R.id.btn_recorder)).getText()), "Waiting button should contain only its status");
            firstLabel.set(((TextView) recorder.findViewById(R.id.record_time)).getText().toString());
        });
        Thread.sleep(2_100);
        onMain(() -> {
            check(recorder.getString(R.string.unattended_waiting_button).contentEquals(((TextView) recorder.findViewById(R.id.btn_recorder)).getText()), "Button status changed while waiting");
            secondLabel.set(((TextView) recorder.findViewById(R.id.record_time)).getText().toString());
            dismissPreviewPrompts();
        });
        check(firstLabel.get().matches("[0-9]{2}:[0-9]{2}:[0-9]{2}") && !firstLabel.get().equals(secondLabel.get()), "Shared timer countdown did not update");
        note("shared-timer-countdown", new JSONObject().put("before", firstLabel.get()).put("after", secondLabel.get()));
        capture("countdown-on-existing-timer.png");
        while (System.currentTimeMillis() < startAt) {
            RecordStatusResultBean status = query();
            if (System.currentTimeMillis() < startAt) check(status.getStatus() == 0 && !"starting".equals(status.deviceState), "Recording started early");
            Thread.sleep(1_000);
        }
        RecordStatusResultBean first = awaitRecording(Math.min(70_000, endAt - System.currentTimeMillis() - 35_000));
        note("window-first-frame", snapshot(first));
        Thread.sleep(1_200);
        onMain(() -> {
            check(recorder.getString(R.string.stop_record).contentEquals(((TextView) recorder.findViewById(R.id.btn_recorder)).getText()), "Recording button did not restore its existing status");
            check(((TextView) recorder.findViewById(R.id.record_time)).length() > 0, "Existing recording timer did not resume");
        });
        capture("05-recording.png");
        if ("true".equals(args.getString("timerOnly"))) {
            note("recording-timer", ((TextView) recorder.findViewById(R.id.record_time)).getText().toString());
            onMain(() -> UnattendedRecordService.finishForDevice(getTargetContext()));
            stop(first);
            await(() -> query().getStatus() == 0, 45_000, "Timer display test stop");
            ownedTask = null;
            return;
        }
        // A device stop simulates an interruption; the normal UI stop intentionally cancels the watchdog.
        stop(first);
        await(() -> query().getStatus() == 0, 25_000, "Interruption stop");
        check(UnattendedRecordService.current(getTargetContext()) != null, "Interruption cancelled unattended task");
        note("interrupted", snapshot(query()));
        RecordStatusResultBean resumed = awaitRecording(Math.min(65_000, endAt - System.currentTimeMillis() - 3_000));
        if (first.localControl == 1) check(!first.sessionId.equals(resumed.sessionId), "Did not create a resumed device session");
        note("resumed", snapshot(resumed));
        note("device-deadline", new JSONObject().put("requestedEndAt", endAt).put("cameraStopAt", resumed.stopAt)
                .put("cameraEnforcesWindow", resumed.stopAt == endAt));
        while (System.currentTimeMillis() < endAt) { query(); Thread.sleep(800); }
        await(() -> query().getStatus() == 0 && UnattendedRecordService.current(getTargetContext()) == null,
                15_000, "Deadline did not stop recording and clear task");
        note("deadline-stopped", snapshot(query()));
        Thread.sleep(31_000);
        check(query().getStatus() == 0 && UnattendedRecordService.current(getTargetContext()) == null, "Recording restarted after deadline");
        note("after-deadline", snapshot(query()));
        capture("04-finished.png");
        ownedTask = null;
    }

    private void open(String suffix) throws Exception {
        name = "WindowCheck" + suffix + new SimpleDateFormat("MMddHHmm", Locale.US).format(new Date());
        form = (AddRecordTaskActivity) startActivitySync(new Intent(getTargetContext(), AddRecordTaskActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        onMain(() -> {
            check(form.findViewById(R.id.local_record_window).getVisibility() == View.VISIBLE, "Local time inputs not visible");
            ((TextView) form.findViewById(R.id.tv_game_name)).setText(name);
            ((SwitchButtonO) form.findViewById(R.id.switch_unattended)).setChecked(true);
            form.getSystemService(InputMethodManager.class).hideSoftInputFromWindow(form.findViewById(R.id.tv_game_name).getWindowToken(), 0);
            form.findViewById(R.id.tv_game_name).clearFocus();
            View row = form.findViewById(R.id.local_record_window);
            while (row != null && !(row instanceof ScrollView)) row = row.getParent() instanceof View ? (View) row.getParent() : null;
            if (row != null) ((ScrollView) row).fullScroll(View.FOCUS_DOWN);
        });
        Thread.sleep(700);
    }

    private void selectTime(boolean start, long time) throws Exception {
        onMain(() -> form.findViewById(start ? R.id.record_start_time : R.id.record_end_time).performClick());
        Thread.sleep(400);
        Calendar calendar = Calendar.getInstance(); calendar.setTimeInMillis(time);
        onMain(() -> {
            selectWheel(com.bigkoo.pickerview.R.id.year, calendar.get(Calendar.YEAR));
            selectWheel(com.bigkoo.pickerview.R.id.month, calendar.get(Calendar.MONTH) + 1);
            selectWheel(com.bigkoo.pickerview.R.id.day, calendar.get(Calendar.DAY_OF_MONTH));
            selectWheel(com.bigkoo.pickerview.R.id.hour, calendar.get(Calendar.HOUR_OF_DAY));
            selectWheel(com.bigkoo.pickerview.R.id.min, calendar.get(Calendar.MINUTE));
            form.findViewById(com.bigkoo.pickerview.R.id.btnSubmit).performClick();
        });
        Thread.sleep(400);
        capture(start ? "02-start-picker.png" : "02-end-picker.png");
    }
    private void selectWheel(int id, int value) {
        WheelView wheel = form.findViewById(id); check(wheel != null, "Time picker not displayed");
        for (int index = 0; index < wheel.getAdapter().getItemsCount(); index++) {
            if (String.valueOf(value).equals(String.valueOf(wheel.getAdapter().getItem(index)))) { wheel.setCurrentItem(index); return; }
        }
        throw new AssertionError("Time picker value missing: " + value);
    }

    private void dismissPreviewPrompts() {
        for (String field : new String[]{"_startSoftApDialog", "_joinSoftApDialog", "_networkTypeDialog"}) {
            try {
                java.lang.reflect.Field property = RecordActivity.class.getDeclaredField(field);
                property.setAccessible(true);
                android.app.Dialog dialog = (android.app.Dialog) property.get(recorder);
                if (dialog != null && dialog.isShowing()) dialog.dismiss();
            } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        }
    }
    private UnattendedRecordTask awaitTask() throws Exception {
        await(() -> UnattendedRecordService.current(getTargetContext()) != null, 30_000, "Form did not save unattended task");
        UnattendedRecordTask task = UnattendedRecordService.current(getTargetContext()); ownedTask = task.id; return task;
    }
    private RecordStatusResultBean awaitRecording(long timeout) throws Exception {
        AtomicReference<RecordStatusResultBean> actual = new AtomicReference<>();
        await(() -> {
            RecordStatusResultBean s = query();
            if (s.getStatus() == 1 && name.equals(s.getVideoName()) && (s.recordingProtocolVersion < 3 || s.firstFrame == 1)) { actual.set(s); return true; }
            return false;
        }, Math.max(1, timeout), "Recording first frame not confirmed");
        return actual.get();
    }
    private RecordStatusResultBean query() throws Exception {
        reply.set(null);
        long deadline = SystemClock.elapsedRealtime() + 25_000;
        while (reply.get() == null && SystemClock.elapsedRealtime() < deadline) {
            onMain(() -> BluetoothManager.getImpl().sendCmdSimpleObj(new RecordStatusRequestBean()));
            long next = SystemClock.elapsedRealtime() + 1_500;
            while (reply.get() == null && SystemClock.elapsedRealtime() < next) Thread.sleep(100);
        }
        check(reply.get() != null, "Camera status reply");
        return reply.get();
    }
    private void stop(RecordStatusResultBean status) {
        onMain(() -> BluetoothManager.getImpl().sendCmdSimpleObj(status.localControlSupported == 1
                ? new LocalRecordStopRequestBean(status.competitionId, status.sessionId) : new RecordStopRequestBean()));
    }
    private JSONObject snapshot(RecordStatusResultBean status) throws Exception {
        return new JSONObject().put("at", System.currentTimeMillis()).put("status", status.getStatus())
                .put("protocol", status.recordingProtocolVersion).put("localSupported", status.localControlSupported)
                .put("firstFrame", status.firstFrame).put("state", status.deviceState).put("name", status.getVideoName())
                .put("competitionId", status.competitionId).put("sessionId", status.sessionId).put("stopAt", status.stopAt)
                .put("fecFile", status.fec_fn).put("avsFile", status.avs_fn);
    }
    private void note(String event, Object data) throws Exception {
        JSONObject entry = new JSONObject().put("event", event).put("at", System.currentTimeMillis()).put("data", data);
        evidence.put(entry);
        try (FileOutputStream output = new FileOutputStream(new File(directory, "result.json"))) { output.write(evidence.toString(2).getBytes(StandardCharsets.UTF_8)); }
        Bundle progress = new Bundle(); progress.putString("stream", "\n" + entry + "\n"); sendStatus(0, progress);
    }
    private void capture(String name) throws Exception {
        Bitmap bitmap = getUiAutomation().takeScreenshot();
        if (bitmap != null) try (FileOutputStream output = new FileOutputStream(new File(directory, name))) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); bitmap.recycle(); }
    }
    private void onMain(Runnable action) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        super.runOnMainSync(() -> { try { action.run(); } catch (Throwable failure) { error.set(failure); } });
        if (error.get() != null) throw new AssertionError(error.get());
    }
    private interface Condition { boolean ready() throws Exception; }
    private void await(Condition condition, long timeout, String message) throws Exception {
        long end = SystemClock.elapsedRealtime() + timeout;
        while (SystemClock.elapsedRealtime() < end) { if (condition.ready()) return; Thread.sleep(200); }
        throw new AssertionError(message);
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public void onRecordVideoStart() { }
    public void onRecordVideoStop() { }
    public void onRecordException(String message) { }
    public void onRecordVideoStatusUpdate(RecordStatusResultBean status) {
        if (status != null && "ok".equalsIgnoreCase(status.getR())) reply.set(status);
    }
}
