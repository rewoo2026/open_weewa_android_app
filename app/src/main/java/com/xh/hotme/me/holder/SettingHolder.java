package com.xh.hotme.me.holder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.camera.ActiveListActivity;
import com.xh.hotme.camera.BindDeviceListFragment;
import com.xh.hotme.camera.CameraListActivity;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.camera.DeviceListFragment;
import com.xh.hotme.camera.softap.CameraNetworkSettingActivity;
import com.xh.hotme.camera.softap.CameraSoftApActivity;
import com.xh.hotme.me.LivePushSettingActivity;
import com.xh.hotme.me.bean.SettingBean;
import com.xh.hotme.me.camsetting.CameraSettingsActivity;
import com.xh.hotme.me.profile.ProfileActivity;
import com.xh.hotme.me.setting.SettingActivity;
import com.xh.hotme.record.setting.CameraAboutActivity;
import com.xh.hotme.record.setting.CameraRunningInfoActivity;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.ToastUtil;

public class SettingHolder extends CommonViewHolder<SettingBean> {

    // views
    private final TextView _nameLabel;
    private final ImageView _iconIv;
    private final View _itemView;

    final Activity _ctx;


    SettingBean _settingMode;


    public static SettingHolder create(Activity ctx, ViewGroup parent) {
        View convertView = LayoutInflater.from(ctx).inflate(R.layout.me_layout_setting_list_item, parent, false);
        return new SettingHolder(ctx, convertView);
    }

    public SettingHolder(Activity context, View itemView) {
        super(itemView);

        // find views
        _ctx = context;
        _iconIv = itemView.findViewById(R.id.iv_icon);
        _nameLabel = itemView.findViewById(R.id.tv_name);
        _itemView = itemView.findViewById(R.id.setting_item);
    }

    @Override
    public void onBind(final SettingBean model, int position) {
        _settingMode = model;
        if(model.getIcon() != 0) {
            _iconIv.setVisibility(View.VISIBLE);
            _iconIv.setImageResource(model.getIcon());
        } else {
            _iconIv.setVisibility(View.GONE);
        }
        _nameLabel.setText(model.getName());

        _itemView.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                switch(model.getType()) {
                    case SettingAdapter.SETTING_TYPE_USERINFO:
                        if(LoginManager.isSignedIn()) {
                            ProfileActivity.start(_ctx);
                        } else {
                            MobileLoginActivity.start(_ctx);
                        }
                        break;
                    case SettingAdapter.SETTING_TYPE_ACCOUNT:
                        SettingActivity.start(_ctx);
                        break;
                    case SettingAdapter.SETTING_TYPE_KUAICHUAN:
                        startSelectCameraForFastTransfer();
                        break;

                    case SettingAdapter.SETTING_TYPE_CAMERA_SOFT_AP_SETTING:
                        CameraSoftApActivity.start(_ctx, CameraSoftApActivity.REQUEST_SOFT_AP_CAMERA_INFO);
                        break;
                    case SettingAdapter.SETTING_TYPE_CAMERA_RUNNING_INFO:
                        CameraRunningInfoActivity.start(_ctx);
                        break;
                    case SettingAdapter.SETTING_TYPE_CAMERA_ABOUT:
                        CameraAboutActivity.start(_ctx);
                        break;

                    case SettingAdapter.SETTING_TYPE_AI_CLIP:
                        if(LoginManager.isSignedIn()) {
                            startSelectCameraForAIClip();
                        } else {
                            MobileLoginActivity.start(_ctx);
                        }

                        break;
                    case SettingAdapter.SETTING_TYPE_MANUAL_DOTTING_CLIP:
                        if(LoginManager.isSignedIn()) {
                            startSelectCameraForManualDottingVideoClip();
                        } else {
                            MobileLoginActivity.start(_ctx);
                        }

                        break;
                    //相机功能
                    case SettingAdapter.SETTING_TYPE_CAMERA_SETTING:
                        if(LoginManager.isSignedIn()) {
                            _ctx.startActivity(new Intent(_ctx, CameraSettingsActivity.class));
                        } else {
                            MobileLoginActivity.start(_ctx);
                        }
                        break;
                    //相机网络
                    case SettingAdapter.SETTING_TYPE_CAMERA_NET:
                        if(LoginManager.isSignedIn()) {
                            _ctx.startActivity(new Intent(_ctx, CameraNetworkSettingActivity.class));
                        } else {
                            MobileLoginActivity.start(_ctx);
                        }
                        break;

                    //直播管理
                    case SettingAdapter.SETTING_TYPE_CAMERA_LIVE:

                        if(!TextUtils.isEmpty(CameraManager.getDeviceIdLast())) {

                            if(LoginManager.isSignedIn()) {
                                Intent intent = new Intent(_ctx, LivePushSettingActivity.class);
                                intent.putExtra("type", 1);
                                _ctx.startActivity(intent);
                            } else {
                                MobileLoginActivity.start(_ctx);
                            }
                        } else {
                            ToastUtil.s(_ctx.getString(R.string.device_disconnect_current));
                        }
                        break;
                }

                return true;
            }
        });

    }

    private void startSelectCameraForFastTransfer() {
        if(DeviceManager.getBindDeviceList() == null || DeviceManager.getBindDeviceList().isEmpty()) {
            ActiveListActivity.start(_ctx, DeviceListFragment.type_soft_ap);
        } else {
            CameraListActivity.start(_ctx, BindDeviceListFragment.type_fast_transfer, false);
        }
    }

    private void startSelectCameraForManualDottingVideoClip() {
        if(DeviceManager.getBindDeviceList() == null || DeviceManager.getBindDeviceList().isEmpty()) {
            ActiveListActivity.start(_ctx, DeviceListFragment.type_setting_video_clip_manual_dotting);
        } else {
            CameraListActivity.start(_ctx, BindDeviceListFragment.type_setting_video_clip_manual_dotting,false);
        }
    }
    private void startSelectCameraForAIClip() {
        if(DeviceManager.getBindDeviceList() == null || DeviceManager.getBindDeviceList().isEmpty()) {
            ActiveListActivity.start(_ctx, DeviceListFragment.type_setting_video_clip_ai);
        } else {
            CameraListActivity.start(_ctx, BindDeviceListFragment.type_setting_video_clip_ai,false);
        }
    }
}
