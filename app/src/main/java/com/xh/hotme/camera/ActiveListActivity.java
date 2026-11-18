package com.xh.hotme.camera;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
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
import com.xh.hotme.bluetooth.event.LoginEvent;
import com.xh.hotme.bluetooth.event.WeewaActiveSuccessEvent;
import com.xh.hotme.bluetooth.listener.IBleScanNotifyListener;
import com.xh.hotme.databinding.ActiveActivityListBinding;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.PermissionsUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@IEventBus
public class ActiveListActivity extends BaseActivity<ActiveActivityListBinding> implements IBleScanNotifyListener {

    // views
    private ImageView _refreshIv;
    private TextView _refreshLabel;

    ObjectAnimator _refreshAnimator;
    DeviceListFragment _fragment;

    int type = DeviceListFragment.type_active;


    private static final String[] PERMISSIONS_STORAGE = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE };

    public static void start(Context context) {
        if(null != context) {
            Intent intent = new Intent(context, ActiveListActivity.class);
            intent.putExtra(Constants.VIDEO_CATEGORY, DeviceListFragment.type_active);
            context.startActivity(intent);
        }
    }

    public static void start(Context context, int type) {
        if(null != context) {
            Intent intent = new Intent(context, ActiveListActivity.class);
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
        // find views
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

                    boolean hasLocationSer = PermissionsUtil.isLocServiceEnable(ActiveListActivity.this);
                    if (!hasLocationSer) {
                        PermissionsUtil.openLocationServiceForResult(ActiveListActivity.this, PermissionsUtil.REQUEST_LOCATION_SERVICE);
                        return true;
                    }

                    if (!PermissionsUtil.hasBlePermission(ActiveListActivity.this)) {
                        requestBluetoothPermission();
                        return true;
                    } else {
                        boolean refreshing = _fragment.onRefresh();
                    }
                } else {
                    DeviceManager.getImpl().onRefreshList(false);
                }

                return true;
            }
        });

        _refreshIv.setVisibility(View.GONE);

        if(getIntent() != null) {
            type = getIntent().getIntExtra(Constants.VIDEO_CATEGORY, DeviceListFragment.type_active);
        }

        _fragment = DeviceListFragment.newInstance(type);

        getSupportFragmentManager().beginTransaction()
            .add(viewBinding.content.getId(), _fragment)
            .commit();

        if(Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            if (!PermissionsUtil.hasBlePermission(ActiveListActivity.this)) {
                requestBluetoothPermission();
            }
        }


        // 添加搜索功能
        viewBinding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String keyword = s.toString().trim();
                if (_fragment != null) {
                    _fragment.filterDevices(keyword);
                }
            }
        });

        // 创建自定义的 EditText 来处理清除按钮点击
        viewBinding.etSearch.setOnTouchListener(null);  // 移除原有的触摸监听器

        // 添加点击监听器
        viewBinding.etSearch.setOnClickListener(v -> {
            // 检查点击位置是否在清除按钮上
            if (v instanceof EditText) {
                EditText editText = (EditText) v;
                if (editText.getCompoundDrawables()[2] != null) {
                    editText.setText("");
                }
            }
        });    }

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
                }
                break;
            case PermissionsUtil.REQUEST_LOCATION_SERVICE:
                boolean hasLocationSer = PermissionsUtil.isLocServiceEnable(ActiveListActivity.this);
                if(hasLocationSer) {
                    //TODO
                    new Handler().post(() -> onRefreshFragment());
                }
                break;
            case PermissionsUtil.REQUEST_PERMISSION:
            case PermissionsUtil.REQUEST_PERMISSION_LOCATION:
            case PermissionsUtil.REQUEST_PERMISSION_BLUE:
                //TODO
                new Handler().post(() -> onRefreshFragment());

                break;

        }
    }

    private boolean onRefreshFragment() {
        boolean hasLocationSer = PermissionsUtil.isLocServiceEnable(ActiveListActivity.this);
        if(!hasLocationSer) {
            PermissionsUtil.openLocationService(ActiveListActivity.this);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActiveSuccessEvent(WeewaActiveSuccessEvent event) {
        AppTrace.i(TAG, "onActiveSuccessEvent");
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoginEvent(LoginEvent event) {
        AppTrace.i(TAG, "onLoginEvent");
        finish();
    }
}
