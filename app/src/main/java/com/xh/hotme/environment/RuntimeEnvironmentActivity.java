package com.xh.hotme.environment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.view.View;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.bluetooth.bean.GetSettingsExRequestBean;
import com.xh.hotme.bluetooth.bean.UpdateSettingsExRequestBean;
import com.xh.hotme.bluetooth.event.WeewaSettingsExEvent;
import com.xh.hotme.bluetooth.event.WeewaSettingsExUpdateEvent;
import com.xh.hotme.databinding.ActivityRuntimeEnvironmentBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.rabbitmq.RabbitMQ;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.utils.YTextWatcher;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@IEventBus
public class RuntimeEnvironmentActivity extends BaseActivity<ActivityRuntimeEnvironmentBinding> {
    private boolean camera;
    private boolean loaded;
    private boolean supportsCustom = true;
    private boolean saving;
    private SharedPreferences customAddresses;
    private boolean updatingCustomAddresses;
    private final Runnable timeout = () -> {
        saving = false;
        viewBinding.saveEnvironment.setEnabled(true);
        viewBinding.environmentStatus.setText(loaded ? "未收到保存结果，请重新读取相机配置后确认" : "读取相机配置超时，请检查连接后重试");
        if (camera) loaded = false;
    };

    public static void start(Context context, boolean camera) {
        if (!RuntimeEnvironment.canEdit()) return;
        context.startActivity(new Intent(context, RuntimeEnvironmentActivity.class).putExtra("camera", camera));
    }

    @Override protected void initView() {
        if (!RuntimeEnvironment.canEdit()) {
            viewBinding.getRoot().setVisibility(View.GONE);
            finish();
            return;
        }
        camera = getIntent().getBooleanExtra("camera", false);
        customAddresses = getSharedPreferences("runtime_environment_custom", MODE_PRIVATE);
        TitleLayoutHelper.initTitle(viewBinding.titleBar, camera ? "相机运行环境" : "App 运行环境");
        viewBinding.environmentHint.setText(camera
                ? "请将相机与 App 设置为同一运行环境。保存后相机服务将重启，录制或直播可能中断；App 的环境需在设置中单独配置。"
                : "请将 App 与相机设置为同一运行环境。切换服务器后需重新登录；相机的环境需在相机设置中单独配置。");
        displayCustomAddresses(RuntimeEnvironment.customDefaults());
        YTextWatcher customAddressWatcher = new YTextWatcher() {
            @Override public void afterTextChanged(Editable text) { rememberCustomAddresses(); }
        };
        viewBinding.rabbitmqAddress.addTextChangedListener(customAddressWatcher);
        viewBinding.apiAddress.addTextChangedListener(customAddressWatcher);
        viewBinding.environmentOptions.setOnCheckedChangeListener((group, id) ->
                viewBinding.customFields.setVisibility(id == viewBinding.environmentCustom.getId() ? View.VISIBLE : View.GONE));
        viewBinding.saveEnvironment.setOnClickListener(v -> {
            if (camera && !loaded) { readCamera(); return; }
            save();
        });
    }

    @Override protected void initData() {
        if (!RuntimeEnvironment.canEdit()) return;
        if (camera) readCamera();
        else { loaded = true; display(RuntimeEnvironment.get()); }
    }

    private void readCamera() {
        loaded = false;
        viewBinding.saveEnvironment.setEnabled(false);
        viewBinding.environmentStatus.setText("正在读取相机配置…");
        GetSettingsExRequestBean request = new GetSettingsExRequestBean();
        request.addKey(RuntimeEnvironment.CAMERA_KEY);
        request.addKey("idle.server:test_server");
        if (!DeviceApiManager.sendCmdSimpleObj(request)) {
            viewBinding.environmentStatus.setText("相机未连接，请连接后重试");
            viewBinding.saveEnvironment.setEnabled(true);
            return;
        }
        _handler.removeCallbacks(timeout);
        _handler.postDelayed(timeout, 15000);
    }

    private void display(EnvironmentProfile profile) {
        displayCustomAddresses(EnvironmentProfile.CUSTOM.equals(profile.mode)
                ? profile : RuntimeEnvironment.customDefaults());
        viewBinding.environmentOptions.check(EnvironmentProfile.CUSTOM.equals(profile.mode) ? viewBinding.environmentCustom.getId()
                : EnvironmentProfile.TEST.equals(profile.mode) ? viewBinding.environmentTest.getId() : viewBinding.environmentProduction.getId());
    }

    private void displayCustomAddresses(EnvironmentProfile profile) {
        updatingCustomAddresses = true;
        try {
            viewBinding.rabbitmqAddress.setText(customAddresses.getString(customAddressKey("rabbitmq"), profile.rabbitmq));
            viewBinding.apiAddress.setText(customAddresses.getString(customAddressKey("api"), profile.api));
        } finally {
            updatingCustomAddresses = false;
        }
    }

    private String customAddressKey(String address) {
        return (camera ? "camera_" : "app_") + address;
    }

