package com.xh.hotme.main;

import android.util.Log;
import android.view.View;

import androidx.annotation.Keep;

import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.GetSettingsRequestBean;
import com.xh.hotme.bluetooth.event.RemoteSwitchEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsEvent;
import com.xh.hotme.camera.HomeDeviceAdapter;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.databinding.HomeFragmentDefaultBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.me.setmeal.utils.SetMealCheckManager;
import com.xh.hotme.me.setmeal.utils.SetMealTimeManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.CommonDialogUtils;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.PlayUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


/**
 * 直播tab
 * 因直播tab数据与设备录制tab数据一样决定继承使用
 */
@IEventBus
public class HomeDeviceListRecordFragment extends HomeDeviceListFragment<HomeFragmentDefaultBinding> {
    private static final String TAG = HomeDeviceListRecordFragment.class.getSimpleName();

    @Keep
    public static HomeDeviceListRecordFragment newInstance() {
        return new HomeDeviceListRecordFragment();
    }

    @Override
    protected void loadData() {
        super.loadData();

        mAdapter.setScene(HomeDeviceAdapter.SCENE_TYE_RECORD);

        setWitchModel();
        _viewBinding.ivModelSwitch.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                CommonDialogUtils.showAccessModelDialog(getActivity(), Constants.ACCESS_MODEL, new IActionCallback() {
                    @Override
                    public void onReturn(int code, Object... objects) {
                        if(code== CALLBACK_ACTION_OK){
                            setWitchModel();
                            //切换后重新加载一下列表状态
                            reloadDeviceList();

                            _viewBinding.ivRefresh.callOnClick();
                        }
                    }
                });
                return true;
            }
        });

    }

    private void updateModelUI(){
        if (Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            _viewBinding.ivModelSwitch.setImageResource(R.mipmap.iv_local_model);
        } else {
            _viewBinding.ivModelSwitch.setImageResource(R.mipmap.iv_remote_model);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onModelSwitchEvent(RemoteSwitchEvent event) {
        updateModelUI();
    }

    private void setWitchModel(){
        if(HotmeApplication.showModelSwitch) {
            _viewBinding.ivModelSwitch.setVisibility(View.VISIBLE);
            updateModelUI();
        }else{
            _viewBinding.ivModelSwitch.setVisibility(View.GONE);
        }
    }

    @Override
    protected void jumpAction() {
        AppTrace.d(TAG, "jumpAction1");

        MainHandler.runOnUiThread(() -> {
            if(isFragmentVisible() && !BaseAppUtil.isDestroy(getActivity()) &&
                BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
                if(!connectBluetoothRequest) {
                    connectBluetoothRequest = false;
                    return;
                }

                //如果连接了热点
                if(NetworkManager.getImpl().isConnectedSoftAp()) {
                    PlayUtil.startPlay(getActivity(), _weewaInfo.d, _connectDevice, _weewaInfo.n);
                    return;
                }

                if(NetUtil.isConnectedAvailableNetwork()) {
                    if(!LoginManager.isLogon()) {
                        MobileLoginActivity.start(getContext());
                        return;
                    }
                }

                if(mAdapter.getScene() == HomeDeviceAdapter.SCENE_TYE_RECORD) {
                    if(!BaseAppUtil.isDestroy(getActivity()) && BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
                        if(NetUtil.isConnectedAvailableNetwork()) {
                            SetMealTimeManager.getInstance().sendCmdGetPlanUsage(1, getContext(),
                                    success -> {
                                        if(success) {
                                            PlayUtil.startPlay(getActivity(), _weewaInfo.d, _connectDevice, _weewaInfo.n);
                                        }
                                    });
                        } else {
                            PlayUtil.startPlay(getActivity(), _weewaInfo.d, _connectDevice, _weewaInfo.n);
                        }
                    }
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSettings(WeewaSettingsEvent event) {
    // 检查Fragment是否可见且有效
        if (!isFragmentVisible() || BaseAppUtil.isDestroy(getActivity()) || 
            !BaseAppUtil.isForegroundActivity(getActivity(), getActivity().getClass().getName())) {
            return;
        }

        if (event.settings.getAuto_upload() != 0) {
            // 检测云存储
            SetMealCheckManager.checkSetMeal(getContext(), new SetMealCheckManager.CheckSetMealCallback() {
                @Override
                public void onSuccess() {
                    PlayUtil.startPlay(getActivity(), _weewaInfo.d, _connectDevice, _weewaInfo.n);
                }
            },3);
        } else {
            // 没有开启，直接通过
            PlayUtil.startPlay(getActivity(), _weewaInfo.d, _connectDevice, _weewaInfo.n);
        }
    }

    @Override
    protected boolean isCurrentHomeFragment() {
        if(getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).isRecordFragmentCheck();
        }
        return false;
    }
}
