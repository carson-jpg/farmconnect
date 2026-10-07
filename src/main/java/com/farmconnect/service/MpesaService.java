package com.farmconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Safaricom Daraja: STK push (Lipa na M-Pesa Online). All secrets come from environment variables. */
@Service
public class MpesaService {
    private static final Logger log = LoggerFactory.getLogger(MpesaService.class);

    @Value("${mpesa.env:sandbox}") private String env;
    @Value("${mpesa.consumer-key:}") private String consumerKey;
    @Value("${mpesa.consumer-secret:}") private String consumerSecret;
    @Value("${mpesa.shortcode:}") private String shortcode;
    @Value("${mpesa.party-b:}") private String partyB;
    @Value("${mpesa.passkey:}") private String passkey;
    @Value("${mpesa.transaction-type:CustomerPayBillOnline}") private String transactionType;
    @Value("${mpesa.callback-url:}") private String callbackUrl;
    @Value("${mpesa.callback-secret:}") private String callbackSecret;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private String token;
    private Instant tokenExpiry = Instant.EPOCH;

    private String base() {
        return "production".equalsIgnoreCase(env) ? "https://api.safaricom.co.ke" : "https://sandbox.safaricom.co.ke";
    }

    private boolean configured() {
        return !consumerKey.isBlank() && !consumerSecret.isBlank() && !shortcode.isBlank()
                && !passkey.isBlank() && !callbackUrl.isBlank() && !callbackSecret.isBlank();
    }

    /** True only if the secret in the callback URL matches ours. */
    public boolean callbackSecretOk(String given) {
        if (callbackSecret.isBlank() || given == null) return false;
        return MessageDigest.isEqual(callbackSecret.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }

    /** 0712345678, 0112345678, +254712345678, 254712345678 -> 254712345678 */
    public static String normalizePhone(String raw) {
        String p = raw == null ? "" : raw.replaceAll("[\\s\\-+]", "");
        if (p.startsWith("0")) p = "254" + p.substring(1);
        if (!p.matches("254\\d{9}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid Safaricom number like 07XXXXXXXX");
        return p;
    }

    private synchronized String token() throws Exception {
        if (token != null && Instant.now().isBefore(tokenExpiry)) return token;
        String basic = Base64.getEncoder().encodeToString((consumerKey + ":" + consumerSecret).getBytes(StandardCharsets.UTF_8));
        HttpRequest req = HttpRequest.newBuilder(URI.create(base() + "/oauth/v1/generate?grant_type=client_credentials"))
                .header("Authorization", "Basic " + basic).GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new IllegalStateException("Daraja token failed: HTTP " + res.statusCode());
        JsonNode j = mapper.readTree(res.body());
        token = j.path("access_token").asText();
        tokenExpiry = Instant.now().plusSeconds(Math.max(60, j.path("expires_in").asLong(3599) - 60));
        return token;
    }

    /** Sends the payment prompt to the customer's phone. Returns the CheckoutRequestID. */
    public String stkPush(String phone254, BigDecimal amount, String accountRef, String desc) {
        if (!configured())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "M-Pesa payments are not set up yet");
        try {
            String ts = LocalDateTime.now(ZoneId.of("Africa/Nairobi")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = Base64.getEncoder().encodeToString((shortcode + passkey + ts).getBytes(StandardCharsets.UTF_8));
            long amt = Math.max(1, amount.setScale(0, RoundingMode.CEILING).longValue());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("BusinessShortCode", shortcode);
            body.put("Password", password);
            body.put("Timestamp", ts);
            body.put("TransactionType", transactionType);
            body.put("Amount", amt);
            body.put("PartyA", phone254);
            body.put("PartyB", partyB.isBlank() ? shortcode : partyB);
            body.put("PhoneNumber", phone254);
            body.put("CallBackURL", callbackUrl.replaceAll("/+$", "") + "/" + callbackSecret);
            body.put("AccountReference", accountRef);
            body.put("TransactionDesc", desc);

            HttpRequest req = HttpRequest.newBuilder(URI.create(base() + "/mpesa/stkpush/v1/processrequest"))
                    .header("Authorization", "Bearer " + token())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode j = mapper.readTree(res.body());
            if (res.statusCode() != 200 || !"0".equals(j.path("ResponseCode").asText())) {
                log.error("STK push rejected: HTTP {} {}", res.statusCode(), res.body());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not start the M-Pesa payment. Try again");
            }
            return j.path("CheckoutRequestID").asText();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not start the M-Pesa payment");
        } catch (Exception e) {
            log.error("STK push failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not start the M-Pesa payment");
        }
    }
}
