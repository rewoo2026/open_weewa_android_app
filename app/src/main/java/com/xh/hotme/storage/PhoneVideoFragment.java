package com.xh.hotme.storage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.liulishuo.filedownloader.util.FileDownloadUtils;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.BaseFragment;
import com.xh.hotme.base.BaseVH;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.RequireStoragePermission;
import com.xh.hotme.base.SwitchType;
import com.xh.hotme.bean.CameraVideoDetailListBean;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bean.CompetitionVideoRequestBean;
import com.xh.hotme.bean.HmCompetition;
import com.xh.hotme.bean.HmVideo;
import com.xh.hotme.bean.HmVideoUploadProgress;
import com.xh.hotme.bluetooth.event.CameraFilesDownloadedEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.databinding.FragmentGeneralVideoListBinding;
import com.xh.hotme.databinding.ItemHomeVideoBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.listener.IApiCallback;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.LiveActivity;
import com.xh.hotme.live.bean.UserSetMealBean;
import com.xh.hotme.me.UserDeviceTypeManager;
import com.xh.hotme.me.setmeal.dialog.NotAvailableDialog;
import com.xh.hotme.me.setmeal.utils.SetMealCheckManager;
import com.xh.hotme.me.setmeal.utils.SetMealTimeManager;
import com.xh.hotme.storage.filetransfer.DownloadTaskCompletedEvent;
import com.xh.hotme.storage.filetransfer.TaskManager;
import com.xh.hotme.storage.filetransfer.TaskModel;
import com.xh.hotme.storage.filetransfer.UploadTaskCompletedEvent;
import com.xh.hotme.utils.AdapterHeadUtils;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.FileUtil;
import com.xh.hotme.utils.GlideUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.ListUtils;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.StringUtils;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.VideoUtil;
import com.xh.hotme.widget.TitleItemDecoration;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IEventBus
@RequireStoragePermission(mandatory = false)
public class PhoneVideoFragment extends BaseFragment<FragmentGeneralVideoListBinding> {
    private final static String TAG = "PhoneVideoFragment";
    private List<CameraVideoListBean.VideosBean> _deviceBeans = new ArrayList<>();
    private RAdapter adapter;

    List<CameraVideoDetailListBean.VideosBean> _videoList = new ArrayList<>();

    final ArrayList<CameraVideoDetailListBean.VideosBean> _selectedBeans = new ArrayList<>();

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
    protected SwitchType getSwitchType() {
        return SwitchType.VIEW_PAGER;
    }

    @Override
    protected void initView() {
        _viewBinding.rv.setLayoutManager(new LinearLayoutManager(getContext()));
        _viewBinding.rv.setAdapter(adapter = new RAdapter());
        _viewBinding.rv.setEmptyView(_viewBinding.lltEmpty);

        _viewBinding.refreshLayout.setOnRefreshListener(refreshLayout -> {
            initLocData();
            new Handler(Looper.getMainLooper()).post(() -> _viewBinding.refreshLayout.finishRefresh(2000));
        });

        _viewBinding.rv.addItemDecoration(new TitleItemDecoration(getContext(), new TitleItemDecoration.TitleDecorationCallback() {
            @Override
            public boolean isHeadItem(int curPosition, int nextposition) {
                return AdapterHeadUtils.shouldShowDateHeader(_deviceBeans, nextposition);
            }

            @NonNull
            @Override
            public String getHeadTitle(int position) {
                return AdapterHeadUtils.getHeadTitle(_deviceBeans.get(position));
            }
        }));
    }

    @Override
    protected void initData() {
        initLocData();
    }

