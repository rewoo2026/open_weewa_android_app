package com.xh.hotme;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;

import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.FullscreenPortrait;
import com.xh.hotme.databinding.ActivitySplashBinding;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.main.MainActivity;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.NetUtil;
import com.xh.hotme.utils.PermissionsUtil;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.StatusBarUtil;
import com.xh.hotme.utils.TimeUtil;
import com.xh.hotme.utils.UMengUtil;
import com.xh.hotme.widget.ModalDialog;
import com.xh.hotme.widget.PrivacyWebDialog;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

@FullscreenPortrait(immersive = false)
public class SplashActivity extends BaseActivity<ActivitySplashBinding> implements EasyPermissions.PermissionCallbacks {

    private final static String TAG = SplashActivity.class.getSimpleName();

    private static final String BUNDLE_FRAGMENTS_KEY = "android:support:fragments";

    // messages
    private final int START_MAIN = 0;

    // request code
    private static final int REQUEST_CHECK_PERMISSION = 100;

    boolean isFirstLaunch = false;

    boolean isShowGuide = false;
    boolean isGuideShowEnd = false;

    private boolean isAgreePrivacy = false;
    private boolean isAgreePermission = false;

    PrivacyWebDialog privacyWebDialog;

    private boolean showModelSwitch = false;

