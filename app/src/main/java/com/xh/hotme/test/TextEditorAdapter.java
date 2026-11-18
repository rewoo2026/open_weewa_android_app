package com.xh.hotme.test;

import android.content.Context;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseQuickAdapter;
import com.xh.hotme.base.BaseViewHolder;
import com.xh.hotme.R;
import com.xh.hotme.listener.ITextModelListener;
import com.xh.hotme.utils.YTextWatcher;

import java.util.List;

/**
 * Created by GJK on 2018/11/9.
 */

public class TextEditorAdapter extends BaseQuickAdapter<TextValueModelBean, BaseViewHolder> {
    private final static String TAG = "BluetoothDeviceAdapter";
    ITextModelListener _listener;

    public TextEditorAdapter(Context context, List data) {

        super(R.layout.dialog_text_edit_list_item, data);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, TextValueModelBean typeBean) {
        TextView tvName = baseViewHolder.findView(R.id.tv_name);
        tvName.setText(typeBean.getName());
        int position = baseViewHolder.getPosition();

        baseViewHolder.itemView.setOnClickListener((View.OnClickListener) v -> {
            typeBean.setSelect(true);
            if(_listener != null) {
                _listener.onSelect(position);
            }

        });

        EditText editText = baseViewHolder.findView(R.id.et_value);
        editText.addTextChangedListener(new YTextWatcher() {
            public void afterTextChanged(Editable s) {
                String content = editText.getText().toString();
                try {
                    typeBean.value = Integer.parseInt(content);
                } catch(Throwable e) {
                    e.printStackTrace();
                }
            }
        });
        editText.setText(String.valueOf(typeBean.value));
    }

    public List<TextValueModelBean> getMbrValue() {

        return getData();
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

}
