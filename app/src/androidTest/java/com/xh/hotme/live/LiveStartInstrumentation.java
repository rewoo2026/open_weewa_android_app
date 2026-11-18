package com.xh.hotme.live;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.bluetooth.Device;
import com.xh.hotme.bluetooth.bean.BaseBluetoothBean;
import com.xh.hotme.http.SdkApi;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/** Explicit live-camera test of the installed release APK; requires a signed-in account. */
public class LiveStartInstrumentation extends Instrumentation {
    private Bundle args;
    private JSONObject mapping;
    private Object bluetooth, recordListener;
    private final AtomicReference<Object> recordStatus = new AtomicReference<>();
    private Activity page;
    private String taskId;
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); args = arguments; start(); }

    @Override public void onStart() {
        Bundle result = new Bundle(); int outcome = Activity.RESULT_OK;
        try {
            mapping = new JSONObject(args.getString("mapping"));
            require(LoginManager.isLogon(), "Phone is not signed in");
            String deviceId = "zzg20230bde63f46f0", address = null;
            for(java.util.Map.Entry<String, ?> e : getTargetContext().getSharedPreferences("com.xh.hotme", Context.MODE_PRIVATE).getAll().entrySet()) {
                if(e.getKey().startsWith("device_") && e.getValue() instanceof String) {
                    try { if(deviceId.equals(new JSONObject((String)e.getValue()).optString("device_id"))) {
                        String candidate = e.getKey().substring(7);
                        if(candidate.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")) address = candidate;
                    }} catch(Exception ignored) {}
                }
            }
            require(address != null, "No cached Bluetooth address for the camera");
            final String cameraAddress = address;
            Class<?> listenerType = type("com.xh.hotme.bluetooth.listener.IRecordControlListener");
            recordListener = Proxy.newProxyInstance(listenerType.getClassLoader(), new Class<?>[]{listenerType}, (proxy, method, parameters) -> {
                if(method.getName().equals(mapped("com.xh.hotme.bluetooth.listener.IRecordControlListener", "onRecordVideoStatusUpdate"))) recordStatus.set(parameters[0]);
                if(method.getName().equals("equals")) return proxy == parameters[0];
                if(method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if(method.getName().equals("toString")) return "LiveStartProbe";
                return null;
            });
            onMain(() -> {
                invoke("com.xh.hotme.camera.CameraManager", "newCamera", null, cameraAddress);
                bluetooth = invoke("com.xh.hotme.bluetooth.BluetoothManager", "getImpl", null);
                invoke("com.xh.hotme.bluetooth.BluetoothHandle", "addPlayNotifyListener", null, recordListener);
                invoke("com.xh.hotme.bluetooth.BluetoothManager", "connectToBle", bluetooth, new Device(cameraAddress, 0, "weewa520"));
            });
            for(int i = 0; i < 30 && !Boolean.TRUE.equals(invoke("com.xh.hotme.bluetooth.BluetoothManager", "isConnected", bluetooth)); i++) Thread.sleep(1000);
            require(Boolean.TRUE.equals(invoke("com.xh.hotme.bluetooth.BluetoothManager", "isConnected", bluetooth)), "Bluetooth did not connect");
            Object before = status();
            int recording = ((Number) before.getClass().getMethod("getStatus").invoke(before)).intValue();
            int protocol = before.getClass().getField("recordingProtocolVersion").getInt(before);
            progress("Camera connected; recording=" + recording + ", protocol=" + protocol);
            JSONObject original = post("/hm/competitionLive/find/2107113982743097346").optJSONObject("data");
            if(original != null) progress("Existing task status=" + original.optInt("competitionStatus") + ", push=" + original.optInt("pushStreamStatus"));
            if("cleanup".equals(args.getString("mode"))) {
                taskId = args.getString("taskId");
                require(taskId != null, "Cleanup requires the test task id");
                result.putString("stream", "\nCleanup requested for test task " + taskId + "\n");
            } else if("probe".equals(args.getString("mode"))) {
                result.putString("stream", "\nPASS: installed release, signed-in account, Bluetooth and protocol query\n");
            } else {
                require(recording == 0, "Camera is recording; live test would affect the current task");
                long selectedTime = System.currentTimeMillis() + 5 * 60 * 1000;
                if(original != null && original.optInt("competitionStatus") != 2) {
                    String end = original.optString("competitionEndTime");
                    if(!end.isEmpty()) selectedTime = Math.max(selectedTime,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(end).getTime() + 120000);
                }
                require(selectedTime - System.currentTimeMillis() < 3 * 60 * 60 * 1000,
                    "Existing task schedule leaves no immediate-start test window");
                final Date selectedDate = new Date(selectedTime);
                String name = "直播复测" + new SimpleDateFormat("MMddHHmmss", Locale.CHINA).format(new Date());
                ActivityMonitor mainMonitor = addMonitor("com.xh.hotme.main.MainActivity", null, false);
                try(android.os.ParcelFileDescriptor shell = getUiAutomation().executeShellCommand(
                    "am start -n com.xh.hotme/.main.MainActivity")) {
                    require(mainMonitor.waitForActivityWithTimeout(10000) != null, "Cannot foreground the app");
                } finally { removeMonitor(mainMonitor); }
                Intent intent = new Intent(); intent.setClassName(getTargetContext(), "com.xh.hotme.live.AddLiveTaskActivity");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                page = startActivitySync(intent);
                onMain(() -> {
                    ((EditText)view("et_competition_name")).setText(name);
                    Date time = selectedDate;
                    setField(page, "competitionTime", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(time));
                    ((TextView)view("tv_time_select")).setText(new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(time));
                    view("plt_rewoo").setSelected(true); view("plt_wx_dy").setSelected(false); view("plt_mgc").setSelected(false);
                    view("tv_ok").performClick();
                });
                progress("Clicked confirm for " + name);
                long deadline = System.currentTimeMillis() + 190000, lastUpdate = 0;
                while(System.currentTimeMillis() < deadline && !page.isFinishing()) {
                    Object bean = field(page, "_bean");
                    if(bean != null) taskId = (String) bean.getClass().getField("competitionId").get(bean);
                    if(!(Boolean)field(page, "_workflowPending")) throw new AssertionError("Live start failed before confirmation");
                    if(System.currentTimeMillis() - lastUpdate > 15000) {
                        progress("Waiting for start; stage=" + field(page, "_liveStartStage")); lastUpdate = System.currentTimeMillis();
                    }
                    Thread.sleep(500);
                }
                require(page.isFinishing() && taskId != null, "Add page did not finish after confirmed start");
                Object actual = status();
                require(((Number)actual.getClass().getMethod("getStatus").invoke(actual)).intValue() == 1, "Camera did not confirm recording");
                JSONObject task = post("/hm/competitionLive/find/" + taskId).getJSONObject("data");
                require(task.optInt("competitionStatus") == 1 && task.optInt("pushStreamStatus") == 1, "Server did not confirm live status");
                require(name.equals(task.optString("competitionName")), "Confirmed the wrong task");
                String pull = post("/hm/competitionLive/pullStreamAddr/playClient?competitionId=" + taskId).getString("data");
                boolean playlist = false;
                for(int i=0; i<15 && !playlist; i++) {
                    try { playlist = playablePlaylist(pull); } catch(Exception ignored) {}
                    if(!playlist) Thread.sleep(2000);
                }
                require(playlist, "No playable HLS segments appeared");
                result.putString("stream", "\nPASS: confirm button -> protocol 3 server start -> page closes -> camera recording -> server live -> HLS segments; task=" + taskId + "\n");
            }
        } catch(Throwable error) {
            outcome = Activity.RESULT_CANCELED;
            result.putString("stream", "\nFAIL: " + error + "; cause=" + error.getCause() + "; task=" + taskId + "\n");
        } finally {
            if(taskId != null) {
                try {
                    JSONObject stopped = post("/hm/competitionLive/finishLive/" + taskId);
                    progress("Test task cleanup response=" + stopped.optInt("code"));
                    int recording = 1;
                    for(int i=0; i<30 && recording != 0; i++) {
                        Object actual = status();
                        recording = ((Number)actual.getClass().getMethod("getStatus").invoke(actual)).intValue();
                        if(recording != 0) Thread.sleep(2000);
                    }
                    progress("Camera recording after cleanup=" + recording);
                    if(recording != 0) {
                        outcome = Activity.RESULT_CANCELED;
                        result.putString("stream", "\nFAIL: test task stop not confirmed; task=" + taskId + "\n");
                    }
                } catch(Throwable error) { progress("Cleanup not confirmed: " + error.getClass().getSimpleName()); }
            }
            if(recordListener != null) onMain(() -> invoke("com.xh.hotme.bluetooth.BluetoothHandle", "removePlayNotifyListener", null, recordListener));
            if(page != null && !page.isFinishing()) onMain(() -> page.finish());
        }
        finish(outcome, result);
    }

    private Object status() throws Exception {
        recordStatus.set(null);
        for(int i=0; i<30 && recordStatus.get()==null; i++) {
            onMain(() -> {
                BaseBluetoothBean command = new BaseBluetoothBean(); command.c = "record_status";
                invoke("com.xh.hotme.bluetooth.BluetoothManager", "sendCmdSimpleObj", bluetooth, command);
            });
            Thread.sleep(1000);
        }
        require(recordStatus.get()!=null, "Camera status query timed out"); return recordStatus.get();
    }
    private JSONObject post(String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(SdkApi.getRequestUrl() + "/hotme-api" + path).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(120000); c.setRequestMethod("POST"); c.setDoOutput(true);
        c.setRequestProperty("Authorization", "Bearer " + LoginManager.getUserToken());
        c.setRequestProperty("Content-Type", "application/json");
        try { c.getOutputStream().write("{}".getBytes(StandardCharsets.UTF_8));
            return new JSONObject(new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } finally { c.disconnect(); }
    }
    private String fetch(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(5000); c.setReadTimeout(5000);
        try { return new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8); } finally { c.disconnect(); }
    }
    private boolean playablePlaylist(String url) throws Exception {
        String body = fetch(url);
        if(body.contains("#EXT-X-STREAM-INF")) {
            String variant = mediaLine(body);
            if(variant == null) return false;
            url = new URL(new URL(url), variant).toString();
            body = fetch(url);
        }
        if(!body.contains("#EXTM3U") || !body.contains("#EXTINF")) return false;
        String segment = mediaLine(body);
        if(segment == null) return false;
        HttpURLConnection c = (HttpURLConnection)new URL(new URL(url), segment).openConnection();
        c.setConnectTimeout(5000); c.setReadTimeout(5000);
        try(java.io.InputStream stream = c.getInputStream()) {
            return c.getResponseCode() == 200 && stream.read() >= 0;
        } finally { c.disconnect(); }
    }
    private String mediaLine(String playlist) {
        for(String line : playlist.split("\\r?\\n")) {
            line = line.trim();
            if(!line.isEmpty() && !line.startsWith("#")) return line;
        }
        return null;
    }
    private android.view.View view(String name) {
        int id = getTargetContext().getResources().getIdentifier(name, "id", "com.xh.hotme");
        android.view.View view = page.findViewById(id); require(view != null, "Missing view " + name); return view;
    }
    private String mapped(String owner, String name) throws Exception { return mapping.getJSONObject(owner).getString(name); }
    private Class<?> type(String name) throws Exception { return Class.forName(mapping.getJSONObject(name).getString("class")); }
    private Object invoke(String owner, String name, Object target, Object... params) {
        try {
            String mapped = mapped(owner, name);
            for(Method method : type(owner).getDeclaredMethods()) if(method.getName().equals(mapped) && method.getParameterCount()==params.length) {
                boolean matches=true; Class<?>[] types=method.getParameterTypes();
                for(int i=0;i<params.length;i++) if(params[i]!=null && !types[i].isInstance(params[i])) matches=false;
                if(matches) { method.setAccessible(true); return method.invoke(target, params); }
            }
            throw new NoSuchMethodException(owner + "." + name);
        } catch(Exception e) { throw new AssertionError(e); }
    }
    private Field mappedField(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(mapped("com.xh.hotme.live.AddLiveTaskActivity", name)); field.setAccessible(true); return field;
    }
    private Object field(Object object, String name) throws Exception { return mappedField(object,name).get(object); }
    private void setField(Object object, String name, Object value) {
        try { mappedField(object,name).set(object,value); } catch(Exception e) { throw new AssertionError(e); }
    }
    private void onMain(Runnable action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        runOnMainSync(() -> { try { action.run(); } catch(Throwable e) { failure.set(e); } });
        if(failure.get()!=null) throw new AssertionError(failure.get());
    }
    private void progress(String message) { Bundle b=new Bundle(); b.putString("stream", message + "\n"); sendStatus(0,b); }
    private static void require(boolean value, String message) { if(!value) throw new AssertionError(message); }
}
