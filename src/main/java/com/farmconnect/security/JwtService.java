package com.farmconnect.security;

import com.farmconnect.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;
    private final long adminExpirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs,
                      @Value("${app.jwt.admin-expiration-ms:28800000}") long adminExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.adminExpirationMs = adminExpirationMs;
    }

    public String generate(User user) {
        // administrators get a shorter session (8 hours by default); a stolen token stops working sooner
        boolean admin = user.getRole() == com.farmconnect.model.Role.ADMIN || user.getRole() == com.farmconnect.model.Role.COUNTY_ADMIN;
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("tv", user.getTokenVersion() == null ? 0 : user.getTokenVersion())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + (admin ? adminExpirationMs : expirationMs)))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    /** The sign-out counter the token was issued with (0 for tokens issued before this existed). */
    public int extractTokenVersion(String token) {
        Object v = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("tv");
        return v instanceof Number n ? n.intValue() : 0;
    }
}
