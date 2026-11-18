package com.xh.hotme.storage;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.UploadPartResult;

import org.junit.Test;

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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Verifies the OBS SDK's real upload transport against the upgraded OkHttp/Okio. */
public class ObsTransportCompatibilityTest {
    @Test(timeout = 20000)
    public void multipartUploadSendsBytesAndReadsEtag() throws Exception {
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
                    while (!(line = readLine(input)).isEmpty()) {
                        if (line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:")) {
                            length = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                        }
                    }
                    assertEquals(payload.length, length);
                    byte[] body = new byte[length];
                    int offset = 0;
                    while (offset < length) {
                        int count = input.read(body, offset, length - offset);
                        if (count < 0) throw new java.io.EOFException("Incomplete upload body");
                        offset += count;
                    }
                    assertArrayEquals(payload, body);
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
            // Anonymous requests avoid Android Base64 stubs in JVM tests.
            // This test covers transport and response parsing, not cloud authentication.
            try (ObsClient client = new ObsClient(config)) {
                UploadPartRequest request = new UploadPartRequest("test-bucket", "match.mp4");
                request.setUploadId("local-upload");
                request.setPartNumber(1);
                request.setPartSize((long) payload.length);
                request.setInput(new ByteArrayInputStream(payload));
                UploadPartResult result = client.uploadPart(request);
                assertEquals("\"test-part-etag\"", result.getEtag());
                assertEquals(1, result.getPartNumber());
            }
            String requestLine = received.get(10, TimeUnit.SECONDS);
            assertTrue(requestLine.startsWith("PUT /test-bucket/match.mp4?"));
            assertTrue(requestLine.contains("partNumber=1"));
            assertTrue(requestLine.contains("uploadId=local-upload"));
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
