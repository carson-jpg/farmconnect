package com.farmconnect.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure money maths for supply orders (no Spring, no database) so it can be unit tested. Amounts are KES with 2 decimals. */
public final class SupplyMath {
    private SupplyMath() {}

    /** amount x bps / 10000, rounded half-up to cents. */
    public static BigDecimal fee(BigDecimal amount, int bps) {
        if (amount.signum() < 0 || bps < 0) throw new IllegalArgumentException("negative input");
        return amount.multiply(BigDecimal.valueOf(bps)).divide(BigDecimal.valueOf(10_000), 2, RoundingMode.HALF_UP);
    }

    public record Split(BigDecimal platform, BigDecimal county, BigDecimal company) {}

    /** The company always gets the remainder, so the three parts add up to exactly the total. */
    public static Split split(BigDecimal total, int platformBps, int countyBps) {
        if (platformBps + countyBps > 10_000) throw new IllegalArgumentException("shares exceed the sale");
        BigDecimal platform = fee(total, platformBps);
        BigDecimal county = fee(total, countyBps);
        return new Split(platform, county, total.subtract(platform).subtract(county));
    }
}
