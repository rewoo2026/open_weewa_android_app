package com.xh.hotme.me.setmeal.utils;

import com.xh.hotme.live.bean.CreateOrderBean;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class OrderAmount {
    private OrderAmount() { }

    public static String positiveFen(String amount) {
        try {
            BigDecimal fen = new BigDecimal(amount.trim()).setScale(0, RoundingMode.UNNECESSARY);
            if (fen.signum() <= 0) throw new IllegalArgumentException();
            return fen.toPlainString();
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("订单金额异常，请返回刷新套餐后重试");
        }
    }

    public static String validate(CreateOrderBean order) {
        if (order == null || order.data == null || order.data.hmOrder == null
                || order.data.hmOrder.orderNo == null || order.data.hmOrder.orderNo.trim().isEmpty()) {
            throw new IllegalArgumentException("订单信息异常，请返回刷新套餐后重试");
        }
        return positiveFen(order.data.hmOrder.amt);
    }

    public static String yuanFromFen(String amount) {
        return new BigDecimal(positiveFen(amount)).movePointLeft(2).setScale(2).toPlainString();
    }

    public static String packagePrice(String price) {
        return new BigDecimal(price.trim()).stripTrailingZeros().toPlainString();
    }
}
