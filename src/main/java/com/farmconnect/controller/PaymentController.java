package com.farmconnect.controller;

import com.farmconnect.model.*;
import com.farmconnect.repository.OrderRepository;
import com.farmconnect.service.MpesaService;
import com.farmconnect.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final OrderRepository orders;
    private final MpesaService mpesa;
    private final NotificationService notifier;

    public record StkRequest(String phone) {}

    /** Buyer: send the M-Pesa payment prompt to the phone. */
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
        String checkoutId = mpesa.stkPush(MpesaService.normalizePhone(raw), o.getTotal(), "FC" + o.getId(), "FarmConnect");
        o.setMpesaCheckoutRequestId(checkoutId);
        o.setPaymentStatus("PENDING");
        o.setUpdatedAt(Instant.now());
        orders.save(o);
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
    @Transactional
    public Map<String, Object> callback(@PathVariable String secret, @RequestBody JsonNode body) {
        if (!mpesa.callbackSecretOk(secret)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        Map<String, Object> ok = Map.of("ResultCode", 0, "ResultDesc", "Accepted");

        JsonNode cb = body.path("Body").path("stkCallback");
        String checkoutId = cb.path("CheckoutRequestID").asText("");
        CustomerOrder o = orders.findByMpesaCheckoutRequestId(checkoutId).orElse(null);
        if (o == null) { log.warn("M-Pesa callback for unknown CheckoutRequestID {}", checkoutId); return ok; }
        if ("PAID".equals(o.getPaymentStatus())) return ok;   // Safaricom can retry; stay idempotent

        if (cb.path("ResultCode").asInt(-1) == 0) {
            String receipt = null;
            BigDecimal paid = null;
            for (JsonNode it : cb.path("CallbackMetadata").path("Item")) {
                String n = it.path("Name").asText();
                if (n.equals("MpesaReceiptNumber")) receipt = it.path("Value").asText();
                if (n.equals("Amount")) paid = it.path("Value").decimalValue();
            }
            BigDecimal due = o.getTotal().setScale(0, RoundingMode.CEILING);
            if (paid != null && paid.compareTo(due) < 0) {
                log.error("Order {} underpaid: got {} expected {}. Check manually. Receipt {}", o.getId(), paid, due, receipt);
                return ok;
            }
            o.setPaymentStatus("PAID");
            o.setMpesaReceipt(receipt);
            o.setUpdatedAt(Instant.now());
            orders.save(o);
            notifier.notifyAndSms(o.getBuyer(), "ORDER", "Payment received for order #" + o.getId(),
                    "M-Pesa receipt " + receipt + ". Thank you!", o.getId());
            Map<Long, User> owners = new LinkedHashMap<>();
            for (OrderItem i : o.getItems()) {
                User f = i.getProduct().getFarm().getOwner();
                owners.putIfAbsent(f.getId(), f);
            }
            owners.values().forEach(f -> notifier.notifyAndSms(f, "ORDER", "Order #" + o.getId() + " is paid",
                    "The buyer paid by M-Pesa. You can confirm the order now.", o.getId()));
        } else {
            o.setPaymentStatus("FAILED");
            o.setUpdatedAt(Instant.now());
            orders.save(o);
        }
        return ok;
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
