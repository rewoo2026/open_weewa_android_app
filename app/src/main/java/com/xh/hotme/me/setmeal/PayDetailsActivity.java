package com.xh.hotme.me.setmeal;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.bluetooth.event.CommonFinishEvent;
import com.xh.hotme.databinding.ActivityPayDetalisBinding;
import com.xh.hotme.listener.IActionCallback;
import com.xh.hotme.live.bean.CreateOrderBean;
import com.xh.hotme.me.setmeal.utils.SetMealPlayManager;
import com.xh.hotme.me.setmeal.utils.OrderAmount;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;

/**
 * @Author: zhangtuan
 * @CreateDate: 2024/1/18
 * @Description: 支付详情
 */
public class PayDetailsActivity extends BaseActivity<ActivityPayDetalisBinding> {
    CreateOrderBean createOrderBean;

    int check = 1;

    @Override
    protected void initView() {
        createOrderBean = (CreateOrderBean) getIntent().getSerializableExtra("bean");
        viewBinding.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return false;
            }
        });

        try {
            String amount = OrderAmount.validate(createOrderBean);
            createOrderBean.data.hmOrder.amt = amount;
            viewBinding.tvName.setText(createOrderBean.data.hmOrder.orderName);
            viewBinding.tvMoney.setText("¥" + OrderAmount.yuanFromFen(amount));
        } catch (IllegalArgumentException error) {
            viewBinding.tvPay.setEnabled(false);
            ToastUtil.s(error.getMessage());
            finish();
            return;
        }

        viewBinding.ivSelectZfb.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                check = 1;
                viewBinding.ivSelectZfb.setImageResource(R.mipmap.icon_pay_details_secelt);
                viewBinding.ivSelectWx.setImageResource(R.mipmap.icon_pay_details_unsecelt);
                return false;
            }
        });

        viewBinding.ivSelectWx.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                check = 2;
                viewBinding.ivSelectWx.setImageResource(R.mipmap.icon_pay_details_secelt);
                viewBinding.ivSelectZfb.setImageResource(R.mipmap.icon_pay_details_unsecelt);
                return false;
            }
        });
        viewBinding.tvPay.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                SetMealPlayManager serMealPlayManager = new SetMealPlayManager(PayDetailsActivity.this);
                serMealPlayManager.setCommonListener((code, objects) -> {
                    if(code == IActionCallback.CALLBACK_ACTION_OK) {
                        if(objects[0].toString().equals("00")) {
                            EventBus.getDefault().post(new CommonFinishEvent(CommonFinishEvent.TYPE_PAY_SUCCESS));
                            finish();
                        }

                    }
                });
                serMealPlayManager.toPay(check, createOrderBean.data.hmOrder.orderNo,
                    createOrderBean.data.hmOrder.orderName,
                    createOrderBean.data.hmOrder.amt,
                    createOrderBean.data.hmOrder.orderTime,
                    createOrderBean.data.hmOrder.orderDetail);
                return false;
            }
        });


    }

    @Override
    protected void initData() {

    }
}
