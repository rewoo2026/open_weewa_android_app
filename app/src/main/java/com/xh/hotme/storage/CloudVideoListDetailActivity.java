package com.xh.hotme.storage;

import static com.xh.hotme.HotmeApplication.getContext;

import android.Manifest;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.gson.Gson;
import com.liulishuo.filedownloader.upload.backend.obs.ObsBackend;
import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.DeleteObjectsRequest;
import com.obs.services.model.DeleteObjectsResult;
import com.obs.services.model.KeyAndVersion;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.BaseVH;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bean.CameraVideoDetailListBean;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bean.GetVideoUrlBean;
import com.xh.hotme.bean.GetVideoUrlsBean;
import com.xh.hotme.bean.HmCompetition;
import com.xh.hotme.bean.HmVideo;
import com.xh.hotme.bean.VersionResultBean;
import com.xh.hotme.bean.VideoNameUploadBean;
import com.xh.hotme.bluetooth.event.CameraFilesDownloadedEvent;
import com.xh.hotme.bluetooth.event.ICloudFileUpdateEvent;
import com.xh.hotme.databinding.ActivityCloudVideoListDetailBinding;
import com.xh.hotme.databinding.VideoDetailItemContentBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.IApiCallback;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.dialog.ShareDialog;
import com.xh.hotme.main.MainActivity;
import com.xh.hotme.storage.filetransfer.TaskManager;
import com.xh.hotme.storage.filetransfer.TaskModel;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.GlideUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.ListUtils;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.VideoUtil;
import com.xh.hotme.widget.GridSpacingItemDecoration;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.FormBody;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * date   : 2023/12/02
 * desc   :云存储相机视频 详情
 */

public class CloudVideoListDetailActivity extends BaseActivity<ActivityCloudVideoListDetailBinding> {
    public static final String EXTRA_VIDEO_LIST = "videos";
    public static final String EXTRA_COMPETITION = "competition";
    final ArrayList<HmVideo> _filteredVideoList = new ArrayList<>();
    boolean _selecting;//选泽显示
    final ArrayList<HmVideo> _selectedBeans = new ArrayList<>();
    long _selectedVideoSize;

    int videoType = VideoUtil.INT_VIDEO_FILTER_TYPE_ALL_VIDEOS;

    List<HmVideo> _videoList = new ArrayList<>();

    // delete reuslt
    private static final class DeleteResult {
        boolean videoDeleteOk = false;
        String videoDeleteErrMsg = null;
        final List<DeleteObjectsResult.ErrorResult> obsErrs = new ArrayList<>();
        final List<Exception> obsExceptions = new ArrayList<>();
    }

    private DeleteResult _deleteResult = null;

    private static final class RenameResult {
        boolean nameConflict = false;
        boolean renameOk = false;
        String renameErrMsg = null;
    }

    private RenameResult _renameResult = null;

    private static final class ObsDeletePrerequisites {
        String bucket = null;
        ObsClient client = null;
        final List<KeyAndVersion> keys = new ArrayList<>();
    }

    VideoNameEditDialog _newEditDialog;

    private HmCompetition _cbean;
    private String _newName = null;

    private String strSelectAll = "";

    private String strCancelSelectAll = "";

