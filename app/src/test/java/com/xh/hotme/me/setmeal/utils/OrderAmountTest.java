package com.xh.hotme.me.setmeal.utils;

import com.xh.hotme.live.bean.CreateOrderBean;
import org.junit.Test;
import static org.junit.Assert.*;

public class OrderAmountTest {
    @Test public void integerFenIsExactAndNotMultipliedTwice() {
        assertEquals("36000", OrderAmount.positiveFen("36000.00"));
        assertEquals("360.00", OrderAmount.yuanFromFen("36000"));
        assertEquals("0.01", OrderAmount.yuanFromFen("1"));
        assertEquals("19.99", OrderAmount.yuanFromFen("1999"));
        assertEquals("19.99", OrderAmount.packagePrice("19.990"));
    }

    @Test public void zeroNegativeFractionalAndMissingAmountsCannotReachPayment() {
        for (String amount : new String[]{null, "", "0", "0.0", "-100", "1.5", "NaN", "invalid"}) {
            assertThrows(IllegalArgumentException.class, () -> OrderAmount.positiveFen(amount));
        }
    }

    @Test public void packageStickerPriceCannotReplaceAnInvalidServerOrder() {
        CreateOrderBean order = new CreateOrderBean();
        order.data = new CreateOrderBean.DataBean();
        order.data.hmOrder = new CreateOrderBean.DataBean.HmOrderBean();
        order.data.hmOrder.orderNo = "order-1";
        order.data.hmOrder.amt = "0";
        order.data.hmPackage = new CreateOrderBean.DataBean.HmPackageBean();
        order.data.hmPackage.packagePrice = 360.0;
        assertThrows(IllegalArgumentException.class, () -> OrderAmount.validate(order));
        order.data.hmOrder.amt = "12345";
        assertEquals("123.45", OrderAmount.yuanFromFen(OrderAmount.validate(order)));
        assertThrows(IllegalArgumentException.class, () -> OrderAmount.validate(null));
    }
}
