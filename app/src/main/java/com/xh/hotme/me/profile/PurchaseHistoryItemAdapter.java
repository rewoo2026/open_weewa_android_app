package com.xh.hotme.me.profile;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseQuickAdapter;
import com.xh.hotme.base.BaseViewHolder;
import com.xh.hotme.R;
import com.xh.hotme.bean.PurchaseInfoBean;

import java.util.List;

/**
 * date   : 2023/6/19
 * desc   :
 */
public class PurchaseHistoryItemAdapter extends BaseQuickAdapter<PurchaseInfoBean, BaseViewHolder> {
    private final static String TAG = "PurchaseHistoryItemAdapter";
    final Context _context;

    public PurchaseHistoryItemAdapter(Context context, List data) {
        super(R.layout.vip_purchase_history_item, data);

        _context = context;

    }


    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, final PurchaseInfoBean purchaseInfoBean) {

        TextView tvName = baseViewHolder.findView(R.id.tv_name);
        ImageView ivServiceIcon = baseViewHolder.findView(R.id.iv_service);
        TextView description = baseViewHolder.findView(R.id.tv_description);
        TextView startTime = baseViewHolder.findView(R.id.tv_start_time);
        TextView endTime = baseViewHolder.findView(R.id.tv_expired_time);
        TextView purchaseType = baseViewHolder.findView(R.id.tv_purchase_source);

        tvName.setText(purchaseInfoBean.name);
        if(purchaseInfoBean.service_type == 0) {
            ivServiceIcon.setImageResource(R.mipmap.vip_diamon_rewo);


        } else if(purchaseInfoBean.service_type == 1) { //基础服务
            ivServiceIcon.setImageResource(R.mipmap.vip_diamon_base_service);

        } else if(purchaseInfoBean.service_type == 2) {   //pro服务

            ivServiceIcon.setImageResource(R.mipmap.vip_diamon_pro_service);
        } else if (purchaseInfoBean.service_type == 10001) {
            ivServiceIcon.setImageResource(R.mipmap.vip_diamon_base_save);
        }else{
            ivServiceIcon.setImageResource(R.mipmap.vip_diamon_base_card);
        }

        // 10001 存储 10002 直播

        startTime.setText(purchaseInfoBean.start_time);
        endTime.setText(purchaseInfoBean.end_time);
        purchaseType.setText(purchaseInfoBean.purchase_type);
        description.setText(purchaseInfoBean.description);

    }


    @Override
    public long getItemId(int position) {
        return position;
    }


}
