package com.xh.hotme.me.setmeal.dialog;

import android.animation.ObjectAnimator;
import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.base.SheetStyle;
import com.xh.hotme.databinding.DialogStartPayBinding;


@Keep
@SheetStyle
public class StartPayDialog extends BaseDialog<DialogStartPayBinding> {
    private ObjectAnimator _refreshAnimator;

    public StartPayDialog(@NonNull final Context context) {
        super(context, R.style.hotme_custom_dialog);
    }

    @Override
    public void show() {
        super.show();
        _refreshAnimator.start();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        _refreshAnimator.cancel();
    }

    @Override
    protected void onInit() {
        _refreshAnimator = ObjectAnimator.ofFloat(viewBinding.ivLoading, "rotation", 1080f);
        _refreshAnimator.setDuration(2000L);
        _refreshAnimator.setRepeatCount(-1);
    }
}
