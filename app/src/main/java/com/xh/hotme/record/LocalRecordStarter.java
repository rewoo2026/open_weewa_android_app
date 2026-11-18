package com.xh.hotme.record;

import android.os.Handler;
import android.os.Looper;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.LocalRecordStopRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStartRequestBean;
import com.xh.hotme.bluetooth.bean.RecordStatusRequestBean;
import com.xh.hotme.bluetooth.listener.IRecordControlListener;
import com.xh.hotme.http.RecordingApi;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CommonResponse;
import com.xh.hotme.record.bean.LocalRecordingGrant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** HTTP business permission followed by Bluetooth execution; no camera WAN dependency. */
final class LocalRecordStarter {
    interface Backend {
        void prepare(String id, long version, String requestId, IGenericApiCallback<LocalRecordingGrant> callback);
        void cancel(LocalRecordingGrant grant, Runnable finished);
        void report(RecordStatusResultBean status);
    }
    interface Device {
        boolean start(LocalRecordingGrant grant);
        boolean query();
        boolean stop(LocalRecordingGrant grant);
    }
    interface Timer { void post(Runnable action, long delay); void remove(Runnable action); }
    interface Listener { void onStarted(); void onFailure(String message); void onWarning(String message); }
    private final Backend backend;
    private final Device device;
    private final Timer timer;
    private final Listener listener;
    private final Runnable detach;
    private boolean requested, active, cancelling, finishingCancel, sent;
    private LocalRecordingGrant grant;
    private boolean offline;
    private final List<Runnable> cancelCallbacks = new ArrayList<>();
    private final Runnable poll = this::query;
    private final Runnable timeout = () -> fail("本地录制启动尚未确认，请检查相机状态");
    private final Runnable cancelTimeout = this::onCancelTimeout;
    private void onCancelTimeout() {
        listener.onWarning("取消录制尚未确认，请检查相机状态"); finishCancel();
    }

    static LocalRecordStarter create(RecordStartRequestBean request, Listener listener) {
        return create(request,listener,null);
    }
    static LocalRecordStarter createOffline(RecordStartRequestBean request, Listener listener,
            com.xh.hotme.account.DailyAccessManager.DeviceAccess access) {
        return create(request,listener,access);
    }
    private static LocalRecordStarter create(RecordStartRequestBean request, Listener listener,
            com.xh.hotme.account.DailyAccessManager.DeviceAccess access) {
        Handler handler = new Handler(Looper.getMainLooper());
        LocalRecordStarter[] instance = new LocalRecordStarter[1];
        IRecordControlListener notifications = new IRecordControlListener() {
            public void onRecordVideoStart() { handler.post(() -> instance[0].query()); }
            public void onRecordVideoStop() { handler.post(() -> instance[0].query()); }
            public void onRecordVideoStatusUpdate(RecordStatusResultBean status) { handler.post(() -> instance[0].onStatus(status)); }
            public void onRecordException(String message) { handler.post(() -> instance[0].onException(message)); }
        };
        instance[0] = new LocalRecordStarter(new Backend() {
            public void prepare(String id, long version, String requestId, IGenericApiCallback<LocalRecordingGrant> callback) {
                if(access!=null) {
                    if(!com.xh.hotme.account.DailyAccessManager.ready()||!access.recordingAllowed||access.permit==null) {callback.onFail("-1","请先联网同步今日录制权限");callback.onFinish();return;}
                    try {OfflineRecordingReports.pending(request,access,requestId);}
                    catch(RuntimeException e){callback.onFail("-2",e.getMessage());callback.onFinish();return;}
                    LocalRecordingGrant g=new LocalRecordingGrant();g.operation_id=requestId;g.request_id=requestId;g.live_cid=id;g.local_control=true;
                    callback.onSuccess(g);callback.onFinish();
                } else RecordingApi.prepareLocal(id, version, requestId, callback);
            }
            public void cancel(LocalRecordingGrant grant, Runnable finished) {
                if(access!=null){finished.run();return;}
                RecordingApi.cancelLocal(grant, new IGenericApiCallback<CommonResponse>() {
                    public void onSuccess(CommonResponse response) { }
                    public void onFail(String code, String message) { listener.onWarning("录制已在本地取消，服务器状态同步失败，请联网后刷新"); }
                    public void onFinish() { finished.run(); }
                });
            }
            public void report(RecordStatusResultBean status) { if(access!=null)OfflineRecordingReports.observe(status);else LocalRecordingReports.enqueue(status, true); }
        }, new Device() {
            public boolean start(LocalRecordingGrant grant) {
                if(access!=null){request.local_control=true;request.offline_permit=access.permit;request.operation_id=grant.operation_id;request.setLive_cid("");}
                else grant.apply(request); return BluetoothManager.getImpl().sendCmdSimpleObj(request); }
            public boolean query() { return BluetoothManager.getImpl().sendCmdSimpleObj(new RecordStatusRequestBean()); }
            public boolean stop(LocalRecordingGrant grant) { return BluetoothManager.getImpl().sendCmdSimpleObj(new LocalRecordStopRequestBean(grant.live_cid, grant.session_id)); }
        }, new Timer() {
            public void post(Runnable action, long delay) { handler.postDelayed(action, delay); }
            public void remove(Runnable action) { handler.removeCallbacks(action); }
        }, listener, () -> BluetoothHandle.removePlayNotifyListener(notifications));
        instance[0].offline=access!=null;
        BluetoothHandle.addPlayNotifyListener(notifications);
        return instance[0];
    }

