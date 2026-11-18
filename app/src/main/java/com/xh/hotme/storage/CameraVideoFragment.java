package com.xh.hotme.storage;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.app.Dialog;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.view.WindowManager;
import androidx.lifecycle.ViewModelProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xh.hotme.R;
import com.xh.hotme.SplashActivity;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.BaseFragment;
import com.xh.hotme.base.DialogStack;
import com.xh.hotme.base.SwitchType;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bean.PostProc;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.AutoUploadStatusRequestBean;
import com.xh.hotme.bluetooth.bean.PostProcStatusRequestBean;
import com.xh.hotme.bluetooth.bean.StopAutoUploadRequestBean;
import com.xh.hotme.bluetooth.bean.VideoListRequestBean;
import com.xh.hotme.bluetooth.event.AutoUploadStatusEvent;
import com.xh.hotme.bluetooth.event.BleDisConnectSuccessEvent;
import com.xh.hotme.bluetooth.event.NetworkStatusEvent;
import com.xh.hotme.bluetooth.event.PostProcStateEvent;
import com.xh.hotme.bluetooth.event.RefreshCameraVideoEvent;
import com.xh.hotme.bluetooth.event.RemoteSwitchEvent;
import com.xh.hotme.bluetooth.event.RemoveDeviceEvent;
import com.xh.hotme.bluetooth.listener.IBleVideoListNotifyListener;
import com.xh.hotme.camera.BindDeviceListFragment;
import com.xh.hotme.camera.CameraListActivity;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.softap.CameraSoftApActivity;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.camera.wifi.HotmeWiFiManager;
import com.xh.hotme.databinding.CameraVideoFragmentBinding;
import com.xh.hotme.http.ConnectLogic;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.main.CloudFragment;
import com.xh.hotme.main.MainActivity;
import com.xh.hotme.record.video.CameraVideoListDetailActivity;
import com.xh.hotme.utils.AdapterHeadUtils;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.PermissionsUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.BluetoothDialog;
import com.xh.hotme.widget.BluetoothLoadingDialog;
import com.xh.hotme.widget.ModalDialog;
import com.xh.hotme.widget.TitleItemDecoration;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;


/**
 * Created by GJK on 2018/11/9.
 */

public class CameraVideoFragment extends BaseFragment<CameraVideoFragmentBinding> implements NetworkManager.OnNetworkStatusChangedListener {
    private static final String TAG = CameraVideoFragment.class.getSimpleName();
    private static final int ZORDER_CONFIRM_STOP_UPLOAD_DIALOG = 10000;
    private static final int ZORDER_POST_PROCESS_DIALOG = 5000;
    private static final int ZORDER_SOFTAP_DIALOG = 8000;
    BluetoothLoadingDialog _connectingDialog;

    public final ArrayList<CameraVideoListBean.VideosBean> _videoList = new ArrayList<>();
    private CameraVideoListAdapter _adapter;
    private CameraBatchDeleteModel batchModel;
    private CameraAnalysisModel analysisModel;
    private Dialog batchProgress;
    private ModalDialog batchConfirm, batchResult, analysisConfirm, analysisDetail;
    private boolean batchResultPresented;

    private ModalDialog _openSoftApDialog;
    private ModalDialog _joinSoftApDialog;
    private ModalDialog _clipVideoDialog;

    boolean isClipVideoShown;
    private ModalDialog _stopAutoUploadDialog;
    private View.OnClickListener _searchDeviceListener;

    private boolean mResume = false;// 标记onResume是否被刚刚调用
    private boolean _isVisibleToUser;

    private PostProc _postProc;
    boolean unhandleProVideo;
    boolean _disableAutoUpload = false;

    boolean _checkAutoUpload = false;
    boolean _requestAutoUpload = false;
    boolean _requestPostProc = false;
    boolean _checkPostProc = false;
    boolean _requestVideoList = false;

    private final Runnable _deviceUsageInfoRunnable = new Runnable() {
        @Override
        public void run() {
            _handler.postDelayed(_deviceUsageInfoRunnable, 1000);
        }
    };

    private final Runnable _disableAutoUploadRunnable = new Runnable() {
        @Override
        public void run() {
            DeviceApiManager.sendCmdSimpleObj(new StopAutoUploadRequestBean());
            _handler.postDelayed(_deviceUsageInfoRunnable, 60000);
        }
    };

