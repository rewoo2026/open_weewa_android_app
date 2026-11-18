package com.xh.hotme.storage;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.weewa.lib.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.*;

/** Runs the production fast-transfer classes, including the signed R8 release. */
public class CameraDownloadInstrumentation extends Instrumentation {
    private int passed;
    private File root;
    private long stressBytes;
    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        stressBytes = Long.parseLong(args == null ? "0" : args.getString("stressBytes", "0"));
        start();
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        int code = Activity.RESULT_OK;
        SharedPreferences prefs = Prefs.INSTANCE.defaultPreference(getTargetContext());
        String previous = prefs.getString("DOWNLOAD_PATH", null);
        try {
            WeewaLib lib = WeewaLib.Companion.shared();
            lib.launch(getTargetContext());
            discovery(lib);
            root = Files.createTempDirectory(getTargetContext().getCacheDir().toPath(), "download-regression-").toFile();
            Prefs.INSTANCE.setDownloadPath(prefs, root.getAbsolutePath());
            transfer("cross-boundary", false, false, false);
            transfer("resume", true, false, false);
            transfer("oversized", false, true, false);
            transfer("already-complete", false, false, true);
            largeFileTail();
            if (stressBytes > 0) sustainedTransfer();
            rejectedFiles();
            result.putString("stream", "\nPASS: " + passed + " signed-app fast-transfer checks: discovery port, repeated launch, malformed/extended discovery packets, short-to-long datagrams, restart, fragmented TCP handshake, exact multi-file bytes, resume, stale oversized files, completed resume, 3.51 GB final-chunk resume, empty list and disk errors. Stress download bytes: " + stressBytes + ".\n");
        } catch (Throwable e) {
            code = Activity.RESULT_CANCELED;
            result.putString("stream", "\nFAIL: " + e + "\n");
            e.printStackTrace();
        } finally {
            if (previous == null) prefs.edit().remove("DOWNLOAD_PATH").commit();
            else prefs.edit().putString("DOWNLOAD_PATH", previous).commit();
            if (root != null) delete(root);
        }
        finish(code, result);
    }
    private void discovery(WeewaLib lib) throws Exception {
        DiscoveryService service = (DiscoveryService) field(lib, "_ds");
        DatagramSocket socket = (DatagramSocket) field(service, "_socket");
        check(socket.getLocalPort() == 52637, "Discovery must listen on camera port 52637");
        lib.launch(getTargetContext());
        check(field(lib, "_ds") == service, "Repeated launch must reuse service");
        try (DatagramSocket sender = new DatagramSocket()) {
            send(sender, "{}"); // missing required field must be harmless
            send(sender, "{\"request\":true}"); // next response is much longer
            send(sender, "{\"request\":false,\"device_name\":\"下载测试相机\",\"device_type\":\"Linux\",\"port\":55001,\"port2\":55002,\"http_port\":38888,\"mode\":0,\"ip\":\"192.0.2.77\",\"firmware_version\":\"future\"}");
            long end = System.currentTimeMillis() + 5000;
            while(service.getMeta("192.0.2.77") == null && System.currentTimeMillis() < end) Thread.sleep(20);
            check(service.getMeta("192.0.2.77") != null, "Discovery must survive malformed and short packets and UTF-8 names");
        }
        long before = System.currentTimeMillis();
        service.stop();
        check(System.currentTimeMillis() - before < 3000, "Discovery stop must unblock receive");
        service.start();
        check(((DatagramSocket)field(service, "_socket")).getLocalPort() == 52637, "Discovery must rebind after restart");
        passed++;
    }
    private void send(DatagramSocket socket, String json) throws Exception {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(bytes, bytes.length, InetAddress.getLoopbackAddress(), 52637));
        Thread.sleep(60);
    }
    private void transfer(String name, boolean resume, boolean oversized, boolean complete) throws Exception {
        File dir = new File(root, name);
        check(dir.mkdirs(), "Create isolated fixture");
        Prefs.INSTANCE.setDownloadPath(Prefs.INSTANCE.defaultPreference(getTargetContext()), dir.getAbsolutePath());
        byte[] first = {11,12,13}, second = {21,22,23,24,25,26};
        File fileA = new File(dir, "录像/a.mp4"), fileB = new File(dir, "录像/b.mp4");
        check(fileA.getParentFile().mkdirs(), "Create nested fixture");
        if(resume) Files.write(fileA.toPath(), new byte[]{11});
        if(oversized) Files.write(fileA.toPath(), new byte[20]);
        if(complete) { Files.write(fileA.toPath(), first); Files.write(fileB.toPath(), second); }
        try (Peer peer = new Peer()) {
            peer.handshake("{\"device_name\":\"相机\",\"device_type\":\"Linux\",\"encrypt\":false,\"files\":[{\"name\":\"录像/a.mp4\",\"size\":3},{\"name\":\"录像/b.mp4\",\"size\":6}],\"firmware_version\":\"future\"}");
            JSONObject accepted = new JSONObject(peer.readFrame());
            check("ok".equals(accepted.getString("r")), "File list accepted");
            long offset = accepted.getJSONArray("files").getJSONObject(0).getLong("start");
            check(offset == (complete ? 3 : resume ? 1 : 0), "Correct validated resume offset");
            if(!complete) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bytes.write(first, (int)offset, first.length - (int)offset);
                bytes.write(second);
                peer.writeFrame(bytes.toByteArray()); // one packet crosses file boundary
            }
            check("ok".equals(new JSONObject(peer.readFrame()).getString("r")), "Transfer confirmed after handshake");
            check(peer.finished.await(5, TimeUnit.SECONDS) && peer.error == null && peer.success, "Successful completion");
            check(Arrays.equals(first, Files.readAllBytes(fileA.toPath())), "First video bytes match");
            check(Arrays.equals(second, Files.readAllBytes(fileB.toPath())), "Second video bytes match without repeated packet prefix");
        }
        passed++;
    }
    private void rejectedFiles() throws Exception {
        try(Peer peer = new Peer()) {
            peer.handshake("{\"device_name\":\"相机\",\"device_type\":\"Linux\",\"encrypt\":false,\"files\":[]}");
            check(peer.finished.await(5, TimeUnit.SECONDS) && peer.error != null && !peer.success, "Empty list reports failure");
        }
        Files.write(new File(root, "blocked").toPath(), new byte[]{1});
        Prefs.INSTANCE.setDownloadPath(Prefs.INSTANCE.defaultPreference(getTargetContext()), root.getAbsolutePath());
        try(Peer peer = new Peer()) {
            peer.handshake("{\"device_name\":\"相机\",\"device_type\":\"Linux\",\"encrypt\":false,\"files\":[{\"name\":\"blocked/video.mp4\",\"size\":3}]}");
            check(peer.finished.await(5, TimeUnit.SECONDS) && peer.error != null && !peer.success, "Disk write error reports failure without crashing");
        }
        passed++;
    }
    private void largeFileTail() throws Exception {
        File dir = new File(root, "large-tail");
        check(dir.mkdirs(), "Create large-file fixture");
        Prefs.INSTANCE.setDownloadPath(Prefs.INSTANCE.defaultPreference(getTargetContext()), dir.getAbsolutePath());
        long size = 3_770_000_000L;
        byte[] tail = new byte[64000];
        for (int i = 0; i < tail.length; i++) tail[i] = (byte) i;
        File large = new File(dir, "large.mp4");
        try (RandomAccessFile file = new RandomAccessFile(large, "rw")) { file.setLength(size - tail.length); }
        try (Peer peer = new Peer()) {
            peer.handshake("{\"device_name\":\"相机\",\"device_type\":\"Linux\",\"encrypt\":false,\"files\":[{\"name\":\"large.mp4\",\"size\":" + size + "}]}");
            long offset = new JSONObject(peer.readFrame()).getJSONArray("files").getJSONObject(0).getLong("start");
            check(offset == size - tail.length && offset > Integer.MAX_VALUE, "3.51 GB resume offset remains 64-bit");
            peer.writeFrame(tail);
            check("ok".equals(new JSONObject(peer.readFrame()).getString("r")), "Large file completion acknowledged");
            check(peer.finished.await(5, TimeUnit.SECONDS) && peer.success && peer.error == null, "Large file completes without crashing");
        }
        check(large.length() == size, "Large file has exact final size");
        try (RandomAccessFile file = new RandomAccessFile(large, "r")) {
            byte[] actual = new byte[tail.length];
            file.seek(size - tail.length); file.readFully(actual);
            check(Arrays.equals(tail, actual), "Final 64 KB persisted at the correct large-file offset");
        }
        check(large.delete(), "Remove sparse fixture");
        passed++;
    }
    private void sustainedTransfer() throws Exception {
        File dir = new File(root, "stress");
        check(dir.mkdirs(), "Create sustained-transfer fixture");
        Prefs.INSTANCE.setDownloadPath(Prefs.INSTANCE.defaultPreference(getTargetContext()), dir.getAbsolutePath());
        byte[] bytes = new byte[64000];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) i;
        java.security.MessageDigest expected = java.security.MessageDigest.getInstance("SHA-256");
        long started = System.currentTimeMillis();
        try (Peer peer = new Peer()) {
            peer.remote.setSoTimeout(120000);
            peer.handshake("{\"device_name\":\"相机\",\"device_type\":\"Linux\",\"encrypt\":false,\"files\":[{\"name\":\"stress.mp4\",\"size\":" + stressBytes + "}]}");
            check(new JSONObject(peer.readFrame()).getJSONArray("files").getJSONObject(0).getLong("start") == 0, "Stress download starts empty");
            for (long sent = 0; sent < stressBytes;) {
                int count = (int) Math.min(bytes.length, stressBytes - sent);
                peer.output.writeShort(count); peer.output.write(bytes, 0, count);
                expected.update(bytes, 0, count); sent += count;
            }
            peer.output.flush();
            check("ok".equals(new JSONObject(peer.readFrame()).getString("r")), "Stress transfer acknowledged");
            check(peer.finished.await(5, TimeUnit.SECONDS) && peer.success && peer.error == null, "Stress transfer completed");
        }
        File file = new File(dir, "stress.mp4");
        check(file.length() == stressBytes, "Stress file size matches");
        java.security.MessageDigest actual = java.security.MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(file)) {
            int count; while ((count = input.read(bytes)) != -1) actual.update(bytes, 0, count);
        }
        check(Arrays.equals(expected.digest(), actual.digest()), "Every byte of the multi-GB download matches");
        System.out.println("STRESS PASS: " + stressBytes + " bytes including SHA-256 verification in " + (System.currentTimeMillis() - started) + " ms");
        check(file.delete(), "Remove stress fixture");
        passed++;
    }
    private static class Peer implements AutoCloseable, IFileSessionDelegate {
        final Socket remote;
        final DataInputStream input;
        final DataOutputStream output;
        final FileDual session;
        final CountDownLatch finished = new CountDownLatch(1);
        volatile String error;
        volatile boolean success;
        Peer() throws Exception {
            try(ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
                Socket receiver = new Socket(InetAddress.getLoopbackAddress(), listener.getLocalPort());
                remote = listener.accept(); remote.setSoTimeout(5000);
                input = new DataInputStream(remote.getInputStream()); output = new DataOutputStream(remote.getOutputStream());
                session = new FileDual(receiver, ""); session.setDelegate(this); session.start();
            }
        }
        void handshake(String json) throws Exception {
            byte[] key = new byte[32]; input.readFully(key);
            output.write(key, 0, 7); output.flush(); Thread.sleep(60);
            output.write(key, 7, key.length - 7); output.flush();
            writeFrame(json.getBytes(StandardCharsets.UTF_8));
        }
        void writeFrame(byte[] bytes) throws Exception { output.writeShort(bytes.length); output.write(bytes); output.flush(); }
        String readFrame() throws Exception { byte[] bytes = new byte[input.readUnsignedShort()]; input.readFully(bytes); return new String(bytes, StandardCharsets.UTF_8); }
        @Override public void onErrorOccurred(FileSession session, String message) { error = message; finished.countDown(); }
        @Override public void onSenderEnded(FileSession session) { success = true; finished.countDown(); }
        @Override public void onDualEnded(FileSession session) { success = true; finished.countDown(); }
        @Override public void onUpdateProgress(FileSession session, double progress) { }
        @Override public void close() throws Exception { session.abort(); remote.close(); }
    }
    private static Object field(Object owner, String name) throws Exception { Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner); }
    private static void check(boolean value, String message) { if(!value) throw new AssertionError(message); }
    private static void delete(File file) { File[] files = file.listFiles(); if(files != null) for(File child : files) delete(child); file.delete(); }
}
