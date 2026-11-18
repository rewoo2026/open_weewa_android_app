package com.xh.hotme.record;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import com.xh.hotme.HotmeApplication;
import com.xh.hotme.account.DailyAccessManager;
import com.xh.hotme.R;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.bean.BaseBluetoothBean;
import com.xh.hotme.bluetooth.bean.RecordStartRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStatusRequestBean;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.live.AddLiveTaskActivity;
import com.xh.hotme.live.bean.HmCompetitionLive;
import com.xh.hotme.live.bean.LiveTaskBean;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.widget.SwitchButtonO;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Callback;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.android.controller.ServiceController;
import java.util.function.LongSupplier;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class, qualifiers = "zh-rCN",
        shadows = {UnattendedModeTest.Http.class, UnattendedModeTest.Device.class, UnattendedModeTest.Daily.class,
                UnattendedModeTest.Bluetooth.class})
public class UnattendedModeTest {
    private Context previousContext;
    private int previousMode;
    private String previousDevice;

    @Before public void setup() {
        previousContext = ReflectionHelpers.getStaticField(HotmeApplication.class, "smContext");
        ReflectionHelpers.setStaticField(HotmeApplication.class, "smContext", RuntimeEnvironment.getApplication());
        previousMode = Constants.ACCESS_MODEL;
        Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_REMOTE;
        previousDevice = CameraManager.getSaveDeviceIdLast();
        CameraManager.setDeviceId("managed-test-camera");
        Http.params = null;
        Http.callback = null;
        Device.commands.clear();
        Device.query = null; Device.start = null;
        Device.connected = false;
        RuntimeEnvironment.getApplication().getSharedPreferences("unattended_recording", 0).edit().clear().commit();
        RuntimeEnvironment.getApplication().getSharedPreferences("offline_recording_outbox", 0).edit().clear().commit();
    }

    @After public void cleanup() {
        RuntimeEnvironment.getApplication().getSharedPreferences("unattended_recording", 0).edit().clear().commit();
        CameraManager.setDeviceId(previousDevice);
        Constants.ACCESS_MODEL = previousMode;
        ReflectionHelpers.setStaticField(HotmeApplication.class, "smContext", previousContext);
    }

    @Test public void manualRecordExplicitlySendsZeroAndStillQueriesCamera() {
        try (var controller = Robolectric.buildActivity(AddRecordTaskActivity.class).create()) {
            AddRecordTaskActivity activity = controller.get();
            ((TextView) activity.findViewById(R.id.tv_game_name)).setText("手动录制");
            Device.connected = true;
            activity.findViewById(R.id.btn_record).performClick();
            assertEquals(List.of("record_status"), Device.commands);
            RecordStatusResultBean status = new RecordStatusResultBean();
            status.setR("ok");
            status.recordingProtocolVersion = 3;
            activity.onRecordVideoStatusUpdate(status);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals("0", Http.params.get("autoPushStream"));
        }
    }

    @Test public void remoteUnattendedRecordingUsesPlatformField() {
        try (var controller = Robolectric.buildActivity(AddRecordTaskActivity.class).create()) {
            AddRecordTaskActivity activity = controller.get();
            ((TextView) activity.findViewById(R.id.tv_game_name)).setText("平台录制");
            ((SwitchButtonO) activity.findViewById(R.id.switch_unattended)).setChecked(true);
            Device.connected = true;
            activity.findViewById(R.id.btn_record).performClick();
            RecordStatusResultBean status = new RecordStatusResultBean(); status.setR("ok");
            status.recordingProtocolVersion = 3;
            activity.onRecordVideoStatusUpdate(status);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals("1", Http.params.get("autoPushStream"));
            assertNull(UnattendedRecordService.current(activity));
        }
    }

