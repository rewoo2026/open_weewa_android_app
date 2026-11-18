package com.xh.hotme.record.score;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseQuickViewBindingAdapter;
import com.xh.hotme.base.BaseViewBindingHolder;
import com.xh.hotme.bean.GameStatusListBean;
import com.xh.hotme.databinding.VideoDotListItemDotBinding;
import com.xh.hotme.record.bean.DotStates;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.TimeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * date   : 2023/6/19
 * desc   :
 */

public class VideoStatListAdapter extends BaseQuickViewBindingAdapter<GameStatusListBean, BaseViewBindingHolder<VideoDotListItemDotBinding>> {
    private final static String TAG = VideoStatListAdapter.class.getSimpleName();


    VideoStateCallback _callBack;

    List<DotStates> _customerList;

    String hostTeamName, guestTeamName;

    GameStatusListBean _selectedGameStatusListBean;

    public VideoStatListAdapter(Context context, List<GameStatusListBean> data) {
        super(R.layout.video_dot_list_item_dot, data);
    }

    public void setCustomerList(List<DotStates> customerList) {
        if (this._customerList == null) {
            this._customerList = new ArrayList<>();
        }
        this._customerList.clear();

        this._customerList.addAll(customerList);
    }

    public void setHostTeamName(String teamName) {
        hostTeamName = teamName;
    }

    public void setGuestTeamName(String teamName) {
        guestTeamName = teamName;
    }

    @Override
    protected void convert(@NonNull BaseViewBindingHolder<VideoDotListItemDotBinding> helper, final GameStatusListBean goalInfoBean) {
        int position = helper.getLayoutPosition();

        VideoDotListItemDotBinding baseViewHolder = helper.viewBind;

        baseViewHolder.tvStatName.setText(goalInfoBean.getName());
        baseViewHolder.tvStatTime.setText(TimeUtil.intToTimeMin2(goalInfoBean.getTime()));

        if (goalInfoBean.getHost() == 1) {  //主队
            if (!TextUtils.isEmpty(hostTeamName)) {
                baseViewHolder.tvTeamName.setText(hostTeamName);
            } else {
                baseViewHolder.tvTeamName.setText("主队");
            }
        } else {
            if (!TextUtils.isEmpty(guestTeamName)) {
                baseViewHolder.tvTeamName.setText(guestTeamName);
            } else {
                baseViewHolder.tvTeamName.setText("客队");
            }
        }

        baseViewHolder.getRoot().setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if (_callBack != null) {
                    _callBack.onClick(position, goalInfoBean);
                }

                return true;
            }
        });

        if(_selectedGameStatusListBean!=null && _selectedGameStatusListBean.getTime() == goalInfoBean.getTime()){
            baseViewHolder.getRoot().setSelected(true);
            baseViewHolder.tvStatName.setTextColor(getContext().getColor(R.color.white));
            baseViewHolder.tvStatTime.setTextColor(getContext().getColor(R.color.white));
            baseViewHolder.tvTeamName.setTextColor(getContext().getColor(R.color.white));
            baseViewHolder.ivPlay.setImageResource(R.mipmap.iv_video_dot_pause);
        } else {
            baseViewHolder.getRoot().setSelected(false);
            baseViewHolder.tvStatName.setTextColor(getContext().getColor(R.color.text_black_3D));
            baseViewHolder.tvStatTime.setTextColor(getContext().getColor(R.color.text_black_3D));
            baseViewHolder.tvTeamName.setTextColor(getContext().getColor(R.color.text_black_3D));
            baseViewHolder.ivPlay.setImageResource(R.mipmap.iv_video_dot_play);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setListener(VideoStateCallback l) {
        _callBack = l;
    }

    public void setSelectedGameStatusListBean(GameStatusListBean statusListBean) {
        _selectedGameStatusListBean = statusListBean;
    }


    @Override
    protected BaseViewBindingHolder getViewBinding(int viewType, LayoutInflater from, ViewGroup parent) {
        return new BaseViewBindingHolder(VideoDotListItemDotBinding.inflate(from, parent, false));
    }

    public interface VideoStateCallback {

        void onClick(int index, GameStatusListBean goalInfoBean);
    }
}