    private void rememberCustomAddresses() {
        if (updatingCustomAddresses) return;
        customAddresses.edit()
                .putString(customAddressKey("rabbitmq"), viewBinding.rabbitmqAddress.getText().toString())
                .putString(customAddressKey("api"), viewBinding.apiAddress.getText().toString())
                .apply();
    }

    private String selectedMode() {
        int id = viewBinding.environmentOptions.getCheckedRadioButtonId();
        return id == viewBinding.environmentCustom.getId() ? EnvironmentProfile.CUSTOM
                : id == viewBinding.environmentTest.getId() ? EnvironmentProfile.TEST : EnvironmentProfile.PRODUCTION;
    }

    private void save() {
        if (!RuntimeEnvironment.canEdit()) return;
        if (saving) return;
        final EnvironmentProfile profile;
        try {
            profile = camera
                    ? RuntimeEnvironment.resolveCamera(selectedMode(), viewBinding.rabbitmqAddress.getText().toString(), viewBinding.apiAddress.getText().toString())
                    : RuntimeEnvironment.resolve(selectedMode(), viewBinding.rabbitmqAddress.getText().toString(), viewBinding.apiAddress.getText().toString());
        } catch (IllegalArgumentException e) {
            viewBinding.environmentStatus.setText(e.getMessage());
            return;
        }
        if (camera) {
            UpdateSettingsExRequestBean request = new UpdateSettingsExRequestBean();
            if (supportsCustom) {
                JSONObject value = new JSONObject();
                value.put("mode", profile.mode);
                value.put("rabbitmq", profile.rabbitmq);
                value.put("api", profile.api);
                request.addSetting(RuntimeEnvironment.CAMERA_KEY, value.toJSONString());
            } else {
                request.addSetting("idle.server:test_server", EnvironmentProfile.TEST.equals(profile.mode) ? "1" : "0");
            }
            saving = true;
            viewBinding.saveEnvironment.setEnabled(false);
            viewBinding.environmentStatus.setText("正在保存相机配置…");
            if (!DeviceApiManager.sendCmdSimpleObj(request)) {
                saving = false;
                viewBinding.saveEnvironment.setEnabled(true);
                showDisconnectToast();
                return;
            }
            _handler.postDelayed(timeout, 15000);
        } else {
            EnvironmentProfile before = RuntimeEnvironment.get();
            if (before.mode.equals(profile.mode) && before.rabbitmq.equals(profile.rabbitmq) && before.api.equals(profile.api)) { finish(); return; }
            if (!RuntimeEnvironment.save(profile)) { viewBinding.environmentStatus.setText("保存失败，请重试"); return; }
            OkHttpUtil.cancelPendingRequests();
            LoginManager.clearLoginInfo();
            com.xh.hotme.bluetooth.DeviceManager.getImpl().clearCacheBindDeviceList();
            com.xh.hotme.bluetooth.DeviceManager._bindDeviceResult = null;
            com.xh.hotme.bluetooth.DeviceManager._bindDevice = null;
            com.xh.hotme.camera.CameraManager.clearOTAInfo();
            RabbitMQ.Companion.getImpl().reloadEnvironment();
            ToastUtil.s("运行环境已保存，请重新登录");
            Intent intent = new Intent(this, MobileLoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN) public void onSettings(WeewaSettingsExEvent event) {
        if (!camera || loaded || !event.has(RuntimeEnvironment.CAMERA_KEY)) return;
        _handler.removeCallbacks(timeout);
        String value = event.get(RuntimeEnvironment.CAMERA_KEY);
        supportsCustom = value != null && !value.isEmpty();
        try {
            if (supportsCustom) {
                JSONObject json = JSON.parseObject(value);
                display(RuntimeEnvironment.resolveCamera(json.getString("mode"), json.getString("rabbitmq"), json.getString("api")));
            } else {
                display(RuntimeEnvironment.resolveCamera("1".equals(event.get("idle.server:test_server")) ? EnvironmentProfile.TEST : EnvironmentProfile.PRODUCTION, "", ""));
            }
            loaded = true;
            viewBinding.environmentCustom.setEnabled(supportsCustom);
            viewBinding.environmentStatus.setText(supportsCustom ? "" : "当前相机固件仅支持正式和测试环境，升级固件后可配置自定义地址");
        } catch (Exception e) {
            viewBinding.environmentStatus.setText("相机配置无效，请检查固件版本");
        }
        viewBinding.saveEnvironment.setEnabled(true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN) public void onSaved(WeewaSettingsExUpdateEvent event) {
        if (!camera || !saving) return;
        saving = false;
        _handler.removeCallbacks(timeout);
        viewBinding.saveEnvironment.setEnabled(true);
        if (event._status == 1) {
            ToastUtil.s("相机环境已保存，服务正在重启");
            finish();
        } else viewBinding.environmentStatus.setText("相机配置保存失败，请重试");
    }

    @Override public void onPause() {
        rememberCustomAddresses();
        super.onPause();
    }

    @Override public void onDestroy() {
        _handler.removeCallbacks(timeout);
        super.onDestroy();
    }
}
