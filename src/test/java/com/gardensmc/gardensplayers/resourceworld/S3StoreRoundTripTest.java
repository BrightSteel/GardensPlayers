package com.gardensmc.gardensplayers.resourceworld;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S3Store against a small in-memory S3 (path-style, as R2 is used): multipart upload, list, download, delete. */
class S3StoreRoundTripTest {

    private HttpServer server;
    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();
    private final Map<String, TreeMap<Integer, byte[]>> uploads = new ConcurrentHashMap<>();
    private int parts;
    @TempDir
    Path temp;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestHeaders().getFirst("Authorization").startsWith("AWS4-HMAC-SHA256 Credential=key/")) {
            reply(exchange, 403, "");
            return;
        }
        var path = exchange.getRequestURI().getPath(); // /bucket/key
        var key = path.substring("/bucket/".length());
        var query = new HashMap<String, String>();
        var raw = exchange.getRequestURI().getRawQuery();
        if (raw != null) for (var pair : raw.split("&")) {
            var kv = pair.split("=", 2);
            query.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        var body = exchange.getRequestBody().readAllBytes();
        switch (exchange.getRequestMethod()) {
            case "POST" -> {
                if (query.containsKey("uploads")) {
                    uploads.put("u1", new TreeMap<>());
                    reply(exchange, 200, "<InitiateMultipartUploadResult><UploadId>u1</UploadId></InitiateMultipartUploadResult>");
                } else {
                    var out = new ByteArrayOutputStream();
                    for (var part : uploads.remove(query.get("uploadId")).values()) out.write(part);
                    objects.put(key, out.toByteArray());
                    reply(exchange, 200, "<CompleteMultipartUploadResult/>");
                }
            }
            case "PUT" -> {
                uploads.get(query.get("uploadId")).put(Integer.parseInt(query.get("partNumber")), body);
                parts++;
                exchange.getResponseHeaders().add("ETag", "\"etag" + query.get("partNumber") + "\"");
                reply(exchange, 200, "");
            }
            case "GET" -> {
                if ("2".equals(query.get("list-type"))) {
                    var xml = new StringBuilder("<ListBucketResult><IsTruncated>false</IsTruncated>");
                    objects.forEach((k, v) -> {
                        if (k.startsWith(query.get("prefix"))) xml.append("<Contents><Key>").append(k).append("</Key><Size>").append(v.length).append("</Size></Contents>");
                    });
                    reply(exchange, 200, xml.append("</ListBucketResult>").toString());
                } else if (objects.containsKey(key)) {
                    exchange.sendResponseHeaders(200, objects.get(key).length);
                    exchange.getResponseBody().write(objects.get(key));
                    exchange.close();
                } else {
                    reply(exchange, 404, "<Error><Code>NoSuchKey</Code></Error>");
                }
            }
            case "DELETE" -> {
                if (query.containsKey("uploadId")) uploads.remove(query.get("uploadId"));
                else objects.remove(key);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            }
            default -> reply(exchange, 405, "");
        }
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Test
    void uploadListDownloadDelete() throws IOException {
        var world = temp.resolve("resource_world.20261007-150045");
        Files.createDirectories(world.resolve("region"));
        var region = new byte[2_000_000];
        new java.util.Random(7).nextBytes(region); // incompressible, like real region files mostly are
        Files.write(world.resolve("region/r.0.0.mca"), region);
        Files.writeString(world.resolve("paper-world.yml"), "_version: 31\n");

        // small parts, so this one world goes up in several
        var store = new S3Store("http://127.0.0.1:" + server.getAddress().getPort(), "bucket", "key", "secret", "auto", 512 * 1024);
        var name = "resource-worlds/resource_world.20261007-150045.zip";
        store.upload(name, out -> WorldArchive.pack(world, out));
        assertTrue(parts >= 4, "uploaded in parts: " + parts);

        var listed = store.list("resource-worlds/");
        assertEquals(1, listed.size());
        assertEquals(name, listed.getFirst().key());

        var copy = temp.resolve("copy");
        try (var in = store.download(name)) {
            WorldArchive.unpack(in, copy);
        }
        assertArrayEquals(region, Files.readAllBytes(copy.resolve("region/r.0.0.mca")));

        store.delete(name);
        assertTrue(store.list("resource-worlds/").isEmpty());
    }
}
