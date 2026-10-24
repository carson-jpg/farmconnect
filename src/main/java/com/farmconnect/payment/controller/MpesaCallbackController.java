package com.farmconnect.payment.controller;

import com.farmconnect.payment.model.CallbackLog;
import com.farmconnect.payment.repository.CallbackLogRepository;
import com.farmconnect.payment.service.PaymentService;
import com.farmconnect.payment.service.PayoutService;
import com.farmconnect.service.MpesaService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Public endpoints Safaricom calls (STK callbacks are in PaymentController). Each URL carries the shared secret.
 * Every message is saved raw first. Errors are logged and answered with "Accepted" so Safaricom does not hammer us;
 * anything we could not process shows up in reconciliation.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class MpesaCallbackController {
    private static final Logger log = LoggerFactory.getLogger(MpesaCallbackController.class);
    private static final Map<String, Object> OK = Map.of("ResultCode", 0, "ResultDesc", "Accepted");

    private final MpesaService mpesa;
    private final PaymentService payments;
    private final PayoutService payouts;
    private final CallbackLogRepository callbackLogs;
    /** Optional: farm-input payouts share the payout result URLs. Field injection keeps the constructor unchanged. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.farmconnect.service.SupplyPayoutService supplyPayouts;

    /** Final outcome of a B2C / B2B payout. */
    @PostMapping("/b2c/result/{secret}")
    public Map<String, Object> payoutResult(@PathVariable String secret, @RequestBody JsonNode body) {
        guard(secret, "PAYOUT_RESULT", body);
        try { if (supplyPayouts == null || !supplyPayouts.onResult(body)) payouts.onResult(body); } catch (Exception e) { log.error("Could not process payout result {}", body, e); }
        return OK;
    }

    @PostMapping("/b2c/timeout/{secret}")
    public Map<String, Object> payoutTimeout(@PathVariable String secret, @RequestBody JsonNode body) {
        guard(secret, "PAYOUT_TIMEOUT", body);
        try { if (supplyPayouts == null || !supplyPayouts.onTimeout(body)) payouts.onTimeout(body); } catch (Exception e) { log.error("Could not process payout timeout {}", body, e); }
        return OK;
    }

    /** C2B validation (only used if Safaricom enabled external validation for your shortcode). */
    @PostMapping("/c2b/validation/{secret}")
    public Map<String, Object> c2bValidation(@PathVariable String secret, @RequestBody JsonNode body) {
        guard(secret, "C2B_VALIDATION", body);
        boolean ok;
        try { ok = payments.c2bAcceptable(body); } catch (Exception e) { log.error("C2B validation failed", e); ok = true; }
        return ok ? OK : Map.of("ResultCode", "C2B00012", "ResultDesc", "Rejected");
    }

    /** C2B confirmation: the customer paid the till/paybill directly with account reference FC<orderId>. */
    @PostMapping("/c2b/confirmation/{secret}")
    public Map<String, Object> c2bConfirmation(@PathVariable String secret, @RequestBody JsonNode body) {
        guard(secret, "C2B_CONFIRMATION", body);
        try { payments.onC2bConfirmation(body); } catch (Exception e) { log.error("Could not process C2B confirmation {}", body, e); }
        return OK;
    }

    private void guard(String secret, String kind, JsonNode body) {
        if (!mpesa.callbackSecretOk(secret)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        CallbackLog l = new CallbackLog();
        l.setKind(kind);
        l.setPayload(body.toString());
        callbackLogs.save(l);
    }
}
