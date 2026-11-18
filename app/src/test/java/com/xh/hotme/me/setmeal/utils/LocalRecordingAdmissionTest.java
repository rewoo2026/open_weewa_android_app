package com.xh.hotme.me.setmeal.utils;

import android.app.Application;
import android.content.Context;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.account.DailyAccessManager;
import com.xh.hotme.bean.BindDeviceBean;
import com.xh.hotme.bluetooth.DeviceManager;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.OkHttpUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class LocalRecordingAdmissionTest {
    private Context originalContext;
    private Object originalCameraInfo;
    private int originalMode;
    private String originalDeviceId;
    private List<BindDeviceBean> originalDevices;
    private Application app;

    @Before public void setup() {
        originalContext = ReflectionHelpers.getStaticField(HotmeApplication.class, "smContext");
        app = RuntimeEnvironment.getApplication();
        ReflectionHelpers.setStaticField(HotmeApplication.class, "smContext", app);
        originalMode = Constants.ACCESS_MODEL;
        originalCameraInfo = ReflectionHelpers.getStaticField(CameraManager.class, "_connectCameraInfo");
        ReflectionHelpers.setStaticField(CameraManager.class, "_connectCameraInfo", null);
        originalDeviceId = CameraManager.getSaveDeviceIdLast();
        originalDevices = new ArrayList<>(DeviceManager.getBindDeviceList());
        DeviceManager.getBindDeviceList().clear();
        app.getSharedPreferences("daily_server_access", 0).edit().clear().commit();
        Constants.ACCESS_MODEL = Constants.ACCESS_MODEL_LOCAL;
    }

    @After public void restore() {
        DeviceManager.getBindDeviceList().clear();
        DeviceManager.getBindDeviceList().addAll(originalDevices);
        CameraManager.setDeviceId(originalDeviceId);
        ReflectionHelpers.setStaticField(CameraManager.class, "_connectCameraInfo", originalCameraInfo);
        Constants.ACCESS_MODEL = originalMode;
        ReflectionHelpers.setStaticField(HotmeApplication.class, "smContext", originalContext);
    }

    private void selectCamera(String id, String type) {
        BindDeviceBean camera = new BindDeviceBean();
        camera.setDeviceId(id);
        camera.setDeviceType(type);
        DeviceManager.getBindDeviceList().add(camera);
        CameraManager.setDeviceId(id);
    }

    private boolean checkRecordingAdmission() {
        List<Boolean> results = new ArrayList<>();
        SetMealTimeManager.getInstance().sendCmdGetPlanUsage(1, app, results::add);
        assertEquals("Admission must resolve without a quota request", 1, results.size());
        return results.get(0);
    }

    @Test public void venuePartnerCameraOpensRecordingInBothModesWithoutDailySync() {
        selectCamera("venue-camera", "4");
        assertFalse(DailyAccessManager.ready());
        assertNull(DailyAccessManager.device("venue-camera"));
        for (int mode : new int[]{Constants.ACCESS_MODEL_LOCAL, Constants.ACCESS_MODEL_REMOTE}) {
            Constants.ACCESS_MODEL = mode;
            assertTrue(checkRecordingAdmission());
        }
    }

    @Test public void otherRecordingExemptTypesAlsoOpenLocallyWithoutDailySync() {
        for (String type : new String[]{"1", "6", "7"}) {
            selectCamera("camera-" + type, type);
            assertTrue(checkRecordingAdmission());
        }
    }

    @Test public void chargedAndUnknownCamerasStillRequireLocalRecordingAllowance() {
        for (String type : new String[]{"2", "3", "5", "", "99"}) {
            selectCamera("camera-" + type, type);
            assertFalse(checkRecordingAdmission());
        }
        CameraManager.setDeviceId("unbound-camera");
        assertFalse(checkRecordingAdmission());
    }

    @Test public void anotherCamerasExemptionDoesNotAuthorizeSelectedChargedCamera() {
        selectCamera("venue-camera", "4");
        selectCamera("personal-camera", "2");
        assertFalse(checkRecordingAdmission());
        CameraManager.setDeviceId("venue-camera");
        assertTrue(checkRecordingAdmission());
    }

    @Test public void venuePartnerCameraNeverQueriesPackageStatusOrShowsAnnualReminder() {
        selectCamera("venue-camera", "4");
        OkHttpClient originalClient = ReflectionHelpers.getStaticField(OkHttpUtil.class, "CLIENT");
        CapturingExecutor executor = new CapturingExecutor();
        OkHttpClient client = new OkHttpClient.Builder().dispatcher(new Dispatcher(executor)).build();
        ReflectionHelpers.setStaticField(OkHttpUtil.class, "CLIENT", client);
        try {
            for (int mode : new int[]{Constants.ACCESS_MODEL_LOCAL, Constants.ACCESS_MODEL_REMOTE}) {
                Constants.ACCESS_MODEL = mode;
                SetMealCheckManager.checkSetMeal(app);
                for (int checkType = 0; checkType <= 3; checkType++) {
                    List<Boolean> results = new ArrayList<>();
                    SetMealCheckManager.checkSetMeal(app, () -> results.add(true), checkType);
                    assertEquals(java.util.Collections.singletonList(true), results);
                }
            }
            assertEquals("Exempt cameras must not query package status", 0, executor.requests);
            selectCamera("personal-camera", "2");
            SetMealCheckManager.checkSetMeal(app);
            assertEquals("Charged cameras must retain the package reminder check", 1, executor.requests);
        } finally {
            client.dispatcher().cancelAll();
            ReflectionHelpers.setStaticField(OkHttpUtil.class, "CLIENT", originalClient);
        }
    }

    // Capture enqueued HTTP calls without running them or contacting a real server.
    private static final class CapturingExecutor extends AbstractExecutorService {
        int requests;
        private boolean shutdown;
        @Override public void execute(Runnable command) { requests++; }
        @Override public void shutdown() { shutdown = true; }
        @Override public List<Runnable> shutdownNow() { shutdown = true; return java.util.Collections.emptyList(); }
        @Override public boolean isShutdown() { return shutdown; }
        @Override public boolean isTerminated() { return shutdown; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return shutdown; }
    }
}
