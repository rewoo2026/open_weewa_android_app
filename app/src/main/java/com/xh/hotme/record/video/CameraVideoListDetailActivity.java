package com.xh.hotme.record.video;

import static com.xh.hotme.HotmeApplication.getContext;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.xh.hotme.R;
import com.xh.hotme.base.BLEAware;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.BaseVH;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.CameraVideoDetailListBean;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.VideoDetailListRequestBean;
import com.xh.hotme.bluetooth.bean.VideoDirRemoveRequestBean;
import com.xh.hotme.bluetooth.bean.VideoRemoveRequestBean;
import com.xh.hotme.bluetooth.event.BleDisConnectSuccessEvent;
import com.xh.hotme.bluetooth.event.DeleteFileEvent;
import com.xh.hotme.bluetooth.event.DeleteVideoDirEvent;
import com.xh.hotme.bluetooth.event.RefreshCameraVideoEvent;
import com.xh.hotme.bluetooth.listener.IBleVideoDetailListNotifyListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.softap.CameraSoftApActivity;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.camera.wifi.HotmeWiFiManager;
import com.xh.hotme.databinding.ActivityCameraviedeolistdetailBinding;
import com.xh.hotme.databinding.VideoDetailItemContentBinding;
import com.xh.hotme.http.ConnectLogic;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.http.DeviceHttpManager;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.storage.ConfirmDownloadActivity;
import com.xh.hotme.storage.TransferFilesActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.CommonDialogUtils;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.FileSizeUtil;
import com.xh.hotme.utils.GlideUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.PermissionsUtil;
import com.xh.hotme.utils.StorageUtil;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.VideoUtil;
import com.xh.hotme.widget.GridSpacingItemDecoration;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;

@IEventBus
@BLEAware
public class CameraVideoListDetailActivity extends BaseActivity<ActivityCameraviedeolistdetailBinding> implements EasyPermissions.PermissionCallbacks, IBleVideoDetailListNotifyListener {

    final ArrayList<CameraVideoDetailListBean.VideosBean> _dataSource = new ArrayList<>();
    final ArrayList<CameraVideoDetailListBean.VideosBean> datas = new ArrayList<>();
    boolean showSelect;//选泽显示
    final ArrayList<CameraVideoDetailListBean.VideosBean> datasSelect = new ArrayList<>();
    final ArrayList<String> paths = new ArrayList<>();
    long allSize;
    CameraVideoListBean.VideosBean myData;

    int videoType = VideoUtil.INT_VIDEO_FILTER_TYPE_ALL_VIDEOS;

    int sourceType = VideoUtil.VIDEO_SOURCE_CAMERA;

    ModalDialog _openSoftApDialog;
    ModalDialog _joinSoftApDialog;

    ModalDialog _deleteFolderDialog;

    String selectAllStr;
    String cancelSelectAllStr;


    private final Runnable _timeOutRunnable = () -> {
        DialogUtil.dismissDialog();
    };


    public static void start(Context ctx, CameraVideoListBean.VideosBean myData) {
        Intent intent = new Intent(ctx, CameraVideoListDetailActivity.class);
        intent.putExtra("bean", GsonUtils.GsonString(myData));
        intent.putExtra("source", 0);
        ctx.startActivity(intent);
    }

    public static void start(Context ctx, int type, CameraVideoListBean.VideosBean myData) {
        Intent intent = new Intent(ctx, CameraVideoListDetailActivity.class);
        intent.putExtra("bean", GsonUtils.GsonString(myData));
        intent.putExtra("source", type);
        ctx.startActivity(intent);
    }

