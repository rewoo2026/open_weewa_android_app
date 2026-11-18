package com.xh.hotme.storage;

import android.app.Application;
import com.xh.hotme.HotmeApplication;
import com.xh.hotme.environment.RuntimeEnvironment;
import com.xh.hotme.http.OkHttpCallbackDecode;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.utils.ToastUtil;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Callback;
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
@Config(sdk = 35, application = Application.class,
        shadows = {CameraAnalysisFailureTest.AnalysisTransport.class, CameraAnalysisFailureTest.Notifications.class})
public class CameraAnalysisFailureTest {
    @Implements(ToastUtil.class)
    public static class Notifications {
        static String message;
        @Implementation protected static void s(String text) { message = text; }
    }
    @Implements(OkHttpUtil.class)
    public static class AnalysisTransport {
        static int status, requests;
        static String body;
        @Implementation protected static void postData(String url, String params,
                Map<String, String> headers, Callback callback) {
            requests++;
            respond(url, callback);
        }
        static void respond(String url, Callback callback) {
            Response http = new Response.Builder().request(new Request.Builder().url(url).build())
                    .protocol(Protocol.HTTP_1_1).code(status).message("Test response")
                    .body(ResponseBody.create(MediaType.parse("application/json"), body)).build();
            try { callback.onResponse(null, http); }
            catch (java.io.IOException error) { throw new AssertionError(error); }
        }
    }

    @Before public void setup() {
        Application app = org.robolectric.RuntimeEnvironment.getApplication();
        ReflectionHelpers.setStaticField(HotmeApplication.class, "smContext", app);
        app.getSharedPreferences("runtime_environment", 0).edit().clear().commit();
        RuntimeEnvironment.init(app);
        AnalysisTransport.status = 200;
        AnalysisTransport.body = "{\"code\":200,\"data\":[]}";
        AnalysisTransport.requests = 0;
        Notifications.message = null;
    }

    private CameraAnalysisModel model() {
        CameraAnalysisModel model = new CameraAnalysisModel();
        model.attach("camera-a");
        MatchAnalysisTask known = new MatchAnalysisTask();
        known.competitionId = "known-match";
        known.state = "analyzing";
        model.tasks.put(known.competitionId, known);
        return model;
    }

    @Test public void missingAiEndpointDoesNotCrashAndStopsAutomaticPolling() {
        CameraAnalysisModel model = model();
        AnalysisTransport.status = 404;
        AnalysisTransport.body = "{\"timestamp\":\"2026-10-09T23:46:05+08:00\",\"status\":404,"
                + "\"error\":\"Not Found\",\"path\":\"/hotme-api/hm/matchAnalysis/list\"}";
        model.start();
        ShadowLooper.idleMainLooper();
        assertTrue(model.error.contains("404"));
        assertTrue(model.tasks.containsKey("known-match"));
        assertFalse(ReflectionHelpers.<Boolean>getField(model, "busy"));
        ShadowLooper.idleMainLooper(15, TimeUnit.SECONDS);
        assertEquals(1, AnalysisTransport.requests);
        model.stop();
    }

    @Test public void nullSuccessDataPreservesTasksAndCanRecoverOnNextRefresh() {
        CameraAnalysisModel model = model();
        AnalysisTransport.body = "{\"code\":200,\"data\":null}";
        model.refresh();
        ShadowLooper.idleMainLooper();
        assertFalse(model.error.isEmpty());
        assertTrue(model.tasks.containsKey("known-match"));
        assertFalse(ReflectionHelpers.<Boolean>getField(model, "busy"));
        AnalysisTransport.body = "{\"code\":200,\"data\":[{\"competitionId\":\"new-match\",\"state\":\"queued\"}]}";
        model.refresh();
        ShadowLooper.idleMainLooper();
        assertEquals("", model.error);
        assertTrue(model.tasks.containsKey("new-match"));
        assertFalse(model.tasks.containsKey("known-match"));
    }

    @Test public void emptyTaskListIsAnAuthoritativeSuccess() {
        CameraAnalysisModel model = model();
        model.refresh();
        ShadowLooper.idleMainLooper();
        assertTrue(model.tasks.isEmpty());
        assertEquals("", model.error);
    }

    @Test public void nullSubmissionDoesNotReportSuccessOrClearSelectedMatches() {
        CameraAnalysisModel model = model();
        model.selecting = true;
        model.selected.add("20261009/match-a");
        AnalysisTransport.body = "{\"code\":200,\"data\":null}";
        model.retry("known-match");
        ShadowLooper.idleMainLooper();
        assertTrue(model.selecting);
        assertEquals(1, model.selected.size());
        assertFalse(model.submitting);
        assertTrue(Notifications.message.contains("提交失败"));
        assertTrue(model.tasks.containsKey("known-match"));
    }

    @Test public void malformedTaskRowsCannotCrashProgressRendering() {
        CameraAnalysisModel model = model();
        AnalysisTransport.body = "{\"code\":200,\"data\":[null,{\"competitionId\":\"\"},"
                + "{\"competitionId\":\"valid-match\",\"state\":\"queued\"}]}";
        model.refresh();
        ShadowLooper.idleMainLooper();
        assertEquals(1, model.tasks.size());
        assertTrue(model.hasActive());
    }

    @Test public void httpErrorsAndMalformedEnvelopesNeverReachSuccessCallbacks() {
        for (int status : new int[]{404, 500, 200}) {
            int[] callbacks = {0, 0, 0};
            String[] failureCode = {null};
            AnalysisTransport.status = status;
            AnalysisTransport.body = status == 500 ? "<html>Server unavailable</html>"
                    : "{\"status\":404,\"error\":\"Not Found\"}";
            AnalysisTransport.respond("https://example.com/hotme-api/hm/matchAnalysis/list",
                    new OkHttpCallbackDecode<MatchAnalysisTask.Response>(MatchAnalysisTask.Response.class) {
                        @Override public void onDataSuccess(MatchAnalysisTask.Response data) { callbacks[0]++; }
                        @Override public void onFailure(String code, String message) { callbacks[1]++; failureCode[0] = code; }
                        @Override public void onFinish() { callbacks[2]++; }
                    });
            assertArrayEquals(new int[]{0, 1, 1}, callbacks);
            assertEquals(status == 200 ? "-2" : String.valueOf(status), failureCode[0]);
        }
    }
}