    @Test public void localRecordingOffersPhoneOwnedUnattendedMode() {
        Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_LOCAL;
        try (var controller = Robolectric.buildActivity(AddRecordTaskActivity.class).create()) {
            AddRecordTaskActivity activity = controller.get();
            SwitchButtonO toggle = activity.findViewById(R.id.switch_unattended);
            assertFalse(toggle.isChecked()); assertTrue(toggle.isEnabled());
            toggle.setChecked(true);
            assertEquals(activity.getString(R.string.start_record), ((TextView) activity.findViewById(R.id.btn_record)).getText().toString());
            assertNull(Http.params);
        }
    }

    @Test public void existingCameraArmsWindowBeforeTheFirstStartAttempt() {
        Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_LOCAL; Device.connected = true;
        try (var controller = Robolectric.buildActivity(AddRecordTaskActivity.class).create()) {
            AddRecordTaskActivity activity = controller.get();
            ((TextView) activity.findViewById(R.id.tv_game_name)).setText("无人值守旧相机");
            ((SwitchButtonO) activity.findViewById(R.id.switch_unattended)).setChecked(true);
            assertEquals(activity.getString(R.string.start_record), ((TextView) activity.findViewById(R.id.btn_record)).getText().toString());
            activity.findViewById(R.id.btn_record).performClick();
            RecordStatusResultBean status = new RecordStatusResultBean(); status.setR("ok");
            activity.onRecordVideoStatusUpdate(status);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals(List.of("record_status"), Device.commands);
            UnattendedRecordTask task = UnattendedRecordService.current(activity);
            assertNotNull(task); assertFalse(task.localAuthorized);
            assertFalse(task.hasRecorded);
            assertEquals(LocalRecordingWindow.DEFAULT_DURATION_MS, task.endAt - task.startAt);
            assertTrue(activity.isFinishing()); assertNull(Http.params);
            String request = new com.google.gson.Gson().toJson(task.request);
            assertFalse(request.contains("unattended_resume")); assertFalse(request.contains("unattended_stop_at"));
        }
    }

