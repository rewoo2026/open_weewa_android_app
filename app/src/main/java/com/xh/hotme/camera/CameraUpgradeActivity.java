package com.xh.hotme.camera;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.DeviceInfo;
import com.xh.hotme.bean.DeviceUsageInfo;
import com.xh.hotme.bean.OTAVersionInfo;
import com.xh.hotme.bluetooth.bean.OTADownloadRequestBean;
import com.xh.hotme.bluetooth.bean.OTAInstallRequestBean;
import com.xh.hotme.bluetooth.bean.OTAStatusRequestBean;
import com.xh.hotme.bluetooth.bean.RestartRequestBean;
import com.xh.hotme.bluetooth.bean.WeewaInfoRequestBean;
import com.xh.hotme.bluetooth.event.DeviceWeewaStartEvent;
import com.xh.hotme.bluetooth.event.OTAInstallSucEvent;
import com.xh.hotme.bluetooth.event.OTAStartDownloadEvent;
import com.xh.hotme.bluetooth.event.OTAStartInstallEvent;
import com.xh.hotme.bluetooth.event.OTAStatusEvent;
import com.xh.hotme.bluetooth.event.PowerOffEvent;
import com.xh.hotme.bluetooth.event.RebootEvent;
import com.xh.hotme.camera.ota.OTAConstant;
import com.xh.hotme.camera.ota.OTAStatus;
import com.xh.hotme.databinding.ActivityWeewaUpgradeBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.camera.ota.CameraOtaOffer;
import com.xh.hotme.camera.ota.CameraOtaCommands;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;


@IEventBus
public class CameraUpgradeActivity extends BaseActivity<ActivityWeewaUpgradeBinding> implements EasyPermissions.PermissionCallbacks {

    private final Runnable _statusRunnable = () -> CameraOtaCommands.send(new OTAStatusRequestBean());

    com.xh.hotme.databinding.SettingOtaLayoutVersionInfoBinding versionInfoBindView;
    com.xh.hotme.databinding.SettingOtaLayoutDownloadingBinding downloadingBindView;
    com.xh.hotme.databinding.SettingOtaLayoutDownloadFailBinding downloadFailBindView;
    //    com.xh.hotme.databinding.SettingOtaLayoutInstallBinding installBindView;
    com.xh.hotme.databinding.SettingOtaLayoutInstallFailBinding installFailBindView;
    com.xh.hotme.databinding.SettingOtaLayoutInstallHintBinding installHintBindView;
    com.xh.hotme.databinding.SettingOtaLayoutInstallProgressBinding installProcessBindView;
    com.xh.hotme.databinding.SettingOtaLayoutInstallSuccessBinding installSuccessBindView;
    com.xh.hotme.databinding.SettingOtaLayoutEnergyLowBinding energyLowBindView;

    private int step = 0;
    private int installProgress = 0;
    private int downloadProgress = 0;
    boolean isConfirmDownload = false;

    private Handler _handler = new Handler(android.os.Looper.getMainLooper());
    private boolean isCheckingOta;
    private final Runnable _checkTimeoutRunnable = () -> {
        if (isCheckingOta) {
            isCheckingOta = false;
            _handler.removeCallbacks(_statusRunnable);
            ToastUtil.s(getString(R.string.ota_check_timeout));
            finish();
        }
    };

    OTAVersionInfo _latestOtaInfo;
    public final static String OTA_INFO = "OTA_INFO";

    private boolean isWaitReboot = false;

    private static final int ZORDER_POWEROFF_DIALOG = 20000;


    private final Runnable _timeOutRunnable = () -> {
        DialogUtil.dismissDialog();
    };


    public static void start(Context context, OTAVersionInfo otaVersionInfo) {
        if (null != context) {
            Intent intent = new Intent(context, CameraUpgradeActivity.class);
            intent.putExtra(OTA_INFO, otaVersionInfo);
            context.startActivity(intent);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (viewBinding == null)
            return;


    }

    @Override
    protected void initView() {

        viewBinding.titleBar.tvTitle.setText(getString(R.string.setting_ota_title));
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                finish();
                return true;
            }
        });

        versionInfoBindView = com.xh.hotme.databinding.SettingOtaLayoutVersionInfoBinding.bind(viewBinding.getRoot());
        downloadingBindView = com.xh.hotme.databinding.SettingOtaLayoutDownloadingBinding.bind(viewBinding.getRoot());
        downloadFailBindView = com.xh.hotme.databinding.SettingOtaLayoutDownloadFailBinding.bind(viewBinding.getRoot());
        installHintBindView = com.xh.hotme.databinding.SettingOtaLayoutInstallHintBinding.bind(viewBinding.getRoot());
