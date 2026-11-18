package com.xh.hotme.live.adapter;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseVH;
import com.xh.hotme.databinding.ListItemLiveTaskBinding;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.live.bean.HmCompetitionLive;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.ReTextUtils;
import com.xh.hotme.utils.TimeUtil;

import java.util.ArrayList;
import java.util.List;


public class LiveTaskListAdapter extends RecyclerView.Adapter<BaseVH<HmCompetitionLive, ListItemLiveTaskBinding>> {

    private final List<HmCompetitionLive> mList = new ArrayList<>();

    public void setList(List<HmCompetitionLive> list) {
        mList.clear();

        // 只显示今天的直播
        for(HmCompetitionLive bean : list) {
            if(TimeUtil.isToday(bean.competitionTime)) {
                mList.add(bean);
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BaseVH<HmCompetitionLive, ListItemLiveTaskBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LiveTaskViewHolder(ListItemLiveTaskBinding.bind(
            LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_live_task, parent, false)
        ));
    }

    @Override
    public void onBindViewHolder(@NonNull BaseVH<HmCompetitionLive, ListItemLiveTaskBinding> holder, @SuppressLint("RecyclerView") int position) {
        holder.update(mList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class LiveTaskViewHolder extends BaseVH<HmCompetitionLive, ListItemLiveTaskBinding> {
        public LiveTaskViewHolder(ListItemLiveTaskBinding viewBinding) {
            super(viewBinding);
        }

        @Override
        public void update(HmCompetitionLive model, int position) {
            if(model == null) return;
            viewBinding.tvTitle.setText(model.competitionName);

            String hostTeamName = TextUtils.isEmpty(model.homeTeamName) ? "主队" : model.homeTeamName;
            String guestTeamName = TextUtils.isEmpty(model.awayTeamName) ? "客队" : model.awayTeamName;
            viewBinding.tvVs.setText(hostTeamName + " VS " + guestTeamName);

            viewBinding.slipView.closeMenu(0);
            try {

                String ss = viewBinding.getRoot().getContext().getResources().getStringArray(R.array.motion_type_list)[Integer.parseInt(model.motionType)];
                viewBinding.tvStyle.setText(ss);
            } catch(Exception e) {
                e.printStackTrace();
            }

            try {
                String ss = "其它";
                if(model.motionType.equalsIgnoreCase("1")) {  //足球
                    ss = viewBinding.getRoot().getContext().getResources()
                            .getStringArray(R.array.place_type_list)
                            [Integer.parseInt(model.placeType)];
                } else if(model.motionType.equalsIgnoreCase("2")){  //篮球
                    ss = viewBinding.getRoot().getContext().getResources()
                            .getStringArray(R.array.place_type_list_bb)
                            [Integer.parseInt(model.placeType)];
                } else if(model.motionType.equalsIgnoreCase("0")){  //训练
                    ss = viewBinding.getRoot().getContext().getResources()
                            .getStringArray(R.array.place_type_list_tran)
                            [0];
                }

                String substring = model.competitionTime.substring(0,
                    model.competitionTime.lastIndexOf(" "));


                String substring1 = model.competitionTime.substring(
                    model.competitionTime.lastIndexOf(" ") + 1, model.competitionTime.length() - 3);

                viewBinding.tvContent.setText(substring + " " + ReTextUtils.Week(substring) + " " + substring1 + " | " + ss);
            } catch(Exception e) {
                e.printStackTrace();
            }


            try {
                long now = System.currentTimeMillis();
                long competitionTime = TimeUtil.str2Millis(model.competitionTime);
                long diff = now - competitionTime;
                long day = diff / (1000 * 60 * 60 * 24); //以天数为单位取整
                long hour = (diff / (60 * 60 * 1000) - day * 24) + day * 24; //以小时为单位取整

                if(Math.abs(hour) <= 2) {
                    viewBinding.tvStart.setEnabled(true);
                    viewBinding.tvStart.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            commonListener.onReturn(IActionCallback.CALLBACK_ACTION_START, model);
                            return false;
                        }
                    });
                } else {
                    viewBinding.tvStart.setEnabled(false);
                    viewBinding.tvStart.setOnClickListener(null);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }

            viewBinding.getRoot().setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    return false;
                }
            });

            viewBinding.menuDelete.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    commonListener.onReturn(IActionCallback.CALLBACK_ACTION_DELETE, model.competitionId, model.competitionName);
                    return false;
                }
            });

            viewBinding.menuEdit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    commonListener.onReturn(IActionCallback.CALLBACK_ACTION_EDIT, model);

                    new Handler().postDelayed(() -> {
                        try {
                            viewBinding.slipView.closeMenu(0);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }, 500);

                    return false;
                }
            });
        }
    }


    IActionCallback commonListener;

    public void setCommonListener(IActionCallback commonListener) {
        this.commonListener = commonListener;
    }
}
