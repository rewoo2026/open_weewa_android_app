package com.xh.hotme.record;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;



import com.xh.hotme.R;
import com.xh.hotme.base.BaseQuickViewBindingAdapter;
import com.xh.hotme.base.BaseViewBindingHolder;
import com.xh.hotme.databinding.ListItemLiveTaskBinding;
import com.xh.hotme.http.CompetitionManager;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.record.bean.HmCompetitionRecordBean;
import com.xh.hotme.record.bean.CameraRecordInfoBean;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.ReTextUtils;
import com.xh.hotme.utils.TimeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class RecordTaskListAdapter extends BaseQuickViewBindingAdapter<HmCompetitionRecordBean, BaseViewBindingHolder<ListItemLiveTaskBinding>> {

    Context _ctx;

    List<String> _playNumberList;
    List<CameraRecordInfoBean> _recordName;

    public RecordTaskListAdapter(Context context, List data) {

        super(R.layout.list_item_live_task, data);
        _ctx = context;
        String[] playNumberList = _ctx.getResources().getStringArray(R.array.play_number_list);

        _playNumberList = Arrays.asList(playNumberList);
        _recordName = CompetitionManager.getCompetitionList(context);
    }

    @Override
    protected BaseViewBindingHolder getViewBinding(int viewType, LayoutInflater from, ViewGroup parent) {
        return new BaseViewBindingHolder(ListItemLiveTaskBinding.inflate(from, parent, false));
    }

    @Override
    protected void convert(@NonNull BaseViewBindingHolder<ListItemLiveTaskBinding> helper, HmCompetitionRecordBean model) {
        ListItemLiveTaskBinding viewBinding = helper.viewBind;

        if (model == null) return;
        viewBinding.tvTitle.setText(model.competitionName);

        String hostTeamName = TextUtils.isEmpty(model.homeTeamName) ? "主队" : model.homeTeamName;
        String guestTeamName = TextUtils.isEmpty(model.awayTeamName) ? "客队" : model.awayTeamName;
        viewBinding.tvVs.setText(hostTeamName + " VS " + guestTeamName);
        viewBinding.tvStart.setText("开始录制");

        viewBinding.slipView.closeMenu(0);
        try {

            String ss = viewBinding.getRoot().getContext().getResources().getStringArray(R.array.motion_type_list)[Integer.parseInt(model.motionType)];
            viewBinding.tvStyle.setText(ss);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String ss = "其它";
            if (model.motionType.equalsIgnoreCase("1")) {  //足球
                ss = viewBinding.getRoot().getContext().getResources()
                        .getStringArray(R.array.place_type_list)
                        [Integer.parseInt(model.placeType)];
            } else if (model.motionType.equalsIgnoreCase("2")) {  //篮球
                ss = viewBinding.getRoot().getContext().getResources()
                        .getStringArray(R.array.place_type_list_bb)
                        [Integer.parseInt(model.placeType)];
            } else if (model.motionType.equalsIgnoreCase("0")) {  //训练
                ss = viewBinding.getRoot().getContext().getResources()
                        .getStringArray(R.array.place_type_list_tran)
                        [0];
            }

            String substring = model.competitionTime.substring(0,
                    model.competitionTime.lastIndexOf(" "));


            String substring1 = model.competitionTime.substring(
                    model.competitionTime.lastIndexOf(" ") + 1, model.competitionTime.length() - 3);

            viewBinding.tvContent.setText(substring + " " + ReTextUtils.Week(substring) + " " + substring1 + " | " + ss);
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            long now = System.currentTimeMillis();
            long competitionTime = TimeUtil.str2Millis(model.competitionTime);
            long diff = now - competitionTime;
            long day = diff / (1000 * 60 * 60 * 24); //以天数为单位取整
            long hour = (diff / (60 * 60 * 1000) - day * 24) + day * 24; //以小时为单位取整

            if (Math.abs(hour) <= 2) {
                viewBinding.tvStart.setEnabled(true);
                viewBinding.tvStart.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        if (commonListener != null) {
                            commonListener.onReturn(IActionCallback.CALLBACK_ACTION_START, model);
                        }
                        return false;
                    }
                });
            } else {
                viewBinding.tvStart.setEnabled(false);
                viewBinding.tvStart.setOnClickListener(null);
            }
        } catch (Exception e) {
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
                if (commonListener != null) {
                    commonListener.onReturn(IActionCallback.CALLBACK_ACTION_DELETE, model.competitionId, model.competitionName);
                }
                return false;
            }
        });

        viewBinding.menuEdit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if (commonListener != null) {
                    commonListener.onReturn(IActionCallback.CALLBACK_ACTION_EDIT, model);
                }

                new Handler().postDelayed(() -> {
                    try {
                        viewBinding.slipView.closeMenu(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 500);

                return false;
            }
        });
    }

    IActionCallback commonListener;

    public void setCommonListener(IActionCallback commonListener) {
        this.commonListener = commonListener;
    }



}
