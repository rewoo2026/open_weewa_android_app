package com.xh.hotme.account;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.xh.hotme.R;
import com.xh.hotme.listener.IApiCallback;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.NetUtil;

/** Exercises the real login view on Android with controlled SMS responses. */
public class MobileLoginSmsInstrumentation extends Instrumentation {
    private Activity activity;
    private TestLoginView view;
    private int passed;

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        int resultCode = Activity.RESULT_OK;
        try {
            Intent intent = new Intent(getTargetContext(), MobileLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = startActivitySync(intent);
            require(NetUtil.isConnectedAvailableNetwork(),
                    "Test environment needs an Android validated network");

            check("rapid clicks send one request and cooldown blocks further clicks", () -> {
                click();
                click();
                click();
                require(view.requests == 1, "Duplicate requests while awaiting response");
                require(!button().isEnabled(), "Button enabled before response");
                view.callback.onSuccess();
                view.callback.onFinish();
                click();
                require(view.requests == 1, "Request sent during cooldown");
                require(button().getText().toString().equals(Constants.sms_count_down
                                + getTargetContext().getString(R.string.second)),
                        "Countdown did not start");
            });
            check("success clears an earlier error and ignores a late failure", () -> {
                view.postSendCodeFail("验证码未过期");
                click();
                view.callback.onSuccess();
                view.callback.onFail("205", "验证码未过期");
                view.callback.onFinish();
                require(view.findViewById(R.id.ll_status).getVisibility() == View.GONE,
                        "Failure banner visible after success");
                require(status().getText().length() == 0, "Old failure text remains");
                require(!button().isEnabled(), "Late failure cancelled cooldown");
            });
            check("specific failure allows retry and old callbacks cannot unlock it", () -> {
                click();
                IApiCallback old = view.callback;
                old.onFail("205", "验证码未过期");
                require(status().getText().toString().equals("验证码未过期"),
                        "Server error was replaced by generic failure");
                require(button().isEnabled(), "Failed request left button disabled");
                click();
                old.onFinish();
                old.onSuccess();
                click();
                require(view.requests == 2, "Old callback unlocked a new request");
                view.callback.onSuccess();
                require(view.findViewById(R.id.ll_status).getVisibility() == View.GONE,
                        "Retry success did not clear error");
            });
            check("missing error text falls back and finish restores retry", () -> {
                click();
                view.callback.onFail("-1", "");
                require(status().getText().toString().equals(
                        getTargetContext().getString(R.string.info_send_sms_fail)),
                        "Missing fallback error");
                click();
                view.callback.onFinish();
                require(button().isEnabled(), "Incomplete response left request locked");
                click();
                require(view.requests == 3, "Retry unavailable after finish");
            });
            check("phone update shares duplicate prevention", () -> {
                view.setLoginType(Constants.TYPE_UPDATE_PHONE);
                click();
                click();
                require(view.requests == 1, "Phone update sent duplicate requests");
                view.callback.onFail("500", "短信服务暂不可用");
                require(status().getText().toString().equals("短信服务暂不可用"),
                        "Phone update lost server error");
                require(button().isEnabled(), "Phone update cannot retry");
            });
            check("synchronous request failure restores retry", () -> {
                view.throwOnSend = true;
                click();
                require(button().isEnabled(), "Exception left button disabled");
                require(view.findViewById(R.id.ll_status).getVisibility() == View.VISIBLE,
                        "Exception did not show a failure");
                view.throwOnSend = false;
                click();
                require(view.requests == 2, "Exception left request locked");
            });
            int cameraTests = CameraTokenHttpIntegration.run(this);
            result.putString("stream", "\nOK (" + passed + " SMS login tests, "
                    + cameraTests + " camera HTTP token tests)\n");
            result.putInt("passed", passed);
            result.putInt("cameraTokenPassed", cameraTests);
        } catch (Throwable failure) {
            result.putString("stream", "\nFAIL: " + failure + "; cause: "
                    + failure.getCause() + "\n");
            result.putInt("passed", passed);
            resultCode = Activity.RESULT_CANCELED;
        } finally {
            runOnMainSync(() -> {
                if (view != null) view.handler.removeCallbacksAndMessages(null);
                if (activity != null) activity.finish();
            });
        }
        finish(resultCode, result);
    }

    private void check(String name, Runnable test) {
        Throwable[] failure = new Throwable[1];
        runOnMainSync(() -> {
            try {
                if (view != null) view.handler.removeCallbacksAndMessages(null);
                view = new TestLoginView(activity);
                activity.setContentView(view);
                ((EditText) view.findViewById(R.id.et_loginAccount)).setText("18601042108");
                ((CheckBox) view.findViewById(R.id.cb_agree)).setChecked(true);
                test.run();
            } catch (Throwable error) {
                failure[0] = error;
            }
        });
        if (failure[0] != null) throw new AssertionError(name, failure[0]);
        passed++;
        Bundle progress = new Bundle();
        progress.putString("stream", "PASS: " + name + "\n");
        sendStatus(0, progress);
    }

    private void click() {
        // Directly dispatch queued click events, including while the button is disabled.
        view.onClick(button());
    }

    private Button button() {
        return view.findViewById(R.id.btn_send_sms_code);
    }

    private TextView status() {
        return view.findViewById(R.id.login_status);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static class TestLoginView extends MobileLoginView {
        int requests;
        IApiCallback callback;
        boolean throwOnSend;

        TestLoginView(Context context) {
            super(context, R.layout.login_layout_mobile_view);
        }

        @Override
        protected void sendSmsRequest(String mobile, IApiCallback callback) {
            requests++;
            if (throwOnSend) throw new IllegalStateException("Simulated request failure");
            this.callback = callback;
        }
    }
}
