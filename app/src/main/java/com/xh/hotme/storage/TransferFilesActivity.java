package com.xh.hotme.storage;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.camera.softap.WifiManager;
import com.xh.hotme.camera.wifi.HotmeWiFiManager;
import com.xh.hotme.databinding.ActivityTransferfilesBinding;
import com.xh.hotme.databinding.ItemTransferfilesConnectBinding;
import com.xh.hotme.databinding.ItemTransferfilesEmptyBinding;
import com.xh.hotme.databinding.ItemTransferfilesOkBinding;
import com.xh.hotme.http.ConnectLogic;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.FileSizeUtil;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.PermissionsUtil;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * date   : 2023/6/22
 * desc   :传输文件
 */
public class TransferFilesActivity extends BaseActivity<ActivityTransferfilesBinding> implements NetworkManager.OnNetworkStatusChangedListener, EasyPermissions.PermissionCallbacks {
    protected static final String TAG = TransferFilesActivity.class.getSimpleName();
    int state = 0;//状态 0无内容 1下载ing 2成功
    ItemTransferfilesEmptyBinding emptyView;
    ItemTransferfilesConnectBinding connectBinding;
    ItemTransferfilesOkBinding okBinding;

    String labelDownloadFileSize;
    String labelDownloadSpeedRate;
    String labelDownloadTime;
    String labelDownloadConnecting;

    ModalDialog errorDialog;

