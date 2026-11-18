package com.xh.hotme.camera.ota;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.bean.DeviceUsageInfo;
import com.xh.hotme.bean.OTAVersionInfo;
import com.xh.hotme.bluetooth.bean.OTADownloadRequestBean;
import com.xh.hotme.bluetooth.bean.OTAInstallRequestBean;
import com.xh.hotme.bluetooth.bean.OTAStatusRequestBean;
import com.xh.hotme.bluetooth.bean.WeewaInfoRequestBean;
import com.xh.hotme.bluetooth.event.OTAInstallSucEvent;
import com.xh.hotme.bluetooth.event.OTAStartDownloadEvent;
import com.xh.hotme.bluetooth.event.OTAStartInstallEvent;
import com.xh.hotme.bluetooth.event.OTAStatusEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.databinding.DialogOtaUpgradeVersionBinding;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


/**
 * @Description:
 * @Author: Maclay
 * @Date: 2023/6/15 17:19
 */
public class OTAUpdateDialog extends BaseDialog<DialogOtaUpgradeVersionBinding> {
    private final static String TAG = "OTAUpdateDialog";

    private int step = 0;
    private int installProgress = 0;
    private int downloadProgress = 0;

    private final static int step_download_start = 1;
    private final static int step_download_progress = 2;
    private final static int step_download_fail = 3;
    private final static int step_download_success = 4;
    private final static int step_install_hint = 5;
    private final static int step_install_progress = 6;
    private final static int step_install_fail = 7;
    private final static int step_install_success = 8;
    private final static int step_energy_low = 9;


    com.xh.hotme.databinding.OtaLayoutVersionInfoBinding versionInfoBindView;
    com.xh.hotme.databinding.OtaLayoutDownloadingBinding downloadingBindView;
    com.xh.hotme.databinding.OtaLayoutDownloadFailBinding downloadFailBindView;
    com.xh.hotme.databinding.OtaLayoutInstallBinding installBindView;
    com.xh.hotme.databinding.OtaLayoutInstallFailBinding installFailBindView;
    com.xh.hotme.databinding.OtaLayoutInstallHintBinding installHintBindView;
    com.xh.hotme.databinding.OtaLayoutInstallProgressBinding installProcessBindView;
    com.xh.hotme.databinding.OtaLayoutInstallSuccessBinding installSuccessBindView;
    com.xh.hotme.databinding.OtaLayoutEnergyLowBinding energyLowBindView;

    private Handler _handler;
    private boolean isCheckingOta;

    private  OTAVersionInfo _latestOtaInfo;

    boolean isConfirmDownload = false;

    private final Runnable _statusRunnable = () -> CameraOtaCommands.send(new OTAStatusRequestBean());


    public interface ICallback {
        void onCancel();

        void onConfirm();
    }

    final Context _ctx;

    private ICallback iCallback;
    private final Runnable _checkTimeoutRunnable = () -> {
        if (isCheckingOta) {
            isCheckingOta = false;
            _handler.removeCallbacks(_statusRunnable);
            DialogUtil.dismissDialog();
            ToastUtil.s(getContext().getString(R.string.ota_check_timeout));
            if (iCallback != null) iCallback.onCancel();
            dismiss();
        }
    };

    public OTAUpdateDialog(@NonNull Context context, OTAVersionInfo latestOtaInfo) {
        super(context);
        _ctx = context;
        _latestOtaInfo = latestOtaInfo;
    }

