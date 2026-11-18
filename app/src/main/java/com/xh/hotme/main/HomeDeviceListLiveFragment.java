package com.xh.hotme.main;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.LiveUrl;
import com.xh.hotme.bluetooth.bean.LiveStatusRequestBean;
import com.xh.hotme.bluetooth.event.LiveStatusResultEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.ConnectActivity;
import com.xh.hotme.camera.HomeDeviceAdapter;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.camera.wifi.HotmeWiFiManager;
import com.xh.hotme.databinding.HomeLiveDevicesBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.live.LiveActivity;
import com.xh.hotme.live.dialog.DevicesDialogUtils;
import com.xh.hotme.me.UserDeviceTypeManager;
import com.xh.hotme.me.setmeal.utils.SetMealCheckManager;
import com.xh.hotme.me.setmeal.utils.SetMealTimeManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;


/**
 * 直播tab
 * 因直播tab数据与设备录制tab数据一样决定继承使用
 */
@IEventBus
public class HomeDeviceListLiveFragment extends HomeDeviceListFragment<HomeLiveDevicesBinding> {
    private static final String TAG = HomeDeviceListLiveFragment.class.getSimpleName();

    @Keep
    public static HomeDeviceListLiveFragment newInstance() {
        return new HomeDeviceListLiveFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.home_live_devices;
    }

    @Override
    protected void loadData() {
        super.loadData();

        mAdapter.setScene(HomeDeviceAdapter.SCENE_TYE_LIVE);

        changeWifi();
        _rootView.findViewById(R.id.tv_disconnect_wifi).setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                HotmeWiFiManager.startWifiSettingPage((Activity) getActivity(), Constants.REQUEST_CODE_WIFI_PREVIEW);
                return false;
            }
        });

    }

    boolean isShowDialog = false;

    //检查热点
    public void changeWifi() {
        if(NetworkManager.getImpl().isConnectedSoftAp()) {
            if(isShowDialog) {
                return;
            }
            isShowDialog = true;
            DevicesDialogUtils.disconnectNetWork(getActivity(), (code, objects) -> {
                if(code == IActionCallback.CALLBACK_ACTION_OK) {
                    HotmeWiFiManager.startWifiSettingPage((Activity) getActivity(), Constants.REQUEST_CODE_WIFI_PREVIEW);
                }
                isShowDialog = false;
            });
            refreshButton.setVisibility(View.GONE);
            _refreshLayout.setVisibility(View.GONE);
            _rootView.findViewById(R.id.layout_error).setVisibility(View.VISIBLE);

        } else {
            refreshButton.setVisibility(View.VISIBLE);
            _refreshLayout.setVisibility(View.VISIBLE);
            _rootView.findViewById(R.id.layout_error).setVisibility(View.GONE);
        }

    }

    protected void jumpAction() {
        AppTrace.d(TAG, "jumpAction2");
        if(isVisible() && !BaseAppUtil.isDestroy(getActivity()) &&
            BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {

            MainHandler.runOnUiThread(() -> {
                if(!NetUtil.isConnectedAvailableNetwork()) {
                    DevicesDialogUtils.openWifiSetting(getActivity());
                    return;
                }
                if(NetworkManager.getImpl().isConnectedSoftAp()) {
                    changeWifi();
                    return;
                }
                if(!LoginManager.isLogon()) {
                    MobileLoginActivity.start(getActivity());
                    return;
                }
                SetMealTimeManager.getInstance().sendCmdGetPlanUsage(3, getActivity(),
                    success -> {

                        if(success) {
                            if(_weewaInfo.n != null) {
                                if(CameraManager.isNetworkValid(_weewaInfo.n)) {  //如果网络正常


                                    Activity activity = getActivity();
                                    if (activity != null && !activity.isFinishing()) {
                                        SetMealCheckManager.checkSetMeal(requireContext(), new SetMealCheckManager.CheckSetMealCallback() {
                                            @Override
                                            public void onSuccess() {
                                                Log.d("asx","asx+验证通过，进入直播");
                                                startActivity(new Intent(getActivity(), LiveActivity.class));
                                            }
                                        },1);
                                    }

                                } else {
                                    ConnectActivity.start(getActivity(), _connectDevice,
                                        Constants.ACTIVE_STEP_SETUP_WIFI, true);
                                }
                            }
                        }

                    });

            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Constants.REQUEST_CODE_WIFI_PREVIEW) {
            if(isFragmentVisible()) {
                if(!NetworkManager.getImpl().isConnectedSoftAp()) {
                    ToastUtil.s(getString(R.string.soft_ap_disconnect));
                }
                changeWifi();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume");
        if(isFragmentVisible()) {
            changeWifi();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLiveStatusResultEvent(LiveStatusResultEvent data) {
        //检查直播状态回调
        _handler.post(() -> {
            if(data != null && data.bean != null) {
                if(data.status == 1) {
                    try {
                        if(data.bean.fec_live_urls.size() > 1) {
                            String _url_push = null;
                            try {
                                for(LiveUrl info : data.bean.fec_live_urls) {
                                    if(!info.url.startsWith("rtmp://127.0.0.1:1935")) {
                                        if(info.st == 1) {
                                            _url_push = info.url;
                                            break;
                                        }
                                    }
                                }
                            } catch(Exception e) {
                                e.printStackTrace();
                            }

                            if(_url_push != null) {
                                if(_connectDevice != null) {
                                    List<BindDeviceBean> data1 = mAdapter.getData();
                                    for(int i = 0; i < data1.size(); i++) {
                                        BindDeviceBean blue = data1.get(i);
                                        if(blue.getBluetoothUuid().equals(_connectDevice.getAddress())) {
                                            blue.setLiveStatus(1);
                                        }
                                    }
                                    mAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    if(_connectDevice != null) {
                        List<BindDeviceBean> data1 = mAdapter.getData();
                        for(int i = 0; i < data1.size(); i++) {
                            BindDeviceBean blue = data1.get(i);
                            if(blue.getBluetoothUuid().equals(_connectDevice.getAddress())) {
                                blue.setLiveStatus(0);
                            }
                        }
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }

    @Override
    protected void onHomeConnectSuccess(BindDeviceBean device, int status) {
        super.onHomeConnectSuccess(device, status);
        //检查直播状态
        DeviceApiManager.sendCmdSimpleObj(new LiveStatusRequestBean());
    }

    @Override
    protected boolean isCurrentHomeFragment() {
        if(getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).isLiveFragmentCheck();
        }
        return false;
    }
}
