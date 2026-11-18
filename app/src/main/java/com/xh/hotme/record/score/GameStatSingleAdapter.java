package com.xh.hotme.record.score;

import android.content.Context;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xh.hotme.base.BaseQuickAdapter;
import com.xh.hotme.base.BaseViewHolder;
import com.xh.hotme.R;
import com.xh.hotme.bean.GameStatusListBean;
import com.xh.hotme.record.bean.DotStates;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.ScoreCenterUtil;
import com.xh.hotme.utils.TimeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * date   : 2023/6/19
 * desc   :
 */

public class GameStatSingleAdapter extends BaseQuickAdapter<GameStatusListBean, BaseViewHolder> {
    private final static String TAG = GameStatSingleAdapter.class.getSimpleName();

    final Context _context;

    GoalEditCallback _callBack;

    List<DotStates> _customerList;

    public void setCustomerList(List<DotStates> customerList) {
        if (this._customerList == null) {
            this._customerList = new ArrayList<>();
        }
        this._customerList.clear();

        this._customerList.addAll(customerList);
    }


    public GameStatSingleAdapter(Context context, List data) {
        super(R.layout.score_center_item_state, data);
        _context = context;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, final GameStatusListBean goalInfoBean) {

        int position = baseViewHolder.getPosition();

        TextView tvStateName = baseViewHolder.findView(R.id.tv_state_name);
        TextView tvTime = baseViewHolder.findView(R.id.tvTime);
        TextView tvEdit = baseViewHolder.findView(R.id.tvEdit);
        TextView tvDel = baseViewHolder.findView(R.id.tvDel);

        if(!TextUtils.isEmpty(goalInfoBean.getName())){
            tvStateName.setText(goalInfoBean.getName());
        } else {
            if(!TextUtils.isEmpty(ScoreCenterUtil.getNameById(goalInfoBean.getType()))) {
                tvStateName.setText(ScoreCenterUtil.getNameById(goalInfoBean.getType()));
            } else {
                //需要确认是否是自定义类型
                if (_customerList != null && !_customerList.isEmpty()) {
                    for (DotStates dotBean : _customerList) {
                        if (dotBean.getId() == goalInfoBean.getType()) {
                            tvStateName.setText(dotBean.getName());
                            break;
                        }
                    }
                }
            }
        }

        tvTime.setText(TimeUtil.intToTimeMin(goalInfoBean.getTime()));

        tvEdit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                AppTrace.d(TAG, "click edit");
                if (_callBack != null) {
                    _callBack.onEdit(position, goalInfoBean);
                }
                return true;
            }
        });

        tvDel.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                AppTrace.d(TAG, "click del");
                if (_callBack != null) {
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