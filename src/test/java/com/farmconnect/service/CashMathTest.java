package com.farmconnect.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class CashMathTest {
    @Test void expectedRoundsUpToWholeShillings() {
        assertEquals(10_100, CashMath.expectedCents(new BigDecimal("100.01")));
        assertEquals(10_000, CashMath.expectedCents(new BigDecimal("100.00")));
        assertEquals(100, CashMath.expectedCents(new BigDecimal("0.20")));
    }

    @Test void splitIsFivePercentFivePercentRest() {
        CashMath.Parts p = CashMath.split(100_000, 500, 500);     // KES 1,000
        assertEquals(5_000, p.platformCents());
        assertEquals(5_000, p.countyCents());
        assertEquals(90_000, p.farmerCents());
        assertEquals(10_000, p.billCents());
    }

    @Test void partsAlwaysAddUp() {
        for (long kes = 1; kes <= 2_000; kes++) {
            CashMath.Parts p = CashMath.split(kes * 100, 500, 500);
            assertEquals(p.saleCents(), p.platformCents() + p.countyCents() + p.farmerCents(), "kes " + kes);
            assertEquals(0, p.platformCents() % 100);
            assertEquals(0, p.countyCents() % 100);
        }
    }

    @Test void rejectsCentsAndZero() {
        assertThrows(IllegalArgumentException.class, () -> CashMath.split(150, 500, 500));
        assertThrows(IllegalArgumentException.class, () -> CashMath.split(0, 500, 500));
    }

    @Test void overdueAfterTheLimit() {
        Instant now = Instant.parse("2026-10-20T00:00:00Z");
        assertFalse(CashMath.overdue(now.minus(7, ChronoUnit.DAYS), now, 7));
        assertTrue(CashMath.overdue(now.minus(8, ChronoUnit.DAYS), now, 7));
        assertFalse(CashMath.overdue(null, now, 7));
    }
}
