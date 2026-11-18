package com.xh.hotme.me.camsetting;

import android.annotation.SuppressLint;
import android.view.View;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.databinding.ActivityConfigLivePasswordBinding;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.SharedPrefs;
import com.xh.hotme.utils.ToastUtil;

public class ConfigLivePasswordActivity extends BaseActivity<ActivityConfigLivePasswordBinding> {
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initView() {
        viewBinding.titleBar.tvTitle.setText(R.string.live_password);
        viewBinding.titleBar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        viewBinding.btnLivePassword.setOnClickListener(v -> toggleEnableLivePassword());
        viewBinding.sbLivePassword.setOnClickListener(v -> toggleEnableLivePassword());
        viewBinding.sbLivePassword.setChecked(SharedPrefs.getImpl().isLivePasswordEnabled());
        viewBinding.livePasswordContainer.setVisibility(SharedPrefs.getImpl().isLivePasswordEnabled() ? View.VISIBLE : View.GONE);
        viewBinding.etLivePassword.setText(SharedPrefs.getImpl().getLivePassword());
        viewBinding.btnOk.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                String pwd = viewBinding.etLivePassword.getText().toString();
                if(pwd.isEmpty()) {
                    ToastUtil.s(getString(R.string.info_live_password_is_empty));
                    return false;
                }
                SharedPrefs.getImpl().setLivePassword(pwd);
                ToastUtil.s(getString(R.string.setup_successful));
                finish();
                return true;
            }
        });
    }

    @Override
    protected void initData() {
    }

    private void toggleEnableLivePassword() {
        boolean enabled = SharedPrefs.getImpl().isLivePasswordEnabled();
        enabled = !enabled;
        SharedPrefs.getImpl().setEnableLivePassword(enabled);
        viewBinding.sbLivePassword.setChecked(enabled);
        viewBinding.livePasswordContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }
}
