package com.farmconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Checks a Google ID token (sent by the Android app) with Google and returns who it belongs to.
 * Set app.google.client-id to your OAuth *Web application* client ID (the same one the app requests the token for).
 */
@Service
public class GoogleAuthService {
    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    public record Identity(String email, String name) {}

    @Value("${app.google.client-id:}") private String clientId;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    public Identity verify(String idToken) {
        if (clientId == null || clientId.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not set up on the server yet");
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(
                    "https://oauth2.googleapis.com/tokeninfo?id_token=" + URLEncoder.encode(idToken, StandardCharsets.UTF_8)))
                    .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google sign-in failed. Please try again.");
            JsonNode n = mapper.readTree(res.body());
            String iss = n.path("iss").asText("");
            String email = n.path("email").asText("");
            boolean issOk = iss.equals("accounts.google.com") || iss.equals("https://accounts.google.com");
            boolean verified = "true".equalsIgnoreCase(n.path("email_verified").asText(""));
            boolean expired = n.path("exp").asLong(0) * 1000L < System.currentTimeMillis();
            if (!clientId.equals(n.path("aud").asText("")) || !issOk || !verified || expired || email.isBlank())
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google sign-in failed. Please try again.");
            String name = n.path("name").asText("");
            if (name.isBlank()) name = email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length());
            return new Identity(email, name);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not reach Google");
        } catch (Exception e) {
            log.error("Google token check failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not reach Google");
        }
    }
}