    @Override
    protected void initView() {
        selectAllStr = getString(R.string.select_all);
        cancelSelectAllStr = getString(R.string.select_all_cancel);
        sourceType = getIntent().getIntExtra("source", VideoUtil.VIDEO_SOURCE_CAMERA);
        String videoBeanStr = getIntent().getStringExtra("bean");
        myData = GsonUtils.GsonToBean(videoBeanStr, CameraVideoListBean.VideosBean.class);
        viewBinding.tvTitle.setText(myData.competition_name);
        viewBinding.ivBack.setOnClickListener(v -> {
            if(datas.isEmpty()) {
                showDeleteCompetitionDialog();
            } else {
                finish();
            }
        });

        showSelect = true;
        viewBinding.topDel.tvDelTitle.setText(myData.competition_name);
        releaseData();
        viewBinding.topDel.lltDel.setVisibility(View.VISIBLE);
        upTopIvSelect();

        viewBinding.topDel.tvSelectAll.setOnClickListener(v -> selectAll());
        viewBinding.topDel.ivDelAllSelect.setOnClickListener(v -> selectAll());

        viewBinding.topDel.ivCLose.setImageResource(R.mipmap.common_title_back);
        viewBinding.topDel.ivCLose.setOnClickListener(v -> {
            if(datas.isEmpty()) {
                showDeleteCompetitionDialog();
            } else {
                finish();
            }
        });

        viewBinding.category.setOnCheckedChangeListener((group, checkedId) -> {
            if(checkedId == R.id.categoryAll) {
                videoType = VideoUtil.INT_VIDEO_FILTER_TYPE_ALL_VIDEOS;
            } else if(checkedId == R.id.categoryTop) {
                videoType = VideoUtil.INT_VIDEO_FILTER_TYPE_HIGH_LIGHT;
            } else if(checkedId == R.id.categoryPlayback) {
                videoType = VideoUtil.INT_VIDEO_FILTER_TYPE_PLAY_BACK;
            } else {
                videoType = VideoUtil.INT_VIDEO_FILTER_TYPE_ALL_VIDEOS;
            }
            hideSelect();
            filterList();
        });

        BluetoothHandle.addVideoDetailListNotifyListener(this);
    }

    private void upTopIvSelect() {
        viewBinding.topDel.tvSelectAll.setText(selectAllStr);
        viewBinding.topDel.tvSelectAll.setTextColor(getResources().getColor(R.color.orange));
        viewBinding.topDel.ivDelAllSelect.setImageResource(R.mipmap.ic_unselect1);
        if(!datas.isEmpty() && datasSelect.size() == datas.size()) {
            viewBinding.topDel.ivDelAllSelect.setImageResource(R.mipmap.ic_select);
            viewBinding.topDel.tvSelectAll.setText(cancelSelectAllStr);
            viewBinding.topDel.tvSelectAll.setTextColor(getResources().getColor(R.color.orange));
        }
    }

    private void hideSelect() {
        releaseData();
//        showSelect = false;
//        viewBinding.topDel.lltDel.setVisibility(View.GONE);
        viewBinding.lltBottom.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void selectAll() {
        releaseData();
        if(viewBinding.topDel.tvSelectAll.getText().equals(selectAllStr)) {
            datasSelect.addAll(datas);
            for(int i = 0; i < datasSelect.size(); i++) {
                allSize += datas.get(i).size;
                paths.add(ConnectLogic.getImpl().getBasePath() + datas.get(i).link);
                paths.add(ConnectLogic.getImpl().getBasePath() + datas.get(i).thumb);

            }
        }

        adapter.notifyDataSetChanged();
        showBottom();
        upTopIvSelect();
    }

    private void releaseData() {
        datasSelect.clear();
        allSize = 0;
        paths.clear();
    }


    RAdapter adapter;

    @Override
    protected void initData() {

        viewBinding.recycleView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        viewBinding.recycleView.addItemDecoration(new GridSpacingItemDecoration(2, DensityUtil.dip2px(10), false));
        viewBinding.recycleView.setAdapter(adapter = new RAdapter());

    }

    private void getCompetitionVideoList() {
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new VideoDetailListRequestBean(myData.path));
        if(isOk){
            DialogUtil.showDialog(getString(R.string.loading));
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        } else {
            showDisconnectToast();
        }
    }

