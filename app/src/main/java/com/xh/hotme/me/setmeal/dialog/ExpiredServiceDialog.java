package com.xh.hotme.me.setmeal.dialog;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.databinding.DialogExpiredServiceBinding;
import com.xh.hotme.me.setmeal.SetMealListActivity;
import com.xh.hotme.utils.ClickGuard;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/15
 * @Description: 基础服务  PRO服务  热我大礼包
 */
public class ExpiredServiceDialog extends BaseDialog<DialogExpiredServiceBinding> {
    public void setText(String title, String content1, String content2, String content3
        , String buyText) {
        viewBinding.remainingExtra.setVisibility(View.GONE);
        viewBinding.tvService.setText(title);
        viewBinding.tvContent1Right.setText(content1);
        viewBinding.tvContent2Right.setText(content2);
        viewBinding.tvContent3Right.setText(content3);
        viewBinding.tvBuy.setText(buyText);
    }

    public void setText(String title, String content1, String content2, String content3, String bigDataCount
        , String buyText) {
        viewBinding.tvService.setText(title);
        viewBinding.tvContent1Right.setText(content1);
        viewBinding.tvContent2Right.setText(content2);
        viewBinding.tvContent3Right.setText(content3);
        viewBinding.tvContent4Right.setText(bigDataCount);
        viewBinding.remainingExtra.setVisibility(View.VISIBLE);
        viewBinding.tvContent4Right.setVisibility(View.VISIBLE);
        viewBinding.tvContent4.setVisibility(View.VISIBLE);
        viewBinding.tvBuy.setText(buyText);
    }

    public ExpiredServiceDialog(@NonNull final Context context) {
        super(context, R.style.hotme_custom_dialog);
    }

    @Override
    protected void onInit() {
        viewBinding.tvBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                dismiss();
                return false;
            }
        });

        viewBinding.tvBuy.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                dismiss();

                Intent intent = new Intent(getContext(), SetMealListActivity.class);
                if(viewBinding.tvBuy.getText().toString().equals(getContext().getString(R.string.upgrade))) {
                    intent.putExtra("type", SetMealListActivity.TYPE_UPDATE);
                } else if(viewBinding.tvBuy.getText().toString().equals(getContext().getString(R.string.vip_renew))) {
                    intent.putExtra("type", SetMealListActivity.TYPE_RENEW);
                } else {
                    intent.putExtra("type", SetMealListActivity.TYPE_REWO);
                }
                getContext().startActivity(intent);
                return false;
            }
        });
    }
}