//        installBindView = com.xh.hotme.databinding.SettingOtaLayoutInstallBinding.bind(viewBinding.getRoot());
        installProcessBindView = com.xh.hotme.databinding.SettingOtaLayoutInstallProgressBinding.bind(viewBinding.getRoot());
        installFailBindView = com.xh.hotme.databinding.SettingOtaLayoutInstallFailBinding.bind(viewBinding.getRoot());
        installSuccessBindView = com.xh.hotme.databinding.SettingOtaLayoutInstallSuccessBinding.bind(viewBinding.getRoot());
        energyLowBindView = com.xh.hotme.databinding.SettingOtaLayoutEnergyLowBinding.bind(viewBinding.getRoot());

        versionInfoBindView.upgradeVersionLayout.setVisibility(View.VISIBLE);
//        installBindView.installLayout.setVisibility(View.GONE);
        downloadingBindView.downloadingLayout.setVisibility(View.GONE);
        downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
        installFailBindView.installFailLayout.setVisibility(View.GONE);
        installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
        installProcessBindView.installProgressLayout.setVisibility(View.GONE);
        installHintBindView.installHintLayout.setVisibility(View.GONE);
        energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

        DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
        viewBinding.tvVersionCurrent.setText(getString(R.string.label_current_version)
                + (deviceInfo == null ? "" : deviceInfo.ota_version));
        viewBinding.tvVersionNew.setText(R.string.ota_checking_camera);
        viewBinding.updateTitle.setText(R.string.ota_checking_camera);
        versionInfoBindView.tvUpgradeContent.setText("");

        viewBinding.pbDownloading.setProgress(100);
        viewBinding.tvDownloadingProgress.setText(getString(R.string.ota_download_img));

        viewBinding.layoutDownloadingLayout.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if (isCheckingOta) return true;

                if (step == OTAConstant.step_energy_low) {
                    finish();
                    return true;
                }

                DeviceUsageInfo deviceUsageInfo = CameraManager.getUsageInfo();
                if(deviceUsageInfo != null) {
                    int energy = deviceUsageInfo.energy;
                    if(energy < 70 && deviceUsageInfo.charging == 0) {
                        step = OTAConstant.step_energy_low;
                        updateStatus();
                        return true;
                    }
                }

                if (step == OTAConstant.step_download_init) {
                    doStartDownload();
                } else if (step == OTAConstant.step_download_fail) {
                    doDownloadFail();
                } else if (step == OTAConstant.step_install_hint) {
                    doInstall();
                } else if (step == OTAConstant.step_install_fail) {
                    doInstallFail();
                } else if (step == OTAConstant.step_install_success) {
                    boolean isOK = doReboot();
                    if(isOK){
                        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                            viewBinding.tvDownloadingProgress.setText(getString(R.string.loading_reboot));
                            // 确认升级的话需要清空提示时间, 这样升级完再进来会再提示ota
                            DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
                            if (deviceInfo != null) {
                                SharedPrefs.getImpl().saveLastOTAPromptTime(deviceInfo.device_id, 0);
                            }
                            CameraManager.clearOTAInfo();
                        } else {
                            // 确认升级的话需要清空提示时间, 这样升级完再进来会再提示ota
                            DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
                            if (deviceInfo != null) {
                                SharedPrefs.getImpl().saveLastOTAPromptTime(deviceInfo.device_id, 0);
                            }
                            CameraManager.clearOTAInfo();
                            viewBinding.tvDownloadingProgress.setText(getString(R.string.loading_install_suc));
                            ToastUtil.s("升级完成，请等待重启成功再试");
                            finish();
                        }
                    }
                }
                return true;
            }
        });
    }

    @Override
    protected void initData() {
        // Ask the camera to refresh its server offer, then poll without refreshing.
        isCheckingOta = true;
        viewBinding.layoutDownloadingLayout.setEnabled(false);
        viewBinding.tvDownloadingProgress.setText(R.string.ota_checking_camera);
        _handler.postDelayed(_checkTimeoutRunnable, 45000);
        if (CameraOtaCommands.send(new OTAStatusRequestBean(true))) {
            _handler.postDelayed(_statusRunnable, 1000);
        } else {
            isCheckingOta = false;
            _handler.removeCallbacks(_checkTimeoutRunnable);
            showDisconnectToast();
            finish();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        _handler.removeCallbacksAndMessages(null);
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
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
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
                            Uri uri = Uri.fromParts("package", CameraUpgradeActivity.this.getPackageName(), null);
                            intent.setData(uri);
                            CameraUpgradeActivity.this.startActivity(intent);
                            return true;
                        }
                    },
                    null
            );
        }
    }


    private void updateStatus() {
        switch (step) {
            case OTAConstant.step_download_init:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.VISIBLE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                viewBinding.pbDownloading.setProgress(100);
                viewBinding.tvDownloadingProgress.setText(getString(R.string.ota_download_img));

                viewBinding.updateTitle.setText(getString(R.string.setting_ota_title_found_new_ota_version));

                break;
            case OTAConstant.step_download_start:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.VISIBLE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
//                installBindView.installLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                viewBinding.pbDownloading.setProgress(100);
                viewBinding.tvDownloadingProgress.setText(getString(R.string.ota_download_img));

                viewBinding.updateTitle.setText(getString(R.string.setting_ota_title_found_new_ota_version));

//                if(isConfirmDownload) {
                boolean isOk = CameraOtaCommands.send(new OTADownloadRequestBean());
                if (!isOk) {
                    showDisconnectToast();
                }
//                }

                break;
            case OTAConstant.step_download_progress:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.VISIBLE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
//                installBindView.installLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                viewBinding.pbDownloading.setProgress(downloadProgress);
                viewBinding.tvDownloadingProgress.setText(String.format(getString(R.string.ota_download_progress), downloadProgress));

                viewBinding.updateTitle.setText(getString(R.string.setting_ota_title_download));
                break;
            case OTAConstant.step_download_fail:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.VISIBLE);
//                installBindView.installLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                viewBinding.pbDownloading.setProgress(100);
                viewBinding.tvDownloadingProgress.setText(getString(R.string.ota_download_img));

                viewBinding.updateTitle.setText(getString(R.string.setting_ota_title_download_fail));
                break;
            case OTAConstant.step_download_success:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.VISIBLE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                viewBinding.pbDownloading.setProgress(downloadProgress);
                viewBinding.tvDownloadingProgress.setText(getString(R.string.ota_upgrade_install_begin));

                viewBinding.updateTitle.setText(getString(R.string.setting_ota_title_upgrade));
                break;
            case OTAConstant.step_install_hint:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
//                installBindView.installLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.VISIBLE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                viewBinding.pbDownloading.setProgress(100);
                viewBinding.tvDownloadingProgress.setText(getString(R.string.ota_upgrade_install_begin));

                viewBinding.updateTitle.setText(getString(R.string.setting_ota_title_upgrade));
                break;
            case OTAConstant.step_install_progress:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
//                installBindView.installLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.VISIBLE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                viewBinding.pbDownloading.setProgress(installProgress);
                viewBinding.tvDownloadingProgress.setText(String.format(getString(R.string.ota_upgrade_progress), installProgress));
                viewBinding.updateTitle.setText(getString(R.string.setting_ota_title_upgrading));
                break;
            case OTAConstant.step_install_fail:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
//                installBindView.installLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.VISIBLE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                viewBinding.pbDownloading.setProgress(100);
                viewBinding.tvDownloadingProgress.setText(getString(R.string.ota_upgrade_install_begin));

                viewBinding.updateTitle.setText(getString(R.string.setting_ota_title_upgrade_fail));
                break;
            case OTAConstant.step_install_success:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
//                installBindView.installLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.VISIBLE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                viewBinding.pbDownloading.setProgress(100);
                viewBinding.tvDownloadingProgress.setText(getString(R.string.setting_ota_title_upgrade_complete));

                installSuccessBindView.tvInstallSucContent.setText(String.format(getString(R.string.ota_upgrade_to_version), (_latestOtaInfo == null ? "" : _latestOtaInfo.version)));

                viewBinding.updateTitle.setText(getString(R.string.setting_ota_title_upgrade_complete));
                break;
            case OTAConstant.step_energy_low:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
//                installBindView.installLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.VISIBLE);


                viewBinding.pbDownloading.setProgress(100);
                viewBinding.tvDownloadingProgress.setText(getString(R.string.confirm));
                viewBinding.updateTitle.setText(getString(R.string.setting_ota_title_energy_low));
                break;

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPowerOffEvent(PowerOffEvent event) {
        AppTrace.d(TAG, "onPowerOffEvent");

        ModalDialog dialog = new ModalDialog(this);
        dialog.setZOrder(ZORDER_POWEROFF_DIALOG);
        dialog.setTitle(getString(R.string.info_device_exit));
        dialog.setMessage(getString(R.string.info_device_power_off));
        dialog.setPositiveButton(getString(R.string.confirm), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                CameraUpgradeActivity.this.finish();
                return true;
            }
        });
        dialog.setCancelable(false);
        dialog.hideNegativeButton();
        dialogStack.push(dialog);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRebootEvent(RebootEvent event) {
        AppTrace.d(TAG, "onRebootEvent");
        _handler.removeCallbacks(_timeOutRunnable);

        DialogUtil.showDialog(getString(R.string.loading_ota_upgrade_reboot));

        _handler.postDelayed(_timeOutRunnable, 20000);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWeewaStartEvent(DeviceWeewaStartEvent event) {
        AppTrace.d(TAG, "onStartEvent");
        _handler.removeCallbacks(_timeOutRunnable);
        DialogUtil.dismissDialog();
        DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());
        if (isWaitReboot) {
            viewBinding.tvDownloadingProgress.setText(getString(R.string.weewa_reboot_success));
            ToastUtil.s(getString(R.string.weewa_reboot_success));

            // 因为这里增加了等待重启的操作，所以当重启完成后，清空ota信息
            DeviceInfo deviceInfo = CameraManager.getDeviceInfo();
            if (deviceInfo != null) {
                SharedPrefs.getImpl().saveLastOTAPromptTime(deviceInfo.device_id, 0);
            }
            CameraManager.clearOTAInfo();

            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartDownloadEvent(OTAStartDownloadEvent event) {
        AppTrace.d(TAG, "onStartDownloadEvent");
        DialogUtil.dismissDialog();
        if (event.status == 1) {
            step = OTAConstant.step_download_progress;  //下载开始后，进入下载中的状态
            updateStatus();
            _handler.post(_statusRunnable);
        } else {
            if (event.errorCode == 2) {
                ToastUtil.s(getString(R.string.info_camera_without_network));
                if (_handler != null) {
                    _handler.removeCallbacksAndMessages(null);
                }
                //更新下相机信息
                DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());
            }else if (event.errorCode == 3) {
                ToastUtil.s(getString(R.string.info_camera_version_is_latest));
                if (_handler != null) {
                    _handler.removeCallbacksAndMessages(null);
                }
                //更新下相机信息
                DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());
            } else if (event.errorCode == 4) {
                step = OTAConstant.step_install_hint;  //已经下载完成，直接跳转到安装流程
                updateStatus();
                _handler.post(_statusRunnable);
            } else {
                CameraOtaCommands.send(new OTAStatusRequestBean());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartInstallEvent(OTAStartInstallEvent event) {
        AppTrace.d(TAG, "onStartInstallEvent");
        DialogUtil.dismissDialog();
        if (event.status == 1) {
            step = OTAConstant.step_install_progress;
            updateStatus();
            DialogUtil.dismissDialog();
            _handler.postDelayed(_statusRunnable, 1000);
        } else {
            step = OTAConstant.step_install_fail;
            updateStatus();
            ToastUtil.s(getString(R.string.ota_upgrade_install_fail));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOTAStatusEvent(OTAStatusEvent event) {
        OTAStatus curStatus = OTAStatus.fromOrdinal(event.status);
        if (curStatus == null) return;
        if (CameraOtaOffer.isPending(event)) {
            if (!isCheckingOta) _handler.postDelayed(_checkTimeoutRunnable, 45000);
            isCheckingOta = true;
            viewBinding.layoutDownloadingLayout.setEnabled(false);
            viewBinding.tvDownloadingProgress.setText(R.string.ota_checking_camera);
            _handler.removeCallbacks(_statusRunnable);
            _handler.postDelayed(_statusRunnable, 1000);
            return;
        }
        isCheckingOta = false;
        DialogUtil.dismissDialog();
        _handler.removeCallbacks(_checkTimeoutRunnable);
        _handler.removeCallbacks(_statusRunnable);
        if (curStatus == OTAStatus.OTA_LATEST || curStatus == OTAStatus.OTA_WAIT_NETWORK) {
            ToastUtil.s(getString(curStatus == OTAStatus.OTA_LATEST
                    ? R.string.info_camera_version_is_latest : R.string.info_camera_without_network));
            DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());
            finish();
            return;
        }
        _latestOtaInfo = CameraOtaOffer.fromStatus(event);
        if (_latestOtaInfo == null) {
            viewBinding.layoutDownloadingLayout.setEnabled(false);
            ToastUtil.s(getString(R.string.ota_camera_offer_mismatch));
            finish();
            return;
        }
        viewBinding.tvVersionCurrent.setText(getString(R.string.label_current_version) + event.versionName);
        viewBinding.tvVersionNew.setText(getString(R.string.label_latest_version) + _latestOtaInfo.version);
        viewBinding.updateTitle.setText(R.string.setting_ota_title_found_new_ota_version);
        versionInfoBindView.tvUpgradeContent.setText(R.string.ota_update_description_unavailable);
        downloadingBindView.tvDownloadingContent.setText(R.string.ota_update_description_unavailable);
        viewBinding.layoutDownloadingLayout.setEnabled(true);
        int progress = event.progress;
        switch (curStatus) {
            case OTA_UPGRADABLE:  //下载完成
                step = OTAConstant.step_install_hint;
                downloadProgress = progress;
                break;
            case OTA_DOWNLOADING:  //下载中
                step = OTAConstant.step_download_progress;
                downloadProgress = progress;
                _handler.postDelayed(_statusRunnable, 1000);
                break;
            case OTA_DOWNLOAD_FAILED:
                step = OTAConstant.step_download_fail;
                break;

            case OTA_DOWNLOADABLE:
                step = OTAConstant.step_download_init;
                break;

            case OTA_UPGRADED:
                step = OTAConstant.step_install_success;
                break;
            case OTA_UPGRADING:
                step = OTAConstant.step_install_progress;
                installProgress = progress;
                _handler.postDelayed(_statusRunnable, 1000);
                break;
            case OTA_UPGRADE_FALED:
                step = OTAConstant.step_install_fail;
                break;
        }
        updateStatus();

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOTAInstallSuccessEvent(OTAInstallSucEvent event) {
        AppTrace.d(TAG, "onOTAInstallSuccessEvent");
        //安装完成后，移除定时状态的查询
        _handler.removeCallbacks(_statusRunnable);

        step = OTAConstant.step_install_success;
        updateStatus();
    }

    private boolean doDownload() {
        isConfirmDownload = true;
        DeviceUsageInfo deviceUsageInfo = CameraManager.getUsageInfo();
        if (deviceUsageInfo != null) {
            int energy = deviceUsageInfo.energy;
            if (energy < 70 && deviceUsageInfo.charging == 0) {
                step = OTAConstant.step_energy_low;
                updateStatus();
                return false;
            }
        }

        boolean isOk = CameraOtaCommands.send(new OTAStatusRequestBean());
        if (!isOk) {
            showDisconnectToast();
            return false;
        } else {
            DialogUtil.showDialog("正在加载");
        }

        return true;
    }

    private boolean doStartDownload() {
        if (!CameraManager.hasWifi() && !CameraManager.hasEthernet() && !isConfirmDownload) {
            ModalDialog dialog = new ModalDialog(this);
            dialog.setTitle(getString(R.string.tip));
            dialog.setMessage(getString(R.string.setting_ota_camera_network_sim_tip));
            dialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                @Override public boolean onClicked() { return true; }
            });
            dialog.setPositiveButton(getString(R.string.upgrade), new ClickGuard.GuardedOnClickListener() {
                @Override public boolean onClicked() {
                    isConfirmDownload = true;
                    doStartDownload();
                    return true;
                }
            });
            dialog.show();
            return false;
        }
        isConfirmDownload = true;
        DeviceUsageInfo deviceUsageInfo = CameraManager.getUsageInfo();
        if (deviceUsageInfo != null) {
            int energy = deviceUsageInfo.energy;
            if (energy < 70 && deviceUsageInfo.charging == 0) {
                step = OTAConstant.step_energy_low;
                updateStatus();
                return false;
            }
        }

        boolean isOk = CameraOtaCommands.send(new OTADownloadRequestBean());
        if (!isOk) {
            showDisconnectToast();
            return false;
        }

        return true;
    }

    private boolean doDownloadFail() {
        boolean isOk = CameraOtaCommands.send(new OTADownloadRequestBean());
        if (!isOk) {
            showDisconnectToast();
            return false;
        }
        return true;
    }

    private boolean doReboot() {
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(new RestartRequestBean());
        if (!isOk) {
            showDisconnectToast();
            return false;
        }
        isWaitReboot = true;
        return true;
    }

    private boolean doInstall() {
        boolean isOk = CameraOtaCommands.send(new OTAInstallRequestBean(0));
        if (!isOk) {
            showDisconnectToast();
            return false;
        }
        return true;
    }

    private boolean doInstallFail() {
        boolean isOk = CameraOtaCommands.send(new OTAInstallRequestBean(0));
        if (!isOk) {
            showDisconnectToast();
            return false;
        }
        return true;
    }
}
