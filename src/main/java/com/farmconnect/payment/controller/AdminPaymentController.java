package com.farmconnect.payment.controller;

import com.farmconnect.model.User;
import com.farmconnect.payment.controller.PaymentViews.*;
import com.farmconnect.payment.model.*;
import com.farmconnect.payment.repository.*;
import com.farmconnect.payment.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Money administration. ADMIN only (officers cannot touch payments). */
@RestController
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPaymentController {
    private final PaymentRepository payments;
    private final AllocationRepository allocations;
    private final PayoutRepository payouts;
    private final LedgerRepository ledger;
    private final BeneficiaryAccountRepository accountRepo;
    private final PaymentService paymentService;
    private final PayoutService payoutService;
    private final RefundService refunds;
    private final ReconciliationService reconciliation;
    private final BeneficiaryService accounts;

    public record ConfirmRequest(String receipt, long amountKes) {}
    public record RefundRequest(String reason, String phone) {}
    public record PhoneRequest(String phone) {}
    public record RetryRequest(boolean confirmNotSent) {}
    public record ReferenceRequest(String reference) {}
    public record ReasonRequest(String reason) {}
    public record StatementRequest(List<ReconciliationService.StatementLine> lines, Instant from, Instant to) {}

    // ------------------------------------------------------------------ payments

    @GetMapping
    @Transactional(readOnly = true)
    public List<PaymentView> list(@RequestParam(required = false) PaymentStatus status) {
        return (status == null ? payments.findTop100ByOrderByIdDesc() : payments.findByStatusOrderByIdDesc(status))
                .stream().map(PaymentViews::of).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public Map<String, Object> detail(@PathVariable Long id) {
        Payment p = payments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("payment", PaymentViews.of(p));
        m.put("allocations", allocations.findByPaymentId(id).stream().map(PaymentViews::of).toList());
        m.put("payouts", payouts.findByPaymentId(id).stream().map(PaymentViews::of).toList());
        m.put("ledger", ledger.findByPaymentIdOrderByIdAsc(id).stream().map(PaymentViews::of).toList());
        return m;
    }

    /** Supply the receipt for a payment whose callback never arrived (read it from the M-Pesa statement). */
    @PostMapping("/{id}/confirm")
    public PaymentView confirm(@PathVariable Long id, @RequestBody ConfirmRequest r, @AuthenticationPrincipal User admin) {
        return PaymentViews.of(paymentService.adminConfirm(id, r.receipt(), r.amountKes(), admin.getId()));
    }

    @PostMapping("/{id}/refund")
    public PayoutView refund(@PathVariable Long id, @RequestBody RefundRequest r) {
        return PaymentViews.of(refunds.refundPayment(id, r.reason(), r.phone()));
    }

    @PostMapping("/{id}/refund-excess")
    public PayoutView refundExcess(@PathVariable Long id, @RequestBody(required = false) PhoneRequest r) {
        return PaymentViews.of(refunds.refundExcess(id, r == null ? null : r.phone()));
    }

    // ------------------------------------------------------------------ payouts and allocations

    @GetMapping("/payouts")
    @Transactional(readOnly = true)
    public List<PayoutView> payoutList(@RequestParam(required = false) PayoutStatus status) {
        return (status == null ? payouts.findTop100ByOrderByIdDesc() : payouts.findByStatus(status)).stream().map(PaymentViews::of).toList();
    }

    @PostMapping("/payouts/{id}/retry")
    public PayoutView retry(@PathVariable Long id, @RequestBody(required = false) RetryRequest r) {
        return PaymentViews.of(payoutService.requeue(id, r != null && r.confirmNotSent()));
    }

    @PostMapping("/payouts/{id}/mark-paid")
    public PayoutView markPaid(@PathVariable Long id, @RequestBody ReferenceRequest r) {
        return PaymentViews.of(payoutService.markPaid(id, r.reference()));
    }

    @PostMapping("/allocations/{id}/hold")
    public AllocationView hold(@PathVariable Long id, @RequestBody(required = false) ReasonRequest r) {
        return PaymentViews.of(payoutService.hold(id, r == null ? null : r.reason()));
    }

    @PostMapping("/allocations/{id}/release")
    public AllocationView release(@PathVariable Long id) {
        return PaymentViews.of(payoutService.release(id));
    }

    // ------------------------------------------------------------------ payout accounts

    @GetMapping("/accounts")
    @Transactional(readOnly = true)
    public List<AccountView> accountList(@RequestParam(defaultValue = "PENDING") AccountStatus status) {
        return accountRepo.findByVerificationStatus(status).stream().map(PaymentViews::of).toList();
    }

    @PostMapping("/accounts/{id}/approve")
    public AccountView approve(@PathVariable Long id) { return PaymentViews.of(accounts.review(id, true)); }

    @PostMapping("/accounts/{id}/reject")
    public AccountView reject(@PathVariable Long id) { return PaymentViews.of(accounts.review(id, false)); }

    // ------------------------------------------------------------------ reports

    /** Totals in KES: collected, and per beneficiary what is still owed vs already paid. */
    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        List<AllocationStatus> owed = List.of(AllocationStatus.RECORDED, AllocationStatus.ELIGIBLE, AllocationStatus.SETTLING, AllocationStatus.ON_HOLD);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("collectedKes", PaymentViews.kes(payments.sumAmount(PaymentStatus.SUCCESS)));
        for (BeneficiaryType t : List.of(BeneficiaryType.PLATFORM, BeneficiaryType.COUNTY, BeneficiaryType.FARMER)) {
            Map<String, BigDecimal> x = new LinkedHashMap<>();
            x.put("owedKes", PaymentViews.kes(allocations.sumByType(t, owed)));
            x.put("settledKes", PaymentViews.kes(allocations.sumByType(t, List.of(AllocationStatus.SETTLED))));
            m.put(t.name().toLowerCase(), x);
        }
        m.put("needsReview", payments.findByStatusOrderByIdDesc(PaymentStatus.NEEDS_REVIEW).size());
        m.put("payoutsUnknown", payouts.findByStatus(PayoutStatus.UNKNOWN).size());
        m.put("payoutsFailed", payouts.findByStatus(PayoutStatus.FAILED).size());
        return m;
    }

    @GetMapping("/reconciliation")
    public ReconciliationService.Report reconcile(@RequestParam(defaultValue = "7") int days) {
        return reconciliation.run(days);
    }

    @PostMapping("/reconciliation/statement")
    public List<ReconciliationService.Issue> statement(@RequestBody StatementRequest r) {
        return reconciliation.compareStatement(r.lines() == null ? List.of() : r.lines(), r.from(), r.to());
    }
}
