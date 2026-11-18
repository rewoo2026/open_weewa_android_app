package com.xh.hotme.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatCheckBox;

public class CheckBoxWithoutText extends AppCompatCheckBox {
    public CheckBoxWithoutText(Context context) {
        super(context);
    }

    public CheckBoxWithoutText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return getCompoundPaddingLeft() + getCompoundPaddingRight();
    }
}