    @Override
    protected void initView() {
        labelDownloadFileSize = getString(R.string.label_download_file_size);
        labelDownloadSpeedRate = getString(R.string.label_download_speed_rate);
        labelDownloadTime = getString(R.string.label_download_time);
        labelDownloadConnecting = getString(R.string.label_download_connecting);

        viewBinding.titleBar.tvTitle.setText(getString(R.string.title_download_file));
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        emptyView = ItemTransferfilesEmptyBinding.bind(viewBinding.getRoot());
        connectBinding = ItemTransferfilesConnectBinding.bind(viewBinding.getRoot());
        okBinding = ItemTransferfilesOkBinding.bind(viewBinding.getRoot());

        connectBinding.tvPause.setOnClickListener(v -> {

            ModalDialog dialog = new ModalDialog(TransferFilesActivity.this);
            dialog.setTitle(getString(R.string.download_cancel));
            dialog.setMessage(getString(R.string.dialog_pause_download_message));
            dialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    return true;
                }
            });
            dialog.setPositiveButton(getString(R.string.confirm), new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    ConnectLogic.getImpl().unReceive();

                    state = -1;
                    showStateView();
                    return true;
                }
            });
            dialog.show();
        });
    }

    @Override
    protected void initData() {
        AppTrace.d(TAG, "initData");
        viewShow(ConnectLogic.getImpl().getSessionState()
            , ConnectLogic.getImpl().getFileSize()
            , ConnectLogic.getImpl().getSessionProgress(),
            ConnectLogic.getImpl().getRateSize());
        ConnectLogic.getImpl().setCallback((sessionStatus, progress, allSize, rateSize) -> viewShow(sessionStatus, allSize, progress, rateSize));

        NetworkManager.getImpl().registerNetworkStatusChangeListener(this, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume");
        String curSsid = WifiManager.getImpl().getConnectWifiSsid();
        if(TextUtils.equals(curSsid, ConnectLogic.getImpl().getSsid())) {
            if(ConnectLogic.getImpl().getSessionState() != ConnectLogic.SessionStatus.FAILED) {
                if(errorDialog != null && errorDialog.isShowing()) errorDialog.dismiss();
                errorDialog = null;
            }
            if(ConnectLogic.getImpl().getSessionState() == ConnectLogic.SessionStatus.UNCONNECT) {
                ConnectLogic.getImpl().toReceive();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        ConnectLogic.getImpl().setCallback(null);

        NetworkManager.getImpl().removeNetworkStatusChangeListener(null, this);
    }

    @Override
    public void onStatusChanged(int newStatus, int oldStatus) {
        AppTrace.d(TAG, "onStatusChanged");

        MainHandler.runOnUiThread(() -> {
            String curSsid = WifiManager.getImpl().getConnectWifiSsid();
            if(TextUtils.equals(curSsid, ConnectLogic.getImpl().getSsid())) {
                if(ConnectLogic.getImpl().getSessionState() != ConnectLogic.SessionStatus.FAILED) {
                    if(errorDialog != null && errorDialog.isShowing()) errorDialog.dismiss();
                    errorDialog = null;
                }
                if(ConnectLogic.getImpl().getSessionState() == ConnectLogic.SessionStatus.UNCONNECT) {
                    ConnectLogic.getImpl().toReceive();
                }
            } else {
                showErrorDialog();
            }
        });


    }

    private void showErrorDialog() {
        if(errorDialog != null && errorDialog.isShowing()) {
            return;
        }
        errorDialog = new ModalDialog(TransferFilesActivity.this);

        errorDialog.setTitle(getString(R.string.dialog_device_connect_interrupt));
        errorDialog.setMessage(getString(R.string.dialog_device_connect_interrupt_message));
        errorDialog.setPositiveButton(getString(R.string.reconnect), v -> {
            errorDialog = null;

            boolean hasPermission = PermissionsUtil.hasBlePermission(TransferFilesActivity.this);
            if(!hasPermission) {
                requestBluetoothPermission();
                return;
            }

            String curSsid = WifiManager.getImpl().getConnectWifiSsid();
            if(TextUtils.equals(curSsid, ConnectLogic.getImpl().getSsid())) {
                ConnectLogic.getImpl().setConnectIng(true);
                ConnectLogic.getImpl().toReceive();
            } else {
                ToastUtil.s(String.format(getString(R.string.connect_soft_ap_for_download_file), ConnectLogic.getImpl().getSsid()));
                new Handler().postDelayed(() -> {
                    HotmeWiFiManager.startWifiSettingPage(TransferFilesActivity.this);
                }, 1500);
            }
        });
        errorDialog.setNegativeButton(getString(R.string.cancel), v -> errorDialog = null);
        errorDialog.show();
    }

//    @Subscribe
//    public void onEvent(SessionEvent event) {
//        YLog.d("--->" + event.toString());
//        viewShow(event.sessionStatus, event.allSize, event.progress, event.rateSize);
//    }

    private void viewShow(ConnectLogic.SessionStatus sessionStatus, long allSize, double progress, long rateSize) {
        if(isFinishing() || isDestroyed()) return;
        AppTrace.d(TAG, "viewShow status: " + sessionStatus.name());
        switch(sessionStatus) {
            case WAITE:
                state = ConnectLogic.getImpl().isDownLoading() ? 1 : 0;
                connectBinding.tvFileSize.setText(labelDownloadFileSize + FileSizeUtil.formatSize(allSize));
                connectBinding.tvRateSize.setText(labelDownloadConnecting);
                connectBinding.progressBar.setProgress((int) (progress * 100));
                connectBinding.tvTime.setText("");
                break;

            case CONNECT:
                state = 1;
                //文件大小：1.23G
                //传送速率：203MB/S
                //预估还需时间：12分钟
                connectBinding.tvFileSize.setText(labelDownloadFileSize + FileSizeUtil.formatSize(allSize));
                connectBinding.tvRateSize.setText(labelDownloadConnecting);
                connectBinding.progressBar.setProgress((int) (progress * 100));
                connectBinding.tvTime.setText("");
                break;
            case START:
                state = 1;
                //文件大小：1.23G
                //传送速率：203MB/S
                //预估还需时间：12分钟
                connectBinding.tvFileSize.setText(labelDownloadFileSize + FileSizeUtil.formatSize(allSize));
                connectBinding.tvRateSize.setText(labelDownloadSpeedRate + FileSizeUtil.formatSize(rateSize) + "/S");
                connectBinding.progressBar.setProgress((int) (progress * 100));
                connectBinding.tvTime.setText(rateSize > 0
                    ? labelDownloadTime + TimeUtil.stringToTime_new((long) ((allSize * (1 - progress)) / rateSize))
                    : "");
                break;
            case DOWNLOADING:
                if(state >= 0) {
                    //2023-06-29 09:23:11.953 8487-8487/com.bj.rwkc D/YLog: --->SessionEvent{sessionStatus=DOWNLOADING, progress=0.7565619833074394, allSize=30707332, rateSize=23232000}
                    state = 1;
                    //文件大小：1.23G
                    //传送速率：203MB/S
                    //预估还需时间：12分钟
                    connectBinding.tvFileSize.setText(labelDownloadFileSize + FileSizeUtil.formatSize(allSize));
                    connectBinding.tvRateSize.setText(labelDownloadSpeedRate + FileSizeUtil.formatSize(rateSize) + "/S");
                    connectBinding.progressBar.setProgress((int) (progress * 100));
                    AppTrace.d(TAG, ">>>>" + rateSize + ">>>>>>> " + (allSize * (1 - progress)) + ">>>>" + progress + ">>>" + allSize);
                    String time;
                    if(rateSize != 0 && 1 > progress) {//500一次
                        time = TimeUtil.stringToTime_new((long) ((allSize / rateSize / 2 * (1 - progress))));
                    } else {
                        time = "0秒";
                    }
                    connectBinding.tvTime.setText(labelDownloadTime + time);
                }
                break;
            case UNCONNECT:
                showErrorDialog();
                break;
            case FAILED:
                state = -1;
                if(errorDialog == null) {
                    errorDialog = new ModalDialog(this);
                    errorDialog.setTitle(getString(R.string.dialog_download_exception_title));
                    errorDialog.setMessage("视频下载失败：" + ConnectLogic.getImpl().getTransferError());
                    errorDialog.setPositiveButton("重新下载", v -> {
                        errorDialog = null;
                        state = 0;
                        ConnectLogic.getImpl().toReceive();
                    });
                    errorDialog.setNegativeButton(getString(R.string.cancel), v -> errorDialog = null);
                    errorDialog.show();
                }
                break;
            case Ended:
                state = 2;
//                传送文件大小：1.23G
//                传送速率：203MB/S
                okBinding.tvFileSizeOk.setText(labelDownloadFileSize + FileSizeUtil.formatSize(allSize));
//                if (rateSize == 0) {
//                    rateSize = allSize;
//                }
                long time = (System.currentTimeMillis() - ConnectLogic.getImpl().getStartDownTime()) / 1000;
                if(time > 0) {
                    rateSize = allSize / time;
                } else {
                    rateSize = allSize;
                }
                okBinding.tvRateSizeOk.setText(labelDownloadSpeedRate + FileSizeUtil.formatSize(rateSize) + "/S");
                okBinding.tvOk.setOnClickListener(v -> {
//                        state = 0;
//                        showStateView();
                    ConnectLogic.getImpl().resetState();
                    finish();
                });
                break;

        }
        showStateView();
    }

    private void showStateView() {
        hideAll();
        if(state <= 0) {
            emptyView.tvNoFile.setVisibility(View.VISIBLE);
        } else if(state == 1) {
            connectBinding.groupConnect.setVisibility(View.VISIBLE);
        }
        if(state == 2) {
            okBinding.groupOK.setVisibility(View.VISIBLE);
        }
    }

    private void hideAll() {
        emptyView.tvNoFile.setVisibility(View.GONE);
        connectBinding.groupConnect.setVisibility(View.GONE);
        okBinding.groupOK.setVisibility(View.GONE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //将请求结果传递EasyPermission库处理
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);

    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
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
                        Uri uri = Uri.fromParts("package", TransferFilesActivity.this.getPackageName(), null);
                        intent.setData(uri);
                        TransferFilesActivity.this.startActivity(intent);
                        return true;
                    }
                },
                null
            );
        }
    }
}
