package com.xh.hotme.test.factory;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseQuickAdapter;
import com.xh.hotme.base.BaseViewHolder;
import com.xh.hotme.R;
import com.xh.hotme.bean.TextModelBean;
import com.xh.hotme.listener.ITextModelListener;
import com.xh.hotme.utils.ColorUtil;

import java.util.List;

/**
 * Created by GJK on 2018/11/9.
 */

public class TestAdapter extends BaseQuickAdapter<TestModelBean, BaseViewHolder> {
    private final static String TAG = "BluetoothDeviceAdapter";
    ITestCommandListener _listener;

    public TestAdapter(Context context, List data) {

        super(R.layout.test_factory_list_item, data);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, TestModelBean typeBean) {


        View rootView = baseViewHolder.findView(R.id.test_factory_list_item_root);
        TextView tvName = baseViewHolder.findView(R.id.tv_test_name);
        TextView tvStatus = baseViewHolder.findView(R.id.tv_status);
        TextView tvTest = baseViewHolder.findView(R.id.tv_test);
        tvName.setText(typeBean.getName());

        updateStatus(baseViewHolder, typeBean);

        tvTest.setOnClickListener(v -> {
            if (_listener != null) {
                _listener.onStart(typeBean);
            }
        });

    }

    private void updateStatus(BaseViewHolder baseViewHolder, TestModelBean typeBean) {
        TextView tvStatus = baseViewHolder.findView(R.id.tv_status);
        View rootView = baseViewHolder.findView(R.id.test_factory_list_item_root);
        switch (typeBean.getStatus()) {
            case 0:
                tvStatus.setText("未测试");
                tvStatus.setTextColor(getContext().getResources().getColor(R.color.text_red));
                rootView.setBackgroundColor(ColorUtil.parseColor("#f5f5f5"));
                break;

            case 1:
                tvStatus.setText("通过");
                tvStatus.setTextColor(getContext().getResources().getColor(R.color.text_green));
                rootView.setBackgroundColor(ColorUtil.parseColor("#999999"));
                break;

            case 2:
                String message = "未通过";
                if(!TextUtils.isEmpty(typeBean.getErrMessage())){
                    message += "(" + typeBean.getErrMessage() +")";
                }
                tvStatus.setText(message);
                tvStatus.setTextColor(getContext().getResources().getColor(R.color.text_red));
                rootView.setBackgroundColor(ColorUtil.parseColor("#f5f5f5"));
                break;
        }
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setListener(ITestCommandListener l) {
        _listener = l;
    }

}
