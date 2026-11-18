package com.xh.hotme.test;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;

import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.GetSettingsExRequestBean;
import com.xh.hotme.bluetooth.bean.UpdateSettingsExRequestBean;
import com.xh.hotme.bluetooth.event.WeewaSettingsExEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExUpdateEvent;
import com.xh.hotme.databinding.ActivityTestAudioNoiseSettingBinding;
import com.xh.hotme.databinding.ActivityTestIspSettingBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@IEventBus
public class TestIspSettingActivity extends BaseActivity<ActivityTestIspSettingBinding> {

    private final static String TAG = TestIspSettingActivity.class.getSimpleName();

    private UpdateSettingsExRequestBean _lastUpdateBean;

    private final Runnable _timeOutRunnable = () -> {
    };


    public static void start(Context context) {
        Intent intent = new Intent(context, TestIspSettingActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {
        TitleLayoutHelper.initTitle(viewBinding.titleBar, "ISP设置");

        viewBinding.sliderExposureGain.addOnChangeListener((slider, v, b) -> viewBinding.etExposureGain.setText(String.valueOf((int)v)));
        viewBinding.sliderExposureGain.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_UP) {
                viewBinding.sliderExposureGain.setTag(false);
                setExposureGain();
            } else if(event.getAction() == MotionEvent.ACTION_DOWN) {
                viewBinding.sliderExposureGain.setTag(true);
            } if(event.getAction() == MotionEvent.ACTION_CANCEL) {
                viewBinding.sliderExposureGain.setTag(false);
            }
            return false;
        });
        viewBinding.etExposureGain.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(viewBinding.sliderExposureGain.getTag() instanceof Boolean) {
                    boolean drag = (Boolean)viewBinding.sliderExposureGain.getTag();
                    if(!drag) {
                        try {
                            viewBinding.sliderExposureGain.setValue(Float.parseFloat(s.toString()));
                            setExposureGain();
                        } catch(NumberFormatException e) {
                        }
                    }
                }
            }
        });
        viewBinding.sliderBrightness.addOnChangeListener((slider, v, b) -> viewBinding.etBrightness.setText(String.valueOf((int)v)));
        viewBinding.sliderBrightness.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_UP) {
                int brightness = (int) viewBinding.sliderBrightness.getValue();
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("isp.0.adjustment:brightness", String.valueOf(brightness));
                _lastUpdateBean = bean;
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                    DialogUtil.showDialog(getString(R.string.info_camere_setting_brightness));
                } else {
                    showDisconnectToast();
                }
            }
            return false;
        });
        viewBinding.sliderContrast.addOnChangeListener((slider, v, b) -> viewBinding.etContrast.setText(String.valueOf((int)v)));
        viewBinding.sliderContrast.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_UP) {
                int contrast = (int) viewBinding.sliderContrast.getValue();
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("isp.0.adjustment:contrast", String.valueOf(contrast));
                _lastUpdateBean = bean;
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                    DialogUtil.showDialog(getString(R.string.info_camere_setting_contrast));
                } else {
                    showDisconnectToast();
                }
            }
            return false;
        });
        viewBinding.sliderSaturation.addOnChangeListener((slider, v, b) -> viewBinding.etSaturation.setText(String.valueOf((int)v)));
        viewBinding.sliderSaturation.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_UP) {
                int saturation = (int) viewBinding.sliderSaturation.getValue();
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("isp.0.adjustment:saturation", String.valueOf(saturation));
                _lastUpdateBean = bean;
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                    DialogUtil.showDialog(getString(R.string.info_camere_setting_saturation));
                } else {
                    showDisconnectToast();
                }
            }
            return false;
        });
        viewBinding.sliderHue.addOnChangeListener((slider, v, b) -> viewBinding.etHue.setText(String.valueOf((int)v)));
        viewBinding.sliderHue.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_UP) {
                int hue = (int) viewBinding.sliderHue.getValue();
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("isp.0.adjustment:hue", String.valueOf(hue));
                _lastUpdateBean = bean;
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                    DialogUtil.showDialog(getString(R.string.info_camere_setting_aberration));
                } else {
                    showDisconnectToast();
                }
            }
            return false;
        });
        viewBinding.sliderSharpness.addOnChangeListener((slider, v, b) -> viewBinding.etSharpness.setText(String.valueOf((int)v)));
        viewBinding.sliderSharpness.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_UP) {
                int hue = (int) viewBinding.sliderSharpness.getValue();
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("isp.0.adjustment:sharpness", String.valueOf(hue));
                _lastUpdateBean = bean;
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                    DialogUtil.showDialog(getString(R.string.info_camere_setting_acutance));
                } else {
                    showDisconnectToast();
                }
            }
            return false;
        });
        viewBinding.btnSetExposureTime.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                try {
                    int num = Integer.parseInt(viewBinding.etExposureTimeNum.getText().toString());
                    int den = Integer.parseInt(viewBinding.etExposureTimeDen.getText().toString());
                    if(num <= 0 || den <= 0) {
                        ToastUtil.s(getString(R.string.info_setting_value_large_zero));
                        return true;
                    }
                    UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                    bean.addSetting("isp.0.exposure:exposure_time", String.format("%d/%d", num, den));
                    _lastUpdateBean = bean;
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                    if(isOk) {
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                        DialogUtil.showDialog(getString(R.string.info_camere_setting_exposure_time));
                    } else {
                        showDisconnectToast();
                    }
                } catch(NumberFormatException e) {
                }
                return true;
            }
        });


        viewBinding.btnIspFrequency.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                try {
                    int fqs = Integer.parseInt(viewBinding.etFrequency.getText().toString());
                    if(fqs > 25 || fqs < 0){
                        ToastUtil.s("目前仅支持最大25帧");
                        return true;
                    }
                    //
                    UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                    bean.addSetting("isp.0.adjustment:fps", String.valueOf(fqs));
                    bean.addSetting("video.0:src_frame_rate_num", String.valueOf(fqs));
                    bean.addSetting("video.0:dst_frame_rate_num", String.valueOf(fqs));
                    bean.addSetting("video.1:src_frame_rate_num", String.valueOf(fqs));
                    bean.addSetting("video.1:dst_frame_rate_num", String.valueOf(fqs));
                    bean.addSetting("video.2:src_frame_rate_num", String.valueOf(fqs));
                    bean.addSetting("video.2:dst_frame_rate_num", String.valueOf(fqs));

                    _lastUpdateBean = bean;
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                    if(isOk) {
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                        DialogUtil.showDialog(getString(R.string.info_camere_setting_frq));
                    } else {
                        showDisconnectToast();
                    }
                } catch(NumberFormatException e) {
                }
                return true;
            }
        });
    }

    @Override
    protected void initData() {
        GetSettingsExRequestBean bean = new GetSettingsExRequestBean();
        bean.addKey("isp.0.exposure:exposure_gain");
        bean.addKey("isp.0.exposure:exposure_time");
        bean.addKey("isp.0.adjustment:brightness");
        bean.addKey("isp.0.adjustment:contrast");
        bean.addKey("isp.0.adjustment:saturation");
        bean.addKey("isp.0.adjustment:hue");
        bean.addKey("isp.0.adjustment:sharpness");
        bean.addKey("isp.0.adjustment:fps");
        DeviceApiManager.sendCmdSimpleObj(bean);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setExposureGain() {
        int brightness = (int) viewBinding.sliderExposureGain.getValue();
        UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
        bean.addSetting("isp.0.exposure:exposure_gain", String.valueOf(brightness));
        _lastUpdateBean = bean;
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
        if(isOk) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
            DialogUtil.showDialog("正在设置曝光增益");
        } else {
            showDisconnectToast();
        }
    }

    private void setFrequency() {
        int frq = Integer.parseInt(viewBinding.etFrequency.getText().toString());
        UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
        bean.addSetting("isp.0.adjustment:fps", String.valueOf(frq));
        _lastUpdateBean = bean;
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
        if(isOk) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
            DialogUtil.showDialog("正在设置帧率");
        } else {
            showDisconnectToast();
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWeewaSettingsExUpdateEvent(WeewaSettingsExUpdateEvent event) {
        AppTrace.d(TAG, "onWeewaSettingsExUpdateEvent");
        DialogUtil.dismissDialog();
        if(event != null && event._status == 1) {
            ToastUtil.s(getString(R.string.info_update_success));
        } else {
            ToastUtil.s(getString(R.string.info_update_fail));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWeewaSettingExEvent(WeewaSettingsExEvent event) {
        AppTrace.d(TAG, "onWeewaSettingExEvent");
        if(event == null || event.settings == null) {
            return;
        }
        if(event.has("isp.0.adjustment:fps")) {
            try {
                int frequency = Integer.parseInt(event.get("isp.0.adjustment:fps"));
                viewBinding.etFrequency.setText(String.valueOf(frequency));
            } catch(NumberFormatException e) {
            }
        }

        if(event.has("isp.0.adjustment:brightness")) {
            try {
                float v = Math.max(0, Math.min(100, Float.parseFloat(event.get("isp.0.adjustment:brightness"))));
                viewBinding.sliderBrightness.setValue(v);
                viewBinding.etBrightness.setText(String.valueOf((int)v));
            } catch(NumberFormatException e) {
            }
        }
        if(event.has("isp.0.adjustment:contrast")) {
            try {
                float v = Math.max(0, Math.min(100, Float.parseFloat(event.get("isp.0.adjustment:contrast"))));
                viewBinding.sliderContrast.setValue(v);
                viewBinding.etContrast.setText(String.valueOf((int)v));
            } catch(NumberFormatException e) {
            }
        }
        if(event.has("isp.0.adjustment:saturation")) {
            try {
                float v = Math.max(0, Math.min(100, Float.parseFloat(event.get("isp.0.adjustment:saturation"))));
                viewBinding.sliderSaturation.setValue(v);
                viewBinding.etSaturation.setText(String.valueOf((int)v));
            } catch(NumberFormatException e) {
            }
        }
        if(event.has("isp.0.adjustment:hue")) {
            try {
                float v = Math.max(0, Math.min(100, Float.parseFloat(event.get("isp.0.adjustment:hue"))));
                viewBinding.sliderHue.setValue(v);
                viewBinding.etHue.setText(String.valueOf((int)v));
            } catch(NumberFormatException e) {
            }
        }
        if(event.has("isp.0.adjustment:sharpness")) {
            try {
                float v = Math.max(0, Math.min(100, Float.parseFloat(event.get("isp.0.adjustment:sharpness"))));
                viewBinding.sliderSharpness.setValue(v);
                viewBinding.etSharpness.setText(String.valueOf((int)v));
            } catch(NumberFormatException e) {
            }
        }
        if(event.has("isp.0.exposure:exposure_gain")) {
            try {
                float v = Math.max(0, Math.min(255, Float.parseFloat(event.get("isp.0.exposure:exposure_gain"))));
                viewBinding.sliderExposureGain.setValue(v);
                viewBinding.etExposureGain.setText(String.valueOf((int)v));
            } catch(NumberFormatException e) {
            }
        }
        if(event.has("isp.0.exposure:exposure_time")) {
            try {
                String tstr = event.get("isp.0.exposure:exposure_time");
                String[] parts = tstr.split("/");
                if(parts.length == 2) {
                    viewBinding.etExposureTimeNum.setText(parts[0]);
                    viewBinding.etExposureTimeDen.setText(parts[1]);
                }
            } catch(NumberFormatException e) {
            }
        }
    }

}
