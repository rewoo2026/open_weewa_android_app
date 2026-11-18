package com.xh.hotme.main;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.RadioButton;

import androidx.fragment.app.Fragment;

import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.base.ActivityStackManager;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.OkHttpUtil;
import com.xh.hotme.widget.MyRadioGroup;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Exercises real FragmentManager save/restore without changing stored login data. */
public class MainTabRestoreInstrumentation extends Instrumentation {
    private MainActivity activity;

    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        int status = Activity.RESULT_OK;
        String originalToken = Constants.HEADER_TOKEN;
        long originalLoginTime = LoginManager.loginTime;
        android.content.SharedPreferences daily=getTargetContext().getSharedPreferences("daily_server_access",0);
        String originalDaily=daily.getString("profile",null);
        Field clientField = null;
        Object originalClient = null;
        ActivityMonitor monitor = addMonitor(MainActivity.class.getName(), null, false);
        try {
            clientField = OkHttpUtil.class.getDeclaredField("CLIENT");
            clientField.setAccessible(true);
            originalClient = clientField.get(null);
            // Keep requests local and authentication in memory for the test.
            clientField.set(null, new OkHttpClient.Builder().addInterceptor(chain ->
                    new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                            .code(200).message("OK").body(ResponseBody.create(
                                    okhttp3.MediaType.parse("application/json"),
                                    "{\"code\":500,\"msg\":\"test\"}")).build()).build());
            Constants.HEADER_TOKEN = "tab-restore-test";
            LoginManager.loginTime = System.currentTimeMillis();
            com.xh.hotme.account.DailyAccessManager.Profile profile=new com.xh.hotme.account.DailyAccessManager.Profile();
            byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest((LoginManager.getUserId()+":"+LoginManager.getUserToken()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder identity=new StringBuilder();for(byte b:digest)identity.append(String.format("%02x",b&255));
            profile.account=identity.toString();profile.serverTime=System.currentTimeMillis();profile.refreshAt=profile.serverTime+3600000;
            profile.savedWall=profile.serverTime;profile.savedElapsed=android.os.SystemClock.elapsedRealtime();
            daily.edit().putString("profile",new com.google.gson.Gson().toJson(profile)).commit();
            try (ParcelFileDescriptor command = getUiAutomation().executeShellCommand(
                    "am start -n com.xh.hotme/.main.MainActivity");
                 FileInputStream output = new FileInputStream(command.getFileDescriptor())) {
                byte[] buffer = new byte[1024];
                while (output.read(buffer) != -1) { }
            }
            activity = (MainActivity) monitor.waitForActivityWithTimeout(15000);
            require(activity != null, "MainActivity did not open");
            waitForIdleSync();

            select(R.id.tab_me);
            select(R.id.tab_icloud);
            select(R.id.tab_me);
            recreate();
            assertTabs(R.id.tab_me, 3);
            select(R.id.tab_home);
            assertTabs(R.id.tab_home, 3);
            recreate();
            assertTabs(R.id.tab_home, 3);
            select(R.id.tab_icloud);
            recreate();
            assertTabs(R.id.tab_icloud, 3);

            // Older builds saved untagged fragments; also recover an already
            // duplicated back stack rather than carrying the overlap forward.
            onMain(() -> activity.getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, HomeDeviceListRecordFragment.newInstance())
                    .add(R.id.container, MeFragment.newInstance())
                    .commitNowAllowingStateLoss());
            recreate();
            assertTabs(R.id.tab_icloud, 3);
            select(R.id.tab_me);
            assertTabs(R.id.tab_me, 3);

            // A saved protected tab must fall back to Home after login expires.
            com.xh.hotme.account.DailyAccessManager.invalidate();
            recreate();
            assertTabs(R.id.tab_home, 3);
            result.putString("stream", "\nOK: Me/Home/Cloud recreation, switching after restore, legacy duplicates, expired-daily-sync fallback\n");
            result.putInt("passed", 6);
        } catch (Throwable e) {
            status = Activity.RESULT_CANCELED;
            result.putString("stream", "\nFAIL: " + e + "\n");
        } finally {
            Constants.HEADER_TOKEN = originalToken;
            LoginManager.loginTime = originalLoginTime;
            if(originalDaily==null)daily.edit().remove("profile").commit();else daily.edit().putString("profile",originalDaily).commit();
            if (clientField != null && originalClient != null) {
                try { clientField.set(null, originalClient); } catch (Exception ignored) { }
            }
            removeMonitor(monitor);
        }
        finish(status, result);
    }

    private void select(int id) {
        onMain(() -> ((MyRadioGroup) activity.findViewById(R.id.tab_group)).check(id));
        waitForIdleSync();
    }

    private void recreate() {
        MainActivity old = activity;
        // The long-lived monitor can still hold the initial onResume hit.
        ActivityMonitor recreation = addMonitor(MainActivity.class.getName(), null, false);
        try {
            onMain(old::recreate);
            activity = (MainActivity) recreation.waitForActivityWithTimeout(15000);
            require(activity != null && activity != old, "Activity recreation failed");
        } finally {
            removeMonitor(recreation);
        }
        waitForIdleSync();
        // The former destruction callback sent finish() for every same-class
        // instance. Wait past lifecycle dispatch to catch that delayed closure.
        try { Thread.sleep(1500); } catch (InterruptedException e) { throw new AssertionError(e); }
        onMain(() -> {
            require(!activity.isFinishing() && !activity.isDestroyed(), "Replacement Activity was closed");
            if(com.xh.hotme.account.DailyAccessManager.ready()) {
                require(ActivityStackManager.impl.getCurrentActivity() == activity,
                        "Activity stack still points to the destroyed instance");
            } else {
                require(ActivityStackManager.impl.getCurrentActivity() instanceof com.xh.hotme.account.DailyAccessActivity,
                        "Expired daily synchronization must show the gate");
            }
        });
    }

    private void assertTabs(int selectedId, int expectedCount) {
        onMain(() -> {
            int count = 0;
            int visible = 0;
            Set<Class<?>> types = new HashSet<>();
            for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) {
                if (fragment.getId() != R.id.container) continue;
                count++;
                require(types.add(fragment.getClass()), "Duplicate tab: " + fragment.getClass().getSimpleName());
                if (!fragment.isHidden()) {
                    visible++;
                    Class<?> expected = selectedId == R.id.tab_me ? MeFragment.class
                            : selectedId == R.id.tab_icloud ? CloudFragment.class
                            : HomeDeviceListRecordFragment.class;
                    require(fragment.getClass() == expected, "Wrong visible tab after restore");
                    require(fragment.getView() != null && fragment.getView().getVisibility() == View.VISIBLE,
                            "Selected tab has no visible view");
                }
            }
            require(count == expectedCount, "Unexpected tab count: " + count);
            require(visible == 1, "Expected one visible page, got " + visible);
            for (int id : new int[]{R.id.tab_home, R.id.tab_icloud, R.id.tab_live, R.id.tab_me}) {
                require(((RadioButton) activity.findViewById(id)).isChecked() == (id == selectedId),
                        "Tab indicator disagrees with visible page");
            }
        });
    }

    private void onMain(Runnable action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        runOnMainSync(() -> { try { action.run(); } catch (Throwable e) { failure.set(e); } });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
