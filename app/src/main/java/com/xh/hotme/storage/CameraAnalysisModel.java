package com.xh.hotme.storage;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.http.*;
import com.xh.hotme.utils.*;
import org.json.*;
import java.util.*;

/** UI observes durable server tasks; stopping the poll never stops upload/analysis. */
public class CameraAnalysisModel extends ViewModel {
    public final LinkedHashSet<String> selected=new LinkedHashSet<>();
    public final Map<String,MatchAnalysisTask> tasks=new HashMap<>();
    public final MutableLiveData<Integer> changes=new MutableLiveData<>(0);
    public String cameraId="",error="";
    public boolean selecting,submitting;
    private boolean polling,busy;
    private long requestVersion;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable poll=new Runnable(){public void run(){refresh();if(polling)handler.postDelayed(this,5000);}};
    public void attach(String id){id=id==null?"":id;if(!cameraId.equals(id)){requestVersion++;cameraId=id;selected.clear();tasks.clear();selecting=false;busy=false;submitting=false;changed();if(polling)refresh();}}
    public void start(){polling=true;handler.removeCallbacks(poll);handler.post(poll);}
    public void stop(){polling=false;handler.removeCallbacks(poll);}
    public boolean hasActive(){for(MatchAnalysisTask t:tasks.values())if(t.active())return true;return false;}
    public boolean canSelect(CameraVideoListBean.VideosBean v){
        MatchAnalysisTask t=tasks.get(v.competition_id);
        return v.competition_id!=null&&!v.competition_id.isEmpty()&&!"recording".equals(v.delete_block_reason)&&!"processing".equals(v.delete_block_reason)&&(t==null||"failed".equals(t.state));
    }
    public void toggle(CameraVideoListBean.VideosBean v){if(!submitting&&canSelect(v)){if(!selected.remove(v.path))selected.add(v.path);changed();}}
    public void toggleGroup(Collection<CameraVideoListBean.VideosBean> items){var paths=new ArrayList<String>();for(var v:items)if(canSelect(v))paths.add(v.path);if(selected.containsAll(paths))selected.removeAll(paths);else selected.addAll(paths);changed();}
    public void enter(){selecting=true;selected.clear();changed();}
    public void cancel(){if(submitting)return;selecting=false;selected.clear();changed();}
    public void refresh(){if(cameraId.isEmpty()||busy||submitting)return;request("list",Collections.emptyList(),false);}
    public void submit(Collection<CameraVideoListBean.VideosBean> items){var ids=new ArrayList<String>();var matches=new JSONArray();for(var v:items)if(selected.contains(v.path)&&canSelect(v)){
        ids.add(v.competition_id);JSONObject m=new JSONObject();try{
            m.put("competitionId",v.competition_id);m.put("competitionType",v.competition_type);m.put("competitionName",v.competition_name);m.put("startTime",v.getTimeInMillis());m.put("duration",v.competition_duration);m.put("motionType",v.motion_type);m.put("placeType",Integer.parseInt(v.place_type==null||v.place_type.isEmpty()?"2":v.place_type));m.put("place",v.address);m.put("homeTeamName",v.host_team);m.put("awayTeamName",v.guest_team);m.put("filesBasePath",v.path.replaceFirst("^/",""));matches.put(m);
        }catch(JSONException|NumberFormatException e){ToastUtil.s("赛事信息不完整，请刷新列表后重试");return;}
    }if(ids.isEmpty())return;submitting=true;changed();request("submit",ids,true,matches);}
    public void retry(String id){if(submitting)return;submitting=true;changed();request("submit",Collections.singletonList(id),true);}
    private void request(String action,List<String> ids,boolean retry){
        request(action,ids,retry,new JSONArray());
    }
    private void request(String action,List<String> ids,boolean retry,JSONArray matches){
        final String device=cameraId;final long version=++requestVersion;busy=true;
        JSONObject body=new JSONObject();try{body.put("deviceId",device);body.put("competitionIds",new JSONArray(ids));body.put("retry",retry);body.put("matches",matches);}catch(JSONException e){throw new IllegalStateException(e);}
        OkHttpUtil.postData(SdkApi.getRequestUrl()+"/hotme-api/hm/matchAnalysis/"+action,body.toString(),null,new OkHttpCallbackDecode<MatchAnalysisTask.Response>(MatchAnalysisTask.Response.class){
            public void onDataSuccess(MatchAnalysisTask.Response response){MainHandler.runOnUiThread(()->{
                if(version!=requestVersion||!device.equals(cameraId))return;
                if(response==null||response.data==null){
                    error="未获取到分析任务，请稍后重试";
                    if("submit".equals(action))ToastUtil.s("提交失败："+error);
                    changed();return;
                }
                tasks.clear();
                for(var t:response.data)if(t!=null&&t.competitionId!=null&&!t.competitionId.isEmpty())tasks.put(t.competitionId,t);
                error="";
                if("submit".equals(action)){selecting=false;selected.clear();ToastUtil.s("任务已创建，可离开页面，后台继续处理");}
                changed();
            });}
            public void onFailure(String code,String message){MainHandler.runOnUiThread(()->{if(version!=requestVersion||!device.equals(cameraId))return;error=message;if("list".equals(action)&&("404".equals(code)||"405".equals(code)))stop();if("submit".equals(action))ToastUtil.s("提交失败："+message);changed();});}
            public void onFinish(){MainHandler.runOnUiThread(()->{if(version!=requestVersion||!device.equals(cameraId))return;busy=false;submitting=false;changed();});}
        });
    }
    public void changed(){changes.setValue(changes.getValue()+1);}
    @Override protected void onCleared(){stop();}
}