    private void filterList() {
        if(_dataSource != null && !_dataSource.isEmpty()) {
            datas.clear();
            for(int i = 0; i < _dataSource.size(); i++) {
                _dataSource.get(i).deviceId = myData.deviceId;
                if(videoType == VideoUtil.INT_VIDEO_FILTER_TYPE_ALL_VIDEOS) {
                    datas.add(_dataSource.get(i));
                } else if(videoType == VideoUtil.INT_VIDEO_FILTER_TYPE_HIGH_LIGHT) {
                    if(_dataSource.get(i).type == VideoUtil.INT_VIDEO_FILTER_TYPE_HIGH_LIGHT
                        || _dataSource.get(i).type == VideoUtil.INT_VIDEO_FILTER_TYPE_HOST_GOAL
                        || _dataSource.get(i).type == VideoUtil.INT_VIDEO_FILTER_TYPE_GUEST_GOAL
                    ) {
                        datas.add(_dataSource.get(i));
                    }
                } else if(videoType == VideoUtil.INT_VIDEO_FILTER_TYPE_PLAY_BACK) {
                    if(_dataSource.get(i).type == VideoUtil.INT_VIDEO_FILTER_TYPE_FULL_VIDEOS
                        || _dataSource.get(i).type == VideoUtil.INT_VIDEO_FILTER_TYPE_PLAY_BACK
                    ) {
                        datas.add(_dataSource.get(i));
                    }
                }
            }
            if(datas.isEmpty()) {
                viewBinding.emptyView.getRoot().setVisibility(View.VISIBLE);
            } else {
                viewBinding.emptyView.getRoot().setVisibility(View.GONE);
            }
            adapter.notifyDataSetChanged();
        } else {
            if(_dataSource == null || _dataSource.isEmpty()) {
                viewBinding.emptyView.getRoot().setVisibility(View.VISIBLE);
            } else {
                viewBinding.emptyView.getRoot().setVisibility(View.GONE);
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        updateStatus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BluetoothHandle.removeVideoDetailListNotifyListener(this);
    }

    @Override
    public void onBackPressed() {
        if(datas.isEmpty()) {
            showDeleteCompetitionDialog();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onVideoDetailList(List<CameraVideoDetailListBean.VideosBean> data) {
        AppTrace.d(TAG, "onVideoDetailList");
        MainHandler.runOnUiThread(() -> {
            DialogUtil.dismissDialog();
            _dataSource.clear();
            _dataSource.addAll(data);
            myData.deviceId = ConnectLogic.getImpl().getDeviceId();
            if(!BaseAppUtil.isDestroy(CameraVideoListDetailActivity.this)) {
                CameraVideoListDetailActivity.this.runOnUiThread(() -> filterList());
            }
        });
    }

    @Override
    public void onVideoDetailListFail(String msg) {
        AppTrace.d(TAG, "onVideoDetailListFail");
        MainHandler.runOnUiThread(() -> {
            ToastUtil.s(msg);
        });
    }


    private class RAdapter extends RecyclerView.Adapter<CVDVh> {

        @NonNull
        @Override
        public CVDVh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new CVDVh(VideoDetailItemContentBinding.inflate(LayoutInflater.from(CameraVideoListDetailActivity.this), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull CVDVh holder, int position) {
            holder.update(datas.get(position), position);
        }

        @Override
        public int getItemCount() {
            return datas.size();
        }
    }

    private class CVDVh extends BaseVH<CameraVideoDetailListBean.VideosBean, VideoDetailItemContentBinding> {

        public CVDVh(VideoDetailItemContentBinding viewBinding) {
            super(viewBinding);
        }

        @Override
        public void update(CameraVideoDetailListBean.VideosBean model, int position) {
            if(model == null) return;

            if(showSelect) {
                viewBinding.ivSelectIcon.setVisibility(View.VISIBLE);
                viewBinding.ivSelectIcon.setBackgroundResource(R.mipmap.ic_unselect);
                if(datasSelect.contains(datas.get(position))) {
                    viewBinding.ivSelectIcon.setBackgroundResource(R.mipmap.ic_select);
                }
            } else {
                viewBinding.ivSelectIcon.setVisibility(View.GONE);
            }
            viewBinding.tvTitle.setText(model.name);
            if(model.duration > 0) {
                viewBinding.tvTime.setText(TimeUtil.intToTimeMin(model.duration));
            } else {
                viewBinding.tvTime.setText("0");
            }
            viewBinding.ivLogo.setCornerRadius(20);
            GlideUtil.loadRoundedCorner(viewBinding.ivLogo.getContext(), ConnectLogic.getImpl().getConnectBaseurl() + "/file?p=/" + model.thumb, viewBinding.ivLogo, 20, R.mipmap.video_default_cover);
//            GlideUtils.showImg(viewBinding.ivLogo, ConnectLogic.getInstance().getConnectBaseurl() + "/file?p=/" + data.thumb);
            viewBinding.getRoot().setOnClickListener(v -> {
                if(showSelect) {
                    if(datasSelect.contains(datas.get(position))) {
                        datasSelect.remove(datas.get(position));
                        paths.remove(ConnectLogic.getImpl().getBasePath() + datas.get(position).link);
                        allSize -= datas.get(position).size;
                        paths.remove(ConnectLogic.getImpl().getBasePath() + datas.get(position).thumb);
                        if(datasSelect.isEmpty()) {
                            paths.remove(ConnectLogic.getImpl().getBasePath() + myData.path + "/mstat.log");
                            paths.remove(ConnectLogic.getImpl().getBasePath() + myData.path + "/rec_info.json");
                        }
                    } else {
                        datasSelect.add(datas.get(position));
                        allSize += datas.get(position).size;
                        paths.add(ConnectLogic.getImpl().getBasePath() + datas.get(position).link);
                        paths.add(ConnectLogic.getImpl().getBasePath() + datas.get(position).thumb);
                        if(!paths.contains(ConnectLogic.getImpl().getBasePath() + myData.path + "/mstat.log")) {
                            paths.add(ConnectLogic.getImpl().getBasePath() + myData.path + "/mstat.log");
                        }
                        if(!paths.contains(ConnectLogic.getImpl().getBasePath() + myData.path + "/rec_info.json")) {
                            paths.add(ConnectLogic.getImpl().getBasePath() + myData.path + "/rec_info.json");
                        }
                    }
                    adapter.notifyItemChanged(position);
                    showBottom();
                } else {
//                        Router.jumpPlay(v.getContext(),ConnectLogic.getInstance().getConnectBaseurl()+"/"+datas.get(position).link);
                }
            });
        }
    }

    private void showBottom() {
        System.out.println("showBottom");
        if(!datasSelect.isEmpty()) {
            viewBinding.topDel.tvDelTitle.setText(String.format(getString(R.string.select_video_files_number), datasSelect.size()));
        } else {
            viewBinding.topDel.tvDelTitle.setText(myData.competition_name);
        }
        upTopIvSelect();
        if(!datasSelect.isEmpty()) {
            viewBinding.lltBottom.setVisibility(View.VISIBLE);
            viewBinding.tvFileSize.setText("(" + FileSizeUtil.formatSize(allSize) + ")");
            viewBinding.llDownload.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                public boolean onClicked() {
                    if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE){
                        String title =getString(R.string.remote_model_switch_model);
                        String message=getString(R.string.remote_model_switch_model_video_download_message);
                        CommonDialogUtils.showAccessModelDialog(CameraVideoListDetailActivity.this, Constants.ACCESS_MODEL, title, message, new IActionCallback() {
                            @Override
                            public void onReturn(int code, Object... objects) {
                                if(code== CALLBACK_ACTION_OK){
                                    CameraInfo cameraInfo = CameraManager.getCameraInfo("");
                                    if(cameraInfo != null) {
                                        //远程模式下，释放连接信息
                                        CameraManager.release();
                                        EventBus.getDefault().post(new BleDisConnectSuccessEvent(cameraInfo.bluetoothId, true));
                                    }
                                    if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL){
                                        DeviceApiManager.disConnect();
                                        finish();
                                    }
                                }
                            }
                        });
                        return true;
                    }
                    onDownloadClicked();
                    return true;
                }
            });

            viewBinding.llDel.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    showConfirmDialog(
                        "",
                        getString(R.string.title_delete_select_video),
                        getString(R.string.ok),
                        getString(R.string.cancel),
                        new ClickGuard.GuardedOnClickListener() {
                            @Override
                            public boolean onClicked() {
                                AppTrace.d(TAG, "delete starting...");
                                if(!DeviceApiManager.isConnected()){
                                    showDisconnectDialog();
                                    return true;
                                }

                                String path = myData.path;
                                ArrayList<String> fileNameList = new ArrayList();
                                for(CameraVideoDetailListBean.VideosBean videosBean : datasSelect) {
                                    String fileName = videosBean.link.substring(path.length() + 1);
                                    fileNameList.add(fileName);
                                }
                                VideoRemoveRequestBean requestBean = new VideoRemoveRequestBean(path, fileNameList);
                                boolean isOk = DeviceApiManager.sendCmdSimpleObj(requestBean);
                                if(isOk) {
                                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                                    DialogUtil.showDialog(getString(R.string.loading_deleting));
                                } else {
                                    if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                                        if (!NetworkManager.getImpl().isConnectedSoftAp()) {
                                            showSoftApDialog();
                                        }
                                    }
                                }
                                return true;
                            }
                        }
                    );
                    return true;
                }
            });

        } else {
            viewBinding.lltBottom.setVisibility(View.GONE);
            allSize = 0;
        }
    }

    @Override
    protected void onStoragePermissionGranted() {
        onDownloadClicked();
    }

    private void onDownloadClicked() {
        if(!checkStoragePermission()) {
            return;
        }

        if(!PermissionsUtil.isLocServiceEnable(CameraVideoListDetailActivity.this)) {
            PermissionsUtil.openLocationService(CameraVideoListDetailActivity.this);
            return;
        }

        if(!PermissionsUtil.hasBlePermission(CameraVideoListDetailActivity.this)){
            requestBluetoothPermission();
            return;
        }

        if(!StorageUtil.isEnoughSpace(allSize)) {
            showHintDialog(getString(R.string.dialog_download_exception_title), getString(R.string.dialog_download_exception_msg_no_enough_space));
            return;
        }

        if(!NetworkManager.getImpl().isConnectedSoftAp()) {

            showHintDialog(getString(R.string.dialog_download_exception_title), getString(R.string.dialog_download_exception_msg_without_soft_ap), getString(R.string.know), new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    if(!PermissionsUtil.isLocServiceEnable(CameraVideoListDetailActivity.this)) {
                        PermissionsUtil.openLocationService(CameraVideoListDetailActivity.this);
                        return true;
                    }
                    boolean hasPermission = PermissionsUtil.hasBlePermission(CameraVideoListDetailActivity.this);
                    if(!hasPermission) {
                        EasyPermissions.requestPermissions(CameraVideoListDetailActivity.this, getString(R.string.permission_title_location_and_device), PermissionsUtil.REQUEST_PERMISSION_BLUE, PermissionsUtil.getBlePermissions());
                        return true;
                    }

                    HotmeWiFiManager.startWifiSettingPage(CameraVideoListDetailActivity.this, Constants.REQUEST_CODE_WIFI_PREVIEW);
                    return true;
                }
            });
            return;
        }

        if(ConnectLogic.getImpl().isDownLoading()) {
            showHintDialog("", getString(R.string.dialog_download_exception_msg_video_downloading), getString(R.string.know), new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    Intent intent = new Intent(CameraVideoListDetailActivity.this, TransferFilesActivity.class);
                    startActivity(intent);
                    return true;
                }
            });
        } else {
            if(!paths.contains(ConnectLogic.getImpl().getBasePath() + myData.path + "/" + myData.thumb)) {
                paths.add(ConnectLogic.getImpl().getBasePath() + myData.path + "/" + myData.thumb);
            }

            if(!paths.contains(ConnectLogic.getImpl().getBasePath() + myData.path + "/match_meta.data")) {
                paths.add(ConnectLogic.getImpl().getBasePath() + myData.path + "/match_meta.data");
            }

            if(!paths.contains(ConnectLogic.getImpl().getBasePath() + myData.path + "/mstat.log")) {
                paths.add(ConnectLogic.getImpl().getBasePath() + myData.path + "/mstat.log");
            }
            ConnectLogic.getImpl().addFiles(myData, allSize, datasSelect, paths);

            startActivity(new Intent(CameraVideoListDetailActivity.this, ConfirmDownloadActivity.class));
            hideSelect();
            showBottom();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeleteEvent(DeleteFileEvent event) {
        _handler.removeCallbacks(_timeOutRunnable);
        if(BaseAppUtil.isDestroy(CameraVideoListDetailActivity.this)) {
            return;
        }
        DialogUtil.dismissDialog();
        if(event != null) {
            if(event.state == 1) {
                datas.removeAll(datasSelect);
                hideSelect();

                if(datas.isEmpty()) {
                    _deleteFolderDialog = new ModalDialog(CameraVideoListDetailActivity.this);
                    _deleteFolderDialog.setMessage(getString(R.string.delete_competition_info_title));
                    _deleteFolderDialog.setPositiveButton(getString(R.string.video_tab_delete), new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            if(!DeviceApiManager.isConnected()){
                                showDisconnectDialog();
                                return true;
                            }
                            boolean isOk = DeviceApiManager.sendCmdSimpleObj(new VideoDirRemoveRequestBean(myData.path));
                            if(isOk) {
                                _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                                if(!BaseAppUtil.isDestroy(CameraVideoListDetailActivity.this)) {
                                    DialogUtil.showDialog(getString(R.string.loading_deleting_video_folder));
                                }
                            }
                            return true;
                        }
                    });
                    _deleteFolderDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            _deleteFolderDialog.dismiss();
                            return true;
                        }
                    });

                    _deleteFolderDialog.show();
                } else {
                    EventBus.getDefault().post(new RefreshCameraVideoEvent());
                }
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeleteDirEvent(DeleteVideoDirEvent event) {
        _handler.removeCallbacks(_timeOutRunnable);
        EventBus.getDefault().post(new RefreshCameraVideoEvent());
        DialogUtil.dismissDialog();
        if(event != null && event.state == 1) {
            finish();
        }
    }

    private void showDeleteCompetitionDialog() {
        _deleteFolderDialog = new ModalDialog(this);
        _deleteFolderDialog.setMessage(getString(R.string.delete_competition_info_title));
        _deleteFolderDialog.setPositiveButton(getString(R.string.video_tab_delete), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new VideoDirRemoveRequestBean(myData.path));
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    if(!BaseAppUtil.isDestroy(CameraVideoListDetailActivity.this)) {
                        DialogUtil.showDialog(getString(R.string.loading_deleting_video_folder));
                    }
                }
                return true;
            }
        });
        _deleteFolderDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _deleteFolderDialog.dismiss();
                finish();
                return true;
            }
        });
        _deleteFolderDialog.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //将请求结果传递EasyPermission库处理
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        // ToastUtil.showToastShort(CameraListActivity.this, "用户授权成功");
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        ToastUtil.s(getString(R.string.permission_user_deny));
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            showConfirmDialog(
                getString(R.string.permission_deny_title),
                getString(R.string.permission_deny_content),
                getString(R.string.setting),
                getString(R.string.cancel),
                new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        //跳转应用消息，间接打开应用权限设置-效率高
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", CameraVideoListDetailActivity.this.getPackageName(), null);
                        intent.setData(uri);
                        CameraVideoListDetailActivity.this.startActivity(intent);
                        return true;
                    }
                },
                null
            );
        }
    }

    private void updateStatus() {
        if(viewBinding != null) {
            CameraInfo cameraInfo = CameraManager.getCameraInfo("");
            if(cameraInfo != null) {
                String deviceId = CameraManager.getDeviceId();
                ConnectLogic.getImpl().setDeviceId(deviceId);
                ConnectLogic.getImpl().setDeviceMac(CameraManager.getDeviceBluetoothId());
                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    if (!NetworkManager.getImpl().isDeviceSoftApLaunched()) {
                        showSoftApStartDialog();
                    } else {
                        if (NetworkManager.getImpl().isConnectedSoftAp()) {
                            getCompetitionVideoList();
                        } else {
                            showSoftApDialog();
                        }
                    }
                } else {
                    if(!NetUtil.isConnectedAvailableNetwork()){
                        showNetworkDialog();
                        return;
                    }

                    getCompetitionVideoList();
                }
            }
        }
    }

    private void showNetworkDialog(){
        if(BaseAppUtil.isDestroy(CameraVideoListDetailActivity.this)) {
            return;
        }

        ModalDialog dialog = new ModalDialog(getContext());
        dialog.setTitle(getString(R.string.remote_model_no_network_title));
        dialog.setMessage(getString(R.string.remote_model_no_network_message));
        dialog.hideNegativeButton();
        dialog.setPositiveButton(getString(R.string.ok), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    private void showSoftApStartDialog() {
        if(BaseAppUtil.isDestroy(CameraVideoListDetailActivity.this)) {
            return;
        }
        if(_openSoftApDialog != null && _openSoftApDialog.isShowing()) {
            return;
        }

        _openSoftApDialog = new ModalDialog(CameraVideoListDetailActivity.this);
        _openSoftApDialog.setMessage(R.string.soft_ap_set_up);
        _openSoftApDialog.setPositiveButton(getString(R.string.wifi_open), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                CameraSoftApActivity.startActivityForResult(
                    CameraVideoListDetailActivity.this,
                    NetworkManager.getImpl().getSoftApSsid(),
                    NetworkManager.getImpl().getSoftApPassword(),
                    NetworkManager.getImpl().getSoftApStatus(),
                    CameraSoftApActivity.REQUEST_SOFT_AP_PREVIEW
                );
                return true;
            }
        });

        _openSoftApDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _openSoftApDialog.dismiss();
                return true;
            }
        });

        _openSoftApDialog.show();
    }

    private void showSoftApDialog() {
        if(BaseAppUtil.isDestroy(CameraVideoListDetailActivity.this)) {
            return;
        }

        if(_joinSoftApDialog != null && _joinSoftApDialog.isShowing()) {
            return;
        }

        _joinSoftApDialog = new ModalDialog(CameraVideoListDetailActivity.this);
        _joinSoftApDialog.setMessage(String.format(getString(R.string.join_softap_to_view_video), NetworkManager.getImpl().getSoftApSsid()));
        _joinSoftApDialog.setPositiveButton(getString(R.string.wifi_join), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(!PermissionsUtil.isLocServiceEnable(CameraVideoListDetailActivity.this)) {
                    PermissionsUtil.openLocationService(CameraVideoListDetailActivity.this);
                    return true;
                }
                boolean hasPermission = PermissionsUtil.hasBlePermission(CameraVideoListDetailActivity.this);
                if(!hasPermission) {
                    EasyPermissions.requestPermissions(CameraVideoListDetailActivity.this, getString(R.string.permission_title_location_and_device), PermissionsUtil.REQUEST_PERMISSION_BLUE, PermissionsUtil.getBlePermissions());
                    return true;
                }

                HotmeWiFiManager.startWifiSettingPage(CameraVideoListDetailActivity.this, Constants.REQUEST_CODE_WIFI_PREVIEW);
                return true;
            }
        });

        _joinSoftApDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _joinSoftApDialog.dismiss();
                finish();
                return true;
            }
        });

        _joinSoftApDialog.show();
    }
}
