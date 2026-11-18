package com.xh.hotme.storage;

import android.content.Intent;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.databinding.ActivityConfirmdownloadBinding;
import com.xh.hotme.http.ConnectLogic;
import com.xh.hotme.utils.ToastUtil;


/**
 * date   : 2023/6/21
 * desc   :下载前提示页
 */
public class ConfirmDownloadActivity extends BaseActivity<ActivityConfirmdownloadBinding> {
    @Override
    protected void initView() {
        ConnectLogic.getImpl();
        viewBinding.ivClose.setOnClickListener(v -> finish());
        viewBinding.tvConfirm.setOnClickListener(v -> {

            if(!ConnectLogic.getImpl().isConnected()) {
                ToastUtil.s(getString(R.string.file_service_is_disconnect));
                return;
            }

            if(!ConnectLogic.getImpl().toReceive()) {
                ToastUtil.s(ConnectLogic.getImpl().getTransferError());
                return;
            }
            startActivity(new Intent(ConfirmDownloadActivity.this, TransferFilesActivity.class));
            finish();
        });
    }

    @Override
    protected void initData() {

    }
}
