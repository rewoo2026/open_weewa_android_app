package com.xh.hotme.test;

import android.content.Context;
import android.content.Intent;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bean.DeviceSettingMbrBean;
import com.xh.hotme.bean.TextModelBean;
import com.xh.hotme.bluetooth.BluetoothManager;
import com.xh.hotme.bluetooth.bean.GetSettingsExRequestBean;
import com.xh.hotme.bluetooth.bean.UpdateSettingsExRequestBean;
import com.xh.hotme.bluetooth.event.WeewaSettingsExEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExUpdateEvent;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivityTestMbrSettingBinding;
import com.xh.hotme.databinding.ActivityTestVideoSettingBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.VideoUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;


@IEventBus
public class TestVideoSettingActivity extends BaseActivity<ActivityTestVideoSettingBinding> {

    private final static String TAG = TestVideoSettingActivity.class.getSimpleName();

    private UpdateSettingsExRequestBean _lastUpdateBean;

    private final Runnable _timeOutRunnable = () -> {
    };


    public static void start(Context context) {
        Intent intent = new Intent(context, TestVideoSettingActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {

        TitleLayoutHelper.initTitle(viewBinding.titleBar, "视频设置");

        viewBinding.btnAvsSetting.setOnClickListener(view -> {
            try {
                // groundDistance单位是厘米所以要乘100
                int groundDistance = (int) (Float.parseFloat(viewBinding.etGroundDistance.getText().toString()) * 100);
                int stitchDistance = (int) Float.parseFloat(viewBinding.etStitchDistance.getText().toString());
                int zoomWidth = (int) Float.parseFloat(viewBinding.etZoomWidth.getText().toString());
                int zoomHeight = (int) Float.parseFloat(viewBinding.etZoomHeight.getText().toString());
                int zoomWidth5p = (int) Float.parseFloat(viewBinding.etZoomWidth5p.getText().toString());
                int zoomHeight5p = (int) Float.parseFloat(viewBinding.etZoomHeight5p.getText().toString());
                int fecFileWidth = (int) Float.parseFloat(viewBinding.etFecFileWidth.getText().toString());
                int fecFileHeight = (int) Float.parseFloat(viewBinding.etFecFileHeight.getText().toString());
                int fecLiveWidth = (int) Float.parseFloat(viewBinding.etFecLiveWidth.getText().toString());
                int fecLiveHeight = (int) Float.parseFloat(viewBinding.etFecLiveHeight.getText().toString());
                UpdateSettingsExRequestBean bean = new UpdateSettingsExRequestBean();
                bean.setRestart(1);
                bean.addSetting("avs:ground_distance", groundDistance);
                bean.addSetting("avs:stitch_distance", stitchDistance);
                bean.addSetting("video.1:zoom_width", zoomWidth);
                bean.addSetting("video.1:zoom_height", zoomHeight);
                bean.addSetting("video.1:zoom_width_5p", zoomWidth5p);
                bean.addSetting("video.1:zoom_height_5p", zoomHeight5p);
                bean.addSetting("video.1:width", fecFileWidth);
                bean.addSetting("video.1:height", fecFileHeight);
                bean.addSetting("video.2:width", fecLiveWidth);
                bean.addSetting("video.2:height", fecLiveHeight);
                _lastUpdateBean = bean;
                boolean isOk = DeviceApiManager.sendCmdSimpleObj(bean);
                if(isOk) {
                    _handler.postDelayed(_timeOutRunnable, BluetoothManager.RESP_TIMEOUT_SHORT);
                    DialogUtil.showDialog("正在设置相机高度和拼接距离,如果rkipc已经启动将会自动重启");
                } else {
                    showDisconnectToast();
                }
            } catch(NumberFormatException e) {
                ToastUtil.s(getString(R.string.info_value_verify_illegal));
            }
        });


    }

    @Override
    protected void initData() {
        GetSettingsExRequestBean bean = new GetSettingsExRequestBean();
        bean.addKey("avs:ground_distance");
        bean.addKey("avs:stitch_distance");
        bean.addKey("video.1:zoom_width");
        bean.addKey("video.1:zoom_height");
        bean.addKey("video.1:zoom_width_5p");
        bean.addKey("video.1:zoom_height_5p");
        bean.addKey("video.1:width");
        bean.addKey("video.1:height");
        bean.addKey("video.2:width");
        bean.addKey("video.2:height");

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
    public void onWeewaSettingExEvent(WeewaSettingsExEvent event) {
        AppTrace.d(TAG, "onWeewaSettingExEvent");
        if(event == null || event.settings == null) {
            return;
        }

        if(event.has("avs:ground_distance")) {
            double ground_distance = 0L;
            try {
                ground_distance = Double.parseDouble(event.get("avs:ground_distance")) / 100;
            } catch(Throwable e) {
                e.printStackTrace();
            }

            viewBinding.etGroundDistance.setText(String.format("%.1f", ground_distance));
            viewBinding.etStitchDistance.setText(event.get("avs:stitch_distance"));
            viewBinding.etZoomWidth.setText(event.get("video.1:zoom_width"));
            viewBinding.etZoomHeight.setText(event.get("video.1:zoom_height"));
            viewBinding.etZoomWidth5p.setText(event.get("video.1:zoom_width_5p"));
            viewBinding.etZoomHeight5p.setText(event.get("video.1:zoom_height_5p"));
            viewBinding.etFecFileWidth.setText(event.get("video.1:width"));
            viewBinding.etFecFileHeight.setText(event.get("video.1:height"));
            viewBinding.etFecLiveWidth.setText(event.get("video.2:width"));
            viewBinding.etFecLiveHeight.setText(event.get("video.2:height"));
        }

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

}
