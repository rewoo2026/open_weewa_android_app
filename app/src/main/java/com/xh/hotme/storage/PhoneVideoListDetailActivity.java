package com.xh.hotme.storage;

import static com.xh.hotme.HotmeApplication.getContext;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.gson.Gson;
import com.liulishuo.filedownloader.upload.backend.obs.ObsConstant;
import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.BaseVH;
import com.xh.hotme.bean.CameraVideoDetailListBean;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bean.CompetitionVideoRequestBean;
import com.xh.hotme.bean.HmCompetition;
import com.xh.hotme.bean.HmVideo;
import com.xh.hotme.bean.HmVideoUploadProgress;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.databinding.ActivityPhoneVideoListDetailBinding;
import com.xh.hotme.databinding.VideoDetailItemContentBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.listener.IApiCallback;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.me.setmeal.utils.SetMealCheckManager;
import com.xh.hotme.me.setmeal.utils.SetMealTimeManager;
import com.xh.hotme.storage.filetransfer.DownloadTaskCompletedEvent;
import com.xh.hotme.storage.filetransfer.TaskManager;
import com.xh.hotme.storage.filetransfer.TaskModel;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.FileUtil;
import com.xh.hotme.utils.GlideUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.ListUtils;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.ShareUtil;
import com.xh.hotme.utils.StringUtils;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.VideoUtil;
import com.xh.hotme.widget.GridSpacingItemDecoration;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * date   : 2023/6/12
 * desc   :相机视频 详情
 */

public class PhoneVideoListDetailActivity extends BaseActivity<ActivityPhoneVideoListDetailBinding> {
    public static final String EXTRA_BEAN = "bean";

    protected static final String TAG = PhoneVideoListDetailActivity.class.getSimpleName();
    final ArrayList<CameraVideoDetailListBean.VideosBean> _filteredVideoList = new ArrayList<>();
    boolean _selecting;//选泽显示
    final ArrayList<CameraVideoDetailListBean.VideosBean> _selectedBeans = new ArrayList<>();
    final ArrayList<String> _selectedVideoPaths = new ArrayList<>();
    long _selectedVideoSize;
    CameraVideoListBean.VideosBean _deviceData;

    int videoType = VideoUtil.INT_VIDEO_FILTER_TYPE_ALL_VIDEOS;

    List<CameraVideoDetailListBean.VideosBean> _videoList = new ArrayList<>();

    VideoNameEditDialog _newEditDialog;

    private static final class UploadResult {
        boolean competitionOk = false;
        String competitionErrMsg = null;
        int notExist = 0;
        int alreadyUploaded = 0;
        int started = 0;
        int failed = 0;
        List<Runnable> pendingTasks = new ArrayList<>();
        Map<String, String> remoteVideoIds = new HashMap<>();
    }

    private UploadResult _uploadResult;

