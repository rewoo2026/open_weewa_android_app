package com.xh.hotme.camera;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.xh.hotme.R;
import com.xh.hotme.base.ActivityStackManager;
import com.xh.hotme.bean.LoginResultBean;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.OkHttpUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Tests the debug APK activation/login screen with controlled API failures. */
public class ActiveLoginInstrumentation extends Instrumentation {
    private Activity activity;
    private final AtomicInteger requests = new AtomicInteger();
    private volatile String responseMessage = "验证码错误";
    private int passed;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        int resultCode = Activity.RESULT_OK;
        Field clientField = null;
        Object originalClient = null;
        try {
            // MIUI blocks activity launches from an instrumentation process in
            // the background. The host opens MainActivity before these checks.
            long foregroundDeadline = System.currentTimeMillis() + 15000;
            while (ActivityStackManager.impl.getCurrentActivity() == null
                    && System.currentTimeMillis() < foregroundDeadline) Thread.sleep(50);
            require(ActivityStackManager.impl.getCurrentActivity() != null, "Open the target app before running checks");
            waitForIdleSync();
            clientField = OkHttpUtil.class.getDeclaredField("CLIENT");
            clientField.setAccessible(true);
            originalClient = clientField.get(null);
            clientField.set(null, new OkHttpClient.Builder().addInterceptor(chain -> {
                String path = chain.request().url().encodedPath();
                if (path.endsWith("checkSmsCode") || path.endsWith("mobileLogin")) requests.incrementAndGet();
                return new Response.Builder().request(chain.request())
                        .protocol(Protocol.HTTP_1_1).code(200).message("OK")
                        .body(ResponseBody.create(MediaType.parse("application/json"),
                                "{\"code\":500,\"msg\":\"" + responseMessage + "\"}"))
                        .build();
            }).build());

            open(Constants.ACTIVE_TYPE_ACTIVE);
            runOnMainSync(() -> {
                ((CheckBox) activity.findViewById(R.id.cb_agree)).setChecked(true);
                activity.findViewById(R.id.btn_loginSubmit).performClick();
                assertStatus(getTargetContext().getString(R.string.login_hint_input_sms));
            });
            require(requests.get() == 0, "Empty code sent a request");
            passed++;

            open(Constants.ACTIVE_TYPE_ACTIVE);
            runOnMainSync(() -> {
                ((CheckBox) activity.findViewById(R.id.cb_agree)).setChecked(false);
                ((EditText) activity.findViewById(R.id.et_sms_code)).setText("123456");
                activity.findViewById(R.id.btn_loginSubmit).performClick();
                assertStatus(getTargetContext().getString(R.string.info_agree_user_proxy_and_privacy_agreement));
            });
            require(requests.get() == 0, "Missing agreement sent a request");
            passed++;

            for (int type : new int[]{Constants.ACTIVE_TYPE_ACTIVE, Constants.ACTIVE_TYPE_LOGIN}) {
                open(type);
                clickValid();
                waitForFailure("验证码错误");
                passed++;
            }

            responseMessage = "";
            open(Constants.ACTIVE_TYPE_LOGIN);
            clickValid();
            waitForFailure(getTargetContext().getString(R.string.login_fail));
            passed++;

            open(Constants.ACTIVE_TYPE_ACTIVE);
            Method begin = MobileActiveLoginActivity.class.getDeclaredMethod("beginRequest", String.class, long.class);
            begin.setAccessible(true);
            Field waiting = MobileActiveLoginActivity.class.getDeclaredField("_waitingForBind");
            waiting.setAccessible(true);
            Field timeout = MobileActiveLoginActivity.class.getDeclaredField("_timeoutRunnable");
            timeout.setAccessible(true);
            runOnMainSync(() -> {
                try {
                    begin.invoke(activity, "测试绑定", 20000L);
                    waiting.setBoolean(activity, true);
                    ((Runnable) timeout.get(activity)).run();
                    assertStatus(getTargetContext().getString(R.string.request_timeout));
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            });
            passed++;
            String previousToken = Constants.HEADER_TOKEN;
            ((MobileActiveLoginActivity) activity).onActive(new LoginResultBean("late-test-token"));
            waitForIdleSync();
            require(java.util.Objects.equals(previousToken, Constants.HEADER_TOKEN), "Late bind response changed login token");
            runOnMainSync(() -> assertStatus(getTargetContext().getString(R.string.request_timeout)));
            passed++;

            open(Constants.ACTIVE_TYPE_ACTIVE);
            runOnMainSync(() -> {
                try {
                    begin.invoke(activity, "测试绑定", 20000L);
                    waiting.setBoolean(activity, true);
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            });
            // Bluetooth callbacks arrive on a worker thread, so exercise that path.
            ((MobileActiveLoginActivity) activity).onActiveFail("绑定失败");
            waitForFailure("绑定失败");
            passed++;
            result.putString("stream", "\nOK (" + passed + " activation/login screen checks)\n");
        } catch (Throwable error) {
            resultCode = Activity.RESULT_CANCELED;
            result.putString("stream", "\nFAIL: " + error + "; cause: " + error.getCause() + "\n");
        } finally {
            runOnMainSync(() -> { if (activity != null) activity.finish(); });
            try { if (clientField != null) clientField.set(null, originalClient); }
            catch (Exception ignored) { }
            finish(resultCode, result);
        }
    }

    private void open(int type) throws Exception {
        Activity previous = activity;
        runOnMainSync(() -> { if (previous != null) previous.finish(); });
        long deadline = System.currentTimeMillis() + 5000;
        while (previous != null && !previous.isDestroyed() && System.currentTimeMillis() < deadline) Thread.sleep(50);
        require(previous == null || previous.isDestroyed(), "Previous singleTask activity was not destroyed");
        waitForIdleSync();
        Intent intent = new Intent(getTargetContext(), MobileActiveLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.REQUEST_MOBILE, "13800000000");
        intent.putExtra(Constants.ACTIVE_LOGIN, type);
        activity = startActivitySync(intent);
        waitForIdleSync();
    }

    private void clickValid() {
        runOnMainSync(() -> {
            ((CheckBox) activity.findViewById(R.id.cb_agree)).setChecked(true);
            ((EditText) activity.findViewById(R.id.et_sms_code)).setText("123456");
            activity.findViewById(R.id.btn_loginSubmit).performClick();
            require(!activity.findViewById(R.id.btn_loginSubmit).isEnabled(), "Request did not lock submit");
        });
    }

    private void waitForFailure(String message) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        boolean[] done = {false};
        while (System.currentTimeMillis() < deadline) {
            runOnMainSync(() -> done[0] = activity.findViewById(R.id.login_status).getVisibility() == View.VISIBLE);
            if (done[0]) break;
            Thread.sleep(50);
        }
        require(done[0], "Failure was not displayed");
        waitForIdleSync();
        runOnMainSync(() -> assertStatus(message));
    }

    private void assertStatus(String expected) {
        TextView status = activity.findViewById(R.id.login_status);
        require(status.getVisibility() == View.VISIBLE && expected.contentEquals(status.getText()),
                "Wrong failure banner: " + status.getText() + "; finishing=" + activity.isFinishing());
        require(activity.findViewById(R.id.btn_loginSubmit).isEnabled(), "Failure prevented retry");
        require(!activity.isFinishing(), "Failure incorrectly finished the login screen");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
