package com.xh.hotme.environment;

import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class CameraEnvironmentPresetTest {
    @Test public void productionResolvesToOfficialDomainsEvenWithOldSavedAddresses() {
        EnvironmentProfile profile = RuntimeEnvironment.resolveCamera(EnvironmentProfile.PRODUCTION,
                "amqp://223.109.239.30:61003/rewoo", "http://223.109.239.30:61004/hotme-api");
        assertEquals("production", profile.mode);
        assertEquals("https://weewa.rewoo360.com/hotme-api", profile.api);
        assertEquals("amqp://mq.rewoo360.com:5672/rewoo", profile.rabbitmq);
    }

    @Test public void testAndCustomProfilesRetainTheirOwnEndpoints() {
        EnvironmentProfile test = RuntimeEnvironment.resolveCamera(EnvironmentProfile.TEST, "", "");
        assertEquals("https://weewa.test.rewoo360.com/hotme-api", test.api);
        assertEquals("amqp://mq.test.rewoo360.com:5672/rewoo", test.rabbitmq);
        EnvironmentProfile custom = RuntimeEnvironment.resolveCamera(EnvironmentProfile.CUSTOM,
                "amqp://mq.example.com:61003/rewoo", "http://api.example.com:61004/hotme-api");
        assertEquals("http://api.example.com:61004/hotme-api", custom.api);
        assertEquals("amqp://mq.example.com:61003/rewoo", custom.rabbitmq);
    }
}
