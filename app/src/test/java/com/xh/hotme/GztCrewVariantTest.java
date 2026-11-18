package com.xh.hotme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.xh.hotme.http.SdkApi;
import com.xh.hotme.utils.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GztCrewVariantTest {
    private String originalRole;
    private boolean originalTestServer;

    @Before
    public void saveConfiguration() {
        originalRole = Constants.ROLE;
        originalTestServer = Constants.TEST_SERVER;
    }

    @After
    public void restoreConfiguration() {
        Constants.ROLE = originalRole;
        Constants.TEST_SERVER = originalTestServer;
    }

    @Test
    public void gztCrewHasInternalTesterFeaturesAndUserAppType() {
        Constants.ROLE = "gztTestr";
        assertTrue(Constants.isGztCrew());
        assertTrue(Constants.shouldShowCameraTest());
        assertTrue(Constants.shouldCheckOTAAlways());
        assertEquals(1, Constants.getAppType());

        Constants.ROLE = "tester";
        assertFalse(Constants.isGztCrew());
        assertTrue(Constants.shouldShowCameraTest());
        assertTrue(Constants.shouldCheckOTAAlways());
        assertEquals(1, Constants.getAppType());
    }

    @Test
    public void gztCrewUsesDedicatedServerForBothServerFlavors() {
        Constants.ROLE = "gztTestr";
        for (boolean testServer : new boolean[]{false, true}) {
            Constants.TEST_SERVER = testServer;
            assertEquals("http://223.109.239.30:61004", SdkApi.getRequestUrl());
            assertEquals("http://223.109.239.30:61004", SdkApi.getDomain());
            assertEquals("http://223.109.239.30:61004/hotme-api/hm/appVersion/getLatestAppByVersion?appType=1&versionCode=118",
                    SdkApi.getAppVersion(Constants.getAppType(), 118));
        }
    }

    @Test
    public void existingRolesKeepTheirFeaturesAppTypesAndServers() {
        String[] roles = {"user", "tester", "service", "collector", "greenKeeper"};
        int[] appTypes = {1, 1, 4, 5, 6};
        for (int i = 0; i < roles.length; i++) {
            Constants.ROLE = roles[i];
            assertFalse(Constants.isGztCrew());
            assertEquals(!"user".equals(roles[i]), Constants.shouldShowCameraTest());
            assertEquals("tester".equals(roles[i]), Constants.shouldCheckOTAAlways());
            assertEquals(appTypes[i], Constants.getAppType());
            Constants.TEST_SERVER = false;
            assertEquals("https://weewa.rewoo360.com", SdkApi.getRequestUrl());
            Constants.TEST_SERVER = true;
            assertEquals(com.xh.hotme.environment.RuntimeEnvironment.canEdit()
                    ? "https://weewa.test.rewoo360.com" : "https://weewa.rewoo360.com", SdkApi.getRequestUrl());
        }
    }
}
