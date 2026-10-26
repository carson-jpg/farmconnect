package com.farmconnect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/** Makes short-lived signed links for private crop photos, so the app's image loader can show them without a login header. */
@Component
public class CropPhotoSigner {
    private static final long LIFETIME_SECONDS = 6 * 3600;
    private final byte[] key;

    public CropPhotoSigner(@Value("${app.jwt.secret}") String secret) {
        this.key = ("crop-photo:" + secret).getBytes(StandardCharsets.UTF_8);
    }

    /** "/api/files/crop/12/abc.jpg?e=1760000000&s=..." for a stored path like "12/abc.jpg". */
    public String url(String relative) { return url(relative, Instant.now().getEpochSecond() + LIFETIME_SECONDS); }

    String url(String relative, long expires) {
        return "/api/files/crop/" + relative + "?e=" + expires + "&s=" + sign(relative, expires);
    }

    public boolean valid(String relative, long expires, String sig) {
        if (sig == null || expires < Instant.now().getEpochSecond()) return false;
        return MessageDigest.isEqual(sign(relative, expires).getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String relative, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] out = mac.doFinal((relative + "|" + expires).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
