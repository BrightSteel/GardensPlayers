package com.gardensmc.gardensplayers.resourceworld;

import org.w3c.dom.Element;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Just enough of the S3 API for Cloudflare R2 (or any S3-compatible store): list, download, delete and multipart
 * upload, signed with AWS Signature Version 4. Small enough to not need the AWS SDK and its two dozen libraries.
 * Blocking: call it off the main thread.
 */
public final class S3Store {

    /** An object in the bucket. */
    public record StoredObject(String key, long size) {
    }

    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final String EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    /** Bytes per uploaded part (R2 wants all but the last the same size, at least 5 MiB). */
    static final int PART_SIZE = 64 * 1024 * 1024;

    private final URI endpoint;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;
    private final String region;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    private final int partSize;

    /** @param endpoint e.g. {@code https://<account id>.r2.cloudflarestorage.com}; R2's region is {@code auto} */
    public S3Store(String endpoint, String bucket, String accessKey, String secretKey, String region) {
        this(endpoint, bucket, accessKey, secretKey, region, PART_SIZE);
    }

    S3Store(String endpoint, String bucket, String accessKey, String secretKey, String region, int partSize) {
        this.partSize = partSize;
        this.endpoint = URI.create(endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint);
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
    }

    // ---- operations ----

    public List<StoredObject> list(String prefix) throws IOException {
        var objects = new ArrayList<StoredObject>();
        String token = null;
        do {
            var query = new TreeMap<String, String>();
            query.put("list-type", "2");
            query.put("prefix", prefix);
            if (token != null) query.put("continuation-token", token);
            var body = send("GET", "", query, Map.of(), null, HttpResponse.BodyHandlers.ofByteArray()).body();
            var doc = parse(body);
            var contents = doc.getElementsByTagName("Contents");
            for (int i = 0; i < contents.getLength(); i++) {
                var element = (Element) contents.item(i);
                objects.add(new StoredObject(text(element, "Key"), Long.parseLong(text(element, "Size"))));
            }
            token = "true".equals(text(doc.getDocumentElement(), "IsTruncated")) ? text(doc.getDocumentElement(), "NextContinuationToken") : null;
        } while (token != null);
        return objects;
    }

    /** The object's bytes as a stream; close it when done. */
    public InputStream download(String key) throws IOException {
        return send("GET", key, Map.of(), Map.of(), null, HttpResponse.BodyHandlers.ofInputStream()).body();
    }

    public void delete(String key) throws IOException {
        send("DELETE", key, Map.of(), Map.of(), null, HttpResponse.BodyHandlers.discarding());
    }

    /** Something that writes an object's content to the stream it's given. */
    public interface Writer {
        void writeTo(OutputStream out) throws IOException;
    }

