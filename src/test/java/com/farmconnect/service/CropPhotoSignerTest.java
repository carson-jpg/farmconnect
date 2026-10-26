package com.farmconnect.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CropPhotoSignerTest {
    private final CropPhotoSigner signer = new CropPhotoSigner("a-long-test-secret-value-1234567890");

    private static String param(String url, String name) {
        for (String p : url.substring(url.indexOf('?') + 1).split("&")) if (p.startsWith(name + "=")) return p.substring(name.length() + 1);
        return null;
    }

    @Test void freshLinkIsValidForItsOwnFile() {
        String url = signer.url("12/abc.jpg");
        assertTrue(url.startsWith("/api/files/crop/12/abc.jpg?e="));
        assertTrue(signer.valid("12/abc.jpg", Long.parseLong(param(url, "e")), param(url, "s")));
    }

    @Test void linkDoesNotOpenAnotherFile() {
        String url = signer.url("12/abc.jpg");
        assertFalse(signer.valid("12/other.jpg", Long.parseLong(param(url, "e")), param(url, "s")));
        assertFalse(signer.valid("13/abc.jpg", Long.parseLong(param(url, "e")), param(url, "s")));
    }

    @Test void expiredOrTamperedLinksAreRefused() {
        long past = Instant.now().getEpochSecond() - 5;
        String url = signer.url("12/abc.jpg", past);
        assertFalse(signer.valid("12/abc.jpg", past, param(url, "s")));
        String good = signer.url("12/abc.jpg");
        long exp = Long.parseLong(param(good, "e"));
        assertFalse(signer.valid("12/abc.jpg", exp + 3600, param(good, "s")));
        assertFalse(signer.valid("12/abc.jpg", exp, "AAAA"));
        assertFalse(signer.valid("12/abc.jpg", exp, null));
    }

    @Test void anotherSecretCannotForgeALink() {
        CropPhotoSigner other = new CropPhotoSigner("a-different-secret-value-0987654321");
        String url = other.url("12/abc.jpg");
        assertFalse(signer.valid("12/abc.jpg", Long.parseLong(param(url, "e")), param(url, "s")));
    }
}