    @Test public void foregroundTimerStartsImmediatelyAndRetriesInitialFailureAndInterruption() {
        Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_LOCAL; Device.connected = true;
        com.xh.hotme.record.bean.RecordInfoBean info = new com.xh.hotme.record.bean.RecordInfoBean("普通录制", 4096);
        info.live_cid = "our-match";
        UnattendedRecordTask task = UnattendedRecordTask.fromRecording(info, false);
        assertNull(UnattendedRecordService.schedule(RuntimeEnvironment.getApplication(), task));
        var service = service();
        try {
            service.get().onStartCommand(null, 0, 1); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals(List.of("record_status"), Device.commands);
            RecordStatusResultBean idle = new RecordStatusResultBean(); idle.setR("ok");
            service.get().onRecordVideoStatusUpdate(idle); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals(List.of("record_status", "record_start"), Device.commands);
            assertNotNull(Device.start); assertNull(Http.params);
            assertEquals(0, Device.start.getAppend());
            assertEquals(task.endAt, Device.start.record_until);
            // No success notification: the initial failure still has a saved task and retries.
            service.get().lowStorage(new com.xh.hotme.bluetooth.event.RecordStoppedOnLowStorageEvent());
            assertNotNull(UnattendedRecordService.current(service.get()));
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(30));
            service.get().onRecordVideoStatusUpdate(idle); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals(List.of("record_status", "record_start", "record_status", "record_start"), Device.commands);
            assertEquals(0, Device.start.getAppend());
            service.get().onRecordVideoStart(); Shadows.shadowOf(Looper.getMainLooper()).idle();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(30));
            service.get().onRecordVideoStatusUpdate(idle); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals(1, Device.start.getAppend());
            // A normal stop removes the task immediately, even before Service destruction.
            UnattendedRecordService.finishForDevice(service.get());
            assertNull(UnattendedRecordService.current(service.get()));
            int commandCount = Device.commands.size();
            service.get().onRecordVideoStatusUpdate(idle);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(30));
            assertEquals(commandCount, Device.commands.size());
        } finally { service.destroy(); }
    }

    @Test public void futureWindowWaitsAndStopsAtEndWithoutAnotherThirtySecondDelay() {
        Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_LOCAL; Device.connected = true;
        long now = System.currentTimeMillis();
        var info = new com.xh.hotme.record.bean.RecordInfoBean("定时录制", 4096);
        var task = UnattendedRecordTask.fromRecording(info, false, LocalRecordingWindow.resolve(now + 10_000L, now + 15_000L, now));
        assertNull(UnattendedRecordService.schedule(RuntimeEnvironment.getApplication(), task));
        var service = service();
        try {
            service.get().onStartCommand(null, 0, 1); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertTrue(Device.commands.isEmpty());
            var notifications = service.get().getSystemService(android.app.NotificationManager.class);
            assertTrue(notifications.getActiveNotifications()[0].getNotification().extras
                    .getCharSequence(android.app.Notification.EXTRA_TEXT).toString().contains("00:00:10"));
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(5));
            assertTrue(notifications.getActiveNotifications()[0].getNotification().extras
                    .getCharSequence(android.app.Notification.EXTRA_TEXT).toString().contains("00:00:05"));
            assertTrue(Device.commands.isEmpty());
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(5));
            assertEquals(List.of("record_status"), Device.commands);
            RecordStatusResultBean idle = new RecordStatusResultBean(); idle.setR("ok");
            service.get().onRecordVideoStatusUpdate(idle); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals(List.of("record_status", "record_start"), Device.commands);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(5));
            assertEquals(List.of("record_status", "record_start", "record_stop", "record_status"), Device.commands);
            service.get().onRecordVideoStatusUpdate(idle); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNull(UnattendedRecordService.current(service.get()));
        } finally { service.destroy(); }
    }

    @Test public void selectedTimesAreSavedAndInvalidWindowDoesNotQueryOrStart() {
        Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_LOCAL; Device.connected = true;
        try (var controller = Robolectric.buildActivity(AddRecordTaskActivity.class).create()) {
            AddRecordTaskActivity activity = controller.get();
            ((TextView) activity.findViewById(R.id.tv_game_name)).setText("预约录制");
            ((SwitchButtonO) activity.findViewById(R.id.switch_unattended)).setChecked(true);
            long start = System.currentTimeMillis() + 60_000L;
            ReflectionHelpers.setField(activity, "_selectedStartAt", start);
            ReflectionHelpers.setField(activity, "_selectedEndAt", start - 1L);
            activity.findViewById(R.id.btn_record).performClick();
            assertTrue(Device.commands.isEmpty()); assertNull(UnattendedRecordService.current(activity));
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1_001));
            ReflectionHelpers.setField(activity, "_selectedEndAt", start + 120_000L);
            activity.findViewById(R.id.btn_record).performClick();
            RecordStatusResultBean status = new RecordStatusResultBean(); status.setR("ok");
            status.recordingProtocolVersion = 5; status.deviceState = "idle"; status.localControlSupported = 1;
            activity.onRecordVideoStatusUpdate(status); Shadows.shadowOf(Looper.getMainLooper()).idle();
            var task = UnattendedRecordService.current(activity);
            assertNotNull(task); assertTrue(task.localAuthorized);
            assertEquals(start, task.startAt); assertEquals(start + 120_000L, task.endAt);
            assertEquals(List.of("record_status"), Device.commands); assertNull(Http.params);
        }
    }

    @Test public void expiredWindowIsRejectedAndTimeFieldsAreOnlyVisibleLocally() {
        try (var controller = Robolectric.buildActivity(AddRecordTaskActivity.class).create()) {
            assertEquals(View.GONE, controller.get().findViewById(R.id.local_record_window).getVisibility());
        }
        Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_LOCAL; Device.connected = true;
        try (var controller = Robolectric.buildActivity(AddRecordTaskActivity.class).create()) {
            AddRecordTaskActivity activity = controller.get();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.local_record_window).getVisibility());
            ((TextView) activity.findViewById(R.id.tv_game_name)).setText("过期录制");
            ((SwitchButtonO) activity.findViewById(R.id.switch_unattended)).setChecked(true);
            ReflectionHelpers.setField(activity, "_selectedStartAt", System.currentTimeMillis() - 120_000L);
            ReflectionHelpers.setField(activity, "_selectedEndAt", System.currentTimeMillis() - 60_000L);
            activity.findViewById(R.id.btn_record).performClick();
            assertTrue(Device.commands.isEmpty()); assertNull(UnattendedRecordService.current(activity));
        }
    }

    @Test public void authorizedStartCarriesDeadlineAndEndCancelsAnUnconfirmedStart() {
        Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_LOCAL; Device.connected = true;
        long now = System.currentTimeMillis();
        var info = new com.xh.hotme.record.bean.RecordInfoBean("离线定时录制", 4096);
        var task = UnattendedRecordTask.fromRecording(info, true, LocalRecordingWindow.resolve(now, now + 5_000L, now));
        assertNull(UnattendedRecordService.schedule(RuntimeEnvironment.getApplication(), task));
        var service = service();
        try {
            service.get().onStartCommand(null, 0, 1); Shadows.shadowOf(Looper.getMainLooper()).idle();
            RecordStatusResultBean idle = new RecordStatusResultBean(); idle.setR("ok"); idle.deviceState = "idle";
            service.get().onRecordVideoStatusUpdate(idle); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(Device.start); assertTrue(Device.start.local_control);
            assertNotNull(Device.start.offline_permit); assertEquals(task.endAt, Device.start.record_until);
            assertEquals(0, Device.start.stop_at); // The camera still assigns its control deadline.
            var starting = new RecordStatusResultBean(); starting.setR("ok"); starting.deviceState = "starting";
            starting.localControl = 1; starting.competitionId = "device-match"; starting.sessionId = "device-session";
            starting.operationId = Device.start.operation_id;
            BluetoothHandle.onRecordStatusNotify(starting); Shadows.shadowOf(Looper.getMainLooper()).idle();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(5));
            assertTrue(Device.commands.contains("record_stop"));
            BluetoothHandle.onRecordStatusNotify(idle); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNull(UnattendedRecordService.current(service.get()));
            long starts = Device.commands.stream().filter("record_start"::equals).count();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(30));
            assertEquals(starts, Device.commands.stream().filter("record_start"::equals).count());
        } finally { service.destroy(); }
    }

    private static ServiceController<UnattendedRecordService> service() {
        var controller = Robolectric.buildService(UnattendedRecordService.class);
        long wall = System.currentTimeMillis(), uptime = android.os.SystemClock.uptimeMillis();
        ReflectionHelpers.setField(controller.get(), "clock", (LongSupplier)
                () -> wall + android.os.SystemClock.uptimeMillis() - uptime);
        return controller.create();
    }

    @Test public void liveEditRestoresAndSavesControlWithoutStartingCamera() {
        HmCompetitionLive bean = liveBean();
        Intent intent = new Intent(RuntimeEnvironment.getApplication(), AddLiveTaskActivity.class)
                .putExtra(AddLiveTaskActivity.EXTRA_EDIT_BEAN, bean);
        try (var controller = Robolectric.buildActivity(AddLiveTaskActivity.class, intent).create()) {
            AddLiveTaskActivity activity = controller.get();
            SwitchButtonO toggle = activity.findViewById(R.id.switch_unattended);
            assertTrue(toggle.isChecked());
            activity.findViewById(R.id.tv_ok).performClick();
            assertEquals(SdkApi.competitionLive_update(), Http.url);
            assertEquals("1", Http.params.get("autoPushStream"));
            saved();
            assertTrue(activity.isFinishing());
            assertTrue(Device.commands.isEmpty());
        }
    }

    @Test public void legacyLiveWithoutFieldDefaultsOffAndCanSaveExplicitZero() {
        HmCompetitionLive bean = liveBean();
        bean.autoPushStream = null;
        Intent intent = new Intent(RuntimeEnvironment.getApplication(), AddLiveTaskActivity.class)
                .putExtra(AddLiveTaskActivity.EXTRA_EDIT_BEAN, bean);
        try (var controller = Robolectric.buildActivity(AddLiveTaskActivity.class, intent).create()) {
            AddLiveTaskActivity activity = controller.get();
            assertFalse(((SwitchButtonO) activity.findViewById(R.id.switch_unattended)).isChecked());
            activity.findViewById(R.id.tv_ok).performClick();
            assertEquals("0", Http.params.get("autoPushStream"));
            ((OkHttpCallbackDecode<?>) Http.callback).onFailure("500", "保存失败");
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(activity.isFinishing());
            assertTrue(activity.findViewById(R.id.switch_unattended).isEnabled());
        }
    }

    private static HmCompetitionLive liveBean() {
        HmCompetitionLive bean = new HmCompetitionLive();
        bean.competitionId = "existing-live";
        bean.competitionName = "托管直播";
        bean.motionType = "1";
        bean.placeType = "2";
        bean.cityCode = "440300000000";
        bean.provinceCode = "440000000000";
        bean.cityCodeDisplay = "深圳市";
        bean.provinceCodeDisplay = "广东省";
        bean.competitionTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(System.currentTimeMillis() + 60 * 60 * 1000));
        bean.pushStreamWxMiniApp = 1;
        bean.autoPushStream = 1;
        return bean;
    }

    @SuppressWarnings("unchecked")
    private static void saved() {
        LiveTaskBean response = new LiveTaskBean();
        response.data = liveBean();
        ((OkHttpCallbackDecode<LiveTaskBean>) Http.callback).onDataSuccess(response);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Implements(OkHttpUtil.class)
    public static class Http {
        static String url;
        static HashMap<String, String> params;
        static Callback callback;
        @Implementation public static void postData(String target, HashMap<String, String> data,
                Map<String, String> headers, Callback result) {
            url = target;
            params = new HashMap<>(data);
            callback = result;
        }
    }

    @Implements(DeviceApiManager.class)
    public static class Device {
        static final List<String> commands = new ArrayList<>();
        static boolean connected;
        static RecordStatusRequestBean query;
        static RecordStartRequestBean start;
        @Implementation public static boolean isConnected() { return connected; }
        @Implementation public static boolean sendCmdSimpleObj(BaseBluetoothBean command) {
            commands.add(command.c);
            if (command instanceof RecordStatusRequestBean) query = (RecordStatusRequestBean) command;
            if (command instanceof RecordStartRequestBean) start = (RecordStartRequestBean) command;
            return true;
        }
    }

    @Implements(BluetoothManager.class)
    public static class Bluetooth {
        @Implementation public boolean sendCmdSimpleObj(BaseBluetoothBean command) {
            return Device.sendCmdSimpleObj(command);
        }
    }

    @Implements(DailyAccessManager.class)
    public static class Daily {
        @Implementation public static boolean ready() { return true; }
        @Implementation public static DailyAccessManager.DeviceAccess device(String id) {
            DailyAccessManager.DeviceAccess access = new DailyAccessManager.DeviceAccess();
            access.deviceId = id; access.recordingAllowed = true; access.permit = new DailyAccessManager.OfflinePermit();
            String claim = "{\"permit_id\":\"test-permit\",\"start_before\":" + (System.currentTimeMillis()+86400000L)
                    + ",\"stop_before\":" + (System.currentTimeMillis()+86400000L)
                    + ",\"max_session_ms\":7200000,\"budget_ms\":7200000}";
            access.permit.payload = android.util.Base64.encodeToString(claim.getBytes(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP);
            access.permit.signature = "test-signature";
            return access;
        }
    }
}
