package com.farmconnect.controller;

import com.farmconnect.model.*;
import com.farmconnect.payment.model.CallbackLog;
import com.farmconnect.payment.repository.CallbackLogRepository;
import com.farmconnect.payment.service.PaymentService;
import com.farmconnect.repository.OrderRepository;
import com.farmconnect.service.MpesaService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Buyer-facing M-Pesa collection. Money rules (who gets what, duplicate protection, ledger) live in PaymentService;
 * this class only handles HTTP.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final OrderRepository orders;
    private final MpesaService mpesa;
    private final PaymentService payments;
    private final CallbackLogRepository callbackLogs;

    public record StkRequest(String phone) {}

    /** Buyer: send the M-Pesa payment prompt to the phone. The amount is always taken from the order, never from the app. */
    @PostMapping("/mpesa/{orderId}/stk")
    @PreAuthorize("hasRole('BUYER')")
    @Transactional
    public Map<String, Object> stk(@PathVariable Long orderId, @RequestBody(required = false) StkRequest r,
                                   @AuthenticationPrincipal User u) {
        CustomerOrder o = mine(orderId, u);
        if (!"MPESA_ONLINE".equals(o.getPaymentMethod()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This order is not set to pay with M-Pesa");
        if (o.getStatus() != OrderStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order can no longer be paid");
        if ("PAID".equals(o.getPaymentStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is already paid");

        String raw = r != null && r.phone() != null && !r.phone().isBlank() ? r.phone() : o.getDeliveryPhone();
        payments.startStk(o, MpesaService.normalizePhone(raw));
        return status(o);
    }

    /** Buyer: poll this after the prompt is sent. */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('BUYER')")
    public Map<String, Object> check(@PathVariable Long orderId, @AuthenticationPrincipal User u) {
        return status(mine(orderId, u));
    }

    /** Safaricom calls this. Public, but the URL carries a secret that only Safaricom (via us) knows. */
    @PostMapping("/callback/{secret}")
    public Map<String, Object> callback(@PathVariable String secret, @RequestBody JsonNode body) {
        if (!mpesa.callbackSecretOk(secret)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        CallbackLog l = new CallbackLog();
        l.setKind("STK");
        l.setPayload(body.toString());
        callbackLogs.save(l);                      // keep the raw message first, whatever happens next
        payments.onStkCallback(body);              // one transaction; if it fails Safaricom's retry will try again
        return Map.of("ResultCode", 0, "ResultDesc", "Accepted");
    }

    private CustomerOrder mine(Long id, User u) {
        CustomerOrder o = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!o.getBuyer().getId().equals(u.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        return o;
    }

    private Map<String, Object> status(CustomerOrder o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", o.getId());
        m.put("paymentStatus", o.getPaymentStatus() == null ? "UNPAID" : o.getPaymentStatus());
        m.put("receipt", o.getMpesaReceipt());
        return m;
    }
}
