package com.farmconnect.service;

import com.farmconnect.payment.service.AllocationCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Pure arithmetic for cash sales that are approved by the super admin. No database, no Spring. */
public final class CashMath {
    private CashMath() {}

    /** What the farmer's commission bill is made of. All values are cents; they always add up to saleCents. */
    public record Parts(long saleCents, long platformCents, long countyCents, long farmerCents) {
        /** What the farmer owes FarmConnect (platform + county share); the rest the farmer already keeps in cash. */
        public long billCents() { return platformCents + countyCents; }
    }

    /** A farmer's produce subtotal rounded UP to whole shillings (M-Pesa moves whole shillings), in cents. At least KES 1. */
    public static long expectedCents(BigDecimal subtotal) {
        long kes = Math.max(1, subtotal.setScale(0, RoundingMode.CEILING).longValueExact());
        return kes * 100;
    }

    /** Same 5% / 5% / rest rule as M-Pesa sales, worked out in whole shillings. */
    public static Parts split(long saleCents, int platformBps, int countyBps) {
        if (saleCents <= 0 || saleCents % 100 != 0) throw new IllegalArgumentException("sale must be whole shillings");
        AllocationCalculator.Split s = AllocationCalculator.split(saleCents / 100, platformBps, countyBps,
                List.of(new AllocationCalculator.FarmerWeight(1L, saleCents / 100)));
        long farmer = s.farmers().get(0).amountCents();
        return new Parts(saleCents, s.platformCents() * 100, s.countyCents() * 100, farmer * 100);
    }

    /** A bill is overdue once it has been unpaid for more than {@code days} days. */
    public static boolean overdue(Instant created, Instant now, int days) {
        return created != null && created.plus(Duration.ofDays(days)).isBefore(now);
    }
}
