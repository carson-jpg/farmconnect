package com.farmconnect.payment.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The agreed money rules in one place (application.properties, prefix "payments.").
 * The farmer always gets the remainder, so only the platform and county shares are configured.
 *
 * Commission base: the produce price of the order (what the buyer is charged; there are no delivery fees yet).
 * Fees: M-Pesa charges are borne by FarmConnect and are NOT deducted from the farmer or county shares.
 */
@Component
@Getter
public class PaymentPolicy {
    @Value("${payments.platform-bps:500}") private int platformBps;
    @Value("${payments.county-bps:500}") private int countyBps;
    @Value("${payments.county-name:Trans Nzoia}") private String countyName;
    /** After an order is DELIVERED, wait this long (dispute window) before the money becomes payable. */
    @Value("${payments.hold-hours:24}") private int holdHours;
    @Value("${payments.max-attempts:5}") private int maxAttempts;
    /** Safaricom's smallest B2C payment. Smaller amounts wait until an admin decides. */
    @Value("${payments.min-payout-kes:10}") private long minPayoutKes;
    /** A payout with no result from Safaricom after this long becomes UNKNOWN (needs a human to check). */
    @Value("${payments.processing-timeout-minutes:30}") private int processingTimeoutMinutes;
    /** An STK push with no callback after this many minutes gets checked with Safaricom. */
    @Value("${payments.pending-check-minutes:3}") private int pendingCheckMinutes;

    @PostConstruct
    void validate() {
        if (platformBps < 0 || countyBps < 0 || platformBps + countyBps > 10_000)
            throw new IllegalStateException("payments.platform-bps + payments.county-bps must be between 0 and 10000");
    }

    public int farmerBps() { return 10_000 - platformBps - countyBps; }
}
