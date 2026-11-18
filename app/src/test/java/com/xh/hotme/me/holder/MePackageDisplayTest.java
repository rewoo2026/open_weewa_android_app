package com.xh.hotme.me.holder;

import android.app.Application;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.R;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.environment.RuntimeEnvironment;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.utils.OkHttpUtil;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class, qualifiers = "zh-rCN",
        shadows = {MePackageDisplayTest.CameraSelection.class, MePackageDisplayTest.PackageTransport.class})
public class MePackageDisplayTest {
    @Implements(CameraManager.class)
    public static class CameraSelection {
        static String deviceId;
        @Implementation protected static String getDeviceIdLast() { return deviceId; }
    }

    @Implements(OkHttpUtil.class)
    public static class PackageTransport {
        static String url, deviceId, response;
        @Implementation protected static void postFormData(String address, FormBody.Builder body,
                Map<String, String> headers, Callback callback) {
            FormBody form = body.build();
            deliver(address, form.value(0), callback);
        }
        @Implementation protected static void postData(String address, HashMap<String, String> params,
                Map<String, String> headers, Callback callback) {
            deliver(address, params.get("deviceId"), callback);
        }
        private static void deliver(String address, String selectedDevice, Callback callback) {
            url = address;
            deviceId = selectedDevice;
            Response http = new Response.Builder().request(new Request.Builder().url(address).build())
                    .protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body(ResponseBody.create(MediaType.parse("application/json"), response)).build();
            try { callback.onResponse(null, http); }
            catch (java.io.IOException error) { throw new AssertionError(error); }
        }
    }

    private Application app;

    @Before public void setup() {
        app = org.robolectric.RuntimeEnvironment.getApplication();
        ReflectionHelpers.setStaticField(HotmeApplication.class, "smContext", app);
        app.getSharedPreferences("runtime_environment", 0).edit().clear().commit();
        RuntimeEnvironment.init(app);
        DeviceManager.getBindDeviceList().clear();
        addDevice("mobile", "6");
        addDevice("venue", "7");
        addDevice("legacy", "1");
        PackageTransport.url = null;
    }

    private void addDevice(String id, String type) {
        BindDeviceBean device = new BindDeviceBean();
        device.setDeviceId(id);
        device.setDeviceName("Camera " + id);
        device.setDeviceType(type);
        DeviceManager.getBindDeviceList().add(device);
    }

    private void load(MeProfileHolder holder, String device, String json) {
        CameraSelection.deviceId = device;
        PackageTransport.response = json;
        ReflectionHelpers.callInstanceMethod(holder, "initData");
        ShadowLooper.idleMainLooper();
    }

    private String packages(String name, String expiry) {
        return "{\"code\":200,\"data\":[{\"packageType\":\"100\",\"countName\":\"" + name
                + "\",\"expireTime\":" + expiry + ",\"children\":["
                + "{\"countType\":2,\"countName\":\"云存储\",\"countUnit\":\"GB\","
                + "\"totalCount\":60,\"remainCount\":57.06,\"expireTime\":\"2099-10-17 00:00:00\",\"buttonLabel\":\"升级\"},"
                + "{\"countType\":3,\"countName\":\"赛事直播\",\"countUnit\":\"小时\","
                + "\"totalCount\":0,\"remainCount\":0,\"buttonLabel\":\"购买\"}]}]}";
    }

    @Test public void mobileCameraShowsServerPackageNameAndBothQuotaRows() {
        MeProfileHolder holder = MeProfileHolder.create(app, new FrameLayout(app));
        try {
            load(holder, "mobile", packages("移动基础版", "null"));
            assertEquals(SdkApi.package_query_all_v2(), PackageTransport.url);
            assertEquals("mobile", PackageTransport.deviceId);
            assertEquals("移动基础版", ((TextView) holder.itemView.findViewById(R.id.special_tv_base)).getText().toString());
            assertEquals(View.VISIBLE, holder.itemView.findViewById(R.id.special_service_item).getVisibility());
            RecyclerView rows = holder.itemView.findViewById(R.id.rv_user_package_count);
            assertEquals(2, rows.getAdapter().getItemCount());
            @SuppressWarnings("rawtypes") RecyclerView.Adapter adapter = rows.getAdapter();
            RecyclerView.ViewHolder row = adapter.createViewHolder(rows, 0);
            adapter.bindViewHolder(row, 0);
            assertEquals("云存储", ((TextView) row.itemView.findViewById(R.id.special_tv_name1)).getText().toString());
            assertEquals("57.06/60.0 GB", ((TextView) row.itemView.findViewById(R.id.special_tv_count1)).getText().toString());
            adapter.bindViewHolder(row, 1);
            assertEquals("赛事直播", ((TextView) row.itemView.findViewById(R.id.special_tv_name1)).getText().toString());
            assertEquals("购买", ((TextView) row.itemView.findViewById(R.id.special_view_item1_buy)).getText().toString());
        } finally { holder.onDestroy(); }
    }

    @Test public void switchingToVenueCameraDisplaysItsPackageAndRootExpiry() {
        MeProfileHolder holder = MeProfileHolder.create(app, new FrameLayout(app));
        try {
            load(holder, "mobile", packages("移动基础版", "null"));
            load(holder, "venue", packages("固定商用版", "\"2099-10-17 00:00:00\""));
            assertEquals(SdkApi.package_query_all_v2(), PackageTransport.url);
            assertEquals("venue", PackageTransport.deviceId);
            assertEquals("固定商用版", ((TextView) holder.itemView.findViewById(R.id.special_tv_base)).getText().toString());
            assertEquals("2099-10-17 00:00:00", ((TextView) holder.itemView.findViewById(R.id.special_vip_expired_time)).getText().toString());
            assertEquals(View.VISIBLE, holder.itemView.findViewById(R.id.special_service_item).getVisibility());
        } finally { holder.onDestroy(); }
    }

    @Test public void legacyCameraKeepsItsOriginalPackageEndpointAndPurchaseEntry() {
        MeProfileHolder holder = MeProfileHolder.create(app, new FrameLayout(app));
        try {
            load(holder, "legacy", "{\"code\":200,\"data\":[]}");
            assertEquals(SdkApi.package_query_all(), PackageTransport.url);
            assertEquals("legacy", PackageTransport.deviceId);
            assertEquals(View.VISIBLE, holder.itemView.findViewById(R.id.vip_purchase).getVisibility());
            assertEquals(View.GONE, holder.itemView.findViewById(R.id.special_service_item).getVisibility());
        } finally { holder.onDestroy(); }
    }

    @Test public void emptyPackageResponseClearsPreviousDeviceQuotaRows() {
        MeProfileHolder holder = MeProfileHolder.create(app, new FrameLayout(app));
        try {
            load(holder, "mobile", packages("移动基础版", "null"));
            load(holder, "venue", "{\"code\":200,\"data\":[]}");
            assertEquals(View.VISIBLE, holder.itemView.findViewById(R.id.tv_select_device).getVisibility());
            assertEquals(View.GONE, holder.itemView.findViewById(R.id.special_service_item).getVisibility());
            RecyclerView rows = holder.itemView.findViewById(R.id.rv_user_package_count);
            assertEquals(0, rows.getAdapter().getItemCount());
        } finally { holder.onDestroy(); }
    }
}
