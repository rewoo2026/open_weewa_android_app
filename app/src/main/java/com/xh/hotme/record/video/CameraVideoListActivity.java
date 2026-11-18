package com.xh.hotme.record.video;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.VideoDirRemoveRequestBean;
import com.xh.hotme.bluetooth.bean.VideoListRequestBean;
import com.xh.hotme.bluetooth.event.DeleteFileEvent;
import com.xh.hotme.bluetooth.event.DeleteVideoDirEvent;
import com.xh.hotme.bluetooth.listener.IBleVideoListNotifyListener;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.softap.CameraSoftApActivity;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.camera.wifi.HotmeWiFiManager;
import com.xh.hotme.databinding.ActivityCameraVideoListBinding;
import com.xh.hotme.http.ConnectLogic;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AdapterHeadUtils;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.PermissionsUtil;
import com.xh.hotme.utils.SwipeHelper;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;
import com.xh.hotme.widget.TitleItemDecoration;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

@IEventBus
public class CameraVideoListActivity extends BaseActivity<ActivityCameraVideoListBinding> implements IBleVideoListNotifyListener {
    private final static String TAG = CameraVideoListActivity.class.getSimpleName();

    public final ArrayList<CameraVideoListBean.VideosBean> _videos = new ArrayList();
    private final ArrayList<Integer> _itemTypes = new ArrayList<>();

    private VideoListAdapter _adapter;
    private SwipeHelper _swipeHelper;

    private ModalDialog _confirmDeleteDialog;

    private ModalDialog _openSoftApDialog;
    private ModalDialog _joinSoftApDialog;

