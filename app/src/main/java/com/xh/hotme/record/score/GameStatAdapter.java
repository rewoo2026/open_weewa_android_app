package com.xh.hotme.record.score;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseQuickAdapter;
import com.xh.hotme.base.BaseViewHolder;
import com.xh.hotme.R;
import com.xh.hotme.bean.GameStatusBean;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;

import java.util.Arrays;
import java.util.List;

/**
 * date   : 2023/6/19
 * desc   :
 */
public class GameStatAdapter extends BaseQuickAdapter<GameStatusBean, BaseViewHolder> {
    private final static String TAG = GameStatAdapter.class.getSimpleName();

    final Context _context;

    GoalEditCallback _callBack;

    final List<String> _statTypeList;


    public GameStatAdapter(Context context, List data) {
        super(R.layout.score_center_item_advance_goal, data);
        _context = context;
        String[] motionTypeList = context.getResources().getStringArray(R.array.stat_type_list);
        _statTypeList = Arrays.asList(motionTypeList);
    }


    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, final GameStatusBean goalInfoBean) {

        TextView tvHostStatName = baseViewHolder.findView(R.id.tv_host_name);
        TextView tvGuestStatName = baseViewHolder.findView(R.id.tv_guest_name);
        TextView tvHostStatScore = baseViewHolder.findView(R.id.tv_host_score);
        TextView tvGuestStatScore = baseViewHolder.findView(R.id.tv_guest_score);
        if(goalInfoBean == null) {
            return;
        }

        tvHostStatName.setText(_statTypeList.get(goalInfoBean.getType()));
        tvGuestStatName.setText(_statTypeList.get(goalInfoBean.getType()));
        tvHostStatScore.setText(String.valueOf(goalInfoBean.getHost_goal()));
        tvGuestStatScore.setText(String.valueOf(goalInfoBean.getGuest_goal()));

        tvHostStatName.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                AppTrace.d(TAG, "click host");
                if(_callBack != null) {
                    _callBack.onAdd(goalInfoBean.getType(), true);
                }
                return true;
            }
        });

        tvGuestStatName.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                AppTrace.d(TAG, "click guest");
                if(_callBack != null) {
                    _callBack.onAdd(goalInfoBean.getType(), false);
                }
                return true;
            }
        });
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setListener(GoalEditCallback l) {
        _callBack = l;
    }

    public String hostTeamName;
    public String guestTeamName;

    public void setHostTeamName(String teamName) {
        hostTeamName = teamName;
    }

    public void setGuestTeamName(String teamName) {
        guestTeamName = teamName;
    }


    public interface GoalEditCallback {
        void onAdd(int index, boolean isHost);

    }
}
