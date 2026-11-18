package com.xh.hotme.record.score;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.bluetooth.event.CompetitionLabelEvent;
import com.xh.hotme.databinding.DialogCustomDotEditBinding;
import com.xh.hotme.record.bean.DotStates;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.DensityUtil;
import com.xh.hotme.utils.DeviceUtil;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.ScoreCenterUtil;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;


/**
 * @Description:
 * @Author: Maclay
 * @Date: 2023/6/15 17:19
 */
public class CustomDotEditDialog extends BaseDialog<DialogCustomDotEditBinding> {
    public interface ICallback {
        void onCancel();

        void onConfirm();
    }

    private ICallback iCallback;

    private String _competitionName;


    LinearLayout llCustomDotView;

    Handler _handler;

    List<DotStates> _dataList;


    View.OnClickListener removeListener;

    private final Runnable _timeOutRunnable = DialogUtil::dismissDialog;

    public CustomDotEditDialog(@NonNull Context context) {
        super(context);
    }

    public CustomDotEditDialog setCallback(ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }


    public CustomDotEditDialog setCancel(String content) {
        viewBinding.tvCancel.setText(content);
        return this;
    }

    public CustomDotEditDialog setConfirm(String content) {
        viewBinding.tvConfirm.setText(content);
        return this;
    }

    @Override
    protected void onInit() {
        _handler = new Handler();
        viewBinding.tvConfirm.setOnClickListener(v -> {
            if (iCallback != null) {
                iCallback.onConfirm();
            }
        });
        viewBinding.tvCancel.setOnClickListener(v -> {
            cancel();
            if (iCallback != null) {
                iCallback.onCancel();
            }
        });

        removeListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView dotView = (TextView) view;
                DotStates dotState = (DotStates) dotView.getTag();
                if (dotState != null) {
                    removeDotView(dotState);

                    //更新到本地文件
                    ScoreCenterUtil.saveCustomDotRecord(_dataList);
                }
            }
        };
        if (_dataList == null) {
            _dataList = new ArrayList<>();
        }

        llCustomDotView = viewBinding.llDotList;
        String content = ScoreCenterUtil.loadCustomDotRecord();
        if (!TextUtils.isEmpty(content)) {
            List<DotStates> customList = GsonUtils.fromJsonList(content, DotStates.class);
            _dataList.addAll(customList);
            for (int i = 0; i < _dataList.size(); i++) {
                DotStates dotState = _dataList.get(i);
                View view = LayoutInflater.from(getContext()).inflate(R.layout.score_center_custom_dot_item, null);
                TextView contentView = view.findViewById(R.id.tvContent);
                contentView.setText(dotState.getName());
                contentView.setTag(dotState);
                contentView.setOnClickListener(removeListener);
                if (i != 0) {
                    ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.leftMargin = DensityUtil.dip2px(10);
                    view.setLayoutParams(params);
                }

                llCustomDotView.addView(view);
            }
        }

        Window window = getWindow();
        WindowManager.LayoutParams windowparams = window.getAttributes();
        windowparams.width = Math.max(DeviceUtil.getWidth() / 2, 1080);
        window.setAttributes(windowparams);

    }

    private void removeDotView(DotStates dotStates) {
        for (int i = 0; i < _dataList.size(); i++) {
            if (_dataList.get(i).getId() == dotStates.getId()) {
                llCustomDotView.removeViewAt(i);
                _dataList.remove(i);
                break;
            }
        }
    }
}