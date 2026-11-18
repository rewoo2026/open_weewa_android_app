package com.xh.hotme.camera.softap;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.bluetooth.BluetoothHandle;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.listener.IBleScanNotifyListener;
import com.xh.hotme.camera.DeviceListFragment;
import com.xh.hotme.databinding.ActiveActivityListBinding;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.PermissionsUtil;

import java.util.ArrayList;


public class SoftApDeviceListActivity extends BaseActivity<ActiveActivityListBinding> implements IBleScanNotifyListener {

    // views
    private ImageView _refreshIv;
    private TextView _refreshLabel;

    ObjectAnimator _refreshAnimator;
    DeviceListFragment _fragment;


    private static final int REQUEST_READ_PHONE_STATE = 0x00000001;

    private static final int REQUEST_LOCATION = 0x00000002;
    private static final int REQUEST_PERMISSION_STORAGE = 0x00000003;

    private static final String[] PERMISSIONS_STORAGE = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE };

    public static void start(Context context) {
        if(null != context) {
            Intent intent = new Intent(context, SoftApDeviceListActivity.class);
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
        // find views
        viewBinding.titleBar.tvTitle.setText(getString(R.string.camera_video_transfer));
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });
        viewBinding.titleBar.ivRight.setImageResource(R.mipmap.home_refresh);

        _refreshLabel = viewBinding.titleBar.tvRight;
        viewBinding.titleBar.tvRight.setVisibility(View.VISIBLE);
        _refreshIv = viewBinding.titleBar.ivRight;
        _refreshIv.setVisibility(View.VISIBLE);

        _refreshAnimator = ObjectAnimator.ofFloat(_refreshIv, "rotation", 1080f);
        _refreshAnimator.setDuration(2000L);
        _refreshAnimator.setRepeatCount(-1);

//        _refreshLabel.setVisibility(View.GONE);
        _refreshLabel.setText(getString(R.string.loading_dialog_refresh));

        _refreshLabel.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                boolean hasLocationSer = PermissionsUtil.isLocServiceEnable(SoftApDeviceListActivity.this);
                if(!hasLocationSer) {
                    PermissionsUtil.openLocationServiceForResult(SoftApDeviceListActivity.this, PermissionsUtil.REQUEST_LOCATION_SERVICE);
                    return true;
                }

                if(PermissionsUtil.hasBlePermission(SoftApDeviceListActivity.this)) {
                    boolean refreshing = _fragment.onRefresh();
                } else {
                    requestBluetoothPermission();
                }
                return true;
            }
        });

        _fragment = DeviceListFragment.newInstance(DeviceListFragment.type_soft_ap);

        getSupportFragmentManager().beginTransaction()
            .add(viewBinding.content.getId(), _fragment)
            .commit();

        requestPermission();
    }

    @Override
    protected void initData() {

    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if(_refreshAnimator != null) {
            _refreshAnimator = null;
        }

        BluetoothHandle.removeScanNotifyListener(this);
    }


    /*动态申请权限操作*/
    private boolean isPermissionRequested = false;

    public void requestPermission() {
        if(Build.VERSION.SDK_INT >= 23 && !isPermissionRequested) {
            isPermissionRequested = true;
            ArrayList<String> permissionsList = new ArrayList<>();
            String[] permissions = {//在这里加入你要使用的权限
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            };

            for(String perm : permissions) {
                if(PackageManager.PERMISSION_GRANTED != checkSelfPermission(perm)) {
                    permissionsList.add(perm);
                    // 进入这里代表没有权限.
                }
            }

            if(!permissionsList.isEmpty()) {
                String[] strings = new String[permissionsList.size()];
                requestPermissions(permissionsList.toArray(strings), REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onScanning(boolean isBackground) {
        if(_refreshIv != null)
            _refreshIv.setVisibility(View.VISIBLE);

        _refreshAnimator.start();

    }

    @Override
    public void onLeScan(Device device) {

    }

    @Override
    public void onTimeout() {
        if(_refreshIv != null) {
            _refreshIv.setVisibility(View.GONE);
        }
        _refreshAnimator.end();
    }

    @Override
    public void onScanEnd() {
        if(_refreshIv != null) {
            _refreshIv.setVisibility(View.GONE);
        }
        _refreshAnimator.end();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
            case REQUEST_READ_PHONE_STATE:
                if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_STORAGE);
                    } else {

                        //TODO
                        new Handler().postDelayed(() -> {
                            if(_fragment != null) {
                                _fragment.onRefresh();
                            }
                        }, 2000);
                    }
                }
                break;
            case REQUEST_PERMISSION_STORAGE:
                if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //TODO
                    new Handler().post(() -> {
                        if(_fragment != null) {
                            _fragment.onRefresh();
                        }
                    });
                }
                break;
            case REQUEST_LOCATION:
                if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //TODO
                    new Handler().post(() -> {
                        if(_fragment != null) {
                            _fragment.onRefresh();
                        }
                    });
                }
                break;
            case PermissionsUtil.REQUEST_LOCATION_SERVICE:
                boolean hasLocationSer = PermissionsUtil.isLocServiceEnable(SoftApDeviceListActivity.this);
                if(hasLocationSer) {
                    //TODO
                    new Handler().post(() -> {
                        if(_fragment != null) {
                            _fragment.onRefresh();
                        }
                    });
                }
                break;
        }
    }
}
