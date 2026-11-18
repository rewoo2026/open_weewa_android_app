package com.xh.hotme.storage;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bluetooth.bean.StorageInfoRequestBean;
import com.xh.hotme.bluetooth.bean.VideoDirRemoveRequestBean;
import com.xh.hotme.bluetooth.bean.VideoListRequestBean;
import com.xh.hotme.bluetooth.event.*;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.http.ConnectLogic;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.Constants;
import org.greenrobot.eventbus.*;
import java.util.*;

/** Survives rotation. Restored process state never resumes destructive commands automatically. */
public final class CameraBatchDeleteModel extends ViewModel {
    private final SavedStateHandle saved;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final MutableLiveData<Integer> changes=new MutableLiveData<>(0);
    public final ArrayList<CameraVideoListBean.VideosBean> videos=new ArrayList<>();
    public final LinkedHashSet<String> selected=new LinkedHashSet<>();
    public BatchDeleteSession session;
    private final HashMap<String,BatchDeleteSession> suspended=new HashMap<>();
    public String cameraId="",message="";
    public int protocol;
    public boolean selecting,loading;
    private String listRequest="";
    private int accessMode;
    private final Runnable deleteTimeout=this::onDeleteTimeout;
    private final Runnable listRetry=new Runnable(){public void run(){
        if(!loading||listRequest.isEmpty())return;
        if(DeviceApiManager.isConnected())DeviceApiManager.sendCmdSimpleObj(new VideoListRequestBean(listRequest));
        handler.postDelayed(this,10000);
    }};
    private final Runnable listTimeout=()->{handler.removeCallbacks(listRetry);loading=false;listRequest="";message="读取比赛列表超时，请保持相机连接后重试";changed();};

