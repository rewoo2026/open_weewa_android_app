package com.xh.hotme.account;

import android.app.Instrumentation;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.xh.hotme.http.DeviceHttpManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises the actual HTTP serializer and response decoder against a local fake camera. */
final class CameraTokenHttpIntegration {
    static int run(Instrumentation instrumentation) throws Exception {
        String originalHost = DeviceHttpManager.getHttpUrl("set_token").host();
        try (ServerSocket server = new ServerSocket(38888)) {
            server.setSoTimeout(5000);
            DeviceHttpManager.setHttpHost("127.0.0.1");
            String[] responses = {"{\"c\":\"set_token\",\"r\":\"ok\"}",
                    "{\"c\":\"set_token\",\"r\":\"fail\"}", "{\"c\":\"set_token\"}"};
            for (int i = 0; i < responses.length; i++) {
                final String response = responses[i];
                final String token = "synthetic-http-token-" + i;
                final boolean expected = i == 0;
                CountDownLatch finished = new CountDownLatch(2);
                AtomicReference<Throwable> failure = new AtomicReference<>();
                Thread camera = new Thread(() -> {
                    try (Socket socket = server.accept()) {
                        socket.setSoTimeout(5000);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                socket.getInputStream(), StandardCharsets.UTF_8));
                        require("POST /set_token HTTP/1.1".equals(reader.readLine()), "Wrong camera endpoint");
                        int length = 0;
                        String header;
                        while ((header = reader.readLine()) != null && !header.isEmpty()) {
                            if (header.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:")) {
                                length = Integer.parseInt(header.substring(header.indexOf(':') + 1).trim());
                            }
                        }
                        require(length > 0, "Missing token request body");
                        char[] body = new char[length];
                        int offset = 0;
                        while (offset < length) {
                            int count = reader.read(body, offset, length - offset);
                            require(count > 0, "Incomplete request body");
                            offset += count;
                        }
                        JsonObject json = new Gson().fromJson(new String(body), JsonObject.class);
                        require("set_token".equals(json.get("c").getAsString()), "Wrong command");
                        require(token.equals(json.get("t").getAsString()), "Wrong token field");
                        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                        String reply = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n"
                                + "Connection: close\r\nContent-Length: " + bytes.length + "\r\n\r\n";
                        socket.getOutputStream().write(reply.getBytes(StandardCharsets.UTF_8));
                        socket.getOutputStream().write(bytes);
                        socket.getOutputStream().flush();
                    } catch (Throwable error) {
                        failure.set(error);
                    } finally {
                        finished.countDown();
                    }
                }, "fake-token-camera");
                camera.start();
                DeviceHttpManager.sendToken(token, success -> {
                    if (success != expected) failure.set(new AssertionError("Incorrect token result"));
                    finished.countDown();
                });
                require(finished.await(10, TimeUnit.SECONDS), "Camera HTTP callback timed out");
                camera.join(1000);
                if (failure.get() != null) throw new AssertionError("Camera HTTP token exchange", failure.get());
                Bundle progress = new Bundle();
                progress.putString("stream", "PASS: camera HTTP token response " + i + "\n");
                instrumentation.sendStatus(0, progress);
            }
            return responses.length;
        } finally {
            DeviceHttpManager.setHttpHost(originalHost);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