    private final Handler _mainHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(android.os.Message msg) {
            if(msg.what == START_MAIN) {
                if(SplashActivity.this.isDestroyed() || SplashActivity.this.isFinishing()) {
                    return;
                }

                startMain();

                //如果不支持模式切换的版本直接进入首页
                if(!HotmeApplication.showModelSwitch) {
                    MainActivity.start(SplashActivity.this);
                    finish();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            //重建时清除 fragment的状态
            savedInstanceState.remove(BUNDLE_FRAGMENTS_KEY);
        }
        super.onCreate(savedInstanceState);

        if(!SharedPrefs.getImpl().getPrivateShowStatus()) {
            showPrivacy();
        } else {
            afterPrivacy();
        }
    }

    @Override
    protected int getStatusBarColor() {
        return 0;
    }

    @Override
    public void initView() {
        viewBinding.guideVp.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(isGuideShowEnd && isShowGuide) {
                    startMain();
                }
                return true;
            }
        });

        isFirstLaunch = SharedPrefs.getImpl().isFirstLaunch();
        isShowGuide = true;
        isGuideShowEnd = true;
        viewBinding.splashLayout.setVisibility(View.VISIBLE);
        viewBinding.guideView.setVisibility(View.GONE);

        StatusBarUtil.setStatusBarTranslucent(this, true);

        long startTimes = SharedPrefs.getImpl().getLong(SharedPrefs.PRFS_LOBOX_INIT_TIMES);
        startTimes += 1;


        SharedPrefs.getImpl().putLong(SharedPrefs.PRFS_LOBOX_INIT_TIMES, startTimes);

        long dateTimeStamp = SharedPrefs.getImpl().getLong(SharedPrefs.PRFS_DAY_LAUNCH_DATE);
        if(dateTimeStamp == 0) {
            SharedPrefs.getImpl().putLong(SharedPrefs.PRFS_DAY_INSTALL_DATE, System.currentTimeMillis());
            SharedPrefs.getImpl().putLong(SharedPrefs.PRFS_DAY_LAUNCH_DATE, System.currentTimeMillis());
            SharedPrefs.getImpl().putLong(SharedPrefs.PRFS_DAY_LAUNCH_TIME, 1);
        } else {
            long currentTimeStamp = System.currentTimeMillis();
            if(!TimeUtil.isSameDay(dateTimeStamp, currentTimeStamp)) {
                SharedPrefs.getImpl().putLong(SharedPrefs.PRFS_DAY_LAUNCH_DATE, System.currentTimeMillis());
                SharedPrefs.getImpl().putLong(SharedPrefs.PRFS_DAY_LAUNCH_TIME, 1);
            } else {
                long loginTimes = SharedPrefs.getImpl().getLong(SharedPrefs.PRFS_DAY_LAUNCH_TIME);
                SharedPrefs.getImpl().putLong(SharedPrefs.PRFS_DAY_LAUNCH_TIME, loginTimes + 1);
            }
        }

        viewBinding.btnLocal.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(isFirstLaunch) {
                    if(isShowGuide && isGuideShowEnd) {
                        Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_LOCAL;
                        gotoMain();
                    }
                } else {
                    Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_LOCAL;
                    gotoMain();
                }

                return true;
            }
        });

        viewBinding.btnNetwork.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(!NetUtil.isConnectedAvailableNetwork()){
                    ModalDialog dialog = new ModalDialog(SplashActivity.this);
                    dialog.setTitle(getString(R.string.remote_model_no_network_title));
                    dialog.setMessage(getString(R.string.remote_model_no_network_message));
                    dialog.hideNegativeButton();
                    dialog.setPositiveButton(getString(R.string.ok), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                    return true;
                }

                if(isFirstLaunch) {
                    if(isShowGuide && isGuideShowEnd) {
                        Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_REMOTE;
                        gotoMain();
                    }
                } else {
                    Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_REMOTE;
                    gotoMain();
                }

                return true;
            }
        });

        if(!HotmeApplication.showModelSwitch){
            viewBinding.btnLocal.setVisibility(View.GONE);
            viewBinding.btnNetwork.setVisibility(View.GONE);
        }

    }

    @Override
    public void onResume(){
        super.onResume();
        AppTrace.d(TAG, "onResume");
    }

    @Override
    public void onPause(){
        super.onPause();
        AppTrace.d(TAG, "onPause");
    }

    private void gotoMain(){
        MainActivity.start(SplashActivity.this);
        finish();
    }


    @Override
    public void initData() {

    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if(privacyWebDialog!=null && privacyWebDialog.isShowing()){
            privacyWebDialog.dismiss();
        }
        privacyWebDialog = null;
    }


    private void showPrivacy() {
        if(privacyWebDialog!=null && privacyWebDialog.isShowing()){
            return;
        }
        privacyWebDialog = new PrivacyWebDialog(SplashActivity.this, String.format("%s%s", getString(R.string.welcome_to_use), getString(R.string.weewa_camera)), SdkApi.user_privacy);
        privacyWebDialog.setOnClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    SharedPrefs.getImpl().setPrivateShowStatus(SplashActivity.this, true);

                    isAgreePrivacy = true;

                    if (SharedPrefs.getImpl().getPermissionShowStatus()) {
                        isAgreePermission = true;

                        // init permission
                        afterPermissionCheck();

                    } else {
                        showPermissionDialog();
                    }


                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                   
                    ModalDialog dialog = new ModalDialog(SplashActivity.this);
                    dialog.setMessage(getString(R.string.privacy_agreement_dialog_title));
                    dialog.setNegativeButton(getString(R.string.privacy_agreement_dialog_btn_exit_app), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (privacyWebDialog != null) {
                                privacyWebDialog.dismiss();
                            }
                            finish();
                        }
                    });
                    dialog.setPositiveButton(getString(R.string.privacy_agreement_dialog_btn_back), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        }
                    });
                    dialog.setMessageTextColor("#666666");
                    dialog.setMessageTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                    dialog.setLeftButtonTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                    dialog.setRightButtonTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                    dialog.setLeftButtonTextColor("#999999");
                    dialog.setRightButtonTextColor("#ffffff");
                    dialog.show();
                }
            }
        });
        privacyWebDialog.setNegativeButtonText(getString(R.string.privacy_agreement_dialog_btn_exit_app));
        privacyWebDialog.setPositiveButtonTitle(getString(R.string.privacy_agreement_dialog_btn_agree));
        privacyWebDialog.show();

    }

    private void afterPrivacy() {
        if(SharedPrefs.getImpl().getPermissionShowStatus()) {
            //umeng初始化
            UMengUtil.init(HotmeApplication.getContext());

            afterPermissionCheck();
        } else {
            showPermissionDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);

        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onBluetoothMayBeOpened() {
        boolean hasPermission = PermissionsUtil.hasBlePermission(SplashActivity.this);
        if(hasPermission) {
            startMain();
        } else {
            showPermissionConfirmDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        AppTrace.d(TAG, "onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //将请求结果传递EasyPermission库处理
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(REQUEST_CHECK_PERMISSION)
    void afterPermissionCheck() {
        AppTrace.d(TAG, "afterPermissionCheck");

        startMain();
    }

    private void startMain() {

        SharedPrefs.getImpl().setFirstLaunch(false);

        if(isFirstLaunch) {
            if(isShowGuide && isGuideShowEnd) {
                _mainHandler.sendEmptyMessageDelayed(START_MAIN, 2000);
            }
        } else {
            _mainHandler.sendEmptyMessageDelayed(START_MAIN, 2000);
        }
    }

    private void showPermissionDialog() {
        SharedPrefs.getImpl().setPermissionShowStatus(true);
        //umeng初始化
        UMengUtil.init(HotmeApplication.getContext());

        // init permission
        afterPermissionCheck();
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        AppTrace.d(TAG, "onPermissionsGranted");

        startMain();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        AppTrace.d(TAG, "onPermissionsDenied");

//        startMain(true);

        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            MainHandler.runOnUiThread(this::showPermissionConfirmDialog);
        } else {
            boolean hasPermission = PermissionsUtil.hasBlePermission(SplashActivity.this);
            if(hasPermission) {
                startMain();
            } else {
                showPermissionConfirmDialog();
            }
        }
    }

    private void showPermissionConfirmDialog() {
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
                    Uri uri = Uri.fromParts("package", SplashActivity.this.getPackageName(), null);
                    intent.setData(uri);
                    requestOpenBluetooth(intent);
                    return true;
                }
            },
            null
        );
    }
}
