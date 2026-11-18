package com.xh.hotme.me.setting;

import static com.xh.hotme.utils.DataCleanManager.getTotalCacheSize;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.core.app.ActivityCompat;

import com.umeng.socialize.UMShareAPI;
import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.MobileLoginActivity;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.bean.VersionResultBean;
import com.xh.hotme.databinding.ActivitySettingsBinding;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.me.profile.ProfileActivity;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.BaseAppUtil;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DataCleanManager;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.ToastUtil;
import com.xh.hotme.widget.ModalDialog;
import com.xh.hotme.widget.VersionDialog;

import pub.devrel.easypermissions.EasyPermissions;

public class SettingActivity extends BaseActivity<ActivitySettingsBinding> {
    private boolean hasNewVersion = false;
    private boolean isVersionChecked = false;
    private boolean isRequest = false;

    VersionResultBean _versionBean;

    private static final int REQUEST_CODE_WRITE_PERMISSION = 2003;
    VersionDialog mVersionDialog;

    public static void start(Context context) {
        if(null != context) {
            Intent intent = new Intent(context, SettingActivity.class);
            context.startActivity(intent);
        }
    }

    @Override
    protected void initView() {
        getCacheSize();
        boolean canEditEnvironment = com.xh.hotme.environment.RuntimeEnvironment.canEdit();
        viewBinding.runtimeEnvironment.setVisibility(canEditEnvironment ? View.VISIBLE : View.GONE);
        if (canEditEnvironment) {
            viewBinding.runtimeEnvironment.setOnClickListener(v ->
                    com.xh.hotme.environment.RuntimeEnvironmentActivity.start(this, false));
        }

        viewBinding.verNum.setText(BaseAppUtil.getAppVersionName());


        viewBinding.aboutMe.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                ContactUsActivity.start(SettingActivity.this);
                return true;
            }
        });

        // clear cache
        viewBinding.clearCache.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                showConfirmDialog("", getString(R.string.cache_delete_title), getString(R.string.cache_delete), getString(R.string.cache_delete_cancel),  new ClickGuard.GuardedOnClickListener() {
                    @Override
                    public boolean onClicked() {
                        DataCleanManager.clearCache(SettingActivity.this);
                        viewBinding.cacheNum.setText("0KB");
                        return true;
                    }
                }, null);
                return true;
            }
        });

        // privacy click
        viewBinding.agreementView.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                SimpleWebActivity.start(SettingActivity.this, getString(R.string.user_proxy), SdkApi.user_agreement);
                return true;
            }
        });

        // privacy click
        viewBinding.privacy.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                SimpleWebActivity.start(SettingActivity.this, getString(R.string.user_agreement), SdkApi.user_agreement);
                return true;
            }
        });

        //版本更新
        viewBinding.verUpdate.setOnClickListener(v -> {
            if(isVersionChecked) {
                if(hasNewVersion && _versionBean != null) {
                    if(EasyPermissions.hasPermissions(SettingActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        showVersionDialog(_versionBean);
                    } else {
                        ActivityCompat.requestPermissions(SettingActivity.this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE }, REQUEST_CODE_WRITE_PERMISSION);
                    }
                } else {
                    ToastUtil.s(getString(R.string.current_version_is_latest));
                }
            } else {
                getVersion(false);
            }
        });

        // back click
        viewBinding.titlebar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        if(!LoginManager.isSignedIn()) {
            viewBinding.deleteAccount.setVisibility(View.GONE);
        }

        viewBinding.deleteAccount.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                DeleteUserActivity.start(SettingActivity.this);
                return true;
            }
        });

        viewBinding.profile.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(LoginManager.isSignedIn()) {
                    ProfileActivity.start(SettingActivity.this);
                } else {
                    MobileLoginActivity.start(SettingActivity.this);
                }
                return true;
            }
        });


        // title
        viewBinding.titlebar.tvTitle.setText(getString(R.string.ly_me_setting));

        getVersion(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewBinding.environmentName.setText(com.xh.hotme.environment.RuntimeEnvironment.label(
                com.xh.hotme.environment.RuntimeEnvironment.get().mode));
    }

    @Override
    protected void initData() {

    }

    public void getCacheSize() {
        try {
            String cache = getTotalCacheSize(SettingActivity.this);
            viewBinding.cacheNum.setText(cache);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void getVersion(boolean isBackground) {

        if(isRequest) {
            return;
        }

        isRequest = true;

        String versionUrl = SdkApi.getAppVersion(Constants.getAppType(), BaseAppUtil.getAppVersionCode());
        OkHttpUtil.get(versionUrl, new OkHttpCallbackDecode<VersionResultBean>() {
            @Override
            public void onDataSuccess(VersionResultBean data) {
                try {
                    isRequest = false;
                    isVersionChecked = true;
                    if(data != null) {
                        _versionBean = data;
                        AppFileUtil.saveJson(GsonUtils.GsonString(data), AppFileUtil.VERSION_FILE);
                        int curCode = BaseAppUtil.getAppVersionCode();
                        int latestCode = Integer.parseInt(data.getVersionCode());
                        hasNewVersion = latestCode > curCode;

                        setVersionStatus(hasNewVersion);

                        if(!isBackground) {
                            showVersionDialog(data);
                        }
                    }

                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(String code, String msg) {
                AppTrace.d(TAG, "error: code=" + code + ", msg=" + msg);
                isRequest = false;
            }
        });
    }

    private void setVersionStatus(boolean isNew) {
        MainHandler.runOnUiThread(() -> {
            if(isNew) {
                viewBinding.verNum.setText(getString(R.string.find_new_version));
                viewBinding.verNum.setTextColor(getResources().getColor(R.color.orange));
            } else {
                viewBinding.verNum.setText(BaseAppUtil.getAppVersionName());
            }
        });
    }


    private void showVersionDialog(final VersionResultBean version) {

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if(mVersionDialog != null) {
                    mVersionDialog.dismiss();
                    mVersionDialog = null;
                }
                mVersionDialog = new VersionDialog();

                //强制更新
                mVersionDialog.showDialog(SettingActivity.this, version, new VersionDialog.ConfirmDialogListener() {
                    @Override
                    public void ok() {

                    }

                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void dismiss() {

                    }
                });
            } catch(Throwable e) {
                e.printStackTrace();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for this
        super.onActivityResult(requestCode, resultCode, data);
        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CODE_WRITE_PERMISSION) {

            String versionContent = AppFileUtil.loadStringFromFile(AppFileUtil.VERSION_FILE);
            if(!TextUtils.isEmpty(versionContent)) {
                VersionResultBean versionResultBean = GsonUtils.GsonToBean(versionContent, VersionResultBean.class);
                if(versionResultBean != null) {
                    int curCode = BaseAppUtil.getAppVersionCode();
                    int latestCode = Integer.parseInt(versionResultBean.getVersionCode());
                    if(latestCode > curCode) {
                        if(EasyPermissions.hasPermissions(SettingActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            showVersionDialog(versionResultBean);
                        }
                    }
                }
            }
        }
    }
}
