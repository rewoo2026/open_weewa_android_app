package com.xh.hotme.me.camsetting;

import static com.xh.hotme.camera.DeviceListFragment.type_test;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.NetworkBean;
import com.xh.hotme.bean.WeewaInfo;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.bean.DeviceUsageInfoRequestBean;
import com.xh.hotme.bluetooth.bean.GetSettingsRequestBean;
import com.xh.hotme.bluetooth.bean.NetStatusRequestBean;
import com.xh.hotme.bluetooth.bean.PoweroffRequestBean;
import com.xh.hotme.account.CameraTokenManager;
import com.xh.hotme.bluetooth.bean.SetTokenRequestBean;
import com.xh.hotme.bluetooth.bean.WeewaInfoRequestBean;
import com.xh.hotme.bluetooth.event.BleDisConnectSuccessEvent;
import com.xh.hotme.bluetooth.event.DeviceOnlineSwitchEvent;
import com.xh.hotme.bluetooth.event.DeviceWeewaStartEvent;
import com.xh.hotme.bluetooth.event.NetChangeEvent;
import com.xh.hotme.bluetooth.event.PowerOffEvent;
import com.xh.hotme.bluetooth.event.RefreshNetworkInfoEvent;
import com.xh.hotme.bluetooth.event.RemoveDeviceEvent;
import com.xh.hotme.bluetooth.event.WeewaInfoOutEvent;
import com.xh.hotme.bluetooth.listener.IBleDeviceInfoNotifyListener;
import com.xh.hotme.bluetooth.listener.IBleLoginTokenListener;
import com.xh.hotme.bluetooth.listener.IBleWeewaInfoNotifyListener;
import com.xh.hotme.camera.ActiveListActivity;
import com.xh.hotme.camera.CameraListActivity;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.CameraUpgradeActivity;
import com.xh.hotme.camera.MobileUnbindDialog;
import com.xh.hotme.camera.softap.CameraNetworkSettingActivity;
import com.xh.hotme.databinding.ActivityCameraSettingsBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.test.TestActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.CommonDialogUtils;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@IEventBus
public class CameraSettingsActivity extends BaseActivity<ActivityCameraSettingsBinding> implements View.OnClickListener, IBleDeviceInfoNotifyListener, IBleLoginTokenListener, IBleWeewaInfoNotifyListener {
    MobileUnbindDialog _unbindDialog;
    CameraInfo _cameraInfo;

    boolean isDeviceOn = false;
    private boolean waitingForUnbindToken;

    ModalDialog _networkDialog;

