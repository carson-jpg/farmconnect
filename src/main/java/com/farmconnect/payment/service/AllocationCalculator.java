package com.farmconnect.payment.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure money-splitting maths (no Spring, no database) so it can be tested on its own.
 * All amounts are integer cents (KES x 100). Percentages are basis points (500 = 5.00%).
 * Never uses floating point.
 */
public final class AllocationCalculator {
    private AllocationCalculator() {}

    /** A farmer's produce subtotal inside one order (cents), used to split the farmer pool fairly. */
    public record FarmerWeight(long farmerId, long subtotalCents) {}

    public record FarmerShare(long farmerId, long amountCents) {}

    public record Split(long platformCents, long countyCents, List<FarmerShare> farmers) {
        public long farmersTotal() { return farmers.stream().mapToLong(FarmerShare::amountCents).sum(); }
        public long total() { return platformCents + countyCents + farmersTotal(); }
    }

    /** round-half-up of amount * bps / 10000, in integer maths. */
    public static long bpsOf(long amountCents, int bps) {
        if (amountCents < 0 || bps < 0) throw new IllegalArgumentException("negative input");
        return Math.addExact(Math.multiplyExact(amountCents, (long) bps), 5_000L) / 10_000L;
    }

    /**
     * platform = round(sale x platformBps), county = round(sale x countyBps), farmers get the remainder,
     * so the three always add up to exactly the sale amount. When an order holds produce from several farmers the
     * farmer pool is divided in proportion to each farmer's produce subtotal (largest-remainder method, so no cent
     * is lost or invented).
     */
    public static Split split(long saleCents, int platformBps, int countyBps, List<FarmerWeight> weights) {
        if (saleCents <= 0) throw new IllegalArgumentException("sale amount must be positive");
        if (platformBps < 0 || countyBps < 0 || platformBps + countyBps > 10_000)
            throw new IllegalArgumentException("invalid percentages");
        if (weights == null || weights.isEmpty()) throw new IllegalArgumentException("no farmers on the order");

        long platform = bpsOf(saleCents, platformBps);
        long county = bpsOf(saleCents, countyBps);
        long pool = saleCents - platform - county;
        if (pool < 0) throw new IllegalStateException("shares exceed the sale");

        long weightSum = 0;
        for (FarmerWeight w : weights) {
            if (w.subtotalCents() < 0) throw new IllegalArgumentException("negative subtotal");
            weightSum = Math.addExact(weightSum, w.subtotalCents());
        }
        if (weightSum == 0) throw new IllegalArgumentException("order has no value");

        int n = weights.size();
        long[] base = new long[n];
        long[] rem = new long[n];
        long given = 0;
        for (int i = 0; i < n; i++) {
            long num = Math.multiplyExact(pool, weights.get(i).subtotalCents());
            base[i] = num / weightSum;
            rem[i] = num % weightSum;
            given += base[i];
        }
        long left = pool - given;           // 0 <= left < n
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> {
            int c = Long.compare(rem[b], rem[a]);              // biggest remainder first
            return c != 0 ? c : Integer.compare(a, b);         // stable tie-break
        });
        for (int k = 0; k < left; k++) base[order[k]]++;

        List<FarmerShare> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(new FarmerShare(weights.get(i).farmerId(), base[i]));
        return new Split(platform, county, out);
    }
}
