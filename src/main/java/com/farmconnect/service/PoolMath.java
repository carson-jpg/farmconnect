package com.farmconnect.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/** The few sums behind a group deal. Whole-number percentages, money to 2 decimals. */
public final class PoolMath {
    private PoolMath() {}

    public static BigDecimal total(Collection<BigDecimal> qtys) {
        BigDecimal t = BigDecimal.ZERO;
        if (qtys != null) for (BigDecimal q : qtys) if (q != null) t = t.add(q);
        return t;
    }

    /** How full the deal is, 0-100 (capped; a deal can be over-subscribed). */
    public static int progressPct(BigDecimal committed, BigDecimal target) {
        if (committed == null || target == null || target.signum() <= 0) return 0;
        int p = committed.multiply(BigDecimal.valueOf(100)).divide(target, 0, RoundingMode.DOWN).intValue();
        return Math.max(0, Math.min(100, p));
    }

    /** What a member owes or earns: quantity x price, or null when no price is set yet. */
    public static BigDecimal amount(BigDecimal qty, BigDecimal price) {
        if (qty == null || price == null) return null;
        return qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
    }

    /** Saving of the group price against the normal price, in whole percent, or null when it cannot be worked out or is not a saving. */
    public static Integer discountPct(BigDecimal list, BigDecimal group) {
        if (list == null || group == null || list.signum() <= 0 || group.signum() < 0 || group.compareTo(list) >= 0) return null;
        return list.subtract(group).multiply(BigDecimal.valueOf(100)).divide(list, 0, RoundingMode.HALF_UP).intValue();
    }
}
