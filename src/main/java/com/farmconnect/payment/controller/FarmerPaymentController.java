package com.farmconnect.payment.controller;

import com.farmconnect.model.User;
import com.farmconnect.payment.model.*;
import com.farmconnect.payment.repository.AllocationRepository;
import com.farmconnect.payment.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A farmer's own earnings and payout phone. Shows nothing about buyers or other farmers. */
@RestController
@RequestMapping("/api/payments/farmer")
@PreAuthorize("hasRole('FARMER')")
@RequiredArgsConstructor
public class FarmerPaymentController {
    private final AllocationRepository allocations;
    private final BeneficiaryService accounts;

    public record Earning(Long orderId, BigDecimal amountKes, BigDecimal paidKes, String status, Instant date) {}
    public record PhoneRequest(String phone) {}

    @GetMapping("/earnings")
    @Transactional(readOnly = true)
    public Map<String, Object> earnings(@AuthenticationPrincipal User me) {
        String id = String.valueOf(me.getId());
        List<PaymentAllocation> all = allocations.findByBeneficiaryTypeAndBeneficiaryIdOrderByIdDesc(BeneficiaryType.FARMER, id);
        long waiting = 0, paid = 0;
        for (PaymentAllocation a : all) {
            if (a.getStatus() == AllocationStatus.SETTLED) paid += a.getAmountCents();
            else if (a.getStatus() != AllocationStatus.REVERSED) waiting += a.getAmountCents();
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("waitingKes", PaymentViews.kes(waiting));
        m.put("paidKes", PaymentViews.kes(paid));
        m.put("recent", all.stream().limit(50).map(a -> new Earning(a.getPayment().getOrder().getId(), PaymentViews.kes(a.getAmountCents()),
                PaymentViews.kes(a.getSettledCents()), farmerStatus(a.getStatus()), a.getCreatedAt())).toList());
        return m;
    }

    @GetMapping("/account")
    public Map<String, Object> account(@AuthenticationPrincipal User me) {
        return accounts.farmerAccount(me).map(FarmerPaymentController::view).orElseGet(() -> Map.of("status", "NONE"));
    }

    /** New payout number. Used only after an admin approves it (unless it is the number you already verified by SMS). */
    @PutMapping("/account")
    public Map<String, Object> change(@RequestBody PhoneRequest r, @AuthenticationPrincipal User me) {
        return view(accounts.changeFarmerPhone(me, r.phone()));
    }

    private static Map<String, Object> view(BeneficiaryAccount a) {
        String p = a.getAccountReference() == null ? "" : a.getAccountReference();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("phone", p.length() > 6 ? p.substring(0, 5) + "****" + p.substring(p.length() - 3) : p);
        m.put("status", a.getVerificationStatus().name());
        return m;
    }

    private static String farmerStatus(AllocationStatus s) {
        return switch (s) {
            case RECORDED -> "WAITING_FOR_DELIVERY";
            case ELIGIBLE, SETTLING -> "PAYING";
            case SETTLED -> "PAID";
            case ON_HOLD -> "ON_HOLD";
            case REVERSED -> "REFUNDED";
        };
    }
}