    public final static int REQUEST_CODE_PHOTO_SELECT = 0x1000;
    public final static int REQUEST_CODE_REMOTE_POWER_ON = 0x1001;
    public final static int REQUEST_CODE_REMOTE_POWER_OFF = 0x1002;
    public final static int REQUEST_CODE_NET_SETUP = 0x1003;
    public final static int REQUEST_CODE_UNBIND = 0x1004;
    public final static int REQUEST_CODE_SCALE = 0x1005;
    public final static int REQUEST_CODE_WATER_MARK = 0x1006;
    public final static int REQUEST_CODE_LIVE_PASSWORD = 0x1007;
    public final static int REQUEST_CODE_VOICE = 0x1008;
    public final static int REQUEST_CODE_VIDEO_SETTING = 0x1009;
    public final static int REQUEST_CODE_CAMERA_UPGRADE = 0x1010;
    public final static int REQUEST_CODE_VIDEO_UPLOAD = 0x1011;
    public final static int REQUEST_CODE_APN_SETTING = 0x1012;
    public final static int REQUEST_CODE_TEST = 0x1013;
    public final static int REQUEST_CODE_CAMERA_ENVIRONMENT = 0x1014;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothHandle.addWeewaInfoNotifyListener(this);
        BluetoothHandle.addDeviceInfoNotifyListener(this);
        BluetoothHandle.addLoginTokenNotifyListener(this);
    }

    @Override
    protected int getNavigationBarColor() {
        return getColor(R.color.bg_gray_light);
    }

    @Override
    protected void initView() {
        viewBinding.titleBar.tvTitle.setText(getString(R.string.ly_me_photo_set));
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });
        viewBinding.photoSelect.setOnClickListener(this);
        viewBinding.btnPhotoOpen.setOnClickListener(this);
        viewBinding.btnPhotoUnbind.setOnClickListener(this);
        viewBinding.btnConfigNet.setOnClickListener(this);

        viewBinding.btnApiTest.setOnClickListener(this);
        viewBinding.btnConfigWatermark.setOnClickListener(this);
        viewBinding.btnConfigAudio.setOnClickListener(this);
        viewBinding.btnLivePassword.setOnClickListener(this);
        viewBinding.btnConfigVideo.setOnClickListener(this);
        viewBinding.btnUpgrade.setOnClickListener(this);
        viewBinding.btnConfigScale.setOnClickListener(this);
        viewBinding.btnApnSetting.setOnClickListener(this);
        boolean canEditEnvironment = com.xh.hotme.environment.RuntimeEnvironment.canEdit();
        viewBinding.cameraEnvironmentContainer.setVisibility(canEditEnvironment ? View.VISIBLE : View.GONE);
        if (canEditEnvironment) viewBinding.btnCameraEnvironment.setOnClickListener(this);

        viewBinding.apiTestContainer.setVisibility(Constants.shouldShowCameraTest() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void initData() {
    }

    private void getCurrentCamera() {
        _cameraInfo = CameraManager.getCameraInfo("");
        if (_cameraInfo == null) {
            AppTrace.d(TAG, "getCurrentCamera: _cameraInfo="+ "null");
        }else{
            AppTrace.d(TAG, "getCurrentCamera: _cameraInfo="+ _cameraInfo);
        }

        if(_cameraInfo != null && _cameraInfo.deviceInfo != null) {
            String deviceName = BluetoothManager.getDisplayDeviceName(_cameraInfo.deviceInfo.device_name);
            viewBinding.photoName.setText(deviceName);

            if(!DeviceApiManager.isConnected()){
                showDisconnectDialog();
                return;
            }

            DeviceApiManager.sendCmdSimpleObj(new GetSettingsRequestBean());

            //获取设备电量
             _handler.postDelayed(new Runnable() {
                 @Override
                 public void run() {
                     DeviceApiManager.sendCmdSimpleObj(new DeviceUsageInfoRequestBean());
                 }
             }, 2000);
        } else {
            if(DeviceManager.getBindDeviceList() == null || DeviceManager.getBindDeviceList().isEmpty()) {
                viewBinding.photoName.setText(getString(R.string.info_please_bind_device));
            } else {
                viewBinding.photoName.setText(getString(R.string.info_please_connect_device));
            }

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume");
        getCurrentCamera();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BluetoothHandle.removeDeviceInfoNotifyListener(this);
        BluetoothHandle.removeLoginTokenNotifyListener(this);
        waitingForUnbindToken = false;
        BluetoothHandle.removeWeewaInfoNotifyListener(this);

        DeviceApiManager.disConnect();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);

        AppTrace.d(TAG, "onActivityResult: requestCode="+ requestCode);
        CameraInfo cameraInfo = CameraManager.getCameraInfo("");
        if(cameraInfo != null && cameraInfo.deviceInfo != null) {
            if(requestCode == REQUEST_CODE_NET_SETUP){
                doNetworkConfig();
            } else if(requestCode == REQUEST_CODE_VIDEO_UPLOAD){
                doVideoUpload();
            } else if(requestCode == REQUEST_CODE_APN_SETTING){
                doApnSetting();
            } else if(requestCode == REQUEST_CODE_CAMERA_UPGRADE){
                doUpgrade();
            } else if(requestCode == REQUEST_CODE_LIVE_PASSWORD){
                doLivePassword();
            } else if (requestCode == REQUEST_CODE_VIDEO_SETTING) {
                doVideoSetting();
            } else if (requestCode == REQUEST_CODE_VOICE) {
                doVoiceSetting();
            } else if (requestCode == REQUEST_CODE_SCALE) {
                doScale();
            } else if (requestCode == REQUEST_CODE_UNBIND) {
                doUnbindDevice();
            } else if (requestCode == REQUEST_CODE_WATER_MARK) {
                doWatermarkConfig();
            } else if (requestCode == REQUEST_CODE_REMOTE_POWER_ON) {
                doPowerOn();
            } else if (requestCode == REQUEST_CODE_CAMERA_ENVIRONMENT) {
                doCameraEnvironment();
            } else if (requestCode == type_test) {
                doTest();
            }
        }
    }


    private void startSelectCamera(int requestCode) {
        if(DeviceManager.getBindDeviceList() == null || DeviceManager.getBindDeviceList().isEmpty()) {
            Intent intent = new Intent(CameraSettingsActivity.this, ActiveListActivity.class);
            startActivityForResult(intent, requestCode);
        } else {
            Intent intent = new Intent(CameraSettingsActivity.this, CameraListActivity.class);
            startActivityForResult(intent, requestCode);
        }
    }

    private void startSelectAllCamera() {
        ActiveListActivity.start(CameraSettingsActivity.this, type_test);
    }

    private void startSelectCameraForDeviceOn() {
        if(DeviceManager.getBindDeviceList() == null || DeviceManager.getBindDeviceList().isEmpty()) {
            Intent intent = new Intent(CameraSettingsActivity.this, ActiveListActivity.class);
            startActivityForResult(intent, REQUEST_CODE_REMOTE_POWER_ON);
        } else {
            Intent intent = new Intent(CameraSettingsActivity.this, CameraListActivity.class);
            intent.putExtra(Constants.SKIP_CAMERA_INFO, true);
            startActivityForResult(intent, REQUEST_CODE_REMOTE_POWER_ON);
        }
    }

    @Override
    public void onClick(View view) {
        boolean isOk = false;
        int id = view.getId();
        if(id == R.id.photo_select) {
            startSelectCamera(REQUEST_CODE_PHOTO_SELECT);
            //开机
        } else if(id == R.id.btn_photo_open) {
            if(_cameraInfo == null) {
                ToastUtil.s(getString(R.string.please_choice_one_device));
                startSelectCameraForDeviceOn();
                return;
            }
            doPowerOn();
        }  else if(id == R.id.btn_photo_unbind) {
            if(_cameraInfo == null) {
                ToastUtil.s(getString(R.string.please_choice_one_device));
                startSelectCamera(REQUEST_CODE_UNBIND);
                return;
            }

            doUnbindDevice();

            //ai视频剪辑
        } else if(id == R.id.btn_config_net) {
            if(_cameraInfo == null) {
                ToastUtil.s(getString(R.string.please_choice_one_device));
                startSelectCamera(REQUEST_CODE_NET_SETUP);
                return;
            }
           doNetworkConfig();
        } else if(id == R.id.btn_config_watermark) {
            if(_cameraInfo == null) {
                ToastUtil.s(getString(R.string.please_choice_one_device));
                startSelectCamera(REQUEST_CODE_WATER_MARK);
                return;
            }
            doWatermarkConfig();
        } else if(id == R.id.btn_config_audio) {
            if(_cameraInfo == null) {
                ToastUtil.s(getString(R.string.please_choice_one_device));
                startSelectCamera(REQUEST_CODE_VOICE);
                return;
            }
            doVoiceSetting();
        } else if(id == R.id.btn_config_video) {
            if(_cameraInfo == null) {
                ToastUtil.s(getString(R.string.please_choice_one_device));
                startSelectCamera(REQUEST_CODE_VIDEO_SETTING);
                return;
            }
            doVideoSetting();
        }  else if(id == R.id.btn_live_password) {
            if(_cameraInfo == null) {
                ToastUtil.s(getString(R.string.please_choice_one_device));
                startSelectCamera(REQUEST_CODE_LIVE_PASSWORD);
                return;
            }
            doLivePassword();

        } else if(id == R.id.btn_config_scale) {
            if(_cameraInfo == null) {
                ToastUtil.s(getString(R.string.please_choice_one_device));
                startSelectCamera(REQUEST_CODE_SCALE);
                return;
            }
            doScale();
        } else if(id == R.id.btn_camera_environment) {
            if (!com.xh.hotme.environment.RuntimeEnvironment.canEdit()) return;
            if (_cameraInfo == null) {
                ToastUtil.s(getString(R.string.please_choice_one_device));
                startSelectCamera(REQUEST_CODE_CAMERA_ENVIRONMENT);
                return;
            }
            doCameraEnvironment();
        } else if(id == R.id.btn_api_test) {
//            if(Constants.ACCESS_MODEL== Constants.ACCESS_MODEL_REMOTE){
//                CommonDialogUtils.showAccessModelDialog(CameraSettingsActivity.this, Constants.ACCESS_MODEL, new IActionCallback() {
//                    @Override
//                    public void onReturn(int code, Object... objects) {
//
//                    }
//                });
//                return;
//            }
            if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE) {
                if(_cameraInfo == null) {
                    ToastUtil.s(getString(R.string.please_choice_one_test_device));
                    startSelectCamera(REQUEST_CODE_TEST);
                    return;
                }
            } else {
                if(_cameraInfo == null) {
                    ToastUtil.s(getString(R.string.please_choice_one_test_device));
                    startSelectAllCamera();
                    return;
                }
            }

//            if(_cameraInfo == null) {
//                ToastUtil.s(getString(R.string.please_choice_one_test_device));
//                startSelectAllCamera();
//                return;
//            }
            doTest();
        } else if(id == R.id.btn_upgrade) {
            if (_cameraInfo == null) {
                ToastUtil.s(getString(R.string.please_choice_one_device));
                startSelectCamera(REQUEST_CODE_CAMERA_UPGRADE);
                return;
            }
            doUpgrade();
        } else if(id == R.id.btn_apn_setting) {
            if (_cameraInfo == null) {
                ToastUtil.s(getString(R.string.please_choice_one_device));
                startSelectCamera(REQUEST_CODE_APN_SETTING);
                return;
            }
            doApnSetting();
        }
    }

    private void doCameraEnvironment() {
        if (!com.xh.hotme.environment.RuntimeEnvironment.canEdit()) return;
        if (!DeviceApiManager.isConnected()) {
            showDisconnectDialog();
            return;
        }
        com.xh.hotme.environment.RuntimeEnvironmentActivity.start(this, true);
    }

    private void doPowerOn(){
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }

        Intent intent = new Intent(this, PowerOnOffActivity.class);
        startActivity(intent);
    }

    private void doUnbindDevice(){
        if (waitingForUnbindToken) return;

        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }

        NetworkBean networkInfo = CameraManager.getNetworkInfo(null);
        if(networkInfo != null && !CameraManager.isNetworkValid(networkInfo)) {
            Device connectDevice = BluetoothManager.getConnectDevice();

            CameraNetworkSettingActivity.start(CameraSettingsActivity.this, connectDevice, CameraNetworkSettingActivity.REQUEST_NETWORK_CONFIG);
            return;
        }

        DialogUtil.showDialog(getString(R.string.loading_sync_user_status));
        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
        waitingForUnbindToken = true;
        if (Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE) {
            if (!DeviceApiManager.sendCmdSimpleObj(new SetTokenRequestBean(LoginManager.getUserToken()))) {
                onSyncTokenFail("send failed");
            }
        } else {
            CameraTokenManager.syncCurrentToken(this);
        }
    }

    private void doNetworkConfig(){
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }

        Device device = BluetoothManager.getConnectDevice();
        CameraNetworkSettingActivity.start(CameraSettingsActivity.this, device, CameraNetworkSettingActivity.REQUEST_NETWORK_CONFIG);
    }

    private void doWatermarkConfig(){
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }

        Intent intent = new Intent(this, ConfigWatermarkActivity.class);
        startActivity(intent);
    }
    private void doVoiceSetting(){
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }

        Intent intent = new Intent(this, ConfigAudioActivity.class);
        startActivity(intent);
    }
    private void doVideoSetting(){
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }

        Intent intent = new Intent(this, ConfigVideoActivity.class);
        startActivity(intent);
    }

    private void doVideoUpload(){
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }

        Intent intent = new Intent(this, ConfigVideoUploadActivity.class);
        startActivity(intent);
    }

    private void doLivePassword(){
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }

        Intent intent = new Intent(this, ConfigLivePasswordActivity.class);
        startActivity(intent);
    }

    private void doScale(){
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }

        Intent intent = new Intent(this, ConfigScaleActivity.class);
        startActivity(intent);
    }

    private void doUpgrade(){
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }

        DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
        if(deviceInfo == null){
            ToastUtil.s(getString(R.string.please_retry_on_device_info_fail));
            return;
        }
        // The page requests refresh=1 and displays the camera-selected version.
        CameraUpgradeActivity.start(CameraSettingsActivity.this, null);
    }
    private void doTest(){
        TestActivity.start(CameraSettingsActivity.this);
    }

    private void doApnSetting(){
        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }

        Intent intent = new Intent(this, ConfigApnActivity.class);
        startActivity(intent);
    }

    private final Runnable _timeOutRunnable = () -> {
        waitingForUnbindToken = false;
        try {
            DialogUtil.dismissDialog();

        } catch(Throwable e) {

        }
    };

    private void showUnbindDialog(String mac) {
        if(_unbindDialog != null && _unbindDialog.isShowing()) {
            _unbindDialog.dismiss();
        }
        _unbindDialog = null;

        _unbindDialog = new MobileUnbindDialog(CameraSettingsActivity.this, mac);
        _unbindDialog.setOnClickListener((dialog, which) -> dialog.dismiss());
        _unbindDialog.show();
    }

    @Override
    public void onDeviceInfo(DeviceInfo data) {
        isDeviceOn = true;

        if(_handler != null) {
            _handler.removeCallbacks(_timeOutRunnable);
        }
    }

    @Override
    public void onDeviceInfoFail() {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPowerOff(PowerOffEvent event) {
        AppTrace.d(TAG, "onPowerOff");
        if(_handler != null) {
            _handler.removeCallbacks(_timeOutRunnable);
        }
        isDeviceOn = false;
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWeewaStart(DeviceWeewaStartEvent event) {
        AppTrace.d(TAG, "onWeewaStart");
        if(_handler != null) {
            _handler.removeCallbacks(_timeOutRunnable);
        }
        isDeviceOn = true;
    }

    @Override
    public void onSyncToken() {
        AppTrace.d(TAG, "onSyncToken");
        MainHandler.runOnUiThread(() -> {
            if (BaseAppUtil.isDestroy(CameraSettingsActivity.this)) return;
            if (!waitingForUnbindToken) return;
            waitingForUnbindToken = false;
            _handler.removeCallbacks(_timeOutRunnable);
            try {
                DialogUtil.dismissDialog();
                if(_cameraInfo != null) {
                    String mac = _cameraInfo.bluetoothId;
                    showUnbindDialog(mac);
                } else {
                    ToastUtil.s(getString(R.string.please_choice_one_device));
                }

            } catch(Throwable e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onSyncTokenFail(String msg) {
        MainHandler.runOnUiThread(() -> {
            if (BaseAppUtil.isDestroy(CameraSettingsActivity.this)) return;
            if (!waitingForUnbindToken) return;
            waitingForUnbindToken = false;
            _handler.removeCallbacks(_timeOutRunnable);
            try {
                DialogUtil.dismissDialog();
                ToastUtil.s(getString(R.string.info_sync_user_token_fail));
            } catch(Throwable e) {
                e.printStackTrace();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshNetworkInfo(RefreshNetworkInfoEvent event) {

        if(!BaseAppUtil.isDestroy(CameraSettingsActivity.this)) {
            if(_handler != null) {
                _handler.post(() -> DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean()));
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemoveDevice(RemoveDeviceEvent event) {
        if(!BaseAppUtil.isDestroy(CameraSettingsActivity.this)) {
            getCurrentCamera();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWeewaInfoOutEvent(WeewaInfoOutEvent event) {
        if(BaseAppUtil.isDestroy(CameraSettingsActivity.this)) {
            return;
        }
        isDeviceOn = false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceDisconnectSuccess(BleDisConnectSuccessEvent event) {
        AppTrace.d(TAG, "onDeviceDisconnectSuccess");
        getCurrentCamera();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetChanged(NetChangeEvent e) {
        DeviceApiManager.sendCmdSimpleObj(new NetStatusRequestBean());
        if (_networkDialog != null && _networkDialog.isShowing()) {
            if (e.netChangeInfo != null && e.netChangeInfo.wifi == 1) {
                _networkDialog.dismiss();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineSwitchEvent(DeviceOnlineSwitchEvent e) {
        AppTrace.d(TAG, "onDeviceOnlineSwitchEvent");
        if(e.isOnline){
            dismissDeviceOfflineDialog();
        } else {
            showDeviceOfflineDialog();
        }
    }

    @Override
    public void onWeewaInfo(WeewaInfo data) {
        isDeviceOn = true;
    }

    @Override
    public void onWeewaInfoFail() {

    }
}
