package com.farmconnect.payment.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background loop. Every step is isolated, so one failure never stops the others, and each step is safe to repeat.
 * Runs on ONE server. If you ever run several, payouts are still safe (atomic claim) but set payments.worker.enabled=false on the extras.
 */
@Component
@RequiredArgsConstructor
public class PayoutWorker {
    private static final Logger log = LoggerFactory.getLogger(PayoutWorker.class);

    private final PaymentService payments;
    private final PayoutService payouts;
    private final ReconciliationService reconciliation;

    @Value("${payments.worker.enabled:true}") private boolean enabled;

    @Scheduled(initialDelayString = "${payments.worker.initial-delay-ms:30000}", fixedDelayString = "${payments.worker.interval-ms:60000}")
    public void tick() {
        if (!enabled) return;
        step("check pending payments", payments::sweepPending);
        step("flag stale payouts", () -> { int n = payouts.sweepStale(); if (n > 0) log.warn("{} payout(s) moved to UNKNOWN", n); });
        step("promote eligible allocations", () -> { int n = payouts.promoteEligible(); if (n > 0) log.info("{} allocation(s) became ELIGIBLE", n); });
        step("queue payouts", () -> { int n = payouts.enqueuePayouts(); if (n > 0) log.info("{} payout(s) queued", n); });
        step("send payouts", payouts::processQueue);
    }

    /** Daily check of our books. Results are available at GET /api/admin/payments/reconciliation. */
    @Scheduled(cron = "${payments.reconcile.cron:0 30 1 * * *}", zone = "Africa/Nairobi")
    public void nightlyReconciliation() {
        if (!enabled) return;
        step("reconciliation", () -> {
            var report = reconciliation.run(30);
            long errors = report.issues().stream().filter(i -> "ERROR".equals(i.severity())).count();
            if (errors > 0) log.error("Reconciliation found {} error(s). Open the admin payments page.", errors);
        });
    }

    private void step(String name, Runnable r) {
        try { r.run(); } catch (Exception e) { log.error("Payment worker step '{}' failed", name, e); }
    }
}
