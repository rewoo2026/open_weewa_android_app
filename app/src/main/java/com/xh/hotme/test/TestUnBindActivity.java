package com.xh.hotme.test;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.event.RemoveDeviceEvent;
import com.xh.hotme.bluetooth.event.UnbindEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivityUnbindDeviceBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.listener.IApiCallback;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;


public class TestUnBindActivity extends BaseActivity<ActivityUnbindDeviceBinding> {

    private final static String TAG = TestUnBindActivity.class.getSimpleName();

    UnbindDialog unbindDialog;

    CameraInfo _cameraInfo;

    public static void start(Context context) {
        Intent intent = new Intent(context, TestUnBindActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {
        TitleLayoutHelper.initTitle(viewBinding.titleBar, "设备解绑");

    }

    @Override
    protected void initData() {

        viewBinding.llCurrentDevice.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _cameraInfo = CameraManager.getCameraInfo("");
                if(_cameraInfo == null || _cameraInfo.deviceInfo == null) {
                    ToastUtil.s(getString(R.string.please_retry_on_device_info_fail));
                    return true;
                }

                if(!NetUtil.isConnectedAvailableNetwork()) {
                    ToastUtil.s(getString(R.string.info_need_outside_network_on_unbind_device));
                    return true;
                }

                unbindDevice(UnbindDialog.UNBIND_DEVICE_ID, _cameraInfo.deviceInfo.device_id);

                return true;
            }
        });

        viewBinding.llDeviceName.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(unbindDialog != null && unbindDialog.isShowing()) {
                    unbindDialog.dismiss();
                }
                unbindDialog = null;

                if(!NetUtil.isConnectedAvailableNetwork()) {
                    ToastUtil.s(getString(R.string.info_need_outside_network_on_unbind_device));
                    return true;
                }

                unbindDialog = new UnbindDialog(TestUnBindActivity.this, UnbindDialog.UNBIND_DEVICE_NAME);
                unbindDialog.setOnClickListener(new UnbindDialog.IUnbindDeviceListener() {
                    @Override
                    public void onConfirm(String deviceId) {
                        DialogUtil.showDialog(getString(R.string.loading_unbinding));

                        unbindDevice(UnbindDialog.UNBIND_DEVICE_NAME, deviceId);
                    }

                    @Override
                    public void onCancel() {
                        unbindDialog.dismiss();
                    }
                });
                unbindDialog.showOnCenter(15);
                return true;
            }
        });


        viewBinding.llPhoneUnbind.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(unbindDialog != null && unbindDialog.isShowing()) {
                    unbindDialog.dismiss();
                }
                unbindDialog = null;

                if(!NetUtil.isConnectedAvailableNetwork()) {
                    ToastUtil.s(getString(R.string.info_need_outside_network_on_unbind_device));
                    return true;
                }

                unbindDialog = new UnbindDialog(TestUnBindActivity.this, UnbindDialog.UNBIND_PHONE);
                unbindDialog.setOnClickListener(new UnbindDialog.IUnbindDeviceListener() {
                    @Override
                    public void onConfirm(String deviceId) {
                        DialogUtil.showDialog(getString(R.string.loading_unbinding));

                        unbindDevice(UnbindDialog.UNBIND_PHONE, deviceId);
                    }

                    @Override
                    public void onCancel() {
                        unbindDialog.dismiss();
                    }
                });
                unbindDialog.showOnCenter(15);
                return true;
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        DialogUtil.dismissDialog();
        if(unbindDialog != null && unbindDialog.isShowing()) {
            unbindDialog.dismiss();
        }
        unbindDialog = null;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);

        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    private void unbindDevice(int unbindType, String deviceId) {
        ApiUtil.unbindValidate(deviceId, new IApiCallback() {
            @Override
            public void onSuccess() {
                ToastUtil.s(getString(R.string.device_unbind_ok));

                if(unbindDialog != null && unbindDialog.isShowing()) {
                    unbindDialog.dismiss();
                }
                if(unbindType == UnbindDialog.UNBIND_DEVICE_ID) {
                    BluetoothManager.getImpl().disconnectBle();
                    CameraManager.release();
                    DeviceManager.getImpl().removeBindDeviceData(_cameraInfo.bluetoothId);
                    CameraManager.setDeviceId("");
                    EventBus.getDefault().post(new RemoveDeviceEvent(_cameraInfo.bluetoothId));
                    EventBus.getDefault().post(new UnbindEvent());
                    finish();

                } else if(unbindType == UnbindDialog.UNBIND_DEVICE_NAME) {
                    if(_cameraInfo.deviceInfo.device_name.equalsIgnoreCase(deviceId)) {
                        BluetoothManager.getImpl().disconnectBle();
                        CameraManager.release();
                        DeviceManager.getImpl().removeBindDeviceData(_cameraInfo.bluetoothId);
                        CameraManager.setDeviceId("");
                        EventBus.getDefault().post(new RemoveDeviceEvent(_cameraInfo.bluetoothId));
                        EventBus.getDefault().post(new UnbindEvent());
                        finish();
                    }
                } else if(unbindType == UnbindDialog.UNBIND_PHONE) {
                    String mobile = LoginManager.getMobile();
                    if(!TextUtils.isEmpty(mobile) && mobile.equalsIgnoreCase(deviceId)) {
                        BluetoothManager.getImpl().disconnectBle();
                        CameraManager.release();
                        CameraManager.setDeviceId("");
                        DeviceManager.getImpl().clearDeviceList();
                        DeviceManager.getImpl().onRefreshList(false);
                        EventBus.getDefault().post(new UnbindEvent());
                        finish();
                    }
                }

            }

            @Override
            public void onFail(String code, String message) {
                ToastUtil.s(message);
            }

            @Override
            public void onFinish() {
                DialogUtil.dismissDialog();
            }
        });
    }
}
