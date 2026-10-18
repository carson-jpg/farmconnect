package com.farmconnect.security;

import jakarta.servlet.http.HttpServletRequest;

/** The caller's address. Behind a proxy (Render, nginx) the real client is the last hop that proxy added to X-Forwarded-For. */
public final class ClientIp {
    private ClientIp() {}

    public static String of(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            String last = parts[parts.length - 1].trim();
            if (!last.isEmpty() && last.length() <= 45) return last;
        }
        return req.getRemoteAddr();
    }
}
