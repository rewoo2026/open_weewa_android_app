package com.xh.hotme.record;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.text.TextUtils;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.google.gson.Gson;
import com.xh.hotme.R;
import com.xh.hotme.account.DailyAccessManager;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.bean.RecordStartRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStatusRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStopRequestBean;
import com.xh.hotme.bluetooth.bean.LocalRecordStopRequestBean;
import com.xh.hotme.bluetooth.event.RecordStartEvent;
import com.xh.hotme.bluetooth.event.RecordStoppedOnLowStorageEvent;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.environment.RuntimeEnvironment;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.http.CompetitionManager;
import com.xh.hotme.record.bean.CameraRecordInfoBean;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.ScoreCenterUtil;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.LongSupplier;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/** Phone-only watchdog for local recording. Uses existing camera commands and start flows. */
public class UnattendedRecordService extends Service implements IRecordControlListener {
    private static final String CHANNEL = "unattended_recording", END = "end_unattended_recording";
    private static final int NOTIFICATION = 7301;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LongSupplier clock = System::currentTimeMillis;
    private UnattendedRecordTask task;
    private UnattendedRecordController controller;
    private PowerManager.WakeLock wakeLock;
    private LocalRecordStarter localStarter;
    private RecordStatusResultBean lastStatus;
    private long observedDeviceVersion;
    private boolean starting, cancellingStart, ending;
    private String message = "正在恢复无人值守任务";

