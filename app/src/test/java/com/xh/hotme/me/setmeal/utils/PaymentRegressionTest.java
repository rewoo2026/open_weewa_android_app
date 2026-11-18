package com.xh.hotme.me.setmeal.utils;

import android.app.Application;
import android.content.Intent;
import android.widget.TextView;
import com.google.gson.Gson;
import com.xh.hotme.R;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.environment.RuntimeEnvironment;
import com.xh.hotme.live.bean.CreateOrderBean;
import com.xh.hotme.me.UserDeviceTypeManager;
import com.xh.hotme.me.setmeal.PayDetailsActivity;
import com.xh.hotme.me.setmeal.bean.UserPackageCountBean;
import com.xh.hotme.utils.OkHttpUtil;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class,
        shadows = {PaymentRegressionTest.CameraSelection.class, PaymentRegressionTest.OrderTransport.class})
public class PaymentRegressionTest {
    @Implements(CameraManager.class)
    public static class CameraSelection {
        static String deviceId;
        @Implementation protected static String getDeviceIdLast() { return deviceId; }
    }

    // Return an API response without creating a real order or invoking a payment provider.
    @Implements(OkHttpUtil.class)
    public static class OrderTransport {
        static HashMap<String, String> request;
        static CreateOrderBean response;
        @Implementation protected static void postData(String url, HashMap<String, String> params,
                Map<String, String> headers, Callback callback) {
            request = new HashMap<>(params);
            Response http = new Response.Builder().request(new Request.Builder().url(url).build())
                    .protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body(ResponseBody.create(MediaType.parse("application/json"), new Gson().toJson(response))).build();
            try { callback.onResponse(null, http); }
            catch (java.io.IOException error) { throw new AssertionError(error); }
        }
    }

    @Before public void setup() {
        Application app = org.robolectric.RuntimeEnvironment.getApplication();
        org.robolectric.util.ReflectionHelpers.setStaticField(HotmeApplication.class, "smContext", app);
        app.getSharedPreferences("runtime_environment", 0).edit().clear().commit();
        RuntimeEnvironment.init(app);
        CameraSelection.deviceId = "camera-a";
        DeviceManager.getBindDeviceList().clear();
        BindDeviceBean device = new BindDeviceBean();
        device.setDeviceId("camera-a");
        device.setDeviceType("6");
        DeviceManager.getBindDeviceList().add(device);
        UserDeviceTypeManager.getInstance().setStoragePackage(null);
        OrderTransport.response = order("36000");
    }

    private UserPackageCountBean.UserPackageData storage(double total) {
        UserPackageCountBean.UserPackageData data = new UserPackageCountBean.UserPackageData();
        data.deviceId = "camera-a";
        data.countType = 2;
        data.totalCount = total;
        data.upgradable = 1;
        data.expireTime = "2099-10-13 23:59:59";
        data.lastOrderId = "previous-storage-order";
        return data;
    }

    private CreateOrderBean order(String amount) {
        CreateOrderBean order = new CreateOrderBean();
        order.code = 200;
        order.data = new CreateOrderBean.DataBean();
        order.data.hmOrder = new CreateOrderBean.DataBean.HmOrderBean();
        order.data.hmOrder.orderNo = "new-storage-order";
        order.data.hmOrder.orderName = "云存储60G/年";
        order.data.hmOrder.amt = amount;
        order.data.hmPackage = new CreateOrderBean.DataBean.HmPackageBean();
        order.data.hmPackage.packagePrice = 360.0;
        return order;
    }

    private int createOrder() {
        int[] result = {0};
        SetMealPayUtils.createOrder("storage-60g", false, (code, objects) -> result[0] = code,
                UserDeviceTypeManager.getInstance().getLastOrderID());
        ShadowLooper.idleMainLooper();
        return result[0];
    }

    @Test public void zeroCapacityUserSendsNewPurchaseWithoutHistoricalCredit() {
        UserDeviceTypeManager.getInstance().setStoragePackage(storage(0));
        assertEquals(1, createOrder());
        assertEquals("camera-a", OrderTransport.request.get("deviceId"));
        assertEquals("storage-60g", OrderTransport.request.get("packageId"));
        assertEquals("1", OrderTransport.request.get("orderType"));
        assertEquals("", OrderTransport.request.get("upgradeOrderId"));
    }

    @Test public void activeStorageKeepsUpgradeAndExplicitRenewalDoesNotDeductCredit() {
        UserDeviceTypeManager.getInstance().setStoragePackage(storage(60));
        assertEquals(1, createOrder());
        assertEquals("2", OrderTransport.request.get("orderType"));
        assertEquals("previous-storage-order", OrderTransport.request.get("upgradeOrderId"));
        UserDeviceTypeManager.getInstance().setLastOrderID("");
        assertEquals(1, createOrder());
        assertEquals("1", OrderTransport.request.get("orderType"));
    }

    @Test public void storageStateCannotCrossDevicesOrServers() {
        UserDeviceTypeManager manager = UserDeviceTypeManager.getInstance();
        manager.setStoragePackage(storage(60));
        CameraSelection.deviceId = "camera-b";
        assertEquals("", manager.getLastOrderID());
        assertEquals(0, manager.getUserSaveCount(), 0);
        CameraSelection.deviceId = "camera-a";
        if (RuntimeEnvironment.canEdit()) {
            assertTrue(RuntimeEnvironment.save(RuntimeEnvironment.resolve("test", "", "")));
            assertEquals("", manager.getLastOrderID());
            assertEquals(0, manager.getUserSaveCount(), 0);
        } else {
            assertFalse(RuntimeEnvironment.save(RuntimeEnvironment.resolve("test", "", "")));
            assertEquals("production", RuntimeEnvironment.get().mode);
            assertEquals("previous-storage-order", manager.getLastOrderID());
            assertEquals(60, manager.getUserSaveCount(), 0);
        }
    }

    @Test public void zeroServerAmountIsRejectedBeforeOpeningPayment() {
        OrderTransport.response = order("0");
        assertEquals(2, createOrder());
    }

    @Test public void paymentPageShowsExactServerChargeRatherThanPackageStickerPrice() {
        Application app = org.robolectric.RuntimeEnvironment.getApplication();
        Intent intent = new Intent(app, PayDetailsActivity.class).putExtra("bean", order("12345"));
        try (var controller = Robolectric.buildActivity(PayDetailsActivity.class, intent).create()) {
            assertEquals("¥123.45", ((TextView) controller.get().findViewById(R.id.tv_money)).getText().toString());
            assertFalse(controller.get().isFinishing());
        }
    }

    @Test public void invalidRestoredPaymentPageCannotStartZeroYuanPayment() {
        Application app = org.robolectric.RuntimeEnvironment.getApplication();
        Intent intent = new Intent(app, PayDetailsActivity.class).putExtra("bean", order("0"));
        try (var controller = Robolectric.buildActivity(PayDetailsActivity.class, intent).create()) {
            assertTrue(controller.get().isFinishing());
            assertFalse(controller.get().findViewById(R.id.tv_pay).isEnabled());
        }
    }
}
