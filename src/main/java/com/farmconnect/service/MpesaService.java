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

    @Value("${mpesa.env:${MPESA_ENV:sandbox}}") private String env;
    @Value("${mpesa.consumer-key:${MPESA_CONSUMER_KEY:}}") private String consumerKey;
    @Value("${mpesa.consumer-secret:${MPESA_CONSUMER_SECRET:}}") private String consumerSecret;
    @Value("${mpesa.shortcode:${MPESA_SHORTCODE:}}") private String shortcode;
    @Value("${mpesa.party-b:${MPESA_PARTYB:}}") private String partyB;
    @Value("${mpesa.passkey:${MPESA_PASSKEY:}}") private String passkey;
    @Value("${mpesa.transaction-type:${MPESA_TRANSACTION_TYPE:CustomerPayBillOnline}}") private String transactionType;
    @Value("${mpesa.callback-url:${MPESA_CALLBACK_URL:}}") private String callbackUrl;
    @Value("${mpesa.callback-secret:${MPESA_CALLBACK_SECRET:}}") private String callbackSecret;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    // ---- payouts (B2C / B2B). Leave empty to keep payouts switched off. ----
    @Value("${mpesa.initiator.name:}") private String initiatorName;
    @Value("${mpesa.initiator.password:}") private String initiatorPassword;
    @Value("${mpesa.initiator.cert-path:}") private String initiatorCertPath;
    @Value("${mpesa.initiator.security-credential:}") private String initiatorCredential;
    @Value("${mpesa.b2c.shortcode:}") private String b2cShortcode;

    private String token;
    private Instant tokenExpiry = Instant.EPOCH;

    private String base() {
        return "production".equalsIgnoreCase(env) ? "https://api.safaricom.co.ke" : "https://sandbox.safaricom.co.ke";
    }

    @jakarta.annotation.PostConstruct
    void reportConfig() {
        java.util.List<String> m = missing();
        if (m.isEmpty()) log.info("M-Pesa: all required settings found (env={}).", env);
        else log.warn("M-Pesa: payments are OFF. Missing settings: {}. Set them as mpesa.xxx in application.properties, or as MPESA_XXX environment variables / properties.", m);
    }

    private boolean configured() { return missing().isEmpty(); }

    /** Names (never values) of the settings that are still empty, so a mistake is easy to spot. */
    private java.util.List<String> missing() {
        java.util.List<String> m = new java.util.ArrayList<>();
        if (consumerKey.isBlank()) m.add("mpesa.consumer-key");
        if (consumerSecret.isBlank()) m.add("mpesa.consumer-secret");
        if (shortcode.isBlank()) m.add("mpesa.shortcode");
        if (passkey.isBlank()) m.add("mpesa.passkey");
        if (callbackUrl.isBlank()) m.add("mpesa.callback-url");
        if (callbackSecret.isBlank()) m.add("mpesa.callback-secret");
        return m;
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

    public record StkResult(String merchantRequestId, String checkoutRequestId) {}

    /** Sends the payment prompt to the customer's phone. Returns the CheckoutRequestID. */
    public String stkPush(String phone254, BigDecimal amount, String accountRef, String desc) {
        return stkPushFull(phone254, amount, accountRef, desc).checkoutRequestId();
    }

    /** Same as stkPush but also returns the MerchantRequestID (we store both on the payment). */
    public StkResult stkPushFull(String phone254, BigDecimal amount, String accountRef, String desc) {
        if (!configured())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "M-Pesa payments are not set up yet. Missing: " + String.join(", ", missing()));
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
            return new StkResult(j.path("MerchantRequestID").asText(null), j.path("CheckoutRequestID").asText());
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

    // =====================================================================================================
    //  Everything below was added for the payment/payout system.
    // =====================================================================================================

    public record StkQueryResult(boolean answered, int resultCode, String description) {}

    /**
     * Asks Safaricom what happened to an STK push. Used when a callback never arrived.
     * NOTE: Safaricom's answer has a result code only (no receipt number, no amount), so a "success" here must still be
     * matched to the real receipt by an admin before any money is allocated.
     */
    public StkQueryResult stkQuery(String checkoutRequestId) {
        if (!configured()) return new StkQueryResult(false, -1, "M-Pesa not configured");
        try {
            String ts = LocalDateTime.now(ZoneId.of("Africa/Nairobi")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = Base64.getEncoder().encodeToString((shortcode + passkey + ts).getBytes(StandardCharsets.UTF_8));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("BusinessShortCode", shortcode);
            body.put("Password", password);
            body.put("Timestamp", ts);
            body.put("CheckoutRequestID", checkoutRequestId);
            HttpResponse<String> res = postJson("/mpesa/stkpushquery/v1/query", body);
            if (res.statusCode() != 200) return new StkQueryResult(false, -1, "HTTP " + res.statusCode()); // e.g. still processing
            JsonNode j = mapper.readTree(res.body());
            if (!j.has("ResultCode")) return new StkQueryResult(false, -1, "no result yet");
            return new StkQueryResult(true, j.path("ResultCode").asInt(-1), j.path("ResultDesc").asText(""));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new StkQueryResult(false, -1, "interrupted");
        } catch (Exception e) {
            log.warn("STK query failed for {}: {}", checkoutRequestId, e.toString());
            return new StkQueryResult(false, -1, "query failed");
        }
    }

    /** ACCEPTED = Safaricom queued it (final result arrives on our result URL). REJECTED = definitely NOT sent.
     *  UNKNOWN = the request may or may not have reached Safaricom: never resend without checking first. */
    public enum SubmitOutcome { ACCEPTED, REJECTED, UNKNOWN }
    public record SubmitResult(SubmitOutcome outcome, String conversationId, String detail) {}

    public boolean payoutsConfigured() {
        return configured() && !initiatorName.isBlank()
                && (!initiatorCredential.isBlank() || (!initiatorPassword.isBlank() && !initiatorCertPath.isBlank()));
    }

    /** Where Safaricom posts payout results: {callback-url minus "/callback"}/b2c/result/{secret}. */
    private String payoutCallbackBase() {
        String b = callbackUrl.replaceAll("/+$", "");
        if (b.endsWith("/callback")) b = b.substring(0, b.length() - "/callback".length());
        return b;
    }

    private String securityCredential() throws Exception {
        if (!initiatorCredential.isBlank()) return initiatorCredential;
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Path.of(initiatorCertPath))) {
            java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate)
                    java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(in);
            javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(javax.crypto.Cipher.ENCRYPT_MODE, cert.getPublicKey());
            return Base64.getEncoder().encodeToString(c.doFinal(initiatorPassword.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private HttpResponse<String> postJson(String path, Map<String, Object> body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base() + path))
                .timeout(java.time.Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /** B2C: business -> customer phone. amountKes is whole shillings. */
    public SubmitResult b2c(String originatorConversationId, String phone254, long amountKes, String remarks) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("OriginatorConversationID", originatorConversationId);
        body.put("CommandID", "BusinessPayment");
        body.put("Amount", amountKes);
        body.put("PartyA", b2cShortcode.isBlank() ? shortcode : b2cShortcode);
        body.put("PartyB", phone254);
        body.put("Remarks", remarks);
        body.put("Occasion", "FarmConnect");
        return submit("/mpesa/b2c/v3/paymentrequest", body);
    }

    /** B2B: business -> paybill (BusinessPayBill) or till (BusinessBuyGoods). */
    public SubmitResult b2b(String originatorConversationId, boolean toTill, String partyB, String accountRef, long amountKes, String remarks) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("OriginatorConversationID", originatorConversationId);
        body.put("CommandID", toTill ? "BusinessBuyGoods" : "BusinessPayBill");
        body.put("SenderIdentifierType", "4");
        body.put("RecieverIdentifierType", "4");          // sic - Safaricom's spelling
        body.put("Amount", amountKes);
        body.put("PartyA", b2cShortcode.isBlank() ? shortcode : b2cShortcode);
        body.put("PartyB", partyB);
        body.put("AccountReference", accountRef == null || accountRef.isBlank() ? "FarmConnect" : accountRef);
        body.put("Remarks", remarks);
        return submit("/mpesa/b2b/v1/paymentrequest", body);
    }

    private SubmitResult submit(String path, Map<String, Object> body) {
        if (!payoutsConfigured())
            return new SubmitResult(SubmitOutcome.REJECTED, null, "M-Pesa payouts are not configured");
        String base = payoutCallbackBase() + "/b2c";
        body.put("InitiatorName", initiatorName);
        body.put("QueueTimeOutURL", base + "/timeout/" + callbackSecret);
        body.put("ResultURL", base + "/result/" + callbackSecret);
        // Everything up to here happens before anything is sent, so a failure is a definite "not sent".
        try {
            body.put("SecurityCredential", securityCredential());
            token();
        } catch (Exception e) {
            log.error("Payout not sent (credential/token problem): {}", e.toString());
            return new SubmitResult(SubmitOutcome.REJECTED, null, "Could not prepare the request: " + e.getMessage());
        }
        try {
            HttpResponse<String> res = postJson(path, body);
            JsonNode j;
            try { j = mapper.readTree(res.body()); } catch (Exception parse) { j = mapper.createObjectNode(); }
            int sc = res.statusCode();
            String desc = j.path("ResponseDescription").asText(j.path("errorMessage").asText(res.body()));
            if (sc == 200 && "0".equals(j.path("ResponseCode").asText())) {
                return new SubmitResult(SubmitOutcome.ACCEPTED, j.path("ConversationID").asText(null), desc);
            }
            if (sc >= 500 || desc.toLowerCase().contains("duplicate"))
                return new SubmitResult(SubmitOutcome.UNKNOWN, null, "HTTP " + sc + " " + desc);
            log.error("Payout rejected: HTTP {} {}", sc, res.body());
            return new SubmitResult(SubmitOutcome.REJECTED, null, "HTTP " + sc + " " + desc);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new SubmitResult(SubmitOutcome.UNKNOWN, null, "interrupted while sending");
        } catch (Exception e) {
            log.error("Payout request failed after sending began: {}", e.toString());
            return new SubmitResult(SubmitOutcome.UNKNOWN, null, "connection problem: " + e.getMessage());
        }
    }
}
