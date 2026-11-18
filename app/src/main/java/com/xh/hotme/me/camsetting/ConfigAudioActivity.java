package com.xh.hotme.me.camsetting;

import android.annotation.SuppressLint;
import android.view.MotionEvent;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.GetSettingsExRequestBean;
import com.xh.hotme.bluetooth.bean.SetVolumeRequestBean;
import com.xh.hotme.bluetooth.bean.UpdateSettingsExRequestBean;
import com.xh.hotme.bluetooth.event.SetVolumeEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExUpdateEvent;
import com.xh.hotme.databinding.ActivityConfigAudioBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@IEventBus
public class ConfigAudioActivity extends BaseActivity<ActivityConfigAudioBinding> {
    private final Runnable _timeOutRunnable = () -> {
        try {
            DialogUtil.dismissDialog();
        } catch(Throwable e) {
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initView() {
        viewBinding.titleBar.tvTitle.setText(R.string.audio_settings);
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        viewBinding.btnRecordAudio.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean isChecked = viewBinding.ivRecordAudio.isSelected();
                setRecordVoice(!isChecked);
                return true;
            }
        });

        viewBinding.btnLiveAudio.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean isChecked = viewBinding.ivLiveAudio.isSelected();
                setRtmpVoice(!isChecked);
                return true;
            }
        });
        viewBinding.sliderVolume.addOnChangeListener((slider, v, b) -> viewBinding.tvVolume.setText(String.valueOf((int) v)));
        viewBinding.sliderVolume.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_UP) {
                int volume = (int) viewBinding.sliderVolume.getValue();
                viewBinding.tvVolume.setText(String.valueOf(volume));

                if(!DeviceApiManager.isConnected()){
                    showDisconnectDialog();
                    return true;
                }
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(new SetVolumeRequestBean(volume));
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
                    DialogUtil.showDialog(getString(R.string.setting_audio_volume));
                } else {
                    showDisconnectToast();
                }
            }
            return false;
        });
    }

    @Override
    protected void initData() {
    }

    @Override
    public void onResume() {
        super.onResume();

        // update settings
        getSettings();
    }

    private void getSettings() {
        GetSettingsExRequestBean bean = new GetSettingsExRequestBean();
        bean.addKey("audio:record_audio");
        bean.addKey("audio:live_audio");
        bean.addKey("audio.0:volume");
        DeviceApiManager.sendCmdSimpleObj(bean);
    }

    private void setRecordVoice(boolean isVoice) {
        UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
        bean.addSetting("audio:record_audio", isVoice ? "1" : "0");
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
        if(isOk) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
            DialogUtil.showDialog(getString(isVoice ? R.string.loading_open_record_audio : R.string.loading_close_record_audio));
        } else {
            showDisconnectToast();
        }
    }

    private void setRtmpVoice(boolean isVoice) {
        UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
        bean.addSetting("audio:live_audio", isVoice ? "1" : "0");
        boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
        if(isOk) {
            _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT);
            DialogUtil.showDialog(getString(isVoice ? R.string.loading_open_live_audio : R.string.loading_close_live_audio));
        } else {
            showDisconnectToast();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSettings(WeewaSettingsExEvent event) {
        AppTrace.d(TAG, "onSettings");
        if(!BaseAppUtil.isDestroy(ConfigAudioActivity.this)) {
            if(_handler != null) {
                _handler.post(() -> {
                    boolean rtmpAudio = "1".equals(event.get("audio:live_audio"));
                    boolean recordAudio = "1".equals(event.get("audio:record_audio"));
                    viewBinding.ivRecordAudio.setSelected(recordAudio);
                    viewBinding.ivLiveAudio.setSelected(rtmpAudio);
                    viewBinding.tvRecordAudio.setText(recordAudio ? R.string.record_audio_on : R.string.record_audio_off);
                    viewBinding.tvLiveAudio.setText(rtmpAudio ? R.string.live_audio_on : R.string.live_audio_off);
                    try {
                        int volume = Integer.parseInt(event.get("audio.0:volume"));
                        viewBinding.sliderVolume.setValue(volume);
                        viewBinding.tvVolume.setText(String.valueOf(volume));
                    } catch(NumberFormatException e) {
                    }
                    DialogUtil.dismissDialog();
                });
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSettingUpdated(WeewaSettingsExUpdateEvent event) {
        getSettings();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetVolume(SetVolumeEvent event) {
        DialogUtil.dismissDialog();
    }
}
