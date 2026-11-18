package com.xh.hotme.environment;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import java.io.File;
import java.io.FileOutputStream;
import android.content.res.Configuration;
import android.view.View;
import android.widget.TextView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.databinding.ActivityMainBinding;
import com.xh.hotme.http.SdkApi;
import com.xh.hotme.me.setmeal.dialog.ExpiredDialog;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, application = Application.class, qualifiers = "zh-rCN-w360dp-h800dp-mdpi")
public class EnvironmentUiTest {
    public static class InsetsActivity extends BaseActivity<ActivityMainBinding> {
        @Override protected void initView() {}
        @Override protected void initData() {}
        View root() { return viewBinding.getRoot(); }
    }

    @Test public void allTabsStayAboveNavigationAndInsetsNeverAccumulate() {
        try (var controller = Robolectric.buildActivity(InsetsActivity.class).create().start().visible()) {
            View root = controller.get().root();
            WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 28, 0, 0))
                    .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 48)).build();
            ViewCompat.dispatchApplyWindowInsets(root, insets);
            ViewCompat.dispatchApplyWindowInsets(root, insets);
            root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, 360, 800);
            assertEquals(28, root.getPaddingTop());
            assertEquals(48, root.getPaddingBottom());
            View tabs = root.findViewById(R.id.tab_group);
            int[] xy = new int[2]; tabs.getLocationInWindow(xy);
            assertTrue(xy[1] + tabs.getHeight() <= 800 - 48);
            for (int id : new int[]{R.id.tab_home, R.id.tab_icloud, R.id.tab_live, R.id.tab_me}) assertTrue(root.findViewById(id).getHeight() > 0);
            WindowInsetsCompat gesture = new WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 16)).build();
            ViewCompat.dispatchApplyWindowInsets(root, gesture);
            assertEquals(24, root.getPaddingTop()); assertEquals(16, root.getPaddingBottom());
        }
    }

    @Test public void expiredDialogHasRoomForButtonsWithLargeFonts() {
        try (var controller = Robolectric.buildActivity(InsetsActivity.class).create().start().visible()) {
            InsetsActivity activity = controller.get();
            Configuration config = new Configuration(activity.getResources().getConfiguration());
            config.fontScale = 1.5f;
            activity.getResources().updateConfiguration(config, activity.getResources().getDisplayMetrics());
            ExpiredDialog dialog = new ExpiredDialog(activity);
            dialog.setText("年费套餐已过期", "到期时间：2026-05-06 00:00:00", "升级", 1);
            dialog.show();
            View content = dialog.findViewById(android.R.id.content);
            content.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(700, View.MeasureSpec.AT_MOST));
            content.layout(0, 0, content.getMeasuredWidth(), content.getMeasuredHeight());
            View buy = dialog.findViewById(R.id.tv_buy);
            assertTrue(buy.getHeight() >= 48);
            int[] xy = new int[2]; buy.getLocationInWindow(xy);
            assertTrue("Purchase button was clipped", xy[1] + buy.getHeight() <= content.getHeight());
            snapshot(content, "expired-dialog-large-font.png");
            ((TextView)dialog.findViewById(R.id.tv_back)).performClick();
            assertFalse(dialog.isShowing());
        }
    }

    private static void settleDrawables(View view) {
        view.jumpDrawablesToCurrentState();
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) settleDrawables(group.getChildAt(i));
        }
    }

    private static void snapshot(View view, String name) {
        String directory = System.getenv("WEEWA_UI_SNAPSHOTS");
        if (directory == null) return;
        settleDrawables(view);
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        File file = new File(directory, name);
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
        catch (java.io.IOException e) { throw new AssertionError(e); }
    }

    @Test public void environmentFormShowsCustomFieldsAndValidation() {
        org.junit.Assume.assumeTrue(RuntimeEnvironment.canEdit());
        Application app = org.robolectric.RuntimeEnvironment.getApplication();
        app.getSharedPreferences("runtime_environment", 0).edit().clear().commit();
        app.getSharedPreferences("runtime_environment_custom", 0).edit().clear().commit();
        RuntimeEnvironment.init(app);
        try (var controller = Robolectric.buildActivity(RuntimeEnvironmentActivity.class).create().start().visible()) {
            RuntimeEnvironmentActivity activity = controller.get();
            android.widget.RadioGroup options = activity.findViewById(R.id.environment_options);
            options.check(R.id.environment_custom);
            assertEquals(R.id.environment_custom, options.getCheckedRadioButtonId());
            assertTrue(((android.widget.RadioButton)activity.findViewById(R.id.environment_custom)).isChecked());
            org.robolectric.shadows.ShadowLooper.idleMainLooper(700, java.util.concurrent.TimeUnit.MILLISECONDS);
            assertEquals(View.VISIBLE, activity.findViewById(R.id.custom_fields).getVisibility());
            ((android.widget.EditText)activity.findViewById(R.id.rabbitmq_address)).setText("amqp://localhost:5672/rewoo");
            ((android.widget.EditText)activity.findViewById(R.id.api_address)).setText("invalid");
            activity.findViewById(R.id.save_environment).performClick();
            assertTrue(((TextView)activity.findViewById(R.id.environment_status)).getText().toString().contains("hotme-api"));
            assertEquals("production", RuntimeEnvironment.get().mode);
            View content = activity.findViewById(android.R.id.content);
            content.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY));
            content.layout(0, 0, 360, 800);
            snapshot(content, "runtime-environment-form.png");
        }
    }

    @Test public void customAddressesSurvivePresetChangesAndReopeningWithoutApplyingThem() {
        org.junit.Assume.assumeTrue(RuntimeEnvironment.canEdit());
        Application app = org.robolectric.RuntimeEnvironment.getApplication();
        app.getSharedPreferences("runtime_environment", 0).edit().clear().commit();
        app.getSharedPreferences("runtime_environment_custom", 0).edit().clear().commit();
        RuntimeEnvironment.init(app);
        String rabbitmq = "amqp://192.168.1.20:61003/rewoo";
        String api = "http://192.168.1.20:61004/hotme-api";
        try {
            try (var controller = Robolectric.buildActivity(RuntimeEnvironmentActivity.class).create().start().visible()) {
                RuntimeEnvironmentActivity activity = controller.get();
                android.widget.RadioGroup options = activity.findViewById(R.id.environment_options);
                options.check(R.id.environment_custom);
                ((android.widget.EditText)activity.findViewById(R.id.rabbitmq_address)).setText(rabbitmq);
                ((android.widget.EditText)activity.findViewById(R.id.api_address)).setText(api);
                options.check(R.id.environment_test);
                options.check(R.id.environment_production);
                assertEquals(EnvironmentProfile.PRODUCTION, RuntimeEnvironment.get().mode);
            }
            RuntimeEnvironment.save(RuntimeEnvironment.resolve(EnvironmentProfile.TEST, "", ""));
            RuntimeEnvironment.init(app);
            try (var controller = Robolectric.buildActivity(RuntimeEnvironmentActivity.class).create().start().visible()) {
                RuntimeEnvironmentActivity activity = controller.get();
                android.widget.RadioGroup options = activity.findViewById(R.id.environment_options);
                assertEquals(R.id.environment_test, options.getCheckedRadioButtonId());
                options.check(R.id.environment_custom);
                assertEquals(rabbitmq, ((android.widget.EditText)activity.findViewById(R.id.rabbitmq_address)).getText().toString());
                assertEquals(api, ((android.widget.EditText)activity.findViewById(R.id.api_address)).getText().toString());
                assertEquals(EnvironmentProfile.TEST, RuntimeEnvironment.get().mode);
            }
        } finally {
            app.getSharedPreferences("runtime_environment", 0).edit().clear().commit();
            app.getSharedPreferences("runtime_environment_custom", 0).edit().clear().commit();
            RuntimeEnvironment.init(app);
        }
    }

    @Test public void repliesFromPreviousEnvironmentCannotRestoreOldLoginState() throws Exception {
        org.junit.Assume.assumeTrue(RuntimeEnvironment.canEdit());
        Application app = org.robolectric.RuntimeEnvironment.getApplication();
        RuntimeEnvironment.init(app);
        int[] callbacks = {0};
        com.xh.hotme.http.OkHttpCallbackDecode<Object> callback = new com.xh.hotme.http.OkHttpCallbackDecode<Object>(Object.class) {
            @Override public void onDataSuccess(Object result) { callbacks[0]++; }
            @Override public void onFailure(String code, String message) { callbacks[0]++; }
            @Override public void onFinish() { callbacks[0]++; }
        };
        RuntimeEnvironment.save(RuntimeEnvironment.resolve("test", "", ""));
        okhttp3.Response response = new okhttp3.Response.Builder()
                .request(new okhttp3.Request.Builder().url("https://old.example.com/hotme-api/login/mobile").build())
                .protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK")
                .body(okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"), "{\"code\":200,\"token\":\"old-token\"}")).build();
        callback.onResponse(null, response);
        callback.onFailure(null, new java.io.IOException("canceled"));
        assertEquals(0, callbacks[0]);
        app.getSharedPreferences("runtime_environment", 0).edit().clear().commit();
        RuntimeEnvironment.init(app);
    }

    @Test public void endpointAndProfileChangesSurviveReload() {
        org.junit.Assume.assumeTrue(RuntimeEnvironment.canEdit());
        Application app = org.robolectric.RuntimeEnvironment.getApplication();
        app.getSharedPreferences("runtime_environment", 0).edit().clear().commit();
        RuntimeEnvironment.init(app);
        assertTrue(RuntimeEnvironment.save(new EnvironmentProfile("custom", "amqp://localhost:5673/rewoo", "https://api.example.com:8443/prefix/hotme-api")));
        assertEquals("https://api.example.com:8443/prefix/hotme-api/login/mobile", SdkApi.mobile_pwd_login());
        assertEquals("https://api.example.com:8443/prefix/hotme-api/hm/package/list", SdkApi.package_list());
        RuntimeEnvironment.init(app);
        assertEquals("custom", RuntimeEnvironment.get().mode);
        assertTrue(RuntimeEnvironment.save(RuntimeEnvironment.resolve("test", "", "")));
        assertTrue(SdkApi.mobile_pwd_login().startsWith("https://weewa.test.rewoo360.com/hotme-api/"));
        app.getSharedPreferences("runtime_environment", 0).edit().clear().commit();
        RuntimeEnvironment.init(app);
    }
}
