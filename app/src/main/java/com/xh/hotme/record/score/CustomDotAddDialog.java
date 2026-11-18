package com.xh.hotme.record.score;

import android.content.Context;
import android.os.Handler;
import android.text.InputFilter;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.bluetooth.event.CompetitionLabelEvent;
import com.xh.hotme.databinding.DialogCustomDotAddBinding;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


/**
 * @Description:
 * @Author: Maclay
 * @Date: 2023/6/15 17:19
 */
public class CustomDotAddDialog extends BaseDialog<DialogCustomDotAddBinding> {
    public interface ICallback {
        void onCancel();

        void onConfirm(String tag);
    }

    private ICallback iCallback;

    private String _gameTag;

    Handler _handler;


    private final Runnable _timeOutRunnable = DialogUtil::dismissDialog;

    public CustomDotAddDialog(@NonNull Context context) {
        super(context);
    }

    public CustomDotAddDialog setCallback(ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }


    public CustomDotAddDialog setTag(String tag) {

        viewBinding.etGameTag.setText(tag == null ? "" : tag);
        return this;
    }

    public String getTag() {
        String tagStr = viewBinding.etGameTag.getText().toString();
        return tagStr;
    }

    public CustomDotAddDialog setCancel(String content) {
        viewBinding.tvCancel.setText(content);
        return this;
    }

    public CustomDotAddDialog setConfirm(String content) {
        viewBinding.tvConfirm.setText(content);
        return this;
    }

    @Override
    protected void onInit() {
        _handler = new Handler();
        viewBinding.etGameTag.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        viewBinding.tvConfirm.setOnClickListener(v -> {
            String tagStr = viewBinding.etGameTag.getText().toString();
            if(!TextUtils.isEmpty(tagStr)) {
                _gameTag = tagStr;

                if(iCallback != null) {
                    iCallback.onConfirm(_gameTag);
                }
            } else {
                ToastUtil.s(getContext().getString(R.string.game_tag_is_null));
            }

        });
        viewBinding.tvCancel.setOnClickListener(v -> {
            cancel();
            if(iCallback != null) {
                iCallback.onCancel();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetGameEvent(CompetitionLabelEvent event) {
        DialogUtil.dismissDialog();
        _handler.removeCallbacksAndMessages(null);
        if(event != null && event.state == 1) {
            cancel();
            if(iCallback != null) {
                iCallback.onConfirm(_gameTag);
            }
        } else {
            ToastUtil.s(getContext().getString(R.string.game_tag_fail));
        }
    }


    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }
}