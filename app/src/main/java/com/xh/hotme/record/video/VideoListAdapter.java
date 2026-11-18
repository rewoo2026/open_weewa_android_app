package com.xh.hotme.record.video;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseVH;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.databinding.VideoListItemContentBinding;
import com.xh.hotme.databinding.VideoListItemHeadBinding;
import com.xh.hotme.utils.AdapterHeadUtils;
import com.xh.hotme.utils.GlideUtil;
import com.xh.hotme.utils.VideoUtil;

import java.util.ArrayList;

/**
 * date   : 2023/6/19
 * desc   :
 */
public class VideoListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final ArrayList<CameraVideoListBean.VideosBean> items;
    private final ArrayList<Integer> _itemTypes;
    private OnItemClick onItemClick;

    public static final int ITEM_VIDEO = 0;
    public static final int ITEM_TIME = 1;

    public interface OnItemClick {
        void onItemClick(int position);
    }

    public VideoListAdapter(ArrayList<CameraVideoListBean.VideosBean> items, ArrayList<Integer> itemTypes) {
        this.items = items;
        _itemTypes = itemTypes;
    }

    public void setOnItemClickListener(OnItemClick onItemClick) {
        this.onItemClick = onItemClick;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == ITEM_TIME) {
            return new MyHeadViewHolder(VideoListItemHeadBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
            ));
        } else {
            return new MyViewHolder(VideoListItemContentBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
            ));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return _itemTypes.get(position);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof MyViewHolder) {
            ((MyViewHolder) holder).update(items.get(position), position);
        } else if(holder instanceof MyHeadViewHolder) {
            ((MyHeadViewHolder) holder).update(items.get(position), position);
        }
    }

    /**
     * Description: HeadViewHolder 标题ViewHolder
     */
    static class MyHeadViewHolder extends BaseVH<CameraVideoListBean.VideosBean, VideoListItemHeadBinding> {
        public MyHeadViewHolder(VideoListItemHeadBinding viewBinding) {
            super(viewBinding);
        }

        @Override
        public void update(CameraVideoListBean.VideosBean data, int position) {
            viewBinding.extraPadding.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            viewBinding.tvTopTitle.setText(AdapterHeadUtils.getHeadTitle(data));
        }
    }

    /**
     * Description: 普通ViewHolder
     */
    class MyViewHolder extends BaseVH<CameraVideoListBean.VideosBean, VideoListItemContentBinding> {
        public MyViewHolder(VideoListItemContentBinding viewBinding) {
            super(viewBinding);
        }

        @Override
        public void update(CameraVideoListBean.VideosBean data, int position) {
            viewBinding.toptitle.getRoot().setVisibility(View.GONE);
            viewBinding.toptitle.tvTopTitle.setVisibility(View.GONE);

            if(!TextUtils.isEmpty(data.path)) {
                GlideUtil.loadRoundedCorner(
                    HotmeApplication.getContext(),
                    BluetoothManager.getCameraFileUrl(data.path + "/" + data.thumb),
                    viewBinding.ivLogo,
                    10,
                    R.mipmap.video_default_cover
                );
                viewBinding.tvTitle.setText(data.competition_name);
                //全场视频: 2     精彩集锦: 6     进球视频: 3
                int totalVideoNumber = data.getVideoTotalCount();
                int highlightNumber = data.highlights_number + data.home_goals + data.away_goals + data.ai_highlights_number;
                int playbackNumber = data.full_video_number + data.playback_number;
                viewBinding.tvVideoNum.setText(VideoUtil.VIDEO_TYPE_STR_ALL_VIDEOS + ":" + totalVideoNumber);
                viewBinding.tvGoodNum.setText(VideoUtil.VIDEO_TYPE_STR_HIGH_LIGHT + ":" + highlightNumber);
                viewBinding.tvGoalNum.setText(VideoUtil.VIDEO_TYPE_STR_PLAY_BACK + ":" + playbackNumber);
                viewBinding.getRoot().setOnClickListener(v -> {
                    if(null != onItemClick) {
                        onItemClick.onItemClick(position);
                    }
                });
                viewBinding.tvVideoNum.setBackgroundColor(Color.TRANSPARENT);
                viewBinding.tvGoodNum.setBackgroundColor(Color.TRANSPARENT);
                viewBinding.tvGoalNum.setBackgroundColor(Color.TRANSPARENT);
                viewBinding.tvTitle.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }
}
