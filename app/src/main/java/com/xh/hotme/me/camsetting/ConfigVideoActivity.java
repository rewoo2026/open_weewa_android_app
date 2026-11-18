package com.xh.hotme.me.camsetting;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.RequireStoragePermission;
import com.xh.hotme.bluetooth.bean.SceneSetupRequestBean;
import com.xh.hotme.bluetooth.bean.GetSettingsRequestBean;
import com.xh.hotme.bluetooth.event.SceneSetupEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivityConfigVideoBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.me.setmeal.utils.SetMealCheckManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;
import com.xh.hotme.widget.SwitchButtonO;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@IEventBus
@RequireStoragePermission(mandatory = true)
public class ConfigVideoActivity extends BaseActivity<ActivityConfigVideoBinding> {
    private static final Logger log = LoggerFactory.getLogger(ConfigVideoActivity.class);
    private ModalDialog _conformTurnOffVideoAnalysisDialog;

    private boolean isCheckingStorage = false;
    private boolean isInitializing = true;  // 添加初始化标志位

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initView() {
        viewBinding.titleBar.tvTitle.setText(R.string.video_settings);
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        viewBinding.sbEnableAvs.setChecked(CameraManager.getDeviceBooleanConfig(Constants.CAMERA_SETTING_ENABLE_AVS));
        viewBinding.sbAutoUploadAvs.setChecked(CameraManager.getDeviceBooleanConfig(Constants.CAMERA_SETTING_AUTO_UPLOAD_AVS));
        viewBinding.sbAutoUpload.setChecked(CameraManager.getDeviceBooleanConfig(Constants.CAMERA_SETTING_AUTO_UPLOAD));
        viewBinding.sbVideoAnalysis.setChecked(CameraManager.getDeviceBooleanConfig(Constants.CAMERA_SETTING_VIDEO_ANALYSIS));
        viewBinding.etAvsMbr.setText(CameraManager.getDeviceStringConfig(Constants.CAMERA_SETTING_AVS_MBR));
        viewBinding.groupAvsMbr.setVisibility(viewBinding.sbVideoAnalysis.isChecked() ? View.VISIBLE : View.GONE);

        viewBinding.btnAutoUpload.setOnClickListener(v -> {
            if (!canSwitch(viewBinding.sbAutoUpload)) {
                return;
            }

            viewBinding.sbAutoUpload.setChecked(!viewBinding.sbAutoUpload.isChecked());
        });
        viewBinding.sbAutoUpload.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                return !canSwitch(viewBinding.sbAutoUpload);
            }
            return false;
        });
        viewBinding.sbAutoUpload.setOnCheckedChangeListener((buttonView, isChecked) -> {

//            SceneSetupRequestBean bean = new SceneSetupRequestBean();
//            bean.setAutoUpload(isChecked ? 1 : 0);
//            bean.setAutoUploadHl(isChecked ? 1 : 0);
//            bean.setAutoUploadFec(isChecked ? 1 : 0);
//            bean.setAutoUploadLog(isChecked ? 1 : 0);
//            bean.setAutoUploadCropBuffer(isChecked ? 1 : 0);
//            DeviceApiManager.sendCmdSimpleObj(bean);

            if (isInitializing) {
                return;  // 初始化时不触发检测
            }
            // 如果是要开启，需要检查
            if (isCheckingStorage) {
                return;
            }
            if (isChecked) {

                // 先恢复开关状态，等检查通过后再开启
                isCheckingStorage = true;


                SetMealCheckManager.checkSetMeal(this, new SetMealCheckManager.CheckSetMealCallback() {
                    @Override
                    public void onSuccess() {
                        // 检查通过后再设置状态并发送指令

                        isCheckingStorage = false;
                        buttonView.setChecked(true);
                        sendAutoUploadCommand(true);
                    }

                    @Override
                    public void onfail() {
                        isCheckingStorage = false;
                        buttonView.setChecked(false);
                    }
                }, 2);
            } else {
                // 关闭时直接发送指令
                sendAutoUploadCommand(isChecked);
            }
        });

        viewBinding.btnAutoUploadAvs.setOnClickListener(v -> {
            if (canSwitch(viewBinding.sbAutoUploadAvs)) {
                viewBinding.sbAutoUploadAvs.setChecked(!viewBinding.sbAutoUploadAvs.isChecked());
            }
        });
        viewBinding.sbAutoUploadAvs.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                return !canSwitch(viewBinding.sbAutoUploadAvs);
            }
            return false;
        });
        viewBinding.sbAutoUploadAvs.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (!DeviceApiManager.isConnected()) {
                showDisconnectDialog();
                return;
            }

            SceneSetupRequestBean bean = new SceneSetupRequestBean();
            bean.setAutoUploadAvs(isChecked ? 1 : 0);
            DeviceApiManager.sendCmdSimpleObj(bean);
        });
        viewBinding.btnEnableAvs.setOnClickListener(v -> {
            if (canSwitch(viewBinding.sbEnableAvs)) {
                viewBinding.sbEnableAvs.setChecked(!viewBinding.sbEnableAvs.isChecked());
            }
        });
        viewBinding.sbEnableAvs.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                return !canSwitch(viewBinding.sbEnableAvs);
            }
            return false;
        });
        viewBinding.sbEnableAvs.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CameraManager.setDeviceConfig(Constants.CAMERA_SETTING_ENABLE_AVS, isChecked ? "1" : "0");
        });
        viewBinding.btnVideoAnalysis.setOnClickListener(v -> {
            viewBinding.sbVideoAnalysis.setChecked(!viewBinding.sbVideoAnalysis.isChecked());
        });
        viewBinding.sbVideoAnalysis.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CameraManager.setDeviceConfig(Constants.CAMERA_SETTING_VIDEO_ANALYSIS, isChecked ? "1" : "0");
            viewBinding.groupAvsMbr.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                viewBinding.sbAutoUpload.setChecked(true);
                viewBinding.sbAutoUploadAvs.setChecked(true);
                viewBinding.sbEnableAvs.setChecked(true);
            }
            
            // Update auto_upload_log and auto_upload_crop_buffer when video analysis mode changes
            SceneSetupRequestBean bean = new SceneSetupRequestBean();
            bean.setAutoUploadLog(isChecked ? 1 : 0);
            bean.setAutoUploadCropBuffer(isChecked ? 1 : 0);
            DeviceApiManager.sendCmdSimpleObj(bean);
        });

        int index = SharedPrefs.getImpl().getInt(SharedPrefs.PREF_AVS_RESOLUTION);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.avs_resolution, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        viewBinding.spAvsResolution.setAdapter(adapter);

        // 设置Spinner的初始选择位置
        viewBinding.spAvsResolution.setSelection(index);
        viewBinding.spAvsResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // do something
                SharedPrefs.getImpl().saveInt(SharedPrefs.PREF_AVS_RESOLUTION, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });


    }


    // 添加发送指令的方法
    private void sendAutoUploadCommand(boolean isChecked) {
         SceneSetupRequestBean bean = new SceneSetupRequestBean();
         bean.setAutoUpload(isChecked ? 1 : 0);
         bean.setAutoUploadHl(isChecked ? 1 : 0);
         bean.setAutoUploadFec(isChecked ? 1 : 0);
         DeviceApiManager.sendCmdSimpleObj(bean);
    }


    @Override
    protected void initData() {
    }

    private boolean canSwitch(SwitchButtonO sb) {
        if(sb.isChecked()) {
            if(viewBinding.sbVideoAnalysis.isChecked()) {
                showConfirmTurnOffVideoAnalysisDialog(sb);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        // save avs mbr
        try {
            int mbr = Integer.parseInt(viewBinding.etAvsMbr.getText().toString());
            if(mbr > 0) {
                CameraManager.setDeviceConfig(Constants.CAMERA_SETTING_AVS_MBR, String.valueOf(mbr));
            }
        } catch(NumberFormatException e) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(!DeviceApiManager.isConnected()){
            showDisconnectDialog();
            return;
        }
        // update settings
        DeviceApiManager.sendCmdSimpleObj(new GetSettingsRequestBean());
    }

    private void showConfirmTurnOffVideoAnalysisDialog(final SwitchButtonO sb) {
        if(_conformTurnOffVideoAnalysisDialog != null && _conformTurnOffVideoAnalysisDialog.isShowing()) {
            return;
        }
        _conformTurnOffVideoAnalysisDialog = new ModalDialog(this);
        _conformTurnOffVideoAnalysisDialog.setPositiveButton(getString(R.string.ok), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                sb.setChecked(false);
                viewBinding.sbVideoAnalysis.setChecked(false);
                return true;
            }
        });

        _conformTurnOffVideoAnalysisDialog.setMessage(getString(R.string.dialog_turn_off_video_analysis_message));
        _conformTurnOffVideoAnalysisDialog.setCancelable(true);
        _conformTurnOffVideoAnalysisDialog.setNegativeButton(getString(R.string.cancel), new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _conformTurnOffVideoAnalysisDialog.dismiss();
                return true;
            }
        });

        _conformTurnOffVideoAnalysisDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(_conformTurnOffVideoAnalysisDialog != null && _conformTurnOffVideoAnalysisDialog.isShowing()) {
            _conformTurnOffVideoAnalysisDialog.dismiss();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSettings(WeewaSettingsEvent event) {
        AppTrace.d(TAG, "onUpdateSettings");
        if(!BaseAppUtil.isDestroy(this)) {
            runOnUiThread(() -> {
                if(event != null && event.settings != null) {
                    isInitializing = true;
                    viewBinding.sbAutoUpload.setChecked(event.settings.getAuto_upload() != 0);
                    viewBinding.sbAutoUploadAvs.setChecked(event.settings.getAuto_upload_avs() != 0);
                    isInitializing = false;
                }
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSettings(SceneSetupEvent event) {
        AppTrace.d(TAG, "onUpdateSettings");
        if(!BaseAppUtil.isDestroy(this)) {
            runOnUiThread(() -> {
                if(event != null && event._status ==1) {
                   ToastUtil.s(getString(R.string.setup_successful));
                }else{
                    ToastUtil.s(getString(R.string.setup_fail));
                }
            });
        }
    }
}