    private void initLocData() {
        _deviceBeans = AppFileUtil.loadAllDevicesJson();
        _viewBinding.lltEmpty.setVisibility(_deviceBeans.isEmpty() ? View.GONE : View.VISIBLE);
        VideoUtil.listSorting(_deviceBeans);
        int size = _deviceBeans.size();
        for(int i = size - 1; i >= 0; i--) {
            CameraVideoListBean.VideosBean selectData = _deviceBeans.get(i);
            if(selectData.getVideoTotalCount() == 0) {
                _deviceBeans.remove(i);
            } else if(!VideoUtil.hasAnyVideos(selectData)) {
                if(!TaskManager.getImpl().hasAnyIncompleteTask(selectData)) {
                    _deviceBeans.remove(i);
                }
            }
        }
        if(_deviceBeans.size() != size) {
            AppFileUtil.saveAllDevicesJson(_deviceBeans);
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStoragePermissionGranted() {
        initLocData();
    }

    private class RAdapter extends RecyclerView.Adapter<BaseVH<CameraVideoListBean.VideosBean, ItemHomeVideoBinding>> {
        @NonNull
        @Override
        public BaseVH<CameraVideoListBean.VideosBean, ItemHomeVideoBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VideoVH(ItemHomeVideoBinding.inflate(LayoutInflater.from(getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull BaseVH<CameraVideoListBean.VideosBean, ItemHomeVideoBinding> holder, int position) {
            holder.update(_deviceBeans.get(position), position);
        }

        @Override
        public int getItemCount() {
            return ListUtils.getListSize(_deviceBeans);
        }
    }

    private class VideoVH extends BaseVH<CameraVideoListBean.VideosBean, ItemHomeVideoBinding> {

        public VideoVH(ItemHomeVideoBinding viewBinding) {
            super(viewBinding);
        }

        @Override
        public void update(CameraVideoListBean.VideosBean listBean, int position) {
            viewBinding.toptitle.getRoot().setVisibility(View.GONE);
            if(AdapterHeadUtils.shouldShowDateHeader(_deviceBeans, position)) {
                viewBinding.toptitle.tvTopTitle.setText(AdapterHeadUtils.getHeadTitle(_deviceBeans.get(position)));
                viewBinding.toptitle.getRoot().setVisibility(View.VISIBLE);
                viewBinding.toptitle.extraPadding.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            }
            GlideUtil.loadRoundedCorner(
                HotmeApplication.getContext(),
                VideoUtil.getThumbFilePath(listBean),
                viewBinding.ivLogo,
                10,
                R.mipmap.video_default_cover
            );
            viewBinding.tvTitle.setText(listBean.competition_name);
            try {
                viewBinding.tvVideoNum.setText(VideoUtil.VIDEO_TYPE_STR_ALL_VIDEOS + ":" + listBean.getVideoTotalCount());
            } catch(Exception e) {
                e.printStackTrace();
            }
            try {
                viewBinding.tvGoodNum.setText(VideoUtil.VIDEO_TYPE_STR_HIGH_LIGHT + ":" + (listBean.highlights_number + listBean.away_goals + listBean.home_goals));
            } catch(Exception e) {
                e.printStackTrace();
            }
            try {
                int playback = listBean.playback_number + listBean.full_video_number;
                viewBinding.tvGoalNum.setText(VideoUtil.VIDEO_TYPE_STR_PLAY_BACK + ":" + playback);
            } catch(Exception e) {
                e.printStackTrace();
            }

            viewBinding.getRoot().setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    if(!TextUtils.isEmpty(listBean.path)) {
                        Context context = viewBinding.getRoot().getContext();
                        Intent intent = new Intent(context, PhoneVideoListDetailActivity.class);
                        if(!(context instanceof Activity)) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        }
                        intent.putExtra(PhoneVideoListDetailActivity.EXTRA_BEAN, GsonUtils.GsonString(listBean));
                        context.startActivity(intent);
                    }

                    return true;
                }
            });

            viewBinding.tvVideoNum.setBackgroundColor(Color.TRANSPARENT);
            viewBinding.tvGoodNum.setBackgroundColor(Color.TRANSPARENT);
            viewBinding.tvGoalNum.setBackgroundColor(Color.TRANSPARENT);
            viewBinding.tvTitle.setBackgroundColor(Color.TRANSPARENT);
            viewBinding.tvUpload.setVisibility(View.VISIBLE);

            updateStatus(listBean);
        }

        private void updateStatus(CameraVideoListBean.VideosBean videoBean){
            int status = getUploadStatus(videoBean); //0未上传；1全部上传；2:部分上传
            AppTrace.d("update status = " + status);
            if(status == 0){
                viewBinding.tvUpload.setBackgroundResource(R.drawable.bg_f16c2f_raduis30);
                viewBinding.tvUpload.setTextColor(getContext().getColor(R.color.white));
                viewBinding.tvUpload.setText(getContext().getString(R.string.upload_file_to_server));
                viewBinding.tvUpload.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        AppTrace.d("update status = asxasx" + status);
                        Activity activity = getActivity();
                        if (activity != null && !activity.isFinishing()) {
//                            SetMealCheckManager.checkSetMeal(requireContext(), new SetMealCheckManager.CheckSetMealCallback() {
//                                @Override
//                                public void onSuccess() {
//                                    Log.d("asx","asx+验证通过，上传录制");
//                                    uploadVideo(videoBean);
//                                }
//                            },2);
                            SetMealCheckManager.checkByDeviceId(requireContext(),new SetMealCheckManager.CheckSetMealCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.d("asx","asx+验证通过，上传录制");
                                    uploadVideo(videoBean);
                                }
                            },videoBean.deviceId);

                        }

                        return true;
                    }
                });
            } else if(status == 1){
                viewBinding.tvUpload.setBackgroundResource(R.drawable.bg_f4f4f4_raduis30);
                viewBinding.tvUpload.setTextColor(getContext().getColor(R.color.text_orange));
                viewBinding.tvUpload.setText(getContext().getString(R.string.uploaded_file_to_server));
                viewBinding.tvUpload.setOnClickListener(null);
            } else if(status == 2){
                viewBinding.tvUpload.setBackgroundResource(R.drawable.bg_f16c2f_raduis30);
                viewBinding.tvUpload.setTextColor(getContext().getColor(R.color.white));
                viewBinding.tvUpload.setText(getContext().getString(R.string.reupload_to_server));
                viewBinding.tvUpload.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {

                        Activity activity = getActivity();
                        if (activity != null && !activity.isFinishing()) {
                            SetMealCheckManager.checkByDeviceId(requireContext(), new SetMealCheckManager.CheckSetMealCallback() {
                                @Override
                                public void onSuccess() {

                                    uploadVideo(videoBean);
                                }
                            },videoBean.deviceId);
                        }
                        return true;
                    }
                });
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refreshVideoList(CameraFilesDownloadedEvent event) {
        AppTrace.d(TAG, "refreshVideoList");
        initLocData();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadTaskCompleted(DownloadTaskCompletedEvent event) {
        AppTrace.d(TAG, "onDownloadTaskCompleted: " + event.model.getUrl());
        // only handle video event
        TaskModel model = event.getModel();
        if(model.getType() != TaskModel.TYPE_VIDEO) return;

        // find video set downloading to false
        for(CameraVideoListBean.VideosBean deviceBean : _deviceBeans) {
            if(!TextUtils.isEmpty(deviceBean.competition_id) &&
                deviceBean.competition_id.equals(model.getCompetitionId())) {
                List<CameraVideoDetailListBean.VideosBean> videoList = AppFileUtil.loadDeviceVideoJson(deviceBean);
                for(CameraVideoDetailListBean.VideosBean videoBean : videoList) {
                    if(!TextUtils.isEmpty(videoBean.videoId) && videoBean.videoId.equals(model.getVideoId())) {
                        if(videoBean.downloading) {
                            videoBean.downloading = false;
                            videoBean.videoStatus = Constants.STATUS_COMPLETE;
                            AppFileUtil.saveDeviceVideoJson(deviceBean, videoList);
                        }
                        break;
                    }
                }
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUploadTaskCompleted(UploadTaskCompletedEvent event) {
        AppTrace.d(TAG, "onUploadTaskCompleted: " + event.model.getUrl());
        // only handle video event
        TaskModel model = event.getModel();
        if(model.getType() != TaskModel.TYPE_VIDEO) return;

        // find video set downloading to false
        for(CameraVideoListBean.VideosBean deviceBean : _deviceBeans) {
            if(!TextUtils.isEmpty(deviceBean.competition_id) &&
                    deviceBean.competition_id.equals(model.getCompetitionId())) {
                List<CameraVideoDetailListBean.VideosBean> videoList = AppFileUtil.loadDeviceVideoJson(deviceBean);
                for(CameraVideoDetailListBean.VideosBean videoBean : videoList) {
                    if(!TextUtils.isEmpty(videoBean.videoId) && videoBean.videoId.equals(model.getVideoId())) {
                        //同步刷新上传按钮的状态
                        updateStoreV2(videoBean.deviceId);
                        initLocData();
                        break;
                    }
                }
            }
        }
    }

        // 添加新方法
        private void updateStoreV2(String deviceId) {
            // update_store_V2
//            String deviceType = UserDeviceTypeManager.getInstance().getDeviceType();
//            if (!deviceType.equals("6")) {
//               return;
//            }


            // 只有设备6才上报
            HashMap<String, String> paramsdata = new HashMap<>();
            paramsdata.put("deviceId", deviceId);

            OkHttpUtil.postData(SdkApi.update_store_V2(), paramsdata, null,
                    new OkHttpCallbackDecode<Object>(Object.class) {
                        @Override
                        public void onDataSuccess(Object data) {

                        }

                        @Override
                        public void onFailure(String code, String message) {
                            MainHandler.runOnUiThread(() -> {
                                DialogUtil.dismissDialog();
                            });
                        }

                        @Override
                        public void onFinish() {
                            MainHandler.runOnUiThread(() -> {
                                DialogUtil.dismissDialog();
                            });
                        }
                    });

        }

    //0未上传；1全部上传；2:部分上传
    private int getUploadStatus(CameraVideoListBean.VideosBean listBean){
        int status = 0;
        int uploadNumber = 0;
        int totalNumber = 0;
        List<CameraVideoDetailListBean.VideosBean>  videoList = AppFileUtil.loadDeviceVideoJson(listBean);
        totalNumber = videoList.size();
        for (CameraVideoDetailListBean.VideosBean videoBean : videoList) {
            boolean isUpload = TaskManager.getImpl().isVideoUpload(videoBean);

            if(isUpload){
                uploadNumber++;
            } else {
                if(videoBean.videoStatus == Constants.STATUS_COMPLETE){
                    uploadNumber++;
                }else{
                    // 这个看起来可以获取大小
//                    videoBean.size
                }
            }
        }
        if(uploadNumber == 0){
            status = 0;
        }else if(uploadNumber == totalNumber){
            status = 1;
        }else{
            status = 2;
        }
        return status;
    }


    private void uploadVideo(CameraVideoListBean.VideosBean listBean){
        if(!LoginManager.validToken()) {
            MobileLoginActivity.start(getActivity());
            return ;
        }
        if(NetworkManager.getImpl().isConnectedSoftAp()) {
            ToastUtil.s(getString(R.string.info_switch_avalible_network_for_camera_softap));
            return;
        }
        boolean isNetValid = NetUtil.isConnectedAvailableNetwork();
        if(!isNetValid) {
            ToastUtil.s(getString(R.string.info_switch_avalible_network));
            return;
        }

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
                            getObsInfo(listBean);
                        } else {
                            uploadWorkflowStep1_addCompetition(listBean);
                        }
                        return true;
                    }
                }
        );
    }

    private void getObsInfo(CameraVideoListBean.VideosBean _deviceData){
        ApiUtil.getObsInfo(new IApiCallback() {
            @Override
            public void onSuccess() {
                uploadWorkflowStep1_addCompetition(_deviceData);
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

    /*
     *  保存比赛Id到本地记录
     */
    private void syncCompetitionInfoToLocal(CameraVideoListBean.VideosBean _deviceData, String competitionId) {
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

    private void syncVideoInfoToLocal(CameraVideoListBean.VideosBean _deviceData, CameraVideoDetailListBean.VideosBean localVideoBean, String VideoId, String domainId) {
        for(int i = 0; i < _videoList.size(); i++) {
            CameraVideoDetailListBean.VideosBean videosBean = _videoList.get(i);
            if(localVideoBean.deviceId.equalsIgnoreCase(videosBean.deviceId) && localVideoBean.link.equalsIgnoreCase(videosBean.link)) {
                videosBean.videoId = VideoId;
                videosBean.domainId = domainId;
                break;
            }
        }
        AppFileUtil.saveDeviceVideoJson(_deviceData, _videoList);
    }


    private void syncVideoInfoToLocal(CameraVideoListBean.VideosBean _deviceData, CameraVideoDetailListBean.VideosBean localVideoBean, String VideoId, String domainId, int videoStatus) {
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

    private void uploadWorkflowStep1_addCompetition(CameraVideoListBean.VideosBean _deviceData) {
        AppTrace.d(TAG, "uploadWorkflowStep1_addCompetition");
        // 先确认filedownloader服务已启动
        if(!TaskManager.getImpl().isReady()) {
            ToastUtil.s(getString(R.string.info_download_server_not_lunched));
            return;
        }

        //初始化上传文件信息
        _videoList = AppFileUtil.loadDeviceVideoJson(_deviceData);
        if(!_selectedBeans.isEmpty()){
            _selectedBeans.clear();
        }
        _selectedBeans.addAll(_videoList);

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
                    syncCompetitionInfoToLocal(_deviceData, data.getCompetitionId());
                    AppTrace.d(TAG, "uploadWorkflowStep1_addCompetition success, competition id: " + data.getCompetitionId());

                    // 如果比赛没有缩略图, 先修复他
                    _uploadResult.competitionOk = true;
                    if(TextUtils.isEmpty(data.getCompetitionImage())) {
                        uploadWorkflowStep2_fixCompetition(_deviceData);
                    } else {
                        uploadWorkflowStep3_queryCompetitionVideos(_deviceData);
                    }
                }

                @Override
                public void onFail(String code, String message) {
                    AppTrace.d(TAG, "uploadWorkflowStep1_addCompetition, add competition info fail, code=" + code + ", message=" + message);
                    _uploadResult.competitionOk = false;
                    _uploadResult.competitionErrMsg = message;
                    if(code.equals("1001")) {
                        SetMealTimeManager.queryAll(getActivity());
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
                    syncCompetitionInfoToLocal(_deviceData, data.getCompetitionId());
                    if(TextUtils.isEmpty(data.getCompetitionImage())) {
                        uploadWorkflowStep2_fixCompetition(_deviceData);
                    } else {
                        uploadWorkflowStep3_queryCompetitionVideos(_deviceData);
                    }
                }

                @Override
                public void onFail(String code, String message) {
                    _uploadResult.competitionOk = false;
                    _uploadResult.competitionErrMsg = message;
                    if(code.equals("1001")) {
                        SetMealTimeManager.queryAll(getActivity());
                    }
                    uploadWorkflowStep9_wrapUp();
                }

                @Override
                public void onFinish() {

                }
            });
        }
    }

    private void uploadWorkflowStep2_fixCompetition(CameraVideoListBean.VideosBean _deviceData) {
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
        uploadWorkflowStep3_queryCompetitionVideos(_deviceData);
    }

    private void uploadWorkflowStep3_queryCompetitionVideos(CameraVideoListBean.VideosBean _deviceData) {
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
                uploadWorkflowStep4_parallelUploadVideos(_deviceData);
            }
        });
    }

    private void uploadWorkflowStep4_parallelUploadVideos(CameraVideoListBean.VideosBean _deviceData) {
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
                    uploadWorkflowStep5_queryVideoStatus(_deviceData, videosBean);
                } else {
                    AppTrace.d(TAG, "uploadWorkflowStep3_parallelUploadVideos, no video id, " + videosBean.link + ", get video id first");
                    uploadWorkflowStep6_addVideo(_deviceData, videosBean);
                }
            }
        }

        // try wrap up
        uploadWorkflowStep9_wrapUp();
    }

    private void uploadWorkflowStep5_queryVideoStatus(CameraVideoListBean.VideosBean _deviceData, CameraVideoDetailListBean.VideosBean videosBean) {
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
                    syncVideoInfoToLocal(_deviceData, videosBean, data.getVideoId(), domainId, data.getVideoStatus());

                    if(data.getVideoStatus() != Constants.STATUS_COMPLETE) {
                        uploadWorkflowStep7_addVideoStatus(_deviceData, videosBean);
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
                    uploadWorkflowStep6_addVideo(_deviceData, videosBean);
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

    private void uploadWorkflowStep7_addVideoStatus(CameraVideoListBean.VideosBean _deviceData, CameraVideoDetailListBean.VideosBean videosBean) {
        ApiUtil.addVideoUploadStatus(videosBean.videoId, new IGenericApiCallback<HmVideoUploadProgress>() {
            @Override
            public void onSuccess(HmVideoUploadProgress data) {
                syncVideoInfoToLocal(_deviceData, videosBean, data.getVideoId(), data.getDomainId(), data.getUploadStatus());
                uploadWorkflowStep8_startVideoUploadTask(_deviceData, videosBean);
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

    private void uploadWorkflowStep6_addVideo(CameraVideoListBean.VideosBean _deviceData, CameraVideoDetailListBean.VideosBean videosBean) {
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
                    syncVideoInfoToLocal(_deviceData, videosBean, data.getVideoId(), domainId, data.getVideoStatus());

                    if(TextUtils.isEmpty(domainId)) {
                        uploadWorkflowStep7_addVideoStatus(_deviceData, videosBean);
                    } else {
                        uploadWorkflowStep8_startVideoUploadTask(_deviceData, videosBean);
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

    private void uploadWorkflowStep8_startVideoUploadTask(CameraVideoListBean.VideosBean _deviceData, CameraVideoDetailListBean.VideosBean videosBean) {
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
            getActivity().runOnUiThread(() -> ToastUtil.s(_uploadResult.competitionErrMsg));
            return;
        }

        // if not all video has a feedback, wait
        if(_selectedBeans.size() != _uploadResult.alreadyUploaded + _uploadResult.notExist + _uploadResult.started + _uploadResult.failed) {
            return;
        }

        // show feedback
        if(_uploadResult.started == 0) {
            showConfirmDialog(getString(R.string.tip), getString(R.string.video_already_upload_please_select_other_videos));
            //refresh again
            initLocData();
        } else if(_uploadResult.alreadyUploaded == 0 && _uploadResult.notExist == 0 && _uploadResult.failed == 0) {
            getActivity().runOnUiThread(() -> ToastUtil.s(String.format("%d个视频开始上传", _uploadResult.started)));
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
    }

    private void startPendingUploadTasks() {
        if(_uploadResult != null) {
            for(Runnable r : _uploadResult.pendingTasks) {
                r.run();
            }
        }
    }
}