    public static CameraVideoFragment newInstance() {
        return new CameraVideoFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        batchModel=new ViewModelProvider(this).get(CameraBatchDeleteModel.class);
        analysisModel=new ViewModelProvider(this).get(CameraAnalysisModel.class);
        batchModel.attachCamera(CameraManager.getDeviceId());

        _videoListNotifyListener = new IBleVideoListNotifyListener() {
            @Override
            public void onVideoList(List<CameraVideoListBean.VideosBean> data) {
                AppTrace.d(TAG, "onVideoList");
                _requestVideoList = false;
                // The model accepts only this camera's correlated snapshot.
            }

            @Override
            public void onVideoListFail(String msg) {
                AppTrace.d(TAG, "onVideoListFail: " + msg);
            }
        };

        BluetoothHandle.addVideoListNotifyListener(_videoListNotifyListener);

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        NetworkManager.getImpl().registerNetworkStatusChangeListener(this, false);
    }

    @Override
    protected SwitchType getSwitchType() {
        return SwitchType.VIEW_PAGER;
    }

    @Override
    protected void initView() {
        if(_handler==null)_handler=new android.os.Handler(android.os.Looper.getMainLooper());
        _viewBinding.refreshLayout.setOnRefreshListener(refreshLayout -> {
            if(batchModel.running()){refreshLayout.finishRefresh();return;}
            updateStatus(false, true);
            MainHandler.getImpl().post(() -> _viewBinding.refreshLayout.finishRefresh(2000));
        });
        _searchDeviceListener = v -> {

            if (!LoginManager.isSignedIn()) {
                MobileLoginActivity.start(getActivity(), Constants.REQUEST_CODE_LOGIN_HOME_DEVICE);
                return;
            }
            if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                if (!BluetoothManager.getImpl().enableBlueTooth()) {
                    BluetoothDialog dialog = new BluetoothDialog(requireActivity());
                    dialog.setOnClickListener(new DialogInterface.OnClickListener() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                (getActivity()).startActivityForResult(enableBtIntent, Constants.REQUEST_CODE_SCAN);
                            }

                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                } else {
                    if (!PermissionsUtil.isLocServiceEnable(requireActivity())) {
                        PermissionsUtil.openLocationService(requireActivity());
                        return;
                    }
                    boolean hasPermission = PermissionsUtil.hasBlePermission(getActivity());
                    if (!hasPermission) {
                        EasyPermissions.requestPermissions(requireActivity(), getString(R.string.permission_title_location_and_device), PermissionsUtil.REQUEST_PERMISSION_BLUE, PermissionsUtil.getBlePermissions());
                        return;
                    }

                    selectCameraList();
                }
            }else {

                if(!NetUtil.isConnectedAvailableNetwork()){
                    showNetworkDialog();
                    return;
                }

                selectCameraList();
            }
        };
        _viewBinding.btnSetting.setOnClickListener(_searchDeviceListener);
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE){
            _viewBinding.btnSetting.setText( getString(R.string.search_camera_by_remote));
        } else {
            _viewBinding.btnSetting.setText( getString(R.string.search_camera_by_bluetooth));
        }

        _viewBinding.photoSelect.setOnClickListener(v -> {
            if(batchModel.running()){ToastUtil.s(getString(R.string.batch_delete_switch_blocked));return;}
            selectCameraList();
            isClipVideoShown = false;
        });
        _viewBinding.batchManage.setOnClickListener(v->{
            if(analysisModel.selecting)analysisModel.cancel();
            else if(batchModel.selecting)batchModel.cancelSelection();
            else if(batchModel.protocol<1)ToastUtil.s(getString(R.string.batch_delete_upgrade));
            else batchModel.enterSelection();
        });
        _viewBinding.matchAiAction.setOnClickListener(v->{if(batchModel.running()||batchModel.needsReconcile())return;batchModel.cancelSelection();analysisModel.enter();});
        _viewBinding.batchSelectAll.setOnClickListener(v->{if(analysisModel.selecting)analysisModel.toggleGroup(batchModel.videos);else batchModel.toggleGroup(batchModel.videos);});
        _viewBinding.batchDelete.setOnClickListener(v->{if(analysisModel.selecting)showAnalysisConfirm();else showBatchConfirm();});
    }

    private void selectCameraList(){
        Intent intent = new Intent(getActivity(), CameraListActivity.class);
        intent.putExtra(Constants.VIDEO_CATEGORY, BindDeviceListFragment.type_camera_video_storage);
        startActivityForResult(intent, 0xFF);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissBatchDialogs();

        NetworkManager.getImpl().removeNetworkStatusChangeListener(null, this);
        if (_handler != null) {
            _handler.removeCallbacks(_disableAutoUploadRunnable);
        }

        BluetoothHandle.removeVideoListNotifyListener(_videoListNotifyListener);

        dismissStopAutoUploadDialog();

        dismissApDialogs();

        if (_clipVideoDialog != null && _clipVideoDialog.isShowing()) {
            _clipVideoDialog.dismiss();
        }
        _clipVideoDialog = null;

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

    }

    private void dismissApDialogs() {
        if (_joinSoftApDialog != null && _joinSoftApDialog.isShowing()) {
            _joinSoftApDialog.dismiss();
        }
        _joinSoftApDialog = null;

        if (_openSoftApDialog != null && _openSoftApDialog.isShowing()) {
            _openSoftApDialog.dismiss();
        }
        _openSoftApDialog = null;
    }

    @Override
    protected void initData() {

        if(_adapter!=null){renderBatchUi();return;}

//        WifiUtils.getInstance(getContext()).connectWifiNoPwsForHot(ConnectLogic.getInstance().getSsid());
        _viewBinding.recycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        /**
         * 自定义 ItemDecoration，实现RecycleView 吸顶
         * */
        // Date controls belong to actual rows; a painted sticky header would cover them.
        _adapter = new CameraVideoListAdapter(_videoList,batchModel,analysisModel);
        _adapter.setTaskClick(this::showAnalysisTask);
        _adapter.setOnItemClickListener(position -> {
            ConnectLogic.getImpl().setSsid(NetworkManager.getImpl().getSoftApSsid());
            ConnectLogic.getImpl().setSsidPwd(NetworkManager.getImpl().getSoftApPassword());

            String deviceId = CameraManager.getDeviceId();
            AppTrace.d(TAG, "set ConnectLogic deviceId = " + deviceId);
            ConnectLogic.getImpl().setDeviceId(deviceId);
            ConnectLogic.getImpl().setDeviceMac(CameraManager.getDeviceBluetoothId());

            if (Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL && !NetworkManager.getImpl().isConnectedSoftAp()) {
                if (NetworkManager.getImpl().isDeviceSoftApLaunched()) showSoftApDialog();
                else showSoftApStartDialog();
                return;
            }
            if (!TextUtils.isEmpty(_videoList.get(position).path)) {
                CameraVideoListDetailActivity.start(getActivity(), _videoList.get(position));
            }
        });
        _viewBinding.recycleView.setAdapter(_adapter);
        _viewBinding.recycleView.setEmptyView(_viewBinding.lltEmpty);
        batchModel.changes().observe(getViewLifecycleOwner(),version->renderBatchUi());
        analysisModel.changes.observe(getViewLifecycleOwner(),version->renderBatchUi());
        renderBatchUi();
    }

    private void showSoftApStartDialog() {
        if (analysisModel != null && analysisModel.selecting) return;
        if (_openSoftApDialog != null && _openSoftApDialog.isShowing()) {
            return;
        }
        DialogStack stack = getDialogStack();
        if (stack == null) return;

        _openSoftApDialog = new ModalDialog(getContext());
        _openSoftApDialog.setZOrder(ZORDER_SOFTAP_DIALOG);
        _openSoftApDialog.setMessage(getString(R.string.soft_ap_set_up));
        _openSoftApDialog.setPositiveButton(getString(R.string.wifi_open), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                CameraSoftApActivity.startActivityForResult(
                        getActivity(),
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
        stack.push(_openSoftApDialog);
    }

    public boolean isCurrentFragment() {
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof CloudFragment) {
            if (!((CloudFragment) getParentFragment()).isCameraFragment()) {
                return false;
            }
            if (getActivity() instanceof MainActivity) {
                return ((MainActivity) getActivity()).isCloudFragmentCheck();
            }
        }
        return true;
    }

    private void showSoftApDialog() {
        if (analysisModel != null && analysisModel.selecting) return;
        AppTrace.d(TAG, "showSoftApDialog");

        if (_joinSoftApDialog != null && _joinSoftApDialog.isShowing()) {
            return;
        }
        DialogStack stack = getDialogStack();
        if (stack == null) return;

        _joinSoftApDialog = new ModalDialog(getActivity());
        _joinSoftApDialog.setZOrder(ZORDER_SOFTAP_DIALOG);
        String title = getString(R.string.join_softap_title);
        _joinSoftApDialog.setMessage(String.format(title, NetworkManager.getImpl().getSoftApSsid()));
        _joinSoftApDialog.setPositiveButton(getString(R.string.wifi_join), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if (!PermissionsUtil.isLocServiceEnable(getActivity())) {
                    PermissionsUtil.openLocationService(getActivity());
                    return true;
                }
                boolean hasPermission = PermissionsUtil.hasBlePermission(getActivity());
                if (!hasPermission) {
                    EasyPermissions.requestPermissions(getActivity(), getString(R.string.permission_title_location_and_device), PermissionsUtil.REQUEST_PERMISSION_BLUE, PermissionsUtil.getBlePermissions());
                    return true;
                }

                HotmeWiFiManager.startWifiSettingPage(getActivity(), Constants.REQUEST_CODE_WIFI_PREVIEW);
                return true;
            }
        });

        _joinSoftApDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _joinSoftApDialog.dismiss();
                return true;
            }
        });
        stack.push(_joinSoftApDialog);
    }


    private void showConnectingDialog(String message) {
        if (_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
            _connectingDialog = null;
        }
        _connectingDialog = new BluetoothLoadingDialog(getContext());
        _connectingDialog.setMessage(message);
        _connectingDialog.show();
    }


    private void dismissConnectingDialog() {
        if (_connectingDialog != null && _connectingDialog.isShowing()) {
            _connectingDialog.dismiss();
        }
        _connectingDialog = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        AppTrace.d(TAG, "onStop");
        mResume = false;
        _handler.removeCallbacks(_disableAutoUploadRunnable);
    }


    @Override
    public void onVisibleChanged(boolean isVisibleToUser) {
        AppTrace.d(TAG, "onVisibleChanged： " + isVisibleToUser);
        if (isVisibleToUser) {
            AppTrace.d(TAG, "onVisibleChanged to updateNetworkStatus");
            NetworkManager.getImpl().updateNetworkStatus();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume");
        mResume = true;
        batchModel.attachCamera(CameraManager.getDeviceId());
        analysisModel.attach(CameraManager.getDeviceId());
        getVideoList();
        analysisModel.start();
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            _viewBinding.btnSetting.setText(getString(R.string.search_camera_by_bluetooth));
        }else{
            _viewBinding.btnSetting.setText( getString(R.string.search_camera_by_remote));
        }

//        updateStatus();

        // 如果用户进入这个界面的时候选择了停止自动上传, 则确保自动上传被禁止, 这个可以通过隔一会发一下stop auto upload
        // 命令来实现
        if (_disableAutoUpload) {
            DeviceApiManager.sendCmdSimpleObj(new StopAutoUploadRequestBean());

            // 只要在这个界面中就保持不触发自动上传
            // 每次调用stop auto upload会让weewa_idle暂停自动上传5分钟, 所以为了让用户在使用相机视频的时候不触发
            // 自动上传, 需要延续这个时间, 因此每隔一分钟发一次停止自动上传让weewa_idle那边刷新一下静默期时间
            _handler.removeCallbacks(_disableAutoUploadRunnable);
            _handler.postDelayed(_disableAutoUploadRunnable, 60000);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        AppTrace.d(TAG, "onStart");
    }

    @Override
    public void onPause() {
        super.onPause();
        analysisModel.stop();
        AppTrace.d(TAG, "onPause");
        mResume = false;
    }

    private void updateStatus(boolean skipCheckFragment, boolean refreshList) {
        AppTrace.d(TAG, "updateStatus");
        if (_viewBinding != null) {
            batchModel.attachCamera(CameraManager.getDeviceId());
            analysisModel.attach(CameraManager.getDeviceId());
            CameraInfo cameraInfo = CameraManager.getCameraInfo("");
            if (cameraInfo != null) {
                String deviceId = CameraManager.getDeviceId();
                ConnectLogic.getImpl().setDeviceId(deviceId);
                ConnectLogic.getImpl().setDeviceMac(CameraManager.getDeviceBluetoothId());
                _viewBinding.listLayer.setVisibility(View.VISIBLE);
                _viewBinding.layoutDisconnectDevice.setVisibility(View.GONE);
                if (cameraInfo.deviceInfo != null) {
                    String deviceName = BluetoothManager.getDisplayDeviceName(cameraInfo.deviceInfo.device_name);
                    _viewBinding.photoName.setText(deviceName);
                }
                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    _viewBinding.btnSetting.setText( getString(R.string.search_camera_by_bluetooth));
                    // Match metadata travels over BLE; phone hotspot is needed only for video preview.
                    if (refreshList || batchModel.videos.isEmpty()) getVideoList();
                } else {
                    _viewBinding.btnSetting.setText( getString(R.string.search_camera_by_remote));
                    if(!NetUtil.isConnectedAvailableNetwork()){
                        showNetworkDialog();
                        return;
                    }
                    if(!refreshList) {
                        if (_videoList == null || _videoList.isEmpty()) {
                            if (!_requestVideoList) {
                                getVideoList();
                                _requestVideoList = true;
                            }
                        }
                    } else {
                        if (!_requestVideoList) {
                            getVideoList();
                            _requestVideoList = true;
                        }
                    }
                }
                if(!_requestPostProc && !_checkPostProc) {
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(new PostProcStatusRequestBean(VideoClipActivity.clip_type_manual_dotting));
                    if(isOk){
                        _requestPostProc = true;
                    }
                }

                // 检查自动上传状态
                if(!_requestAutoUpload && !_checkAutoUpload) {
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(new AutoUploadStatusRequestBean());
                    if(isOk) {
                        _requestAutoUpload = true;
                    }
                }
            } else {
                if (_viewBinding != null) {
                    _disableAutoUpload = false;
                    _viewBinding.photoName.setText("");
                    _viewBinding.listLayer.setVisibility(View.GONE);
                    _viewBinding.layoutDisconnectDevice.setVisibility(View.VISIBLE);
                    _viewBinding.btnSetting.setOnClickListener(_searchDeviceListener);
                }
                _checkPostProc = false;
                _checkAutoUpload = false;
                _videoList.clear();
                _adapter.notifyDataSetChanged();
            }
        }
    }

    private void showNetworkDialog(){
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onModelSwitchEvent(RemoteSwitchEvent event) {
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            _viewBinding.btnSetting.setText(getString(R.string.search_camera_by_bluetooth));
        }else{
            _viewBinding.btnSetting.setText( getString(R.string.search_camera_by_remote));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceDisconnectSuccess(BleDisConnectSuccessEvent event) {
        AppTrace.d(TAG, "onDeviceDisconnectSuccess");
        updateStatus(false, false);
    }

    IBleVideoListNotifyListener _videoListNotifyListener;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAutoUploadStatusEvent(AutoUploadStatusEvent event) {
        _checkAutoUpload = true;
        _requestAutoUpload = false;
        if (mResume && isCurrentFragment()) {
            // 如果用户还没选择停止自动上传, 则检查是否正在自动上传,如果是则显示对话框
            if (!_disableAutoUpload && !analysisModel.selecting && analysisModel.tasks.isEmpty()) {
                // 如果正在自动上传, 提示是否停止自动上传
                if (event.uploading) {
                    showStopAutoUploadDialog();
                }
            }
        }
    }

    private void dismissStopAutoUploadDialog() {
        if (_stopAutoUploadDialog != null && _stopAutoUploadDialog.isShowing()) {
            _stopAutoUploadDialog.dismiss();
        }
        _stopAutoUploadDialog = null;
    }

    private void showStopAutoUploadDialog() {
        if (_stopAutoUploadDialog != null && _stopAutoUploadDialog.isShowing()) {
            return;
        }
        DialogStack stack = getDialogStack();
        if (stack == null) return;

        _stopAutoUploadDialog = new ModalDialog(getActivity());
        _stopAutoUploadDialog.setZOrder(ZORDER_CONFIRM_STOP_UPLOAD_DIALOG);
        _stopAutoUploadDialog.setMessage(getString(R.string.stop_auto_upload_during_access_camera_video));
        _stopAutoUploadDialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _disableAutoUpload = true;

                // stop auto upload
                DeviceApiManager.sendCmdSimpleObj(new StopAutoUploadRequestBean());

                // 只要在这个界面中就保持不触发自动上传
                // 每次调用stop auto upload会让weewa_idle暂停自动上传5分钟, 所以为了让用户在使用相机视频的时候不触发
                // 自动上传, 需要延续这个时间, 因此每隔一分钟发一次停止自动上传让weewa_idle那边刷新一下静默期时间
                _handler.removeCallbacks(_disableAutoUploadRunnable);
                _handler.postDelayed(_disableAutoUploadRunnable, 60000);
                return true;
            }
        });

        _stopAutoUploadDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _stopAutoUploadDialog.dismiss();
                EventBus.getDefault().post(new CloudFragment.RequestChangeTabEvent(0));
                return true;
            }
        });
        stack.push(_stopAutoUploadDialog);
    }

    private void getVideoList() {
        batchModel.attachCamera(CameraManager.getDeviceId());
        batchModel.refresh();
    }

    private void renderBatchUi() {
        if(_viewBinding==null||_adapter==null)return;
        _videoList.clear();_videoList.addAll(batchModel.videos);_adapter.notifyDataSetChanged();
        boolean selecting=batchModel.selecting,locked=batchModel.running()||batchModel.needsReconcile();
        boolean ai=analysisModel.selecting;
        if(ai)selecting=true;
        java.util.Set<String> selected=ai?analysisModel.selected:batchModel.selected;
        _viewBinding.batchManage.setText(selecting?R.string.cancel:R.string.batch_manage);
        _viewBinding.batchManage.setEnabled(!batchModel.running());
        _viewBinding.batchSelectAll.setVisibility(selecting?View.VISIBLE:View.GONE);
        boolean all=true;for(CameraVideoListBean.VideosBean item:batchModel.videos)if((ai?analysisModel.canSelect(item):batchModel.canSelect(item))&&!selected.contains(item.path))all=false;
        _viewBinding.batchSelectAll.setText(all?R.string.batch_unselect_all:R.string.batch_select_all);
        _viewBinding.batchSelectAll.setEnabled(!locked&&!batchModel.loading);
        _viewBinding.batchBottom.setVisibility(selecting?View.VISIBLE:View.GONE);
        _viewBinding.batchCount.setText(batchModel.loading?"正在读取比赛列表…":getString(selecting?R.string.batch_selected_count:R.string.batch_match_count,selecting?selected.size():batchModel.videos.size()));
        _viewBinding.batchSelectedCount.setText(getString(R.string.batch_selected_count,selected.size()));
        _viewBinding.batchDelete.setText(ai?"开始分析（"+selected.size()+"）":getString(R.string.batch_delete_count,selected.size()));
        _viewBinding.batchDelete.setEnabled(!locked&&!batchModel.loading&&!analysisModel.submitting&&!selected.isEmpty());
        _viewBinding.matchAiAction.setVisibility(selecting?View.GONE:View.VISIBLE);
        _viewBinding.matchAiAction.setEnabled(!locked);
        _viewBinding.batchManage.setEnabled(!batchModel.running()&&!analysisModel.submitting);
        boolean waiting=false;for(MatchAnalysisTask t:analysisModel.tasks.values())if("waiting_network".equals(t.state))waiting=true;
        _viewBinding.matchAiHint.setVisibility(ai||!analysisModel.tasks.isEmpty()?View.VISIBLE:View.GONE);
        _viewBinding.matchAiHint.setText(ai?"支持选择多场。提交任务后即可离开页面。":waiting?"请将相机连接Wi-Fi或网线，连接后自动上传。已上传赛事直接分析，您可离开页面。":analysisModel.hasActive()?"任务在后台继续处理，您可离开页面。进度每5秒刷新。":"任务处理已结束，点击赛事查看结果或失败原因。");
        if(ai)dismissApDialogs();
        if(ai||!analysisModel.tasks.isEmpty())dismissStopAutoUploadDialog();
        if(analysisModel.hasActive()){_disableAutoUpload=false;_handler.removeCallbacks(_disableAutoUploadRunnable);dismissStopAutoUploadDialog();}
        _viewBinding.batchDelete.setAlpha(_viewBinding.batchDelete.isEnabled()?1f:.4f);
        _viewBinding.photoSelect.setEnabled(!batchModel.running());
        _viewBinding.refreshLayout.setEnableRefresh(!batchModel.running());
        if(batchModel.running()) {
            if(batchProgress==null&&!BaseAppUtil.isDestroy(getActivity())) {
                batchProgress=new Dialog(requireContext(),R.style.hotme_modal_dialog);
                batchProgress.setContentView(R.layout.camera_batch_delete_progress);batchProgress.setCancelable(false);
                batchProgress.findViewById(R.id.batch_progress_stop).setOnClickListener(v->batchModel.stop());
                batchProgress.show();
                if(batchProgress.getWindow()!=null)batchProgress.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.86),WindowManager.LayoutParams.WRAP_CONTENT);
            }
            if(batchProgress!=null) {
                int done=batchModel.session.count(BatchDeleteSession.Outcome.SUCCESS)+batchModel.session.count(BatchDeleteSession.Outcome.FAILED);
                ((TextView)batchProgress.findViewById(R.id.batch_progress_count)).setText(done+" / "+batchModel.session.items.size());
                ProgressBar progress=batchProgress.findViewById(R.id.batch_progress_bar);progress.setMax(batchModel.session.items.size());progress.setProgress(done);
                TextView stop=batchProgress.findViewById(R.id.batch_progress_stop);stop.setText(batchModel.session.stopRequested?R.string.batch_delete_stopping:R.string.batch_delete_stop);stop.setEnabled(!batchModel.session.stopRequested);
            }
        } else {
            if(batchProgress!=null){batchProgress.dismiss();batchProgress=null;}
            if(!batchResultPresented&&!batchModel.loading&&batchModel.session.cameraId.equals(batchModel.cameraId)
                    && (batchModel.session.state==BatchDeleteSession.State.FINISHED||batchModel.session.state==BatchDeleteSession.State.PAUSED))showBatchResult();
        }
    }

    private void showBatchConfirm() {
        if(batchModel.running()||batchModel.needsReconcile()||batchModel.selected.isEmpty())return;
        if(!TextUtils.isEmpty(batchModel.message)){ToastUtil.s(batchModel.message);return;}
        batchConfirm=new ModalDialog(requireContext());
        batchConfirm.setTitle(getString(R.string.batch_delete_confirm,batchModel.selected.size()));
        batchConfirm.setMessage(R.string.batch_delete_warning);
        batchConfirm.setNegativeButton(getString(R.string.cancel),v->{});
        batchConfirm.setPositiveButton(getString(R.string.batch_delete_action,batchModel.selected.size()),v->{
            batchResultPresented=false;
            if(!batchModel.begin())ToastUtil.s(TextUtils.isEmpty(batchModel.message)?"所选比赛暂不可删除，请刷新列表后重试":batchModel.message);
        });batchConfirm.show();
    }

    private void showAnalysisConfirm() {
        if(analysisModel.selected.isEmpty()||analysisModel.submitting)return;
        ModalDialog dialog=new ModalDialog(requireContext());analysisConfirm=dialog;
        dialog.setTitle("分析 "+analysisModel.selected.size()+" 场赛事");
        dialog.setMessage("提交后即可离开页面。\n\n未上传赛事：相机连接Wi-Fi或网线后自动上传视频和数据，上传完成后自动分析。无网络或4G时保留等待任务。\n\n已上传赛事：直接交给视频分析服务器分析。");
        dialog.setNegativeButton("返回选择",v->{});
        dialog.setPositiveButton("提交分析任务",v->analysisModel.submit(batchModel.videos));dialog.show();
    }

    private void showAnalysisTask(MatchAnalysisTask task) {
        ModalDialog dialog=new ModalDialog(requireContext());analysisDetail=dialog;dialog.setTitle(task.competitionName);
        String detail=task.label()+"\n\n相机上传："+task.uploadPercent+"%\nAI分析："+task.analysisPercent+"%";
        if(task.totalFrames>0)detail+="\n已处理帧："+task.processedFrames+" / "+task.totalFrames;
        detail+="\n\n"+(task.message==null?"":task.message)+"\n任务在后台处理，可离开页面。";dialog.setMessage(detail);
        dialog.setNegativeButton("关闭",v->{});
        if("failed".equals(task.state))dialog.setPositiveButton("重试",v->analysisModel.retry(task.competitionId));
        else if("done".equals(task.state))dialog.setPositiveButton("查看分析结果",v->{Intent intent=new Intent(getActivity(),MatchAnalysisResultActivity.class);intent.putExtra("deviceId",task.deviceId);intent.putExtra("competitionId",task.competitionId);startActivity(intent);});
        else {dialog.hideNegativeButton();dialog.setPositiveButton("知道了",v->{});}dialog.show();
    }

    private void showBatchResult() {
        if(BaseAppUtil.isDestroy(getActivity()))return;
        batchResultPresented=true;
        BatchDeleteSession session=batchModel.session;
        StringBuilder text=new StringBuilder("成功删除 ").append(session.count(BatchDeleteSession.Outcome.SUCCESS)).append(" 场\n")
                .append("删除失败 ").append(session.count(BatchDeleteSession.Outcome.FAILED)).append(" 场\n")
                .append("尚未执行 ").append(session.count(BatchDeleteSession.Outcome.PENDING)).append(" 场");
        if(session.hasUnknown())text.append("\n待核对 ").append(session.count(BatchDeleteSession.Outcome.UNKNOWN)).append(" 场");
        int shown=0;for(BatchDeleteSession.Item item:session.items)if(!item.reason.isEmpty()&&shown++<5)text.append("\n\n").append(item.title).append("：").append(item.reason);
        batchResult=new ModalDialog(requireContext());batchResult.setTitle(R.string.batch_delete_result);batchResult.setMessage(text.toString());
        batchResult.setNegativeButton(getString(R.string.batch_delete_finished),v->batchModel.acknowledgeResult());
        if(session.hasUnknown())batchResult.setPositiveButton(getString(R.string.batch_delete_verify),v->{batchResultPresented=false;batchModel.refresh();});
        else if(!session.remainingPaths().isEmpty())batchResult.setPositiveButton(getString(R.string.batch_delete_retry),v->{batchModel.selected.clear();batchModel.selected.addAll(session.remainingPaths());batchModel.selecting=true;renderBatchUi();showBatchConfirm();});
        else {batchResult.hideNegativeButton();batchResult.setPositiveButton(getString(R.string.batch_delete_finished),v->batchModel.acknowledgeResult());}
        batchResult.show();
    }

    private void dismissBatchDialogs() {
        if(analysisConfirm!=null){analysisConfirm.dismiss();analysisConfirm=null;}
        if(analysisDetail!=null){analysisDetail.dismiss();analysisDetail=null;}
        if(batchProgress!=null){batchProgress.dismiss();batchProgress=null;}
        if(batchConfirm!=null){batchConfirm.dismiss();batchConfirm=null;}
        if(batchResult!=null){batchResult.dismiss();batchResult=null;}
    }

    @Override public void onDestroyView() {
        dismissBatchDialogs();super.onDestroyView();_viewBinding=null;_rootView=null;_adapter=null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStateEvent(PostProcStateEvent event) {
        AppTrace.d(TAG, "PostProcStateEvent");
        _checkPostProc = true;
        _requestAutoUpload = false;
        _postProc = event.postProcInfo;
        List<String> waitList = _postProc.wait;
        unhandleProVideo = waitList != null && !waitList.isEmpty();
        if (isVisible()) {
            if (unhandleProVideo) {
                if (!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
                    if (isClipVideoShown) { //只是在进入页面第一次会提示
                        return;
                    }

                    if (getActivity() instanceof MainActivity) {
                        if (!((MainActivity) getActivity()).isCloudFragmentCheck()) {
                            return;
                        }
                    }

                    if (_clipVideoDialog != null && _clipVideoDialog.isShowing()) {
                        _clipVideoDialog.dismiss();
                    }
                    _clipVideoDialog = null;

                    DialogStack stack = getDialogStack();
                    if (stack == null) return;

                    _clipVideoDialog = new ModalDialog(getActivity());
                    _clipVideoDialog.setZOrder(ZORDER_POST_PROCESS_DIALOG);
                    String title = getString(R.string.video_clip_title_help);
                    _clipVideoDialog.setTitle(title);
                    _clipVideoDialog.setMessage(getString(R.string.video_clip_content));
                    _clipVideoDialog.setPositiveButton(getString(R.string.video_clip_btn_clip), new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            _clipVideoDialog.dismiss();
                            getActivity().startActivity(new Intent(getActivity(), VideoClipActivity.class));
                            return true;
                        }
                    });

                    _clipVideoDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                        @Override
                        public boolean onClicked() {
                            _clipVideoDialog.dismiss();
                            return true;
                        }
                    });
                    stack.push(_clipVideoDialog);
                    isClipVideoShown = true;
                }
            } else {
                if (_clipVideoDialog != null && _clipVideoDialog.isShowing()) {
                    _clipVideoDialog.dismiss();
                }
                _clipVideoDialog = null;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemoveDeviceEvent(RemoveDeviceEvent event) {
        AppTrace.d(TAG, "onRemoveDeviceEvent");
        updateStatus(false, false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetworkStatusEvent(NetworkStatusEvent event) {
        AppTrace.d(TAG, "onNetworkStatusEvent");
        dismissApDialogs();
        updateStatus(false, false);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshCameraVideoEvent(RefreshCameraVideoEvent event) {
        AppTrace.d(TAG, "onRefreshCameraVideoEvent");
        dismissApDialogs();
        updateStatus(false, true);
    }

    @Override
    public void onStatusChanged(int newStatus, int oldStatus) {
        AppTrace.d(TAG, "onStatusChanged, newStatus=" + newStatus+" ,oldStatus=" + oldStatus);
        if (NetworkManager.getImpl().isConnectedSoftAp()) {
            MainHandler.runOnUiThread(() -> {
                if (isVisible()) {
                    updateStatus(false, false);
                }
            });
        }
    }
}
