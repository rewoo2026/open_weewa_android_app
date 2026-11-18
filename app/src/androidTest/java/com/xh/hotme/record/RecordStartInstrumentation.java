package com.xh.hotme.record;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.EditText;

import com.alibaba.fastjson.JSONObject;
import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.bean.RecordStatusRequestBean;
import com.xh.hotme.bluetooth.bean.LocalRecordStopRequestBean;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.CameraMonitor;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.http.RecordingApi;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CommonResponse;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.widget.ModalDialog;
import com.xh.hotme.record.bean.LocalRecordingGrant;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.bluetooth.bean.RecordStartRequestBean;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Mocked UI regression by default; -e live true records a short labelled test. */
public class RecordStartInstrumentation extends Instrumentation implements IRecordControlListener {
    private Bundle args;
    private AddRecordTaskActivity page;
    private final AtomicReference<RecordStatusResultBean> status = new AtomicReference<>();
    private final AtomicInteger starts = new AtomicInteger(), queries = new AtomicInteger(), cancels = new AtomicInteger();
    private volatile boolean confirm;
    private volatile boolean reject = true;
    private String taskId;
    private RecordActivity recorderPage;

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); args = arguments; start(); }
    @Override public void onStart() {
        Bundle result = new Bundle(); int outcome = Activity.RESULT_OK;
        boolean live = "true".equals(args.getString("live"));
        int originalMode = Constants.ACCESS_MODEL;
        boolean originalOnline = CameraMonitor.getImpl().isOnline();
        String originalHistory = live ? null : AppFileUtil.loadStringFromFile(AppFileUtil.COMPETITION_FILE);
        Field client = null; Object originalClient = null;
        try {
            await(() -> com.xh.hotme.HotmeApplication.getInstance() != null, 10000, "Application did not initialize");
            onMain(() -> { });
            require(LoginManager.isLogon(), "Sign in before testing recording");
            String address = null;
            for (Map.Entry<String, ?> entry : getTargetContext().getSharedPreferences("com.xh.hotme", Context.MODE_PRIVATE).getAll().entrySet()) {
                if (entry.getKey().startsWith("device_") && entry.getValue() instanceof String) {
                    try {
                        if ("zzg20230bde63f46f0".equals(JSONObject.parseObject((String) entry.getValue()).getString("device_id"))) {
                            String candidate = entry.getKey().substring(7);
                            if (candidate.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")) address = candidate;
                        }
                    } catch (Exception ignored) { }
                }
            }
            require(address != null, "Test camera has no cached Bluetooth address");
            final String cameraAddress = address;
            if (!live) {
                client = OkHttpUtil.class.getDeclaredField("CLIENT"); client.setAccessible(true);
                originalClient = client.get(null);
                client.set(null, new OkHttpClient.Builder().addInterceptor(chain -> {
                    String path = chain.request().url().encodedPath(), body = "{\"code\":500,\"msg\":\"test\"}";
                    if (path.endsWith("/competitionRecord/add") || path.endsWith("/competitionRecord/update")) body = "{\"code\":200,\"data\":{\"competitionId\":\"mock-record\"}}";
                    else if (path.contains("/competitionRecord/startRecord/")) {
                        starts.incrementAndGet();
                        body = reject ? "{\"code\":501,\"msg\":\"录制次数不足\",\"data\":null}"
                                : "{\"code\":202,\"data\":null}";
                    } else if (path.contains("/competitionRecord/stopRecord/")) {
                        cancels.incrementAndGet();
                        body = "{\"code\":200,\"data\":null}";
                    } else if (path.contains("/competitionRecord/find/")) {
                        queries.incrementAndGet();
                        body = "{\"code\":200,\"data\":{\"competitionId\":\"mock-record\",\"competitionStatus\":1,\"recordingExecutionStatus\":\""
                                + (confirm ? "recording" : "starting") + "\"}}";
                    }
                    return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                            .code(200).message("OK").body(ResponseBody.create(okhttp3.MediaType.parse("application/json"), body)).build();
                }).build());
            }
            Constants.ACCESS_MODEL = live ? Constants.ACCESS_MODEL_LOCAL : Constants.ACCESS_MODEL_REMOTE;
            onMain(() -> {
                CameraManager.newCamera(cameraAddress);
                BluetoothHandle.addPlayNotifyListener(this);
                if (live) BluetoothManager.getImpl().connectToBle(new Device(cameraAddress, 0, "weewa520"));
            });
            if (live) {
                await(() -> BluetoothManager.getImpl().isConnected(), 30000, "Bluetooth did not connect");
                RecordStatusResultBean before = queryStatus();
                require(before.getStatus() <= 0, "Camera is recording; this test must not interrupt it");
                require(before.recordingProtocolVersion >= 4 && before.localControlSupported == 1, "Expected a local-control protocol 4 test camera");
            }
            if (live && args.getString("grant") != null) {
                runIssuedGrant(result);
            } else {
            ActivityMonitor main = addMonitor("com.xh.hotme.main.MainActivity", null, false);
            try (ParcelFileDescriptor command = getUiAutomation().executeShellCommand("am start -n com.xh.hotme/.main.MainActivity");
                 FileInputStream output = new FileInputStream(command.getFileDescriptor())) {
                byte[] buffer = new byte[1024]; while (output.read(buffer) != -1) { }
                require(main.waitForActivityWithTimeout(10000) != null, "Cannot foreground app");
            } finally { removeMonitor(main); }
            Intent intent = new Intent(getTargetContext(), AddRecordTaskActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            page = (AddRecordTaskActivity) startActivitySync(intent);
            String name = "录制复测" + new SimpleDateFormat("MMddHHmmss").format(new Date());
            onMain(() -> {
                ((EditText) page.findViewById(R.id.tv_game_name)).setText(name);
                ((EditText) page.findViewById(R.id.home_team_name)).setText("主队");
                ((EditText) page.findViewById(R.id.guest_team_name)).setText("客队");
                if (!live) CameraMonitor.getImpl().setOnline(true);
                page.findViewById(R.id.btn_record).performClick();
                if (!live) injectStatus();
            });
            if (!live) {
                await(() -> starts.get() == 1 && stage() == 0, 15000, "Rejected start did not recover");
                onMain(() -> {
                    require(!page.isFinishing(), "Rejected start closed the form");
                    require(page.findViewById(R.id.btn_record).isEnabled(), "Rejected start left button disabled");
                    require(name.contentEquals(((EditText) page.findViewById(R.id.tv_game_name)).getText()), "Form contents were lost");
                });
                reject = false;
                Thread.sleep(600);
                onMain(() -> {
                    CameraMonitor.getImpl().setOnline(true);
                    page.findViewById(R.id.btn_record).performClick();
                    injectStatus();
                    page.findViewById(R.id.btn_record).performClick();
                });
                await(() -> queries.get() >= 1, 15000, "Pending start was not queried");
                onMain(() -> require(!page.isFinishing(), "Pending start incorrectly reported success"));
                require(starts.get() == 2, "Duplicate click submitted another start");
                onMain(() -> page._connectingDialog.findViewById(R.id.iv_close).performClick());
                await(() -> cancels.get() == 1 && stage() == 0, 10000, "Cancel did not stop the pending server task");
                onMain(() -> require(page.findViewById(R.id.btn_record).isEnabled(), "Cancel left button disabled"));
                Thread.sleep(600);
                int previousQueries = queries.get();
                onMain(() -> {
                    CameraMonitor.getImpl().setOnline(true);
                    page.findViewById(R.id.btn_record).performClick();
                    injectStatus();
                });
                await(() -> queries.get() > previousQueries, 15000, "Retry after cancel was not queried");
                confirm = true;
                await(page::isFinishing, 10000, "Confirmed start did not close form");
                require(starts.get() == 3 && cancels.get() == 1, "Success unexpectedly stopped the recording");
                result.putString("stream", "\nPASS: protocol 3 button path; rejected request recovers with input intact; 202 waits; duplicate suppressed; cancel stops pending task; retry confirms and closes form\n");
            } else {
                await(() -> { if (!page.competitionId.isEmpty()) taskId = page.competitionId; return page.isFinishing(); },
                        190000, "Recording start not confirmed; stage=" + stage());
                RecordStatusResultBean actual = queryStatus();
                require(actual.getStatus() == 1 && taskId != null && taskId.equals(actual.getLiveCId()), "Wrong recording task on camera");
                require(name.equals(actual.getVideoName()), "Camera recorded different metadata");
                int hold = Integer.parseInt(args.getString("holdSeconds", "8"));
                Thread.sleep(Math.min(120, Math.max(8, hold)) * 1000L);
                if ("true".equals(args.getString("uiStop"))) {
                    Intent recorderIntent = new Intent(getTargetContext(), RecordActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    recorderIntent.putExtra(Constants.REQUEST_DEVICE_INFO, GsonUtils.GsonString(CameraManager.getDeviceInfo()));
                    recorderIntent.putExtra(Constants.REQUEST_DEVICE, GsonUtils.GsonString(BluetoothManager.getConnectDevice()));
                    recorderPage = (RecordActivity) startActivitySync(recorderIntent);
                    // Feed the actual BLE response into the production view.
                    onMain(() -> recorderPage.onRecordVideoStatusUpdate(actual));
                    Thread.sleep(1500);
                    onMain(() -> recorderPage.findViewById(R.id.btn_recorder).performClick());
                    onMain(() -> {
                        try {
                            Field f = RecordActivity.class.getDeclaredField("_dialog"); f.setAccessible(true);
                            ModalDialog dialog = (ModalDialog) f.get(recorderPage);
                            require(dialog != null && dialog.isShowing(), "Stop button did not open confirmation without camera Wi-Fi");
                            dialog.findViewById(R.id.dlg_right_btn).performClick();
                        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
                    });
                    await(() -> queryStatus().getStatus() <= 0, 45000, "UI local stop not confirmed");
                    // Let all production status listeners consume the final BLE
                    // reply before this instrumentation closes their Activity.
                    Thread.sleep(2000);
                    AtomicReference<Boolean> ended = new AtomicReference<>(false);
                    await(() -> {
                        onMain(() -> RecordingApi.query(taskId, new IGenericApiCallback<com.xh.hotme.record.bean.HmCompetitionRecordBean>() {
                            public void onSuccess(com.xh.hotme.record.bean.HmCompetitionRecordBean record) { ended.set(record.competitionStatus == 2); }
                            public void onFail(String code, String message) { }
                            public void onFinish() { }
                        }));
                        Thread.sleep(1800);
                        return ended.get();
                    }, 30000, "UI stop was not synchronized to the server");
                }
                result.putString("stream", "\nPASS: actual button -> server authorization -> page closes -> camera recording; task=" + taskId + "\n");
            }
            }
        } catch (Throwable error) {
            outcome = Activity.RESULT_CANCELED;
            result.putString("stream", "\nFAIL: " + error + "; task=" + taskId + "\n");
        } finally {
            if (live && page != null && !page.competitionId.isEmpty()) {
                taskId = page.competitionId;
                try {
                    RecordStatusResultBean actual = queryStatus();
                    if (actual.getStatus() == 1) {
                        require(taskId.equals(actual.getLiveCId()), "Cleanup must not interrupt another task");
                        onMain(() -> require(BluetoothManager.getImpl().sendCmdSimpleObj(
                                new LocalRecordStopRequestBean(taskId, actual.sessionId)), "Cannot send local stop"));
                        await(() -> queryStatus().getStatus() <= 0, 45000, "Test recording is still active");
                        RecordStatusResultBean idle = queryStatus();
                        onMain(() -> LocalRecordingReports.enqueue(taskId, actual.sessionId, idle.deviceVersion, false));
                    }
                } catch (Throwable error) {
                    outcome = Activity.RESULT_CANCELED;
                    result.putString("stream", "\nFAIL: cleanup not confirmed; " + error + "; task=" + taskId + "\n");
                }
            }
            onMain(() -> {
                BluetoothHandle.removePlayNotifyListener(this);
                if (page != null && !page.isFinishing()) page.finish();
                if (recorderPage != null && !recorderPage.isFinishing()) recorderPage.finish();
            });
            Constants.ACCESS_MODEL = originalMode;
            CameraMonitor.getImpl().setOnline(originalOnline);
            if (!live) AppFileUtil.saveJson(originalHistory == null || originalHistory.isEmpty() ? "[]" : originalHistory,
                    AppFileUtil.COMPETITION_FILE);
            if (client != null && originalClient != null) try { client.set(null, originalClient); } catch (Exception ignored) { }
        }
        finish(outcome, result);
    }

    private void runIssuedGrant(Bundle result) throws Exception {
        LocalRecordingGrant grant = JSONObject.parseObject(args.getString("grant"), LocalRecordingGrant.class);
        RecordStatusResultBean before = queryStatus();
        require(grant.valid(grant.live_cid, before.deviceVersion), "Grant is not bound to the observed camera fence");
        RecordInfoBean info = new RecordInfoBean("LOCAL_ACCEPT_" + grant.live_cid, 4096);
        info.width=1920; info.height=1080; info.motionType=1; info.placeType=2; info.player_num=8;
        info.ctype=1; info.live_cid=grant.live_cid; info.avs_mbr=12288; info.live_mbr=4096; info.avsw=7600; info.avsh=2160;
        RecordStartRequestBean request=info.convertRecordStartRequestBean(); grant.apply(request);
        boolean leaveRunning="startOnly".equals(args.getString("mode"));
        require(!leaveRunning || grant.stop_at-System.currentTimeMillis() <= 120000, "Handoff recording needs a short bounded deadline");
        try {
            onMain(() -> require(BluetoothManager.getImpl().sendCmdSimpleObj(request), "Cannot relay issued grant"));
            await(() -> { RecordStatusResultBean s=queryStatus(); return s.getStatus()==1 && s.firstFrame==1 && grant.session_id.equals(s.sessionId); }, 80000, "Issued grant did not produce a first frame");
            RecordStatusResultBean actual=queryStatus();
            require(actual.localControl==1 && actual.deviceVersion==grant.device_version, "Camera did not accept exact server sequence");
            onMain(() -> LocalRecordingReports.enqueue(actual,true));
            if ("true".equals(args.getString("duplicate"))) {
                onMain(() -> BluetoothManager.getImpl().sendCmdSimpleObj(request));
                Thread.sleep(1800);
                RecordStatusResultBean same=queryStatus();
                require(same.deviceVersion==actual.deviceVersion && grant.session_id.equals(same.sessionId) && same.firstFrame==1, "Duplicate start changed its session or fence");
            }
            if (args.getString("oldSession") != null) {
                onMain(() -> BluetoothManager.getImpl().sendCmdSimpleObj(new LocalRecordStopRequestBean(args.getString("oldId"), args.getString("oldSession"))));
                Thread.sleep(1800);
                RecordStatusResultBean same=queryStatus();
                require(same.getStatus()==1 && same.deviceVersion==actual.deviceVersion && grant.session_id.equals(same.sessionId), "Old local stop affected new session");
            }
            if (leaveRunning) {
                result.putString("stream", "\nPASS: bounded recording handed to host; " + JSONObject.toJSONString(actual) + "\n");
                return;
            }
            if ("auto".equals(args.getString("mode"))) {
                require(grant.stop_at-System.currentTimeMillis() < 150000, "Automatic deadline test is not bounded");
                await(() -> queryStatus().getStatus()<=0, Math.max(10000,grant.stop_at-System.currentTimeMillis()+30000), "Camera did not enforce offline quota deadline");
            } else {
                Thread.sleep(Integer.parseInt(args.getString("holdSeconds", "8"))*1000L);
                onMain(() -> require(BluetoothManager.getImpl().sendCmdSimpleObj(new LocalRecordStopRequestBean(grant.live_cid,grant.session_id)), "Cannot stop local session"));
                await(() -> queryStatus().getStatus()<=0,45000,"Local stop was not confirmed");
            }
            RecordStatusResultBean idle=queryStatus();
            onMain(() -> LocalRecordingReports.enqueue(grant.live_cid,grant.session_id,idle.deviceVersion,false));
            result.putString("stream", "\nPASS: exact issued grant, duplicate/old-session guards, local stop or offline deadline; " + JSONObject.toJSONString(idle) + "\n");
        } finally {
            if (!leaveRunning) {
                RecordStatusResultBean current=queryStatus();
                if (current.getStatus()==1 && grant.session_id.equals(current.sessionId)) {
                    onMain(() -> BluetoothManager.getImpl().sendCmdSimpleObj(new LocalRecordStopRequestBean(grant.live_cid,grant.session_id)));
                    await(() -> queryStatus().getStatus()<=0,45000,"Grant test cleanup did not stop its own recording");
                }
            }
        }
    }

    private RecordStatusResultBean queryStatus() throws Exception {
        status.set(null);
        // Connection can precede notifications; retry this read-only query.
        long deadline = System.currentTimeMillis() + 20000;
        while (status.get() == null && System.currentTimeMillis() < deadline) {
            onMain(() -> DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean()));
            long attempt = System.currentTimeMillis() + 1500;
            while (status.get() == null && System.currentTimeMillis() < attempt) Thread.sleep(100);
        }
        require(status.get() != null, "No camera status reply");
        return status.get();
    }
    private void injectStatus() {
        BluetoothHandle.postRecordStatus(JSONObject.parseObject(
                "{\"c\":\"record_status\",\"r\":\"ok\",\"st\":0,\"recordingProtocolVersion\":3}"));
    }
    private int stage() {
        try { Field f = AddRecordTaskActivity.class.getDeclaredField("_recordStartStage"); f.setAccessible(true); return f.getInt(page); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private interface Check { boolean get() throws Exception; }
    private void await(Check check, long duration, String message) throws Exception {
        long deadline = System.currentTimeMillis() + duration;
        while (System.currentTimeMillis() < deadline) { if (check.get()) return; Thread.sleep(100); }
        throw new AssertionError(message);
    }
    private void onMain(Runnable action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        runOnMainSync(() -> { try { action.run(); } catch (Throwable e) { failure.set(e); } });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public void onRecordVideoStart() { }
    public void onRecordVideoStop() { }
    public void onRecordException(String message) { }
    public void onRecordVideoStatusUpdate(RecordStatusResultBean reply) { if (reply != null && "ok".equals(reply.getR())) status.set(reply); }
}
