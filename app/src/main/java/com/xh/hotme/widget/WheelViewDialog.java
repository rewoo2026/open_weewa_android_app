package com.xh.hotme.widget;


import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.databinding.DialogWheelViewSingleBinding;
import com.xh.hotme.utils.ClickGuard;

import java.util.ArrayList;
import java.util.List;


@Keep
public class WheelViewDialog extends BaseDialog<DialogWheelViewSingleBinding> {
    private static final String TAG = "WheelViewDialog";

    private final List<String> dataList = new ArrayList<>();
    String selectedItem;
    int selectIndex;

    public interface ICallback {
        void onCancel();

        void onConfirm(int index, String content);
    }

    private WheelViewDialog.ICallback iCallback;


    public WheelViewDialog(@NonNull final Context context) {
        super(context, R.style.hotme_custom_dialog);
    }

    public WheelViewDialog setCallback(WheelViewDialog.ICallback iCallback) {
        this.iCallback = iCallback;
        return this;
    }

    public WheelViewDialog setTitle(String content) {
        viewBinding.title.setText(content);
        return this;
    }

    public WheelViewDialog setConfirm(String content) {
        viewBinding.btnOk.setText(content);
        return this;
    }

    public WheelViewDialog setDataList(List<String> data, int selectedIndex) {
        this.dataList.clear();
        this.dataList.addAll(data);
        viewBinding.wheelPicker.setVisibleItemCount(5);
        viewBinding.wheelPicker.setData(dataList);
        viewBinding.wheelPicker.setCyclic(false);
        selectedItem = this.dataList.get(selectedIndex);
        selectIndex = selectedIndex;
        viewBinding.wheelPicker.setSelectedItemPosition(selectedIndex);
        return this;
    }

    public WheelViewDialog setVisibleItemCount(int count) {
        viewBinding.wheelPicker.setVisibleItemCount(count);
        return this;
    }

    public WheelViewDialog setSelectedItemPosition(int index) {
        viewBinding.wheelPicker.setSelectedItemPosition(index);
        selectedItem = this.dataList.get(index);
        selectIndex = index;
        return this;
    }

    @Override
    protected void onInit() {

        viewBinding.wheelPicker.setOnItemSelectedListener((picker, data, position) -> {
            selectedItem = dataList.get(position);
            selectIndex = position;
        });

        viewBinding.ivClose.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(iCallback != null) {
                    iCallback.onCancel();
                }

                dismiss();
                return true;
            }
        });

        viewBinding.btnOk.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {

                if(iCallback != null) {
                    iCallback.onConfirm(selectIndex, selectedItem);
                }

                dismiss();

                return true;
            }
        });
    }
}
