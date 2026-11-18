package com.xh.hotme.test;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bean.WeewaInfo;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.bean.GetEthernetSettingsResponseBean;
import com.xh.hotme.bluetooth.bean.SetEthernetDhcpRequestBean;
import com.xh.hotme.bluetooth.bean.SetEthernetStaticRequestBean;
import com.xh.hotme.bluetooth.bean.WeewaInfoRequestBean;
import com.xh.hotme.bluetooth.event.SetEthernetDhcpEvent;
import com.xh.hotme.bluetooth.event.SetEthernetStaticEvent;
import com.xh.hotme.bluetooth.listener.IBleWeewaInfoNotifyListener;
import com.xh.hotme.databinding.ActivityEthernetSettingBinding;
import com.xh.hotme.http.DeviceApiManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import com.xh.hotme.utils.MainHandler;

@IEventBus
public class EthernetSettingActivity extends BaseActivity<ActivityEthernetSettingBinding> implements IBleWeewaInfoNotifyListener {

    private boolean isDhcpMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BluetoothHandle.addWeewaInfoNotifyListener(this);
    }

    @Override
    protected void initView() {
        TitleLayoutHelper.initTitle(viewBinding.titlebar, "有线网络设置");
        viewBinding.titlebar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        // 初始化单选按钮组
        viewBinding.rgNetworkMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_dhcp) {
                    isDhcpMode = true;
                    viewBinding.layoutManualInput.setVisibility(View.GONE);
                } else {
                    isDhcpMode = false;
                    viewBinding.layoutManualInput.setVisibility(View.VISIBLE);
                }
            }
        });

        // 设置保存按钮点击事件
        viewBinding.btnSave.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if (isDhcpMode) {
                    // 发送DHCP模式设置命令 fix
                    DialogUtil.showDialog("正在设置DHCP模式...");
                    DeviceApiManager.sendCmdSimpleObj(new SetEthernetStaticRequestBean(1,"", "", "", ""));
                } else {
                    // 验证输入并发送静态IP设置命令
                    if (validateInput()) {
                        DialogUtil.showDialog("正在设置静态IP...");
                        setStaticIpMode();
                    }
                }
                return true;
            }
        });

        // 默认选中DHCP模式
        viewBinding.rbDhcp.setChecked(true);
        viewBinding.layoutManualInput.setVisibility(View.GONE);
    }

    @Override
    protected void initData() {
        // 获取当前网络设置
        getCurrentEthernetSettings();
    }


    private void getCurrentEthernetSettings() {
        // 这里需要实现获取当前有线网络设置的逻辑
        AppTrace.d("asx","getCurrentEthernetSettings");
        DeviceApiManager.sendCmdSimpleObj(new WeewaInfoRequestBean());

    }

    private boolean validateInput() {
        String ip = viewBinding.etIpAddress.getText().toString().trim();
        String subnet = viewBinding.etSubnetMask.getText().toString().trim();
        String gateway = viewBinding.etGateway.getText().toString().trim();
        String dns = viewBinding.etDns.getText().toString().trim();

        if (ip.isEmpty() || subnet.isEmpty() || gateway.isEmpty() || dns.isEmpty()) {
            ToastUtil.s("请填写所有字段");
            return false;
        }

        // 验证IP地址格式
        if (!isValidIpAddress(ip) || !isValidIpAddress(subnet) || 
            !isValidIpAddress(gateway) || !isValidIpAddress(dns)) {
            ToastUtil.s("IP地址格式不正确");
            return false;
        }

        return true;
    }

    private boolean isValidIpAddress(String ip) {
        String pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(pattern);
    }

    private void setStaticIpMode() {
        String ip = viewBinding.etIpAddress.getText().toString().trim();
        String subnet = viewBinding.etSubnetMask.getText().toString().trim();
        String gateway = viewBinding.etGateway.getText().toString().trim();
        String dns = viewBinding.etDns.getText().toString().trim();

        // 发送静态IP设置命令 fix
        DeviceApiManager.sendCmdSimpleObj(new SetEthernetStaticRequestBean(0,ip, subnet, gateway, dns));
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        BluetoothHandle.removeWeewaInfoNotifyListener(this);
        DialogUtil.dismissDialog();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetEthernetDhcpEvent(SetEthernetDhcpEvent event) {
        DialogUtil.dismissDialog();
        if (isDhcpMode) {
            if (event.status == 1) {
                ToastUtil.s("DHCP模式设置成功");
                finish();
            } else {
                ToastUtil.s("DHCP模式设置失败");
            }
        }else{
            if (event.status == 1) {
                ToastUtil.s("静态IP设置成功");
                finish();
            } else {
                ToastUtil.s("静态IP设置失败");
            }
        }

    }


    @Override
    public void onWeewaInfo(WeewaInfo data) {
        AppTrace.d("asx","onWeewaInfo" + data.ethbean.ip + data.d.device_name);

        MainHandler.runOnUiThread(() -> {
            try {
                if (data != null) {
                    updateEthernetSettings(data.ethbean);
                } else {
                    AppTrace.d(TAG, "onWeewaInfo fail，data is null");
                    ToastUtil.s(getString(R.string.please_retry_on_weewa_info_fail));
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateEthernetSettings(GetEthernetSettingsResponseBean info) {
        if (info.dhcp == 0) {
            // 此时为手动
            viewBinding.rbDhcp.setChecked(false);
            viewBinding.rbStatic.setChecked(true);
            viewBinding.layoutManualInput.setVisibility(View.VISIBLE);
            viewBinding.etIpAddress.setText(info.ip);
            viewBinding.etSubnetMask.setText(info.netmask);
            viewBinding.etGateway.setText(info.gateway);
            viewBinding.etDns.setText(info.dns);
        } else {
            viewBinding.rbDhcp.setChecked(true);
            viewBinding.rbStatic.setChecked(false);
            viewBinding.layoutManualInput.setVisibility(View.GONE);
            viewBinding.etIpAddress.setText("");
            viewBinding.etSubnetMask.setText("");
            viewBinding.etGateway.setText("");
            viewBinding.etDns.setText("");
        }
    }



    @Override
    public void onWeewaInfoFail() {

    }



}