package com.xh.hotme.test;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseQuickAdapter;
import com.xh.hotme.base.BaseViewHolder;
import com.xh.hotme.R;
import com.xh.hotme.bean.TextModelBean;
import com.xh.hotme.listener.ITextModelListener;

import java.util.List;

/**
 * Created by GJK on 2018/11/9.
 */

public class TextCheckBoxAdapter extends BaseQuickAdapter<TextModelBean, BaseViewHolder> {
    private final static String TAG = "BluetoothDeviceAdapter";
    ITextModelListener _listener;

    public TextCheckBoxAdapter(Context context, List data) {

        super(R.layout.dialog_text_checkbox_list_item, data);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, TextModelBean typeBean) {
        CheckBox cbType = baseViewHolder.findView(R.id.cb_type);
        TextView tvName = baseViewHolder.findView(R.id.tv_type_name);
        View rootView = baseViewHolder.findView(R.id.item_root_view);
        tvName.setText(typeBean.getName());
        int position = baseViewHolder.getPosition();

        baseViewHolder.itemView.setOnClickListener((View.OnClickListener) v -> {
            typeBean.setSelect(true);
            if(_listener != null) {
                _listener.onSelect(position);
            }

        });

        cbType.setOnClickListener(v -> {
            typeBean.setSelect(true);
            if(_listener != null) {
                _listener.onSelect(position);
            }

        });

        if(typeBean.isSelect()) {
            cbType.setChecked(true);
            rootView.setBackgroundResource(R.drawable.test_list_bg_selected);
        } else {
            cbType.setChecked(false);
            rootView.setBackgroundResource(R.drawable.test_list_bg_unselect);
        }
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setListener(ITextModelListener l) {
        _listener = l;
    }

}
