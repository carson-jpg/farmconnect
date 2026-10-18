package com.farmconnect.payment.controller;

import com.farmconnect.model.User;
import com.farmconnect.payment.controller.PaymentViews.*;
import com.farmconnect.payment.model.*;
import com.farmconnect.payment.repository.*;
import com.farmconnect.payment.service.*;
import com.farmconnect.security.Staff;
import com.farmconnect.service.AreaScope;
import com.farmconnect.service.AreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Money administration. The super admin (ADMIN) does everything. A county admin can only LOOK at payments that include
 * farmers from their county (list, detail, payouts, summary); every action that moves or changes money stays ADMIN only.
 * Officers cannot touch payments.
 */
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
    private final AreaService areas;

    public record ConfirmRequest(String receipt, long amountKes) {}
    public record RefundRequest(String reason, String phone) {}
    public record PhoneRequest(String phone) {}
    public record RetryRequest(boolean confirmNotSent) {}
    public record ReferenceRequest(String reference) {}
    public record ReasonRequest(String reason) {}
    public record StatementRequest(List<ReconciliationService.StatementLine> lines, Instant from, Instant to) {}

    // ------------------------------------------------------------------ payments

    // ---- county scoping (read-only views for county admins)

    /** Farmer ids a county admin may see money for; null for the super admin (everything). */
    private Set<Long> scopeIds(User me) {
        return Staff.isCountyAdmin(me) ? areas.farmerIdsIn(AreaScope.of(me)) : null;
    }

    private static boolean inArea(Payment p, Set<Long> ids) { return AreaService.orderTouches(p.getOrder(), ids); }

    /** Does this allocation/payout belong to someone a county admin may see (a farmer in the county, or the county itself)? */
    private static boolean mine(BeneficiaryType type, String beneficiaryId, Set<Long> ids) {
        if (ids == null) return true;
        if (type == BeneficiaryType.COUNTY) return true;
        if (type != BeneficiaryType.FARMER) return false;           // platform share and buyer refunds are not the county's business
        try { return ids.contains(Long.parseLong(beneficiaryId)); } catch (NumberFormatException e) { return false; }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COUNTY_ADMIN')")
    @Transactional(readOnly = true)
    public List<PaymentView> list(@RequestParam(required = false) PaymentStatus status, @AuthenticationPrincipal User me) {
        Set<Long> ids = scopeIds(me);
        if (ids == null)
            return (status == null ? payments.findTop100ByOrderByIdDesc() : payments.findByStatusOrderByIdDesc(status))
                    .stream().map(PaymentViews::of).toList();
        return payments.findAll().stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> inArea(p, ids))
                .sorted(Comparator.comparing(Payment::getId).reversed()).limit(100)
                .map(PaymentViews::of).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COUNTY_ADMIN')")
    @Transactional(readOnly = true)
    public Map<String, Object> detail(@PathVariable Long id, @AuthenticationPrincipal User me) {
        Payment p = payments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        Set<Long> ids = scopeIds(me);
        if (!inArea(p, ids)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("payment", PaymentViews.of(p));
        m.put("allocations", allocations.findByPaymentId(id).stream().filter(a -> mine(a.getBeneficiaryType(), a.getBeneficiaryId(), ids))
                .map(PaymentViews::of).toList());
        m.put("payouts", payouts.findByPaymentId(id).stream()
                .filter(x -> ids == null || (x.getKind() == PayoutKind.ALLOCATION && mine(x.getBeneficiaryType(), x.getBeneficiaryId(), ids)))
                .map(PaymentViews::of).toList());
        // the internal ledger is for the super admin only
        m.put("ledger", ids != null ? List.of() : ledger.findByPaymentIdOrderByIdAsc(id).stream().map(PaymentViews::of).toList());
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
    @PreAuthorize("hasAnyRole('ADMIN','COUNTY_ADMIN')")
    @Transactional(readOnly = true)
    public List<PayoutView> payoutList(@RequestParam(required = false) PayoutStatus status, @AuthenticationPrincipal User me) {
        Set<Long> ids = scopeIds(me);
        if (ids == null)
            return (status == null ? payouts.findTop100ByOrderByIdDesc() : payouts.findByStatus(status)).stream().map(PaymentViews::of).toList();
        return payouts.findAll().stream()
                .filter(x -> status == null || x.getStatus() == status)
                .filter(x -> x.getKind() == PayoutKind.ALLOCATION && inArea(x.getPayment(), ids) && mine(x.getBeneficiaryType(), x.getBeneficiaryId(), ids))
                .sorted(Comparator.comparing(Payout::getId).reversed()).limit(100)
                .map(PaymentViews::of).toList();
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
    @PreAuthorize("hasAnyRole('ADMIN','COUNTY_ADMIN')")
    @Transactional(readOnly = true)
    public Map<String, Object> summary(@AuthenticationPrincipal User me) {
        List<AllocationStatus> owed = List.of(AllocationStatus.RECORDED, AllocationStatus.ELIGIBLE, AllocationStatus.SETTLING, AllocationStatus.ON_HOLD);
        Set<Long> ids = scopeIds(me);
        if (ids != null) return countySummary(ids, owed);
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

    /** What a county admin sees: only payments that include the county's farmers, and no platform commission. */
    private Map<String, Object> countySummary(Set<Long> ids, List<AllocationStatus> owed) {
        List<Payment> inScope = payments.findAll().stream().filter(p -> inArea(p, ids)).toList();
        Set<Long> mineIds = new java.util.HashSet<>();
        for (Payment p : inScope) mineIds.add(p.getId());
        long collected = inScope.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS && p.getMpesaReceiptNumber() != null)
                .mapToLong(Payment::getAmountCents).sum();
        List<PaymentAllocation> allocs = allocations.findAll().stream()
                .filter(a -> mineIds.contains(a.getPayment().getId()) && mine(a.getBeneficiaryType(), a.getBeneficiaryId(), ids)).toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("collectedKes", PaymentViews.kes(collected));
        for (BeneficiaryType t : List.of(BeneficiaryType.COUNTY, BeneficiaryType.FARMER)) {
            long o = allocs.stream().filter(a -> a.getBeneficiaryType() == t && owed.contains(a.getStatus())).mapToLong(PaymentAllocation::getAmountCents).sum();
            long d = allocs.stream().filter(a -> a.getBeneficiaryType() == t && a.getStatus() == AllocationStatus.SETTLED).mapToLong(PaymentAllocation::getAmountCents).sum();
            Map<String, BigDecimal> x = new LinkedHashMap<>();
            x.put("owedKes", PaymentViews.kes(o));
            x.put("settledKes", PaymentViews.kes(d));
            m.put(t.name().toLowerCase(), x);
        }
        m.put("needsReview", inScope.stream().filter(p -> p.getStatus() == PaymentStatus.NEEDS_REVIEW).count());
        List<Payout> myPayouts = payouts.findAll().stream()
                .filter(x -> x.getKind() == PayoutKind.ALLOCATION && mineIds.contains(x.getPayment().getId())
                        && mine(x.getBeneficiaryType(), x.getBeneficiaryId(), ids)).toList();
        m.put("payoutsUnknown", myPayouts.stream().filter(x -> x.getStatus() == PayoutStatus.UNKNOWN).count());
        m.put("payoutsFailed", myPayouts.stream().filter(x -> x.getStatus() == PayoutStatus.FAILED).count());
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
