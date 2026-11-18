package com.xh.hotme.storage;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.base.BaseFragment;
import com.xh.hotme.base.BaseVH;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.SwitchType;
import com.xh.hotme.bean.AIEnableAnalyseBean;
import com.xh.hotme.bean.CompetitionListRequestBean;
import com.xh.hotme.bean.CompetitionListResultBean;
import com.xh.hotme.bean.HmCompetition;
import com.xh.hotme.bean.CompetitionVideoRequestBean;
import com.xh.hotme.bean.HmVideo;
import com.xh.hotme.bean.UserInfoBean;
import com.xh.hotme.bluetooth.event.DeviceUnbindEvent;
import com.xh.hotme.bluetooth.event.ICloudFileUpdateEvent;
import com.xh.hotme.bluetooth.event.LoginEvent;
import com.xh.hotme.databinding.FragmentGeneralVideoListBinding;
import com.xh.hotme.databinding.ItemCloudVideoBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.utils.AdapterHeadUtils;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.GlideUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.ListUtils;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.VideoUtil;
import com.xh.hotme.widget.TitleItemDecoration;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by GJK on 2018/11/9.
 */
@IEventBus
public class CloudVideoFragment extends BaseFragment<FragmentGeneralVideoListBinding> {
    private static final String TAG = CloudVideoFragment.class.getSimpleName();

    private RAdapter adapter;

    int _pageIndex = 0;

    boolean _isRequest = false;
    boolean _isRequestError = false;

    final List<HmCompetition> _dataList = new ArrayList<>();
    private boolean _hasMore = true;

    @Override
    protected SwitchType getSwitchType() {
        return SwitchType.VIEW_PAGER;
    }