    private static android.content.SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("unattended_recording", MODE_PRIVATE);
    }
    public static UnattendedRecordTask current(Context context) {
        try {
            UnattendedRecordTask task = new Gson().fromJson(prefs(context).getString("task", ""), UnattendedRecordTask.class);
            if (task == null || TextUtils.isEmpty(task.id) || task.deviceId == null || task.userId == null
                    || task.environment == null || task.request == null) return null;
            // Older tasks were only saved after recording had already started.
            if (task.endAt == 0) {
                task.startAt = System.currentTimeMillis();
                task.endAt = task.startAt + LocalRecordingWindow.DEFAULT_DURATION_MS;
                task.hasRecorded = true;
                if (!prefs(context).edit().putString("task", new Gson().toJson(task)).commit()) return null;
            }
            return task.endAt > task.startAt ? task : null;
        }
        catch (RuntimeException e) { return null; }
    }
    public static String schedule(Context context, UnattendedRecordTask task) {
        if (task == null || task.endAt <= task.startAt || task.endAt <= System.currentTimeMillis()) return context.getString(R.string.local_record_window_expired);
        if (current(context) != null) return "已有无人值守任务，请先关闭后再创建";
        if (!prefs(context).edit().putString("task", new Gson().toJson(task)).commit()) return "无法保存无人值守设置";
        try { ContextCompat.startForegroundService(context, new Intent(context, UnattendedRecordService.class)); }
        catch (RuntimeException e) { prefs(context).edit().remove("task").commit(); return "无人值守后台服务未能启动"; }
        return null;
    }
    /** The normal stop button cancels the timer before issuing its existing stop request. */
    public static void finishForDevice(Context context) {
        UnattendedRecordTask task = current(context);
        if (task != null && task.deviceId.equals(CameraManager.getDeviceId())) {
            prefs(context).edit().remove("task").commit();
            context.stopService(new Intent(context, UnattendedRecordService.class));
        }
    }
    public static void restore(Context context) {
        UnattendedRecordTask task = current(context);
        if (task != null && task.userId.equals(LoginManager.getUserId())
                && task.environment.equals(RuntimeEnvironment.cacheScope())) {
            try { ContextCompat.startForegroundService(context, new Intent(context, UnattendedRecordService.class)); }
            catch (RuntimeException ignored) { }
        }
    }
    @Override public void onCreate() {
        super.onCreate();
        getSystemService(NotificationManager.class).createNotificationChannel(
                new NotificationChannel(CHANNEL, "无人值守", NotificationManager.IMPORTANCE_LOW));
        startForeground(NOTIFICATION, notification());
        BluetoothHandle.addPlayNotifyListener(this);
        EventBus.getDefault().register(this);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && END.equals(intent.getAction())) {
            prefs(this).edit().remove("task").commit(); stopSelf(); return START_NOT_STICKY;
        }
        UnattendedRecordTask saved = current(this);
        if (saved == null) { stopSelf(); return START_NOT_STICKY; }
        if (task != null && task.id.equals(saved.id)) return START_STICKY;
        if (controller != null) controller.cancel();
        if (localStarter != null) localStarter.cancel();
        task = saved;
        localStarter = null;
        lastStatus = null;
        starting = cancellingStart = ending = false;
        controller = new UnattendedRecordController(new UnattendedRecordController.Port() {
            public boolean connected() { return selected() && DeviceApiManager.isConnected(); }
            public boolean query() { return DeviceApiManager.sendCmdSimpleObj(new RecordStatusRequestBean()); }
            public boolean start() { return restartRecording(); }
            public boolean stop() { return stopAtDeadline(); }
            public void finished() {
                UnattendedRecordTask current = current(UnattendedRecordService.this);
                if (current != null && task.id.equals(current.id)) prefs(UnattendedRecordService.this).edit().remove("task").commit();
                stopSelf();
            }
        }, task.startAt, task.endAt);
        if (wakeLock == null) {
            wakeLock = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hotme:unattended-recording");
            wakeLock.acquire();
        }
        handler.removeCallbacks(tick);
        handler.post(tick);
        return START_STICKY;
    }
    private boolean selected() {
        UnattendedRecordTask saved = current(this);
        return task != null && saved != null && task.id.equals(saved.id)
                && task.isSelectedDevice();
    }
    private final Runnable tick = new Runnable() {
        public void run() {
            if (controller == null) return;
            if (current(UnattendedRecordService.this) == null) { stopSelf(); return; }
            long now = clock.getAsLong();
            if (now < task.startAt && !ending) {
                showState(getString(R.string.unattended_waiting_countdown, UnattendedRecordTask.remainingTime(task.startAt - now)));
            }
            controller.tick(now);
            handler.postDelayed(this, controller.nextDelay(clock.getAsLong()));
        }
    };
    private boolean restartRecording() {
        long now = clock.getAsLong();
        if (!selected() || starting || ending || now < task.startAt || now >= task.endAt) return false;
        RecordStartRequestBean request = new Gson().fromJson(new Gson().toJson(task.request), RecordStartRequestBean.class);
        if (task.hasRecorded) request.setAppend(1);
        request.record_until = task.endAt;
        request.setVt(now);
        showState("正在启动录制，结束时间：" + new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(task.endAt)));
        if (!task.localAuthorized) return DeviceApiManager.sendCmdSimpleObj(request);
        DailyAccessManager.DeviceAccess access = DailyAccessManager.device(task.deviceId);
        if (access == null || access.permit == null || !access.recordingAllowed) { showState("今日录制权限不可用，请联网同步后重试"); return false; }
        starting = true;
        UnattendedRecordTask owned = task;
        // The same authorized start path used by the existing Start Recording button.
        localStarter = LocalRecordStarter.createOffline(request, new LocalRecordStarter.Listener() {
            public void onStarted() {
                if (task != owned) return;
                starting = false;
                localStarter = null;
                if (!selected()) return;
                if (clock.getAsLong() >= task.endAt) { controller.tick(clock.getAsLong()); return; }
                if (!ending) recordingStarted(lastStatus == null ? task.info : lastStatus.convertRecordInfoBean());
            }
            public void onFailure(String error) {
                if (task != owned) return;
                starting = false; localStarter = null;
                if (!ending) showState(error);
            }
            public void onWarning(String error) { if (task == owned && !ending) showState(error); }
        }, access);
        localStarter.start("", observedDeviceVersion);
        return true;
    }

    private boolean stopAtDeadline() {
        ending = true;
        showState("已到结束时间，正在停止录制；请保持相机连接");
        if (localStarter != null && starting && !cancellingStart) {
            cancellingStart = true;
            UnattendedRecordTask owned = task;
            localStarter.cancel(() -> {
                if (task == owned) { starting = false; cancellingStart = false; localStarter = null; }
            });
        }
        if (!selected() || !DeviceApiManager.isConnected()) return false;
        if (!task.localAuthorized) return DeviceApiManager.sendCmdSimpleObj(new RecordStopRequestBean());
        if (lastStatus == null || !"ok".equalsIgnoreCase(lastStatus.getR())) return false;
        String id = TextUtils.isEmpty(lastStatus.competitionId) ? lastStatus.getLiveCId() : lastStatus.competitionId;
        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(lastStatus.sessionId)) return false;
        return DeviceApiManager.sendCmdSimpleObj(new LocalRecordStopRequestBean(id, lastStatus.sessionId));
    }

    private void recordingStarted(RecordInfoBean info) {
        if (!task.hasRecorded) {
            task.hasRecorded = true;
            prefs(this).edit().putString("task", new Gson().toJson(task)).commit();
            if (info != null) {
                CameraRecordInfoBean history = CameraRecordInfoBean.newInstance(info);
                history.deviceId = CameraManager.getDeviceBluetoothId();
                CompetitionManager.addCompetition(this, history);
                ScoreCenterUtil.removeCustomDotRecord();
            }
        }
        if (info != null) EventBus.getDefault().post(new RecordStartEvent(info));
        showState("录制中，结束时间：" + new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(task.endAt)));
    }
    @Override public void onRecordVideoStatusUpdate(RecordStatusResultBean status) {
        handler.post(() -> {
            if (controller == null || !selected()) return;
            lastStatus = status;
            if (status != null) observedDeviceVersion = status.deviceVersion;
            long now = clock.getAsLong();
            if (!starting && !ending && now >= task.startAt && now < task.endAt && !task.hasRecorded && status != null
                    && "ok".equalsIgnoreCase(status.getR()) && status.getStatus() == 1
                    && (!task.localAuthorized || status.firstFrame == 1)) recordingStarted(status.convertRecordInfoBean());
            controller.status(status, clock.getAsLong());
        });
    }
    @Override public void onRecordVideoStart() {
        handler.post(() -> {
            long now = clock.getAsLong();
            if (selected() && !task.localAuthorized && !ending && now >= task.startAt && now < task.endAt) {
                if (task.info != null) task.info.time = now;
                recordingStarted(task.info);
            }
        });
    }
    @Override public void onRecordVideoStop() { }
    @Override public void onRecordException(String error) { if (selected()) showState(error); }
    @Subscribe(threadMode = ThreadMode.MAIN) public void lowStorage(RecordStoppedOnLowStorageEvent event) {
        if (selected() && !ending) showState("存储空间不足，清理后将在录制时间窗内自动重试");
    }
    private Notification notification() {
        PendingIntent content = PendingIntent.getActivity(this, 0, new Intent(this, RecordActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent end = PendingIntent.getService(this, 1, new Intent(this, UnattendedRecordService.class).setAction(END),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("无人值守").setContentText(message).setContentIntent(content)
                .setOngoing(true).setOnlyAlertOnce(true).addAction(0, "关闭无人值守", end).build();
    }
    private void showState(String value) {
        if (value == null || value.equals(message)) return;
        message = value;
        getSystemService(NotificationManager.class).notify(NOTIFICATION, notification());
    }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() {
        if (controller != null) controller.cancel();
        handler.removeCallbacksAndMessages(null);
        BluetoothHandle.removePlayNotifyListener(this);
        EventBus.getDefault().unregister(this);
        if (localStarter != null) localStarter.cancel();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
}