    @Override
    protected void initView() {
        String competition = getIntent().getStringExtra(EXTRA_COMPETITION);
        _cbean =  GsonUtils.GsonToBean(competition, HmCompetition.class);
//        String videos =  getIntent().getStringExtra(EXTRA_VIDEO_LIST);
//        _videoList = GsonUtils.fromJsonList (videos, HmVideo.class);
        _videoList = VideoManager.getImpl().getVideoList();
        if(_videoList != null) {
            _filteredVideoList.addAll(_videoList);
            VideoManager.getImpl().setVideoList(null);
        }

        viewBinding.tvTitle.setText(_cbean.getCompetitionName());
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
                videoType = VideoUtil.INT_VIDEO_TYPE_HIGH_LIGHT;
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
        //隐藏上传至服务器按钮
        viewBinding.lltBottom.tabUpload.setVisibility(View.GONE);

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
                            deleteWorkflowStep1_deleteCompetitionVideo();
                            return true;
                        }
                    }
                );
            }
        });

        viewBinding.lltBottom.tabShare.setOnClickListener(v -> {
            // check empty url
            HmVideo bean = _selectedBeans.get(0);

            // 先获取URL再分享弹窗
            ApiUtil.getVideoPlayUrl(bean.getVideoId(), new IGenericApiCallback<String>() {
                @Override
                public void onSuccess(String videoUrl) {
                    // show share dialog
                    ShareDialog shareDialog = new ShareDialog(CloudVideoListDetailActivity.this);

                    try {
                        shareDialog.setUrl(videoUrl);
                        shareDialog.setShareAsVideo(true);
                        shareDialog.setName(bean.getHmCompetition().getCompetitionName());
                        shareDialog.setSubName(bean.getVideoName());
                        shareDialog.show();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFail(String code, String msg) {
                    AppTrace.d(TAG, "getUrl error: code=" + code + ", msg=" + msg);
                    ToastUtil.s(getString(R.string.info_null_video_url_to_share));
                }

                @Override
                public void onFinish() {}


            });
        });

        viewBinding.lltBottom.tabRename.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                AppTrace.d(TAG, "重命名");
                if(_selectedBeans.size() == 1) {
                    String videoName = _selectedBeans.get(0).getVideoName();
                    if(_newEditDialog == null) {
                        _newEditDialog = new VideoNameEditDialog(CloudVideoListDetailActivity.this);
                        _newEditDialog.setCallback(new VideoNameEditDialog.ICallback() {
                            @Override
                            public void onCancel() {

                            }

                            @Override
                            public void onConfirm(String name) {
                                _newName = name;
                                _renameResult = new RenameResult();
                                renameWorkflowStep1_checkConflict();
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

        viewBinding.lltBottom.tabDownloadToPhone.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                AppTrace.d(TAG, "下载到手机");
                if(_selectedBeans.isEmpty()) {
                    ToastUtil.s(getString(R.string.info_select_video_for_download));
                    return true;
                }

                showConfirmDialog(
                    "",
                    "下载到手机",
                    getString(R.string.ok),
                    getString(R.string.cancel),
                    new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            startDownload();
                            return true;
                        }
                    }
                );

                return true;
            }
        });
        strSelectAll = getString(R.string.select_all);
        strCancelSelectAll = getString(R.string.select_all_cancel);
    }

    private CameraVideoListBean.VideosBean findDeviceBean(List<CameraVideoListBean.VideosBean> allDevices, HmVideo remoteBean) {
        for(CameraVideoListBean.VideosBean bean : allDevices) {
            if(bean.deviceId.equals(remoteBean.getDeviceId()) && bean.path.equals(remoteBean.getPath())) {
                return bean;
            }
        }
        return null;
    }

    private CameraVideoDetailListBean.VideosBean findVideoBean(List<CameraVideoDetailListBean.VideosBean> videos, HmVideo remoteBean) {
        if(videos == null) return null;
        for(CameraVideoDetailListBean.VideosBean bean : videos) {
            // 先比较video id, 没有的话比较名称
            if(!TextUtils.isEmpty(bean.videoId) && bean.videoId.equals(remoteBean.getVideoId())) {
                return bean;
            } else if(!TextUtils.isEmpty(bean.name) && bean.name.equals(remoteBean.getVideoName())) {
                return bean;
            }
        }
        return null;
    }

    private void startDownload() {
        showLoading();
        new Thread(() -> {
            // 因为这个界面本身就显示一个比赛下面的视频, 因为不可能出现新建多个bean的情况
            CameraVideoListBean.VideosBean deviceBean = null;
            List<CameraVideoListBean.VideosBean> allDevices = AppFileUtil.loadAllDevicesJson();
            List<CameraVideoDetailListBean.VideosBean> deviceVideos = null;
            List<HmVideo> downloadList = new ArrayList<>();
            int notReady = 0;
            int downloaded = 0;
            for(HmVideo bean : _selectedBeans) {
                // if no competition, ignore this
                if(bean.getHmCompetition() == null) continue;

                // if video not uploaded, ignore
                if(bean.getVideoStatus() != Constants.STATUS_COMPLETE) {
                    notReady++;
                    continue;
                }

                // get device bean
                if(deviceBean == null) {
                    deviceBean = findDeviceBean(allDevices, bean);
                }

                // if not found, need add to local
                if(deviceBean == null) {
                    // populate device bean
                    deviceBean = new CameraVideoListBean.VideosBean(bean);
                    allDevices.add(deviceBean);

                    // 既然device bean不存在, 那么即使device video文件存在我们也不要, 因为他可能是之前
                    // 的错误造成两者没有同步, 这里我们要认为视频就是没有
                    deviceVideos = new ArrayList<>();
                } else {
                    // device bean在的话, 那就载入视频列表
                    if(deviceVideos == null) {
                        deviceVideos = AppFileUtil.loadDeviceVideoJson(deviceBean);
                    }
                }

                // 已经存在了, 不用重复下载
                CameraVideoDetailListBean.VideosBean videoBean = findVideoBean(deviceVideos, bean);
                if(videoBean != null) {
                    downloaded++;
                    continue;
                }

                // increase video counter
                switch(bean.getVideoClassify()) {
                    case Constants.VIDEO_CLASSIFY_FULL:
                        deviceBean.full_video_number++;
                        break;
                    case Constants.VIDEO_CLASSIFY_HIGHLIGHTS:
                        deviceBean.highlights_number++;
                        break;
                    case Constants.VIDEO_CLASSIFY_HIGHLIGHTS_AI:
                        deviceBean.ai_highlights_number++;
                        break;
                    case Constants.VIDEO_CLASSIFY_PLAYBACK:
                        deviceBean.playback_number++;
                        break;
                    case Constants.VIDEO_CLASSIFY_GUEST_GOAL:
                        deviceBean.away_goals++;
                        break;
                    case Constants.VIDEO_CLASSIFY_HOST_GOAL:
                        deviceBean.home_goals++;
                        break;
                }

                // add for downloading
                downloadList.add(bean);
            }

            // toast
            if(downloadList.isEmpty()) {
                runOnUiThread(() -> {
                    dismissLoading();
                    ToastUtil.l("选中的视频因为某些原因(已下载,未上传完成等)无需下载");
                });
                return;
            } else {
                if(downloaded > 0 || notReady > 0) {
                    int finalDownloaded = downloaded;
                    int finalNotReady = notReady;
                    runOnUiThread(() -> ToastUtil.l(String.format("%d个视频已开始下载, 其他%d个视频因为某些原因(已下载,未上传完成等)无需下载",
                        downloadList.size(), finalDownloaded + finalNotReady)));
                } else {
                    runOnUiThread(() -> ToastUtil.s(getString(R.string.info_selected_video_downloading)));
                }
            }

            // check filedownloader service
            if(!TaskManager.getImpl().isReady()) {
                runOnUiThread(() -> {
                    dismissLoading();
                    ToastUtil.s(getString(R.string.info_download_server_not_lunched));
                });
                return;
            }

            List<String> videoIDS = new ArrayList<>();
            FormBody.Builder formBuilder = new FormBody.Builder();
            for(HmVideo bean : downloadList) {
                videoIDS.add(bean.getVideoId());
                formBuilder.add("videoIds", bean.getVideoId());
            }


            String apiUrl = SdkApi.get_encode_videos_url();

            List<CameraVideoDetailListBean.VideosBean> finalDeviceVideos = deviceVideos;
            CameraVideoListBean.VideosBean finalDeviceBean = deviceBean;
            OkHttpUtil.postFormData(apiUrl,formBuilder,null, new OkHttpCallbackDecode<GetVideoUrlsBean>(GetVideoUrlsBean.class) {
                @Override
                public void onDataSuccess(GetVideoUrlsBean data) {
                    List<String> encodeUrls = data.data;

                    // start download task
//                    for(HmVideo bean : downloadList) {
                    for (int i = 0; i < downloadList.size(); i++) {
                        // generate local bean
                        HmVideo bean = downloadList.get(i);
                        String videoUrl = encodeUrls.get(i);


                        CameraVideoDetailListBean.VideosBean localBean = new CameraVideoDetailListBean.VideosBean(bean);
                        localBean.downloading = true;

                        // start download task
                        TaskModel model = TaskManager.getImpl().addTask(bean, videoUrl);
                        if(model == null) continue;

                        // save download id and add to video list
                        finalDeviceVideos.add(localBean);

                        // start thumb task
                        if(!TextUtils.isEmpty(bean.getVideoImage())) {
                            TaskManager.getImpl().addThumbTask(bean);
                        }
                    }

                    // save local json
                    VideoUtil.listSorting(allDevices);
                    AppFileUtil.saveAllDevicesJson(allDevices);
                    AppFileUtil.saveDeviceVideoJson(finalDeviceBean, finalDeviceVideos);

                    //同步刷新通知
                    EventBus.getDefault().post(new CameraFilesDownloadedEvent());

                    // start competition task if needed
                    String localPath = VideoUtil.getThumbFilePath(finalDeviceBean);
                    String competitionImageUrl = downloadList.get(0).getHmCompetition().getCompetitionImage();
                    if(!new File(localPath).exists() && !TextUtils.isEmpty(competitionImageUrl)) {
                        TaskManager.getImpl().addThumbTask(downloadList.get(0), TaskModel.TYPE_COMPETITION_THUMB);
                    }

                    MainHandler.runOnUiThread(() -> {
                        hideSelect();
                        dismissLoading();
                    });
                }

                @Override
                public void onFailure(String code, String msg) {
                    AppTrace.d(TAG, "error: code=" + code + ", msg=" + msg);
                }
            });


        }).start();
    }

    private void filterList() {
        if(ListUtils.isValid(_videoList)) {
            _filteredVideoList.clear();
            for(int i = 0; i < _videoList.size(); i++) {
                if(videoType == VideoUtil.INT_VIDEO_FILTER_TYPE_ALL_VIDEOS) {
                    _filteredVideoList.add(_videoList.get(i));
                } else if(videoType == VideoUtil.INT_VIDEO_FILTER_TYPE_HIGH_LIGHT) {
                    if(_videoList.get(i).getVideoClassify() == VideoUtil.INT_VIDEO_FILTER_TYPE_HIGH_LIGHT
                        || _videoList.get(i).getVideoClassify() == VideoUtil.INT_VIDEO_FILTER_TYPE_HOST_GOAL
                        || _videoList.get(i).getVideoClassify() == VideoUtil.INT_VIDEO_FILTER_TYPE_GUEST_GOAL
                    ) {
                        _filteredVideoList.add(_videoList.get(i));
                    }
                } else if(videoType == VideoUtil.INT_VIDEO_FILTER_TYPE_PLAY_BACK) {
                    if(_videoList.get(i).getVideoClassify() == VideoUtil.INT_VIDEO_FILTER_TYPE_PLAY_BACK
                        || _videoList.get(i).getVideoClassify() == VideoUtil.INT_VIDEO_FILTER_TYPE_FULL_VIDEOS) {
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
        viewBinding.topDel.tvDelTitle.setText(strSelectAll);
        _selectedBeans.clear();
        viewBinding.topDel.lltDel.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
        upTopIvSelect();
    }

    private void selectAll() {
        releaseData();
        if(viewBinding.topDel.tvSelectAll.getText().equals(strSelectAll)) {
            _selectedBeans.addAll(_filteredVideoList);
            for(int i = 0; i < _selectedBeans.size(); i++) {
                HmVideo data = _selectedBeans.get(i);
                _selectedVideoSize += data.getVideoSize();
//                paths.add(getFilePath(data));
            }
        }

        adapter.notifyDataSetChanged();
        upTopIvSelect();
        showBottom();

    }

    private void upTopIvSelect() {
        viewBinding.topDel.tvSelectAll.setText(strSelectAll);
        viewBinding.topDel.tvSelectAll.setTextColor(getResources().getColor(R.color.orange));
        viewBinding.topDel.ivDelAllSelect.setImageResource(R.mipmap.ic_unselect1);
        if(!_filteredVideoList.isEmpty() && _selectedBeans.size() == _filteredVideoList.size()) {
            viewBinding.topDel.ivDelAllSelect.setImageResource(R.mipmap.ic_select);
            viewBinding.topDel.tvSelectAll.setText(strCancelSelectAll);
            viewBinding.topDel.tvSelectAll.setTextColor(getResources().getColor(R.color.orange));
        }
    }


    private void releaseData() {
        _selectedBeans.clear();
        _selectedVideoSize = 0;
    }

    RAdapter adapter;

    @Override
    protected void initData() {
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

    private class CVDVh extends BaseVH<HmVideo, VideoDetailItemContentBinding> {

        public CVDVh(VideoDetailItemContentBinding viewBinding) {
            super(viewBinding);
        }

        @Override
        public void update(HmVideo model, int position) {
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
            viewBinding.tvTitle.setText(model.getVideoName());
            if(model.getVideoDuration() > 0) {
                viewBinding.tvTime.setText(TimeUtil.intToTimeMin(model.getVideoDuration()));
            } else {
                viewBinding.tvTime.setText("0");
            }

            GlideUtil.loadRoundedCorner(
                HotmeApplication.getContext(),
                model.getVideoImage(),
                viewBinding.ivLogo,
                10,
                R.mipmap.video_default_cover
            );

            viewBinding.getRoot().setOnClickListener(v -> {
                if(_selecting) {
                    if(_selectedBeans.contains(_filteredVideoList.get(position))) {
                        _selectedBeans.remove(_filteredVideoList.get(position));
                        _selectedVideoSize -= _filteredVideoList.get(position).getVideoSize();
                    } else {
                        _selectedBeans.add(_filteredVideoList.get(position));
                        _selectedVideoSize += _filteredVideoList.get(position).getVideoSize();
                    }
                    adapter.notifyItemChanged(position);
                    showBottom();
                } else if(TextUtils.isEmpty(model.getVideoUrl()) || TaskManager.getImpl().isVideoUploading(model)) {
                    showHintDialog(getString(R.string.info_try_it_later_for_video_uploading));
                } else {
                    String videoID = model.getVideoId();

                    getVideoPlayUrl(videoID);
                }
            });
        }
    }

    private void getVideoPlayUrl(String videoID) {
        ApiUtil.getVideoPlayUrl(videoID, new IGenericApiCallback<String>() {
            @Override
            public void onSuccess(String data) {
                try {
                    Log.d("asx","asx返回数据" + data);
                    VideoPlayActivity.start(CloudVideoListDetailActivity.this, data);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(String code, String msg) {
                AppTrace.d(TAG, "error: code=" + code + ", msg=" + msg);
            }

            @Override
            public void onFinish() {}
        });
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

    private void deleteWorkflowStep3_wrapUp() {
        _filteredVideoList.removeAll(_selectedBeans);
        _selectedBeans.clear();

        // broadcast event
        int allVideoNumber = _filteredVideoList.size();
        int highlight = 0;
        int playback = 0;
        for(HmVideo HmVideo : _filteredVideoList) {
            if(HmVideo.getVideoClassify() == VideoUtil.INT_VIDEO_FILTER_TYPE_HIGH_LIGHT
                || HmVideo.getVideoClassify() == VideoUtil.INT_VIDEO_FILTER_TYPE_HOST_GOAL
                || HmVideo.getVideoClassify() == VideoUtil.INT_VIDEO_FILTER_TYPE_GUEST_GOAL
            ) {
                highlight++;
            } else if(HmVideo.getVideoClassify() == VideoUtil.INT_VIDEO_FILTER_TYPE_FULL_VIDEOS
                || HmVideo.getVideoClassify() == VideoUtil.INT_VIDEO_FILTER_TYPE_PLAY_BACK
            ) {
                playback++;
            }
        }
        ICloudFileUpdateEvent event = new ICloudFileUpdateEvent(_cbean.getCompetitionId(), allVideoNumber, highlight, playback);
        EventBus.getDefault().post(event);

        // update ui
        viewBinding.topDel.tvDelTitle.post(() -> {
            adapter.notifyDataSetChanged();
            dismissLoading();
            if(_filteredVideoList.isEmpty()) {
                finish();
            }
        });
    }

    private void renameWorkflowStep1_checkConflict() {
        DialogUtil.showDialog(getString(R.string.info_rename_ing));

        new Thread(() -> {
            HmVideo video = _selectedBeans.get(0);

            // check name conflict
            for(HmVideo bean : _videoList) {
                if(bean == video) continue;
                if(bean.getVideoName().equals(video.getVideoName())) {
                    _renameResult.nameConflict = true;
                    _renameResult.renameErrMsg = getString(R.string.info_change_name_for_the_same_name);
                    break;
                }
            }

            // go next
            if(_renameResult.nameConflict) {
                _renameResult.renameOk = false;
                renameWorkflowStep3_wrapUp();
            } else {
                renameWorkflowStep2_renameRemoteVideo();
            }
        }).start();
    }

    private void renameWorkflowStep2_renameRemoteVideo() {
        HmVideo bean = _selectedBeans.get(0);
        VideoNameUploadBean uploadBean = new VideoNameUploadBean();
        uploadBean.setVideoId(bean.getVideoId());
        uploadBean.setVideoName(_newName);
        ApiUtil.updateCompetitionVideoName(uploadBean, new IApiCallback() {
            @Override
            public void onSuccess() {
                AppTrace.d(TAG, "update success");
                _selectedBeans.get(0).setVideoName(_newName);
                for(HmVideo video : _filteredVideoList) {
                    if(video.getVideoId().equals(bean.getVideoId())) {
                        video.setVideoName(_newName);
                        break;
                    }
                }
                for(HmVideo video : _videoList) {
                    if(video.getVideoId().equals(bean.getVideoId())) {
                        video.setVideoName(_newName);
                        break;
                    }
                }
                adapter.notifyDataSetChanged();

                _renameResult.renameOk = true;
                renameWorkflowStep3_wrapUp();
            }

            @Override
            public void onFail(String code, String message) {
                _renameResult.renameOk = false;
                _renameResult.renameErrMsg = message;
                renameWorkflowStep3_wrapUp();
            }

            @Override
            public void onFinish() {
            }
        });
    }

    private void renameWorkflowStep3_wrapUp() {
        runOnUiThread(() -> {
            if(_renameResult.renameOk) {
                ToastUtil.s(getString(R.string.info_rename_success));
            } else {
                ToastUtil.s(_renameResult.renameErrMsg);
            }
            DialogUtil.dismissDialog();
        });
    }

    private void deleteWorkflowStep1_deleteCompetitionVideo() {
        if(_selectedBeans.isEmpty()) {
            return;
        }

        // loading dialog
        showLoading(getString(R.string.info_deleting));

        // prepare id list
        _deleteResult = new DeleteResult();
        ArrayList<String> idList = new ArrayList<>();
        for(int i = 0; i < _selectedBeans.size(); i++) {
            idList.add(_selectedBeans.get(i).getVideoId());
        }

        ApiUtil.deleteCompetitionVideo(idList, new IApiCallback() {
            @Override
            public void onSuccess() {
                try {
                    _deleteResult.videoDeleteOk = true;

                    // 现在不删obs的文件, 跳过
//                    deleteWorkflowStep2_deleteInObs();
                    deleteWorkflowStep3_wrapUp();
                } catch(Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(String code, String message) {
                AppTrace.d(TAG, "delete fail, code=" + code + ", msg=" + message);
                _deleteResult.videoDeleteOk = false;
                _deleteResult.videoDeleteErrMsg = message;
                deleteWorkflowStep3_wrapUp();
            }

            @Override
            public void onFinish() {
            }
        });
    }

    private void prepareObs(String url, Map<String, ObsDeletePrerequisites> delMap) {
        // get url triple
        ObsBackend.ObsUrlTriple t = ObsBackend.parse(url);
        if(!t.isValid()) return;

        // prepare
        String hash = t.bucket + t.host;
        ObsDeletePrerequisites pre = delMap.get(hash);
        if(pre == null) {
            pre = new ObsDeletePrerequisites();
            pre.bucket = t.bucket;
            delMap.put(hash, pre);
        }
        if(pre.client == null) {
            pre.client = ObsBackend.newClient(t.host);
        }
        pre.keys.add(new KeyAndVersion(t.key));
    }

    private void deleteWorkflowStep2_deleteInObs() {
        new Thread(() -> {
            // 这里的逻辑尽最大可能做了兼容, 可以处理视频位于不同的bucket, 不同的host等奇葩情况
            Map<String, ObsDeletePrerequisites> delMap = new HashMap<>();
            for(HmVideo bean : _selectedBeans) {
                if(!TextUtils.isEmpty(bean.getVideoUrl())) {
                    prepareObs(bean.getVideoUrl(), delMap);
                }
                if(!TextUtils.isEmpty(bean.getVideoImage())) {
                    prepareObs(bean.getVideoImage(), delMap);
                }
            }

            // 如果没有视频剩下了, 服务器那边会删除比赛, 那么我们也把比赛的缩略图删了干净!
            if(_selectedBeans.size() == _filteredVideoList.size()) {
                if(!TextUtils.isEmpty(_cbean.getCompetitionImage())) {
                    prepareObs(_cbean.getCompetitionImage(), delMap);
                }
                if(!TextUtils.isEmpty(_cbean.getWxShareImage())) {
                    prepareObs(_cbean.getWxShareImage(), delMap);
                }
            }

            // start delete
            for(ObsDeletePrerequisites pre : delMap.values()) {
                DeleteObjectsRequest request = new DeleteObjectsRequest();
                request.setBucketName(pre.bucket);
                request.setKeyAndVersions(pre.keys.toArray(new KeyAndVersion[0]));
                try {
                    DeleteObjectsResult result = pre.client.deleteObjects(request);
                    if(result.getErrorResults() != null) {
                        _deleteResult.obsErrs.addAll(result.getErrorResults());
                    }
                } catch(ObsException e) {
                    _deleteResult.obsExceptions.add(e);
                }
            }

            // end
            deleteWorkflowStep3_wrapUp();
        }).start();
    }
}
