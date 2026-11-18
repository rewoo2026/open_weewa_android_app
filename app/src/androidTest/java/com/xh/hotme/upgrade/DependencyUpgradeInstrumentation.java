package com.xh.hotme.upgrade;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xh.hotme.base.BaseQuickAdapter;
import com.xh.hotme.base.BaseViewHolder;
import com.xh.hotme.bluetooth.event.AutoUploadStatusEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.UploadPartResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Device-only SDK regression. Uses loopback HTTP and never writes business data. */
public class DependencyUpgradeInstrumentation extends Instrumentation {
    private final StringBuilder checks = new StringBuilder();
    private int passed;
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        int status = Activity.RESULT_OK;
        try {
            uploadPart();
            pass("Signed OBS upload on Android: request bytes, Authorization, URL, part number and ETag");
            JSONObject json = JSON.parseObject("{\"progress\":42,\"failed\":[\"fixture.mp4\"]}");
            require(json.getIntValue("progress") == 42 && json.get("failed") instanceof java.util.List,
                    "Camera JSON compatibility");
            pass("Fastjson camera status parsing");
            ObjectMapper mapper = new ObjectMapper();
            require(mapper.readTree(mapper.writeValueAsString(java.util.Collections.singletonMap("progress",42)))
                    .get("progress").asInt() == 42, "Jackson JSON round trip");
            pass("Jackson encode/decode on ART");
            StatusListener listener = new StatusListener();
            EventBus bus = EventBus.builder().build();
            bus.register(listener);
            AutoUploadStatusEvent event = new AutoUploadStatusEvent();event.progress=42;
            bus.post(event);bus.unregister(listener);
            require(listener.progress == 42, "EventBus camera event dispatch");
            pass("EventBus subscriber delivery");
            onMain(() -> {
                RecyclerView list = new RecyclerView(getTargetContext());
                list.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getTargetContext()));
                BaseQuickAdapter<String,BaseViewHolder> adapter = new BaseQuickAdapter<String,BaseViewHolder>(
                        android.R.layout.simple_list_item_1, new java.util.ArrayList<>(java.util.Collections.singletonList("fixture"))) {
                    @Override protected void convert(BaseViewHolder holder, String item) {
                        holder.setText(android.R.id.text1, item);
                    }
                };
                list.setAdapter(adapter);
                RecyclerView.ViewHolder holder=adapter.createViewHolder(list, adapter.getItemViewType(0));
                adapter.bindViewHolder(holder,0);
                require("fixture".contentEquals(((TextView)holder.itemView.findViewById(android.R.id.text1)).getText()),
                        "BRVAH row binding");
                adapter.setStateView(new View(getTargetContext()));adapter.setList(java.util.Collections.emptyList());
                require(adapter.getItems().isEmpty() && adapter.getItemCount()==1, "BRVAH empty state");
                new com.scwang.smart.refresh.layout.SmartRefreshLayout(getTargetContext());
                io.github.muddz.styleabletoast.StyleableToast.makeText(getTargetContext(), "fixture", com.xh.hotme.R.style.HotMeToast);
            });
            pass("BRVAH row/empty-state, SmartRefreshLayout and StyleableToast construction");
            android.graphics.Bitmap bitmap=Glide.with(getTargetContext()).asBitmap()
                    .load(android.R.drawable.ic_menu_camera).submit().get(10,TimeUnit.SECONDS);
            require(bitmap!=null && bitmap.getWidth()>0,"Glide resource decode");
            pass("Glide bitmap decode");
            com.tencent.mm.opensdk.openapi.IWXAPI wx=com.tencent.mm.opensdk.openapi.WXAPIFactory.createWXAPI(getTargetContext(),null);
            wx.isWXAppInstalled();wx.detach();
            pass("WeChat SDK initialization and installation query");
            checkScanBitmap();
            pass("mPaaS native QR decode of a generated fixture");
            result.putString("stream", "\nPASS: " + passed + " device SDK checks\n" + checks);
        } catch (Throwable error) {
            status=Activity.RESULT_CANCELED;
            result.putString("stream", "\nFAIL after " + passed + " checks: " + error + "\n" + checks);
            android.util.Log.e("DependencyUpgradeTest","Device regression failed",error);
        } finally {
            result.putInt("passed",passed);finish(status,result);
        }
    }
    public static class StatusListener {
        int progress;
        @Subscribe public void status(AutoUploadStatusEvent event){progress=event.progress;}
    }
    private void pass(String name){passed++;checks.append("PASS: ").append(name).append('\n');android.util.Log.i("DependencyUpgradeTest",name);}
    private void onMain(Runnable action) {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(()->{try{action.run();}catch(Throwable error){failure.set(error);}});
        if(failure.get()!=null)throw new AssertionError(failure.get());
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private void checkScanBitmap() throws Exception {
        ActivityMonitor monitor=addMonitor(com.xh.hotme.test.factory.CustomScanActivity.class.getName(),null,false);
        android.app.Activity scan;
        try {
            // Some device ROMs block target-process background activity starts.
            // Shell launch through UiAutomation is permitted and remains bounded.
            try (android.os.ParcelFileDescriptor command=getUiAutomation().executeShellCommand(
                    "am start -n com.xh.hotme/.test.factory.CustomScanActivity");
                 java.io.FileInputStream output=new java.io.FileInputStream(command.getFileDescriptor())) {
                byte[] buffer=new byte[1024];while(output.read(buffer)!=-1){}
            }
            scan=monitor.waitForActivityWithTimeout(10000);
            require(scan!=null,"Scan activity launch timed out");
        } finally { removeMonitor(monitor); }
        android.graphics.Bitmap bitmap = null;
        try {
            java.lang.reflect.Field scannerField=scan.getClass().getDeclaredField("mpScanner");
            scannerField.setAccessible(true);
            com.alipay.android.phone.scancode.export.adapter.MPScanner scanner=
                    (com.alipay.android.phone.scancode.export.adapter.MPScanner)scannerField.get(scan);
            String fixture="weewa-sdk-upgrade-fixture";
            com.google.zxing.common.BitMatrix matrix=new com.google.zxing.qrcode.QRCodeWriter().encode(
                    fixture,com.google.zxing.BarcodeFormat.QR_CODE,512,512);
            bitmap=android.graphics.Bitmap.createBitmap(512,512,android.graphics.Bitmap.Config.ARGB_8888);
            int[] pixels=new int[512*512];
            for(int y=0;y<512;y++)for(int x=0;x<512;x++)pixels[y*512+x]=matrix.get(x,y)?0xff000000:0xffffffff;
            bitmap.setPixels(pixels,0,512,0,0,512,512);
            com.alipay.android.phone.scancode.export.adapter.MPScanResult decoded=scanner.scanFromBitmap(bitmap);
            require(decoded!=null && fixture.equals(decoded.getText()),"mPaaS native QR decode");
        } finally {
            if(bitmap!=null)bitmap.recycle();
            onMain(scan::finish);
        }
    }
    private void uploadPart() throws Exception {
        byte[] payload = "camera-video-part".getBytes(StandardCharsets.UTF_8);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(10000);
            Future<String> received = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(10000);
                    InputStream input = socket.getInputStream();
                    String requestLine = readLine(input);
                    String line;
                    int length = -1;
                    boolean signed = false;
                    while (!(line = readLine(input)).isEmpty()) {
                        if (line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:")) {
                            length = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                        }
                        if (line.toLowerCase(java.util.Locale.ROOT).startsWith("authorization:")) {
                            signed = line.contains("test-access-key:");
                        }
                    }
                    require(signed, "OBS request not signed");
                    require(payload.length == length, "Upload content length");
                    byte[] body = new byte[length];
                    int offset = 0;
                    while (offset < length) {
                        int count = input.read(body, offset, length - offset);
                        if (count < 0) throw new java.io.EOFException("Incomplete upload body");
                        offset += count;
                    }
                    require(java.util.Arrays.equals(payload, body), "Upload bytes");
                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\n"
                            + "Content-Length: 0\r\nETag: \"test-part-etag\"\r\n"
                            + "x-obs-request-id: local-test\r\nConnection: close\r\n\r\n")
                            .getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                    return requestLine;
                }
            });

            ObsConfiguration config = new ObsConfiguration();
            config.setEndPoint("http://127.0.0.1:" + server.getLocalPort());
            config.setPathStyle(true);
            config.setAuthType(AuthTypeEnum.OBS);
            config.setAuthTypeNegotiation(false);
            config.setMaxErrorRetry(0);
            config.setConnectionTimeout(5000);
            config.setSocketTimeout(5000);
            try (ObsClient client = new ObsClient("test-access-key", "test-secret-key", config)) {
                UploadPartRequest request = new UploadPartRequest("test-bucket", "match.mp4");
                request.setUploadId("local-upload");
                request.setPartNumber(1);
                request.setPartSize((long) payload.length);
                request.setInput(new ByteArrayInputStream(payload));
                UploadPartResult result;
                try {
                    result = client.uploadPart(request);
                } catch (Exception transportFailure) {
                    // Preserve mock-server assertion failures instead of reporting
                    // the subsequent socket reset as an SDK transport failure.
                    try { received.get(1, TimeUnit.SECONDS); }
                    catch (java.util.concurrent.ExecutionException serverFailure) {
                        throw new AssertionError("Loopback server: " + serverFailure.getCause(), serverFailure.getCause());
                    } catch (java.util.concurrent.TimeoutException ignored) {}
                    throw transportFailure;
                }
                require("\"test-part-etag\"".equals(result.getEtag()), "Upload ETag");
                require(result.getPartNumber() == 1, "Upload part number");
            }
            String requestLine = received.get(10, TimeUnit.SECONDS);
            require(requestLine.startsWith("PUT /test-bucket/match.mp4?"), "Upload URL");
            require(requestLine.contains("partNumber=1"), "Part query");
            require(requestLine.contains("uploadId=local-upload"), "Upload ID query");
        } finally {
            executor.shutdownNow();
        }
    }

    private static String readLine(InputStream input) throws java.io.IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int value;
        while ((value = input.read()) != -1) {
            if (value == '\n') return line.toString(StandardCharsets.US_ASCII.name()).replace("\r", "");
            line.write(value);
        }
        throw new java.io.EOFException("Incomplete HTTP headers");
    }
}