    LocalRecordStarter(Backend backend, Device device, Timer timer, Listener listener, Runnable detach) {
        this.backend = backend; this.device = device; this.timer = timer; this.listener = listener; this.detach = detach;
    }
    String competitionId(){return grant==null?"":grant.live_cid;}
    void start(String id, long observedVersion) {
        if (requested) return;
        requested = true; active = true; timer.post(timeout, 120000);
        String requestId = UUID.randomUUID().toString();
        backend.prepare(id, observedVersion, requestId, new IGenericApiCallback<LocalRecordingGrant>() {
            public void onSuccess(LocalRecordingGrant result) {
                boolean ours = result != null && requestId.equals(result.request_id) && (offline ? requestId.equals(result.operation_id) : id.equals(result.live_cid) && result.session_id != null && !result.session_id.isEmpty() && result.operation_id != null && !result.operation_id.isEmpty());
                if (!active) { if (ours) backend.cancel(result, () -> { }); return; }
                grant = ours ? result : null;
                if (!ours || (!offline && !result.valid(id, observedVersion)) || !requestId.equals(result.request_id)) {
                    fail("本地录制授权响应异常"); return;
                }
                sent = device.start(result);
                if (!sent) { fail("蓝牙连接已断开，请重新连接相机"); return; }
                timer.remove(timeout); timer.post(timeout, 90000); timer.post(poll, 2000);
            }
            public void onFail(String code, String message) { if (active) fail(message); }
            public void onFinish() { }
        });
    }
    void onStatus(RecordStatusResultBean status) {
        if (status == null || !"ok".equalsIgnoreCase(status.getR()) || grant == null) return;
        String id = status.competitionId == null || status.competitionId.isEmpty() ? status.getLiveCId() : status.competitionId;
        if(offline)OfflineRecordingReports.observe(status);
        boolean matching = offline ? grant.operation_id.equals(status.operationId) : grant.live_cid.equals(id) && grant.session_id.equals(status.sessionId);
        if(offline&&matching&&id!=null&&!id.isEmpty()&&status.sessionId!=null) {
            grant.live_cid=id;grant.session_id=status.sessionId;grant.device_version=status.deviceVersion;
        }
        if(offline&&cancelling&&status.localControl==1&&grant.session_id!=null&&grant.session_id.equals(status.sessionId))matching=true;
        if (cancelling) {
            if ((status.getStatus() <= 0 && "idle".equals(status.deviceState)) || (status.getStatus() == 1 && !matching)) finishCancel();
            else if (matching) device.stop(grant);
        } else if (active && matching && status.getStatus() == 1 && status.firstFrame == 1
                && status.localControl == 1 && status.deviceVersion >= grant.device_version) {
            clear(); detach.run(); backend.report(status); listener.onStarted();
        }
    }
    void onException(String message) {
        if (active && grant != null && !cancelling) fail(message);
    }
    private void query() {
        if ((!active && !cancelling) || finishingCancel) return;
        timer.remove(poll); device.query(); timer.post(poll, 2000);
    }
    private void fail(String message) {
        if (!active) return;
        cancel(() -> listener.onFailure(message == null || message.isEmpty() ? "本地录制启动失败" : message));
    }
    void cancel() { cancel(() -> { }); }
    void cancel(Runnable finished) {
        if (cancelling) { cancelCallbacks.add(finished); return; }
        if (!active) { finished.run(); return; }
        clear(); cancelCallbacks.add(finished); cancelling = true;
        if (grant == null || !sent) { finishCancel(); return; }
        if(!offline || grant.session_id!=null)device.stop(grant); timer.post(cancelTimeout, 30000); query();
    }
    private void finishCancel() {
        if (!cancelling || finishingCancel) return;
        finishingCancel = true;
        timer.remove(poll); timer.remove(cancelTimeout); detach.run();
        Runnable done = () -> {
            cancelling = false;
            List<Runnable> callbacks = new ArrayList<>(cancelCallbacks); cancelCallbacks.clear();
            for (Runnable callback : callbacks) callback.run();
        };
        if (grant == null) done.run(); else backend.cancel(grant, done);
    }
    private void clear() { active = false; timer.remove(poll); timer.remove(timeout); }
}