    public CameraBatchDeleteModel(SavedStateHandle saved) {
        this.saved=saved;
        BatchDeleteSession restored=saved.get("delete_session");session=restored==null?new BatchDeleteSession():restored;
        session.pause("App 已重新启动，需重新核对");
        String id=saved.get("delete_camera");cameraId=id==null?"":id;
        HashMap<String,BatchDeleteSession> previous=saved.get("delete_suspended");if(previous!=null)suspended.putAll(previous);
        ArrayList<String> paths=saved.get("delete_selected");if(paths!=null)selected.addAll(paths);
        Boolean selectingSaved=saved.get("delete_selecting");selecting=Boolean.TRUE.equals(selectingSaved);
        EventBus.getDefault().register(this);
    }
    public LiveData<Integer> changes() { return changes; }
    private void onDeleteTimeout(){session.pause("删除回复超时，需重新核对");changed();refresh();}
    public boolean running() { return session.state==BatchDeleteSession.State.RUNNING; }
    public boolean needsReconcile() { return session.hasUnknown(); }
    public boolean canSelect(CameraVideoListBean.VideosBean video) {
        return protocol>=1 && video.path!=null && !video.path.isEmpty()
                && video.delete_identity!=null && !video.delete_identity.isEmpty()
                && (video.delete_block_reason==null || video.delete_block_reason.isEmpty());
    }
    public void attachCamera(String id) {
        if(id==null)id="";
        if(cameraId.equals(id))return;
        handler.removeCallbacks(deleteTimeout);handler.removeCallbacks(listTimeout);handler.removeCallbacks(listRetry);
        session.pause("相机已切换，删除队列已暂停");
        if(!session.items.isEmpty())suspended.put(cameraId,session);
        session=suspended.containsKey(id)?suspended.get(id):new BatchDeleteSession();
        cameraId=id;protocol=0;videos.clear();selected.clear();selecting=false;loading=false;listRequest="";changed();
    }
    public void refresh() {
        if(loading || running() || cameraId.isEmpty())return;
        listRequest=UUID.randomUUID().toString();loading=true;message="";
        if(!DeviceApiManager.sendCmdSimpleObj(new VideoListRequestBean(listRequest))){loading=false;listRequest="";message="相机连接已断开，请重新连接后核对比赛";}
        else {handler.postDelayed(listTimeout,60000);handler.postDelayed(listRetry,5000);}
        changed();
    }
    @Subscribe(threadMode=ThreadMode.MAIN) public void snapshot(CameraVideoSnapshotEvent event) {
        CameraVideoListBean data=event.snapshot;
        boolean correlated=data!=null && listRequest.equals(data.request_id) && cameraId.equals(data.device_id);
        boolean legacy=data!=null && data.video_delete_protocol==0 && loading && (data.request_id==null||data.request_id.isEmpty());
        if(!correlated&&!legacy)return;
        handler.removeCallbacks(listTimeout);handler.removeCallbacks(listRetry);loading=false;listRequest="";message="";
        Map<String,String> previousIdentity=new HashMap<>();for(CameraVideoListBean.VideosBean v:videos)previousIdentity.put(v.path,v.delete_identity);
        protocol=data.video_delete_protocol;videos.clear();if(data.videos!=null)videos.addAll(data.videos);
        Set<String> existing=new HashSet<>();for(CameraVideoListBean.VideosBean v:videos)existing.add(v.path);
        if(correlated){Set<String> sameMatch=new HashSet<>();for(CameraVideoListBean.VideosBean v:videos)for(BatchDeleteSession.Item i:session.items)if(i.path.equals(v.path)&&i.identity.equals(v.delete_identity))sameMatch.add(v.path);session.reconcile(cameraId,sameMatch);}
        selected.retainAll(existing);
        for(CameraVideoListBean.VideosBean v:videos)if(previousIdentity.containsKey(v.path)&&!Objects.equals(previousIdentity.get(v.path),v.delete_identity))selected.remove(v.path);
        if(session.cameraId.equals(cameraId))for(BatchDeleteSession.Item item:session.items)if(item.outcome==BatchDeleteSession.Outcome.SUCCESS){selected.remove(item.path);videos.removeIf(v->item.path.equals(v.path)&&item.identity.equals(v.delete_identity));}
        changed();
    }
    public void enterSelection() { selecting=true;selected.clear();changed(); }
    public void cancelSelection() { if(running())return;selecting=false;selected.clear();changed(); }
    public void toggle(String path) { if(running()||needsReconcile())return;for(CameraVideoListBean.VideosBean v:videos)if(path.equals(v.path)&&canSelect(v)){if(!selected.remove(path))selected.add(path);break;}changed(); }
    public void toggleGroup(Collection<CameraVideoListBean.VideosBean> group) {
        if(running()||needsReconcile())return;
        ArrayList<String> paths=new ArrayList<>();for(CameraVideoListBean.VideosBean v:group)if(canSelect(v))paths.add(v.path);
        if(selected.containsAll(paths))selected.removeAll(paths);else selected.addAll(paths);changed();
    }
    public boolean begin() {
        if(running()||needsReconcile()||protocol<1||selected.isEmpty())return false;
        if(!cameraId.equals(CameraManager.getDeviceId())||!DeviceApiManager.isConnected()){message="相机连接已断开";changed();return false;}
        if(downloading()){message="视频正在下载，请完成或停止下载后再删除";changed();return false;}
        ArrayList<BatchDeleteSession.Item> items=new ArrayList<>();for(CameraVideoListBean.VideosBean v:videos)if(selected.contains(v.path)&&v.delete_identity!=null&&!v.delete_identity.isEmpty())items.add(new BatchDeleteSession.Item(v.path,v.competition_name,v.delete_identity));
        if(items.isEmpty())return false;accessMode=Constants.ACCESS_MODEL;session.start(cameraId,items);drive();return true;
    }
    private boolean downloading() { return ConnectLogic.getImpl().isDownLoading()&&cameraId.equals(ConnectLogic.getImpl().getDeviceId()); }
    private void drive() {
        if(!running())return;
        if(!cameraId.equals(CameraManager.getDeviceId())||accessMode!=Constants.ACCESS_MODEL||!DeviceApiManager.isConnected()||downloading()){
            session.pause("连接或访问模式已变化，请核对比赛后重试");changed();return;
        }
        BatchDeleteSession.Item item=session.next();changed();
        if(item==null){DeviceApiManager.sendCmdSimpleObj(new StorageInfoRequestBean());refresh();return;}
        if(!DeviceApiManager.sendCmdSimpleObj(new VideoDirRemoveRequestBean(item.path,item.requestId,cameraId,item.identity))){session.pause("发送失败，请重新连接后核对");changed();return;}
        handler.postDelayed(deleteTimeout,45000);
    }
    @Subscribe(threadMode=ThreadMode.MAIN) public void result(CameraVideoDeleteResultEvent result) {
        if(!session.reply(result.deviceId,result.requestId,result.path,"ok".equals(result.result),reason(result.result)))return;
        handler.removeCallbacks(deleteTimeout);
        if("ok".equals(result.result)){selected.remove(result.path);videos.removeIf(v->result.path.equals(v.path));}
        drive();
    }
    @Subscribe(threadMode=ThreadMode.MAIN) public void disconnected(BleDisConnectSuccessEvent event) {
        handler.removeCallbacks(listTimeout);handler.removeCallbacks(listRetry);loading=false;listRequest="";
        if(running()){handler.removeCallbacks(deleteTimeout);session.pause("相机连接已断开，需重新核对");changed();}
    }
    public void stop() { session.requestStop();changed();if(session.current()==null&&running())drive(); }
    public void acknowledgeResult() { session.state=needsReconcile()?BatchDeleteSession.State.PAUSED:BatchDeleteSession.State.IDLE;changed(); }
    public static String reason(String reason) {
        if(reason==null)return "相机未返回删除结果";
        switch(reason){case "recording":return "正在录制，暂不可删除";case "processing":return "正在处理，暂不可删除";case "uploading":return "正在上传，暂不可删除";case "downloading":return "正在下载或播放，暂不可删除";case "invalid path":return "比赛目录无效";case "wrong device":return "相机不匹配";case "match changed":return "比赛已变化，请重新选择";case "not a match directory":return "不是比赛目录";case "delete busy":return "比赛正在删除";case "failed to delete path":return "文件删除失败";default:return reason;}
    }
    private void changed() {
        saved.set("delete_session",session);saved.set("delete_suspended",suspended);saved.set("delete_camera",cameraId);saved.set("delete_selected",new ArrayList<>(selected));saved.set("delete_selecting",selecting);
        changes.setValue(changes.getValue()==null?1:changes.getValue()+1);
    }
    @Override protected void onCleared(){handler.removeCallbacksAndMessages(null);EventBus.getDefault().unregister(this);}
}
