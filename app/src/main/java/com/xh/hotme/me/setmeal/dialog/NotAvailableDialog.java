package com.xh.hotme.me.setmeal.dialog;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.databinding.DialogNotAvailableBinding;
import com.xh.hotme.me.setmeal.SetMealListActivity;
import com.xh.hotme.utils.ClickGuard;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/15
 * @Description: 套餐不可用
 */
public class NotAvailableDialog extends BaseDialog<DialogNotAvailableBinding> {
    public void setText(Context context, String str, int type) {
        viewBinding.tvBuy.setText(str);
        viewBinding.tvBuy.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                dismiss();

                Intent intent = new Intent(context, SetMealListActivity.class);
                intent.putExtra("type", type);
                context.startActivity(intent);
                return true;
            }
        });
    }

    public NotAvailableDialog(@NonNull final Context context) {
        super(context, R.style.hotme_custom_dialog);
    }

    @Override
    protected void onInit() {
        viewBinding.tvBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                dismiss();
                return true;
            }
        });
    }
}
