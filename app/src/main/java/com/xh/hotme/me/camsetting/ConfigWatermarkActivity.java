package com.xh.hotme.me.camsetting;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.xh.hotme.BuildConfig;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.DeleteWatermarkRequestBean;
import com.xh.hotme.bluetooth.bean.SetWatermarkRequestBean;
import com.xh.hotme.bluetooth.event.DeleteWatermarkEvent;
import com.xh.hotme.bluetooth.event.SetWatermarkEvent;
import com.xh.hotme.camera.softap.CameraSoftApActivity;
import com.xh.hotme.camera.softap.NetworkManager;
import com.xh.hotme.camera.wifi.HotmeWiFiManager;
import com.xh.hotme.databinding.ActivityConfigWatermarkBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.me.shared.SelectPhotoDialog;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BitmapUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.FileUtil;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;
import com.xh.hotme.widget.imagepicker.ImagePickerCallback;
import com.xh.hotme.widget.imagepicker.LetoImagePicker;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@IEventBus
public class ConfigWatermarkActivity extends BaseActivity<ActivityConfigWatermarkBinding> implements View.OnClickListener, ImagePickerCallback {
    private static final int REQ_START_SOFTAP_FOR_WATERMARK = 1003;
    private static final int REQ_CONNECT_SOFTAP_FOR_WATERMARK = 1004;
    private static final int REQ_START_SOFTAP_FOR_RESET_WATERMARK = 1001;
    private static final int REQ_CONNECT_SOFTAP_FOR_RESET_WATERMARK = 1002;

    ModalDialog _joinSoftApDialog;
    ModalDialog _startSoftApDialog;
    ModalDialog _resetWatermarkDialog;

    private static final int REQ_CAMERA_ACCESS = 1001;
    private static final int REQ_ALBUM_ACCESS = 1002;

    // image picker
    LetoImagePicker _imagePicker;
    private Bitmap _bmp = null;
    private String _pendingWatermarkFile = null;
    private static final String WATERMARK_FILENAME = "watermark.jpg";
    private static final int WATERMARK_SUGGESTED_WIDTH = 110;
    private static final int WATERMARK_SUGGESTED_HEIGHT = 39;
    private static final File CURRENT_WATERMARK_FILE = new File(Environment.getExternalStorageDirectory(), String.format("%s/watermark.jpg", BuildConfig.APPLICATION_ID));

    private boolean hideObsScore;
    private String str_request_timeout;

    private final Runnable _timeOutRunnable = () -> {
        DialogUtil.dismissDialog();
        ToastUtil.s(str_request_timeout);
    };

