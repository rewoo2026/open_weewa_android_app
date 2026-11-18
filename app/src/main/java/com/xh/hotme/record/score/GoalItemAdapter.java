package com.xh.hotme.record.score;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseQuickAdapter;
import com.xh.hotme.base.BaseViewHolder;
import com.xh.hotme.R;
import com.xh.hotme.bean.GameStatusListBean;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.TimeUtil;

import java.util.List;

/**
 * date   : 2023/6/19
 * desc   :
 */
public class GoalItemAdapter extends BaseQuickAdapter<GameStatusListBean, BaseViewHolder> {
    private final static String TAG = GoalItemAdapter.class.getSimpleName();

    final Context _context;

    GoalEditCallback _callBack;


    public GoalItemAdapter(Context context, List data) {
        super(R.layout.score_center_item_goal, data);
        _context = context;
    }


    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, final GameStatusListBean goalInfoBean) {

        int position = baseViewHolder.getPosition();

        TextView tvNumber = baseViewHolder.findView(R.id.tvNumber);
        TextView tvTeamName = baseViewHolder.findView(R.id.tv_team_name);
        TextView tvTime = baseViewHolder.findView(R.id.tvTime);
        TextView tvEdit = baseViewHolder.findView(R.id.tvEdit);
        TextView tvDel = baseViewHolder.findView(R.id.tvDel);

        tvNumber.setText(String.valueOf(position + 1));

        if(goalInfoBean.getHost() == 1) {
            tvTeamName.setText(hostTeamName);
        } else {
            tvTeamName.setText(guestTeamName);
        }


        tvTime.setText(TimeUtil.intToTimeMin(goalInfoBean.getTime()));

        tvEdit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                AppTrace.d(TAG, "click edit");
                if(_callBack != null) {
                    _callBack.onEdit(position, goalInfoBean);
                }
                return true;
            }
        });

        tvDel.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                AppTrace.d(TAG, "click del");
                if(_callBack != null) {
                    _callBack.onDelete(position, goalInfoBean);
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
        void onEdit(int index, GameStatusListBean goalInfoBean);

        void onDelete(int index, GameStatusListBean goalInfoBean);
    }
}