    @Override
    protected void initView() {
        String videoStr = getIntent().getStringExtra(EXTRA_BEAN);
        _deviceData = GsonUtils.GsonToBean(videoStr, CameraVideoListBean.VideosBean.class);
        viewBinding.tvTitle.setText(_deviceData.competition_name);
        viewBinding.ivBack.setOnClickListener(v -> {
            finish();
        });

        viewBinding.tvSelectAll.setOnClickListener(v -> showSelectView());
        viewBinding.ivAllSelect.setOnClickListener(v -> showSelectView());
        viewBinding.topDel.tvSelectAll.setOnClickListener(v -> selectAll());
        viewBinding.topDel.ivDelAllSelect.setOnClickListener(v -> selectAll());
        viewBinding.topDel.ivCLose.setOnClickListener(v -> {
            _selecting = false;
            viewBinding.topDel.lltDel.setVisibility(View.GONE);
            viewBinding.lltBottom.getRoot().setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        });


        viewBinding.category.setOnCheckedChangeListener((group, checkedId) -> {
            if(checkedId == R.id.categoryAll) {
                videoType = VideoUtil.INT_VIDEO_FILTER_TYPE_ALL_VIDEOS;
            } else if(checkedId == R.id.categoryTop) {
                videoType = VideoUtil.INT_VIDEO_FILTER_TYPE_HIGH_LIGHT;
            } else if(checkedId == R.id.categoryPlayback) {
                videoType = VideoUtil.INT_VIDEO_TYPE_PLAY_BACK;
            } else {
                videoType = VideoUtil.INT_VIDEO_FILTER_TYPE_ALL_VIDEOS;
            }
            hideSelect();
            filterList();
        });
        viewBinding.lltBottom.getRoot().setVisibility(View.GONE);

        //隐藏下载按钮
        viewBinding.lltBottom.tabDownload.setVisibility(View.GONE);
        //隐藏服务器下载按钮
        viewBinding.lltBottom.tabDownloadToPhone.setVisibility(View.GONE);

        viewBinding.lltBottom.tabDelete.setOnClickListener(v -> {
            if(_selectedBeans.isEmpty()) {
                ToastUtil.s(getString(R.string.info_select_video_for_delete));
            } else {
                showConfirmDialog(
                    "",
                    getString(R.string.title_delete_select_video),
                    getString(R.string.ok),
                    getString(R.string.cancel),
                    new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            startDelete();
                            return true;
                        }
                    }
                );
            }
        });
        viewBinding.lltBottom.tabShare.setText(getString(R.string.share_to_wechat));
        viewBinding.lltBottom.tabShare.setOnClickListener(v -> {
            AppTrace.d(TAG, "分享");
            if(_selectedBeans.size() == 1) {
                shareVideo();
            } else if(_selectedBeans.isEmpty()) {
                ToastUtil.s(getString(R.string.info_select_wechat_file));
            } else {
                ToastUtil.s(getString(R.string.info_only_support_single_file));
            }
        });

        viewBinding.lltBottom.tabRename.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                AppTrace.d(TAG, "重命名");
                if(_selectedBeans.size() == 1) {
                    String videoName = _selectedBeans.get(0).name;
                    if(_newEditDialog == null) {
                        _newEditDialog = new VideoNameEditDialog(PhoneVideoListDetailActivity.this);
                        _newEditDialog.setCallback(new VideoNameEditDialog.ICallback() {
                            @Override
                            public void onCancel() {
                            }

                            @Override
                            public void onConfirm(String name) {
                                startRename(name);
                            }
                        }).onInit();
                        _newEditDialog.setTitle("重新命名");
                    }
                    _newEditDialog.setContent(videoName);
                    _newEditDialog.showOnCenter(40);
                }
                return true;
            }
        });

        viewBinding.lltBottom.tabUpload.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                AppTrace.d(TAG, "上传到服务器11");
                AppTrace.d(TAG, "asx开始上传");
                if(_selectedBeans.isEmpty()) {
                    ToastUtil.s(getString(R.string.info_select_video_for_upload));
                    return true;
                }

                if(!LoginManager.validToken()) {
                    MobileLoginActivity.start(PhoneVideoListDetailActivity.this);
                    return true;
                }
                if(NetworkManager.getImpl().isConnectedSoftAp()) {
                    ToastUtil.s(getString(R.string.info_switch_avalible_network_for_camera_softap));
                    return true;
                }
                boolean isNetValid = NetUtil.isConnectedAvailableNetwork();
                if(!isNetValid) {
                    ToastUtil.s(getString(R.string.info_switch_avalible_network));
                    return true;
                }


                AppTrace.d(TAG, "看下设备信息，都有啥"+ _deviceData);
                SetMealCheckManager.checkByDeviceId(PhoneVideoListDetailActivity.this, new SetMealCheckManager.CheckSetMealCallback() {
                    @Override
                    public void onSuccess() {

                        showConfirmDialog(
                                "",
                                "添加至服务器上传列表",
                                getString(R.string.ok),
                                getString(R.string.cancel),
                                new ClickGuard.GuardedOnClickListener() {
                                    @Override
                                    public boolean onClicked() {
                                        AppTrace.d(TAG, "begin to upload file");
                                        if(FileDownloadUtils.getObsConfig() == null){
                                            AppTrace.d(TAG, "get obs info");
                                            getObsInfo();
                                        } else {
                                            uploadWorkflowStep1_addCompetition();
                                        }
                                        return true;
                                    }
                                }
                        );
                    }
                },_deviceData.deviceId);


                return true;
            }
        });

    }


    private void filterList() {
        if(ListUtils.isValid(_videoList)) {
            _filteredVideoList.clear();
            for(int i = 0; i < _videoList.size(); i++) {
                _videoList.get(i).deviceId = _deviceData.deviceId;
                if(videoType == VideoUtil.INT_VIDEO_FILTER_TYPE_ALL_VIDEOS) {
                    _filteredVideoList.add(_videoList.get(i));
                } else if(videoType == VideoUtil.INT_VIDEO_FILTER_TYPE_HIGH_LIGHT) {
                    if(_videoList.get(i).type == VideoUtil.INT_VIDEO_FILTER_TYPE_HIGH_LIGHT
                        || _videoList.get(i).type == VideoUtil.INT_VIDEO_FILTER_TYPE_HOST_GOAL
                        || _videoList.get(i).type == VideoUtil.INT_VIDEO_FILTER_TYPE_GUEST_GOAL
                    ) {
                        _filteredVideoList.add(_videoList.get(i));
                    }
                } else if(videoType == VideoUtil.INT_VIDEO_FILTER_TYPE_PLAY_BACK) {
                    if(_videoList.get(i).type == VideoUtil.INT_VIDEO_FILTER_TYPE_PLAY_BACK
                        || _videoList.get(i).type == VideoUtil.INT_VIDEO_FILTER_TYPE_FULL_VIDEOS
                    ) {
                        _filteredVideoList.add(_videoList.get(i));
                    }
                }
            }
            if(_filteredVideoList.isEmpty()) {
                viewBinding.emptyView.getRoot().setVisibility(View.VISIBLE);
            } else {
                viewBinding.emptyView.getRoot().setVisibility(View.GONE);
            }
            adapter.notifyDataSetChanged();
        }
    }


    private void hideSelect() {
        releaseData();
        _selecting = false;
        viewBinding.topDel.lltDel.setVisibility(View.GONE);
        viewBinding.lltBottom.getRoot().setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void showSelectView() {
        _selecting = true;
        _selectedVideoSize = 0;
        viewBinding.topDel.tvDelTitle.setText(getString(R.string.select_all));
        _selectedBeans.clear();
        _selectedVideoPaths.clear();
        viewBinding.topDel.lltDel.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
        upTopIvSelect();
    }

    private void selectAll() {
        releaseData();
        if(viewBinding.topDel.tvSelectAll.getText().equals(getString(R.string.select_all))) {
            _selectedBeans.addAll(_filteredVideoList);
            for(int i = 0; i < _selectedBeans.size(); i++) {
                CameraVideoDetailListBean.VideosBean data = _selectedBeans.get(i);
                _selectedVideoSize += data.size;
                _selectedVideoPaths.add(VideoUtil.getVideoFilePath(data));
            }
        }

        adapter.notifyDataSetChanged();
        upTopIvSelect();
        showBottom();

    }

    private void upTopIvSelect() {
        viewBinding.topDel.tvSelectAll.setText(getString(R.string.select_all));
        viewBinding.topDel.tvSelectAll.setTextColor(getResources().getColor(R.color.orange));
        viewBinding.topDel.ivDelAllSelect.setImageResource(R.mipmap.ic_unselect1);
        if(!_filteredVideoList.isEmpty() && _selectedBeans.size() == _filteredVideoList.size()) {
            viewBinding.topDel.ivDelAllSelect.setImageResource(R.mipmap.ic_select);
            viewBinding.topDel.tvSelectAll.setText(getString(R.string.select_all_cancel));
            viewBinding.topDel.tvSelectAll.setTextColor(getResources().getColor(R.color.orange));
        }
    }


    private void releaseData() {
        _selectedBeans.clear();
        _selectedVideoSize = 0;
        _selectedVideoPaths.clear();
    }

    RAdapter adapter;

    @Override
    protected void initData() {
        _videoList = AppFileUtil.loadDeviceVideoJson(_deviceData);
        _filteredVideoList.addAll(_videoList);

        viewBinding.recycleView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        viewBinding.recycleView.addItemDecoration(new GridSpacingItemDecoration(2, DensityUtil.dip2px(10), false));
        viewBinding.recycleView.setAdapter(adapter = new RAdapter());
    }

    private class RAdapter extends RecyclerView.Adapter<CVDVh> {
        @NonNull
        @Override
        public CVDVh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            // 创建普通的item
            return new CVDVh(VideoDetailItemContentBinding.inflate(LayoutInflater.from(getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull CVDVh holder, int position) {
            holder.update(_filteredVideoList.get(position), position);
        }

        @Override
        public int getItemCount() {
            return _filteredVideoList.size();
        }
    }

    private class CVDVh extends BaseVH<CameraVideoDetailListBean.VideosBean, VideoDetailItemContentBinding> {

        public CVDVh(VideoDetailItemContentBinding viewBinding) {
            super(viewBinding);
        }

        @Override
        public void update(CameraVideoDetailListBean.VideosBean model, int position) {
            if(model == null) return;

            if(_selecting) {
                viewBinding.ivPlayIcon.setVisibility(View.GONE);
                viewBinding.ivSelectIcon.setVisibility(View.VISIBLE);
                viewBinding.ivSelectIcon.setBackgroundResource(R.mipmap.ic_unselect);
                if(_selectedBeans.contains(_filteredVideoList.get(position))) {
                    viewBinding.ivSelectIcon.setBackgroundResource(R.mipmap.ic_select);
                }
            } else {
                viewBinding.ivPlayIcon.setVisibility(View.VISIBLE);
                viewBinding.ivSelectIcon.setVisibility(View.GONE);
            }
            viewBinding.tvTitle.setText(model.name);
            if(model.duration > 0) {
                viewBinding.tvTime.setText(TimeUtil.intToTimeMin(model.duration));
            } else {
                viewBinding.tvTime.setText("0");
            }
            viewBinding.ivLogo.setCornerRadius(20);
            String filePath = VideoUtil.getThumbFilePath(model);
            GlideUtil.loadRoundedCorner(HotmeApplication.getContext(), filePath, viewBinding.ivLogo, 10, R.mipmap.video_default_cover);

            viewBinding.getRoot().setOnClickListener(v -> {
                if(_selecting) {
                    if(_selectedBeans.contains(_filteredVideoList.get(position))) {
                        _selectedBeans.remove(_filteredVideoList.get(position));
                        _selectedVideoPaths.remove(VideoUtil.getVideoFilePath(model));
                        _selectedVideoSize -= _filteredVideoList.get(position).size;
                    } else {
                        _selectedBeans.add(_filteredVideoList.get(position));
                        _selectedVideoSize += _filteredVideoList.get(position).size;
                        _selectedVideoPaths.add(VideoUtil.getVideoFilePath(model));
                    }
                    adapter.notifyItemChanged(position);
                    showBottom();
                } else if(model.downloading) {
                    ToastUtil.s(getString(R.string.info_file_downloading));
                } else {
                    String filePath1 = VideoUtil.getVideoFilePath(model);
                    File videoPath = new File(filePath1);
                    if(!videoPath.exists()) {
                        ToastUtil.s(getString(R.string.info_file_isnt_exist_or_deleted));
                        return;
                    }
                    if(model.type == VideoUtil.INT_VIDEO_FILTER_TYPE_PLAY_BACK) {
                        VideoPlaySuperActivity.start(PhoneVideoListDetailActivity.this, _deviceData, model);
                    } else {
                        VideoPlayActivity.start(PhoneVideoListDetailActivity.this, filePath1);
                    }
                }
            });
        }
    }

    private void setButtonEnable(TextView view, boolean isEnable, int enableResId, int disableResId) {
        view.setEnabled(isEnable);
        int resId = isEnable ? enableResId : disableResId;
        Drawable dra = getDrawable(resId);
        dra.setBounds(0, 0, dra.getIntrinsicWidth(), dra.getIntrinsicHeight());
        view.setCompoundDrawables(null, dra, null, null);
    }

    private void showBottom() {
        System.out.println("showBottom");
        viewBinding.topDel.tvDelTitle.setText("选择" + _selectedBeans.size() + "个视频");
        if(!_selectedBeans.isEmpty()) {
            viewBinding.lltBottom.getRoot().setVisibility(View.VISIBLE);
            viewBinding.topDel.tvDelTitle.setText("选择" + _selectedBeans.size() + "个视频");

            viewBinding.lltBottom.tabRename.setEnabled(_selectedBeans.size() == 1);
            viewBinding.lltBottom.tabShare.setEnabled(_selectedBeans.size() == 1);
        } else {
            viewBinding.lltBottom.getRoot().setVisibility(View.GONE);
        }
        upTopIvSelect();
    }

    private void startRename(String newName) {
        DialogUtil.showDialog("");

        new Thread(() -> {
            boolean renameOk = true;
            String renameErrMsg = null;
            do {
                CameraVideoDetailListBean.VideosBean videoBean = _selectedBeans.get(0);

                // check conflict
                for(CameraVideoDetailListBean.VideosBean video : _filteredVideoList) {
                    if(video == videoBean) continue;
                    if(video.name.equals(videoBean.name)) {
                        renameOk = false;
                        renameErrMsg = "已存在同名视频, 请换一个名称";
                        break;
                    }
                }

                // break if conflict
                if(!renameOk) {
                    break;
                }

                // set name
                videoBean.name = newName;
                for(CameraVideoDetailListBean.VideosBean video : _filteredVideoList) {
                    if(video.link.equals(videoBean.link)) {
                        video.name = newName;
                        break;
                    }
                }
                for(CameraVideoDetailListBean.VideosBean video : _videoList) {
                    if(video.link.equals(_selectedBeans.get(0).link)) {
                        video.name = newName;
                        break;
                    }
                }
            } while(false);

            // show result
            boolean finalRenameOk = renameOk;
            String finalRenameErrMsg = renameErrMsg;
            runOnUiThread(() -> {
                if(finalRenameOk) {
                    adapter.notifyDataSetChanged();
                    AppFileUtil.saveDeviceVideoJson(_deviceData, _videoList);
                } else {
                    ToastUtil.s(finalRenameErrMsg);
                }
                DialogUtil.dismissDialog();
            });
        }).start();
    }

    private void startDelete() {
        showLoading();

        new Thread() {
            @Override
            public void run() {
                // delete files
                for(String path : _selectedVideoPaths) {
                    FileUtil.deleteFile(path);
                }
                try {
                    Thread.sleep(100);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }

                viewBinding.topDel.tvDelTitle.post(() -> {
                    viewBinding.topDel.tvDelTitle.setText("选择");

                    // save device video json
                    AppFileUtil.saveDeviceVideoJson(_deviceData, _filteredVideoList);

                    // 如果没有视频了, 删掉device+path信息, 否则还需要更新视频数量
                    if(_filteredVideoList.size() == _selectedBeans.size()) {
                        // delete directory
                        FileUtil.deleteFile(VideoUtil.getVideoDirPath(_deviceData));

                        // delete related cached jsons
                        FileUtil.deleteFile(AppFileUtil.getDeviceVideoJsonFile(_deviceData));
                        AppFileUtil.removeDeviceBean(_deviceData);

                        // finish this activity
                        finish();
                    } else {
                        // adjust video count
                        for(CameraVideoDetailListBean.VideosBean bean : _selectedBeans) {
                            switch(bean.type) {
                                case Constants.VIDEO_CLASSIFY_FULL:
                                    _deviceData.full_video_number--;
                                    break;
                                case Constants.VIDEO_CLASSIFY_PLAYBACK:
                                    _deviceData.playback_number--;
                                    break;
                                case Constants.VIDEO_CLASSIFY_HIGHLIGHTS:
                                    _deviceData.highlights_number--;
                                    break;
                                case Constants.VIDEO_CLASSIFY_GUEST_GOAL:
                                    _deviceData.away_goals--;
                                    break;
                                case Constants.VIDEO_CLASSIFY_HOST_GOAL:
                                    _deviceData.home_goals--;
                                    break;
                            }
                        }
                        AppFileUtil.replaceDeviceBean(_deviceData);

                        // remove selected from data list
                        _filteredVideoList.removeAll(_selectedBeans);
                        _selectedBeans.clear();
                        _selectedVideoPaths.clear();

                        // update video json
                        AppFileUtil.saveDeviceVideoJson(_deviceData, _filteredVideoList);

                        // update list
                        adapter.notifyDataSetChanged();

                        // dismiss dialog
                        dismissLoading();
                    }
                });

            }
        }.start();
    }

    /*
     *  保存比赛Id到本地记录
     */
    private void syncCompetitionInfoToLocal(String competitionId) {
        // 得到一级列表
        List<CameraVideoListBean.VideosBean> listData = AppFileUtil.loadAllDevicesJson();
        VideoUtil.listSorting(listData);

        // 更新当前的bean
        for(int i = 0; i < listData.size(); i++) {
            CameraVideoListBean.VideosBean videosBean = listData.get(i);
            if(_deviceData.deviceId.equalsIgnoreCase(videosBean.deviceId) &&
                _deviceData.competition_name.equalsIgnoreCase(videosBean.competition_name) &&
                _deviceData.path.equalsIgnoreCase(videosBean.path)
            ) {
                videosBean.competition_id = competitionId;
                break;
            }
        }
        AppFileUtil.saveAllDevicesJson(listData);

        // 更新二级列表
        for(CameraVideoDetailListBean.VideosBean bean : _videoList) {
            bean.competitionId = competitionId;
        }
        AppFileUtil.saveDeviceVideoJson(_deviceData, _videoList);
    }

    private void syncVideoInfoToLocal(CameraVideoDetailListBean.VideosBean localVideoBean, String VideoId, String domainId, int videoStatus) {
        for(int i = 0; i < _videoList.size(); i++) {
            CameraVideoDetailListBean.VideosBean videosBean = _videoList.get(i);
            if(localVideoBean.deviceId.equalsIgnoreCase(videosBean.deviceId) && localVideoBean.link.equalsIgnoreCase(videosBean.link)) {
                videosBean.videoId = VideoId;
                videosBean.domainId = domainId;
                videosBean.videoStatus = videoStatus;
                break;
            }
        }
        AppFileUtil.saveDeviceVideoJson(_deviceData, _videoList);
    }

    private void uploadWorkflowStep1_addCompetition() {
        AppTrace.d(TAG, "uploadWorkflowStep1_addCompetition");
        // 先确认filedownloader服务已启动
        if(!TaskManager.getImpl().isReady()) {
            ToastUtil.s(getString(R.string.info_download_server_not_lunched));
            return;
        }

        _uploadResult = new UploadResult();
        if(TextUtils.isEmpty(_deviceData.competition_id)) {
            HmCompetition requestBean = new HmCompetition();
            requestBean.setDeviceId(_deviceData.deviceId);
            requestBean.setCompetitionName(_deviceData.competition_name);
            long time = TimeUtil.str2Millis(_deviceData.time);
            String formatTime = TimeUtil.millis2YMDHMS(time);
            requestBean.setCompetitionTime(formatTime);
            requestBean.setMotionType(_deviceData.motion_type);
            requestBean.setCompetitionStatus(Constants.COMPETITION_STOP);
            requestBean.setPlace(_deviceData.address);
            if(_deviceData.player_num == 5) {
                requestBean.setPlaceType(1);
            } else if(_deviceData.player_num == 11) {
                requestBean.setPlaceType(3);
            } else {
                requestBean.setPlaceType(2);
            }
            requestBean.setHomeTeamName(_deviceData.host_team);
            requestBean.setAwayTeamName(_deviceData.guest_team);
            requestBean.setHomeTeamScore(_deviceData.home_goals);
            requestBean.setAwayTeamScore(_deviceData.away_goals);

            AppTrace.d(TAG, "uploadWorkflowStep1_addCompetition, upload competition info: " + new Gson().toJson(requestBean));
            ApiUtil.addCompetition(requestBean, new IGenericApiCallback<HmCompetition>() {
                @Override
                public void onSuccess(HmCompetition data) {
                    _deviceData.competition_id = data.getCompetitionId();
                    syncCompetitionInfoToLocal(data.getCompetitionId());
                    AppTrace.d(TAG, "uploadWorkflowStep1_addCompetition success, competition id: " + data.getCompetitionId());

                    // 如果比赛没有缩略图, 先修复他
                    _uploadResult.competitionOk = true;
                    if(TextUtils.isEmpty(data.getCompetitionImage())) {
                        uploadWorkflowStep2_fixCompetition();
                    } else {
                        uploadWorkflowStep3_queryCompetitionVideos();
                    }
                }

                @Override
                public void onFail(String code, String message) {
                    AppTrace.d(TAG, "uploadWorkflowStep1_addCompetition, add competition info fail, code=" + code + ", message=" + message);
                    _uploadResult.competitionOk = false;
                    _uploadResult.competitionErrMsg = message;
                    if(code.equals("1001")) {
                        SetMealTimeManager.queryAll(PhoneVideoListDetailActivity.this);
                    }
                    uploadWorkflowStep9_wrapUp();
                }

                @Override
                public void onFinish() {
                }
            });
        } else {
            ApiUtil.getCompetitionView(_deviceData.competition_id, new IGenericApiCallback<HmCompetition>() {
                @Override
                public void onSuccess(HmCompetition data) {
                    _uploadResult.competitionOk = true;
                    syncCompetitionInfoToLocal(data.getCompetitionId());
                    if(TextUtils.isEmpty(data.getCompetitionImage())) {
                        uploadWorkflowStep2_fixCompetition();
                    } else {
                        uploadWorkflowStep3_queryCompetitionVideos();
                    }
                }

                @Override
                public void onFail(String code, String message) {
                    _uploadResult.competitionOk = false;
                    _uploadResult.competitionErrMsg = message;
                    if(code.equals("1001")) {
                        SetMealTimeManager.queryAll(PhoneVideoListDetailActivity.this);
                    }
                    uploadWorkflowStep9_wrapUp();
                }

                @Override
                public void onFinish() {

                }
            });
        }
    }

    private void uploadWorkflowStep2_fixCompetition() {
        // upload competition, 上传完成的时候在VideoUploadListener里会做一次update把缩略图补上,就
        // 达到了fix competition的目的
        _uploadResult.pendingTasks.add(() -> {
            File thumbFile = new File(VideoUtil.getThumbFilePath(_deviceData));
            if(thumbFile.exists()) {
                TaskManager.getImpl().addThumbTask(
                    _deviceData,
                    thumbFile.getAbsolutePath(),
                    thumbFile.length()
                );
            }
        });

        // next step
        uploadWorkflowStep3_queryCompetitionVideos();
    }

    private void uploadWorkflowStep3_queryCompetitionVideos() {
        CompetitionVideoRequestBean bean = new CompetitionVideoRequestBean();
        bean.setCompetitionId(_deviceData.competition_id);
        bean.setPageNum(1);
        bean.setPageSize(9999);
        ApiUtil.getCompetitionVideo(bean, new IGenericApiCallback<List<HmVideo>>() {
            @Override
            public void onSuccess(List<HmVideo> data) {
                for(HmVideo v : data) {
                    _uploadResult.remoteVideoIds.put(v.getVideoName(), v.getVideoId());
                    _uploadResult.remoteVideoIds.put(StringUtils.lastPathComponent(v.getVideoUrl()), v.getVideoId());
                }
            }

            @Override
            public void onFail(String code, String message) {
            }

            @Override
            public void onFinish() {
                uploadWorkflowStep4_parallelUploadVideos();
            }
        });
    }

    private void uploadWorkflowStep4_parallelUploadVideos() {
        for(CameraVideoDetailListBean.VideosBean videosBean : _selectedBeans) {
            if(!VideoUtil.isVideoFileExist(videosBean)) {
                AppTrace.d(TAG, "uploadWorkflowStep3_parallelUploadVideos, video " + videosBean.path + " doesn't exist! skip!");
                _uploadResult.notExist++;
            } else {
                String videoId = videosBean.videoId;
                if(TextUtils.isEmpty(videoId)) {
                    String fn = StringUtils.lastPathComponent(videosBean.link);
                    if(_uploadResult.remoteVideoIds.containsKey(fn)) {
                        videoId = _uploadResult.remoteVideoIds.get(fn);
                    }
                }
                if(!TextUtils.isEmpty(videoId)) {
                    videosBean.videoId = videoId;
                    AppTrace.d(TAG, "uploadWorkflowStep3_parallelUploadVideos, has video id, " + videosBean.link + ", query status first");
                    uploadWorkflowStep5_queryVideoStatus(videosBean);
                } else {
                    AppTrace.d(TAG, "uploadWorkflowStep3_parallelUploadVideos, no video id, " + videosBean.link + ", get video id first");
                    uploadWorkflowStep6_addVideo(videosBean);
                }
            }
        }

        // try wrap up
        uploadWorkflowStep9_wrapUp();
    }

    private void uploadWorkflowStep5_queryVideoStatus(CameraVideoDetailListBean.VideosBean videosBean) {
        ApiUtil.queryVideo(videosBean.videoId, new IGenericApiCallback<HmVideo>() {
            @Override
            public void onSuccess(HmVideo data) {
                if(data != null) {
                    String domainId = "";
                    videosBean.path = VideoUtil.getVideoFilePath(videosBean);
                    videosBean.setVideoId(data.getVideoId());
                    if(data.getHmVideoUploadProgress() != null) {
                        domainId = data.getHmVideoUploadProgress().getDomainId();
                    }
                    videosBean.setDomainId(domainId);
                    syncVideoInfoToLocal(videosBean, data.getVideoId(), domainId, data.getVideoStatus());

                    if(data.getVideoStatus() != Constants.STATUS_COMPLETE) {
                        uploadWorkflowStep7_addVideoStatus(videosBean);
                    } else {
                        AppTrace.d(TAG, "uploadWorkflowStep4_queryVideoStatus, video " + videosBean.path + " is already uploaded, skip");
                        _uploadResult.alreadyUploaded++;
                        uploadWorkflowStep9_wrapUp();
                    }
                }
            }

            @Override
            public void onFail(String code, String message) {
                if(code.equals("602")) {
                    // 视频不存在, 说明视频之前可能被删掉了
                    uploadWorkflowStep6_addVideo(videosBean);
                } else {
                    AppTrace.d(TAG, "查询视频失败：" + message);
                    _uploadResult.failed++;
                    uploadWorkflowStep9_wrapUp();
                }
            }

            @Override
            public void onFinish() {

            }
        });
    }

    private void uploadWorkflowStep7_addVideoStatus(CameraVideoDetailListBean.VideosBean videosBean) {
        ApiUtil.addVideoUploadStatus(videosBean.videoId, new IGenericApiCallback<HmVideoUploadProgress>() {
            @Override
            public void onSuccess(HmVideoUploadProgress data) {
                syncVideoInfoToLocal(videosBean, data.getVideoId(), data.getDomainId(), data.getUploadStatus());
                uploadWorkflowStep8_startVideoUploadTask(videosBean);
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "上传视频失败：" + message);
                _uploadResult.failed++;
                uploadWorkflowStep9_wrapUp();
            }

            @Override
            public void onFinish() {

            }
        });
    }

    private void uploadWorkflowStep6_addVideo(CameraVideoDetailListBean.VideosBean videosBean) {
        HmVideo video = new HmVideo();
        video.setDeviceId(_deviceData.deviceId);
        video.setCompetitionId(_deviceData.competition_id);
        if(_deviceData.competition_type == 0) {
            _deviceData.competition_type = Constants.COMPETITION_TYPE_RECORD;
        }
        video.setCompetitionType(_deviceData.competition_type);
        video.setVideoDuration(videosBean.duration);
        video.setVideoSize(videosBean.size);
        video.setVideoWidth(videosBean.video_width);
        video.setVideoHeight(videosBean.video_height);
        video.setVideoClassify(videosBean.type);
        video.setVideoStatus(Constants.STATUS_STARTING);
        video.setUploadType(Constants.UPLOAD_MANUAL);   //手动上传是2； 1是自动上传

        // 添加视频文件录制开始时间, 通过获取文件的创建时间来得到
        videosBean.path = VideoUtil.getVideoFilePath(videosBean);
        File f = new File(videosBean.path);
        if(f.exists() && f.isFile()) {
            long ctime = FileUtil.getCreationTime(f);
            if(ctime != -1) {
                video.setVideoTime(TimeUtil.millis2YMDHMS(ctime));
            }
        }

        // 使用文件名作为视频名称, 避免重复上传, 但是要看看这个有没有副作用
        video.setVideoName(StringUtils.lastPathComponent(videosBean.path));

        AppTrace.d(TAG, "upload video info: " + new Gson().toJson(video));

        ApiUtil.addVideo(video, new IGenericApiCallback<HmVideo>() {
            @Override
            public void onSuccess(HmVideo data) {
                if(data != null) {
                    String domainId = "";
                    videosBean.setVideoId(data.getVideoId());
                    if(data.getHmVideoUploadProgress() != null) {
                        domainId = data.getHmVideoUploadProgress().getDomainId();
                    }
                    videosBean.setDomainId(domainId);
                    syncVideoInfoToLocal(videosBean, data.getVideoId(), domainId, data.getVideoStatus());

                    if(TextUtils.isEmpty(domainId)) {
                        uploadWorkflowStep7_addVideoStatus(videosBean);
                    } else {
                        uploadWorkflowStep8_startVideoUploadTask(videosBean);
                    }
                }
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "上传视频失败：" + message);
                _uploadResult.failed++;
                uploadWorkflowStep9_wrapUp();
            }

            @Override
            public void onFinish() {

            }
        });

    }

    private void uploadWorkflowStep8_startVideoUploadTask(CameraVideoDetailListBean.VideosBean videosBean) {
        // create task and start
        _uploadResult.pendingTasks.add(() -> {
            TaskModel taskModel = TaskManager.getImpl().addTask(videosBean, _deviceData.competition_name);
            if(!TextUtils.isEmpty(videosBean.thumb)) {
                File thumbFile = new File(VideoUtil.getThumbFilePath(videosBean));
                if(thumbFile.exists()) {
                    TaskManager.getImpl().addThumbTask(
                        videosBean,
                        _deviceData.competition_name,
                        thumbFile.getAbsolutePath(),
                        thumbFile.length()
                    );
                }
            }
            AppTrace.d(TAG, "videoWorkflowStep3_startVideoUploadTask: videoId=" + videosBean.videoId + ", upload task started! task id: " + taskModel.getId());
        });
        _uploadResult.started++;
        uploadWorkflowStep9_wrapUp();
    }

    private void uploadWorkflowStep9_wrapUp() {
        // if competition operation is failed, show toast
        if(!_uploadResult.competitionOk) {
            runOnUiThread(() -> ToastUtil.s(_uploadResult.competitionErrMsg));
            return;
        }

        // if not all video has a feedback, wait
        if(_selectedBeans.size() != _uploadResult.alreadyUploaded + _uploadResult.notExist + _uploadResult.started + _uploadResult.failed) {
            return;
        }

        // show feedback
        if(_uploadResult.started == 0) {
            boolean isDownloading = false;
            if(_selectedBeans.size() != 0) {
                for (CameraVideoDetailListBean.VideosBean data : _selectedBeans) {
                    if(data.downloading) {
                        isDownloading = true;
                    }
                }
            }
            if(isDownloading) {
                showConfirmDialog(getString(R.string.tip), getString(R.string.video_download_unfinished_please_download_again));
            } else {
                showConfirmDialog(getString(R.string.tip), getString(R.string.video_already_upload_please_select_other_videos));
            }
        } else if(_uploadResult.alreadyUploaded == 0 && _uploadResult.notExist == 0 && _uploadResult.failed == 0) {
            runOnUiThread(() -> ToastUtil.s(String.format("%d个视频开始上传", _uploadResult.started)));
            startPendingUploadTasks();
        } else {
            showConfirmDialog(
                    getString(R.string.tip),
                    getString(R.string.video_already_upload_continue_upload_other_videos),
                getString(R.string.ok),
                getString(R.string.cancel),
                new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        startPendingUploadTasks();
                        return true;
                    }
                }
            );
        }

        // remove selection
        runOnUiThread(this::hideSelect);
    }

    private void startPendingUploadTasks() {
        if(_uploadResult != null) {
            for(Runnable r : _uploadResult.pendingTasks) {
                r.run();
            }
        }
    }

    private void shareVideo() {
        CameraVideoDetailListBean.VideosBean data = _selectedBeans.get(0);
        String path = VideoUtil.getVideoFilePath(data);
        File videoFile = new File(path);
        if(!videoFile.exists()) {
            ToastUtil.s(getString(R.string.info_file_isnt_exist_or_deleted));
            return;
        }

        long fileSize = FileUtil.getFileSize(path);
        if(fileSize >= 200 * 1024 * 1024) {
            ToastUtil.s(getString(R.string.info_share_file_size_limit_200M));
            AppTrace.d(TAG, "分享失败，文件大于200M");
            return;
        }
        ShareUtil.shareLocalVideoFileToWx(PhoneVideoListDetailActivity.this, path);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadTaskCompleted(DownloadTaskCompletedEvent event) {
        // only handle video event
        TaskModel model = event.getModel();
        if(model.getType() != TaskModel.TYPE_VIDEO) return;

        // 这里是更新内存中的数据, json的更新在PhoneVideoFragment里做了
        for(CameraVideoDetailListBean.VideosBean videoBean : _videoList) {
            if(!TextUtils.isEmpty(videoBean.videoId) && videoBean.videoId.equals(model.getVideoId())) {
                if(videoBean.downloading) {
                    videoBean.downloading = false;
                }
                break;
            }
        }
    }

    private void getObsInfo(){
        ApiUtil.getObsInfo(new IApiCallback() {
            @Override
            public void onSuccess() {
                AppTrace.d(TAG, "getObsInfo onSuccess");
                uploadWorkflowStep1_addCompetition();
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "getObsInfo fail: code=" +  code+", message=" +message);
                ToastUtil.s(getString(R.string.get_obs_server_info_fail));
            }

            @Override
            public void onFinish() {

            }
        });

    }
}
