package com.xh.hotme.record.setting;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.TitleLayoutHelper;
import com.xh.hotme.bean.CameraInfo;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.databinding.ActivityCameraAboutBinding;
import com.xh.hotme.utils.TimeUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class CameraAboutActivity extends BaseActivity<ActivityCameraAboutBinding> {

    private final static String TAG = CameraAboutActivity.class.getSimpleName();

    CameraAboutAdapter _adapter;

    List<CameraAboutModuleBean> _moduleList;

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, CameraAboutActivity.class);
        ctx.startActivity(intent);
    }


    @Override
    protected void initView() {
        TitleLayoutHelper.initTitle(viewBinding.titleBar, "关于本机");
    }

    @Override
    protected void initData() {

        _adapter = new CameraAboutAdapter(CameraAboutActivity.this);

        CameraInfo cameraInfo = CameraManager.getCameraInfo(null);

        if(cameraInfo != null) {
            updateUI(cameraInfo);
        }

        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(CameraAboutActivity.this));
        viewBinding.recyclerView.setAdapter(_adapter);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void updateUI(CameraInfo cameraInfo) {

        if(_moduleList != null) {
            _moduleList.clear();
        } else {
            _moduleList = new ArrayList<>();
        }

        String activeTime = "";

        if(!TextUtils.isEmpty(cameraInfo.deviceInfo.active_time) && !cameraInfo.deviceInfo.active_time.equalsIgnoreCase("null")) {
            activeTime = TimeUtil.stampToDate(cameraInfo.deviceInfo.active_time, new SimpleDateFormat("yyyy-MM-dd"));
        }

        CameraAboutModuleBean deviceNameModule = new CameraAboutModuleBean("设备名称", cameraInfo.deviceInfo.device_name);
        CameraAboutModuleBean accountModule = new CameraAboutModuleBean("绑定手机号", cameraInfo.deviceInfo.account);
        CameraAboutModuleBean registerModule = new CameraAboutModuleBean("激活时间", activeTime);
        CameraAboutModuleBean deviceIdModule = new CameraAboutModuleBean("设备ID", cameraInfo.deviceInfo.device_id);
        CameraAboutModuleBean softwareVersionModule = new CameraAboutModuleBean("固件版本", cameraInfo.deviceInfo.ota_version);
        CameraAboutModuleBean macModule = new CameraAboutModuleBean("MAC地址", cameraInfo.deviceInfo.mac);

        _moduleList.add(deviceNameModule);
        _moduleList.add(accountModule);
        _moduleList.add(registerModule);
        _moduleList.add(deviceIdModule);
        _moduleList.add(softwareVersionModule);
        _moduleList.add(macModule);
        _adapter.setModuleList(_moduleList);

        _adapter.notifyDataSetChanged();
    }
}
