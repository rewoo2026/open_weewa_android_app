package com.xh.hotme.environment;

import android.app.Application;
import android.content.Intent;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.utils.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class UserProductionEnvironmentTest {
    private Application app;

    @Before public void setup() {
        org.junit.Assume.assumeFalse(RuntimeEnvironment.canEdit());
        app = org.robolectric.RuntimeEnvironment.getApplication();
        ReflectionHelpers.setStaticField(HotmeApplication.class, "smContext", app);
        Constants.TEST_SERVER = false;
        app.getSharedPreferences("runtime_environment", 0).edit().clear().commit();
    }

    @After public void cleanup() {
        if (app == null) return;
        Constants.TEST_SERVER = false;
        app.getSharedPreferences("runtime_environment", 0).edit().clear().commit();
        RuntimeEnvironment.init(app);
    }

    private void assertProduction() {
        assertEquals(EnvironmentProfile.PRODUCTION, RuntimeEnvironment.get().mode);
        assertEquals("https://weewa.rewoo360.com/hotme-api", RuntimeEnvironment.get().api);
        assertEquals("amqp://mq.rewoo360.com:5672/rewoo", RuntimeEnvironment.get().rabbitmq);
        assertTrue(SdkApi.mobile_pwd_login().startsWith("https://weewa.rewoo360.com/hotme-api/"));
    }

    @Test public void previouslySavedTestAndCustomProfilesAreReplacedOnStartup() {
        for (String mode : new String[]{"test", "custom"}) {
            app.getSharedPreferences("runtime_environment", 0).edit().putString("mode", mode)
                    .putString("rabbitmq", "amqp://localhost:5672/rewoo")
                    .putString("api", "http://localhost:8080/hotme-api").commit();
            Constants.TEST_SERVER = true;
            RuntimeEnvironment.init(app);
            assertProduction();
            assertEquals("production", app.getSharedPreferences("runtime_environment", 0).getString("mode", ""));
        }
    }

    @Test public void testAndCustomChangesCannotBeSavedInProductionUserBuild() {
        RuntimeEnvironment.init(app);
        long revision = RuntimeEnvironment.revision();
        assertFalse(RuntimeEnvironment.save(RuntimeEnvironment.resolve("test", "", "")));
        assertFalse(RuntimeEnvironment.save(RuntimeEnvironment.resolve("custom",
                "amqp://localhost:5672/rewoo", "http://localhost:8080/hotme-api")));
        assertEquals(revision, RuntimeEnvironment.revision());
        assertProduction();
    }

    @Test public void directOpeningOfAppOrCameraEditorImmediatelyClosesIt() {
        RuntimeEnvironment.init(app);
        for (boolean camera : new boolean[]{false, true}) {
            Intent intent = new Intent(app, RuntimeEnvironmentActivity.class).putExtra("camera", camera);
            try (var controller = Robolectric.buildActivity(RuntimeEnvironmentActivity.class, intent).create()) {
                assertTrue(controller.get().isFinishing());
            }
        }
        assertProduction();
    }
}