    public OTAUpdateDialog setCallback(ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        requestCameraOtaRefresh();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if(EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        isCheckingOta = false;
        if (_handler != null) _handler.removeCallbacksAndMessages(null);
    }

    public void setOTAVersionInfo(OTAVersionInfo latestOtaInfo){
        this._latestOtaInfo = latestOtaInfo;
        if (_latestOtaInfo != null) {
            if (TextUtils.isEmpty(_latestOtaInfo.updateTitle)) {
                versionInfoBindView.tvUpgradeTitle.setText(String.format("%s%s", getContext().getString(R.string.ota_version_upgrade), _latestOtaInfo.getVersion()));
            } else {
                versionInfoBindView.tvUpgradeTitle.setText(_latestOtaInfo.updateTitle);
            }
            versionInfoBindView.tvUpgradeContent.setText(_latestOtaInfo.getDescr() == null ? getContext().getString(R.string.ota_update_description_unavailable) : _latestOtaInfo.getDescr());

            installSuccessBindView.tvInstallSucContent.setText(String.format(getContext().getString(R.string.ota_upgrade_install_suc_content), _latestOtaInfo.getVersion()));
        }
    }


    @Override
    protected void onInit() {
        if(viewBinding == null)
            return;
        _handler = new android.os.Handler();

        versionInfoBindView = com.xh.hotme.databinding.OtaLayoutVersionInfoBinding.bind(viewBinding.getRoot());
        downloadingBindView = com.xh.hotme.databinding.OtaLayoutDownloadingBinding.bind(viewBinding.getRoot());
        downloadFailBindView = com.xh.hotme.databinding.OtaLayoutDownloadFailBinding.bind(viewBinding.getRoot());
        installHintBindView = com.xh.hotme.databinding.OtaLayoutInstallHintBinding.bind(viewBinding.getRoot());
        installBindView = com.xh.hotme.databinding.OtaLayoutInstallBinding.bind(viewBinding.getRoot());
        installProcessBindView = com.xh.hotme.databinding.OtaLayoutInstallProgressBinding.bind(viewBinding.getRoot());
        installFailBindView = com.xh.hotme.databinding.OtaLayoutInstallFailBinding.bind(viewBinding.getRoot());
        installSuccessBindView = com.xh.hotme.databinding.OtaLayoutInstallSuccessBinding.bind(viewBinding.getRoot());
        energyLowBindView = com.xh.hotme.databinding.OtaLayoutEnergyLowBinding.bind(viewBinding.getRoot());

        versionInfoBindView.upgradeVersionLayout.setVisibility(View.VISIBLE);
        installBindView.installLayout.setVisibility(View.GONE);
        downloadingBindView.downloadingLayout.setVisibility(View.GONE);
        downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
        installFailBindView.installFailLayout.setVisibility(View.GONE);
        installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
        installProcessBindView.installProgressLayout.setVisibility(View.GONE);
        installHintBindView.installHintLayout.setVisibility(View.GONE);
        energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

        setOTAVersionInfo(_latestOtaInfo);

        versionInfoBindView.tvUpgradeConfirm.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if (isCheckingOta) return true;
                isConfirmDownload = true;
                DeviceUsageInfo deviceUsageInfo = CameraManager.getUsageInfo();
                if(deviceUsageInfo != null) {
                    int energy = deviceUsageInfo.energy;
                    if(energy < 70 && deviceUsageInfo.charging == 0) {
                        step = step_energy_low;
                        updateStatus();
                        return true;
                    }
                }

                boolean isOk = requestCameraOtaRefresh();
                if(!isOk) {
                    showDisconnectToast();
                } else {
                    DialogUtil.showDialog("正在加载");
                }
                return true;
            }
        });
        versionInfoBindView.tvUpgradeNext.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(iCallback != null) {
                    iCallback.onCancel();
                }
                return true;
            }
        });

        downloadFailBindView.tvDownloadFailConfirm.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if (isCheckingOta) return true;
                boolean isOk = CameraOtaCommands.send(new OTADownloadRequestBean());
                if(!isOk) {
                    showDisconnectToast();
                }
                return true;
            }
        });

        installBindView.tvInstallConfirm.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                step = step_install_hint;
                updateStatus();
                return true;
            }
        });

        installHintBindView.tvInstallHintConfirm.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if (isCheckingOta) return true;
                boolean isOk = CameraOtaCommands.send(new OTAInstallRequestBean(1));
                if(!isOk) {
                    showDisconnectToast();
                } else {
                    DialogUtil.showDialog(getContext().getString(R.string.loading));
                }
                return true;
            }
        });

        installFailBindView.tvInstallFailConfirm.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if (isCheckingOta) return true;
                boolean isOk = CameraOtaCommands.send(new OTAInstallRequestBean(1));
                if(!isOk) {
                    showDisconnectToast();
                }

                return true;
            }
        });


        installFailBindView.tvUpgradeInstallFailExit.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_handler != null) {
                    _handler.removeCallbacksAndMessages(null);
                }

                if(iCallback != null) {
                    iCallback.onCancel();
                }
                return true;
            }
        });


        installSuccessBindView.tvInstallSucConfirm.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(_handler != null) {
                    _handler.removeCallbacksAndMessages(null);
                }
                if(iCallback != null) {
                    iCallback.onConfirm();
                }

                return true;
            }
        });
        energyLowBindView.tvLowEnergyConfirm.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_handler != null) {
                    _handler.removeCallbacksAndMessages(null);
                }

                if(iCallback != null) {
                    iCallback.onCancel();
                }
                return true;
            }
        });

        setOnDismissListener(dialog -> {
            if(_handler != null) {
                _handler.removeCallbacksAndMessages(null);
            }
        });

        setCancelable(false);
        setCanceledOnTouchOutside(false);

    }

    private boolean requestCameraOtaRefresh() {
        isCheckingOta = true;
        versionInfoBindView.tvUpgradeConfirm.setEnabled(false);
        _handler.removeCallbacks(_checkTimeoutRunnable);
        _handler.postDelayed(_checkTimeoutRunnable, 45000);
        boolean sent = CameraOtaCommands.send(new OTAStatusRequestBean(true));
        if (sent) {
            _handler.removeCallbacks(_statusRunnable);
            _handler.postDelayed(_statusRunnable, 1000);
        } else {
            isCheckingOta = false;
            _handler.removeCallbacks(_checkTimeoutRunnable);
            versionInfoBindView.tvUpgradeConfirm.setEnabled(false);
            isConfirmDownload = false;
            showDisconnectToast();
            if (iCallback != null) iCallback.onCancel();
            dismiss();
        }
        return sent;
    }


    private void updateStatus() {
        switch(step) {
            case step_download_start:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.VISIBLE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
                installBindView.installLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                if(isConfirmDownload) {
                    isConfirmDownload = false;
                    boolean isOk = CameraOtaCommands.send(new OTADownloadRequestBean());
                    if(!isOk) {
                        showDisconnectToast();
                    }
                }

                break;
            case step_download_progress:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.VISIBLE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
                installBindView.installLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                downloadingBindView.tvDownloadingProgress.setText(downloadProgress + "%");
                downloadingBindView.pbDownloading.setProgress(downloadProgress);
                break;
            case step_download_fail:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.VISIBLE);
                installBindView.installLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                break;
            case step_download_success:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
                installBindView.installLayout.setVisibility(View.VISIBLE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                break;
            case step_install_hint:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
                installBindView.installLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.VISIBLE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                break;
            case step_install_progress:


                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
                installBindView.installLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.VISIBLE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                installProcessBindView.pbInstalling.setProgress(installProgress);
                break;
            case step_install_fail:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
                installBindView.installLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.VISIBLE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                break;
            case step_install_success:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
                installBindView.installLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.VISIBLE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.GONE);

                break;
            case step_energy_low:

                versionInfoBindView.upgradeVersionLayout.setVisibility(View.GONE);
                downloadingBindView.downloadingLayout.setVisibility(View.GONE);
                downloadFailBindView.downloadFailLayout.setVisibility(View.GONE);
                installBindView.installLayout.setVisibility(View.GONE);
                installHintBindView.installHintLayout.setVisibility(View.GONE);
                installProcessBindView.installProgressLayout.setVisibility(View.GONE);
                installFailBindView.installFailLayout.setVisibility(View.GONE);
                installSuccessBindView.installSuccessLayout.setVisibility(View.GONE);
                energyLowBindView.lowEnergyLayout.setVisibility(View.VISIBLE);

                break;

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartDownloadEvent(OTAStartDownloadEvent event) {
        AppTrace.d(TAG, "onStartDownloadEvent");
        DialogUtil.dismissDialog();
        if(event.status == 1) {
            step = step_download_progress;  //下载开始后，进入下载中的状态
            updateStatus();
            _handler.post(_statusRunnable);
        } else {
            if (event.errorCode == 2) {
                ToastUtil.s(getContext().getString(R.string.info_camera_without_network));
                if (_handler != null) {
                    _handler.removeCallbacksAndMessages(null);
                }
                //更新下相机信息
                DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());
            }else if (event.errorCode == 3) {
                ToastUtil.s(getContext().getString(R.string.info_camera_version_is_latest));
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
        if(event.status == 1) {
            step = step_install_progress;
            updateStatus();
            DialogUtil.dismissDialog();
            _handler.postDelayed(_statusRunnable, 1000);
        } else {
            step = step_install_fail;
            updateStatus();
            ToastUtil.s(getContext().getString(R.string.ota_upgrade_install_fail));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOTAStatusEvent(OTAStatusEvent event) {
        OTAStatus curStatus = OTAStatus.fromOrdinal(event.status);
        if (curStatus == null) return;
        if (CameraOtaOffer.isPending(event)) {
            if (!isCheckingOta) _handler.postDelayed(_checkTimeoutRunnable, 45000);
            isCheckingOta = true;
            versionInfoBindView.tvUpgradeConfirm.setEnabled(false);
            _handler.removeCallbacks(_statusRunnable);
            _handler.postDelayed(_statusRunnable, 1000);
            return;
        }
        isCheckingOta = false;
        DialogUtil.dismissDialog();
        _handler.removeCallbacks(_checkTimeoutRunnable);
        _handler.removeCallbacks(_statusRunnable);
        if (curStatus == OTAStatus.OTA_LATEST || curStatus == OTAStatus.OTA_WAIT_NETWORK) {
            ToastUtil.s(getContext().getString(curStatus == OTAStatus.OTA_LATEST
                    ? R.string.info_camera_version_is_latest : R.string.info_camera_without_network));
            isConfirmDownload = false;
            if (iCallback != null) iCallback.onCancel();
            dismiss();
            return;
        }
        OTAVersionInfo offer = CameraOtaOffer.fromStatus(event);
        if (offer == null) {
            isConfirmDownload = false;
            ToastUtil.s(getContext().getString(R.string.ota_camera_offer_mismatch));
            if (iCallback != null) iCallback.onCancel();
            dismiss();
            return;
        }
        setOTAVersionInfo(offer);
        versionInfoBindView.tvUpgradeConfirm.setEnabled(true);
        int progress = event.progress;
        switch(curStatus) {
            case OTA_UPGRADABLE:  //下载完成
                step = step_download_success;
                downloadProgress = progress;
                break;
            case OTA_DOWNLOADING:  //下载中
                step = step_download_progress;
                downloadProgress = progress;
                _handler.postDelayed(_statusRunnable, 1000);
                break;
            case OTA_DOWNLOAD_FAILED:
                step = step_download_fail;
                break;

            case OTA_DOWNLOADABLE:
                step = step_download_start;
                break;

            case OTA_UPGRADED:
                step = step_install_success;
                break;
            case OTA_UPGRADING:
                step = step_install_progress;
                installProgress = progress;
                _handler.postDelayed(_statusRunnable, 1000);
                break;
            case OTA_UPGRADE_FALED:
                step = step_install_fail;
                break;
        }
        updateStatus();

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOTAInstallSuccessEvent(OTAInstallSucEvent event) {
        AppTrace.d(TAG, "onOTAInstallSuccessEvent");
        step = step_install_success;
        updateStatus();
    }
}
