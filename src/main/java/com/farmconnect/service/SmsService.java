package com.farmconnect.service;

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
 * app.sms.provider=log            -> prints the SMS in the server console (development).
 * app.sms.provider=africastalking -> sends a real SMS through Africa's Talking.
 */
@Service
public class SmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${app.sms.provider:log}") private String provider;
    @Value("${app.sms.at-username:sandbox}") private String username;
    @Value("${app.sms.at-api-key:}") private String apiKey;

    public void send(String phoneE164, String message) {
        if (!"africastalking".equalsIgnoreCase(provider)) {
            log.warn("[SMS-DEV] to {}: {}", phoneE164, message);
            return;
        }
        try {
            String base = "sandbox".equals(username) ? "https://api.sandbox.africastalking.com" : "https://api.africastalking.com";
            String body = "username=" + enc(username) + "&to=" + enc(phoneE164) + "&message=" + enc(message);
            HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/version1/messaging"))
                    .header("apiKey", apiKey)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300) throw new IllegalStateException("SMS provider returned " + res.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send SMS");
        } catch (Exception e) {
            log.error("SMS failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send SMS");
        }
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}