    /**
     * Uploads what {@code writer} writes, in {@link #PART_SIZE} parts as it's written (so nothing the size of the whole
     * object is held in memory or on disk). If anything fails, the unfinished upload is aborted.
     */
    public void upload(String key, Writer writer) throws IOException {
        var created = parse(send("POST", key, Map.of("uploads", ""), Map.of(), new byte[0], HttpResponse.BodyHandlers.ofByteArray()).body());
        var uploadId = text(created.getDocumentElement(), "UploadId");
        var etags = new ArrayList<String>();
        try {
            try (var parts = new PartStream(key, uploadId, etags)) {
                writer.writeTo(parts);
            }
            var xml = new StringBuilder("<CompleteMultipartUpload>");
            for (int i = 0; i < etags.size(); i++) {
                xml.append("<Part><PartNumber>").append(i + 1).append("</PartNumber><ETag>").append(etags.get(i)).append("</ETag></Part>");
            }
            xml.append("</CompleteMultipartUpload>");
            var done = send("POST", key, Map.of("uploadId", uploadId), Map.of(), xml.toString().getBytes(StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofByteArray());
            // a 200 can still carry an error document
            if (new String(done.body(), StandardCharsets.UTF_8).contains("<Error>")) {
                throw new IOException("Completing the upload of " + key + " failed: " + new String(done.body(), StandardCharsets.UTF_8));
            }
        } catch (IOException | RuntimeException e) {
            try {
                send("DELETE", key, Map.of("uploadId", uploadId), Map.of(), null, HttpResponse.BodyHandlers.discarding());
            } catch (IOException abortFailed) {
                e.addSuppressed(abortFailed);
            }
            throw e;
        }
    }

    // buffers PART_SIZE bytes, then uploads them as the next part
    private final class PartStream extends OutputStream {
        private final String key;
        private final String uploadId;
        private final List<String> etags;
        private final byte[] buffer = new byte[partSize];
        private int filled;

        PartStream(String key, String uploadId, List<String> etags) {
            this.key = key;
            this.uploadId = uploadId;
            this.etags = etags;
        }

        @Override
        public void write(int b) throws IOException {
            if (filled == buffer.length) flushPart();
            buffer[filled++] = (byte) b;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            while (length > 0) {
                if (filled == buffer.length) flushPart();
                int n = Math.min(length, buffer.length - filled);
                System.arraycopy(bytes, offset, buffer, filled, n);
                filled += n;
                offset += n;
                length -= n;
            }
        }

        private void flushPart() throws IOException {
            var body = java.util.Arrays.copyOf(buffer, filled);
            var query = Map.of("partNumber", String.valueOf(etags.size() + 1), "uploadId", uploadId);
            var response = send("PUT", key, query, Map.of(), body, HttpResponse.BodyHandlers.discarding());
            etags.add(response.headers().firstValue("etag").orElseThrow(() -> new IOException("No ETag for part " + (etags.size() + 1))));
            filled = 0;
        }

        @Override
        public void close() throws IOException {
            // an empty object still needs its one part
            if (filled > 0 || etags.isEmpty()) flushPart();
        }
    }

    // ---- signing and sending ----

    private <T> HttpResponse<T> send(String method, String key, Map<String, String> query, Map<String, String> extraHeaders,
                                     byte[] body, HttpResponse.BodyHandler<T> handler) throws IOException {
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var path = "/" + bucket + (key.isEmpty() ? "/" : "/" + key);
        var payloadHash = body == null ? EMPTY_HASH : sha256Hex(body);
        var headers = new TreeMap<String, String>(extraHeaders);
        headers.put("host", endpoint.getHost() + (endpoint.getPort() > 0 ? ":" + endpoint.getPort() : ""));
        headers.put("x-amz-content-sha256", payloadHash);
        headers.put("x-amz-date", AMZ_DATE.format(now));
        var authorization = authorization(method, path, new TreeMap<>(query), headers, payloadHash, now);

        var canonicalQuery = canonicalQuery(new TreeMap<>(query));
        var uri = URI.create(endpoint + encodePath(path) + (canonicalQuery.isEmpty() ? "" : "?" + canonicalQuery));
        var request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(10))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body))
                .header("Authorization", authorization);
        headers.forEach((name, value) -> {
            if (!name.equals("host")) request.header(name, value);
        });
        try {
            var response = http.send(request.build(), handler);
            if (response.statusCode() / 100 != 2) {
                var detail = response.body() instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8)
                        : response.body() instanceof InputStream in ? new String(in.readAllBytes(), StandardCharsets.UTF_8) : "";
                throw new IOException(method + " " + path + " failed with HTTP " + response.statusCode() + ": " + detail);
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        }
    }

    /** The Authorization header for a request (AWS Signature Version 4, service s3). */
    String authorization(String method, String path, TreeMap<String, String> query, TreeMap<String, String> headers,
                         String payloadHash, ZonedDateTime now) {
        var date = now.format(DateTimeFormatter.BASIC_ISO_DATE).substring(0, 8);
        var amzDate = AMZ_DATE.format(now);
        var canonicalHeaders = new StringBuilder();
        for (var header : headers.entrySet()) {
            canonicalHeaders.append(header.getKey()).append(':').append(header.getValue().trim()).append('\n');
        }
        var signedHeaders = String.join(";", headers.keySet());
        var canonicalRequest = method + "\n" + encodePath(path) + "\n" + canonicalQuery(query) + "\n"
                + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;
        var scope = date + "/" + region + "/s3/aws4_request";
        var stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + scope + "\n" + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        byte[] key = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        key = hmac(key, region);
        key = hmac(key, "s3");
        key = hmac(key, "aws4_request");
        var signature = HexFormat.of().formatHex(hmac(key, stringToSign));
        return "AWS4-HMAC-SHA256 Credential=" + accessKey + "/" + scope + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
    }

    private static String canonicalQuery(TreeMap<String, String> query) {
        var parts = new ArrayList<String>();
        query.forEach((name, value) -> parts.add(encode(name) + "=" + encode(value)));
        return String.join("&", parts);
    }

    // each segment encoded, the slashes kept
    private static String encodePath(String path) {
        var segments = path.split("/", -1);
        var out = new ArrayList<String>();
        for (var segment : segments) out.add(encode(segment));
        return String.join("/", out);
    }

    // RFC 3986: everything but A-Z a-z 0-9 - _ . ~ is escaped
    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static org.w3c.dom.Document parse(byte[] xml) throws IOException {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new IOException("Couldn't read the storage's reply", e);
        }
    }

    private static String text(Element parent, String tag) {
        var nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }
}
