package com.xh.hotme.storage;

import android.text.TextUtils;
import android.view.*;
import androidx.recyclerview.widget.RecyclerView;
import com.xh.hotme.R;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.databinding.CameraVideoBatchItemBinding;
import com.xh.hotme.utils.*;
import java.util.*;

public class CameraVideoListAdapter extends RecyclerView.Adapter<CameraVideoListAdapter.Holder> {
    private final ArrayList<CameraVideoListBean.VideosBean> items;
    private final CameraBatchDeleteModel model;
    private final CameraAnalysisModel analysis;
    private java.util.function.Consumer<MatchAnalysisTask> taskClick;
    private OnItemClick click;
    public interface OnItemClick { void onItemClick(int position); }
    public CameraVideoListAdapter(ArrayList<CameraVideoListBean.VideosBean> items,CameraBatchDeleteModel model,CameraAnalysisModel analysis){this.items=items;this.model=model;this.analysis=analysis;}
    public void setTaskClick(java.util.function.Consumer<MatchAnalysisTask> click){taskClick=click;}
    public void setOnItemClickListener(OnItemClick click){this.click=click;}
    @Override public Holder onCreateViewHolder(ViewGroup parent,int type){return new Holder(CameraVideoBatchItemBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false));}
    @Override public int getItemCount(){return items.size();}
    @Override public void onBindViewHolder(Holder holder,int position){holder.bind(items.get(position),position);}
    final class Holder extends RecyclerView.ViewHolder {
        final CameraVideoBatchItemBinding b;
        Holder(CameraVideoBatchItemBinding b){super(b.getRoot());this.b=b;}
        void bind(CameraVideoListBean.VideosBean data,int position){
            b.dateHeader.setVisibility(AdapterHeadUtils.shouldShowDateHeader(items,position)?View.VISIBLE:View.GONE);
            b.dateTitle.setText(AdapterHeadUtils.getHeadTitle(data));
            ArrayList<CameraVideoListBean.VideosBean> day=new ArrayList<>();
            for(CameraVideoListBean.VideosBean item:items)if(TimeUtil.isSameDay(item.getTimeInMillis(),data.getTimeInMillis()))day.add(item);
            boolean ai=analysis.selecting, selecting=ai||model.selecting;
            boolean full=true;for(CameraVideoListBean.VideosBean v:day)if((ai?analysis.canSelect(v):model.canSelect(v))&&!(ai?analysis.selected:model.selected).contains(v.path))full=false;
            b.selectDay.setVisibility(selecting?View.VISIBLE:View.GONE);
            b.selectDay.setText(full?R.string.batch_unselect_day:R.string.batch_select_day);
            b.selectDay.setEnabled(!model.running()&&!model.needsReconcile());
            b.selectDay.setOnClickListener(v->{if(ai)analysis.toggleGroup(day);else model.toggleGroup(day);});
            GlideUtil.loadRoundedCorner(b.cover.getContext(),BluetoothManager.getCameraFileUrl(data.path+"/"+data.thumb),b.cover,10,R.mipmap.video_default_cover);
            b.matchTitle.setText(data.competition_name);
            b.videoCounts.setText(b.getRoot().getContext().getString(R.string.batch_video_counts,data.getVideoTotalCount(),data.full_video_number+data.playback_number,data.highlights_number+data.home_goals+data.away_goals+data.ai_highlights_number));
            String reason=TextUtils.isEmpty(data.delete_block_reason)?"":CameraBatchDeleteModel.reason(data.delete_block_reason);
            b.blockReason.setText(reason);b.blockReason.setVisibility(reason.isEmpty()||ai?View.GONE:View.VISIBLE);
            b.selectMatch.setOnCheckedChangeListener(null);b.selectMatch.setVisibility(selecting?View.VISIBLE:View.GONE);
            b.selectMatch.setChecked((ai?analysis.selected:model.selected).contains(data.path));b.selectMatch.setContentDescription("选择比赛 "+data.competition_name);
            b.selectMatch.setEnabled(ai?analysis.canSelect(data)&&!analysis.submitting:model.canSelect(data)&&!model.running()&&!model.needsReconcile());
            b.selectMatch.setOnCheckedChangeListener((button,checked)->{if(ai)analysis.toggle(data);else model.toggle(data.path);});
            MatchAnalysisTask task=analysis.tasks.get(data.competition_id);
            b.aiProgressGroup.setVisibility(task!=null&&!selecting?View.VISIBLE:View.GONE);
            if(task!=null){String status=task.label();if(("preparing".equals(task.state)||"queued".equals(task.state))&&!TextUtils.isEmpty(task.message))status+=" · "+task.message;b.aiStatus.setText(status);b.aiUploadPercent.setText("相机上传  "+task.uploadPercent+"%");b.aiUploadBar.setProgress(task.uploadPercent);b.aiAnalysisPercent.setText("AI分析  "+task.analysisPercent+"%");b.aiAnalysisBar.setProgress(task.analysisPercent);b.aiStatus.setOnClickListener(v->{if(taskClick!=null)taskClick.accept(task);});}
            b.matchRow.setOnClickListener(v->{int p=getBindingAdapterPosition();if(p==RecyclerView.NO_POSITION)return;if(ai)analysis.toggle(data);else if(model.selecting)model.toggle(data.path);else if(click!=null)click.onItemClick(p);});
            b.matchRow.setOnLongClickListener(v->{if(analysis.selecting){analysis.toggle(data);return true;}if(model.protocol<1||model.running())return false;if(!model.selecting)model.enterSelection();model.toggle(data.path);return true;});
        }
    }
}
