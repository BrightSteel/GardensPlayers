package com.gardensmc.gardensplayers.resourceworld;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checked against the worked examples in AWS's "Signature Calculations for the Authorization Header" for S3. */
class S3StoreTest {

    private static final S3Store STORE = new S3Store("https://examplebucket.s3.amazonaws.com", "examplebucket",
            "AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", "us-east-1");
    private static final ZonedDateTime WHEN = ZonedDateTime.of(2013, 5, 24, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final String EMPTY = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private static TreeMap<String, String> headers(String... pairs) {
        var map = new TreeMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) map.put(pairs[i], pairs[i + 1]);
        return map;
    }

    @Test
    void getObject() {
        // the example signs a virtual-hosted request: no bucket in the path
        var auth = STORE.authorization("GET", "/test.txt", new TreeMap<>(),
                headers("host", "examplebucket.s3.amazonaws.com", "range", "bytes=0-9",
                        "x-amz-content-sha256", EMPTY, "x-amz-date", "20130524T000000Z"), EMPTY, WHEN);
        assertTrue(auth.endsWith("Signature=f0e8bdb87c964420e857bd35b5d6ed310bd44f0170aba48dd91039c6036bdb41"), auth);
    }

    @Test
    void listObjects() {
        var query = new TreeMap<String, String>();
        query.put("max-keys", "2");
        query.put("prefix", "J");
        var auth = STORE.authorization("GET", "/", query,
                headers("host", "examplebucket.s3.amazonaws.com",
                        "x-amz-content-sha256", EMPTY, "x-amz-date", "20130524T000000Z"), EMPTY, WHEN);
        assertTrue(auth.endsWith("Signature=34b48302e7b5fa45bde8084f4b7868a86f0a534bc59db6670ed5711ef69dc6f7"), auth);
    }

    @Test
    void encodesLikeS3() {
        assertEquals("resource_world.20261007-150045.zip", S3Store.encode("resource_world.20261007-150045.zip"));
        assertEquals("a%20b%2Fc~", S3Store.encode("a b/c~"));
    }
}