    private final Runnable _timeOutRunnable = () -> {
        DialogUtil.dismissDialog();
    };


    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, CameraVideoListActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateStatus();
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
                            getVideoList();
                        } else {
                            showSoftApDialog();
                        }
                    }
                }else{
                    if(!NetUtil.isConnectedAvailableNetwork()){
                        ToastUtil.l(R.string.error_network_error);
                        return;
                    }
                    getVideoList();
                }
            }
        }
    }

    private void showSoftApStartDialog() {
        if(_openSoftApDialog != null && _openSoftApDialog.isShowing()) {
            return;
        }

        _openSoftApDialog = new ModalDialog(this);
        _openSoftApDialog.setMessage(R.string.soft_ap_set_up);
        _openSoftApDialog.setPositiveButton(getString(R.string.wifi_open), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                CameraSoftApActivity.startActivityForResult(
                    CameraVideoListActivity.this,
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
        if(_joinSoftApDialog != null && _joinSoftApDialog.isShowing()) {
            return;
        }

        _joinSoftApDialog = new ModalDialog(this);
        _joinSoftApDialog.setMessage(String.format(getString(R.string.join_softap_to_view_video), NetworkManager.getImpl().getSoftApSsid()));
        _joinSoftApDialog.setPositiveButton(getString(R.string.wifi_join), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(!PermissionsUtil.isLocServiceEnable(CameraVideoListActivity.this)) {
                    PermissionsUtil.openLocationService(CameraVideoListActivity.this);
                    return true;
                }
                boolean hasPermission = PermissionsUtil.hasBlePermission(CameraVideoListActivity.this);
                if(!hasPermission) {
                    EasyPermissions.requestPermissions(CameraVideoListActivity.this, getString(R.string.permission_title_location_and_device), PermissionsUtil.REQUEST_PERMISSION_BLUE, PermissionsUtil.getBlePermissions());
                    return true;
                }

                HotmeWiFiManager.startWifiSettingPage(CameraVideoListActivity.this, Constants.REQUEST_CODE_WIFI_PREVIEW);
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

    @Override
    protected void initView() {
        BluetoothHandle.addVideoListNotifyListener(this);

        viewBinding.titleBar.tvTitle.setText("相机视频");
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });
        viewBinding.titleBar.tvTitle.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                getVideoList();
                return true;
            }
        });
        viewBinding.lltEmpty.setVisibility(View.VISIBLE);
    }

    @Override
    protected void initData() {
        CameraVideoListBean.VideosBean bean = new CameraVideoListBean.VideosBean();
        bean.time = TimeUtil.millis2YMDHMS(System.currentTimeMillis());

        viewBinding.recycleView.setLayoutManager(new LinearLayoutManager(this));
        /**
         * 自定义 ItemDecoration，实现RecycleView 吸顶
         * */
        viewBinding.recycleView.addItemDecoration(new TitleItemDecoration(this
            , new TitleItemDecoration.TitleDecorationCallback() {
            @Override
            public boolean isHeadItem(int curPosition, int nextposition) {
                if(nextposition >= _videos.size()) return false;
                return _itemTypes.get(nextposition) == VideoListAdapter.ITEM_TIME;
            }

            @NonNull
            @Override
            public String getHeadTitle(int position) {
                try {
                    CameraVideoListBean.VideosBean cur = _videos.get(position);
                    return AdapterHeadUtils.getHeadTitle(cur);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                return "";
            }
        }));
        _adapter = new VideoListAdapter(_videos, _itemTypes);
        _adapter.setOnItemClickListener(position -> {
            // 当前如果有一行处于滑动删除按钮可见状态,则点击操作会缩回,而不是打开详情
            if(_swipeHelper.hasSwiped()) {
                _swipeHelper.recoverSwiped();
                return;
            }

            if(!TextUtils.isEmpty(_videos.get(position).path)) {
                CameraVideoListDetailActivity.start(CameraVideoListActivity.this, _videos.get(position));
            }
        });
        viewBinding.recycleView.setAdapter(_adapter);
        viewBinding.recycleView.setEmptyView(viewBinding.lltEmpty);
        viewBinding.refreshLayout.setOnRefreshListener(refreshLayout -> {
            getVideoList();
            MainHandler.getImpl().post(() -> viewBinding.refreshLayout.finishRefresh(2000));
        });

        // handle swipe deletion
        _swipeHelper = new SwipeHelper(this, viewBinding.recycleView, VideoListAdapter.ITEM_VIDEO) {
            @Override
            public void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, List<UnderlayButton> underlayButtons) {
                underlayButtons.add(new SwipeHelper.UnderlayButton(
                    getString(R.string.delete),
                    0,
                    Color.parseColor("#FF3C30"),
                    pos -> {
                        showConfirmDeleteDialog(pos);
                    }
                ));
            }
        };
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeleteFileEvent(DeleteFileEvent event) {
        if(event.state != 0) {
            getVideoList();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeleteVideoDirEvent(DeleteVideoDirEvent event) {
        if(event.state != 0) {
            getVideoList();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BluetoothHandle.removeVideoListNotifyListener(this);

        if(_confirmDeleteDialog != null && _confirmDeleteDialog.isShowing()) {
            _confirmDeleteDialog.dismiss();
        }
    }

    private void showConfirmDeleteDialog(int pos) {
        if(_confirmDeleteDialog != null && _confirmDeleteDialog.isShowing()) {
            return;
        }
        _confirmDeleteDialog = new ModalDialog(this);
        _confirmDeleteDialog.setTitle(R.string.delete_this_match_title);
        _confirmDeleteDialog.setMessage(R.string.match_video_will_be_deleted);
        _confirmDeleteDialog.setPositiveButton(getString(R.string.btn_confirm), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                CameraVideoListBean.VideosBean bean = _videos.get(pos);
                boolean isOk = DeviceApiManager.sendCmdSimpleObj( new VideoDirRemoveRequestBean(bean.path));
                if(isOk) {
                    int count = _videos.size() - pos - 1;
                    _videos.remove(pos);
                    _itemTypes.remove(pos);
                    _adapter.notifyItemRemoved(pos);
                    if(count > 0) {
                        _adapter.notifyItemRangeChanged(pos, count);
                    }
                    _swipeHelper.clearSwiped();
                }
                return true;
            }
        });

        _confirmDeleteDialog.setCancelable(true);
        _confirmDeleteDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _confirmDeleteDialog.dismiss();
                return true;
            }
        });

        _confirmDeleteDialog.show();
    }

    private void buildModelList(List<CameraVideoListBean.VideosBean> data) {
        _videos.clear();
        _itemTypes.clear();
        for(int i = 0; i < data.size(); i++) {
            if(AdapterHeadUtils.shouldShowDateHeader(data, i)) {
                _itemTypes.add(VideoListAdapter.ITEM_TIME);
                _videos.add(data.get(i));
            }
            _itemTypes.add(VideoListAdapter.ITEM_VIDEO);
            _videos.add(data.get(i));
        }
    }

    private void getVideoList() {
       boolean isOk = DeviceApiManager.sendCmdSimpleObj(new VideoListRequestBean());
       if(isOk){
           _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
           DialogUtil.showDialog(getString(R.string.loading));
       } else {
           showDisconnectToast();
       }
    }

    @Override
    public void onVideoList(List<CameraVideoListBean.VideosBean> data) {
        _handler.removeCallbacks(_timeOutRunnable);
        if(!BaseAppUtil.isDestroy(CameraVideoListActivity.this)) {
            runOnUiThread(() -> {
                if(data!=null && data.size()>0){
                    viewBinding.lltEmpty.setVisibility(View.GONE);
                }else{
                    viewBinding.lltEmpty.setVisibility(View.VISIBLE);
                }
                DialogUtil.dismissDialog();
                buildModelList(data);
                _adapter.notifyDataSetChanged();
            });
        }
    }

    @Override
    public void onVideoListFail(String msg) {
        AppTrace.d(TAG, "onVideoListFail: " + msg);
        _handler.removeCallbacks(_timeOutRunnable);
    }
}
