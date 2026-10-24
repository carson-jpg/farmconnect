package com.farmconnect.controller;

import com.farmconnect.dto.SupplyDtos.*;
import com.farmconnect.model.SupplyPaymentStatus;
import com.farmconnect.model.SupplyPayoutStatus;
import com.farmconnect.model.User;
import com.farmconnect.repository.SupplyLedgerRepository;
import com.farmconnect.security.ClientIp;
import com.farmconnect.service.AuditService;
import com.farmconnect.service.SupplyPaymentService;
import com.farmconnect.service.SupplyPayoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Farm-input money, for the SUPER ADMIN only: collections, company payouts, and a ledger balance check. */
@RestController
@RequestMapping("/api/admin/supply-payments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSupplyPaymentController {
    private final SupplyPaymentService payments;
    private final SupplyPayoutService payouts;
    private final SupplyLedgerRepository ledger;
    private final AuditService audit;

    @GetMapping("/payments")
    public List<SupplyPaymentView> payments(@RequestParam(required = false) SupplyPaymentStatus status) { return payments.list(status); }

    /** A payment whose callback never arrived: the admin reads the receipt from the M-Pesa statement. */
    @PostMapping("/payments/{id}/confirm")
    public SupplyPaymentView confirm(@PathVariable Long id, @Valid @RequestBody ConfirmRequest r,
                                     @AuthenticationPrincipal User me, HttpServletRequest http) {
        SupplyPaymentView v = payments.adminConfirm(id, r.receipt(), r.amountKes(), me.getId());
        audit.record(me, "SUPPLY_PAYMENT_CONFIRMED", "payment #" + id, "receipt " + r.receipt() + " KES " + r.amountKes(), ClientIp.of(http), true);
        return v;
    }

    @GetMapping("/payouts")
    public List<PayoutView> payouts(@RequestParam(required = false) SupplyPayoutStatus status) { return payouts.list(status); }

    @PostMapping("/payouts/{id}/requeue")
    public PayoutView requeue(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean confirmedNotSent,
                              @AuthenticationPrincipal User me, HttpServletRequest http) {
        PayoutView v = payouts.requeue(id, confirmedNotSent);
        audit.record(me, "SUPPLY_PAYOUT_REQUEUED", "payout #" + id, "confirmedNotSent=" + confirmedNotSent, ClientIp.of(http), true);
        return v;
    }

    @PostMapping("/payouts/{id}/mark-paid")
    public PayoutView markPaid(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body,
                               @AuthenticationPrincipal User me, HttpServletRequest http) {
        String ref = body == null ? null : body.get("reference");
        PayoutView v = payouts.markPaid(id, ref);
        audit.record(me, "SUPPLY_PAYOUT_MARKED_PAID", "payout #" + id, ref, ClientIp.of(http), true);
        return v;
    }

    /** Every payment's debits must equal its credits. */
    @GetMapping("/ledger-check")
    public Map<String, Object> ledgerCheck() {
        List<Long> bad = ledger.unbalancedPayments();
        return Map.of("balanced", bad.isEmpty(), "unbalancedPayments", bad);
    }
}