    @Override
    protected void initView() {
        str_request_timeout = getString(R.string.request_timeout);
        viewBinding.titleBar.tvTitle.setText(R.string.watermark_config);
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });
        viewBinding.btnOk.setOnClickListener(this);
        viewBinding.btnReset.setOnClickListener(this);
        viewBinding.btnSelectWatermark.setOnClickListener(this);
        viewBinding.btnDelete.setOnClickListener(this);

        // show current watermark
        _bmp = BitmapFactory.decodeFile(CURRENT_WATERMARK_FILE.getAbsolutePath());
        if(_bmp != null) {
            viewBinding.watermark.setImageBitmap(_bmp);
            viewBinding.watermark.setScaleX(2);
            viewBinding.watermark.setScaleY(2);
        }

        hideObsScore = SharedPrefs.getImpl().getBoolean(SharedPrefs.PREF_HIDE_OSD, false);

        viewBinding.sbHideObsScore.setChecked(hideObsScore);
        viewBinding.sbHideObsScore.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                AppTrace.d(TAG, "hideObsScore = " + isChecked);
                if(isChecked){
                    SharedPrefs.getImpl().saveBoolean(SharedPrefs.PREF_HIDE_OSD, true);
                } else {
                    SharedPrefs.getImpl().saveBoolean(SharedPrefs.PREF_HIDE_OSD, false);
                }

                hideObsScore = isChecked;

                ToastUtil.s(getString(R.string.info_save_to_local_success));
            }
        });
    }

    @Override
    protected void initData() {
        _imagePicker = LetoImagePicker.getInstance(this);
        viewBinding.etMatchName.setText(SharedPrefs.getImpl().getDefaultMatchName());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(_bmp != null) {
            _bmp.recycle();
            _bmp = null;
        }

        if(_joinSoftApDialog != null && _joinSoftApDialog.isShowing()) {
            _joinSoftApDialog.dismiss();
        }
        _joinSoftApDialog = null;
        if(_startSoftApDialog != null && _startSoftApDialog.isShowing()) {
            _startSoftApDialog.dismiss();
        }
        _startSoftApDialog = null;
        if(_resetWatermarkDialog != null && _resetWatermarkDialog.isShowing()) {
            _resetWatermarkDialog.dismiss();
        }
        _resetWatermarkDialog = null;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.btn_ok) {
            SharedPrefs.getImpl().setDefaultMatchName(viewBinding.etMatchName.getText().toString());
            if(_bmp != null) {
                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return;
                }

                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    if (NetworkManager.getImpl().isConnectedSoftAp()) {
                        // to base64
                        String b64Bmp = BitmapUtil.getBmpBase64(_bmp);

                        // send watermark by http api
                       boolean isOk =  DeviceApiManager.sendCmdSimpleObjBySoftAp(new SetWatermarkRequestBean(b64Bmp));
                       if(isOk){
                           _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                           DialogUtil.showDialog("");
                       }
                    } else if (NetworkManager.getImpl().isDeviceSoftApLaunched()) {
                        showSoftApDialog(getString(R.string.connect_softap_before_set_watermark), REQ_CONNECT_SOFTAP_FOR_WATERMARK);
                    } else {
                        showSoftApStartDialog(REQ_START_SOFTAP_FOR_WATERMARK);
                    }
                } else {
                    if(!NetUtil.isConnectedAvailableNetwork()){
                        ToastUtil.l(R.string.error_network_error);
                        return;
                    }
                    // to base64
                    String b64Bmp = BitmapUtil.getBmpBase64(_bmp);

                    // send watermark by http api
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(new SetWatermarkRequestBean(b64Bmp));
                    if(isOk){
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                        DialogUtil.showDialog("");
                    }
                }
            } else {
                finish();
            }
        } else if(id == R.id.btn_select_watermark) {
            showWatermarkPicker();
        } else if(id == R.id.btn_delete) {
            viewBinding.watermark.setImageResource(R.mipmap.watermark_placeholder);
            viewBinding.watermark.setScaleX(1);
            viewBinding.watermark.setScaleY(1);
            if(_bmp != null) {
                _bmp.recycle();
                _bmp = null;
            }
        } else if(id == R.id.btn_reset) {
            if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                if (NetworkManager.getImpl().isConnectedSoftAp()) {
                    showResetWatermarkDialog();
                } else if (NetworkManager.getImpl().isDeviceSoftApLaunched()) {
                    showSoftApDialog(getString(R.string.connect_softap_before_set_watermark), REQ_CONNECT_SOFTAP_FOR_RESET_WATERMARK);
                } else {
                    showSoftApStartDialog(REQ_START_SOFTAP_FOR_RESET_WATERMARK);
                }
            }else{
                if(!NetUtil.isConnectedAvailableNetwork()){
                    ToastUtil.l(R.string.error_network_error);
                   return;
                }
                showResetWatermarkDialog();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case REQ_CAMERA_ACCESS:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    _imagePicker.pickFromCamera(WATERMARK_FILENAME, WATERMARK_SUGGESTED_WIDTH, WATERMARK_SUGGESTED_HEIGHT, true, false, this);
                }
                break;
            case REQ_ALBUM_ACCESS:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    _imagePicker.pickFromAlbum(WATERMARK_FILENAME, WATERMARK_SUGGESTED_WIDTH, WATERMARK_SUGGESTED_HEIGHT, false, this);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQ_START_SOFTAP_FOR_WATERMARK:
            case REQ_CONNECT_SOFTAP_FOR_WATERMARK:
                onClick(viewBinding.btnOk);
                break;
            case REQ_CONNECT_SOFTAP_FOR_RESET_WATERMARK:
            case REQ_START_SOFTAP_FOR_RESET_WATERMARK:
                onClick(viewBinding.btnReset);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void showWatermarkPicker() {
        SelectPhotoDialog dialog = new SelectPhotoDialog(this);
        dialog.disableCamera();
        dialog.setCameraBtnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                List<String> perms = new ArrayList<>();
                if(Build.VERSION.SDK_INT >= 23) {
                    if(ContextCompat.checkSelfPermission(ConfigWatermarkActivity.this,
                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                        perms.add(Manifest.permission.CAMERA);
                    }
                    if(ContextCompat.checkSelfPermission(ConfigWatermarkActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                    if(ContextCompat.checkSelfPermission(ConfigWatermarkActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }
                if(perms.isEmpty()) {
                    _imagePicker.pickFromCamera(WATERMARK_FILENAME, WATERMARK_SUGGESTED_WIDTH, WATERMARK_SUGGESTED_HEIGHT, true, false, ConfigWatermarkActivity.this);
                    dialog.dismiss();
                } else if(Build.VERSION.SDK_INT >= 23) {
                    requestPermissions(perms.toArray(new String[0]), REQ_CAMERA_ACCESS);
                }

                return true;
            }
        });
        dialog.setAlbumBtnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                List<String> perms = new ArrayList<>();
                if(Build.VERSION.SDK_INT >= 23) {
                    if(ContextCompat.checkSelfPermission(ConfigWatermarkActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                    if(ContextCompat.checkSelfPermission(ConfigWatermarkActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }
                if(perms.isEmpty()) {
                    _imagePicker.pickFromAlbum(WATERMARK_FILENAME, WATERMARK_SUGGESTED_WIDTH, WATERMARK_SUGGESTED_HEIGHT, false, ConfigWatermarkActivity.this);
                    dialog.dismiss();
                } else if(Build.VERSION.SDK_INT >= 23) {
                    requestPermissions(perms.toArray(new String[0]), REQ_ALBUM_ACCESS);
                }

                return true;
            }
        });
        dialog.show();
    }

    private void showSoftApStartDialog(int requestCode) {
        if(_startSoftApDialog != null && _startSoftApDialog.isShowing()) {
            return;
        }

        _startSoftApDialog = new ModalDialog(this);
        _startSoftApDialog.setMessage(getString(R.string.start_softap_before_set_watermark));
        _startSoftApDialog.setPositiveButton(getString(R.string.wifi_open), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                CameraSoftApActivity.startActivityForResult(
                    ConfigWatermarkActivity.this,
                    NetworkManager.getImpl().getSoftApSsid(),
                    NetworkManager.getImpl().getSoftApPassword(),
                    NetworkManager.getImpl().getSoftApStatus(),
                    requestCode
                );
                return true;
            }
        });

        _startSoftApDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _startSoftApDialog.dismiss();
                return true;
            }
        });

        _startSoftApDialog.show();
    }

    private void showSoftApDialog(String message, int requestCode) {
        if(_joinSoftApDialog != null && _joinSoftApDialog.isShowing()) {
            return;
        }

        _joinSoftApDialog = new ModalDialog(this);
        _joinSoftApDialog.setMessage(message);
        _joinSoftApDialog.setPositiveButton(getString(R.string.wifi_join), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                HotmeWiFiManager.startWifiSettingPage(ConfigWatermarkActivity.this, requestCode);
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

        _joinSoftApDialog.show();
    }

    private void showResetWatermarkDialog() {
        if(_resetWatermarkDialog != null && _resetWatermarkDialog.isShowing()) {
            return;
        }

        _resetWatermarkDialog = new ModalDialog(this);
        _resetWatermarkDialog.setTitle(getString(R.string.reset_watermark));
        _resetWatermarkDialog.setMessage(getString(R.string.info_confirm_reset_watermark));
        _resetWatermarkDialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                DeviceApiManager.sendCmdSimpleObj(new DeleteWatermarkRequestBean());
                return true;
            }
        });

        _resetWatermarkDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _resetWatermarkDialog.dismiss();
                return true;
            }
        });

        _resetWatermarkDialog.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetWatermark(SetWatermarkEvent event) {
        _handler.removeCallbacks(_timeOutRunnable);
        DialogUtil.dismissDialog();
        if(event.result.equals(BleConstants.BLE_RESULT_SUCCESS)) {
            ToastUtil.s(getString(R.string.watermark_set_successful));
            FileUtil.copyFile(_pendingWatermarkFile, CURRENT_WATERMARK_FILE.getAbsolutePath());
            finish();
        } else {
            ToastUtil.s(event.result);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeleteWatermark(DeleteWatermarkEvent event) {
        if(event.result.equals(BleConstants.BLE_RESULT_SUCCESS)) {
            ToastUtil.s(getString(R.string.watermark_delete_successful));

            // clear match name
            SharedPrefs.getImpl().setDefaultMatchName("");
            viewBinding.etMatchName.setText("");

            // clear cached watermark file and ui
            viewBinding.watermark.setImageResource(R.mipmap.watermark_placeholder);
            viewBinding.watermark.setScaleX(1);
            viewBinding.watermark.setScaleY(1);
            if(_bmp != null) {
                _bmp.recycle();
                _bmp = null;
            }
            FileUtil.deleteFile(CURRENT_WATERMARK_FILE.getAbsolutePath());
        } else {
            ToastUtil.s(event.result);
        }
    }

    @Override
    public void onImagePicked(String file) {
        _pendingWatermarkFile = file;
        _bmp = BitmapFactory.decodeFile(file);
        if(_bmp != null) {
            viewBinding.watermark.setImageBitmap(_bmp);
            viewBinding.watermark.setScaleX(2);
            viewBinding.watermark.setScaleY(2);
        } else {
            ToastUtil.s(getString(R.string.info_watermark_decode_error));
        }
    }

    @Override
    public void onImagePickingCancelled() {

    }
}
