package com.xh.hotme.test;

import android.content.Context;
import android.content.Intent;

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
import com.xh.hotme.databinding.ActivityTestVideoSettingBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@IEventBus
public class TestAudioNoiseSettingActivity extends BaseActivity<ActivityTestAudioNoiseSettingBinding> {

    private final static String TAG = TestAudioNoiseSettingActivity.class.getSimpleName();

    private UpdateSettingsExRequestBean _lastUpdateBean;

    private final Runnable _timeOutRunnable = () -> {
    };


    public static void start(Context context) {
        Intent intent = new Intent(context, TestAudioNoiseSettingActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {

        TitleLayoutHelper.initTitle(viewBinding.titleBar, "音频降噪设置");

        viewBinding.btnAnrSetting.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                try {
                    // groundDistance单位是厘米所以要乘100
                    float v1 = Float.parseFloat(viewBinding.etNoiseFactor.getText().toString());
                    int v2 = (int) Float.parseFloat(viewBinding.etSwU.getText().toString());
                    float v3 = Float.parseFloat(viewBinding.etPsiMin.getText().toString());
                    float v4 = Float.parseFloat(viewBinding.etPsiMax.getText().toString());
                    float v5 = Float.parseFloat(viewBinding.etFGmin.getText().toString());
                    float v6 = Float.parseFloat(viewBinding.etAttackTime.getText().toString());
                    float v7 = Float.parseFloat(viewBinding.etReleaseTime.getText().toString());
                    float v8 = Float.parseFloat(viewBinding.etMaxGain.getText().toString());
                    float v9 = Float.parseFloat(viewBinding.etMaxPeak.getText().toString());
                    float v10 = Float.parseFloat(viewBinding.etFRk0.getText().toString());
                    float v11 = Float.parseFloat(viewBinding.etFRth0.getText().toString());
                    float v12 = Float.parseFloat(viewBinding.etFRth1.getText().toString());
                    float v13 = Float.parseFloat(viewBinding.etFRth2.getText().toString());
                    int v14 = (int) Float.parseFloat(viewBinding.etFilterType.getText().toString());
                    int v15 = (int) Float.parseFloat(viewBinding.etFilterMode.getText().toString());
                    int v16 = (int) Float.parseFloat(viewBinding.etFc.getText().toString());
                    int v17 = (int) Float.parseFloat(viewBinding.etBw.getText().toString());
                    UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                    bean.addSetting("audio.anr:noiseFactor", v1);
                    bean.addSetting("audio.anr:swU", v2);
                    bean.addSetting("audio.anr:PsiMin", v3);
                    bean.addSetting("audio.anr:PsiMax", v4);
                    bean.addSetting("audio.anr:fGmin", v5);
                    bean.addSetting("audio.agc:attack_time", v6);
                    bean.addSetting("audio.agc:release_time", v7);
                    bean.addSetting("audio.agc:max_gain", v8);
                    bean.addSetting("audio.agc:max_peak", v9);
                    bean.addSetting("audio.agc:fRk0", v10);
                    bean.addSetting("audio.agc:fRth0", v11);
                    bean.addSetting("audio.agc:fRth1", v12);
                    bean.addSetting("audio.agc:fRth2", v13);
                    bean.addSetting("audio.hpf:Filter_type", v14);
                    bean.addSetting("audio.hpf:Filter_mode", v15);
                    bean.addSetting("audio.hpf:fc", v16);
                    bean.addSetting("audio.hpf:bw", v17);
                    _lastUpdateBean = bean;
                    boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                    if(isOk) {
                        _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                        DialogUtil.showDialog("正在设置音频降噪参数");
                    } else {
                        showDisconnectToast();
                    }
                } catch(NumberFormatException e) {
                    ToastUtil.s(getString(R.string.info_value_verify_illegal));
                }
                return true;
            }
        });



//        viewBinding.btnToggleSoundCard.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
//            @Override
//            public boolean onClicked() {
//                boolean to_off = viewBinding.btnToggleSoundCard.getText().toString().startsWith("关闭");
//                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
//                bean.setRestart(1);
//                bean.addSetting("audio.0:card_name", to_off ? "hw:0,0" : "softvol_cap");
//                _lastUpdateBean = bean;
//                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
//                if(isOk) {
//                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
//                    DialogUtil.showDialog(String.format("正在%s音量功能,如果rkipc已经启动将会自动重启", to_off ? "关闭" : "打开"));
//                } else {
//                    showDisconnectToast();
//                }
//                return true;
//            }
//        });
        viewBinding.btnToggleAnr.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean to_off = viewBinding.btnToggleAnr.getText().toString().startsWith("关闭");
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("audio:noise_reduction", to_off ? "0" : "1");
                _lastUpdateBean = bean;
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                    DialogUtil.showDialog(String.format("正在%s音频降噪", to_off ? "关闭" : "打开"));
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });
        viewBinding.btnToggleAgc.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean to_off = viewBinding.btnToggleAgc.getText().toString().startsWith("关闭");
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("audio:noise_reduction_agc", to_off ? "0" : "1");
                _lastUpdateBean = bean;
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                    DialogUtil.showDialog(String.format("正在%s音频增益", to_off ? "关闭" : "打开"));
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });
        viewBinding.btnToggleHpf.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean to_off = viewBinding.btnToggleHpf.getText().toString().startsWith("关闭");
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.addSetting("audio:noise_reduction_hpf", to_off ? "0" : "1");
                _lastUpdateBean = bean;
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                    DialogUtil.showDialog(String.format("正在%s音频过滤", to_off ? "关闭" : "打开"));
                } else {
                    showDisconnectToast();
                }
                return true;
            }
        });

    }

    @Override
    protected void initData() {
        GetSettingsExRequestBean bean = new GetSettingsExRequestBean();
        bean.addKey("audio.0:card_name");
        bean.addKey("audio:noise_reduction");
        bean.addKey("audio:noise_reduction_agc");
        bean.addKey("audio:noise_reduction_hpf");
        bean.addKey("audio.anr:noiseFactor");
        bean.addKey("audio.anr:swU");
        bean.addKey("audio.anr:PsiMin");
        bean.addKey("audio.anr:PsiMax");
        bean.addKey("audio.anr:fGmin");
        bean.addKey("audio.agc:attack_time");
        bean.addKey("audio.agc:release_time");
        bean.addKey("audio.agc:max_gain");
        bean.addKey("audio.agc:max_peak");
        bean.addKey("audio.agc:fRk0");
        bean.addKey("audio.agc:fRth0");
        bean.addKey("audio.agc:fRth1");
        bean.addKey("audio.agc:fRth2");
        bean.addKey("audio.hpf:Filter_type");
        bean.addKey("audio.hpf:Filter_mode");
        bean.addKey("audio.hpf:fc");
        bean.addKey("audio.hpf:bw");


        DeviceApiManager.sendCmdSimpleObj(bean);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);

        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWeewaSettingsExUpdateEvent(WeewaSettingsExUpdateEvent event) {
        AppTrace.d(TAG, "onWeewaSettingsExUpdateEvent");
        DialogUtil.dismissDialog();
        if(event != null && event._status == 1) {
            ToastUtil.s(getString(R.string.info_update_success));

            // refresh settings
            if(_lastUpdateBean != null && _lastUpdateBean.has("audio.0:card_name") ||
                    _lastUpdateBean.has("audio:noise_reduction") ||
                    _lastUpdateBean.has("audio:noise_reduction_agc") ||
                    _lastUpdateBean.has("audio:noise_reduction_hpf")
            ) {
                GetSettingsExRequestBean bean = new GetSettingsExRequestBean();
                bean.addKey("audio.0:card_name");
                bean.addKey("audio:noise_reduction");
                bean.addKey("audio:noise_reduction_agc");
                bean.addKey("audio:noise_reduction_hpf");
                DeviceApiManager.sendCmdSimpleObj(bean);
            }
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

        if(event.has("audio.anr:noiseFactor")) {
            viewBinding.etNoiseFactor.setText(event.get("audio.anr:noiseFactor"));
            viewBinding.etSwU.setText(event.get("audio.anr:swU"));
            viewBinding.etPsiMin.setText(event.get("audio.anr:PsiMin"));
            viewBinding.etPsiMax.setText(event.get("audio.anr:PsiMax"));
            viewBinding.etFGmin.setText(event.get("audio.anr:fGmin"));
            viewBinding.etAttackTime.setText(event.get("audio.agc:attack_time"));
            viewBinding.etReleaseTime.setText(event.get("audio.agc:release_time"));
            viewBinding.etMaxGain.setText(event.get("audio.agc:max_gain"));
            viewBinding.etMaxPeak.setText(event.get("audio.agc:max_peak"));
            viewBinding.etFRk0.setText(event.get("audio.agc:fRk0"));
            viewBinding.etFRth0.setText(event.get("audio.agc:fRth0"));
            viewBinding.etFRth1.setText(event.get("audio.agc:fRth1"));
            viewBinding.etFRth2.setText(event.get("audio.agc:fRth2"));
            viewBinding.etFilterType.setText(event.get("audio.hpf:Filter_type"));
            viewBinding.etFilterMode.setText(event.get("audio.hpf:Filter_mode"));
            viewBinding.etFc.setText(event.get("audio.hpf:fc"));
            viewBinding.etBw.setText(event.get("audio.hpf:bw"));
        }

        if(event.has("audio.0:card_name")) {
            String card = event.get("audio.0:card_name");
//            boolean soft = "softvol_cap".equals(card);
//            viewBinding.btnToggleSoundCard.setText(soft ? "关闭音量功能(使用硬件声卡)" : "打开音量功能(使用软声卡)");
            viewBinding.btnToggleSoundCard.setText("当前声卡："+card);
        }
        if(event.has("audio:noise_reduction")) {
            boolean on = "1".equals(event.get("audio:noise_reduction"));
            viewBinding.btnToggleAnr.setText(on ? "关闭音频降噪(anr)" : "打开音频降噪(anr)");
        }
        if(event.has("audio:noise_reduction_agc")) {
            boolean on = "1".equals(event.get("audio:noise_reduction_agc"));
            viewBinding.btnToggleAgc.setText(on ? "关闭音频增益(agc)" : "打开音频增益(agc)");
        }
        if(event.has("audio:noise_reduction_hpf")) {
            boolean on = "1".equals(event.get("audio:noise_reduction_hpf"));
            viewBinding.btnToggleHpf.setText(on ? "关闭音频过滤(hpf)" : "打开音频过滤(hpf)");
        }
    }

}