    @Override
    protected void initView() {
        _viewBinding.rv.setLayoutManager(new LinearLayoutManager(getContext()));
        _viewBinding.rv.setAdapter(adapter = new RAdapter());
        _viewBinding.rv.setEmptyView(_viewBinding.lltEmpty);
        _viewBinding.lltEmpty.setVisibility(View.VISIBLE);

        _viewBinding.lltEmpty.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _pageIndex = 0;
                getCompetitionList();
                return false;
            }
        });
        _viewBinding.refreshLayout.setOnRefreshListener(refreshLayout -> {
            boolean isNetValid = NetUtil.isConnectedAvailableNetwork();
            if(!isNetValid) {
                AppTrace.w(TAG, getString(R.string.info_switch_avalible_network));
                ToastUtil.s(getString(R.string.info_switch_avalible_network));
                _viewBinding.refreshLayout.finishRefresh();
                return;
            }
            _pageIndex = 0;
            _hasMore = true;
            getCompetitionList();
        });
        _viewBinding.refreshLayout.setOnLoadMoreListener(refreshLayout -> {
            getCompetitionList();
        });
        _viewBinding.rv.addItemDecoration(new TitleItemDecoration(requireContext(), new TitleItemDecoration.TitleDecorationCallback() {
            @Override
            public boolean isHeadItem(int curPosition, int nextposition) {
                return AdapterHeadUtils.shouldShowDateHeader(_dataList, nextposition);
            }

            @NonNull
            @Override
            public String getHeadTitle(int position) {
                return AdapterHeadUtils.getHeadTitle(_dataList.get(position));
            }
        }));
    }

    @Override
    protected void initData() {

        getCompetitionList();

    }

    private void removeEmptyCompetition() {
        int size = _dataList.size();
        for(int i = size - 1; i >= 0; i--) {
            HmCompetition bean = _dataList.get(i);
            if(bean.getVideoTotalCount() <= 0) {
                _dataList.remove(i);
            }
        }
    }

    private void getCompetitionList() {
        UserInfoBean userInfoBean = LoginManager.getUserLoginInfo();
        if(userInfoBean == null) {
            _dataList.clear();
            adapter.notifyDataSetChanged();
            _viewBinding.refreshLayout.finishRefresh();
            return;
        }

        if(!_hasMore) {
            _viewBinding.refreshLayout.finishRefresh();
            return;
        }

        if(_isRequest) {
            return;
        }
        _pageIndex++;
        _isRequest = true;

        CompetitionListRequestBean requestBean = new CompetitionListRequestBean();
        requestBean.setPageNum(_pageIndex);
        requestBean.setPageSize(Constants.COMPETITION_PAGE_NUMBER);

        ApiUtil.getCompetitionViewList(requestBean, new IGenericApiCallback<CompetitionListResultBean>() {
            @Override
            public void onSuccess(CompetitionListResultBean data) {
                _isRequest = false;
                _isRequestError = false;
                MainHandler.runOnUiThread(() -> {
                    try {
                        if(_pageIndex == 1) {
                            _dataList.clear();
                        }
                        if(data != null && data.getList() != null && !data.getList().isEmpty()) {
                            _dataList.addAll(data.getList());
                            removeEmptyCompetition();
                        }

                        _hasMore = data.getHasNextPage();
                        adapter.notifyDataSetChanged();

                        // finish
                        if(_pageIndex == 1) {
                            _viewBinding.refreshLayout.finishRefresh();
                        } else {
                            if(_hasMore) {
                                _viewBinding.refreshLayout.finishLoadMore(true);
                            } else {
                                _viewBinding.refreshLayout.finishLoadMoreWithNoMoreData();
                            }
                        }

                        // 如果结果太少, 触发下一页, 因为smartRefreshLayout貌似不自动触发
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if(!_isRequestError && _dataList.size() < 10 && _hasMore) {
                                getCompetitionList();
                            }
                        });

                    } catch(Throwable e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onFail(String code, String message) {
                _isRequest = false;
                _isRequestError = true;

                // finish
                _viewBinding.refreshLayout.finishRefresh();
            }

            @Override
            public void onFinish() {
                _isRequest = false;
            }
        });
    }

    private class RAdapter extends RecyclerView.Adapter<BaseVH<HmCompetition, ItemCloudVideoBinding>> {

        @NonNull
        @Override
        public BaseVH<HmCompetition, ItemCloudVideoBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VideoVH(ItemCloudVideoBinding.inflate(LayoutInflater.from(getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull BaseVH<HmCompetition, ItemCloudVideoBinding> holder, int position) {
            holder.update(_dataList.get(position), position);
        }

        @Override
        public int getItemCount() {
            return ListUtils.getListSize(_dataList);
        }
    }

    private class VideoVH extends BaseVH<HmCompetition, ItemCloudVideoBinding> {
        public VideoVH(ItemCloudVideoBinding viewBinding) {
            super(viewBinding);
        }

        @Override
        public void update(HmCompetition bean, int position) {
            viewBinding.toptitle.getRoot().setVisibility(View.GONE);
            if(AdapterHeadUtils.shouldShowDateHeader(_dataList, position)) {
                viewBinding.toptitle.tvTopTitle.setText(AdapterHeadUtils.getHeadTitle(_dataList.get(position)));
                viewBinding.toptitle.getRoot().setVisibility(View.VISIBLE);
                viewBinding.toptitle.extraPadding.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            }
            GlideUtil.loadRoundedCorner(
                HotmeApplication.getContext(),
                bean.getCompetitionImage(),
                viewBinding.ivLogo,
                10,
                R.mipmap.video_default_cover
            );

            viewBinding.tvTitle.setText(bean.getCompetitionName());
            try {
                viewBinding.tvVideoNum.setText(VideoUtil.VIDEO_TYPE_STR_ALL_VIDEOS + ":" + bean.getVideoTotalCount());
            } catch(Exception e) {
                e.printStackTrace();
            }
            try {
                viewBinding.tvGoodNum.setText(VideoUtil.VIDEO_TYPE_STR_HIGH_LIGHT + ":" + bean.getVideoHighlightCount());
            } catch(Exception e) {
                e.printStackTrace();
            }
            try {
                viewBinding.tvGoalNum.setText(VideoUtil.VIDEO_TYPE_STR_PLAY_BACK + ":" + bean.getVideoReplayCount());
            } catch(Exception e) {
                e.printStackTrace();
            }

            viewBinding.getRoot().setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    getCompetitionVideo(bean, position);

                    return true;
                }
            });

            viewBinding.tvVideoNum.setBackgroundColor(Color.TRANSPARENT);
            viewBinding.tvGoodNum.setBackgroundColor(Color.TRANSPARENT);
            viewBinding.tvGoalNum.setBackgroundColor(Color.TRANSPARENT);
            viewBinding.tvTitle.setBackgroundColor(Color.TRANSPARENT);

            updateUI(bean);
        }
        private void updateUI(HmCompetition bean){
            if(!isAdded()){
                return;
            }
            if(bean.getAnalyseStatus() == null) {  //未处理
                viewBinding.tvAnalyseStatus.setText(getString(R.string.ai_analyse_status_un_start));
                viewBinding.tvAnalyse.setVisibility(View.VISIBLE);
                viewBinding.tvAnalyse.setText(getString(R.string.ai_analyse));
                viewBinding.tvAnalyse.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        doAIAnalyse(bean);
                        return true;
                    }
                });
            } else if(bean.getAnalyseStatus()==0){  //未处理
                viewBinding.tvAnalyseStatus.setText(getString(R.string.ai_analyse_status_queue));
                viewBinding.tvAnalyse.setVisibility(View.GONE);
                viewBinding.tvAnalyse.setOnClickListener(null);
            } else if(bean.getAnalyseStatus()==1){  //处理中
                viewBinding.tvAnalyseStatus.setText(getString(R.string.ai_analyse_status_processing));
                viewBinding.tvAnalyse.setVisibility(View.GONE);
                viewBinding.tvAnalyse.setOnClickListener(null);
            } else if(bean.getAnalyseStatus()==2){  //处理结束
                viewBinding.tvAnalyseStatus.setText(getString(R.string.ai_analyse_status_processe_ok));
                viewBinding.tvAnalyse.setVisibility(View.GONE);
                viewBinding.tvAnalyse.setOnClickListener(null);
            } else if(bean.getAnalyseStatus()==3){  //处理失败
                viewBinding.tvAnalyseStatus.setText(getString(R.string.ai_analyse_status_processe_fail));
                viewBinding.tvAnalyse.setVisibility(View.VISIBLE);
                viewBinding.tvAnalyse.setText(getString(R.string.ai_analyse_again));
                viewBinding.tvAnalyse.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {

                        doAIAnalyse(bean);
                        return true;
                    }
                });
            }
        }

        private void doAIAnalyse(HmCompetition bean){
            ApiUtil.canEnableAnalyseTask(bean.getCompetitionId(), new IGenericApiCallback<AIEnableAnalyseBean>() {
                @Override
                public void onSuccess(AIEnableAnalyseBean data) {

                    if(data.data){
                        ApiUtil.enableAnalyseTask(bean.getCompetitionId(), new IGenericApiCallback<AIEnableAnalyseBean>() {
                            @Override
                            public void onSuccess(AIEnableAnalyseBean data) {
                                if(data.data){
                                    bean.setAnalyseStatus(1);
                                    updateUI(bean);

                                    //修改成功后刷新下整个列表
                                    _pageIndex = 0;
                                    _hasMore = true;
                                    getCompetitionList();
                                }
                            }

                            @Override
                            public void onFail(String code, String message) {
                                ToastUtil.s(message);
                            }

                            @Override
                            public void onFinish() {

                            }
                        });
                    } else {
                        if(TextUtils.isEmpty(data.msg)){
                            if(isAdded()) {
                                ToastUtil.s(getString(R.string.ai_analyse_status_processe_fail_reason));
                            }
                        } else {
                            ToastUtil.s(data.msg);
                        }

                    }
                }

                @Override
                public void onFail(String code, String message) {

                }

                @Override
                public void onFinish() {

                }
            });
        }

    }
    

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceUnbind(DeviceUnbindEvent event) {
        AppTrace.d(TAG, "onDeviceUnbind");
        _pageIndex = 0;
        getCompetitionList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUpdate(LoginEvent event) {
        AppTrace.e(TAG, "onUpdate for login");

        if(event != null) {
            _pageIndex = 0;
            getCompetitionList();
        }
    }


    private void getCompetitionVideo(HmCompetition bean, int position) {

        CompetitionVideoRequestBean requestBean = new CompetitionVideoRequestBean();
        requestBean.setCompetitionId(bean.getCompetitionId());
        AppTrace.d(TAG, "data大小=" + bean.getCompetitionId());
        ApiUtil.getCompetitionVideo(requestBean, new IGenericApiCallback<List<HmVideo>>() {
            @Override
            public void onSuccess(List<HmVideo> data) {
                if(data != null && !data.isEmpty()) {
                    AppTrace.d(TAG, "data大小=" + data.size());
                    VideoManager.getImpl().setVideoList(data);
                    Intent intent = new Intent(getActivity(), CloudVideoListDetailActivity.class);
//                    intent.putExtra(CloudVideoListDetailActivity.EXTRA_VIDEO_LIST,  GsonUtils.GsonString(data) );
                    intent.putExtra(CloudVideoListDetailActivity.EXTRA_COMPETITION, GsonUtils.GsonString(bean) );
                    requireActivity().startActivity(intent);
                } else {
                    if(bean.getCompetitionType() == Constants.COMPETITION_TYPE_RECORD) {
                        ApiUtil.deleteCompetition(bean.getCompetitionId(), null);
                    } else {
                        ApiUtil.deleteCompetitionLive(bean.getCompetitionId(), null);
                    }
                    _dataList.remove(bean);
                    adapter.notifyItemRemoved(position);
                }
            }

            @Override
            public void onFail(String code, String message) {
                if(isAdded()) {
                    ToastUtil.s(getString(R.string.info_get_video_fail));
                }
            }

            @Override
            public void onFinish() {

            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogin(LoginEvent event) {
        AppTrace.d(TAG, "onLogin");
        if(event != null) {
            _dataList.clear();
            _pageIndex = 0;
            getCompetitionList();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshCompetition(ICloudFileUpdateEvent event) {
        if(event != null) {
            if(!_dataList.isEmpty()) {
                int size = _dataList.size();
                for(int i = 0; i < size; i++) {
                    HmCompetition bean = _dataList.get(i);
                    if(bean.getCompetitionId().equalsIgnoreCase(event.id)) {
                        if(event.allVideoNumber == 0) {
                            // 如果是日期头, 只通知被删item有布局问题, 需要来一个dataset通知
                            boolean isHead = AdapterHeadUtils.shouldShowDateHeader(_dataList, i);
                            _dataList.remove(i);
                            if(isHead) {
                                adapter.notifyDataSetChanged();
                            } else {
                                adapter.notifyItemRemoved(i);
                            }
                        } else {
                            bean.setVideoHighlightCount(event.highlightNumber);
                            bean.setVideoTotalCount(event.allVideoNumber);
                            bean.setVideoReplayCount(event.playbackNumber);
                            adapter.notifyItemChanged(i);
                        }
                        break;
                    }
                }
            }
        }
    }
}
