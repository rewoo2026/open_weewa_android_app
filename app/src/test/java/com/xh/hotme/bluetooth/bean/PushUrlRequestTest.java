package com.xh.hotme.bluetooth.bean;

import android.app.Application;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.utils.GsonUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class PushUrlRequestTest {
    @Test public void signedApiUrlsSurviveTheCameraCommandWireFormat() throws Exception {
        for (String url : new String[]{
                "rtmp://camera.example/live/stream?auth_key=fixture-key",
                "rtmp://camera.example/live/stream?auth_info=a%2Bb%2F%3D&timestamp=0",
                "rtmp://camera.example/live/stream?token=fixture-token&x=1&x=2",
                "rtmp://camera.example/live/stream?auth_key=fixture-key&vhost=viewer.example",
                "rtmp://camera.example/live/stream"
        }) assertCameraReceives(url);
    }

    @Test public void savedCustomLiveKeysCannotAlterAnApiUrl() throws Exception {
        Application app = org.robolectric.RuntimeEnvironment.getApplication();
        ReflectionHelpers.setStaticField(HotmeApplication.class, "smContext", app);
        String address = "rtmp://camera.example/live/stream?auth_key=";
        String key = "&auth_key=stale-custom-key";
        JSONObject saved = new JSONObject().put("address", address).put("key", key);
        app.getSharedPreferences(app.getPackageName(), 0).edit()
                .putString("weewa_last_device_id", "signed-url-test-camera")
                .putString("user_other_livesigned-url-test-camera", saved.toString()).commit();
        assertCameraReceives(address);
        assertCameraReceives(address + key);
    }

    private static void assertCameraReceives(String apiUrl) throws Exception {
        // Bluetooth, HTTP and RabbitMQ use this serializer. JSON escaping must
        // round-trip to the exact string used to construct the camera request.
        JSONObject add = new JSONObject(GsonUtils.GsonString(new FecRtmpAddUrlRequestBean(apiUrl)));
        JSONObject remove = new JSONObject(GsonUtils.GsonString(new FecRtmpRemoveUrlRequestBean(apiUrl)));
        assertEquals(apiUrl, add.getString("url"));
        assertEquals("", add.getString("key"));
        assertEquals(apiUrl, add.getString("url") + add.getString("key"));
        assertEquals(apiUrl, remove.getString("url"));
        assertEquals("", remove.getString("key"));
    }
}
