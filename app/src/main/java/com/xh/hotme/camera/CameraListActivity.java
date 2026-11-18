package com.xh.hotme.camera;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.bluetooth.event.RefreshBindDeviceEvent;
import com.xh.hotme.bluetooth.listener.IBleScanNotifyListener;
import com.xh.hotme.databinding.ActivityCameraListBinding;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.PermissionsUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

@IEventBus
public class CameraListActivity extends BaseActivity<ActivityCameraListBinding> implements IBleScanNotifyListener, EasyPermissions.PermissionCallbacks {
    // views
    private ImageView _refreshIv;
    private TextView _refreshLabel;

    ObjectAnimator _refreshAnimator;
    BindDeviceListFragment _fragment;

    boolean skipCameraInfo = false;
    int _type;
    private static final String[] PERMISSIONS_STORAGE = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE };


    public static void start(Context context, boolean skipCameraInfo) {
        if(null != context) {
            Intent intent = new Intent(context, CameraListActivity.class);
            intent.putExtra(Constants.SKIP_CAMERA_INFO, skipCameraInfo);
            intent.putExtra(Constants.VIDEO_CATEGORY, BindDeviceListFragment.type_camera_manager);
            context.startActivity(intent);
        }
    }

    public static void start(Context context, int type, boolean skipCameraInfo) {
        if(null != context) {
            Intent intent = new Intent(context, CameraListActivity.class);
            intent.putExtra(Constants.SKIP_CAMERA_INFO, skipCameraInfo);
            intent.putExtra(Constants.VIDEO_CATEGORY, type);
            context.startActivity(intent);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothHandle.addScanNotifyListener(this);
    }

    @Override
    protected void initView() {

        if(getIntent() != null) {
            skipCameraInfo = getIntent().getBooleanExtra(Constants.SKIP_CAMERA_INFO, false);

            _type  = getIntent().getIntExtra(Constants.VIDEO_CATEGORY, BindDeviceListFragment.type_camera_manager);
        }

        viewBinding.titleBar.tvTitle.setText(getString(R.string.active_device_list_title));
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        _refreshLabel = viewBinding.titleBar.tvRight;
        _refreshIv = viewBinding.titleBar.ivRight;
        _refreshIv.setImageResource(R.mipmap.home_refresh);

        _refreshAnimator = ObjectAnimator.ofFloat(_refreshIv, "rotation", 1080f);
        _refreshAnimator.setDuration(2000L);
        _refreshAnimator.setRepeatCount(-1);

        _refreshLabel.setVisibility(View.VISIBLE);
        _refreshLabel.setText(getString(R.string.loading_dialog_refresh));


        _refreshLabel.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
                    boolean hasLocationSer = PermissionsUtil.isLocServiceEnable(CameraListActivity.this);
                    if (!hasLocationSer) {
                        PermissionsUtil.openLocationServiceForResult(CameraListActivity.this, PermissionsUtil.REQUEST_LOCATION_SERVICE);
                        return true;
                    }

                    boolean hasPermission = PermissionsUtil.hasBlePermission(CameraListActivity.this);
                    if (!hasPermission) {
                        EasyPermissions.requestPermissions(CameraListActivity.this, getString(R.string.bluetooth_permission_title), PermissionsUtil.REQUEST_PERMISSION_BLUE, PermissionsUtil.getBlePermissions());
                    } else {
                        boolean refreshing = _fragment.onRefresh();
                    }
                } else {
                    onScanning(false);
                    DeviceManager.getImpl().onRefreshList(false);
                }

                return true;
            }
        });

        _refreshIv.setVisibility(View.GONE);

        _fragment = BindDeviceListFragment.newInstance(_type, skipCameraInfo);

        getSupportFragmentManager().beginTransaction()
            .add(viewBinding.content.getId(), _fragment)
            .commit();

        if(!PermissionsUtil.hasBlePermission(CameraListActivity.this)){
            requestBluetoothPermission();
        }
    }

    @Override
    protected void initData() {

    }
    @Override
    public void onResume() {
        super.onResume();
        AppTrace.d(TAG, "onResume");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppTrace.d(TAG, "onDestroy");
        if(_refreshAnimator != null) {
            _refreshAnimator = null;
        }
        BluetoothHandle.removeScanNotifyListener(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //将请求结果传递EasyPermission库处理
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);

        switch(requestCode) {
            case PermissionsUtil.REQUEST_READ_PHONE_STATE:
                if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, PermissionsUtil.REQUEST_PERMISSION_STORAGE);
                    } else {
                        onRefreshFragment();
                    }
                }
                break;
            case PermissionsUtil.REQUEST_PERMISSION_STORAGE:
                if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //TODO
                    new Handler().post(() -> onRefreshFragment());
                } else {

                }
                break;
            case PermissionsUtil.REQUEST_LOCATION_SERVICE:
                boolean hasLocationSer = PermissionsUtil.isLocServiceEnable(CameraListActivity.this);
                if(hasLocationSer) {
                    //TODO
                    new Handler().post(() -> onRefreshFragment());
                }
                break;
            case PermissionsUtil.REQUEST_PERMISSION:
            case PermissionsUtil.REQUEST_PERMISSION_LOCATION:
            case PermissionsUtil.REQUEST_PERMISSION_BLUE:
                //TODO
                new Handler().post(() -> {
                    if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                        onRefreshFragment();
                    }
                });

                break;

        }
    }

    private boolean onRefreshFragment() {
        boolean hasLocationSer = PermissionsUtil.isLocServiceEnable(CameraListActivity.this);
        if(!hasLocationSer) {
            PermissionsUtil.openLocationService(CameraListActivity.this);
            return true;
        }

        boolean hasPermission = PermissionsUtil.hasBlePermission(this);
        if(hasPermission) {
            if(_fragment != null) {
                _fragment.onRefresh();
            }
        } else {
            //PermissionsUtil.requestBlePermission(this);
            return false;
        }

        return true;
    }

    @Override
    public void onScanning(boolean isBackground) {
        _refreshLabel.setVisibility(View.GONE);
        _refreshIv.setVisibility(View.VISIBLE);
        if(_refreshAnimator != null) {
            _refreshAnimator.start();
        }
    }

    @Override
    public void onLeScan(Device device) {

    }

    @Override
    public void onTimeout() {
        _refreshLabel.setVisibility(View.VISIBLE);
        _refreshIv.setVisibility(View.GONE);
        if(_refreshAnimator != null) {
            _refreshAnimator.end();
        }
    }

    @Override
    public void onScanEnd() {
        _refreshLabel.setVisibility(View.VISIBLE);
        _refreshIv.setVisibility(View.GONE);
        if(_refreshAnimator != null) {
            _refreshAnimator.end();
        }
    }


    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        // ToastUtil.showToastShort(CameraListActivity.this, "用户授权成功");
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        ToastUtil.s(getString(R.string.permission_user_deny));
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            showConfirmDialog(
                getString(R.string.permission_deny_title),
                getString(R.string.permission_deny_content),
                getString(R.string.setting),
                getString(R.string.cancel),
                new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        //跳转应用消息，间接打开应用权限设置-效率高
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", CameraListActivity.this.getPackageName(), null);
                        intent.setData(uri);
                        CameraListActivity.this.startActivity(intent);
                        return true;
                    }
                },
                null
            );
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshBindDevice(RefreshBindDeviceEvent event) {
        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_REMOTE) {
            onScanEnd();
        }
    }
}
