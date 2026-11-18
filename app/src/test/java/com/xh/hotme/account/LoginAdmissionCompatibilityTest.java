package com.xh.hotme.account;

import android.app.Application;
import android.content.Context;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.utils.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class LoginAdmissionCompatibilityTest {
    private Context originalContext;
    private String originalToken;
    private long originalLoginTime;
    private int originalMode;

    @Before public void setup() {
        originalContext = ReflectionHelpers.getStaticField(HotmeApplication.class, "smContext");
        originalToken = Constants.HEADER_TOKEN;
        originalLoginTime = LoginManager.loginTime;
        originalMode = Constants.ACCESS_MODEL;
        Application app = org.robolectric.RuntimeEnvironment.getApplication();
        ReflectionHelpers.setStaticField(HotmeApplication.class, "smContext", app);
        app.getSharedPreferences("daily_server_access", 0).edit().clear().commit();
        Constants.HEADER_TOKEN = "test-login-token";
        LoginManager.loginTime = System.currentTimeMillis();
    }

    @After public void restore() {
        ReflectionHelpers.setStaticField(HotmeApplication.class, "smContext", originalContext);
        Constants.HEADER_TOKEN = originalToken;
        LoginManager.loginTime = originalLoginTime;
        Constants.ACCESS_MODEL = originalMode;
    }

    @Test public void bothModesAcceptValidLoginWithoutOfflineRecordingPermission() {
        for (int mode : new int[]{Constants.ACCESS_MODEL_LOCAL, Constants.ACCESS_MODEL_REMOTE}) {
            Constants.ACCESS_MODEL = mode;
            assertFalse(DailyAccessManager.ready());
            assertTrue(LoginManager.validToken());
            assertNull(DailyAccessManager.device("camera-test"));
        }
    }

    @Test public void expiredLoginStillRequiresAuthenticationInBothModes() {
        LoginManager.loginTime = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000;
        for (int mode : new int[]{Constants.ACCESS_MODEL_LOCAL, Constants.ACCESS_MODEL_REMOTE}) {
            Constants.ACCESS_MODEL = mode;
            assertFalse(LoginManager.validToken());
        }
    }
}
