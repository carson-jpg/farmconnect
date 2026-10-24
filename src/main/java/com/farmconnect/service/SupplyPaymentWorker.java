package com.farmconnect.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Background loop for supply payments. Each step is isolated and safe to repeat. Off unless supply.payments.enabled=true. */
@Component
@RequiredArgsConstructor
public class SupplyPaymentWorker {
    private static final Logger log = LoggerFactory.getLogger(SupplyPaymentWorker.class);

    private final SupplyPaymentService payments;
    private final SupplyPayoutService payouts;

    @Value("${supply.payments.enabled:false}") private boolean enabled;
    @Value("${payments.worker.enabled:true}") private boolean workerEnabled;

    @Scheduled(initialDelayString = "${payments.worker.initial-delay-ms:30000}", fixedDelayString = "${payments.worker.interval-ms:60000}")
    public void tick() {
        if (!enabled || !workerEnabled) return;
        step("check pending supply payments", payments::sweepPending);
        step("flag stale supply payouts", () -> { int n = payouts.sweepStale(); if (n > 0) log.warn("{} supply payout(s) moved to UNKNOWN", n); });
        step("send supply payouts", payouts::processQueue);
    }

    private void step(String name, Runnable r) {
        try { r.run(); } catch (Exception e) { log.error("Supply payment worker step '{}' failed", name, e); }
    }
}
