package com.xh.hotme.me.setmeal.dialog;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseDialog;
import com.xh.hotme.databinding.DialogExpiredBinding;
import com.xh.hotme.me.camsetting.ConfigVideoActivity;
import com.xh.hotme.me.setmeal.SetMealListActivity;
import com.xh.hotme.utils.ClickGuard;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/15
 * @Description: 套餐使用次数耗尽  年费套餐过期
 */
public class ExpiredDialog extends BaseDialog<DialogExpiredBinding> {
    private int type;

    public void setText(String title, String content, String buy, int type) {
        this.type = type;
        viewBinding.tvTop.setText(title);
        viewBinding.tvBottom.setText(content);
        viewBinding.tvBuy.setText(buy);

        viewBinding.features.setVisibility(View.VISIBLE);
        viewBinding.liveFeature.setVisibility(View.GONE);
        viewBinding.ivIcon1.setVisibility(View.VISIBLE);
        viewBinding.ivIcon2.setVisibility(View.VISIBLE);
        viewBinding.ivIcon3.setVisibility(View.VISIBLE);
        viewBinding.tvText1.setVisibility(View.VISIBLE);
        viewBinding.tvText2.setVisibility(View.VISIBLE);
        viewBinding.tvText3.setVisibility(View.VISIBLE);
        viewBinding.ivIcon4.setVisibility(View.GONE);
        viewBinding.tvText4.setVisibility(View.GONE);
        viewBinding.tvLine.setVisibility(View.GONE);

    }

    public  void  setDialogText(String title, String content, String buy, int type, int showType) {
        this.type = type;
        viewBinding.tvTop.setText(title);
        viewBinding.tvBottom.setText(content);
        viewBinding.tvBuy.setText(buy);

        viewBinding.features.setVisibility(View.GONE);
        viewBinding.liveFeature.setVisibility(View.VISIBLE);
        viewBinding.ivIcon1.setVisibility(View.GONE);
        viewBinding.ivIcon2.setVisibility(View.GONE);
        viewBinding.ivIcon3.setVisibility(View.GONE);
        viewBinding.tvText1.setVisibility(View.GONE);
        viewBinding.tvText2.setVisibility(View.GONE);
        viewBinding.tvText3.setVisibility(View.GONE);
        viewBinding.ivIcon4.setVisibility(View.VISIBLE);
        viewBinding.tvText4.setVisibility(View.VISIBLE);
        viewBinding.tvLine.setVisibility(View.GONE);

//        showType 展示类型 区分直播1和存储2
        if (showType == 2) {
            viewBinding.liveFeature.setVisibility(View.GONE);
            viewBinding.ivIcon4.setVisibility(View.GONE);
            viewBinding.tvText4.setVisibility(View.GONE);
            viewBinding.tvLine.setVisibility(View.VISIBLE);
        }

    }

    // 需要一个给录制上传开关的弹窗
    public  void  setDialogTextForSwithStrong(String title, String content, String buy, String back, int type) {
        this.type = type;
        viewBinding.tvTop.setText(title);
        viewBinding.tvBottom.setText(content);
        viewBinding.features.setVisibility(View.GONE);
        viewBinding.liveFeature.setVisibility(View.GONE);
        viewBinding.tvBottom.setTextSize(16);  
        viewBinding.tvBuy.setText(buy);
        viewBinding.tvBuy.setTextSize(16); 
        viewBinding.tvBack.setText(back);
        viewBinding.tvBack.setTextSize(16); 
        viewBinding.tvBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                dismiss();
                Intent intent = new Intent(getContext(), ConfigVideoActivity.class);
                getContext().startActivity(intent);
                return true;
            }
        });


        viewBinding.ivIcon1.setVisibility(View.GONE);
        viewBinding.ivIcon2.setVisibility(View.GONE);
        viewBinding.ivIcon3.setVisibility(View.GONE);
        viewBinding.tvText1.setVisibility(View.GONE);
        viewBinding.tvText2.setVisibility(View.GONE);
        viewBinding.tvText3.setVisibility(View.GONE);

        viewBinding.ivIcon4.setVisibility(View.GONE);
        viewBinding.tvText4.setVisibility(View.GONE);
        viewBinding.tvLine.setVisibility(View.GONE);


    }

    public ExpiredDialog(@NonNull final Context context) {
        super(context, R.style.hotme_custom_dialog);
    }

    @Override
    protected void onInit() {
        viewBinding.tvBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                dismiss();
                return true;
            }
        });

        viewBinding.tvBuy.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                dismiss();

                Intent intent = new Intent(getContext(), SetMealListActivity.class);
                intent.putExtra("type", type);
                getContext().startActivity(intent);
                return true;
            }
        });
    }
}
