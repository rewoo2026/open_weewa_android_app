package com.xh.hotme.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Keep;
import androidx.core.content.ContextCompat;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.databinding.HotmeDialogModalBinding;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ColorUtil;

/**
 * 自定义白色对话框，由api调用的模态对话框
 */
@Keep
public class ModalDialog extends BaseDialog<HotmeDialogModalBinding> {

    private static final String TAG = "ModalDialog";

    private View.OnClickListener mLeftBtnClickListener;
    private View.OnClickListener mRightBtnClickListener;

    public ModalDialog(Context context) {
        super(context, R.style.hotme_modal_dialog);
    }

    @Override
    protected void onInit() {
        setCancelable(false);

        viewBinding.dlgLeftBtn.setOnClickListener(v -> {
            if(mLeftBtnClickListener != null) {
                mLeftBtnClickListener.onClick(v);
            }
            dismiss();
        });
        viewBinding.dlgRightBtn.setOnClickListener(v -> {
            if(mRightBtnClickListener != null) {
                mRightBtnClickListener.onClick(v);
            }
            dismiss();
        });
        setTitle("");
    }

    /**
     * 设置标题栏图标
     */
    public void setTitleIcon(int drawableId) {
        Drawable drawable = getContext().getResources().getDrawable(drawableId);
        setTitleIcon(drawable);
    }

    /**
     * 设置标题栏图标
     */
    public void setTitleIcon(Drawable drawable) {
        if(drawable == null) {
            return;
        }
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        viewBinding.dlgTitle.setCompoundDrawables(drawable, null, null, null);
        viewBinding.dlgTitleView.setVisibility(View.VISIBLE);
    }

    /**
     * 设置对话框标题
     */
    public void setTitle(int strId) {
        setTitle(getContext().getString(strId));
    }

    /**
     * 设置对话框标题
     */
    public void setTitle(String title) {
        if(TextUtils.isEmpty(title)) {
            viewBinding.dlgTitleView.setVisibility(View.GONE);
            return;
        }
        viewBinding.dlgTitle.setText(title);
        viewBinding.dlgTitleView.setVisibility(View.VISIBLE);
        viewBinding.dlgMsg.setTextColor(ContextCompat.getColor(getContext(), R.color.color_909090));
    }

    /**
     * 设置对话框内容
     */
    public void setMessage(int strId) {
        setMessage(getContext().getString(strId));
    }

    /**
     * 设置对话框内容
     */
    public void setMessage(String msg) {
        viewBinding.dlgMsg.setText(msg);
    }


    /**
     * 设置对话框内容
     */
    public void hideMessage() {
        viewBinding.dlgMsg.setVisibility(View.GONE);
    }

    /**
     * 设置对话框内容文字颜色
     *
     * @param color 颜色值
     */
    public void setMessageTextColor(String color) {
        try {
            viewBinding.dlgMsg.setTextColor(ColorUtil.parseColor(color));
        } catch(Exception e) {
            AppTrace.e(TAG, String.format("setMessageTextColor(%s) parse color error", color));
        }
    }

    /**
     * 设置对话框内容文字大小
     *
     * @param size 颜色值
     */
    public void setMessageTextSize(int unit, float size) {
        try {
            viewBinding.dlgMsg.setTextSize(unit, size);
        } catch(Exception e) {
            AppTrace.e(TAG, String.format("setMessageTextSize(%s) parse size ", size));
        }
    }

    /**
     * 设置左侧按钮文字大小
     *
     * @param size 大小
     */
    public void setLeftButtonTextSize(int unit, float size) {
        try {
            viewBinding.dlgLeftBtn.setTextSize(unit, size);
        } catch(Exception e) {
            AppTrace.e(TAG, String.format("setLeftButtonTextSize(%s) parse size ", size));
        }
    }

    /**
     * 设置右侧按钮文字大小
     *
     * @param size 大小
     */
    public void setRightButtonTextSize(int unit, float size) {
        try {
            viewBinding.dlgRightBtn.setTextSize(unit, size);
        } catch(Exception e) {
            AppTrace.e(TAG, String.format("setRightButtonTextSize(%s) parse size ", size));
        }
    }


    /**
     * 设置左侧按钮文字颜色
     *
     * @param color 颜色值
     */
    public void setLeftButtonTextColor(String color) {
        try {
            viewBinding.dlgLeftBtn.setTextColor(ColorUtil.parseColor(color));
        } catch(Exception e) {
            AppTrace.e(TAG, String.format("setLeftButtonTextColor(%s) parse color error", color));
        }
    }

    /**
     * 设置右侧按钮文字颜色
     *
     * @param color 颜色值
     */
    public void setRightButtonTextColor(String color) {
        try {
            viewBinding.dlgRightBtn.setTextColor(ColorUtil.parseColor(color));
        } catch(Exception e) {
            AppTrace.e(TAG, String.format("setRightButtonTextColor(%s) parse color error", color));
        }
    }

    /**
     * 设置左边按钮文字和点击监听
     */
    public void setNegativeButton(int strId, View.OnClickListener listener) {
        setNegativeButton(getContext().getString(strId), listener);
    }

    /**
     * 设置左边按钮文字和点击监听
     */
    public void setNegativeButton(String text, View.OnClickListener listener) {
        viewBinding.dlgBtnView.setVisibility(View.VISIBLE);
        if(viewBinding.dlgRightBtn.getVisibility() == View.VISIBLE) {
            viewBinding.splitBar.setVisibility(View.VISIBLE);
        } else {
            viewBinding.splitBar.setVisibility(View.GONE);
        }
        viewBinding.dlgLeftBtn.setText(text);
        viewBinding.dlgLeftBtn.setVisibility(View.VISIBLE);
        mLeftBtnClickListener = listener;
    }

    public void hideNegativeButton() {
        viewBinding.dlgBtnView.setVisibility(View.VISIBLE);
        viewBinding.dlgRightBtn.setVisibility(View.VISIBLE);
        viewBinding.dlgLeftBtn.setVisibility(View.GONE);
        viewBinding.splitBar.setVisibility(View.GONE);
    }

    /**
     * 设置右边按钮文字和点击监听
     */
    public void setPositiveButton(int strId, View.OnClickListener listener) {
        setPositiveButton(getContext().getString(strId), listener);
    }

    /**
     * 设置右边按钮文字和监听
     */
    public void setPositiveButton(String text, View.OnClickListener listener) {
        viewBinding.dlgBtnView.setVisibility(View.VISIBLE);
        if(viewBinding.dlgLeftBtn.getVisibility() == View.VISIBLE) {
            viewBinding.splitBar.setVisibility(View.VISIBLE);
        } else {
            viewBinding.splitBar.setVisibility(View.GONE);
        }
        viewBinding.dlgRightBtn.setText(text);
        viewBinding.dlgRightBtn.setVisibility(View.VISIBLE);
        mRightBtnClickListener = listener;
    }
}
